package net.jaams.jaamscore.mixins;

import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

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

@Mixin(software.bernie.geckolib.renderer.GeoReplacedEntityRenderer.class)
public abstract class GeoReplacedEntityRendererMixin {
	@Inject(method = "preRender", at = @At("TAIL"), remap = false)
	private <T extends software.bernie.geckolib.core.animatable.GeoAnimatable> void preRenderCallback$RandomMobSizes(PoseStack poseStack, T animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, MultiBufferSource bufferSource,
			VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, CallbackInfo ci) {
		Entity entity = GeoReplacedEntityRenderer.class.cast(this).getCurrentEntity(); // Sin usar @Shadow
		if (entity instanceof Mob mob) {
			AttributeInstance coreScaleAttribute = mob.getAttribute(JaamsCoreModAttributes.CORESCALE.get());
			if (coreScaleAttribute != null) {
				float scaleFactor = (float) coreScaleAttribute.getValue();
				poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
			}
		}
	}
}
