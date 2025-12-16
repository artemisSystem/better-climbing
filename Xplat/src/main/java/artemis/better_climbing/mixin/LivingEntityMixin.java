package artemis.better_climbing.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	public LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	private int climbDownTicks = 0;
	private int climbUpTicks = 0;
	private boolean climbingUpThisTick = false;

	@Shadow
	public abstract boolean onClimbable();

	@WrapOperation(
		method = "handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D")
	)
	private double better_climbing_modifyHorizontalMovementWhenClimbing(double speed, double vanillaSpeedMin, double vanillaSpeedMax, Operation<Double> original) {
		// no-op on server
		if (!level().isClientSide()) return original.call(speed, vanillaSpeedMin, vanillaSpeedMax);

		if (!this.onGround() && this.isCrouching()) {
			return original.call(speed, vanillaSpeedMin, vanillaSpeedMax);
		} else {
			return speed;
		}
	}

	@WrapOperation(
		method = "handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
		at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D")
	)
	private double better_climbing_modifyVerticalMovementWhenClimbing(double currentYSpeed, double vanillaDownSpeed, Operation<Double> original) {
		// no-op on server
		if (!level().isClientSide()) return original.call(currentYSpeed, vanillaDownSpeed);

		// From looking 20 degrees down to 90 degrees down, scale downwards speed from vanilla speed (-0.15) to -0.4
		double maxDownSpeed = Mth.clampedMap(getXRot(), 20, 90, vanillaDownSpeed, -0.4);
		if (maxDownSpeed < -0.15) {
			// Increase down speed to 1.5x over 3 seconds
			maxDownSpeed = Mth.clampedMap(climbDownTicks, 0, 60, maxDownSpeed, maxDownSpeed * 1.5);
		}
		return original.call(currentYSpeed, maxDownSpeed);
	}

	@Inject(
		method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
		at = @At("RETURN")
	)
	private void better_climbing_incrementClimbTimer(CallbackInfoReturnable<Vec3> cir) {
		if (!level().isClientSide()) return;

		Vec3 movement = cir.getReturnValue();
		if (onClimbable() && movement.y < 0 && getXRot() > 20) {
			climbDownTicks++;
		} else {
			climbDownTicks = 0;
		}
		if (climbingUpThisTick) {
			climbUpTicks++;
			climbingUpThisTick = false;
		} else {
			climbUpTicks = 0;
		}
	}

	@ModifyArg(
		method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;<init>(DDD)V"), index = 1
	)
	private double better_climbing_allowJumpingInLadderAndSpeedUpClimbing(double vanillaClimbSpeed) {
		// no-op on server
		if (!level().isClientSide()) return vanillaClimbSpeed;

		climbingUpThisTick = true;
		// Vanilla speed is 0.20
		double increasedVanillaClimbSpeed = vanillaClimbSpeed * 1.25;
		// Increase climb speed to 2x over 3 seconds
		double climbYSpeed = Mth.clampedMap(climbUpTicks, 0, 60, increasedVanillaClimbSpeed, increasedVanillaClimbSpeed * 2);
		return Math.max(this.getDeltaMovement().y, climbYSpeed);
	}

	@WrapOperation(
		method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
		at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;horizontalCollision:Z", opcode = Opcodes.GETFIELD)
	)
	private boolean better_climbing_cancelNonDeliberateCollission(LivingEntity livingEntity, Operation<Boolean> original) {
		if (level().isClientSide() && (livingEntity instanceof LocalPlayer player)) {
			return original.call(livingEntity) && player.input.getMoveVector().length() > 0;
		}
		// no-op
		return original.call(livingEntity);
	}
}
