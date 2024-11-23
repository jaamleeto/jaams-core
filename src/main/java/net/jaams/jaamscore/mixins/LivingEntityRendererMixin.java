package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;

import net.jaams.jaamscore.manager.ScaleManager;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> {
	@Shadow
	protected abstract void scale(final T entity, final PoseStack poseStack, final float partialTick);

	@Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;scale(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private void onScaleRedirect(final LivingEntityRenderer livingRenderer, final T entity, final PoseStack poseStack, final float partialTick) {
		this.scale(entity, poseStack, partialTick);
		float scaleFactor = ScaleManager.getScale(entity.getUUID());
		poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
	}
}
