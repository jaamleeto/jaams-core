package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;

@Mixin(LivingEntity.class)
public abstract class BaseLivingEntityMixin extends BaseEntityMixin {
	@Shadow
	protected void hurtArmor(DamageSource source, float damage) {
		throw new IllegalStateException("Mixin failed to shadow the \"LivingEntity.hurtArmor(float)\" method!");
	}

	@Shadow
	public int getArmorValue() {
		throw new IllegalStateException("Mixin failed to shadow the \"LivingEntity.getArmorValue()\" method!");
	}

	@Shadow
	public double getAttributeValue(Attribute attribute) {
		throw new IllegalStateException("Mixin failed to shadow the \"LivingEntity.getAttributeValue(Attribute)\" method!");
	}

	@Shadow
	public ItemStack getItemInHand(InteractionHand hand) {
		throw new IllegalStateException("Mixin failed to shadow the \"LivingEntity.getItemInHand(InteractionHand)\" method!");
	}
}
