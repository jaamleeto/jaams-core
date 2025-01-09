package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;

import net.jaams.jaamscore.init.JaamsCoreModAttributes;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
	public void modifyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		Player player = (Player) (Object) this;
		AttributeInstance coreScaleAttribute = player.getAttribute(JaamsCoreModAttributes.CORESCALE.get());
		if (coreScaleAttribute != null) {
			float scaleFactor = (float) coreScaleAttribute.getValue();
			if (scaleFactor != 1.0F) {
				EntityDimensions currentDimensions = cir.getReturnValue();
				EntityDimensions newDimensions = new EntityDimensions(currentDimensions.width * scaleFactor, currentDimensions.height * scaleFactor, currentDimensions.fixed);
				cir.setReturnValue(newDimensions);
			}
		}
	}
}
