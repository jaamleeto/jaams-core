package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import net.jaams.jaamscore.manager.ScaleManager;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(ThrownTridentRenderer.class)
public abstract class ThrownTridentRendererMixin extends EntityRenderer<ThrownTrident> {
	protected ThrownTridentRendererMixin(EntityRendererProvider.Context context) {
		super(context);
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void adjustTridentScale(ThrownTrident trident, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		Entity owner = trident.getOwner();
		float customScale = ScaleManager.getScale(owner.getUUID());
		if (customScale != 1.0F) {
			matrixStack.scale(customScale, customScale, customScale);
		}
	}
}
