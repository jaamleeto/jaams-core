package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.BEFORE))
	private <E extends Entity> void injectBeforeRender(E entity, double x, double y, double z, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
		if (entity instanceof Projectile) {
			RenderSystem.enableDepthTest();
			float widthScale = getWidthScale(entity, partialTicks);
			float heightScale = getHeightScale(entity, partialTicks);
			poseStack.pushPose();
			poseStack.scale(widthScale, heightScale, widthScale);
			poseStack.translate(x / widthScale - x, y / heightScale - y, z / widthScale - z);
		}
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = At.Shift.AFTER))
	private <E extends Entity> void injectAfterRender(E entity, double x, double y, double z, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
		if (entity instanceof Projectile) {
			poseStack.popPose();
			RenderSystem.disableDepthTest();
		}
	}

	private float getWidthScale(Entity entity, float partialTicks) {
		return 1.5F;
	}

	private float getHeightScale(Entity entity, float partialTicks) {
		return 1.5F;
	}
}
