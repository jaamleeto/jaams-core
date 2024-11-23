package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;

import net.jaams.jaamscore.back_item.BackItemLayer;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public PlayerRendererMixin(EntityRendererProvider.Context pContext, PlayerModel<AbstractClientPlayer> pModel, float pShadowRadius) {
		super(pContext, pModel, pShadowRadius);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void onInit(EntityRendererProvider.Context context, boolean useSmallArms, CallbackInfo ci) {
		this.addLayer(new BackItemLayer(this));
	}
}
