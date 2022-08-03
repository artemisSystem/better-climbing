package artemis.better_climbing.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
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
	private double better_climbing_modifyHorizontalMovement(double val, double _trashLow, double _trashHigh) {
		return val;
	}


	@Redirect(
		method = "handleOnClimbable(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
		at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(DD)D")
	)
	private double better_climbing_modifyY(double y, double _trashLow) {
		double maxDownSpeed = Mth.clampedMap(this.getXRot(), 20, 90, -0.15, -0.4);
		return Math.max(y, maxDownSpeed);
	}

	@ModifyArg(
					method = "handleRelativeFrictionAndCalculateMovement(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
					at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;<init>(DDD)V"), index = 1
	)
	private double better_climbing_allowJumpingInLadder(double climbYSpeed) {
		return Math.max(this.getDeltaMovement().y, climbYSpeed);
	}
}