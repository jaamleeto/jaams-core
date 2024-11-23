package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.LlamaSpitRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import net.jaams.jaamscore.manager.ScaleManager;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(LlamaSpitRenderer.class)
public abstract class LlamaSpitRendererMixin extends EntityRenderer<LlamaSpit> {
	private LlamaSpitRendererMixin(EntityRendererProvider.Context context) {
		super(context);
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void modifyScale(LlamaSpit llamaSpit, float p_115374_, float p_115375_, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
		Entity owner = llamaSpit.getOwner();
		if (owner != null) {
			float customScale = ScaleManager.getScale(owner.getUUID());
			if (customScale != 1.0F) {
				poseStack.scale(customScale, customScale, customScale);
			}
		}
	}
}
