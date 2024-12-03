package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.MultiBufferSource;

import net.jaams.jaamscore.init.JaamsCoreModAttributes;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(software.bernie.geckolib.renderer.GeoEntityRenderer.class)
public abstract class GeoEntityRendererMixin {
	@Inject(method = "preRender*", at = @At("TAIL"), remap = false)
	private <T extends Entity & software.bernie.geckolib.core.animatable.GeoAnimatable> void preRender$RandomMobSizes(PoseStack poseStack, T animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, MultiBufferSource bufferSource,
			VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
		if (animatable instanceof Mob mob) {
			AttributeInstance coreScaleAttribute = mob.getAttribute(JaamsCoreModAttributes.CORESCALE.get());
			if (coreScaleAttribute != null) {
				float scaleFactor = (float) coreScaleAttribute.getValue();
				poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
			}
		}
	}
}
