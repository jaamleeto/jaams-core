package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;

@Mixin(Mob.class)
public abstract class BaseMobMixin extends BaseLivingEntityMixin {
	@Shadow
	@Final
	public GoalSelector goalSelector;

	@Shadow
	public void setItemSlot(EquipmentSlot slotIn, ItemStack item) {
		throw new IllegalStateException("Mixin failed to shadow the \"Zombie.setItemSlot(...)\" method!");
	}
}
