
package net.jaams.jaamscore.handler;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import net.jaams.jaamscore.manager.BossBarManager;

import java.util.UUID;
import java.util.Map;

import com.google.gson.JsonObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BossBarHandler {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Map<String, BossBarOverlay> OVERLAY_MAP = Map.of("progress", BossBarOverlay.PROGRESS, "notched_6", BossBarOverlay.NOTCHED_6, "notched_10", BossBarOverlay.NOTCHED_10, "notched_12", BossBarOverlay.NOTCHED_12, "notched_20",
			BossBarOverlay.NOTCHED_20);
	private static final Map<String, BossBarColor> COLOR_MAP = Map.of("red", BossBarColor.RED, "blue", BossBarColor.BLUE, "green", BossBarColor.GREEN, "yellow", BossBarColor.YELLOW, "purple", BossBarColor.PURPLE, "white", BossBarColor.WHITE, "pink",
			BossBarColor.PINK);

	public static void applyBossBar(LivingEntity entity, JsonObject config) {
		JsonObject bossBarSettings = config.getAsJsonObject("bossbar");
		if (bossBarSettings != null) {
			String title = bossBarSettings.has("title") ? bossBarSettings.get("title").getAsString() : entity.getName().getString();
			BossEvent.BossBarColor color = parseBossBarColor(bossBarSettings.has("color") ? bossBarSettings.get("color").getAsString() : "white");
			BossEvent.BossBarOverlay overlay = parseBossBarOverlay(bossBarSettings.has("overlay") ? bossBarSettings.get("overlay").getAsString() : "progress");
			float renderDistance = bossBarSettings.has("render_distance") ? bossBarSettings.get("render_distance").getAsFloat() : 32.0f;
			boolean disappearsWhenFar = bossBarSettings.has("disappears_when_far") && bossBarSettings.get("disappears_when_far").getAsBoolean();
			boolean onlyShowInCombat = bossBarSettings.has("only_show_in_combat") && bossBarSettings.get("only_show_in_combat").getAsBoolean();
			boolean animateChanges = bossBarSettings.has("animate_changes") && bossBarSettings.get("animate_changes").getAsBoolean();
			ServerBossEvent bossBar = new ServerBossEvent(Component.literal(title), color, overlay);
			bossBar.setProgress(entity.getHealth() / entity.getMaxHealth());
			BossBarManager.addBossBar(entity.getUUID(), bossBar, renderDistance, disappearsWhenFar, onlyShowInCombat, animateChanges);
			CompoundTag data = entity.getPersistentData();
			data.putString("bossBarTitle", title);
			data.putString("bossBarColor", color.name());
			data.putString("bossBarOverlay", overlay.name());
			data.putFloat("bossBarRenderDistance", renderDistance);
			data.putBoolean("bossBarDisappearsWhenFar", disappearsWhenFar);
			data.putBoolean("bossBarOnlyShowInCombat", onlyShowInCombat);
			data.putBoolean("bossBarAnimateChanges", animateChanges);
		}
	}

	@SubscribeEvent
	public static void onLivingBossbarUpdate(LivingEvent.LivingTickEvent event) {
		LivingEntity entity = event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null;
		if (entity == null || entity.isDeadOrDying() || entity.isRemoved()) {
			BossBarManager.removeBossBar(entity != null ? entity.getUUID() : event.getEntity().getUUID());
			return;
		}
		UUID entityId = entity.getUUID();
		CompoundTag data = entity.getPersistentData();
		boolean disappearsWhenFar = data.getBoolean("bossBarDisappearsWhenFar");
		boolean onlyShowInCombat = data.getBoolean("bossBarOnlyShowInCombat");
		boolean animateChanges = data.getBoolean("bossBarAnimateChanges");
		for (Player player : entity.level().players()) {
			if (player instanceof ServerPlayer) {
				double distance = player.distanceTo(entity);
				if (disappearsWhenFar && distance > data.getFloat("bossBarRenderDistance")) {
					BossBarManager.hideBossBarForPlayer(entityId, (ServerPlayer) player);
					continue;
				}
				if (onlyShowInCombat && entity.getLastHurtByMob() == null) {
					BossBarManager.hideBossBarForPlayer(entityId, (ServerPlayer) player);
					continue;
				}
				// Actualiza la boss bar con o sin animación según el valor de animateChanges
				BossBarManager.updateBossBar(entityId, entity.getHealth(), entity.getMaxHealth(), player, entity, animateChanges);
			}
		}
	}

	@SubscribeEvent
	public static void onBossbarEntityJoinWorld(EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof LivingEntity livingEntity && !event.getLevel().isClientSide()) {
			restoreBossBar(livingEntity);
		}
	}

	@SubscribeEvent
	public static void onBossbarEntityLeaveWorld(EntityLeaveLevelEvent event) {
		if (event.getEntity() instanceof LivingEntity livingEntity && !event.getLevel().isClientSide()) {
			BossBarManager.removeBossBar(livingEntity.getUUID());
		}
	}

	public static void restoreBossBar(LivingEntity entity) {
		CompoundTag data = entity.getPersistentData();
		if (data.contains("bossBarTitle") && data.contains("bossBarColor") && data.contains("bossBarOverlay")) {
			String title = data.getString("bossBarTitle");
			BossEvent.BossBarColor color = BossEvent.BossBarColor.valueOf(data.getString("bossBarColor"));
			BossEvent.BossBarOverlay overlay = BossEvent.BossBarOverlay.valueOf(data.getString("bossBarOverlay"));
			float renderDistance = data.getFloat("bossBarRenderDistance");
			boolean disappearsWhenFar = data.getBoolean("bossBarDisappearsWhenFar");
			boolean onlyShowInCombat = data.getBoolean("bossBarOnlyShowInCombat");
			boolean animateChanges = data.getBoolean("bossBarAnimateChanges");
			ServerBossEvent bossBar = new ServerBossEvent(Component.literal(title), color, overlay);
			bossBar.setProgress(entity.getHealth() / entity.getMaxHealth());
			BossBarManager.addBossBar(entity.getUUID(), bossBar, renderDistance, disappearsWhenFar, onlyShowInCombat, animateChanges);
		}
	}

	private static BossBarOverlay parseBossBarOverlay(String overlay) {
		if (OVERLAY_MAP.containsKey(overlay.toLowerCase())) {
			return OVERLAY_MAP.get(overlay.toLowerCase());
		} else {
			LOGGER.warn("Formato de overlay incorrecto: '{}'. Usando valor predeterminado 'progress'.", overlay);
			return BossBarOverlay.PROGRESS;
		}
	}

	private static BossBarColor parseBossBarColor(String color) {
		if (COLOR_MAP.containsKey(color.toLowerCase())) {
			return COLOR_MAP.get(color.toLowerCase());
		} else {
			LOGGER.warn("Formato de color incorrecto: '{}'. Usando valor predeterminado 'white'.", color);
			return BossBarColor.WHITE;
		}
	}
}
