package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import net.jaams.jaamscore.manager.ScaleManager;

import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(ThrownItemRenderer.class)
public abstract class ThrownItemRendererMixin<T extends Entity & ItemSupplier> {
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), cancellable = true)
	private void modifyScale(T entity, float p_116086_, float p_116087_, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
		Entity owner = null;
		if (entity instanceof Projectile) {
			owner = ((Projectile) entity).getOwner();
		}
		if (owner != null) {
			float customScale = ScaleManager.getScale(owner.getUUID());
			if (customScale != 1.0F) {
				poseStack.scale(customScale, customScale, customScale);
			}
		}
	}
}
