package net.jaams.jaamscore.handler;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.Advancement;

import net.jaams.jaamscore.configuration.JaamsCoreCommonConfiguration;
import net.jaams.jaamscore.commands.ToggleFireworksCommand;

import java.util.Random;

@Mod.EventBusSubscriber
public class VisualHandler {
	private static final Random RANDOM = new Random();
	private static final int FIREWORK_LAUNCH_DELAY_TICKS = 20;
	private static long lastFireworkLaunchTime = 0;

	@SubscribeEvent
	public static void onAdvancement(AdvancementEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			Advancement advancement = event.getAdvancement();
			DisplayInfo displayInfo = advancement.getDisplay();
			if (displayInfo != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
				FrameType frameType = displayInfo.getFrame();
				ServerLevel level = (ServerLevel) player.level();
				long currentTime = level.getGameTime();
				if (currentTime - lastFireworkLaunchTime < FIREWORK_LAUNCH_DELAY_TICKS) {
					return;
				}
				lastFireworkLaunchTime = currentTime;
				if (frameType == FrameType.TASK && displayInfo.shouldShowToast() || frameType == FrameType.GOAL || frameType == FrameType.CHALLENGE) {
					if (ToggleFireworksCommand.shouldLaunchFireworks(player, frameType)) {
						if (frameType == FrameType.TASK && JaamsCoreCommonConfiguration.TASKFIREWORKS.get()) {
							launchFireworks(level, player, JaamsCoreCommonConfiguration.TASKFIREWORKSAMOUNT.get().intValue(), frameType);
						}
						if (frameType == FrameType.GOAL && JaamsCoreCommonConfiguration.GOALFIREWORKS.get()) {
							launchFireworks(level, player, JaamsCoreCommonConfiguration.GOALFIREWORKSAMOUNT.get().intValue(), frameType);
						}
						if (frameType == FrameType.CHALLENGE && JaamsCoreCommonConfiguration.CHALLENGEFIREWORKS.get()) {
							launchFireworks(level, player, JaamsCoreCommonConfiguration.CHALLENGEFIREWORKSAMOUNT.get().intValue(), frameType);
						}
					}
				}
			}
		}
	}

	private static void launchFireworks(ServerLevel level, ServerPlayer player, int fireworksCount, FrameType frameType) {
		for (int i = 0; i < fireworksCount; i++) {
			ItemStack fireworkStack = createCustomFirework(frameType);
			launchFirework(level, player, fireworkStack);
		}
	}

	private static void launchFirework(ServerLevel level, ServerPlayer player, ItemStack fireworkStack) {
		double x = player.getX();
		double y = player.getY() + player.getBbHeight() / 2;
		double z = player.getZ();
		FireworkRocketEntity fireworkEntity = new FireworkRocketEntity(level, x, y, z, fireworkStack);
		fireworkEntity.setDeltaMovement(0, 0.05, 0);
		level.addFreshEntity(fireworkEntity);
	}

	private static ItemStack createCustomFirework(FrameType frameType) {
		ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
		CompoundTag fireworkTag = new CompoundTag();
		ListTag explosions = new ListTag();
		switch (frameType) {
			case TASK :
				explosions.add(createSimpleExplosion());
				fireworkTag.putByte("Flight", JaamsCoreCommonConfiguration.TASKFIREWORKSFLIGHT.get().byteValue());
				break;
			case GOAL :
				explosions.add(createFancyExplosion());
				fireworkTag.putByte("Flight", JaamsCoreCommonConfiguration.GOALFIREWORKSFLIGHT.get().byteValue());
				break;
			case CHALLENGE :
				explosions.add(createCreeperExplosion());
				fireworkTag.putByte("Flight", JaamsCoreCommonConfiguration.CHALLENGEFIREWORKSFLIGHT.get().byteValue());
				break;
		}
		fireworkTag.put("Explosions", explosions);
		fireworkStack.getOrCreateTag().put("Fireworks", fireworkTag);
		return fireworkStack;
	}

	private static CompoundTag createSimpleExplosion() {
		CompoundTag explosionTag = new CompoundTag();
		explosionTag.putByte("Type", JaamsCoreCommonConfiguration.TASKFIREWORKSTYPE.get().byteValue());
		explosionTag.putIntArray("Colors", new int[]{0xFFFF00});
		explosionTag.putBoolean("Flicker", JaamsCoreCommonConfiguration.TASKFIREWORKSFLICKER.get());
		explosionTag.putBoolean("Trail", JaamsCoreCommonConfiguration.TASKFIREWORKSTRAIL.get());
		return explosionTag;
	}

	private static CompoundTag createFancyExplosion() {
		CompoundTag explosionTag = new CompoundTag();
		explosionTag.putByte("Type", JaamsCoreCommonConfiguration.GOALFIREWORKSTYPE.get().byteValue());
		explosionTag.putIntArray("Colors", new int[]{0xFF0000, 0x00FF00, 0x0000FF});
		explosionTag.putIntArray("FadeColors", new int[]{0xFFFFFF});
		explosionTag.putBoolean("Flicker", JaamsCoreCommonConfiguration.GOALFIREWORKSFLICKER.get());
		explosionTag.putBoolean("Trail", JaamsCoreCommonConfiguration.GOALFIREWORKSTRAIL.get());
		return explosionTag;
	}

	private static CompoundTag createCreeperExplosion() {
		CompoundTag explosionTag = new CompoundTag();
		int[] colors = {0x800080, 0xFF0000, 0xFFFF00};
		int randomColor = colors[(int) (Math.random() * colors.length)];
		explosionTag.putByte("Type", JaamsCoreCommonConfiguration.CHALLENGEFIREWORKSTYPE.get().byteValue());
		explosionTag.putIntArray("Colors", new int[]{randomColor});
		explosionTag.putIntArray("FadeColors", new int[]{0x000000});
		explosionTag.putBoolean("Flicker", JaamsCoreCommonConfiguration.CHALLENGEFIREWORKSFLICKER.get());
		explosionTag.putBoolean("Trail", JaamsCoreCommonConfiguration.CHALLENGEFIREWORKSTRAIL.get());
		return explosionTag;
	}
}
