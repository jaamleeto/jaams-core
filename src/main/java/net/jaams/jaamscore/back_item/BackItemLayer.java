
package net.jaams.jaamscore.back_item;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.Minecraft;

import net.jaams.jaamscore.network.JaamsCoreModVariables;
import net.jaams.jaamscore.configuration.JaamsCoreCommonConfiguration;
import net.jaams.jaamscore.configuration.JaamsCoreClientConfiguration;

import java.util.Map;
import java.util.HashMap;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;

@OnlyIn(Dist.CLIENT)
public class BackItemLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
	private final RenderLayerParent<T, M> pRenderer;
	private final Map<Class<? extends Item>, TriConsumer<PoseStack, T, ItemStack>> itemTransformers = new HashMap<>();
	private final Map<TagKey<Item>, TriConsumer<PoseStack, T, ItemStack>> tagTransformers = new HashMap<>();

	public BackItemLayer(RenderLayerParent<T, M> pRenderer) {
		super(pRenderer);
		this.pRenderer = pRenderer;
		initializeTransformers();
	}

	private void initializeTransformers() {
		// Inicializar el mapa de clases
		addItemTransformer(SwordItem.class, this::handleSword);
		addItemTransformer(BowItem.class, this::handleBow);
		addItemTransformer(ShieldItem.class, this::handleShields);
		addItemTransformer(TridentItem.class, this::handleTrident);
		addItemTransformer(PickaxeItem.class, this::handleTool);
		addItemTransformer(ShovelItem.class, this::handleTool);
		addItemTransformer(HoeItem.class, this::handleTool);
		addItemTransformer(AxeItem.class, this::handleAxe);
		addItemTransformer(CrossbowItem.class, this::handleCrossbow);
		// Inicializar el mapa de tags
		addTagTransformer("jaams_weaponry:mini_weapons", this::handleMiniWeapons);
		addTagTransformer("jaams_weaponry:farmers_knives_compat", this::handleSword);
		addTagTransformer("jaams_weaponry:shotguns", this::handleShotgun);
		addTagTransformer("jaams_weaponry:pistols", this::handlePistol);
		addTagTransformer("jaams_weaponry:scatterguns", this::handlePistol);
		addTagTransformer("jaams_weaponry:back_weapon_compat", this::handleSword);
		addTagTransformer("jaams_weaponry:back_bow_compat", this::handleBow);
		addTagTransformer("jaams_weaponry:back_pistol_compat", this::handlePistol);
		addTagTransformer("jaams_weaponry:back_shotgun_compat", this::handleShotgun);
	}

	private void addItemTransformer(Class<? extends Item> itemClass, TriConsumer<PoseStack, T, ItemStack> transformer) {
		itemTransformers.put(itemClass, transformer);
	}

	private void addTagTransformer(String tagName, TriConsumer<PoseStack, T, ItemStack> transformer) {
		tagTransformers.put(ItemTags.create(new ResourceLocation(tagName)), transformer);
	}

	private void setupPositionAndRotation(PoseStack matrixStack, T entity, ItemStack stack) {
		matrixStack.translate(0, 0.35d, 0.16d);
		Item item = stack.getItem();
		ResourceLocation itemRegistryName = BuiltInRegistries.ITEM.getKey(item);
		if (JaamsCoreClientConfiguration.RENDERFIXED.get()) {
			handleCompatItem(matrixStack, entity, stack);
			return;
		}
		if (itemRegistryName != null && JaamsCoreCommonConfiguration.BACKWHITELIST.get().contains(itemRegistryName.toString())) {
			handleCompatItem(matrixStack, entity, stack);
			return;
		}
		TriConsumer<PoseStack, T, ItemStack> specificTransformer = itemTransformers.get(item.getClass());
		if (specificTransformer != null) {
			specificTransformer.accept(matrixStack, entity, stack);
			return;
		}
		for (Map.Entry<TagKey<Item>, TriConsumer<PoseStack, T, ItemStack>> entry : tagTransformers.entrySet()) {
			if (stack.is(entry.getKey())) {
				entry.getValue().accept(matrixStack, entity, stack);
				return;
			}
		}
		for (Map.Entry<Class<? extends Item>, TriConsumer<PoseStack, T, ItemStack>> entry : itemTransformers.entrySet()) {
			if (entry.getKey().isInstance(item)) {
				entry.getValue().accept(matrixStack, entity, stack);
				return;
			}
		}
	}

	@Override
	public void render(PoseStack matrixStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		entity.getCapability(JaamsCoreModVariables.PLAYER_VARIABLES_CAPABILITY).ifPresent(capability -> {
			ItemStack mainWeapon = capability.MainWeapon;
			// Renderizar el MainWeapon
			if (!mainWeapon.isEmpty()) {
				matrixStack.pushPose();
				HumanoidModel<T> model = pRenderer.getModel();
				model.body.translateAndRotate(matrixStack);
				setupPositionAndRotation(matrixStack, entity, mainWeapon);
				renderItem(matrixStack, bufferSource, packedLight, entity, mainWeapon);
				matrixStack.popPose();
			}
		});
		// Renderizar una espada de diamante para entidades que no sean el jugador
		if (!(entity instanceof Player)) {
			ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
			matrixStack.pushPose();
			HumanoidModel<T> model = pRenderer.getModel();
			model.body.translateAndRotate(matrixStack);
			setupPositionAndRotation(matrixStack, entity, diamondSword);
			renderItem(matrixStack, bufferSource, packedLight, entity, diamondSword);
			matrixStack.popPose();
		}
	}

	private void renderItem(PoseStack matrixStack, MultiBufferSource bufferSource, int packedLight, T entity, ItemStack stack) {
		ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
		BakedModel model = itemRenderer.getItemModelShaper().getItemModel(stack);
		ItemDisplayContext transformType = getTransformType(stack);
		itemRenderer.renderStatic(stack, transformType, packedLight, OverlayTexture.NO_OVERLAY, matrixStack, bufferSource, entity.level(), entity.getId());
	}

	private ItemDisplayContext getTransformType(ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (JaamsCoreCommonConfiguration.BACKWHITELIST.get().contains(registryName.toString())) {
			return ItemDisplayContext.FIXED;
		} else if (stack.getItem() instanceof ShieldItem) {
			return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
		} else if (JaamsCoreClientConfiguration.RENDERFIXED.get()) {
			return ItemDisplayContext.FIXED;
		}
		return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

	private void handleCompatItem(PoseStack matrixStack, T entity, ItemStack stack) {
		ResourceLocation itemRegistryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		boolean isWhitelisted = itemRegistryName != null && JaamsCoreCommonConfiguration.BACKWHITELIST.get().contains(itemRegistryName.toString());
		boolean renderFixed = JaamsCoreClientConfiguration.RENDERFIXED.get();
		// Aplicar escala
		matrixStack.scale(0.65f, 0.65f, 0.65f);
		if (hasArmor(entity)) {
			matrixStack.translate(0, 0, 0.03d);
		}
		if (isWhitelisted || renderFixed) {
			if (stack.getItem() instanceof ShieldItem) {
				handleShields(matrixStack, entity, stack);
			} else if (isItemWeapon(stack)) {
				if (hasArmor(entity)) {
					matrixStack.translate(0, 0, 0.07d);
				}
				matrixStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));
				matrixStack.translate(0.1, 0.01, 0.0);
			} else {
				if (hasArmor(entity)) {
					matrixStack.translate(0, 0, 0.07d);
				}
				matrixStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
				matrixStack.translate(0.0, -0.0, 0.0);
			}
		} else {
			TriConsumer<PoseStack, T, ItemStack> specificTransformer = itemTransformers.get(stack.getItem().getClass());
			if (specificTransformer != null) {
				specificTransformer.accept(matrixStack, entity, stack);
			} else {
				matrixStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
				matrixStack.translate(0.0, -0.0, 0.0);
			}
		}
	}

	private void handleShields(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		ResourceLocation itemRegistryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		boolean isWhitelisted = itemRegistryName != null && JaamsCoreCommonConfiguration.BACKWHITELIST.get().contains(itemRegistryName.toString());
		boolean renderFixed = JaamsCoreClientConfiguration.RENDERFIXED.get();
		// Ajustar la escala
		float scale = isWhitelisted || renderFixed ? 0.95f : 0.75f;
		matrixStack.scale(scale, scale, scale);
		if (hasArmor(livingEntity))
			matrixStack.translate(0, 0, 0.03d);
		matrixStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
		matrixStack.translate(0.0, 0.15, -0.25);
	}

	private void handleMiniWeapons(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.9f, 0.9f, 0.9f);
		if (hasArmor(livingEntity))
			matrixStack.translate(0, 0, 0.03d);
		matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.XN.rotationDegrees(-55.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.ZN.rotationDegrees(0.0f));
		matrixStack.translate(0.0, -0.28, -0.01);
	}

	private void handlePistol(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.9f, 0.9f, 0.9f);
		if (hasArmor(livingEntity))
			matrixStack.translate(0, 0, 0.03d);
		matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.XN.rotationDegrees(-130.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.ZN.rotationDegrees(0.0f));
		matrixStack.translate(-0.02, 0.1, 0.2);
	}

	private void handleShotgun(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.9f, 0.9f, 0.9f);
		if (hasArmor(livingEntity))
			matrixStack.translate(0, 0, 0.03d);
		matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.XN.rotationDegrees(-60.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.ZN.rotationDegrees(0.0f));
		matrixStack.translate(-0.01, -0.2, -0.01);
	}

	private void handleHuntersBoomerang(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.9f, 0.9f, 0.9f);
		if (hasArmor(livingEntity))
			matrixStack.translate(0, 0, 0.03d);
		matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.XN.rotationDegrees(-55.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.ZN.rotationDegrees(0.0f));
		matrixStack.translate(0.0, -0.3, -0.05);
	}

	private void handleNunchaku(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.9f, 0.9f, 0.9f);
		if (hasArmor(livingEntity))
			matrixStack.translate(0, 0, 0.03d);
		matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.XN.rotationDegrees(-55.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.ZN.rotationDegrees(0.0f));
		matrixStack.translate(0.0, -0.08, 0.0);
	}

	private void handleSword(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.9f, 0.9f, 0.9f);
		if (hasArmor(livingEntity)) {
			matrixStack.translate(0, 0, 0.03d);
		}
		if (registryName != null && registryName.equals(new ResourceLocation("farmersdelight:skillet"))) {
			matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
			matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
			matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
			matrixStack.mulPose(Axis.XN.rotationDegrees(-45.0f));
			matrixStack.mulPose(Axis.YN.rotationDegrees(190.0f));
			matrixStack.mulPose(Axis.ZN.rotationDegrees(0.0f));
			matrixStack.translate(0.08, -0.6, -0.1);
		} else {
			matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
			matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
			matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
			matrixStack.mulPose(Axis.XN.rotationDegrees(-55.0f));
			matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
			matrixStack.mulPose(Axis.ZN.rotationDegrees(0.0f));
			matrixStack.translate(0.0, -0.28, -0.01);
		}
	}

	private void handleTrident(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(1.0f, 1.0f, 1.0f);
		matrixStack.mulPose(Axis.ZN.rotationDegrees(90.0f));
		if (hasArmor(livingEntity))
			matrixStack.translate(0, 0, 0.03d);
		matrixStack.mulPose(Axis.ZN.rotationDegrees(135.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(60.0f));
		matrixStack.translate(0, 0.2, -0.07);
	}

	private void handleTool(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		float translateX = JaamsCoreClientConfiguration.TRANSLATEX.get().floatValue();
		float translateY = JaamsCoreClientConfiguration.TRANSLATEY.get().floatValue();
		float translateZ = JaamsCoreClientConfiguration.TRANSLATEZ.get().floatValue();
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.9f, 0.9f, 0.9f);
		if (hasArmor(livingEntity)) {
			matrixStack.translate(translateX, translateY, translateZ);
		}
		matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.XN.rotationDegrees(-55.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.ZN.rotationDegrees(0.0f));
		matrixStack.translate(-0.01, -0.27, -0.04);
	}

	private void handleAxe(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.9f, 0.9f, 0.9f);
		if (hasArmor(livingEntity))
			matrixStack.translate(0, 0, 0.03d);
		matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.XN.rotationDegrees(-55.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.ZN.rotationDegrees(0.0f));
		matrixStack.translate(-0.01, -0.27, -0.04);
	}

	private void handleShortBow(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.8f, 0.8f, 0.8f);
		if (hasArmor(livingEntity)) {
		}
		matrixStack.translate(0, 0, 0.03d);
		matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.XN.rotationDegrees(140.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.ZN.rotationDegrees(10.0f));
		matrixStack.translate(0.07, 0.14, -0.2);
	}

	private void handleCrossbow(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		float translateX = JaamsCoreClientConfiguration.TRANSLATEX.get().floatValue();
		float translateY = JaamsCoreClientConfiguration.TRANSLATEY.get().floatValue();
		float translateZ = JaamsCoreClientConfiguration.TRANSLATEZ.get().floatValue();
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.9f, 0.9f, 0.9f);
		if (hasArmor(livingEntity)) {
		}
		matrixStack.translate(translateX, translateY, translateZ);
		matrixStack.mulPose(Axis.XP.rotationDegrees(90.0f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(-30.0f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.XN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.ZN.rotationDegrees(0.0f));
		matrixStack.translate(-0.14, 0.0, 0.09);
	}

	private void handleBow(PoseStack matrixStack, LivingEntity livingEntity, ItemStack stack) {
		float translateX = JaamsCoreClientConfiguration.TRANSLATEX.get().floatValue();
		float translateY = JaamsCoreClientConfiguration.TRANSLATEY.get().floatValue();
		float translateZ = JaamsCoreClientConfiguration.TRANSLATEZ.get().floatValue();
		ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(stack.getItem());
		matrixStack.scale(0.8f, 0.8f, 0.8f);
		if (hasArmor(livingEntity)) {
			matrixStack.translate(translateX, translateY, translateZ);
		}
		matrixStack.mulPose(Axis.XP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
		matrixStack.mulPose(Axis.ZP.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.XN.rotationDegrees(140.0f));
		matrixStack.mulPose(Axis.YN.rotationDegrees(0.0f));
		matrixStack.mulPose(Axis.ZN.rotationDegrees(10.0f));
		matrixStack.translate(0.07, 0.14, -0.1);
	}

	private boolean hasArmor(LivingEntity livingEntity) {
		return livingEntity.hasItemInSlot(EquipmentSlot.CHEST);
	}

	private static boolean isItemWeapon(ItemStack itemStack) {
		return !(itemStack.getItem() instanceof CrossbowItem || itemStack.getItem() instanceof BowItem) && itemStack.getItem().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)
				&& itemStack.getItem().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_SPEED);
	}
}
