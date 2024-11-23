package net.jaams.jaamscore.init;

import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.jaams.jaamscore.configuration.JaamsCoreCommonConfiguration;
import net.jaams.jaamscore.configuration.JaamsCoreClientConfiguration;
import net.jaams.jaamscore.JaamsCoreMod;

@Mod.EventBusSubscriber(modid = JaamsCoreMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class JaamsCoreModConfigs {
	@SubscribeEvent
	public static void register(FMLConstructModEvent event) {
		event.enqueueWork(() -> {
			ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, JaamsCoreClientConfiguration.SPEC, "jaams_core_client.toml");
			ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, JaamsCoreCommonConfiguration.SPEC, "jaams_core_common.toml");
		});
	}
}
