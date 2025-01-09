package net.jaams.jaamscore.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;

import net.jaams.jaamscore.util.ModUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.WatchService;
import java.nio.file.WatchKey;
import java.nio.file.WatchEvent;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.FileSystems;

import java.io.IOException;
import java.io.BufferedWriter;

import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

public class ItemTransformConfigLoader {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CONFIG_DIR = "config/jaams/core_settings/core_transforms/";
	private static final Logger LOGGER = LoggerFactory.getLogger(ItemTransformConfigLoader.class);
	private static final Map<String, List<JsonObject>> itemTransforms = new ConcurrentHashMap<>();

	public static void init() {
		loadAllItemTransforms();
		startWatchingConfigs();
	}

	private static void loadAllItemTransforms() {
		Set<String> previousKeys = new HashSet<>(itemTransforms.keySet());
		itemTransforms.clear();
		try {
			Path configPath = Paths.get(CONFIG_DIR);
			if (!Files.exists(configPath)) {
				Files.createDirectories(configPath);
			}
			Files.walkFileTree(configPath, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toString().endsWith(".json")) {
						loadTransformConfig(file);
					}
					return FileVisitResult.CONTINUE;
				}
			});
			// Comparar las claves cargadas con las anteriores y eliminar las obsoletas
			previousKeys.removeAll(itemTransforms.keySet());
			for (String removedKey : previousKeys) {
				LOGGER.info("Removing outdated transform config for item: {}", removedKey);
				itemTransforms.remove(removedKey);
			}
			// Realizar limpieza de transformaciones obsoletas
			cleanUpObsoleteTransforms();
			if (itemTransforms.isEmpty()) {
				LOGGER.warn("No transform configs found. Generating default example config.");
				generateExampleJson();
				loadAllItemTransforms(); // Recargar después de generar ejemplo
			}
		} catch (IOException e) {
			LOGGER.error("Error loading item transformation configs: ", e);
		}
	}

	private static void loadTransformConfig(Path path) {
		try {
			String content = Files.readString(path);
			JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
			if (!validateJsonConfig(jsonObject)) {
				LOGGER.warn("Skipping invalid transform config: {}", path);
				return;
			}
			String targetItem = jsonObject.get("target").getAsString();
			itemTransforms.computeIfAbsent(targetItem, k -> new ArrayList<>()).add(jsonObject);
			LOGGER.info("Loaded or updated transform config for item: {}", targetItem);
		} catch (IOException | JsonSyntaxException e) {
			LOGGER.error("Error reading transform config from file '{}': {}", path, e.getMessage());
		}
	}

	private static void startWatchingConfigs() {
		Thread watcherThread = new Thread(() -> {
			while (true) {
				try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
					Path configPath = Paths.get(CONFIG_DIR);
					if (!Files.exists(configPath)) {
						Files.createDirectories(configPath);
					}
					configPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
					while (true) {
						WatchKey key = watchService.take();
						try {
							for (WatchEvent<?> event : key.pollEvents()) {
								Path filePath = configPath.resolve((Path) event.context());
								if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE || event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
									LOGGER.info("Detected change in config file: {}", filePath);
									removeTransformConfig(filePath);
									loadTransformConfig(filePath);
								} else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
									LOGGER.info("Detected deletion of config file: {}", filePath);
									removeTransformConfig(filePath);
								}
							}
						} catch (Exception e) {
							LOGGER.error("Error processing events for config directory: ", e);
						} finally {
							key.reset();
						}
						cleanUpObsoleteTransforms(); // Limpieza de transformaciones obsoletas
						cleanUpEntitiesUsingObsoleteTransforms(); // Limpieza de entidades con datos residuales
					}
				} catch (IOException | InterruptedException e) {
					LOGGER.error("Error monitoring item transform config directory, restarting watcher: ", e);
					try {
						Thread.sleep(5000); // Retardo antes de reiniciar
					} catch (InterruptedException ignored) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}, "Config-Watcher-Thread");
		watcherThread.setDaemon(true);
		watcherThread.start();
	}

	private static void removeTransformConfig(Path path) {
		try {
			String content = Files.readString(path);
			JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
			if (validateJsonConfig(jsonObject)) {
				String targetItem = jsonObject.get("target").getAsString();
				itemTransforms.remove(targetItem);
				LOGGER.info("Removed outdated transform config for item: {}", targetItem);
			}
		} catch (IOException | JsonSyntaxException e) {
			LOGGER.error("Error removing transform config from file '{}': {}", path, e.getMessage());
		}
	}

	private static boolean validateJsonConfig(JsonObject jsonObject) {
		return jsonObject.has("id") && jsonObject.has("transformations") && jsonObject.has("target");
	}

	private static void cleanUpObsoleteTransforms() {
		try {
			// Crear un conjunto para almacenar todos los targets válidos desde los archivos JSON actuales
			Set<String> validTargets = new HashSet<>();
			// Recorrer todos los archivos JSON en el directorio de configuración
			Path configPath = Paths.get(CONFIG_DIR);
			Files.walkFileTree(configPath, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toString().endsWith(".json")) {
						String content = Files.readString(file);
						try {
							JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
							if (validateJsonConfig(jsonObject)) {
								validTargets.add(jsonObject.get("target").getAsString());
							}
						} catch (JsonSyntaxException e) {
							LOGGER.warn("Skipping invalid JSON file during cleanup: {}", file);
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});
			// Comparar los targets actuales en itemTransforms con los válidos
			Set<String> currentTargets = new HashSet<>(itemTransforms.keySet());
			for (String target : currentTargets) {
				if (!validTargets.contains(target)) {
					// Si un target ya no es válido, eliminarlo
					itemTransforms.remove(target);
					LOGGER.info("Removed obsolete transform config for item: {}", target);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error during cleanup of obsolete item transforms: ", e);
		}
	}

	private static void cleanUpEntitiesUsingObsoleteTransforms() {
		try {
			Set<String> validTargets = new HashSet<>(itemTransforms.keySet());
			Minecraft.getInstance().level.entitiesForRendering().forEach(entity -> {
				entity.getAllSlots().forEach(stack -> {
					if (!stack.isEmpty()) {
						ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
						if (registryName != null && !validTargets.contains(registryName.toString())) {
							LOGGER.info("Clearing obsolete transform for entity: {} with item: {}", entity.getName().getString(), registryName);
							stack.getOrCreateTag().remove("transformData");
						}
					}
				});
			});
		} catch (Exception e) {
			LOGGER.error("Error during cleanup of entities using obsolete transforms: ", e);
		}
	}

	public static JsonObject getTransformConfig(ItemStack stack, Entity entity) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (registryName != null) {
			String itemKey = registryName.toString();
			List<JsonObject> configs = itemTransforms.get(itemKey);
			if (configs != null) {
				return selectBestConfig(configs, stack, entity);
			}
		}
		// Evaluar configuraciones por tag
		for (Map.Entry<String, List<JsonObject>> entry : itemTransforms.entrySet()) {
			String key = entry.getKey();
			if (key.startsWith("#")) {
				String tagName = key.substring(1);
				TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), new ResourceLocation(tagName));
				if (stack.is(tag)) {
					return selectBestConfig(entry.getValue(), stack, entity);
				}
			}
		}
		return null;
	}

	private static JsonObject selectBestConfig(List<JsonObject> configs, ItemStack stack, Entity entity) {
		JsonObject bestConfig = null;
		int bestPriority = Integer.MIN_VALUE;
		for (JsonObject config : configs) {
			if (applyConditions(config, stack, entity)) {
				int priority = config.has("priority") ? config.get("priority").getAsInt() : 0;
				if (priority > bestPriority) {
					bestConfig = config;
					bestPriority = priority;
				}
			}
		}
		return bestConfig;
	}

	private static boolean applyConditions(JsonObject config, ItemStack stack, Entity entity) {
		if (config.has("conditions")) {
			JsonObject conditions = config.getAsJsonObject("conditions");
			return checkConditions(conditions, stack, entity);
		}
		return true;
	}

	private static boolean checkConditions(JsonObject conditions, ItemStack stack, Entity entity) {
		if (conditions.has("state")) {
			String state = conditions.get("state").getAsString();
			if (state.equals("damaged") && !stack.isDamaged())
				return false;
			if (state.equals("undamaged") && stack.isDamaged())
				return false;
		}
		if (conditions.has("nbt")) {
			JsonObject nbtCondition = conditions.getAsJsonObject("nbt");
			String tag = nbtCondition.get("tag").getAsString();
			String value = nbtCondition.get("value").getAsString();
			if (stack.getTag() == null || !stack.getTag().getString(tag).equals(value))
				return false;
		}
		if (conditions.has("epic_battle_mode")) {
			boolean battleMode = conditions.get("epic_battle_mode").getAsBoolean();
			if (battleMode != ModUtils.isEntityInBattleMode(entity))
				return false;
		}
		return true;
	}

	private static void generateExampleJson() {
		try {
			Path configPath = Paths.get(CONFIG_DIR, "example_item_transform.json");
			if (!Files.exists(configPath)) {
				JsonObject exampleJson = new JsonObject();
				exampleJson.addProperty("id", "example_transform");
				exampleJson.addProperty("target", "minecraft:golden_sword");
				exampleJson.addProperty("priority", 5);
				// Adding example conditions
				JsonObject conditions = new JsonObject();
				conditions.addProperty("state", "damaged");
				exampleJson.add("conditions", conditions);
				// Adding transformations
				JsonObject transformations = new JsonObject();
				String[] types = {"third_person_right_hand", "third_person_left_hand", "first_person_right_hand", "first_person_left_hand", "gui", "fixed", "ground"};
				for (String type : types) {
					JsonObject transform = new JsonObject();
					transform.add("rotation", createCompactJsonArray(0.0, 0.0, 0.0));
					transform.add("translation", createCompactJsonArray(0.0, 0.0, 0.0));
					transform.add("scale", createCompactJsonArray(1.0, 1.0, 1.0));
					transformations.add(type, transform);
				}
				exampleJson.add("transformations", transformations);
				// Writing JSON to file
				try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
					GSON.toJson(exampleJson, writer);
				}
				LOGGER.info("Generated example item transform config at '{}'", configPath);
			}
		} catch (IOException e) {
			LOGGER.error("Error generating example JSON file: ", e);
		}
	}

	private static JsonArray createCompactJsonArray(double... values) {
		JsonArray array = new JsonArray();
		for (double value : values) {
			array.add(value);
		}
		return array;
	}
}
