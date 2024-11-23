package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;

import net.jaams.jaamscore.init.JaamsCoreModConfigs;
import net.jaams.jaamscore.configuration.JaamsCoreCommonConfiguration;
import net.jaams.jaamscore.configuration.JaamsCoreClientConfiguration;

@Mixin(JaamsCoreModConfigs.class)
public abstract class JaamsCoreModConfigsMixin {
	@Inject(method = "register", at = @At("HEAD"), cancellable = true, remap = false)
	private static void injectRegister(FMLConstructModEvent event, CallbackInfo ci) {
		event.enqueueWork(() -> {
			ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, JaamsCoreClientConfiguration.SPEC, "jaams/jaams_core_client.toml");
			ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, JaamsCoreCommonConfiguration.SPEC, "jaams/jaams_core_common.toml");
		});
		ci.cancel();
	}
}
