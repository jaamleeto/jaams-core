package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import net.jaams.jaamscore.config.ItemConfigLoader;

import com.google.gson.JsonObject;

@Mixin(Entity.class)
public abstract class ItemBannedMixin {
	@Inject(at = @At("HEAD"), method = "tick")
	public void removeBannedItems(CallbackInfo info, int i) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		ItemStack item = player.getInventory().getItem(i);
		try {
			JsonObject config = ItemConfigLoader.getItemConfig(item);
			if (config != null && config.has("banned_item")) {
				JsonObject option = config.getAsJsonObject("banned_item");
				if (ItemConfigLoader.evaluateCondition(option, item) && option.has("value") && option.get("value").getAsBoolean()) {
					item.setCount(0);
				}
			}
		} catch (Exception e) {
		}
	}
}
