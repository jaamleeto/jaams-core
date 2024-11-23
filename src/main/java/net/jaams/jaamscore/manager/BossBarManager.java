
package net.jaams.jaamscore.manager;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class BossBarManager {
	private static final ConcurrentHashMap<UUID, ServerBossEvent> bossBars = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<UUID, Float> renderDistances = new ConcurrentHashMap<>();

	public static void addBossBar(UUID entityUUID, ServerBossEvent bossBar, float renderDistance, boolean disappearsWhenFar, boolean onlyShowInCombat, boolean animateChanges) {
		bossBars.put(entityUUID, bossBar);
		renderDistances.put(entityUUID, renderDistance);
		bossBar.setVisible(false);
	}

	public static void removeBossBar(UUID entityUUID) {
		ServerBossEvent bossBar = bossBars.remove(entityUUID);
		renderDistances.remove(entityUUID);
		if (bossBar != null) {
			bossBar.removeAllPlayers();
		}
	}

	public static void hideBossBarForPlayer(UUID entityId, ServerPlayer player) {
		ServerBossEvent bossBar = bossBars.get(entityId);
		if (bossBar != null) {
			bossBar.removePlayer(player);
			if (bossBar.getPlayers().isEmpty()) {
				bossBar.setVisible(false);
			}
		}
	}

	public static void updateBossBar(UUID entityUUID, float health, float maxHealth, Player player, LivingEntity entity, boolean animateChanges) {
		ServerBossEvent bossBar = bossBars.get(entityUUID);
		Float renderDistance = renderDistances.get(entityUUID);
		if (bossBar != null && renderDistance != null) {
			double distanceSquared = player.distanceToSqr(entity);
			boolean inRange = distanceSquared <= renderDistance * renderDistance;
			float progress = health / maxHealth;
			if (animateChanges) {
				bossBar.setProgress(Mth.lerp(0.1f, bossBar.getProgress(), progress));
			} else {
				bossBar.setProgress(progress);
			}
			if (inRange) {
				if (!bossBar.getPlayers().contains(player)) {
					bossBar.addPlayer((ServerPlayer) player);
					bossBar.setVisible(true);
				}
			} else {
				if (bossBar.getPlayers().contains(player)) {
					bossBar.removePlayer((ServerPlayer) player);
					if (bossBar.getPlayers().isEmpty()) {
						bossBar.setVisible(false);
					}
				}
			}
		}
	}
}
