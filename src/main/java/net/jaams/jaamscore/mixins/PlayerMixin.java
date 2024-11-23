package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;

import net.jaams.jaamscore.manager.ScaleManager;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "defineSynchedData", at = @At("RETURN"))
	private void onEntityInit(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		float scaleFactor = ScaleManager.getScale(player.getUUID());
		if (scaleFactor != 1.0F) {
			player.refreshDimensions();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		float scaleFactor = ScaleManager.getScale(player.getUUID());
		if (scaleFactor != 1.0F) {
			player.refreshDimensions();
		}
	}

	@Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
	public void modifyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		Player player = (Player) (Object) this;
		float scaleFactor = ScaleManager.getScale(player.getUUID());
		if (scaleFactor != 1.0F) {
			EntityDimensions currentDimensions = cir.getReturnValue();
			EntityDimensions newDimensions = new EntityDimensions(currentDimensions.width * scaleFactor, currentDimensions.height * scaleFactor, currentDimensions.fixed);
			cir.setReturnValue(newDimensions);
		}
	}
}
