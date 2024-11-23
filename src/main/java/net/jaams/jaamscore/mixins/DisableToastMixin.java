package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.Advancement;

import net.jaams.jaamscore.configuration.JaamsCoreClientConfiguration;

@Mixin(ToastComponent.class)
public class DisableToastMixin {
	@Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
	private void disableUnwantedToasts(Toast toast, CallbackInfo ci) {
		if (toast instanceof AdvancementToast advancementToast) {
			Advancement advancement = ((AdvancementAccessorMixin) advancementToast).getAdvancement();
			if (advancement != null && advancement.getDisplay() != null) {
				FrameType frameType = advancement.getDisplay().getFrame();
				if (frameType == FrameType.TASK) {
					if (!JaamsCoreClientConfiguration.TOASTTASK.get()) {
						ci.cancel();
					}
				} else if (frameType == FrameType.GOAL) {
					if (!JaamsCoreClientConfiguration.TOASTGOAL.get()) {
						ci.cancel();
					}
				} else if (frameType == FrameType.CHALLENGE) {
					if (!JaamsCoreClientConfiguration.TOASTCHALLENGE.get()) {
						ci.cancel();
					}
				} else {
					if (!JaamsCoreClientConfiguration.TOASTOTHER.get()) {
						ci.cancel();
					}
				}
			}
		}
		if (toast instanceof RecipeToast) {
			if (!JaamsCoreClientConfiguration.TOASTRECIPE.get()) {
				ci.cancel();
			}
		}
		if (!(toast instanceof AdvancementToast) && !(toast instanceof RecipeToast)) {
			if (!JaamsCoreClientConfiguration.TOASTOTHER.get()) {
				ci.cancel();
			}
		}
	}
}
