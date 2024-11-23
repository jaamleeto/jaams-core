
package net.jaams.jaamscore.client.renderer;

import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidArmorModel;

import net.jaams.jaamscore.handler.PlayerSkinHandler;
import net.jaams.jaamscore.entity.CorePlayerEntity;
import net.jaams.jaamscore.custom_models.ExtendedPlayerModel;
import net.jaams.jaamscore.back_item.BackItemLayer;

import com.mojang.blaze3d.vertex.PoseStack;

public class CorePlayerRenderer extends HumanoidMobRenderer<CorePlayerEntity, ExtendedPlayerModel<CorePlayerEntity>> {
	private final ExtendedPlayerModel<CorePlayerEntity> steveModel;
	private final ExtendedPlayerModel<CorePlayerEntity> alexModel;
	private final RenderLayer<CorePlayerEntity, ExtendedPlayerModel<CorePlayerEntity>> steveArmorLayer;
	private final RenderLayer<CorePlayerEntity, ExtendedPlayerModel<CorePlayerEntity>> alexArmorLayer;
	private final PlayerSkinHandler skinHandler;
	private final int armorLayerIndex;
	// Capas adicionales
	private final BackItemLayer<CorePlayerEntity, ExtendedPlayerModel<CorePlayerEntity>> backItemLayer;

	public CorePlayerRenderer(EntityRendererProvider.Context context) {
		super(context, new ExtendedPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), false), 0.5f);
		// Modelos de Alex y Steve con ExtendedPlayerModel
		this.alexModel = new ExtendedPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
		this.steveModel = new ExtendedPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), true);
		// Inicialización de skinHandler
		this.skinHandler = new PlayerSkinHandler(); // Or obtain it from context if applicable
		// Capas de armadura específicas para cada modelo
		steveArmorLayer = new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager());
		alexArmorLayer = new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)), new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)), context.getModelManager());
		// Instancias de capas adicionales
		this.backItemLayer = new BackItemLayer<>(this);
		// Añadir capas adicionales siempre activas
		//this.addLayer(new BeeStingerLayer<>(this));
		this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
		// Índice de la capa de armadura para referencia
		armorLayerIndex = layers.size() - 1;
	}

	@Override
	public void render(CorePlayerEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
		// Selección de modelo y capa de armadura según el nombre
		String nameTag = entity.getName().getString();
		boolean isSteve = isUppercase(nameTag);
		model = isSteve ? steveModel : alexModel;
		// Remover capas de armadura previas
		layers.remove(steveArmorLayer);
		layers.remove(alexArmorLayer);
		// Añadir capa de armadura según el modelo
		layers.add(armorLayerIndex, isSteve ? steveArmorLayer : alexArmorLayer);
		// Manejo de capas de BackItem y Arrow según el estado de baby
		if (entity.isBaby()) {
			layers.remove(backItemLayer);
		} else {
			if (!layers.contains(backItemLayer)) {
				this.addLayer(backItemLayer);
			}
		}
		// Reset de las poses de los brazos y configuración de poses según ítem en mano
		resetArmPoses();
		setHandPose(entity);
		super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
	}

	private boolean isUppercase(String name) {
		return name.equals(name.toUpperCase());
	}

	private void resetArmPoses() {
		model.leftArmPose = ExtendedPlayerModel.ArmPose.EMPTY;
		model.rightArmPose = ExtendedPlayerModel.ArmPose.EMPTY;
	}

	@Override
	public ResourceLocation getTextureLocation(CorePlayerEntity entity) {
		return skinHandler.getSkin(entity.getName().getString(), entity.getUUID());
	}

	private void setHandPose(CorePlayerEntity entity) {
		// Pose para la mainhand
		applyArmPose(entity, entity.getMainHandItem(), entity.getMainArm() == HumanoidArm.RIGHT);
		// Pose para la offhand
		applyArmPose(entity, entity.getOffhandItem(), entity.getMainArm() != HumanoidArm.RIGHT);
	}

	private void applyArmPose(CorePlayerEntity entity, ItemStack stack, boolean isRightHand) {
		HumanoidModel.ArmPose pose = HumanoidModel.ArmPose.EMPTY;
		if (!stack.isEmpty()) {
			pose = HumanoidModel.ArmPose.ITEM;
			// Siempre usa la pose CROSSBOW_HOLD si la ballesta está cargada
			if (stack.getItem() instanceof CrossbowItem) {
				if (CrossbowItem.isCharged(stack)) {
					pose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
				} else if (entity.isChargingCrossbow()) {
					pose = HumanoidModel.ArmPose.CROSSBOW_CHARGE;
				}
			} else if (stack.getItem() instanceof BowItem && entity.isAggressive()) {
				pose = HumanoidModel.ArmPose.BOW_AND_ARROW;
			} else if (stack.getUseAnimation() == UseAnim.BLOCK) {
				pose = HumanoidModel.ArmPose.BLOCK;
			} else if (stack.getUseAnimation() == UseAnim.SPYGLASS) {
				pose = HumanoidModel.ArmPose.SPYGLASS;
			} else if (stack.getUseAnimation() == UseAnim.TOOT_HORN) {
				pose = HumanoidModel.ArmPose.TOOT_HORN;
			} else if (stack.getUseAnimation() == UseAnim.BRUSH) {
				pose = HumanoidModel.ArmPose.BRUSH;
			}
		}
		// Aplica la pose al brazo correspondiente
		if (isRightHand) {
			model.rightArmPose = pose;
		} else {
			model.leftArmPose = pose;
		}
	}
}
