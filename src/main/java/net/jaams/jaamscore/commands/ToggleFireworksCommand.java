package net.jaams.jaamscore.commands;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.advancements.FrameType;

import java.util.Map;
import java.util.HashMap;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;

@Mod.EventBusSubscriber
public class ToggleFireworksCommand {
	private static final Map<ServerPlayer, Map<FrameType, Boolean>> playerFireworkPreferences = new HashMap<>();

	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("togglefireworks").then(Commands.argument("frametype", StringArgumentType.string()).suggests((context, builder) -> {
			builder.suggest("TASK").suggest("GOAL").suggest("CHALLENGE").suggest("ALL");
			return builder.buildFuture();
		}).executes(context -> {
			ServerPlayer player = context.getSource().getPlayerOrException();
			String frameTypeString = StringArgumentType.getString(context, "frametype").toUpperCase();
			return toggleFireworks(player, frameTypeString);
		})));
	}

	private static int toggleFireworks(ServerPlayer player, String frameTypeString) throws CommandSyntaxException {
		FrameType frameType = null;
		if (!frameTypeString.equals("ALL")) {
			try {
				frameType = FrameType.valueOf(frameTypeString);
			} catch (IllegalArgumentException e) {
				player.sendSystemMessage(Component.literal("Invalid advancement frame type: " + frameTypeString));
				return 0;
			}
		}
		// Obtener las preferencias del jugador, o crear un nuevo mapa si no existe
		Map<FrameType, Boolean> preferences = playerFireworkPreferences.computeIfAbsent(player, k -> new HashMap<>());
		if (frameType != null) {
			boolean currentSetting = preferences.getOrDefault(frameType, true);
			preferences.put(frameType, !currentSetting);
			player.sendSystemMessage(Component.literal("Fireworks for advancements of type " + frameTypeString + " are now " + (!currentSetting ? "enabled" : "disabled")));
		} else {
			boolean allEnabled = preferences.values().stream().allMatch(Boolean::booleanValue);
			for (FrameType type : FrameType.values()) {
				preferences.put(type, !allEnabled);
			}
			player.sendSystemMessage(Component.literal("Fireworks for advancements of all frame types are now " + (!allEnabled ? "enabled" : "disabled")));
		}
		return 1;
	}

	public static boolean shouldLaunchFireworks(ServerPlayer player, FrameType frameType) {
		return playerFireworkPreferences.getOrDefault(player, new HashMap<>()).getOrDefault(frameType, true);
	}
}
