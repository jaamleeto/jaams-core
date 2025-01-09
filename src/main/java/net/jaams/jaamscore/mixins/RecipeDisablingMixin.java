package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import org.slf4j.LoggerFactory;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.GsonHelper;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

import net.jaams.jaamscore.config.ItemConfigLoader;

import java.util.Map;
import java.util.HashMap;

import com.google.gson.JsonParseException;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

@Mixin(net.minecraft.world.item.crafting.RecipeManager.class)
public class RecipeDisablingMixin {
	@Inject(at = @At("HEAD"), method = "apply", cancellable = true)
	private void filterBannedRecipes(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
		Map<ResourceLocation, JsonElement> filteredMap = new HashMap<>();
		for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
			ResourceLocation resourceLocation = entry.getKey();
			JsonElement jsonElement = entry.getValue();
			try {
				JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "top element");
				JsonElement resultElement = jsonObject.get("result");
				if (resultElement == null) {
					if (jsonObject.get("output") != null) {
						resultElement = jsonObject.get("output");
					} else {
						filteredMap.put(resourceLocation, jsonElement);
						continue;
					}
				}
				boolean shouldDisable = false;
				if (resultElement.isJsonObject()) {
					String itemId = getResultItemId(resultElement.getAsJsonObject());
					if (itemId != null && isRecipeBanned(itemId)) {
						shouldDisable = true;
					}
				} else if (resultElement.isJsonPrimitive() && resultElement.getAsJsonPrimitive().isString()) {
					String itemId = resultElement.getAsString();
					if (itemId != null && isRecipeBanned(itemId)) {
						shouldDisable = true;
					}
				} else if (resultElement.isJsonArray()) {
					JsonArray resultArray = resultElement.getAsJsonArray();
					for (JsonElement element : resultArray) {
						if (element.isJsonObject()) {
							String itemId = getResultItemId(element.getAsJsonObject());
							if (itemId != null && isRecipeBanned(itemId)) {
								shouldDisable = true;
								break;
							}
						} else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
							String itemId = element.getAsString();
							if (itemId != null && isRecipeBanned(itemId)) {
								shouldDisable = true;
								break;
							}
						}
					}
				}
				if (!shouldDisable) {
					filteredMap.put(resourceLocation, jsonElement);
				}
			} catch (IllegalArgumentException | JsonParseException e) {
				LoggerFactory.getLogger(RecipeDisablingMixin.class).debug("Parsing error loading recipe {}", resourceLocation, e);
				filteredMap.put(resourceLocation, jsonElement);
			}
		}
		map.clear();
		map.putAll(filteredMap);
	}

	private boolean isRecipeBanned(String itemId) {
		try {
			Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
			if (item == null) {
				return false;
			}
			ItemStack itemStack = new ItemStack(item);
			JsonObject config = ItemConfigLoader.getItemConfig(itemStack);
			if (config != null && config.has("banned_recipe")) {
				JsonObject option = config.getAsJsonObject("banned_recipe");
				if (ItemConfigLoader.evaluateCondition(option, itemStack) && option.has("value")) {
					return option.get("value").getAsBoolean();
				}
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(RecipeDisablingMixin.class).error("Error checking banned_recipe status for item {}: ", itemId, e);
		}
		return false;
	}

	private String getResultItemId(JsonObject resultObject) {
		if (resultObject.has("item")) {
			return GsonHelper.getAsString(resultObject, "item");
		} else if (resultObject.has("id")) {
			return GsonHelper.getAsString(resultObject, "id");
		} else if (resultObject.has("result")) {
			return GsonHelper.getAsString(resultObject, "result");
		} else if (resultObject.has("output")) {
			return GsonHelper.getAsString(resultObject, "output");
		}
		return null;
	}
}
