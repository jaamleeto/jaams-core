package net.jaams.jaamscore.config;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraft.world.entity.EntityType;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
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

import java.io.Writer;
import java.io.IOException;

import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

public class EntityConfigLoader {
	private static final Gson GSON = new Gson();
	private static final String CONFIG_DIR = "config/jaams/core_settings/core_entity/";
	private static final Logger LOGGER = LogManager.getLogger();
	private static Map<String, List<JsonObject>> entityConfigs = new HashMap<>();
	private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private static Thread watcherThread;

	public static void init() {
		loadAllEntityConfigs();
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
			try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(configDirectoryPath)) {
				if (!stream.iterator().hasNext()) {
					createDefaultJsonFile(configDirectoryPath);
				}
			}
			try (DirectoryStream<java.nio.file.Path> jsonStream = Files.newDirectoryStream(configDirectoryPath, "*.json")) {
				for (java.nio.file.Path filePath : jsonStream) {
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

	private static final Set<String> warnedFiles = new HashSet<>();

	private static void loadJsonFile(java.nio.file.Path filePath) {
		try {
			String content = new String(Files.readAllBytes(filePath));
			JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
			if (!jsonObject.has("id")) {
				String generatedId = generateUniqueId(filePath);
				jsonObject.addProperty("id", generatedId);
			}
			if (jsonObject.has("target")) {
				JsonElement entityElement = jsonObject.get("target");
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
				String filePathString = filePath.toString();
				if (!warnedFiles.contains(filePathString)) {
					LOGGER.warn("The JSON file '{}' does not contain a 'target' key.", filePathString);
					warnedFiles.add(filePathString);
				}
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
	}

	public static Map<String, List<JsonObject>> getEntityConfigs() {
		return entityConfigs;
	}

	public static void createDefaultJsonFile(java.nio.file.Path configDirectoryPath) {
		java.nio.file.Path defaultFilePath = configDirectoryPath.resolve("default_example.json");
		// Crear la configuración JSON por defecto
		JsonObject defaultConfig = new JsonObject();
		defaultConfig.addProperty("description", "Default configuration file");
		JsonArray entities = new JsonArray();
		entities.add("jaams_core:example_entity");
		defaultConfig.add("target", entities);
		JsonObject equipment = new JsonObject();
		// Configuración de mainhand
		JsonObject mainhand = new JsonObject();
		mainhand.addProperty("id", "example:custom_sword");
		mainhand.addProperty("drop_chance", 0.5);
		mainhand.addProperty("quantity", 1);
		JsonArray mainhandAttributes = new JsonArray();
		JsonObject attackDamage = new JsonObject();
		attackDamage.addProperty("id", "example:attack_damage");
		attackDamage.addProperty("amount", 5.0);
		attackDamage.addProperty("operation", "add");
		mainhandAttributes.add(attackDamage);
		mainhand.add("attributes", mainhandAttributes);
		JsonArray mainhandArray = new JsonArray();
		mainhandArray.add(mainhand);
		equipment.add("mainhand", mainhandArray);
		// Configuración de head
		JsonObject head = new JsonObject();
		head.addProperty("id", "example:custom_helmet");
		head.addProperty("drop_chance", 0.2);
		head.addProperty("quantity", 1);
		JsonArray lore = new JsonArray();
		lore.add("A legendary helmet");
		lore.add("Worn by ancient heroes");
		head.add("lore", lore);
		JsonArray headArray = new JsonArray();
		headArray.add(head);
		equipment.add("head", headArray);
		defaultConfig.add("equipment", equipment);
		// Configuración de atributos
		JsonObject attributes = new JsonObject();
		JsonObject health = new JsonObject();
		health.addProperty("value", 20.0);
		health.addProperty("name", "Increased Health");
		health.addProperty("operation", "add");
		attributes.add("example:generic.max_health", health);
		JsonObject speed = new JsonObject();
		speed.addProperty("value", 0.2);
		speed.addProperty("name", "Speed Boost");
		speed.addProperty("operation", "multiply_base");
		attributes.add("example:generic.movement_speed", speed);
		defaultConfig.add("attributes", attributes);
		// Configuración de efectos de poción
		JsonObject potionEffects = new JsonObject();
		JsonObject strength = new JsonObject();
		strength.addProperty("level", 1);
		strength.addProperty("duration", 1200);
		potionEffects.add("example:strength", strength);
		JsonObject regeneration = new JsonObject();
		regeneration.addProperty("level", 2);
		regeneration.addProperty("duration", 600);
		potionEffects.add("example:regeneration", regeneration);
		defaultConfig.add("potion_effects", potionEffects);
		// Configuración de entity_settings
		JsonObject entitySettings = new JsonObject();
		JsonArray scale = new JsonArray();
		scale.add(1.5);
		entitySettings.add("scale", scale);
		JsonArray nameTag = new JsonArray();
		nameTag.add("Custom Entity");
		entitySettings.add("name_tag", nameTag);
		defaultConfig.add("target_settings", entitySettings);
		// Configuración de entry_settings
		JsonObject entrySettings = new JsonObject();
		entrySettings.addProperty("disable_spawn", false);
		entrySettings.addProperty("allow_re_modification", true);
		defaultConfig.add("entry_settings", entrySettings);
		// Escribir el JSON con formato bonito
		try (Writer writer = Files.newBufferedWriter(defaultFilePath)) {
			Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
			gsonPretty.toJson(defaultConfig, writer);
			LOGGER.info("Default configuration file created at: " + defaultFilePath);
		} catch (IOException e) {
			LOGGER.error("Error creating default configuration file: " + e.getMessage(), e);
		}
	}
}
