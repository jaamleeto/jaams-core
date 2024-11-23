
package net.jaams.jaamscore.handler;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

@Mod.EventBusSubscriber
public class EntityBehaviorsHandler {
	private static final Logger LOGGER = LogManager.getLogger();

	public static void applyEntityBehaviors(LivingEntity entity, JsonObject entityBehaviors) {
		CompoundTag data = entity.getPersistentData();
		if (entityBehaviors.has("explosion_on_attack")) {
			JsonObject explosionAttackBehavior = entityBehaviors.getAsJsonObject("explosion_on_attack");
			if (explosionAttackBehavior.get("enabled").getAsBoolean()) {
				float power = explosionAttackBehavior.has("power") ? explosionAttackBehavior.get("power").getAsFloat() : 2.0F;
				boolean breaksBlocks = explosionAttackBehavior.has("breaks_blocks") && explosionAttackBehavior.get("breaks_blocks").getAsBoolean();
				float chance = explosionAttackBehavior.has("chance") ? explosionAttackBehavior.get("chance").getAsFloat() : 1.0F;
				data.putBoolean("explosionOnAttack", true);
				data.putFloat("explosionPowerOnAttack", power);
				data.putBoolean("explosionBreaksBlocksOnAttack", breaksBlocks);
				data.putFloat("explosionChanceOnAttack", chance);
				LOGGER.info("Attack explosion behavior applied to entity {}: Power = {}, BreaksBlocks = {}, Chance = {}", entity.getUUID(), power, breaksBlocks, chance);
			}
		}
		if (entityBehaviors.has("explosion_on_death")) {
			JsonObject explosionDeathBehavior = entityBehaviors.getAsJsonObject("explosion_on_death");
			if (explosionDeathBehavior.get("enabled").getAsBoolean()) {
				float power = explosionDeathBehavior.has("power") ? explosionDeathBehavior.get("power").getAsFloat() : 2.0F;
				boolean breaksBlocks = explosionDeathBehavior.has("breaks_blocks") && explosionDeathBehavior.get("breaks_blocks").getAsBoolean();
				float chance = explosionDeathBehavior.has("chance") ? explosionDeathBehavior.get("chance").getAsFloat() : 1.0F;
				data.putBoolean("explosionOnDeath", true);
				data.putFloat("explosionPowerOnDeath", power);
				data.putBoolean("explosionBreaksBlocksOnDeath", breaksBlocks);
				data.putFloat("explosionChanceOnDeath", chance);
				LOGGER.info("Death explosion behavior applied to entity {}: Power = {}, BreaksBlocks = {}, Chance = {}", entity.getUUID(), power, breaksBlocks, chance);
			}
		}
		if (entityBehaviors.has("effects_on_attack")) {
			JsonObject effectsOnAttackBehavior = entityBehaviors.getAsJsonObject("effects_on_attack");
			if (effectsOnAttackBehavior.get("enabled").getAsBoolean()) {
				float attackEffectsChance = effectsOnAttackBehavior.has("chance") ? effectsOnAttackBehavior.get("chance").getAsFloat() : 1.0F;
				data.putBoolean("attackEffectsEnabled", true);
				data.putFloat("attackEffectsChance", attackEffectsChance);
				JsonArray effectsArray = effectsOnAttackBehavior.getAsJsonArray("effects");
				CompoundTag attackEffectsData = new CompoundTag();
				int i = 0;
				for (JsonElement effectElement : effectsArray) {
					JsonObject effectObj = effectElement.getAsJsonObject();
					CompoundTag attackEffectTag = new CompoundTag();
					attackEffectTag.putString("attackEffectType", effectObj.get("effect").getAsString());
					attackEffectTag.putInt("attackEffectDuration", effectObj.get("duration").getAsInt());
					attackEffectTag.putInt("attackEffectAmplifier", effectObj.get("amplifier").getAsInt());
					attackEffectTag.putBoolean("attackEffectAmbient", effectObj.has("ambient") && effectObj.get("ambient").getAsBoolean());
					attackEffectTag.putBoolean("attackEffectVisible", effectObj.has("visible") && effectObj.get("visible").getAsBoolean());
					attackEffectsData.put("attack_effect_" + i, attackEffectTag);
					i++;
				}
				data.put("attackEffectsData", attackEffectsData);
				LOGGER.info("Attack effects behavior applied to entity {}: Chance = {}", entity.getUUID(), attackEffectsChance);
			}
		}
		if (entityBehaviors.has("effects_on_attacker")) {
			JsonObject effectsOnAttackerBehavior = entityBehaviors.getAsJsonObject("effects_on_attacker");
			if (effectsOnAttackerBehavior.get("enabled").getAsBoolean()) {
				float attackerEffectsChance = effectsOnAttackerBehavior.has("chance") ? effectsOnAttackerBehavior.get("chance").getAsFloat() : 1.0F;
				data.putBoolean("attackerEffectsEnabled", true);
				data.putFloat("attackerEffectsChance", attackerEffectsChance);
				JsonArray effectsArray = effectsOnAttackerBehavior.getAsJsonArray("effects");
				CompoundTag attackerEffectsData = new CompoundTag();
				int i = 0;
				for (JsonElement effectElement : effectsArray) {
					JsonObject effectObj = effectElement.getAsJsonObject();
					CompoundTag attackerEffectTag = new CompoundTag();
					attackerEffectTag.putString("attackerEffectType", effectObj.get("effect").getAsString());
					attackerEffectTag.putInt("attackerEffectDuration", effectObj.get("duration").getAsInt());
					attackerEffectTag.putInt("attackerEffectAmplifier", effectObj.get("amplifier").getAsInt());
					attackerEffectTag.putBoolean("attackerEffectAmbient", effectObj.has("ambient") && effectObj.get("ambient").getAsBoolean());
					attackerEffectTag.putBoolean("attackerEffectVisible", effectObj.has("visible") && effectObj.get("visible").getAsBoolean());
					attackerEffectsData.put("attacker_effect_" + i, attackerEffectTag);
					i++;
				}
				data.put("attackerEffectsData", attackerEffectsData);
				LOGGER.info("Attacker effects behavior applied to entity {}: Chance = {}", entity.getUUID(), attackerEffectsChance);
			}
		}
		if (entityBehaviors.has("firework_on_death")) {
			JsonObject fireworkDeathBehavior = entityBehaviors.getAsJsonObject("firework_on_death");
			if (fireworkDeathBehavior.get("enabled").getAsBoolean()) {
				float fireworkDeathChance = fireworkDeathBehavior.has("chance") ? fireworkDeathBehavior.get("chance").getAsFloat() : 1.0F;
				int fireworkDeathCount = fireworkDeathBehavior.has("count") ? fireworkDeathBehavior.get("count").getAsInt() : 1;
				data.putBoolean("fireworkOnDeathEnabled", true);
				data.putFloat("fireworkOnDeathChance", fireworkDeathChance);
				data.putInt("fireworkOnDeathCount", fireworkDeathCount);
				CompoundTag fireworkDeathData = new CompoundTag();
				fireworkDeathData.putInt("fireworkFlight", fireworkDeathBehavior.has("flight") ? fireworkDeathBehavior.get("flight").getAsInt() : 1);
				JsonArray explosions = fireworkDeathBehavior.getAsJsonArray("explosions");
				ListTag explosionsList = new ListTag();
				for (JsonElement explosionElement : explosions) {
					JsonObject explosionObj = explosionElement.getAsJsonObject();
					CompoundTag explosionTag = new CompoundTag();
					explosionTag.putBoolean("fireworkFlicker", explosionObj.has("flicker") && explosionObj.get("flicker").getAsBoolean());
					explosionTag.putBoolean("fireworkTrail", explosionObj.has("trail") && explosionObj.get("trail").getAsBoolean());
					explosionTag.putByte("fireworkType", explosionObj.has("type") ? explosionObj.get("type").getAsByte() : 0);
					JsonArray colors = explosionObj.getAsJsonArray("colors");
					List<Integer> colorList = new ArrayList<>();
					for (JsonElement color : colors) {
						colorList.add(color.getAsInt());
					}
					explosionTag.putIntArray("fireworkColors", colorList);
					explosionsList.add(explosionTag);
				}
				fireworkDeathData.put("fireworkExplosions", explosionsList);
				data.put("fireworkOnDeathData", fireworkDeathData);
				LOGGER.info("Firework on death behavior applied to entity {}: Chance = {}, Count = {}", entity.getUUID(), fireworkDeathChance, fireworkDeathCount);
			}
		}
		if (entityBehaviors.has("avoid_behavior")) {
			JsonObject avoidBehavior = entityBehaviors.getAsJsonObject("avoid_behavior");
			if (avoidBehavior.get("enabled").getAsBoolean()) {
				double speedFar = avoidBehavior.has("speed_far") ? avoidBehavior.get("speed_far").getAsDouble() : 1.5;
				double speedNear = avoidBehavior.has("speed_near") ? avoidBehavior.get("speed_near").getAsDouble() : 1.0;
				double avoidRange = avoidBehavior.has("avoid_range") ? avoidBehavior.get("avoid_range").getAsDouble() : 12.0;
				data.putBoolean("hasAvoidBehavior", true);
				data.putDouble("speedFar", speedFar);
				data.putDouble("speedNear", speedNear);
				data.putDouble("avoidRange", avoidRange);
				if (entity instanceof PathfinderMob mob) {
					if (avoidBehavior.has("avoid_targets")) {
						JsonArray targets = avoidBehavior.getAsJsonArray("avoid_targets");
						addAvoidGoals(mob, targets, speedFar, speedNear, avoidRange);
					}
					LOGGER.info("Avoid behavior applied to entity {} with speeds {}, {}, and avoid range {}", entity.getUUID(), speedNear, speedFar, avoidRange);
				}
			}
		}
		if (entityBehaviors.has("panic_behavior")) {
			JsonObject panicBehavior = entityBehaviors.getAsJsonObject("panic_behavior");
			if (panicBehavior.get("enabled").getAsBoolean()) {
				boolean removeGoal = panicBehavior.has("remove") && panicBehavior.get("remove").getAsBoolean();
				double speed = panicBehavior.has("speed") ? panicBehavior.get("speed").getAsDouble() : 1.5;
				data.putBoolean("hasPanicBehavior", true);
				data.putDouble("panicSpeed", speed);
				data.putBoolean("removePanicGoal", removeGoal);
				if (entity instanceof PathfinderMob mob) {
					if (removeGoal) {
						removePanicGoal(mob);
						LOGGER.info("Panic behavior removed from entity {}", entity.getUUID());
					} else {
						mob.goalSelector.addGoal(1, new PanicGoal(mob, speed));
						LOGGER.info("Panic behavior applied to entity {} with speed {}", entity.getUUID(), speed);
					}
				} else {
					LOGGER.warn("Entity {} is not an instance of PathfinderMob and cannot have panic behavior", entity.getUUID());
				}
			}
		}
		if (entityBehaviors.has("attack_behavior")) {
			JsonObject attackBehavior = entityBehaviors.getAsJsonObject("attack_behavior");
			if (attackBehavior.get("enabled").getAsBoolean()) {
				float attackDamage = attackBehavior.has("attack_damage") ? attackBehavior.get("attack_damage").getAsFloat() : 3.0F;
				double speed = attackBehavior.has("speed") ? attackBehavior.get("speed").getAsDouble() : 1.0;
				boolean revengeOnly = attackBehavior.has("attack_on_revenge") && attackBehavior.get("attack_on_revenge").getAsBoolean();
				double attackRange = attackBehavior.has("attack_range") ? attackBehavior.get("attack_range").getAsDouble() : 16.0;
				data.putBoolean("hasAttackBehavior", true);
				data.putFloat("attackDamage", attackDamage);
				data.putDouble("attackSpeed", speed);
				data.putBoolean("revengeOnly", revengeOnly);
				data.putDouble("attackRange", attackRange);
				if (entity instanceof PathfinderMob mob) {
					if (mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
						mob.goalSelector.addGoal(2, new MeleeAttackGoal(mob, speed, true));
						if (attackBehavior.has("targets")) {
							JsonArray targets = attackBehavior.getAsJsonArray("targets");
							setTargetGoals(mob, targets, revengeOnly, attackRange);
						}
						LOGGER.info("Attack behavior applied to entity {} with damage {}, speed {}, revengeOnly {}, and attack range {}", entity.getUUID(), attackDamage, speed, revengeOnly, attackRange);
					} else {
						LOGGER.warn("Entity {} does not have the required attribute ATTACK_DAMAGE to apply attack goals", entity.getUUID());
					}
				}
			}
		}
	}

	private static void setTargetGoals(PathfinderMob entity, JsonArray targets, boolean revengeOnly, double range) {
		for (int i = 0; i < targets.size(); i++) {
			String targetTypeName = targets.get(i).getAsString();
			EntityType<?> targetType = EntityType.byString(targetTypeName).orElse(null);
			if (targetType != null) {
				if (revengeOnly) {
					entity.targetSelector.addGoal(1, new HurtByTargetGoal(entity));
				} else {
					entity.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(entity, LivingEntity.class, 10, true, false, livingEntity -> livingEntity.getType().equals(targetType)));
				}
			} else {
				LOGGER.warn("Target type {} not found or is not valid", targetTypeName);
			}
		}
	}

	private static void addAvoidGoals(PathfinderMob entity, JsonArray targets, double speedFar, double speedNear, double avoidRange) {
		for (int i = 0; i < targets.size(); i++) {
			String targetTypeName = targets.get(i).getAsString();
			EntityType<?> targetType = EntityType.byString(targetTypeName).orElse(null);
			if (targetType != null) {
				entity.goalSelector.addGoal(2, new AvoidEntityGoal<>(entity, LivingEntity.class, (float) avoidRange, speedNear, speedFar, livingEntity -> livingEntity.getType().equals(targetType)));
			} else {
				LOGGER.warn("Avoid target type {} not found or is not valid", targetTypeName);
			}
		}
	}

	private static void removePanicGoal(PathfinderMob mob) {
		mob.goalSelector.getAvailableGoals().removeIf(prioritizedGoal -> prioritizedGoal.getGoal() instanceof PanicGoal);
	}

	@SubscribeEvent
	public static void onFireworkLivingEntityDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (!entity.level().isClientSide()) {
			CompoundTag data = entity.getPersistentData();
			if (data.getBoolean("fireworkOnDeathEnabled")) {
				float chance = data.contains("fireworkOnDeathChance") ? data.getFloat("fireworkOnDeathChance") : 1.0F;
				int count = data.contains("fireworkOnDeathCount") ? data.getInt("fireworkOnDeathCount") : 1;
				if (Math.random() < chance) {
					CompoundTag fireworkData = data.getCompound("fireworkOnDeathData");
					for (int j = 0; j < count; j++) {
						ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
						CompoundTag fireworksTag = new CompoundTag();
						fireworksTag.putInt("Flight", fireworkData.getInt("fireworkFlight"));
						ListTag explosionsList = fireworkData.getList("fireworkExplosions", 10);
						ListTag explosionTagList = new ListTag();
						for (int i = 0; i < explosionsList.size(); i++) {
							CompoundTag explosionData = explosionsList.getCompound(i);
							CompoundTag explosionTag = new CompoundTag();
							explosionTag.putBoolean("Flicker", explosionData.getBoolean("fireworkFlicker"));
							explosionTag.putBoolean("Trail", explosionData.getBoolean("fireworkTrail"));
							explosionTag.putByte("Type", explosionData.getByte("fireworkType"));
							explosionTag.putIntArray("Colors", explosionData.getIntArray("fireworkColors"));
							explosionTagList.add(explosionTag);
						}
						fireworksTag.put("Explosions", explosionTagList);
						fireworkStack.addTagElement("Fireworks", fireworksTag);
						FireworkRocketEntity firework = new FireworkRocketEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), fireworkStack);
						entity.level().addFreshEntity(firework);
					}
					LOGGER.info("Firework(s) launched on death for entity {}: Count = {}, Data = {}", entity.getUUID(), count, fireworkData);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onEffectLivingEntityAttack(LivingAttackEvent event) {
		Entity sourceEntity = event.getSource().getEntity();
		if (sourceEntity instanceof LivingEntity attacker && !event.getEntity().level().isClientSide()) {
			CompoundTag data = attacker.getPersistentData();
			if (data.getBoolean("effectsOnAttackEnabled")) {
				float chance = data.contains("effectsOnAttackChance") ? data.getFloat("effectsOnAttackChance") : 1.0F;
				if (Math.random() < chance) {
					LivingEntity target = (LivingEntity) event.getEntity();
					CompoundTag effectsData = data.getCompound("effectsOnAttackEffects");
					for (String key : effectsData.getAllKeys()) {
						CompoundTag effectTag = effectsData.getCompound(key);
						MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(effectTag.getString("effect")));
						int duration = effectTag.getInt("duration");
						int amplifier = effectTag.getInt("amplifier");
						boolean ambient = effectTag.getBoolean("ambient");
						boolean visible = effectTag.getBoolean("visible");
						if (effect != null) {
							MobEffectInstance effectInstance = new MobEffectInstance(effect, duration, amplifier, ambient, visible);
							target.addEffect(effectInstance);
							LOGGER.info("Applied effect {} to entity {}: Duration = {}, Amplifier = {}, Ambient = {}, Visible = {}", effectTag.getString("effect"), target.getUUID(), duration, amplifier, ambient, visible);
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onEffectEntityAttacked(LivingHurtEvent event) {
		applyAttackerEffects(event.getEntity(), event.getSource().getEntity());
	}

	@SubscribeEvent
	public static void onEffectEntityKilled(LivingDeathEvent event) {
		applyAttackerEffects(event.getEntity(), event.getSource().getEntity());
	}

	private static void applyAttackerEffects(Entity attackedEntity, Entity source) {
		if (attackedEntity instanceof LivingEntity attacked && source instanceof LivingEntity attacker) {
			CompoundTag data = attacked.getPersistentData();
			if (data.getBoolean("attackerEffectsEnabled")) {
				float chance = data.getFloat("attackerEffectsChance");
				if (attacked.getRandom().nextFloat() <= chance) {
					CompoundTag effectsData = data.getCompound("attackerEffectsData");
					for (String key : effectsData.getAllKeys()) {
						CompoundTag effectTag = effectsData.getCompound(key);
						String effectType = effectTag.getString("attackerEffectType");
						int duration = effectTag.getInt("attackerEffectDuration");
						int amplifier = effectTag.getInt("attackerEffectAmplifier");
						boolean ambient = effectTag.getBoolean("attackerEffectAmbient");
						boolean visible = effectTag.getBoolean("attackerEffectVisible");
						MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(effectType));
						if (mobEffect != null) {
							attacker.addEffect(new MobEffectInstance(mobEffect, duration, amplifier, ambient, visible));
							LOGGER.info("Applied effect {} to attacker {}: Duration = {}, Amplifier = {}", effectType, attacker.getUUID(), duration, amplifier);
						} else {
							LOGGER.warn("Effect {} not found in registry, skipping...", effectType);
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onBehaviorLivingEntityAttacked(LivingAttackEvent event) {
		Entity sourceEntity = event.getSource().getEntity();
		if (sourceEntity instanceof LivingEntity attacker && !event.getEntity().level().isClientSide()) {
			CompoundTag data = attacker.getPersistentData();
			if (data.getBoolean("explosionOnAttack")) {
				float chance = data.getFloat("explosionChanceOnAttack");
				if (Math.random() <= chance) {
					float power = data.getFloat("explosionPowerOnAttack");
					Level.ExplosionInteraction interaction = data.getBoolean("explosionBreaksBlocksOnAttack") ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
					event.getEntity().level().explode(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), power, interaction);
					LOGGER.info("Explosion triggered by entity {} on attack: Power = {}, Interaction = {}, Chance = {}", attacker.getUUID(), power, interaction, chance);
				} else {
					LOGGER.info("Attack explosion not triggered for entity {} due to chance = {}", attacker.getUUID(), chance);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onBehaviorLivingEntityDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (!entity.level().isClientSide()) {
			CompoundTag data = entity.getPersistentData();
			if (data.getBoolean("explosionOnDeath")) {
				float chance = data.getFloat("explosionChanceOnDeath");
				if (Math.random() <= chance) {
					float power = data.getFloat("explosionPowerOnDeath");
					Level.ExplosionInteraction interaction = data.getBoolean("explosionBreaksBlocksOnDeath") ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
					entity.level().explode(null, entity.getX(), entity.getY(), entity.getZ(), power, interaction);
					LOGGER.info("Explosion triggered on death for entity {}: Power = {}, Interaction = {}, Chance = {}", entity.getUUID(), power, interaction, chance);
				} else {
					LOGGER.info("Death explosion not triggered for entity {} due to chance = {}", entity.getUUID(), chance);
				}
			}
		}
	}
}
