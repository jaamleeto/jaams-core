
package net.jaams.jaamscore.manager;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.JsonObject;

public class LastAppliedConfigManager {
	private static final Map<UUID, JsonObject> lastConfigMap = new HashMap<>();

	public static JsonObject getLastConfig(UUID entityUUID) {
		return lastConfigMap.get(entityUUID);
	}

	public static void setLastConfig(UUID entityUUID, JsonObject config) {
		lastConfigMap.put(entityUUID, config);
	}
}
