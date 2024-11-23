package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvents;

@Mixin(Pillager.class)
public abstract class PillagerMixin extends BaseMobMixin {
	private static final Logger LOGGER = LogManager.getLogger();

	@SuppressWarnings("deprecation")
	@Inject(at = @At("HEAD"), method = "canFireProjectileWeapon(Lnet/minecraft/world/item/ProjectileWeaponItem;)Z", cancellable = true)
	public void canFireProjectileWeapon(ProjectileWeaponItem weaponItem, CallbackInfoReturnable<Boolean> callback) {
		if (weaponItem instanceof CrossbowItem) {
			callback.setReturnValue(true);
		}
	}

	@Inject(at = @At("HEAD"), method = "performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V", cancellable = true)
	public void performRangedAttack(LivingEntity target, float velocity, CallbackInfo callback) {
		Pillager pillager = (Pillager) (Object) this;
		ItemStack mainHandItem = pillager.getMainHandItem();
		if (mainHandItem.getItem() instanceof CrossbowItem) {
			CrossbowItem.performShooting(pillager.level(), pillager, InteractionHand.MAIN_HAND, mainHandItem, velocity, 1.0F);
			pillager.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (pillager.getRandom().nextFloat() * 0.4F + 0.8F));
			callback.cancel();
		}
	}

	@Inject(at = @At("HEAD"), method = "shootCrossbowProjectile(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/projectile/Projectile;F)V")
	private void shootCrossbowProjectile(LivingEntity target, ItemStack crossbow, Projectile projectile, float velocity, CallbackInfo info) {
		/*
			if (crossbow.getItem() instanceof GreatCrossbowItem && projectile instanceof Arrow) {
				float damageMultiplier = 2.5F;
				Arrow arrow = (Arrow) projectile;
				arrow.setBaseDamage(arrow.getBaseDamage() * damageMultiplier);
			}
			if (crossbow.getItem() instanceof HuntersCrossbowItem && projectile instanceof Arrow) {
				Arrow arrow = (Arrow) projectile;
				arrow.setBaseDamage(Math.min(arrow.getBaseDamage(), 2.0));
			}
		
		*/}
}
