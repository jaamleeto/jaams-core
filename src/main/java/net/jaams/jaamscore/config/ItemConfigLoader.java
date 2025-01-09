package net.jaams.jaamscore.config;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import net.minecraftforge.fml.ModList;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.WatchService;
import java.nio.file.WatchKey;
import java.nio.file.WatchEvent;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.StandardOpenOption;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.FileSystems;

import java.io.IOException;

import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;

public class ItemConfigLoader {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CONFIG_DIR = "config/jaams/core_settings/core_items/";
	public static final Logger LOGGER = LoggerFactory.getLogger(ItemConfigLoader.class);
	private static final Map<String, List<JsonObject>> itemConfigs = new ConcurrentHashMap<>();

	public static void init() {
		loadAllItemConfigs();
		startWatchingConfigs();
	}

	private static void loadAllItemConfigs() {
		itemConfigs.clear();
		try {
			Path configPath = Paths.get(CONFIG_DIR);
			if (!Files.exists(configPath)) {
				Files.createDirectories(configPath);
			}
			Files.walkFileTree(configPath, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toString().endsWith(".json")) {
						try {
							loadItemConfig(file);
						} catch (Exception e) {
							LOGGER.error("Failed to load config file '{}': {}", file, e.getMessage(), e);
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});
			if (itemConfigs.isEmpty()) {
				LOGGER.warn("No item configs found. Generating default example config.");
				generateExampleJson();
				loadAllItemConfigs();
			}
		} catch (IOException e) {
			LOGGER.error("Error loading item configs: ", e);
		}
	}

	private static void loadItemConfig(Path path) {
		try {
			String content = Files.readString(path);
			JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
			if (!validateJsonConfig(jsonObject)) {
				LOGGER.warn("Skipping invalid item config: {}", path);
				return;
			}
			JsonElement targetElement = jsonObject.get("target");
			if (targetElement != null) {
				if (targetElement.isJsonArray()) {
					for (JsonElement target : targetElement.getAsJsonArray()) {
						updateConfig(target.getAsString(), jsonObject);
					}
				} else if (targetElement.isJsonPrimitive()) {
					updateConfig(targetElement.getAsString(), jsonObject);
				}
				LOGGER.info("Loaded or updated item config from: {}", path);
			} else {
				LOGGER.warn("Missing 'target' field in config: {}", path);
			}
		} catch (JsonSyntaxException e) {
			LOGGER.error("Invalid JSON syntax in config file '{}': {}", path, e.getMessage());
		} catch (IOException e) {
			LOGGER.error("Error reading item config from file '{}': {}", path, e.getMessage());
		}
	}

	private static void updateConfig(String key, JsonObject config) {
		itemConfigs.compute(key, (k, v) -> {
			if (v == null) {
				v = new ArrayList<>();
			} else {
				v.clear(); // Clear existing configs to reflect the latest changes
			}
			v.add(config);
			return v;
		});
	}

	private static boolean validateJsonConfig(JsonObject jsonObject) {
		return jsonObject.has("target");
	}

	public static JsonObject getItemConfig(ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (registryName != null) {
			String itemKey = registryName.toString();
			List<JsonObject> configs = itemConfigs.get(itemKey);
			if (configs != null && !configs.isEmpty()) {
				return configs.get(0); // Return the first matched config; replace with selection logic if needed
			}
		}
		for (Map.Entry<String, List<JsonObject>> entry : itemConfigs.entrySet()) {
			String key = entry.getKey();
			if (key.startsWith("#")) {
				String tagName = key.substring(1);
				TagKey<Item> tag = TagKey.create(BuiltInRegistries.ITEM.key(), new ResourceLocation(tagName));
				if (stack.is(tag)) {
					return entry.getValue().get(0); // Return the first matched config
				}
			}
		}
		return null;
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
					LOGGER.info("Watching item config directory for changes...");
					while (true) {
						WatchKey key = watchService.take();
						for (WatchEvent<?> event : key.pollEvents()) {
							try {
								Path filePath = configPath.resolve((Path) event.context());
								if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE || event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
									loadItemConfig(filePath);
								} else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
									removeConfig(filePath);
								}
							} catch (Exception e) {
								LOGGER.error("Error handling file system event: {}", e.getMessage(), e);
							}
						}
						key.reset();
					}
				} catch (IOException | InterruptedException e) {
					LOGGER.error("Error monitoring item config directory, restarting watcher: ", e);
				}
			}
		}, "Config-Watcher-Thread");
		watcherThread.setDaemon(true);
		watcherThread.start();
	}

	private static void removeConfig(Path filePath) {
		try {
			String content = Files.readString(filePath);
			JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
			JsonElement targetElement = jsonObject.get("target");
			if (targetElement != null) {
				if (targetElement.isJsonArray()) {
					for (JsonElement target : targetElement.getAsJsonArray()) {
						itemConfigs.remove(target.getAsString());
					}
				} else if (targetElement.isJsonPrimitive()) {
					itemConfigs.remove(targetElement.getAsString());
				}
				LOGGER.info("Removed config associated with: {}", filePath);
			} else {
				LOGGER.warn("Missing 'target' field while removing config from: {}", filePath);
			}
		} catch (IOException | JsonSyntaxException e) {
			LOGGER.error("Error removing config from file '{}': {}", filePath, e.getMessage());
		}
	}

	private static void generateExampleJson() {
		try {
			JsonObject exampleConfig = new JsonObject();
			exampleConfig.addProperty("target", "example_item");
			exampleConfig.addProperty("example_property", "example_value");
			Path examplePath = Paths.get(CONFIG_DIR, "example_item.json");
			Files.writeString(examplePath, GSON.toJson(exampleConfig), StandardOpenOption.CREATE);
			LOGGER.info("Generated example config at: {}", examplePath);
		} catch (IOException e) {
			LOGGER.error("Failed to generate example config: ", e);
		}
	}

	public static boolean evaluateCondition(JsonObject option, ItemStack stack) {
		if (!option.has("condition")) {
			return true;
		}
		JsonObject condition = option.getAsJsonObject("condition");
		// Mod loaded
		if (condition.has("mod_id")) {
			String modId = condition.get("mod_id").getAsString();
			boolean inverted = modId.startsWith("!");
			modId = inverted ? modId.substring(1) : modId;
			boolean modLoaded = ModList.get().isLoaded(modId);
			if (inverted ? modLoaded : !modLoaded) {
				return false;
			}
		}
		// Is damaged
		if (condition.has("is_damaged")) {
			boolean isDamaged = stack.isDamaged();
			boolean expected = condition.get("is_damaged").getAsBoolean();
			if (expected != isDamaged) {
				return false;
			}
		}
		// Is enchanted
		if (condition.has("is_enchanted")) {
			boolean isEnchanted = stack.isEnchanted();
			boolean expected = condition.get("is_enchanted").getAsBoolean();
			if (expected != isEnchanted) {
				return false;
			}
		}
		// Is stackable
		if (condition.has("is_stackable")) {
			boolean isStackable = stack.getMaxStackSize() > 1;
			boolean expected = condition.get("is_stackable").getAsBoolean();
			if (expected != isStackable) {
				return false;
			}
		}
		// Is item
		if (condition.has("is_item")) {
			String itemId = condition.get("is_item").getAsString();
			boolean inverted = itemId.startsWith("!");
			itemId = inverted ? itemId.substring(1) : itemId;
			boolean matches = stack.getItem() == BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
			if (inverted ? matches : !matches) {
				return false;
			}
		}
		// Is tagged
		if (condition.has("is_tagged")) {
			String tag = condition.get("is_tagged").getAsString();
			boolean inverted = tag.startsWith("!");
			tag = inverted ? tag.substring(1) : tag;
			if (!tag.startsWith("#")) {
				LOGGER.error("Invalid tag format: '" + tag + "'. Tags must start with '#'!");
				return false;
			}
			tag = tag.substring(1); // Eliminar el prefijo '#' para obtener el nombre real del tag
			// Crear el TagKey usando Registries.ITEM
			TagKey<Item> tagKey = TagKey.create(Registries.ITEM, new ResourceLocation(tag));
			boolean hasTag = stack.is(tagKey); // Verificar si el stack pertenece al tag
			if (inverted ? hasTag : !hasTag) {
				return false;
			}
		}
		// Is rarity
		if (condition.has("is_rarity")) {
			String rarity = condition.get("is_rarity").getAsString().toUpperCase();
			boolean inverted = rarity.startsWith("!");
			rarity = inverted ? rarity.substring(1) : rarity;
			try {
				boolean matches = stack.getRarity() == Rarity.valueOf(rarity);
				if (inverted ? matches : !matches) {
					return false;
				}
			} catch (IllegalArgumentException e) {
				LOGGER.error("Invalid rarity value: " + rarity);
				return false;
			}
		}
		// Has NBT
		if (condition.has("has_nbt")) {
			boolean hasNbt = stack.hasTag();
			boolean expected = condition.get("has_nbt").getAsBoolean();
			if (expected != hasNbt) {
				return false;
			}
		}
		// Has specific NBT tags
		if (condition.has("has_int_tag")) {
			String key = condition.get("has_int_tag").getAsString();
			boolean inverted = key.startsWith("!");
			key = inverted ? key.substring(1) : key;
			boolean hasIntTag = stack.getTag() != null && stack.getTag().contains(key, 3); // 3 = int NBT
			if (inverted ? hasIntTag : !hasIntTag) {
				return false;
			}
		}
		if (condition.has("has_boolean_tag")) {
			String key = condition.get("has_boolean_tag").getAsString();
			boolean inverted = key.startsWith("!");
			key = inverted ? key.substring(1) : key;
			boolean hasBooleanTag = stack.getTag() != null && stack.getTag().contains(key, 1); // 1 = byte NBT (boolean)
			if (inverted ? hasBooleanTag : !hasBooleanTag) {
				return false;
			}
		}
		if (condition.has("has_short_nbt")) {
			String key = condition.get("has_short_nbt").getAsString();
			boolean inverted = key.startsWith("!");
			key = inverted ? key.substring(1) : key;
			boolean hasShortTag = stack.getTag() != null && stack.getTag().contains(key, 2); // 2 = short NBT
			if (inverted ? hasShortTag : !hasShortTag) {
				return false;
			}
		}
		if (condition.has("has_long_nbt")) {
			String key = condition.get("has_long_nbt").getAsString();
			boolean inverted = key.startsWith("!");
			key = inverted ? key.substring(1) : key;
			boolean hasLongTag = stack.getTag() != null && stack.getTag().contains(key, 4); // 4 = long NBT
			if (inverted ? hasLongTag : !hasLongTag) {
				return false;
			}
		}
		if (condition.has("has_string_nbt")) {
			String key = condition.get("has_string_nbt").getAsString();
			boolean inverted = key.startsWith("!");
			key = inverted ? key.substring(1) : key;
			boolean hasStringTag = stack.getTag() != null && stack.getTag().contains(key, 8); // 8 = string NBT
			if (inverted ? hasStringTag : !hasStringTag) {
				return false;
			}
		}
		return true;
	}
}
