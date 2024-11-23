
package net.jaams.jaamscore.manager;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.Map;

public class ScaleManager {
	private static final Map<UUID, Float> entityScales = new ConcurrentHashMap<>();

	public static void setScale(ServerLevel serverLevel, UUID entityUUID, float scale) {
		if (scale == 1.0f) {
			entityScales.remove(entityUUID);
		} else {
			entityScales.put(entityUUID, scale);
		}
		Entity entity = serverLevel.getEntity(entityUUID);
	}

	public static void setScale(UUID entityUUID, float scale) {
		if (scale == 1.0f) {
			entityScales.remove(entityUUID);
		} else {
			entityScales.put(entityUUID, scale);
		}
	}

	public static float getScale(UUID entityUUID) {
		return entityScales.getOrDefault(entityUUID, 1.0f);
	}

	public static boolean hasCustomScale(UUID entityUUID) {
		return entityScales.containsKey(entityUUID);
	}

	public static void removeScale(ServerLevel serverLevel, UUID entityUUID) {
		entityScales.remove(entityUUID);
		Entity entity = serverLevel.getEntity(entityUUID);
	}

	public static void removeScale(UUID entityUUID) {
		entityScales.remove(entityUUID);
	}

	public static void clear() {
		entityScales.clear();
	}
}
