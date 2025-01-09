package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.ServerPlayer;

import net.jaams.jaamscore.config.ItemConfigLoader;

import com.google.gson.JsonObject;

@Mixin(value = ServerPlayerGameMode.class, priority = 10000)
public abstract class InteractionMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger(ItemConfigLoader.class);

	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void handleInteraction(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
		try {
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("disable_interaction")) {
				JsonObject option = config.getAsJsonObject("disable_interaction");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value") && option.get("value").getAsBoolean()) {
					ci.setReturnValue(InteractionResult.FAIL);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error handling interaction: ", e);
		}
	}
}
