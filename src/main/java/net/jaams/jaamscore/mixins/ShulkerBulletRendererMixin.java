package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.ShulkerBulletRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import net.jaams.jaamscore.manager.ScaleManager;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(ShulkerBulletRenderer.class)
public abstract class ShulkerBulletRendererMixin extends EntityRenderer<ShulkerBullet> {
	private ShulkerBulletRendererMixin(EntityRendererProvider.Context context) {
		super(context);
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void injectCustomScale(ShulkerBullet bullet, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
		Entity owner = bullet.getOwner();
		if (owner != null) {
			float customScale = ScaleManager.getScale(owner.getUUID());
			if (customScale != 1.0F) {
				poseStack.scale(customScale, customScale, customScale);
			}
		}
	}
}
