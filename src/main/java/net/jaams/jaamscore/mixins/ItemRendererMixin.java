package net.jaams.jaamscore.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;

import net.jaams.jaamscore.config.ItemTransformConfigLoader;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
	private static final Logger LOGGER = LogManager.getLogger();

	@Shadow
	public void render(ItemStack stack, ItemDisplayContext displayContext, boolean leftHanded, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, BakedModel model) {
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void injectCustomTransform(ItemStack stack, ItemDisplayContext displayContext, boolean leftHanded, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, BakedModel model, CallbackInfo ci) {
		JsonObject customTransform = getCustomTransform(stack);
		if (customTransform != null) {
			try {
				applyStaticTransformations(customTransform, displayContext, poseStack);
				applyDynamicAnimations(customTransform, poseStack, displayContext, leftHanded);
			} catch (Exception e) {
				LOGGER.error("Error applying custom transformation for item {}: {}", stack.getItem(), e.getMessage());
				LOGGER.debug("Stack trace:", e);
			}
		}
	}

	private JsonObject getCustomTransform(ItemStack stack) {
		try {
			return ItemTransformConfigLoader.getTransformConfig(stack, Minecraft.getInstance().player);
		} catch (Exception e) {
			LOGGER.error("Failed to load custom transform config for item {}: {}", stack.getItem(), e.getMessage());
			LOGGER.debug("Stack trace:", e);
			return null;
		}
	}

	private void applyStaticTransformations(JsonObject customTransform, ItemDisplayContext displayContext, PoseStack poseStack) {
		if (!customTransform.has("transformations"))
			return;
		JsonObject transformations = customTransform.getAsJsonObject("transformations");
		String contextKey = displayContext.name().toLowerCase();
		if (transformations.has(contextKey)) {
			JsonObject transform = transformations.getAsJsonObject(contextKey);
			applyTransformations(poseStack, transform);
		}
	}

	private void applyTransformations(PoseStack poseStack, JsonObject transform) {
		applyRotation(poseStack, transform);
		applyTranslation(poseStack, transform);
		applyScaling(poseStack, transform);
	}

	private void applyRotation(PoseStack poseStack, JsonObject transform) {
		if (transform.has("rotation")) {
			try {
				JsonArray rotation = transform.getAsJsonArray("rotation");
				poseStack.mulPose(Axis.XP.rotationDegrees(rotation.get(0).getAsFloat()));
				poseStack.mulPose(Axis.YP.rotationDegrees(rotation.get(1).getAsFloat()));
				poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.get(2).getAsFloat()));
			} catch (Exception e) {
				LOGGER.error("Invalid rotation data: {}", e.getMessage());
				LOGGER.debug("Stack trace:", e);
			}
		}
	}

	private void applyTranslation(PoseStack poseStack, JsonObject transform) {
		if (transform.has("translation")) {
			try {
				JsonArray translation = transform.getAsJsonArray("translation");
				poseStack.translate(translation.get(0).getAsDouble(), translation.get(1).getAsDouble(), translation.get(2).getAsDouble());
			} catch (Exception e) {
				LOGGER.error("Invalid translation data: {}", e.getMessage());
				LOGGER.debug("Stack trace:", e);
			}
		}
	}

	private void applyScaling(PoseStack poseStack, JsonObject transform) {
		if (transform.has("scale")) {
			try {
				JsonArray scale = transform.getAsJsonArray("scale");
				poseStack.scale(scale.get(0).getAsFloat(), scale.get(1).getAsFloat(), scale.get(2).getAsFloat());
			} catch (Exception e) {
				LOGGER.error("Invalid scale data: {}", e.getMessage());
				LOGGER.debug("Stack trace:", e);
			}
		}
	}

	private void applyDynamicAnimations(JsonObject customTransform, PoseStack poseStack, ItemDisplayContext displayContext, boolean leftHanded) {
		if (!customTransform.has("animation"))
			return;
		JsonObject animation = customTransform.getAsJsonObject("animation");
		JsonObject specificAnimation = determineAnimationForView(animation, displayContext, leftHanded);
		if (specificAnimation != null) {
			applyAnimationTransformations(poseStack, specificAnimation);
		}
	}

	private JsonObject determineAnimationForView(JsonObject animation, ItemDisplayContext displayContext, boolean leftHanded) {
		String contextKey = switch (displayContext) {
			case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> "first_person";
			case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> "third_person";
			case GUI -> "gui";
			case GROUND -> "ground";
			case FIXED -> "fixed";
			case HEAD -> "head";
			default -> null;
		};
		if (contextKey != null && animation.has(contextKey)) {
			return animation.getAsJsonObject(contextKey);
		}
		return animation.has("default") ? animation.getAsJsonObject("default") : null;
	}

	private void applyAnimationTransformations(PoseStack poseStack, JsonObject specificAnimation) {
		float time = getCurrentGameTime();
		applyDynamicRotation(poseStack, specificAnimation, time);
		applyPulsatingScale(poseStack, specificAnimation, time);
		applyOscillation(poseStack, specificAnimation, time);
	}

	private void applyDynamicRotation(PoseStack poseStack, JsonObject specificAnimation, float time) {
		if (specificAnimation.has("rotation")) {
			try {
				JsonArray rotation = specificAnimation.getAsJsonArray("rotation");
				poseStack.mulPose(Axis.XP.rotationDegrees(rotation.get(0).getAsFloat() * time));
				poseStack.mulPose(Axis.YP.rotationDegrees(rotation.get(1).getAsFloat() * time));
				poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.get(2).getAsFloat() * time));
			} catch (Exception e) {
				LOGGER.error("Invalid dynamic rotation data: {}", e.getMessage());
				LOGGER.debug("Stack trace:", e);
			}
		}
	}

	private void applyPulsatingScale(PoseStack poseStack, JsonObject specificAnimation, float time) {
		if (specificAnimation.has("scale_pulse")) {
			try {
				JsonArray scalePulse = specificAnimation.getAsJsonArray("scale_pulse");
				float minScale = scalePulse.get(0).getAsFloat();
				float maxScale = scalePulse.get(1).getAsFloat();
				float speed = scalePulse.get(2).getAsFloat();
				float scaleFactor = minScale + (maxScale - minScale) * ((float) Math.sin(time * speed) + 1) / 2;
				poseStack.scale(scaleFactor, scaleFactor, scaleFactor);
			} catch (Exception e) {
				LOGGER.error("Invalid pulsating scale data: {}", e.getMessage());
				LOGGER.debug("Stack trace:", e);
			}
		}
	}

	private void applyOscillation(PoseStack poseStack, JsonObject specificAnimation, float time) {
		if (specificAnimation.has("oscillation")) {
			try {
				JsonArray oscillation = specificAnimation.getAsJsonArray("oscillation");
				float offsetX = (float) Math.sin(time * oscillation.get(3).getAsFloat()) * oscillation.get(0).getAsFloat();
				float offsetY = (float) Math.sin(time * oscillation.get(3).getAsFloat()) * oscillation.get(1).getAsFloat();
				float offsetZ = (float) Math.sin(time * oscillation.get(3).getAsFloat()) * oscillation.get(2).getAsFloat();
				poseStack.translate(offsetX, offsetY, offsetZ);
			} catch (Exception e) {
				LOGGER.error("Invalid oscillation data: {}", e.getMessage());
				LOGGER.debug("Stack trace:", e);
			}
		}
	}

	private float getCurrentGameTime() {
		return Minecraft.getInstance().player.level().getGameTime() + Minecraft.getInstance().getFrameTime();
	}
}
