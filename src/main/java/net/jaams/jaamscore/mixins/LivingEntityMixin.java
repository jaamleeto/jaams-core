package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;

import net.jaams.jaamscore.manager.ScaleManager;
import net.jaams.jaamscore.entity.CorePlayerEntity;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	public LivingEntityMixin(EntityType<?> type, Level world) {
		super(type, world);
	}

	@Inject(method = "createLivingAttributes", at = @At("RETURN"), cancellable = true)
	private static void addAttackDamageToAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
		AttributeSupplier.Builder builder = cir.getReturnValue();
		if (builder != null) {
			builder.add(Attributes.ATTACK_DAMAGE);
			cir.setReturnValue(builder);
		}
	}

	@Inject(method = "defineSynchedData", at = @At("RETURN"))
	private void onEntityInit(CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		float scaleFactor = ScaleManager.getScale(entity.getUUID());
		if (scaleFactor != 1.0F) {
			entity.refreshDimensions();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		float scaleFactor = ScaleManager.getScale(entity.getUUID());
		if (scaleFactor != 1.0F) {
			entity.refreshDimensions();
		}
	}

	@Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
	public void modifyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;
		float scaleFactor = ScaleManager.getScale(entity.getUUID());
		if (scaleFactor != 1.0F) {
			EntityDimensions currentDimensions = cir.getReturnValue();
			EntityDimensions newDimensions = new EntityDimensions(currentDimensions.width * scaleFactor, currentDimensions.height * scaleFactor, currentDimensions.fixed);
			this.setBoundingBox(newDimensions.makeBoundingBox(this.position()));
			cir.setReturnValue(newDimensions);
		}
	}

	@Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F", at = @At("RETURN"), cancellable = true)
	private void modifyEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;
		float scaleFactor = ScaleManager.getScale(entity.getUUID());
		if (scaleFactor != 1.0F) {
			if (entity instanceof AbstractPiglin || entity instanceof AbstractSkeleton || entity instanceof AbstractVillager || entity instanceof CaveSpider || entity instanceof Cow || entity instanceof Dolphin || entity instanceof EnderMan
					|| entity instanceof Endermite || entity instanceof Fox || entity instanceof Ghast || entity instanceof Shulker || entity instanceof Silverfish || entity instanceof SnowGolem || entity instanceof Spider || entity instanceof Witch
					|| entity instanceof WitherSkeleton || entity instanceof Zombie || entity instanceof ZombifiedPiglin || entity instanceof Player || entity instanceof CorePlayerEntity) {
				cir.setReturnValue(cir.getReturnValue() * scaleFactor);
			}
		}
	}
}
