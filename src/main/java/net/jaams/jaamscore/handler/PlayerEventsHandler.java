package net.jaams.jaamscore.handler;

import org.slf4j.LoggerFactory;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;

import net.jaams.jaamscore.config.ItemConfigLoader;

import com.google.gson.JsonObject;

@Mod.EventBusSubscriber
public class PlayerEventsHandler {
	@SubscribeEvent
	public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
		Player player = event.getEntity();
		if (player == null) {
			return;
		}
		ItemStack itemStack = event.getItemStack();
		try {
			JsonObject config = ItemConfigLoader.getItemConfig(itemStack);
			if (config != null && config.has("disable_interaction")) {
				JsonObject disableInteractionOption = config.getAsJsonObject("disable_interaction");
				if (ItemConfigLoader.evaluateCondition(disableInteractionOption, itemStack) && disableInteractionOption.has("value")) {
					boolean disabled = disableInteractionOption.get("value").getAsBoolean();
					if (disabled) {
						event.setCanceled(true);
						event.setCancellationResult(InteractionResult.FAIL);
						return;
					}
				}
			}
			if (config != null && config.has("use_item")) {
				JsonObject useItemOption = config.getAsJsonObject("use_item");
				if (ItemConfigLoader.evaluateCondition(useItemOption, itemStack) && useItemOption.has("enabled")) {
					boolean enabled = useItemOption.get("enabled").getAsBoolean();
					if (enabled) {
						player.startUsingItem(event.getHand());
						event.setCanceled(true);
						event.setCancellationResult(InteractionResult.CONSUME);
					}
				}
			}
		} catch (Exception e) {
			LoggerFactory.getLogger("ItemInteraction").error("Error handling right-click interaction: ", e);
		}
	}
}
