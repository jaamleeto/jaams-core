
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.jaams.jaamscore.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.jaams.jaamscore.client.renderer.CorePlayerRenderer;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class JaamsCoreModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(JaamsCoreModEntities.CORE_PLAYER.get(), CorePlayerRenderer::new);
	}
}
