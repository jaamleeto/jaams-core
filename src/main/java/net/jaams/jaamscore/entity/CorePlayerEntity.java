
package net.jaams.jaamscore.entity;

import org.jetbrains.annotations.NotNull;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.items.ItemStackHandler;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;

import net.jaams.jaamscore.init.JaamsCoreModEntities;
import net.jaams.jaamscore.configuration.JaamsCoreCommonConfiguration;

import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.Set;
import java.util.Random;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import com.mojang.authlib.GameProfile;

public class CorePlayerEntity extends Monster implements RangedAttackMob, CrossbowAttackMob {
	private final GameProfile gameProfile;
	private Difficulty lastDifficulty;
	private static final UUID BABY_SPEED_BOOST_ID = UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D836");
	private static final AttributeModifier BABY_SPEED_BOOST = new AttributeModifier(BABY_SPEED_BOOST_ID, "Baby speed boost", 0.5D, AttributeModifier.Operation.MULTIPLY_BASE);
	private static final EntityDataAccessor<Boolean> IS_CHILD = SynchedEntityData.defineId(CorePlayerEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> IS_CHARGING_CROSSBOW = SynchedEntityData.defineId(CorePlayerEntity.class, EntityDataSerializers.BOOLEAN);
	private final BreakDoorGoal breakDoorGoal = new BreakDoorGoal(this, (difficulty) -> difficulty == Difficulty.HARD);
	private final RangedBowAttackGoal<CorePlayerEntity> bowAttackGoal = new RangedBowAttackGoal<>(this, 1.0D, 20, 15.0F);
	private final RangedCrossbowAttackGoal<CorePlayerEntity> crossbowAttackGoal = new RangedCrossbowAttackGoal<>(this, 1.0D, 15.0F);
	private final MeleeAttackGoal meleeAttackGoal = new MeleeAttackGoal(this, 1.2D, false);
	// Inventario de la entidad
	private final ItemStackHandler inventory = new ItemStackHandler(36);

	public CorePlayerEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(JaamsCoreModEntities.CORE_PLAYER.get(), world);
	}

	public CorePlayerEntity(EntityType<CorePlayerEntity> type, Level world) {
		super(type, world);
		setMaxUpStep(0.6f);
		xpReward = 0;
		setNoAi(false);
		setCombatTask();
		String name = this.getName() != null ? this.getName().getString() : "Unknown";
		this.gameProfile = new GameProfile(this.getUUID(), name);
		initializeDefaultInventory();
	}

	private List<EntityType<?>> getEntityTypesFromConfig(List<? extends String> entityNames) {
		List<EntityType<?>> entityTypes = new ArrayList<>();
		for (String name : entityNames) {
			EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(name));
			if (type != null) {
				entityTypes.add(type);
			}
		}
		return entityTypes;
	}

	private boolean canBreakDoors;

	@Override
	protected void registerGoals() {
		super.registerGoals();
		if (canOpenDoor()) {
			goalSelector.addGoal(1, new OpenDoorGoal(this, true));
			((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
		}
		this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LivingEntity.class, 6.0F, 1.0D, 1.2D, entity -> getEntityTypesFromConfig(JaamsCoreCommonConfiguration.AVOIDENTITIES.get()).contains(entity.getType())));
		if (level().getDifficulty() != Difficulty.PEACEFUL) {
			this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> getEntityTypesFromConfig(JaamsCoreCommonConfiguration.ATTACKENTITIES.get()).contains(entity.getType())));
		}
		this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9D));
		this.goalSelector.addGoal(8, new FloatGoal(this));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, true, entity -> {
			if (entity instanceof CorePlayerEntity corePlayer && corePlayer.isBaby()) {
				LivingEntity attacker = corePlayer.getLastHurtByMob();
				return attacker != null && attacker != this;
			}
			return false;
		}));
	}

	@Override
	public void tick() {
		super.tick();
	}

	@Override
	protected void dropEquipment() {
		for (int i = 0; i < inventory.getSlots(); i++) {
			ItemStack itemStack = inventory.getStackInSlot(i);
			if (!itemStack.isEmpty()) {
				spawnAtLocation(itemStack);
			}
		}
		super.dropEquipment();
	}

	public boolean addItemToInventory(ItemStack stack) {
		return inventory.insertItem(0, stack, false).isEmpty();
	}

	public ItemStack getItemFromInventory(int slot) {
		return inventory.getStackInSlot(slot);
	}

	private static final int INVENTORY_SIZE = 36;
	private final Random random = new Random();

	private void initializeDefaultInventory() {
		Set<Integer> usedSlots = new HashSet<>();
		addItemToRandomSlot(new ItemStack(Items.IRON_SWORD), usedSlots);
		addItemToRandomSlot(new ItemStack(Items.GOLDEN_APPLE, 5), usedSlots);
		addItemToRandomSlot(new ItemStack(Items.IRON_AXE), usedSlots);
		addItemToRandomSlot(new ItemStack(Items.POTION), usedSlots);
	}

	private void addItemToRandomSlot(ItemStack item, Set<Integer> usedSlots) {
		List<Integer> availableSlots = IntStream.range(0, INVENTORY_SIZE).filter(slot -> !usedSlots.contains(slot)).boxed().collect(Collectors.toList());
		int slot = availableSlots.get(random.nextInt(availableSlots.size()));
		inventory.setStackInSlot(slot, item);
		usedSlots.add(slot);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putBoolean("CanBreakDoors", canBreakDoors);
		compound.putBoolean("IsBaby", isBaby());
		compound.put("Inventory", inventory.serializeNBT());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		setCanBreakDoors(compound.getBoolean("CanBreakDoors"));
		setBaby(compound.getBoolean("IsBaby"));
		if (compound.contains("Inventory")) {
			inventory.deserializeNBT(compound.getCompound("Inventory"));
		}
		setCombatTask();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		getEntityData().define(IS_CHILD, false);
		getEntityData().define(IS_CHARGING_CROSSBOW, false);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		if (IS_CHILD.equals(key)) {
			refreshDimensions();
		}
		super.onSyncedDataUpdated(key);
	}

	@Override
	public float getStandingEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
		return isBaby() ? 0.93F : 1.62F;
	}

	@Override
	public double getPassengersRidingOffset() {
		return (double) this.getBbHeight() * 0.92D;
	}

	@Override
	public double getMyRidingOffset() {
		if (this.isBaby()) {
			return (this.getVehicle() instanceof Animal) ? -0.25D : 0.0D;
		} else if (this.getVehicle() instanceof CorePlayerEntity && !((CorePlayerEntity) this.getVehicle()).isBaby()) {
			return 0.45D;
		}
		return -0.45D;
	}

	@Override
	public boolean isFallFlying() {
		return false;
	}

	@Override
	public boolean isPersistenceRequired() {
		return true;
	}

	@Override
	public boolean isBaby() {
		return getEntityData().get(IS_CHILD);
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public void setBaby(boolean isChild) {
		super.setBaby(isChild);
		getEntityData().set(IS_CHILD, isChild);
		if (!level().isClientSide) {
			AttributeInstance attribute = getAttribute(Attributes.MOVEMENT_SPEED);
			attribute.removeModifier(BABY_SPEED_BOOST);
			if (isChild) {
				attribute.addTransientModifier(BABY_SPEED_BOOST);
			}
		}
	}

	@Override
	public void performRangedAttack(LivingEntity target, float distanceFactor) {
		ItemStack weaponStack = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof ProjectileWeaponItem && canFireProjectileWeapon((ProjectileWeaponItem) item)));
		if (weaponStack.getItem() instanceof CrossbowItem) {
			performCrossbowAttack(this, 1.6F);
		} else if (weaponStack.getItem() instanceof BowItem) {
			ItemStack itemstack = getProjectile(weaponStack);
			AbstractArrow arrow = ProjectileUtil.getMobArrow(this, itemstack, distanceFactor);
			arrow.setOwner(this);
			double x = target.getX() - getX();
			double y = target.getY(1D / 3D) - arrow.getY();
			double z = target.getZ() - getZ();
			double d3 = Math.sqrt(x * x + z * z);
			arrow.shoot(x, y + d3 * 0.2F, z, 1.6F, 14 - level().getDifficulty().getId() * 4);
			playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
			level().addFreshEntity(arrow);
		}
	}

	@Override
	public boolean canFireProjectileWeapon(ProjectileWeaponItem item) {
		return item instanceof BowItem || item instanceof CrossbowItem;
	}

	@Override
	public void shootCrossbowProjectile(LivingEntity target, ItemStack crossbow, Projectile projectile, float angle) {
		shootCrossbowProjectile(this, target, projectile, angle, 1.6F);
	}

	public boolean isChargingCrossbow() {
		return entityData.get(IS_CHARGING_CROSSBOW);
	}

	@Override
	public void setChargingCrossbow(boolean pIsCharging) {
		entityData.set(IS_CHARGING_CROSSBOW, pIsCharging);
	}

	@Override
	public void onCrossbowAttackPerformed() {
		noActionTime = 0;
	}

	@Override
	public void setItemSlot(@NotNull EquipmentSlot slotIn, @NotNull ItemStack stack) {
		super.setItemSlot(slotIn, stack);
		if (!level().isClientSide) {
			setCombatTask();
		}
	}

	public void setCombatTask() {
		if (!level().isClientSide) {
			goalSelector.removeGoal(bowAttackGoal);
			goalSelector.removeGoal(crossbowAttackGoal);
			goalSelector.removeGoal(meleeAttackGoal);
			ItemStack itemstack = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof ProjectileWeaponItem && canFireProjectileWeapon((ProjectileWeaponItem) item)));
			LivingEntity target = getTarget();
			if (target != null && distanceToSqr(target) < 4.0D) {
				goalSelector.addGoal(2, meleeAttackGoal);
			} else {
				if (itemstack.getItem() instanceof CrossbowItem) {
					goalSelector.addGoal(2, crossbowAttackGoal);
				} else if (itemstack.getItem() instanceof BowItem) {
					bowAttackGoal.setMinAttackInterval(level().getDifficulty() != Difficulty.HARD ? 20 : 40);
					goalSelector.addGoal(2, bowAttackGoal);
				} else {
					goalSelector.addGoal(2, meleeAttackGoal);
					goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
				}
			}
		}
	}

	public void setCanBreakDoors(boolean enabled) {
		if (GoalUtils.hasGroundPathNavigation(this)) {
			if (canBreakDoors != enabled) {
				canBreakDoors = enabled;
				((GroundPathNavigation) getNavigation()).setCanOpenDoors(enabled || canOpenDoor());
				if (enabled)
					goalSelector.addGoal(1, breakDoorGoal);
				else
					goalSelector.removeGoal(breakDoorGoal);
			}
		} else if (canBreakDoors) {
			goalSelector.removeGoal(breakDoorGoal);
			canBreakDoors = false;
		}
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.death"));
	}

	@Override
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}

	@Override
	public void checkDespawn() {
		if (level().getDifficulty() != Difficulty.PEACEFUL) {
			super.checkDespawn();
		}
	}

	public GameProfile getGameProfile() {
		return this.gameProfile;
	}

	private boolean isSitting;
	private boolean isWaving;
	private boolean isRunning;
	private boolean isLookingAround;

	private boolean canOpenDoor() {
		return true;
	}

	public boolean isSitting() {
		return isSitting;
	}

	public boolean isWaving() {
		return isWaving;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean isLookingAround() {
		return isLookingAround;
	}

	public void setSitting(boolean sitting) {
		this.isSitting = sitting;
		if (sitting)
			clearOtherStates("isSitting");
	}

	public void setWaving(boolean waving) {
		this.isWaving = waving;
		if (waving)
			clearOtherStates("isWaving");
	}

	public void setRunning(boolean running) {
		this.isRunning = running;
		if (running)
			clearOtherStates("isRunning");
	}

	public void setLookingAround(boolean lookingAround) {
		this.isLookingAround = lookingAround;
		if (lookingAround)
			clearOtherStates("isLookingAround");
	}

	private void clearOtherStates(String activeState) {
		if (!"isSitting".equals(activeState))
			isSitting = false;
		if (!"isWaving".equals(activeState))
			isWaving = false;
		if (!"isRunning".equals(activeState))
			isRunning = false;
		if (!"isLookingAround".equals(activeState))
			isLookingAround = false;
	}

	public static void init() {
		SpawnPlacements.register(JaamsCoreModEntities.CORE_PLAYER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				(entityType, world, reason, pos, random) -> (world.getDifficulty() != Difficulty.PEACEFUL && Monster.isDarkEnoughToSpawn(world, pos, random) && Mob.checkMobSpawnRules(entityType, world, reason, pos, random)));
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 20);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 1);
		builder = builder.add(Attributes.FOLLOW_RANGE, 16);
		return builder;
	}
}
