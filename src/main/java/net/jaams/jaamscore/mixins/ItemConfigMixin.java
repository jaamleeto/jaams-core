package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import net.jaams.jaamscore.config.ItemConfigLoader;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

@Mixin(Item.class)
public abstract class ItemConfigMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger(ItemConfigLoader.class);

	@Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
	private void modifyValidRepairItem(ItemStack stack, ItemStack repairMaterial, CallbackInfoReturnable<Boolean> cir) {
		try {
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("valid_repair_item")) {
				JsonObject option = config.getAsJsonObject("valid_repair_item");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value")) {
					JsonArray validMaterials = option.getAsJsonArray("value");
					for (var element : validMaterials) {
						String itemId = element.getAsString();
						if (repairMaterial.is(BuiltInRegistries.ITEM.get(new ResourceLocation(itemId)))) {
							cir.setReturnValue(true);
							return;
						}
					}
					cir.setReturnValue(false);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error processing valid repair item config: ", e);
		}
	}

	@Inject(method = "getEnchantmentValue", at = @At("HEAD"), cancellable = true)
	private void modifyEnchantmentValue(CallbackInfoReturnable<Integer> cir) {
		try {
			ItemStack dummyStack = new ItemStack((Item) (Object) this);
			JsonObject config = ItemConfigLoader.getItemConfig(dummyStack);
			if (config != null && config.has("enchantment_value")) {
				JsonObject option = config.getAsJsonObject("enchantment_value");
				if (ItemConfigLoader.evaluateCondition(option, dummyStack) && option.has("value")) {
					cir.setReturnValue(option.get("value").getAsInt());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying enchantment value: ", e);
		}
	}

	@Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
	private void modifyMaxStackSize(CallbackInfoReturnable<Integer> cir) {
		try {
			ItemStack dummyStack = new ItemStack((Item) (Object) this);
			JsonObject config = ItemConfigLoader.getItemConfig(dummyStack);
			if (config != null && config.has("max_stack_size")) {
				JsonObject option = config.getAsJsonObject("max_stack_size");
				if (ItemConfigLoader.evaluateCondition(option, dummyStack) && option.has("value")) {
					cir.setReturnValue(option.get("value").getAsInt());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying max stack size: ", e);
		}
	}

	@Inject(method = "isFireResistant", at = @At("HEAD"), cancellable = true)
	private void modifyFireResistance(CallbackInfoReturnable<Boolean> cir) {
		try {
			ItemStack dummyStack = new ItemStack((Item) (Object) this);
			JsonObject config = ItemConfigLoader.getItemConfig(dummyStack);
			if (config != null && config.has("fire_resistant")) {
				JsonObject option = config.getAsJsonObject("fire_resistant");
				if (ItemConfigLoader.evaluateCondition(option, dummyStack) && option.has("value")) {
					cir.setReturnValue(option.get("value").getAsBoolean());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying fire resistance: ", e);
		}
	}

	@Inject(method = "getFoodProperties", at = @At("RETURN"), cancellable = true)
	private void modifyFoodProperties(CallbackInfoReturnable<FoodProperties> cir) {
		try {
			ItemStack dummyStack = new ItemStack((Item) (Object) this);
			JsonObject config = ItemConfigLoader.getItemConfig(dummyStack);
			if (config != null && config.has("food_properties")) {
				JsonObject option = config.getAsJsonObject("food_properties");
				if (ItemConfigLoader.evaluateCondition(option, dummyStack) && option.has("value")) {
					JsonObject foodProps = option.getAsJsonObject("value");
					FoodProperties.Builder builder = new FoodProperties.Builder();
					if (foodProps.has("nutrition")) {
						builder.nutrition(foodProps.get("nutrition").getAsInt());
					}
					if (foodProps.has("saturation")) {
						builder.saturationMod(foodProps.get("saturation").getAsFloat());
					}
					cir.setReturnValue(builder.build());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying food properties: ", e);
		}
	}

	@Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
	private void modifyItemRarity(CallbackInfoReturnable<Rarity> cir) {
		try {
			ItemStack dummyStack = new ItemStack((Item) (Object) this);
			JsonObject config = ItemConfigLoader.getItemConfig(dummyStack);
			if (config != null && config.has("rarity")) {
				JsonObject option = config.getAsJsonObject("rarity");
				if (ItemConfigLoader.evaluateCondition(option, dummyStack) && option.has("value")) {
					try {
						cir.setReturnValue(Rarity.valueOf(option.get("value").getAsString().toUpperCase()));
					} catch (IllegalArgumentException e) {
						LOGGER.error("Invalid 'rarity' value in config.");
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying item rarity: ", e);
		}
	}

	@Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
	private void modifyIsEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		try {
			JsonObject config = ItemConfigLoader.getItemConfig(stack);
			if (config != null && config.has("enchantable")) {
				JsonObject option = config.getAsJsonObject("enchantable");
				if (ItemConfigLoader.evaluateCondition(option, stack) && option.has("value")) {
					cir.setReturnValue(option.get("value").getAsBoolean());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error modifying enchantable status: ", e);
		}
	}
}
