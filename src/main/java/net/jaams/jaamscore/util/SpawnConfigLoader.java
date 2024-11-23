package net.jaams.jaamscore.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.jaams.jaamscore.manager.CustomSpawnManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
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
import com.google.gson.JsonArray;
import com.google.gson.Gson;

public class SpawnConfigLoader {
	private static final Gson GSON = new Gson();
	private static final String CONFIG_DIR = "config/jaams/core_spawns/";
	private static final String DATAPACK_DIR = "data";
	private static final Logger LOGGER = LogManager.getLogger();
	public static Map<String, List<JsonObject>> spawnConfigs = new HashMap<>();
	private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private static Thread watcherThread;

	public static void init() {
		loadAllSpawnConfigs();
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
		for (List<JsonObject> configs : spawnConfigs.values()) {
			configs.removeIf(jsonObject -> jsonObject.has("id") && jsonObject.get("fileName").getAsString().equals(fileName));
		}
		LOGGER.info("Configurations deleted for file: " + fileName);
	}

	public static Map<String, List<JsonObject>> loadAllSpawnConfigs() {
		spawnConfigs.clear();
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
		return spawnConfigs;
	}

	private static void loadDatapackConfigs() {
		try {
			java.nio.file.Path datapackPath = Paths.get(DATAPACK_DIR);
			if (Files.exists(datapackPath)) {
				try (DirectoryStream<java.nio.file.Path> namespacesStream = Files.newDirectoryStream(datapackPath)) {
					for (java.nio.file.Path namespacePath : namespacesStream) {
						java.nio.file.Path customSpawnPath = namespacePath.resolve("core_custom_spawn");
						if (Files.exists(customSpawnPath)) {
							try (DirectoryStream<java.nio.file.Path> jsonStream = Files.newDirectoryStream(customSpawnPath, "*.json")) {
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
			// Verificar si contiene los campos obligatorios
			if (!jsonObject.has("spawn") || !jsonObject.has("entity")) {
				LOGGER.warn("JSON format error in file '{}': Missing required fields (e.g., 'spawn', 'entity')", filePath);
				return;
			}
			// Generar un id único si no existe en el JSON
			if (!jsonObject.has("id")) {
				String generatedId = generateUniqueId(filePath);
				jsonObject.addProperty("id", generatedId);
			}
			jsonObject.addProperty("fileName", filePath.getFileName().toString());
			// Procesar el elemento 'spawn'
			JsonElement spawnElement = jsonObject.get("spawn");
			if (spawnElement.isJsonArray()) {
				for (JsonElement element : spawnElement.getAsJsonArray()) {
					String spawnType = element.getAsString();
					spawnConfigs.computeIfAbsent(spawnType, k -> new ArrayList<>()).add(jsonObject);
				}
			} else {
				String spawnType = spawnElement.getAsString();
				spawnConfigs.computeIfAbsent(spawnType, k -> new ArrayList<>()).add(jsonObject);
			}
			LOGGER.info("Loaded JSON file successfully: '{}'", filePath);
		} catch (NoSuchFileException e) {
			LOGGER.error("JSON file not found: '{}'", filePath);
		} catch (JsonSyntaxException e) {
			LOGGER.error("JSON syntax error in file '{}': {}", filePath, e.getMessage());
		} catch (IOException e) {
			LOGGER.error("Error processing JSON file: '{}'", filePath, e);
		}
	}

	public static CustomSpawnManager parseSpawnConfig(JsonObject spawnJson) {
		CustomSpawnManager spawnManager = new CustomSpawnManager();
		// Parsear la entidad
		spawnManager.entity = spawnJson.get("entity").getAsString();
		// Parsear las condiciones
		JsonObject conditionsJson = spawnJson.getAsJsonObject("conditions");
		if (conditionsJson != null) {
			CustomSpawnManager.Conditions conditions = new CustomSpawnManager.Conditions();
			// Biomas
			if (conditionsJson.has("biomes")) {
				conditions.biomes = parseStringList(conditionsJson.getAsJsonArray("biomes"));
			}
			// Estructuras
			if (conditionsJson.has("structures")) {
				conditions.structures = parseStringList(conditionsJson.getAsJsonArray("structures"));
			}
			// Bloques
			if (conditionsJson.has("on_blocks")) {
				conditions.on_blocks = parseStringList(conditionsJson.getAsJsonArray("on_blocks"));
			}
			// Altura mínima y máxima
			if (conditionsJson.has("min_height")) {
				conditions.min_height = conditionsJson.get("min_height").getAsInt();
			}
			if (conditionsJson.has("max_height")) {
				conditions.max_height = conditionsJson.get("max_height").getAsInt();
			}
			// Hora del día
			if (conditionsJson.has("time_of_day")) {
				conditions.time_of_day = conditionsJson.get("time_of_day").getAsString();
			}
			// Clima
			if (conditionsJson.has("weather")) {
				conditions.weather = conditionsJson.get("weather").getAsString();
			}
			spawnManager.conditions = conditions;
		}
		// Parsear configuraciones
		JsonObject settingsJson = spawnJson.getAsJsonObject("settings");
		if (settingsJson != null) {
			CustomSpawnManager.Settings settings = new CustomSpawnManager.Settings();
			// Probabilidad de spawn
			if (settingsJson.has("chance")) {
				settings.chance = settingsJson.get("chance").getAsDouble();
			}
			// Configuración del grupo de spawn
			if (settingsJson.has("spawnGroup")) {
				JsonObject groupJson = settingsJson.getAsJsonObject("spawnGroup");
				CustomSpawnManager.SpawnGroup spawnGroup = new CustomSpawnManager.SpawnGroup();
				if (groupJson.has("min")) {
					spawnGroup.min = groupJson.get("min").getAsInt();
				}
				if (groupJson.has("max")) {
					spawnGroup.max = groupJson.get("max").getAsInt();
				}
				settings.spawnGroup = spawnGroup;
			}
			spawnManager.settings = settings;
		}
		return spawnManager;
	}

	private static List<String> parseStringList(JsonArray jsonArray) {
		List<String> list = new ArrayList<>();
		for (JsonElement element : jsonArray) {
			list.add(element.getAsString());
		}
		return list;
	}

	private static String generateUniqueId(java.nio.file.Path filePath) {
		return filePath.getFileName().toString() + "_" + filePath.hashCode();
	}

	public static void reloadConfigs() {
		loadAllSpawnConfigs();
		loadDatapackConfigs();
	}

	public static Map<String, List<JsonObject>> getSpawnConfigs() {
		return spawnConfigs;
	}
}
