
package net.jaams.jaamscore.handler;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;

import net.jaams.jaamscore.configuration.JaamsCoreCommonConfiguration;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TwoHandedHandler {
	private static final Map<UUID, ItemStack> offHandItems = new HashMap<>();

	private static boolean isValidTwoHandedItem(ItemStack itemStack) {
		if (!JaamsCoreCommonConfiguration.TWOHANDED.get()) {
			return false;
		}
		ResourceLocation itemResourceLocation = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
		for (String stringIterator : JaamsCoreCommonConfiguration.TWOHANDEDBLACKLIST.get()) {
			ResourceLocation resourceLocation = new ResourceLocation(stringIterator);
			if (itemResourceLocation != null && itemResourceLocation.equals(resourceLocation)) {
				return false;
			}
		}
		for (String stringIterator : JaamsCoreCommonConfiguration.TWOHANDEDWHITELIST.get()) {
			ResourceLocation resourceLocation = new ResourceLocation(stringIterator);
			if (itemResourceLocation != null && itemResourceLocation.equals(resourceLocation)) {
				return true;
			}
		}
		return false;
	}

	public TwoHandedHandler() {
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new TwoHandedHandler();
	}

	@Mod.EventBusSubscriber
	private static class OffhandHandlerForgeBusEvents {
		@SubscribeEvent
		public static void serverLoad(ServerStartingEvent event) {
		}

		@OnlyIn(Dist.CLIENT)
		@SubscribeEvent
		public static void clientLoad(FMLClientSetupEvent event) {
		}

		@SubscribeEvent
		public static void onPlayerOffhandInteract(PlayerInteractEvent.RightClickItem event) {
			Player player = event.getEntity();
			if (!player.level().isClientSide) {
				ItemStack mainHandStack = player.getMainHandItem();
				if (JaamsCoreCommonConfiguration.TWOHANDED.get() && isValidTwoHandedItem(mainHandStack)) {
					if (event.getHand() == InteractionHand.OFF_HAND) {
						event.setCanceled(true);
					}
				}
			}
		}

		@SubscribeEvent
		public static void onPlayerOffhandUseItem(PlayerInteractEvent.RightClickBlock event) {
			Player player = event.getEntity();
			if (!player.level().isClientSide) {
				ItemStack mainHandStack = player.getMainHandItem();
				if (JaamsCoreCommonConfiguration.TWOHANDED.get() && isValidTwoHandedItem(mainHandStack)) {
					if (event.getHand() == InteractionHand.OFF_HAND) {
						event.setCanceled(true);
					}
				}
			}
		}

		@SubscribeEvent
		public static void onPlayerOffhandUseItemFinish(LivingEntityUseItemEvent.Start event) {
			if (event.getEntity() instanceof Player) {
				Player player = (Player) event.getEntity();
				if (!player.level().isClientSide) {
					ItemStack mainHandStack = player.getMainHandItem();
					if (JaamsCoreCommonConfiguration.TWOHANDED.get() && isValidTwoHandedItem(mainHandStack)) {
						if (player.getUsedItemHand() == InteractionHand.OFF_HAND) {
							event.setCanceled(true);
						}
					}
				}
			}
		}

		@SubscribeEvent
		public static void onOffhandLivingDeath(LivingDeathEvent event) {
			LivingEntity entity = event.getEntity();
			if (entity instanceof ServerPlayer) {
				ServerPlayer player = (ServerPlayer) entity;
				boolean keepInventory = player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
				UUID playerId = player.getUUID();
				if (offHandItems.containsKey(playerId)) {
					ItemStack restoredItem = offHandItems.remove(playerId);
					if (keepInventory) {
						player.setItemInHand(InteractionHand.OFF_HAND, restoredItem);
					} else {
						if (ModList.get().isLoaded("corpse")) {
							player.setItemInHand(InteractionHand.OFF_HAND, restoredItem);
						} else {
							player.drop(restoredItem, false);
						}
					}
				}
			}
		}

		@SubscribeEvent
		public static void onOffhandPlayerTick(TickEvent.PlayerTickEvent event) {
			Player player = event.player;
			UUID playerId = player.getUUID();
			ItemStack mainHandItem = player.getMainHandItem();
			ItemStack offHandItem = player.getOffhandItem();
			boolean keepInventory = player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
			if (!player.isAlive() || player.level().isClientSide) {
				return;
			}
			if (ModList.get().isLoaded("inventorio") && JaamsCoreCommonConfiguration.TWOHANDED.get()) {
				if (!offHandItem.isEmpty() && isValidTwoHandedItem(offHandItem)) {
					player.setItemInHand(InteractionHand.OFF_HAND, mainHandItem.copy());
					player.setItemInHand(InteractionHand.MAIN_HAND, offHandItem.copy());
					mainHandItem = player.getMainHandItem();
					offHandItem = player.getOffhandItem();
				}
				return;
			}
			if (!JaamsCoreCommonConfiguration.TWOHANDED.get()) {
				if (offHandItems.containsKey(playerId)) {
					ItemStack restoredItem = offHandItems.remove(playerId);
					if (player.getOffhandItem().isEmpty()) {
						player.setItemInHand(InteractionHand.OFF_HAND, restoredItem);
					} else {
						player.drop(restoredItem, false);
					}
				}
				return;
			}
			if (!offHandItem.isEmpty() && isValidTwoHandedItem(offHandItem)) {
				player.setItemInHand(InteractionHand.OFF_HAND, mainHandItem.copy());
				player.setItemInHand(InteractionHand.MAIN_HAND, offHandItem.copy());
				mainHandItem = player.getMainHandItem();
				offHandItem = player.getOffhandItem();
			}
			if (!mainHandItem.isEmpty() && isValidTwoHandedItem(mainHandItem)) {
				if (!offHandItem.isEmpty() && !offHandItems.containsKey(playerId)) {
					offHandItems.put(playerId, offHandItem.copy());
					player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
					player.swing(InteractionHand.OFF_HAND, true);
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
				}
			} else {
				if (offHandItems.containsKey(playerId)) {
					ItemStack restoredItem = offHandItems.remove(playerId);
					if (player.getOffhandItem().isEmpty()) {
						player.setItemInHand(InteractionHand.OFF_HAND, restoredItem);
						player.swing(InteractionHand.OFF_HAND, true);
						player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
					} else {
						player.drop(restoredItem, false);
					}
				}
			}
		}

		@OnlyIn(Dist.CLIENT)
		@SubscribeEvent
		public static void pressKeyInGuiOffhand(ScreenEvent.KeyPressed.Pre event) {
			Minecraft minecraft = Minecraft.getInstance();
			if (event.getKeyCode() == minecraft.options.keySwapOffhand.getKey().getValue()) {
				Player player = minecraft.player;
				if (player != null) {
					ItemStack mainHandItem = player.getMainHandItem();
					boolean mainHandIsTwoHanded = isValidTwoHandedItem(mainHandItem);
					if (event.getScreen() instanceof AbstractContainerScreen) {
						Slot slot = ((AbstractContainerScreen<?>) event.getScreen()).getSlotUnderMouse();
						boolean slotItemIsTwoHanded = (slot != null && slot.hasItem() && isValidTwoHandedItem(slot.getItem()));
						if ((mainHandIsTwoHanded || slotItemIsTwoHanded) && JaamsCoreCommonConfiguration.TWOHANDED.get()) {
							event.setCanceled(true);
						}
					}
				}
			}
		}

		@OnlyIn(Dist.CLIENT)
		@SubscribeEvent
		public static void onTickOffhand(TickEvent.ClientTickEvent event) {
			Minecraft mc = Minecraft.getInstance();
			if (event.phase == TickEvent.Phase.START && mc.player != null) {
				if (mc.options.keySwapOffhand.isDown()) {
					ItemStack mainHandItem = mc.player.getMainHandItem();
					if (isValidTwoHandedItem(mainHandItem) && JaamsCoreCommonConfiguration.TWOHANDED.get()) {
						mc.options.keySwapOffhand.setDown(false);
						mc.options.keySwapOffhand.consumeClick();
					}
				}
			}
		}

		@OnlyIn(Dist.CLIENT)
		@SubscribeEvent
		public static void onKeyInputOffhand(InputEvent.Key event) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.options.keySwapOffhand.matches(event.getKey(), event.getScanCode())) {
				ItemStack mainHandItem = mc.player.getMainHandItem();
				if (isValidTwoHandedItem(mainHandItem) && JaamsCoreCommonConfiguration.TWOHANDED.get()) {
					mc.options.keySwapOffhand.consumeClick();
				}
			}
		}
	}
}
