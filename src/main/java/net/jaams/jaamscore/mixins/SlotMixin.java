package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;

import net.jaams.jaamscore.configuration.JaamsCoreCommonConfiguration;

@Mixin(Slot.class)
public abstract class SlotMixin {
	private static boolean isValidTwoHandedItem(ItemStack itemStack) {
		ResourceLocation itemResourceLocation = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
		for (String stringIterator : JaamsCoreCommonConfiguration.TWOHANDEDBLACKLIST.get()) {
			ResourceLocation resourceLocation = new ResourceLocation(stringIterator);
			if (itemResourceLocation != null && itemResourceLocation.equals(resourceLocation)) {
				return false;
			}
		}
		for (String stringIterator : JaamsCoreCommonConfiguration.TWOHANDEDWHITELIST.get()) {
			ResourceLocation resourceLocation = new ResourceLocation(stringIterator);
			if (itemResourceLocation != null && itemResourceLocation.equals(resourceLocation)) {
				return true;
			}
		}
		return false;
	}

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void mayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		Slot slot = (Slot) (Object) this;
		if (slot.container instanceof Inventory && (slot.getSlotIndex() == 40 || slot.getSlotIndex() == 36)) {
			Player player = Minecraft.getInstance().player;
			if (player != null) {
				ItemStack mainHandStack = player.getMainHandItem();
				if ((isValidTwoHandedItem(stack) || isValidTwoHandedItem(mainHandStack)) && JaamsCoreCommonConfiguration.TWOHANDED.get()) {
					cir.setReturnValue(false);
				}
			}
		}
	}
}
