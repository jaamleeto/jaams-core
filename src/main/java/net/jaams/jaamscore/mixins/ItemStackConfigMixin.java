package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemStack;

import net.jaams.jaamscore.config.ItemConfigLoader;

import com.google.gson.JsonObject;

@Mixin(ItemStack.class)
public abstract class ItemStackConfigMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger(ItemConfigLoader.class);

	@Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
	private void modifyItemDurability(CallbackInfoReturnable<Integer> cir) {
		try {
			ItemStack stack = (ItemStack) (Object) this;
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("durability")) {
				JsonObject option = config.getAsJsonObject("durability");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value")) {
					cir.setReturnValue(option.get("value").getAsInt());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying item durability: ", e);
		}
	}

	@Inject(method = "getBarWidth", at = @At("RETURN"), cancellable = true)
	public void getBarWidth(CallbackInfoReturnable<Integer> cir) {
		try {
			ItemStack stack = (ItemStack) (Object) this;
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("durability")) {
				JsonObject option = config.getAsJsonObject("durability");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value")) {
					int customMaxDamage = option.get("value").getAsInt();
					int currentDamage = stack.getDamageValue();
					int barWidth = Math.round(13.0F - (13.0F * currentDamage / (float) customMaxDamage));
					cir.setReturnValue(barWidth);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying item bar width: ", e);
		}
	}

	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void modifyMiningSpeed(CallbackInfoReturnable<Float> cir) {
		try {
			ItemStack stack = (ItemStack) (Object) this;
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("destroy_speed")) {
				JsonObject option = config.getAsJsonObject("destroy_speed");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value")) {
					cir.setReturnValue(option.get("value").getAsFloat());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying mining speed: ", e);
		}
	}

	@Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
	private void modifyFoilEffect(CallbackInfoReturnable<Boolean> cir) {
		try {
			ItemStack stack = (ItemStack) (Object) this;
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("has_foil")) {
				JsonObject option = config.getAsJsonObject("has_foil");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value")) {
					cir.setReturnValue(option.get("value").getAsBoolean());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying foil effect: ", e);
		}
	}

	@Inject(method = "isDamageableItem", at = @At("RETURN"), cancellable = true)
	private void modifyDamageableStatus(CallbackInfoReturnable<Boolean> cir) {
		try {
			ItemStack stack = (ItemStack) (Object) this;
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("damageable")) {
				JsonObject option = config.getAsJsonObject("damageable");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value")) {
					cir.setReturnValue(option.get("value").getAsBoolean());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying damageable status: ", e);
		}
	}

	@Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
	private void modifyUseAnimation(CallbackInfoReturnable<UseAnim> cir) {
		try {
			ItemStack stack = (ItemStack) (Object) this;
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("use_animation")) {
				JsonObject option = config.getAsJsonObject("use_animation");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value")) {
					String animation = option.get("value").getAsString();
					try {
						cir.setReturnValue(UseAnim.valueOf(animation.toUpperCase()));
					} catch (IllegalArgumentException e) {
						LOGGER.error("Invalid 'use_animation' value in config: {}", animation);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying use animation: ", e);
		}
	}

	@Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
	private void modifyUseDuration(CallbackInfoReturnable<Integer> cir) {
		try {
			ItemStack stack = (ItemStack) (Object) this;
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("use_duration")) {
				JsonObject option = config.getAsJsonObject("use_duration");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value")) {
					cir.setReturnValue(option.get("value").getAsInt());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying use duration: ", e);
		}
	}
}
