
package net.jaams.jaamscore.manager;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.registries.tags.ITag;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.ModList;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.Difficulty;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.Advancement;

import net.jaams.jaamscore.dyeable.IDyeableItem;

import java.util.UUID;
import java.util.Random;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import java.lang.reflect.Method;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

public class EntityEquipmentManager {
	private static final Logger LOGGER = LogManager.getLogger();

	public static void applyEquipment(LivingEntity entity, Map<String, List<JsonObject>> equipmentConfig) {
		Map<String, EquipmentSlot> slots = Map.of("mainhand", EquipmentSlot.MAINHAND, "offhand", EquipmentSlot.OFFHAND, "head", EquipmentSlot.HEAD, "chest", EquipmentSlot.CHEST, "legs", EquipmentSlot.LEGS, "feet", EquipmentSlot.FEET);
		slots.forEach((key, slot) -> {
			if (equipmentConfig.containsKey(key)) {
				List<JsonObject> itemConfigs = equipmentConfig.get(key);
				itemConfigs.forEach(itemConfig -> {
					try {
						if (!shouldApplyItemConfig(entity, itemConfig)) {
							return;
						}
						if (!itemConfig.has("id") || !itemConfig.has("quantity") || !itemConfig.has("drop_chance")) {
							LOGGER.warn("Missing required fields in equipment configuration: {}", itemConfig);
							return;
						}
						String itemIdentifier = itemConfig.get("id").getAsString();
						double dropChance = itemConfig.get("drop_chance").getAsDouble();
						double chance = itemConfig.has("chance") ? itemConfig.get("chance").getAsDouble() : 1.0;
						if (Math.random() > chance) {
							return;
						}
						int quantity = itemConfig.get("quantity").getAsInt();
						ItemStack equippedItem = createConfiguredItem(itemConfig, itemIdentifier, quantity, slot == EquipmentSlot.HEAD);
						if (!equippedItem.isEmpty()) {
							entity.setItemSlot(slot, equippedItem);
							if (entity instanceof Mob mob) {
								mob.setDropChance(slot, (float) dropChance);
							}
						}
					} catch (Exception e) {
						LOGGER.error("Error applying equipment configuration for entity {}: {}", entity, itemConfig, e);
					}
				});
			}
		});
		if (equipmentConfig.containsKey("loot")) {
			List<JsonObject> lootConfigs = equipmentConfig.get("loot");
			lootConfigs.forEach(lootConfig -> {
				try {
					if (!shouldApplyItemConfig(entity, lootConfig)) {
						return;
					}
					if (!lootConfig.has("id") || !lootConfig.has("quantity")) {
						LOGGER.warn("Missing required fields in loot configuration: {}", lootConfig);
						return;
					}
					String lootItemIdentifier = lootConfig.get("id").getAsString();
					int quantity = lootConfig.get("quantity").getAsInt();
					double chance = lootConfig.has("chance") ? lootConfig.get("chance").getAsDouble() : 1.0;
					if (Math.random() > chance) {
						return;
					}
					ItemStack lootItem = createConfiguredItem(lootConfig, lootItemIdentifier, quantity, false);
					if (!lootItem.isEmpty()) {
						CompoundTag lootTag = createLootTag(lootItem);
						entity.getPersistentData().put("custom_loot", lootTag);
					}
				} catch (Exception e) {
					LOGGER.error("Error applying loot configuration for entity {}: {}", entity, lootConfig, e);
				}
			});
		}
	}

	private static boolean isValidConfig(JsonObject config, String... requiredFields) {
		for (String field : requiredFields) {
			if (!config.has(field)) {
				return false;
			}
		}
		return true;
	}

	private static ItemStack createConfiguredItem(JsonObject itemConfig, String itemIdentifier, int quantity, boolean isPlayerHead) {
		ItemStack itemStack = ItemStack.EMPTY;
		if (isPlayerHead && itemConfig.has("player_names")) {
			JsonArray playerNamesArray = itemConfig.getAsJsonArray("player_names");
			List<String> playerNames = new ArrayList<>();
			for (JsonElement nameElement : playerNamesArray) {
				playerNames.add(nameElement.getAsString());
			}
			itemStack = createPlayerHead(playerNames, quantity);
		} else {
			List<ItemStack> itemStacks = getItemStacks(itemIdentifier);
			if (!itemStacks.isEmpty()) {
				itemStack = itemStacks.get(0).copy();
				itemStack.setCount(quantity);
			}
		}
		if (!itemStack.isEmpty()) {
			applyItemConfig(itemStack, itemConfig);
		}
		return itemStack;
	}

	private static CompoundTag createLootTag(ItemStack itemStack) {
		CompoundTag tag = new CompoundTag();
		tag.put("Item", itemStack.save(new CompoundTag()));
		return tag;
	}

	private static boolean hasEnchantmentInEquipment(Player player, Enchantment enchantment) {
		if (enchantment == null) {
			return false;
		}
		for (ItemStack itemStack : player.getAllSlots()) {
			if (itemStack.getEnchantmentLevel(enchantment) > 0) {
				return true;
			}
		}
		return false;
	}

	private static ItemStack createPlayerHead(List<String> playerNames, int quantity) {
		String playerName = playerNames.get(new Random().nextInt(playerNames.size()));
		ItemStack skull = new ItemStack(Items.PLAYER_HEAD, quantity);
		CompoundTag skullTag = skull.getOrCreateTag();
		skullTag.putString("SkullOwner", playerName);
		return skull;
	}

	private static void applyItemConfig(ItemStack equippedItem, JsonObject itemConfig) {
		if (itemConfig.has("color")) {
			List<Integer> colors = new ArrayList<>();
			JsonElement colorElement = itemConfig.get("color");
			if (colorElement.isJsonArray()) {
				colorElement.getAsJsonArray().forEach(color -> colors.add(parseItemColor(color.getAsString())));
			} else {
				colors.add(parseItemColor(colorElement.getAsString()));
			}
			int selectedColor = colors.get((int) (Math.random() * colors.size()));
			applyDyeColor(equippedItem, selectedColor);
		}
		if (itemConfig.has("item_name")) {
			List<String> names = new ArrayList<>();
			JsonElement nameElement = itemConfig.get("item_name");
			if (nameElement.isJsonArray()) {
				nameElement.getAsJsonArray().forEach(name -> names.add(name.getAsString()));
			} else {
				names.add(nameElement.getAsString());
			}
			String selectedName = names.get((int) (Math.random() * names.size()));
			equippedItem.setHoverName(Component.literal(parseTextWithColorCodes(selectedName)));
		}
		if (itemConfig.has("lore")) {
			JsonArray loreArray = itemConfig.getAsJsonArray("lore");
			ListTag loreTag = new ListTag();
			loreArray.forEach(loreElement -> {
				String formattedLore = parseTextWithColorCodes(loreElement.getAsString());
				loreTag.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(formattedLore))));
			});
			equippedItem.getOrCreateTagElement("display").put("Lore", loreTag);
		}
		if (itemConfig.has("enchantments")) {
			itemConfig.getAsJsonArray("enchantments").forEach(enchantmentElement -> {
				JsonObject enchantmentObject = enchantmentElement.getAsJsonObject();
				String enchantmentId = enchantmentObject.get("id").getAsString();
				int level = enchantmentObject.get("level").getAsInt();
				double probability = enchantmentObject.has("probability") ? enchantmentObject.get("probability").getAsDouble() : 1.0;
				if (Math.random() <= probability) {
					Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(enchantmentId));
					if (enchantment != null) {
						equippedItem.enchant(enchantment, level);
					}
				}
			});
		}
		if (itemConfig.has("attributes")) {
			itemConfig.getAsJsonArray("attributes").forEach(attributeElement -> {
				JsonObject attributeObject = attributeElement.getAsJsonObject();
				String attributeId = attributeObject.get("id").getAsString();
				String operation = attributeObject.get("operation").getAsString();
				double amount = attributeObject.get("amount").getAsDouble();
				Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
				if (attribute != null) {
					AttributeModifier.Operation attrOperation = switch (operation.toLowerCase()) {
						case "add" -> AttributeModifier.Operation.ADDITION;
						case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
						case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
						default -> null;
					};
					if (attrOperation != null) {
						UUID baseUUID = attributeObject.has("uuid") ? UUID.fromString(attributeObject.get("uuid").getAsString()) : UUID.randomUUID();
						attributeObject.getAsJsonArray("slots").forEach(slotElement -> {
							EquipmentSlot slot = EquipmentSlot.byName(slotElement.getAsString());
							AttributeModifier modifier = new AttributeModifier(baseUUID, "Item modifier", amount, attrOperation);
							equippedItem.addAttributeModifier(attribute, modifier, slot);
						});
					}
				}
			});
		}
		if (itemConfig.has("nbt")) {
			JsonObject nbtObject = itemConfig.getAsJsonObject("nbt");
			CompoundTag nbtTag = equippedItem.getOrCreateTag();
			processNbtObject(nbtObject, nbtTag);
			if (nbtObject.has("CustomModelData")) {
				equippedItem.getOrCreateTag().putInt("CustomModelData", nbtObject.get("CustomModelData").getAsInt());
			}
			if (nbtObject.has("StoredEnchantments") && equippedItem.is(Items.ENCHANTED_BOOK)) {
				ListTag enchantmentsTag = new ListTag();
				nbtObject.getAsJsonArray("StoredEnchantments").forEach(enchantmentElement -> {
					JsonObject enchantmentObject = enchantmentElement.getAsJsonObject();
					CompoundTag enchantmentTag = new CompoundTag();
					enchantmentTag.putString("id", enchantmentObject.get("id").getAsString());
					enchantmentTag.putShort("lvl", (short) enchantmentObject.get("lvl").getAsInt());
					enchantmentsTag.add(enchantmentTag);
				});
				nbtTag.put("StoredEnchantments", enchantmentsTag);
			}
			if (nbtObject.has("CustomPotionEffects") && equippedItem.is(Items.POTION)) {
				ListTag effectsTag = new ListTag();
				nbtObject.getAsJsonArray("CustomPotionEffects").forEach(effectElement -> {
					JsonObject effectObject = effectElement.getAsJsonObject();
					CompoundTag effectTag = new CompoundTag();
					effectTag.putInt("Id", effectObject.get("Id").getAsInt());
					effectTag.putInt("Duration", effectObject.get("Duration").getAsInt());
					effectTag.putInt("Amplifier", effectObject.get("Amplifier").getAsInt());
					effectsTag.add(effectTag);
				});
				nbtTag.put("CustomPotionEffects", effectsTag);
			}
			if (nbtObject.has("Potion") && equippedItem.is(Items.TIPPED_ARROW)) {
				nbtTag.putString("Potion", nbtObject.get("Potion").getAsString());
			}
		}
	}

	private static void processNbtObject(JsonObject nbtObject, CompoundTag nbtTag) {
		nbtObject.entrySet().forEach(entry -> {
			String key = entry.getKey();
			JsonElement value = entry.getValue();
			if (value.isJsonPrimitive()) {
				JsonPrimitive primitive = value.getAsJsonPrimitive();
				if (primitive.isString()) {
					nbtTag.putString(key, primitive.getAsString());
				} else if (primitive.isNumber()) {
					if (primitive.getAsString().contains(".")) {
						nbtTag.putDouble(key, primitive.getAsDouble());
					} else {
						nbtTag.putInt(key, primitive.getAsInt());
					}
				} else if (primitive.isBoolean()) {
					nbtTag.putBoolean(key, primitive.getAsBoolean());
				}
			} else if (value.isJsonArray()) {
				JsonArray array = value.getAsJsonArray();
				if (isByteArray(array)) {
					byte[] byteArray = new byte[array.size()];
					for (int i = 0; i < array.size(); i++) {
						byteArray[i] = array.get(i).getAsByte();
					}
					nbtTag.putByteArray(key, byteArray);
				} else if (isIntArray(array)) {
					int[] intArray = new int[array.size()];
					for (int i = 0; i < array.size(); i++) {
						intArray[i] = array.get(i).getAsInt();
					}
					nbtTag.putIntArray(key, intArray);
				} else {
					// Si es un arreglo genérico (tratar como ListTag)
					ListTag listTag = processJsonArray(array);
					nbtTag.put(key, listTag);
				}
			} else if (value.isJsonObject()) {
				CompoundTag nestedTag = new CompoundTag();
				processNbtObject(value.getAsJsonObject(), nestedTag);
				nbtTag.put(key, nestedTag);
			}
		});
	}

	private static ListTag processJsonArray(JsonArray jsonArray) {
		ListTag listTag = new ListTag();
		for (JsonElement element : jsonArray) {
			if (element.isJsonPrimitive()) {
				JsonPrimitive primitive = element.getAsJsonPrimitive();
				if (primitive.isString()) {
					listTag.add(StringTag.valueOf(primitive.getAsString()));
				} else if (primitive.isNumber()) {
					if (primitive.getAsString().contains(".")) {
						listTag.add(DoubleTag.valueOf(primitive.getAsDouble()));
					} else {
						listTag.add(IntTag.valueOf(primitive.getAsInt()));
					}
				} else if (primitive.isBoolean()) {
					listTag.add(ByteTag.valueOf((byte) (primitive.getAsBoolean() ? 1 : 0)));
				}
			} else if (element.isJsonObject()) {
				CompoundTag nestedTag = new CompoundTag();
				processNbtObject(element.getAsJsonObject(), nestedTag);
				listTag.add(nestedTag);
			}
		}
		return listTag;
	}

	private static boolean isByteArray(JsonArray array) {
		for (JsonElement element : array) {
			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
				return false;
			}
		}
		return true;
	}

	private static boolean isIntArray(JsonArray array) {
		for (JsonElement element : array) {
			if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
				return false;
			}
		}
		return true;
	}

	private static int parseColor(String color) {
		return switch (color.toLowerCase()) {
			case "red" -> 0xFF0000;
			case "green" -> 0x00FF00;
			case "blue" -> 0x0000FF;
			case "gold" -> 0xFFD700;
			case "dark_gray" -> 0xA9A9A9;
			default -> 0xFFFFFF;
		};
	}

	private static String parseTextWithColorCodes(String text) {
		return text.replace("&", "§");
	}

	private static List<ItemStack> getItemStacks(String identifier) {
		List<ItemStack> itemStacks = new ArrayList<>();
		if (identifier.startsWith("tag:")) {
			String tagName = identifier.substring(4);
			ResourceLocation tagLocation = new ResourceLocation(tagName);
			TagKey<Item> tagKey = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagLocation);
			ITag<Item> tag = ForgeRegistries.ITEMS.tags().getTag(tagKey);
			if (tag != null) {
				tag.forEach(item -> itemStacks.add(new ItemStack(item)));
				if (!itemStacks.isEmpty()) {
					Collections.shuffle(itemStacks);
					return itemStacks.subList(0, 1);
				}
			}
		} else if (identifier.contains(",")) {
			String[] items = identifier.split(",");
			for (String itemId : items) {
				Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId.trim()));
				if (item != null) {
					itemStacks.add(new ItemStack(item));
				}
			}
			if (!itemStacks.isEmpty()) {
				Collections.shuffle(itemStacks);
				return itemStacks.subList(0, 1);
			}
		} else {
			Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(identifier));
			if (item != null) {
				itemStacks.add(new ItemStack(item));
			}
		}
		return itemStacks;
	}

	private static int parseItemColor(String colorName) {
		Random random = new Random();
		if (colorName.equalsIgnoreCase("random")) {
			return random.nextInt(0xFFFFFF + 1);
		}
		switch (colorName.toLowerCase()) {
			case "red" :
				return 0xFF0000;
			case "green" :
				return 0x00FF00;
			case "blue" :
				return 0x0000FF;
			case "black" :
				return 0x000000;
			case "white" :
				return 0xFFFFFF;
			case "yellow" :
				return 0xFFFF00;
			case "orange" :
				return 0xFFA500;
			case "purple" :
				return 0x800080;
			case "pink" :
				return 0xFFC0CB;
			case "brown" :
				return 0x8B4513;
			case "cyan" :
				return 0x00FFFF;
			case "magenta" :
				return 0xFF00FF;
			case "lime" :
				return 0x32CD32;
			case "gray" :
				return 0x808080;
			case "light_gray" :
				return 0xD3D3D3;
			case "light_blue" :
				return 0xADD8E6;
			case "dark_blue" :
				return 0x00008B;
			case "dark_green" :
				return 0x006400;
			case "dark_red" :
				return 0x8B0000;
			case "gold" :
				return 0xFFD700;
			case "silver" :
				return 0xC0C0C0;
			case "navy" :
				return 0x000080;
			case "teal" :
				return 0x008080;
			case "violet" :
				return 0xEE82EE;
			case "beige" :
				return 0xF5F5DC;
			default :
				return 0xFFFFFF;
		}
	}

	private static void applyDyeColor(ItemStack itemStack, int color) {
		Item item = itemStack.getItem();
		if (item instanceof IDyeableItem dyeableItem) {
			dyeableItem.setColor(itemStack, color);
		} else {
			try {
				Method setColorMethod = item.getClass().getMethod("setColor", ItemStack.class, int.class);
				if (setColorMethod != null) {
					setColorMethod.invoke(item, itemStack, color);
					return;
				}
			} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | java.lang.reflect.InvocationTargetException e) {
				ResourceLocation itemRegistryName = ForgeRegistries.ITEMS.getKey(item);
				if (itemRegistryName != null) {
				}
			}
			itemStack.getOrCreateTag().putInt("Color", color);
		}
	}

	public static void applyAttributes(LivingEntity entity, Map<String, JsonObject> attributesConfig) {
		attributesConfig.forEach((attributeId, config) -> {
			Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
			if (attribute != null) {
				double chance = config.has("chance") ? config.get("chance").getAsDouble() : 1.0;
				if (Math.random() > chance) {
					return;
				}
				double value = config.has("value") ? config.get("value").getAsDouble() : 0.0;
				String modifierName = config.has("name") ? config.get("name").getAsString() : "Unknown";
				String operationStr = config.has("operation") ? config.get("operation").getAsString() : "add";
				String uuidStr = config.has("uuid") ? config.get("uuid").getAsString() : "random";
				UUID uuid = "random".equalsIgnoreCase(uuidStr) ? UUID.randomUUID() : UUID.fromString(uuidStr);
				AttributeModifier.Operation operation;
				switch (operationStr.toLowerCase()) {
					case "add" :
						operation = AttributeModifier.Operation.ADDITION;
						break;
					case "multiply_base" :
						operation = AttributeModifier.Operation.MULTIPLY_BASE;
						break;
					case "multiply_total" :
						operation = AttributeModifier.Operation.MULTIPLY_TOTAL;
						break;
					default :
						operation = AttributeModifier.Operation.ADDITION;
				}
				AttributeModifier modifier = new AttributeModifier(uuid, modifierName, value, operation);
				AttributeInstance instance = entity.getAttributes().getInstance(attribute);
				if (instance != null) {
					instance.removeModifier(modifier.getId());
					instance.addPermanentModifier(modifier);
				}
				if (attribute == Attributes.MAX_HEALTH) {
					entity.setHealth(entity.getMaxHealth());
				}
			}
		});
	}

	public static void applyPotionEffects(LivingEntity entity, Map<String, JsonObject> potionsConfig) {
		potionsConfig.forEach((potionId, config) -> {
			if (config instanceof JsonObject) {
				JsonObject potionAttributes = (JsonObject) config;
				MobEffect potion = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(potionId));
				if (potion != null) {
					double chance = potionAttributes.has("chance") ? potionAttributes.get("chance").getAsDouble() : 1.0;
					if (Math.random() > chance) {
						return;
					}
					int level = potionAttributes.has("level") ? potionAttributes.get("level").getAsInt() : 1;
					int duration = potionAttributes.has("duration") ? potionAttributes.get("duration").getAsInt() : 600;
					boolean particles = potionAttributes.has("particles") ? potionAttributes.get("particles").getAsBoolean() : true;
					boolean icons = potionAttributes.has("icons") ? potionAttributes.get("icons").getAsBoolean() : true;
					MobEffectInstance effectInstance = new MobEffectInstance(potion, duration, level - 1, false, particles, icons);
					entity.addEffect(effectInstance);
				}
			}
		});
	}

	private static boolean shouldApplyItemConfig(LivingEntity entity, JsonObject itemConfig) {
		if (itemConfig.has("mod_id")) {
			String requiredModId = itemConfig.get("mod_id").getAsString();
			if (!ModList.get().isLoaded(requiredModId)) {
				return false;
			}
		}
		if (itemConfig.has("biome")) {
			JsonArray biomeArray = itemConfig.getAsJsonArray("biome");
			boolean biomeMatches = false;
			Holder<Biome> biomeHolder = entity.level().getBiome(entity.blockPosition());
			ResourceLocation currentBiomeKey = entity.level().registryAccess().registryOrThrow(Registries.BIOME).getKey(biomeHolder.value());
			for (JsonElement biomeElement : biomeArray) {
				String biomeId = biomeElement.getAsString();
				ResourceLocation biomeResource = new ResourceLocation(biomeId);
				if (currentBiomeKey != null && currentBiomeKey.equals(biomeResource)) {
					biomeMatches = true;
					break;
				}
			}
			if (!biomeMatches) {
				return false;
			}
		}
		if (itemConfig.has("structure")) {
			JsonArray structureArray = itemConfig.getAsJsonArray("structure");
			boolean structureMatches = false;
			BlockPos entityPos = entity.blockPosition();
			Level level = entity.level();
			if (level instanceof ServerLevel) {
				ServerLevel serverLevel = (ServerLevel) level;
				List<ResourceLocation> requiredStructures = new ArrayList<>();
				for (JsonElement structureElement : structureArray) {
					String structureId = structureElement.getAsString();
					requiredStructures.add(new ResourceLocation(structureId));
				}
				boolean structureMatch = requiredStructures.stream().anyMatch(structureLocation -> {
					TagKey<Structure> structureTagKey = TagKey.create(Registries.STRUCTURE, structureLocation);
					BlockPos nearestStructurePos = serverLevel.findNearestMapStructure(structureTagKey, entityPos, 50, false);
					return nearestStructurePos != null;
				});
				if (!structureMatch) {
					return false;
				}
			}
		}
		if (itemConfig.has("difficulty")) {
			JsonArray difficultyArray = itemConfig.getAsJsonArray("difficulty");
			boolean difficultyMatches = false;
			Difficulty currentDifficulty = entity.level().getDifficulty();
			for (JsonElement difficultyElement : difficultyArray) {
				String difficulty = difficultyElement.getAsString();
				if (currentDifficulty.getKey().equals(difficulty)) {
					difficultyMatches = true;
					break;
				}
			}
			if (!difficultyMatches) {
				return false;
			}
		}
		if (itemConfig.has("is_baby")) {
			boolean shouldBeBaby = itemConfig.get("is_baby").getAsBoolean();
			if (entity instanceof AgeableMob ageable && ageable.isBaby() != shouldBeBaby) {
				return false;
			}
		}
		if (itemConfig.has("is_riding")) {
			boolean shouldBeRiding = itemConfig.get("is_riding").getAsBoolean();
			if (entity.isPassenger() != shouldBeRiding) {
				return false;
			}
		}
		if (itemConfig.has("nearby_player_advancements")) {
			JsonArray advancementsArray = itemConfig.getAsJsonArray("nearby_player_advancements");
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer instanceof ServerPlayer serverPlayer) {
				for (JsonElement advancementElement : advancementsArray) {
					String advancementId = advancementElement.getAsString();
					Advancement advancement = serverPlayer.getServer().getAdvancements().getAdvancement(new ResourceLocation(advancementId));
					if (advancement != null && serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone()) {
						return true;
					}
				}
				return false;
			}
		}
		if (itemConfig.has("player_advancements")) {
			JsonArray advancementsArray = itemConfig.getAsJsonArray("player_advancements");
			if (entity instanceof ServerPlayer serverPlayer) {
				for (JsonElement advancementElement : advancementsArray) {
					String advancementId = advancementElement.getAsString();
					Advancement advancement = serverPlayer.getServer().getAdvancements().getAdvancement(new ResourceLocation(advancementId));
					if (advancement != null && serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone()) {
						return true;
					}
				}
				return false;
			}
		}
		if (itemConfig.has("nearby_player_health")) {
			JsonObject healthConfig = itemConfig.getAsJsonObject("nearby_player_health");
			double minHealth = healthConfig.get("min").getAsDouble();
			double maxHealth = healthConfig.get("max").getAsDouble();
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				double playerHealth = nearestPlayer.getHealth();
				if (playerHealth < minHealth || playerHealth > maxHealth)
					return false;
			}
		}
		if (itemConfig.has("nearby_player_armor_value")) {
			JsonObject armorConfig = itemConfig.getAsJsonObject("nearby_player_armor_value");
			int minArmor = armorConfig.get("min").getAsInt();
			int maxArmor = armorConfig.get("max").getAsInt();
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				int armorValue = nearestPlayer.getArmorValue();
				if (armorValue < minArmor || armorValue > maxArmor) {
					return false;
				}
			}
		}
		if (itemConfig.has("nearby_player_hunger")) {
			JsonObject hungerConfig = itemConfig.getAsJsonObject("nearby_player_hunger");
			int minHunger = hungerConfig.get("min").getAsInt();
			int maxHunger = hungerConfig.get("max").getAsInt();
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				int playerHunger = nearestPlayer.getFoodData().getFoodLevel();
				if (playerHunger < minHunger || playerHunger > maxHunger) {
					return false;
				}
			}
		}
		if (itemConfig.has("nearby_player_exp")) {
			JsonObject expConfig = itemConfig.getAsJsonObject("nearby_player_exp");
			int minExp = expConfig.get("min").getAsInt();
			int maxExp = expConfig.get("max").getAsInt();
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				int playerExperience = nearestPlayer.totalExperience;
				if (playerExperience < minExp || playerExperience > maxExp)
					return false;
			}
		}
		if (itemConfig.has("nearby_player_states")) {
			JsonArray statesArray = itemConfig.getAsJsonArray("nearby_player_states");
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				boolean allStatesMatched = true;
				for (JsonElement stateElement : statesArray) {
					String requiredState = stateElement.getAsString();
					boolean isMatchingState = false;
					switch (requiredState) {
						case "riding" :
							isMatchingState = nearestPlayer.isPassenger();
							break;
						case "sneaking" :
							isMatchingState = nearestPlayer.isCrouching();
							break;
						case "sprinting" :
							isMatchingState = nearestPlayer.isSprinting();
							break;
						case "swimming" :
							isMatchingState = nearestPlayer.isSwimming();
							break;
						case "gliding" :
							isMatchingState = nearestPlayer.isFallFlying();
							break;
						case "sleeping" :
							isMatchingState = nearestPlayer.isSleeping();
							break;
						case "burning" :
							isMatchingState = nearestPlayer.isOnFire();
							break;
						case "wet" :
							isMatchingState = nearestPlayer.isInWater();
							break;
						default :
							allStatesMatched = false;
					}
					if (!isMatchingState) {
						allStatesMatched = false;
						break;
					}
				}
				if (!allStatesMatched) {
					return false;
				}
			}
		}
		if (itemConfig.has("nearby_player_enchantments")) {
			JsonArray enchantmentsArray = itemConfig.getAsJsonArray("nearby_player_enchantments");
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				boolean allEnchantmentsMatched = true;
				for (JsonElement enchantmentElement : enchantmentsArray) {
					String enchantmentId = enchantmentElement.getAsString();
					Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(enchantmentId));
					boolean hasEnchantment = hasEnchantmentInEquipment(nearestPlayer, enchantment);
					if (!hasEnchantment) {
						allEnchantmentsMatched = false;
						break;
					}
				}
				if (!allEnchantmentsMatched) {
					return false;
				}
			}
		}
		if (itemConfig.has("time")) {
			JsonObject timeConfig = itemConfig.getAsJsonObject("time");
			long currentTime = entity.level().getDayTime() % 24000;
			long startTime = timeConfig.get("start").getAsLong();
			long endTime = timeConfig.get("end").getAsLong();
			if (currentTime < startTime || currentTime > endTime) {
				return false;
			}
		}
		if (itemConfig.has("height")) {
			JsonObject heightConfig = itemConfig.getAsJsonObject("height");
			int minHeight = heightConfig.get("min").getAsInt();
			int maxHeight = heightConfig.get("max").getAsInt();
			int currentHeight = entity.blockPosition().getY();
			if (currentHeight < minHeight || currentHeight > maxHeight) {
				return false;
			}
		}
		if (itemConfig.has("light")) {
			JsonObject lightConfig = itemConfig.getAsJsonObject("light");
			int minLight = lightConfig.get("min").getAsInt();
			int maxLight = lightConfig.get("max").getAsInt();
			int currentLight = entity.level().getBrightness(LightLayer.BLOCK, entity.blockPosition());
			if (currentLight < minLight || currentLight > maxLight) {
				return false;
			}
		}
		if (itemConfig.has("entity_type")) {
			String entityType = itemConfig.get("entity_type").getAsString();
			String currentEntityType = EntityType.getKey(entity.getType()).toString();
			if (!currentEntityType.equals(entityType)) {
				return false;
			}
		}
		if (itemConfig.has("nearby_player_distance_traveled")) {
			JsonObject distTraveledObj = itemConfig.getAsJsonObject("nearby_player_distance_traveled");
			float minDist = distTraveledObj.has("min") ? distTraveledObj.get("min").getAsFloat() : 0.0F;
			float maxDist = distTraveledObj.has("max") ? distTraveledObj.get("max").getAsFloat() : Float.MAX_VALUE;
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50.0D);
			if (nearestPlayer != null) {
				float distanceWalked = nearestPlayer.walkDist;
				if (distanceWalked < minDist || distanceWalked > maxDist) {
					return false;
				}
			}
		}
		if (itemConfig.has("distance_from_spawn")) {
			JsonObject distFromSpawnObj = itemConfig.getAsJsonObject("distance_from_spawn");
			float minDist = distFromSpawnObj.has("min") ? distFromSpawnObj.get("min").getAsFloat() : 0.0F;
			float maxDist = distFromSpawnObj.has("max") ? distFromSpawnObj.get("max").getAsFloat() : Float.MAX_VALUE;
			BlockPos spawnPos = entity.level().getSharedSpawnPos();
			double distanceFromSpawn = entity.blockPosition().distSqr(spawnPos);
			if (distanceFromSpawn < (minDist * minDist) || distanceFromSpawn > (maxDist * maxDist)) {
				return false;
			}
		}
		if (itemConfig.has("nearby_blocks")) {
			JsonArray blockArray = itemConfig.getAsJsonArray("nearby_blocks");
			boolean blockMatches = false;
			for (JsonElement blockElement : blockArray) {
				String blockId = blockElement.getAsString();
				ResourceLocation blockResource = new ResourceLocation(blockId);
				Block block = ForgeRegistries.BLOCKS.getValue(blockResource);
				if (block == null) {
					continue;
				}
				BlockPos entityPos = entity.blockPosition();
				for (BlockPos nearbyPos : BlockPos.betweenClosed(entityPos.offset(-5, -5, -5), entityPos.offset(5, 5, 5))) {
					BlockState nearbyBlock = entity.level().getBlockState(nearbyPos);
					if (nearbyBlock.is(block)) {
						blockMatches = true;
						break;
					}
				}
				if (blockMatches)
					break;
			}
			if (!blockMatches) {
				return false;
			}
		}
		if (itemConfig.has("nearby_mobs")) {
			JsonArray mobArray = itemConfig.getAsJsonArray("nearby_mobs");
			boolean mobMatches = false;
			for (JsonElement mobElement : mobArray) {
				String mobId = mobElement.getAsString();
				ResourceLocation mobResource = new ResourceLocation(mobId);
				List<Mob> nearbyMobs = entity.level().getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(50));
				for (Mob mob : nearbyMobs) {
					if (EntityType.getKey(mob.getType()).equals(mobResource)) {
						mobMatches = true;
						break;
					}
				}
				if (mobMatches)
					break;
			}
			if (!mobMatches) {
				return false;
			}
		}
		if (itemConfig.has("nearby_player_effects")) {
			JsonArray effectArray = itemConfig.getAsJsonArray("nearby_player_effects");
			boolean effectMatches = false;
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				for (JsonElement effectElement : effectArray) {
					String effectId = effectElement.getAsString();
					MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effectId));
					if (nearestPlayer.hasEffect(effect)) {
						effectMatches = true;
						break;
					}
				}
			}
			if (!effectMatches) {
				return false;
			}
		}
		if (itemConfig.has("entity_effects")) {
			JsonArray effectArray = itemConfig.getAsJsonArray("entity_effects");
			boolean effectMatches = false;
			if (entity instanceof LivingEntity) {
				LivingEntity livingEntity = (LivingEntity) entity;
				for (JsonElement effectElement : effectArray) {
					String effectId = effectElement.getAsString();
					MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effectId));
					if (livingEntity.hasEffect(effect)) {
						effectMatches = true;
						break;
					}
				}
			}
			if (!effectMatches) {
				return false;
			}
		}
		if (itemConfig.has("nearby_player_items")) {
			JsonArray itemArray = itemConfig.getAsJsonArray("nearby_player_items");
			boolean itemMatches = false;
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				for (JsonElement itemElement : itemArray) {
					String itemId = itemElement.getAsString();
					Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
					for (ItemStack playerStack : nearestPlayer.getInventory().items) {
						if (playerStack.getItem() == item) {
							itemMatches = true;
							break;
						}
					}
					if (itemMatches) {
						break;
					}
				}
			}
			if (!itemMatches) {
				return false;
			}
		}
		if (itemConfig.has("moon_phase")) {
			JsonArray moonPhaseArray = itemConfig.getAsJsonArray("moon_phase");
			int currentMoonPhase = (int) (entity.level().getDayTime() / 24000L % 8L);
			boolean moonPhaseMatches = false;
			for (JsonElement moonPhaseElement : moonPhaseArray) {
				int requiredMoonPhase = moonPhaseElement.getAsInt();
				if (currentMoonPhase == requiredMoonPhase) {
					moonPhaseMatches = true;
					break;
				}
			}
			if (!moonPhaseMatches) {
				return false;
			}
		}
		if (itemConfig.has("dimension")) {
			String dimensionId = itemConfig.get("dimension").getAsString();
			ResourceKey<Level> currentDimension = entity.level().dimension();
			if (!currentDimension.location().toString().equals(dimensionId)) {
				return false;
			}
		}
		if (itemConfig.has("weather")) {
			JsonArray weatherArray = itemConfig.getAsJsonArray("weather");
			boolean weatherMatches = false;
			for (JsonElement weatherElement : weatherArray) {
				String weatherType = weatherElement.getAsString().trim();
				if ((weatherType.equals("rain") && entity.level().isRaining()) || (weatherType.equals("thunder") && entity.level().isThundering()) || (weatherType.equals("clear") && !entity.level().isRaining())) {
					weatherMatches = true;
					break;
				}
			}
			if (!weatherMatches) {
				return false;
			}
		}
		return true;
	}

	private static BossBarOverlay parseBossBarOverlay(String overlay) {
		return switch (overlay.toLowerCase()) {
			case "progress" -> BossBarOverlay.PROGRESS;
			case "notched_6" -> BossBarOverlay.NOTCHED_6;
			case "notched_10" -> BossBarOverlay.NOTCHED_10;
			case "notched_12" -> BossBarOverlay.NOTCHED_12;
			case "notched_20" -> BossBarOverlay.NOTCHED_20;
			default -> {
				yield BossBarOverlay.PROGRESS;
			}
		};
	}

	private static BossBarColor parseBossBarColor(String value) {
		return switch (value.toLowerCase()) {
			case "red" -> BossBarColor.RED;
			case "blue" -> BossBarColor.BLUE;
			case "green" -> BossBarColor.GREEN;
			case "yellow" -> BossBarColor.YELLOW;
			case "purple" -> BossBarColor.PURPLE;
			case "white" -> BossBarColor.WHITE;
			default -> BossBarColor.WHITE;
		};
	}
}
