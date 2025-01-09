package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import org.slf4j.LoggerFactory;

import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.Util;

import net.jaams.jaamscore.config.ItemConfigLoader;

import com.google.gson.JsonObject;

@Mixin(value = MerchantMenu.class, priority = 10000)
public class VillagerTradeMixin {
	@Inject(at = {@At("RETURN")}, method = "getOffers", cancellable = true)
	public void getRecipes(CallbackInfoReturnable<MerchantOffers> info) {
		if (info.getReturnValue() != null) {
			MerchantOffers filteredOffers = new MerchantOffers(Util.make(new CompoundTag(), tag -> tag.put("Recipes", new ListTag())));
			info.getReturnValue().forEach(offer -> {
				if (!isBannedTrade(offer.getResult())) {
					filteredOffers.add(offer);
				}
			});
			info.setReturnValue(filteredOffers);
		}
	}

	private boolean isBannedTrade(ItemStack result) {
		try {
			JsonObject config = ItemConfigLoader.getItemConfig(result);
			if (config != null && config.has("banned_trade")) {
				JsonObject option = config.getAsJsonObject("banned_trade");
				if (ItemConfigLoader.evaluateCondition(option, result) && option.has("value")) {
					return option.get("value").getAsBoolean();
				}
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(VillagerTradeMixin.class).error("Error checking banned trade status for item: ", e);
		}
		return false;
	}
}
