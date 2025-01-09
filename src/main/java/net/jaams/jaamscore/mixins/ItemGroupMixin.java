package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;

import net.jaams.jaamscore.config.ItemConfigLoader;

import java.util.Set;
import java.util.Collection;

import com.google.gson.JsonObject;

@Mixin(value = CreativeModeTab.class, priority = 10000)
public abstract class ItemGroupMixin {
	@Shadow
	private Collection<ItemStack> displayItems = ItemStackLinkedSet.createTypeAndTagSet();
	@Shadow
	private Set<ItemStack> displayItemsSearchTab = ItemStackLinkedSet.createTypeAndTagSet();

	@Inject(method = "buildContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;rebuildSearchTree()V"))
	private void filterBannedItemsFromCreativeTab(CreativeModeTab.ItemDisplayParameters displayContext, CallbackInfo ci) {
		displayItems.removeIf(stack -> isBannedFromCreativeTab(stack));
		displayItemsSearchTab.removeIf(stack -> isBannedFromCreativeTab(stack));
	}

	private boolean isBannedFromCreativeTab(ItemStack stack) {
		try {
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("banned_creative_tab")) {
				JsonObject option = config.getAsJsonObject("banned_creative_tab");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value")) {
					return option.get("value").getAsBoolean();
				}
			}
		} catch (Exception e) {
		}
		return false;
	}
}
