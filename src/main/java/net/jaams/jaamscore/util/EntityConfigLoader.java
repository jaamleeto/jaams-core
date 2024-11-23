package net.jaams.jaamscore.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraft.world.entity.EntityType;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import java.nio.file.WatchService;
import java.nio.file.WatchKey;
import java.nio.file.WatchEvent;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.DirectoryStream;

import java.io.IOException;

import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.Gson;

public class EntityConfigLoader {
	private static final Gson GSON = new Gson();
	private static final String CONFIG_DIR = "config/jaams/core_entity/";
	private static final String DATAPACK_DIR = "data";
	private static final Logger LOGGER = LogManager.getLogger();
	private static Map<String, List<JsonObject>> entityConfigs = new HashMap<>();
	private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private static Thread watcherThread;

	public static void init() {
		loadAllEntityConfigs();
		loadDatapackConfigs(); // Cargar archivos JSON desde datapacks
		startWatchingConfigs();
	}

	public static void startWatchingConfigs() {
		if (watcherThread == null) {
			watcherThread = new Thread(() -> {
				try {
					java.nio.file.Path configPath = java.nio.file.Paths.get(CONFIG_DIR);
					WatchService watchService = FileSystems.getDefault().newWatchService();
					configPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
					while (true) {
						WatchKey key = watchService.take();
						for (WatchEvent<?> event : key.pollEvents()) {
							java.nio.file.Path changed = (java.nio.file.Path) event.context();
							if (changed.toString().endsWith(".json")) {
								java.nio.file.Path filePath = configPath.resolve(changed);
								if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
									LOGGER.info("JSON file created: " + changed);
									executorService.submit(() -> loadJsonFile(filePath));
								} else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
									LOGGER.info("JSON file modified: " + changed);
									executorService.submit(() -> loadJsonFile(filePath));
								} else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
									LOGGER.info("JSON file deleted: " + changed);
									removeConfigForFile(changed.toString());
								}
							}
						}
						key.reset();
					}
				} catch (IOException | InterruptedException e) {
					LOGGER.error("Error monitoring configuration directory: " + e.getMessage(), e);
				}
			});
			watcherThread.start();
		}
	}

	private static void removeConfigForFile(String fileName) {
		for (List<JsonObject> configs : entityConfigs.values()) {
			configs.removeIf(jsonObject -> jsonObject.has("id") && jsonObject.get("fileName").getAsString().equals(fileName));
		}
		LOGGER.info("Configurations deleted for file: " + fileName);
	}

	public static Map<String, List<JsonObject>> loadAllEntityConfigs() {
		entityConfigs.clear();
		try {
			java.nio.file.Path configDirectoryPath = Paths.get(CONFIG_DIR);
			if (!Files.exists(configDirectoryPath)) {
				Files.createDirectories(configDirectoryPath);
			}
			try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(configDirectoryPath, "*.json")) {
				for (java.nio.file.Path filePath : stream) {
					loadJsonFile(filePath);
				}
			} catch (IOException e) {
				LOGGER.error("Error reading JSON files in directory: " + CONFIG_DIR, e);
			}
		} catch (IOException e) {
			LOGGER.error("Error creating configuration directory: " + CONFIG_DIR, e);
		}
		return entityConfigs;
	}

	// Método adicional para cargar configuraciones desde datapacks
	private static void loadDatapackConfigs() {
		try {
			java.nio.file.Path datapackPath = Paths.get(DATAPACK_DIR);
			if (Files.exists(datapackPath)) {
				try (DirectoryStream<java.nio.file.Path> namespacesStream = Files.newDirectoryStream(datapackPath)) {
					for (java.nio.file.Path namespacePath : namespacesStream) {
						java.nio.file.Path coreEntityPath = namespacePath.resolve("core_entity");
						if (Files.exists(coreEntityPath)) {
							try (DirectoryStream<java.nio.file.Path> jsonStream = Files.newDirectoryStream(coreEntityPath, "*.json")) {
								for (java.nio.file.Path jsonFile : jsonStream) {
									loadJsonFile(jsonFile);
									LOGGER.info("Loaded JSON from datapack namespace: {}", jsonFile.toString());
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error loading datapack configurations: ", e);
		}
	}

	private static void loadJsonFile(java.nio.file.Path filePath) {
		try {
			String content = new String(Files.readAllBytes(filePath));
			JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
			// Generar un id único si no existe en el JSON
			if (!jsonObject.has("id")) {
				String generatedId = generateUniqueId(filePath);
				jsonObject.addProperty("id", generatedId);
			}
			if (jsonObject.has("entity")) {
				JsonElement entityElement = jsonObject.get("entity");
				jsonObject.addProperty("fileName", filePath.getFileName().toString());
				if (entityElement.isJsonArray()) {
					for (JsonElement element : entityElement.getAsJsonArray()) {
						String entityType = element.getAsString();
						entityConfigs.computeIfAbsent(entityType, k -> new ArrayList<>()).add(jsonObject);
					}
				} else {
					String entityType = entityElement.getAsString();
					entityConfigs.computeIfAbsent(entityType, k -> new ArrayList<>()).add(jsonObject);
				}
			} else {
				LOGGER.warn("The JSON file '{}' does not contain an 'entity' key.", filePath.toString());
			}
		} catch (NoSuchFileException e) {
			LOGGER.error("JSON file not found: '{}'", filePath);
		} catch (JsonSyntaxException e) {
			LOGGER.error("JSON syntax error in file '{}': {}", filePath, e.getMessage());
		} catch (IOException e) {
			LOGGER.error("Error processing JSON file: '{}'", filePath, e);
		}
	}

	private static String generateUniqueId(java.nio.file.Path filePath) {
		return filePath.getFileName().toString() + "_" + filePath.hashCode();
	}

	public static Map<String, JsonObject> parseEquipmentConfig(JsonObject jsonObject) {
		Map<String, JsonObject> equipmentConfigs = new HashMap<>();
		if (jsonObject.has("equipment")) {
			JsonObject equipmentObject = jsonObject.getAsJsonObject("equipment");
			for (Map.Entry<String, JsonElement> entry : equipmentObject.entrySet()) {
				equipmentConfigs.put(entry.getKey(), entry.getValue().getAsJsonObject());
			}
		}
		return equipmentConfigs;
	}

	public static Map<String, JsonObject> parseAttributesConfig(JsonObject jsonObject) {
		Map<String, JsonObject> attributesConfigs = new HashMap<>();
		if (jsonObject.has("attributes")) {
			JsonObject attributes = jsonObject.getAsJsonObject("attributes");
			attributes.entrySet().forEach(entry -> {
				String attributeId = entry.getKey();
				JsonObject attributeConfig = entry.getValue().getAsJsonObject();
				attributesConfigs.put(attributeId, attributeConfig);
			});
		}
		return attributesConfigs;
	}

	public static Map<String, JsonObject> parsePotionEffectsConfig(JsonObject jsonObject) {
		Map<String, JsonObject> potionEffectsConfig = new HashMap<>();
		if (jsonObject.has("potion_effects")) {
			JsonObject potionEffects = jsonObject.getAsJsonObject("potion_effects");
			potionEffects.entrySet().forEach(entry -> {
				potionEffectsConfig.put(entry.getKey(), entry.getValue().getAsJsonObject());
			});
		}
		return potionEffectsConfig;
	}

	public static Set<String> getRegisteredEntityTypes() {
		return entityConfigs.keySet();
	}

	public static JsonObject getEntityConfig(EntityType<?> entityType) {
		String entityName = EntityType.getKey(entityType).toString();
		List<JsonObject> configs = entityConfigs.get(entityName);
		return configs != null && !configs.isEmpty() ? configs.get(0) : null;
	}

	public static void reloadConfigs() {
		loadAllEntityConfigs();
		loadDatapackConfigs();
	}

	public static Map<String, List<JsonObject>> getEntityConfigs() {
		return entityConfigs;
	}
}
