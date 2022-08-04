package artemis.better_climbing.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	public LivingEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Redirect(
		method = "handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D")
	)
	private double better_climbing_modifyHorizontalMovement(double speed, double vanillaSpeedMin, double vanillaSpeedMax) {
		if (!this.onGround && this.isCrouching()) {
			return Mth.clamp(speed, vanillaSpeedMin, vanillaSpeedMax);
		} else {
			return speed;
		}
	}

	@Redirect(
		method = "handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
		at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D")
	)
	private double better_climbing_modifyVerticalMovement(double y, double vanillaDownSpeed) {
		double maxDownSpeed = Mth.clampedMap(this.getXRot(), 20, 90, vanillaDownSpeed, -0.4);
		return Math.max(y, maxDownSpeed);
	}

	@ModifyArg(
		method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;<init>(DDD)V"), index = 1
	)
	private double better_climbing_allowJumpingInLadder(double climbYSpeed) {
		return Math.max(this.getDeltaMovement().y, climbYSpeed + 0.05);
	}
}