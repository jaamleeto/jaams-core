
package net.jaams.jaamscore.core;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.Difficulty;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.Advancement;

import net.jaams.jaamscore.util.EntityConfigLoader;
import net.jaams.jaamscore.packets.ScaleSyncPacket;
import net.jaams.jaamscore.manager.ScaleManager;
import net.jaams.jaamscore.manager.EntityEquipmentManager;
import net.jaams.jaamscore.handler.EntityBehaviorsHandler;
import net.jaams.jaamscore.handler.BossBarHandler;
import net.jaams.jaamscore.JaamsCoreMod;

import java.util.Random;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

@Mod.EventBusSubscriber
public class EntityCore {
	private static final Logger LOGGER = LogManager.getLogger();

	@SubscribeEvent
	public static void onInvulnerableEntityHurt(LivingHurtEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity.isInvulnerable()) {
			entity.hurtTime = 0;
			entity.hurtDuration = 0;
			entity.invulnerableTime = 0;
			entity.clearFire();
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (ScaleManager.hasCustomScale(entity.getUUID())) {
			JaamsCoreMod.queueServerWork(20, () -> {
				ScaleManager.removeScale(entity.getUUID());
			});
		}
	}

	@SubscribeEvent
	public static void onLivingDrops(LivingDropsEvent event) {
		if (!(event.getEntity().level() instanceof ServerLevel serverLevel))
			return;
		LivingEntity entity = event.getEntity();
		if (!entity.getPersistentData().contains("custom_loot"))
			return;
		ListTag lootTagList = entity.getPersistentData().getList("custom_loot", 10);
		for (int i = 0; i < lootTagList.size(); i++) {
			CompoundTag lootTag = lootTagList.getCompound(i);
			ItemStack lootStack = ItemStack.of(lootTag.getCompound("Item"));
			ItemEntity itemEntity = new ItemEntity(serverLevel, entity.getX(), entity.getY(), entity.getZ(), lootStack);
			event.getDrops().add(itemEntity);
		}
		entity.getPersistentData().remove("custom_loot");
	}

	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
		if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) event.getEntity();
			CompoundTag data = entity.getPersistentData();
			String processedKey = "hasBeenModified";
			String lastConfigHashKey = "lastAppliedConfigHash";
			if (data.contains("entityScale")) {
				float storedScale = data.getFloat("entityScale");
				ScaleManager.setScale(entity.getUUID(), storedScale);
				ScaleSyncPacket.sendToAllTracking(entity, storedScale);
			}
			try {
				String entityType = EntityType.getKey(entity.getType()).toString();
				List<JsonObject> matchedConfigs = new ArrayList<>();
				EntityConfigLoader.reloadConfigs();
				for (Map.Entry<String, List<JsonObject>> entry : EntityConfigLoader.getEntityConfigs().entrySet()) {
					for (JsonObject config : entry.getValue()) {
						JsonObject entryConditions = config.getAsJsonObject("entry_conditions");
						if (!checkEntryConditions(entity, entryConditions)) {
							continue;
						}
						JsonObject entrySettings = config.getAsJsonObject("entry_settings");
						float chance = 1.0f;
						if (entrySettings != null && entrySettings.has("chance")) {
							chance = entrySettings.get("chance").getAsFloat();
						}
						if (chance < 1.0f && Math.random() > chance) {
							continue;
						}
						JsonArray entityArray = config.getAsJsonArray("entity");
						boolean matches = false;
						for (JsonElement entityElement : entityArray) {
							String entityConfig = entityElement.getAsString();
							if (entityConfig.equals(entityType) || (entityConfig.startsWith("tag:") && checkEntityInTag(entity, entityConfig))) {
								matches = true;
								break;
							}
						}
						if (matches) {
							matchedConfigs.add(config);
						}
					}
				}
				if (!matchedConfigs.isEmpty()) {
					String lastConfigHash = data.getString(lastConfigHashKey);
					matchedConfigs.removeIf(config -> calculateConfigHash(config).equals(lastConfigHash));
					JsonObject finalConfig = selectConfigWithWeight(matchedConfigs);
					if (finalConfig != null) {
						JsonObject finalEntrySettings = finalConfig.getAsJsonObject("entry_settings");
						JsonObject entitySettings = finalConfig.getAsJsonObject("entity_settings");
						JsonObject entityBehaviors = finalConfig.getAsJsonObject("entity_behaviors");
						boolean disableSpawn = entitySettings.has("disable_spawn") && entitySettings.get("disable_spawn").getAsBoolean();
						if (entity instanceof Player) {
							disableSpawn = false;
						}
						if (disableSpawn) {
							event.setCanceled(true);
							return;
						}
						boolean allowReModification = finalEntrySettings == null || !finalEntrySettings.has("allow_re_modification") || finalEntrySettings.get("allow_re_modification").getAsBoolean();
						if (data.getBoolean(processedKey) && !allowReModification) {
							return;
						}
						applyEntityConfigurations(entity, finalConfig);
						data.putBoolean(processedKey, true);
						data.putString(lastConfigHashKey, calculateConfigHash(finalConfig));
						if (finalConfig.has("bossbar")) {
							BossBarHandler.applyBossBar(entity, finalConfig);
						}
						if (entityBehaviors != null) {
							EntityBehaviorsHandler.applyEntityBehaviors(entity, entityBehaviors);
						}
					}
				}
			} catch (JsonSyntaxException e) {
				LOGGER.error("Syntax error in JSON configuration: " + e.getMessage());
			} catch (Exception e) {
				LOGGER.error("Unexpected error while processing configuration: " + e.getMessage());
			}
		}
	}

	private static String calculateConfigHash(JsonObject config) {
		return Integer.toHexString(config.toString().hashCode());
	}

	private static JsonObject selectConfigWithWeight(List<JsonObject> configs) {
		double totalWeight = 0.0;
		List<Double> cumulativeWeights = new ArrayList<>();
		for (JsonObject config : configs) {
			double weight = config.has("entry_settings") && config.getAsJsonObject("entry_settings").has("weight") ? config.getAsJsonObject("entry_settings").get("weight").getAsDouble() : 1.0; // Peso por defecto
			totalWeight += weight;
			cumulativeWeights.add(totalWeight);
		}
		double randomValue = new Random().nextDouble() * totalWeight;
		for (int i = 0; i < configs.size(); i++) {
			if (randomValue < cumulativeWeights.get(i)) {
				return configs.get(i);
			}
		}
		return null;
	}

	private static boolean checkEntryConditions(LivingEntity entity, JsonObject entryConditions) {
		if (entryConditions == null) {
			return true;
		}
		String modId = entryConditions.has("modid") ? entryConditions.get("modid").getAsString() : null;
		if (modId != null && !net.minecraftforge.fml.ModList.get().isLoaded(modId)) {
			return false;
		}
		if (entryConditions.has("allowed_players")) {
			JsonArray allowedPlayers = entryConditions.getAsJsonArray("allowed_players");
			String playerName = entity.getName().getString();
			boolean playerAllowed = false;
			for (JsonElement allowedPlayer : allowedPlayers) {
				if (allowedPlayer.getAsString().equals(playerName)) {
					playerAllowed = true;
					break;
				}
			}
			if (!playerAllowed) {
				return false;
			}
		}
		if (entryConditions.has("biome")) {
			JsonArray biomeArray = entryConditions.getAsJsonArray("biome");
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
		if (entryConditions.has("structure")) {
			JsonArray structureArray = entryConditions.getAsJsonArray("structure");
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
		if (entryConditions.has("difficulty")) {
			JsonArray difficultyArray = entryConditions.getAsJsonArray("difficulty");
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
		if (entryConditions.has("is_baby")) {
			boolean shouldBeBaby = entryConditions.get("is_baby").getAsBoolean();
			if (entity instanceof AgeableMob ageable && ageable.isBaby() != shouldBeBaby) {
				return false;
			}
		}
		if (entryConditions.has("is_riding")) {
			boolean shouldBeRiding = entryConditions.get("is_riding").getAsBoolean();
			if (entity.isPassenger() != shouldBeRiding) {
				return false;
			}
		}
		if (entryConditions.has("nearby_player_advancements")) {
			JsonArray advancementsArray = entryConditions.getAsJsonArray("nearby_player_advancements");
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
		if (entryConditions.has("player_advancements")) {
			JsonArray advancementsArray = entryConditions.getAsJsonArray("player_advancements");
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
		if (entryConditions.has("nearby_player_health")) {
			JsonObject healthConfig = entryConditions.getAsJsonObject("nearby_player_health");
			double minHealth = healthConfig.get("min").getAsDouble();
			double maxHealth = healthConfig.get("max").getAsDouble();
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				double playerHealth = nearestPlayer.getHealth();
				if (playerHealth < minHealth || playerHealth > maxHealth) {
					return false;
				}
			}
		}
		if (entryConditions.has("nearby_player_armor_value")) {
			JsonObject armorConfig = entryConditions.getAsJsonObject("nearby_player_armor_value");
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
		if (entryConditions.has("nearby_player_hunger")) {
			JsonObject hungerConfig = entryConditions.getAsJsonObject("nearby_player_hunger");
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
		if (entryConditions.has("nearby_player_exp")) {
			JsonObject expConfig = entryConditions.getAsJsonObject("nearby_player_exp");
			int minExp = expConfig.get("min").getAsInt();
			int maxExp = expConfig.get("max").getAsInt();
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				int playerExperience = nearestPlayer.totalExperience;
				if (playerExperience < minExp || playerExperience > maxExp) {
					return false;
				}
			}
		}
		if (entryConditions.has("nearby_player_states")) {
			JsonArray statesArray = entryConditions.getAsJsonArray("nearby_player_states");
			Player nearestPlayer = entity.level().getNearestPlayer(entity, 50);
			if (nearestPlayer != null) {
				boolean allStatesMatched = true;
				for (JsonElement stateElement : statesArray) {
					String requiredState = stateElement.getAsString();
					boolean isMatchingState = switch (requiredState) {
						case "riding" -> nearestPlayer.isPassenger();
						case "sneaking" -> nearestPlayer.isCrouching();
						case "sprinting" -> nearestPlayer.isSprinting();
						case "swimming" -> nearestPlayer.isSwimming();
						case "gliding" -> nearestPlayer.isFallFlying();
						case "sleeping" -> nearestPlayer.isSleeping();
						case "burning" -> nearestPlayer.isOnFire();
						case "wet" -> nearestPlayer.isInWater();
						default -> false;
					};
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
		if (entryConditions.has("nearby_player_enchantments")) {
			JsonArray enchantmentsArray = entryConditions.getAsJsonArray("nearby_player_enchantments");
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
		if (entryConditions.has("time")) {
			JsonObject timeConfig = entryConditions.getAsJsonObject("time");
			long currentTime = entity.level().getDayTime() % 24000;
			long startTime = timeConfig.get("start").getAsLong();
			long endTime = timeConfig.get("end").getAsLong();
			if (currentTime < startTime || currentTime > endTime) {
				return false;
			}
		}
		if (entryConditions.has("height")) {
			JsonObject heightConfig = entryConditions.getAsJsonObject("height");
			int minHeight = heightConfig.get("min").getAsInt();
			int maxHeight = heightConfig.get("max").getAsInt();
			int currentHeight = entity.blockPosition().getY();
			if (currentHeight < minHeight || currentHeight > maxHeight) {
				return false;
			}
		}
		if (entryConditions.has("light")) {
			JsonObject lightConfig = entryConditions.getAsJsonObject("light");
			int minLight = lightConfig.get("min").getAsInt();
			int maxLight = lightConfig.get("max").getAsInt();
			int currentLight = entity.level().getBrightness(LightLayer.BLOCK, entity.blockPosition());
			if (currentLight < minLight || currentLight > maxLight) {
				return false;
			}
		}
		if (entryConditions.has("entity_type")) {
			String entityType = entryConditions.get("entity_type").getAsString();
			String currentEntityType = EntityType.getKey(entity.getType()).toString();
			if (!currentEntityType.equals(entityType)) {
				return false;
			}
		}
		if (entryConditions.has("nearby_player_distance_traveled")) {
			JsonObject distTraveledObj = entryConditions.getAsJsonObject("nearby_player_distance_traveled");
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
		if (entryConditions.has("distance_from_spawn")) {
			JsonObject distFromSpawnObj = entryConditions.getAsJsonObject("distance_from_spawn");
			float minDist = distFromSpawnObj.has("min") ? distFromSpawnObj.get("min").getAsFloat() : 0.0F;
			float maxDist = distFromSpawnObj.has("max") ? distFromSpawnObj.get("max").getAsFloat() : Float.MAX_VALUE;
			BlockPos spawnPos = entity.level().getSharedSpawnPos();
			double distanceFromSpawn = entity.blockPosition().distSqr(spawnPos);
			if (distanceFromSpawn < (minDist * minDist) || distanceFromSpawn > (maxDist * maxDist)) {
				return false;
			}
		}
		if (entryConditions.has("nearby_blocks")) {
			JsonArray blockArray = entryConditions.getAsJsonArray("nearby_blocks");
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
				if (blockMatches) {
					break;
				}
			}
			if (!blockMatches) {
				return false;
			}
		}
		if (entryConditions.has("nearby_mobs")) {
			JsonArray mobArray = entryConditions.getAsJsonArray("nearby_mobs");
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
				if (mobMatches) {
					break;
				}
			}
			if (!mobMatches) {
				return false;
			}
		}
		if (entryConditions.has("nearby_player_effects")) {
			JsonArray effectArray = entryConditions.getAsJsonArray("nearby_player_effects");
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
		if (entryConditions.has("entity_effects")) {
			JsonArray effectArray = entryConditions.getAsJsonArray("entity_effects");
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
		if (entryConditions.has("nearby_player_items")) {
			JsonArray itemArray = entryConditions.getAsJsonArray("nearby_player_items");
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
		if (entryConditions.has("moon_phase")) {
			JsonArray moonPhaseArray = entryConditions.getAsJsonArray("moon_phase");
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
		if (entryConditions.has("dimension")) {
			String dimensionId = entryConditions.get("dimension").getAsString();
			ResourceKey<Level> currentDimension = entity.level().dimension();
			if (!currentDimension.location().toString().equals(dimensionId)) {
				return false;
			}
		}
		if (entryConditions.has("weather")) {
			JsonArray weatherArray = entryConditions.getAsJsonArray("weather");
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

	private static void applyEntityConfigurations(LivingEntity entity, JsonObject finalConfig) {
		Map<String, List<JsonObject>> equipmentConfig = new HashMap<>();
		// Load equipment and attributes
		for (Map.Entry<String, JsonElement> entry : finalConfig.getAsJsonObject("equipment").entrySet()) {
			String slot = entry.getKey();
			JsonArray itemsArray = entry.getValue().getAsJsonArray();
			List<JsonObject> itemList = new ArrayList<>();
			for (JsonElement itemElement : itemsArray) {
				try {
					itemList.add(itemElement.getAsJsonObject());
				} catch (JsonSyntaxException e) {
				}
			}
			equipmentConfig.put(slot, itemList);
		}
		if (!equipmentConfig.isEmpty()) {
			EntityEquipmentManager.applyEquipment(entity, equipmentConfig);
		}
		// Apply attributes
		try {
			Map<String, JsonObject> attributesConfig = EntityConfigLoader.parseAttributesConfig(finalConfig);
			EntityEquipmentManager.applyAttributes(entity, attributesConfig);
		} catch (Exception e) {
			LOGGER.error("Error applying attributes: ", e);
		}
		// Apply potion effects
		try {
			Map<String, JsonObject> potionEffectsConfig = EntityConfigLoader.parsePotionEffectsConfig(finalConfig);
			EntityEquipmentManager.applyPotionEffects(entity, potionEffectsConfig);
		} catch (Exception e) {
			// Handle exception if necessary
		}
		// Apply special entity configuration
		if (finalConfig.has("entity_settings") && !finalConfig.get("entity_settings").isJsonNull()) {
			JsonObject entitySettings = finalConfig.getAsJsonObject("entity_settings");
			applySpecialEntityConfig(entity, entitySettings);
		} else {
		}
	}

	public static void applySpecialEntityConfig(LivingEntity entity, JsonObject finalConfig) {
		// Apply silent
		if (finalConfig.has("silent")) {
			boolean silent = finalConfig.get("silent").getAsBoolean();
			entity.setSilent(silent);
			LOGGER.info("Entity {} has been set to silent: {}", entity.getName().getString(), silent);
		}
		// Apply invulnerable
		if (finalConfig.has("invulnerable")) {
			boolean invulnerable = finalConfig.get("invulnerable").getAsBoolean();
			entity.setInvulnerable(invulnerable);
			LOGGER.info("Entity {} has been set to invulnerable: {}", entity.getName().getString(), invulnerable);
		}
		// Apply glowing
		if (finalConfig.has("glowing")) {
			boolean glowing = finalConfig.get("glowing").getAsBoolean();
			entity.setGlowingTag(glowing);
			LOGGER.info("Entity {} has been set to glowing: {}", entity.getName().getString(), glowing);
		}
		// Apply nametag
		if (finalConfig.has("name_tag")) {
			JsonArray nametags = finalConfig.getAsJsonArray("name_tag");
			List<String> names = new ArrayList<>();
			for (JsonElement nameElement : nametags) {
				names.add(nameElement.getAsString());
			}
			String randomName = names.get((int) (Math.random() * names.size()));
			String formattedName = parseTextWithColorCodes(randomName); // Parse colors and formatting codes
			entity.setCustomName(Component.literal(formattedName));
			entity.setCustomNameVisible(true);
			LOGGER.info("Entity {} has been given the name: {}", entity.getName().getString(), formattedName);
		}
		// Apply size for slimes
		if (finalConfig.has("slime_size")) {
			JsonArray sizes = finalConfig.getAsJsonArray("slime_size");
			List<Float> sizeOptions = new ArrayList<>();
			for (JsonElement sizeElement : sizes) {
				sizeOptions.add(sizeElement.getAsFloat());
			}
			float randomSize = sizeOptions.get((int) (Math.random() * sizeOptions.size()));
			if (entity instanceof Slime slime) {
				slime.setSize((int) randomSize, true);
				LOGGER.info("Slime entity {} has been adjusted to size: {}", entity.getName().getString(), randomSize);
			}
		}
		// Apply scale
		if (finalConfig.has("scale")) {
			JsonArray scales = finalConfig.getAsJsonArray("scale");
			List<Float> scaleOptions = new ArrayList<>();
			for (JsonElement scaleElement : scales) {
				scaleOptions.add(scaleElement.getAsFloat());
			}
			float randomScale = scaleOptions.get((int) (Math.random() * scaleOptions.size()));
			ScaleManager.setScale(entity.getUUID(), randomScale);
			entity.getPersistentData().putFloat("entityScale", randomScale);
			LOGGER.info("Scale of entity {} has been set to {}", entity.getName().getString(), randomScale);
		}
		// Apply color for sheep
		if (finalConfig.has("sheep_color")) {
			JsonArray colors = finalConfig.getAsJsonArray("sheep_color");
			List<Integer> colorOptions = new ArrayList<>();
			for (JsonElement colorElement : colors) {
				colorOptions.add(colorElement.getAsInt());
			}
			int randomColor = colorOptions.get((int) (Math.random() * colorOptions.size()));
			if (entity instanceof Sheep sheep) {
				sheep.setColor(DyeColor.byId(randomColor));
				LOGGER.info("Sheep {} color has been set to: {}", entity.getName().getString(), DyeColor.byId(randomColor).getName());
			}
		}
	}

	private static String parseTextWithColorCodes(String text) {
		return text.replace("&", "§");
	}

	private static boolean checkEntityInTag(LivingEntity entity, String tag) {
		String tagName = tag.substring(4);
		TagKey<EntityType<?>> entityTag = TagKey.create(ForgeRegistries.ENTITY_TYPES.getRegistryKey(), new ResourceLocation(tagName));
		return entity.getType().is(entityTag);
	}
}
