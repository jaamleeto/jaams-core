package net.jaams.jaamscore.packets;

import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.FriendlyByteBuf;

import net.jaams.jaamscore.manager.ScaleManager;
import net.jaams.jaamscore.JaamsCoreMod;

import java.util.function.Supplier;
import java.util.UUID;

@Mod.EventBusSubscriber
public class ScaleSyncPacket {
	private final UUID entityUUID;
	private final float scale;

	public ScaleSyncPacket(UUID entityUUID, float scale) {
		this.entityUUID = entityUUID;
		this.scale = scale;
	}

	public static void encode(ScaleSyncPacket packet, FriendlyByteBuf buffer) {
		buffer.writeUUID(packet.entityUUID);
		buffer.writeFloat(packet.scale);
	}

	public static ScaleSyncPacket decode(FriendlyByteBuf buffer) {
		return new ScaleSyncPacket(buffer.readUUID(), buffer.readFloat());
	}

	public static void sendToAllTracking(Entity entity, float scale) {
		if (entity.level() instanceof ServerLevel serverLevel) {
			serverLevel.getPlayers(player -> player.hasLineOfSight(entity)).forEach(player -> JaamsCoreMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new ScaleSyncPacket(entity.getUUID(), scale)));
		}
	}

	public static void handle(ScaleSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			if (context.getDirection().getReceptionSide().isClient()) {
				Entity entity = null;
				if (entity != null) {
					ScaleManager.setScale(packet.entityUUID, packet.scale);
				}
			}
		});
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			JaamsCoreMod.PACKET_HANDLER.registerMessage(0, ScaleSyncPacket.class, ScaleSyncPacket::encode, ScaleSyncPacket::decode, ScaleSyncPacket::handle);
		});
	}
}
