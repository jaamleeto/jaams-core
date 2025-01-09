package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity.RemovalReason;

import net.jaams.jaamscore.config.ItemConfigLoader;

import com.google.gson.JsonObject;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Inject(at = @At("HEAD"), method = "tick")
	private void removeBannedItemEntities(CallbackInfo ci) {
		ItemStack item = ((ItemEntity) (Object) this).getItem();
		try {
			JsonObject config = ItemConfigLoader.getItemConfig(item);
			if (config != null && config.has("banned_item_entity")) {
				JsonObject option = config.getAsJsonObject("banned_item_entity");
				if (ItemConfigLoader.evaluateCondition(option, item) && option.has("value") && option.get("value").getAsBoolean()) {
					((ItemEntity) (Object) this).remove(RemovalReason.DISCARDED);
				}
			}
		} catch (Exception e) {
		}
	}
}
