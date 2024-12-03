package net.jaams.jaamscore.packets;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.network.FriendlyByteBuf;

import net.jaams.jaamscore.handler.CorePlayerSkinHandler;
import net.jaams.jaamscore.JaamsCoreMod;

import java.util.function.Supplier;
import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CorePlayerSkinPacket {
	private static final Logger LOGGER = LogManager.getLogger();
	private final UUID playerUUID;
	private final String nameTag;

	public CorePlayerSkinPacket(UUID playerUUID, String nameTag) {
		this.playerUUID = playerUUID;
		this.nameTag = nameTag;
	}

	public CorePlayerSkinPacket(FriendlyByteBuf buffer) {
		this.playerUUID = buffer.readUUID();
		this.nameTag = buffer.readUtf(32767);
	}

	public void toBytes(FriendlyByteBuf buffer) {
		buffer.writeUUID(playerUUID);
		buffer.writeUtf(nameTag);
	}

	public void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> {
			if (context.get().getDirection().getReceptionSide().isClient()) {
				CorePlayerSkinHandler.fetchOrDownloadSkin(nameTag, playerUUID);
			}
		});
		context.get().setPacketHandled(true);
	}

	public static void updateSkin(String nameTag, UUID entityUUID) {
		LOGGER.info("[CorePlayerSkinPacket] Enviando paquete con nameTag: {}, UUID: {}", nameTag, entityUUID);
		JaamsCoreMod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new CorePlayerSkinPacket(entityUUID, nameTag));
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		JaamsCoreMod.addNetworkMessage(CorePlayerSkinPacket.class, CorePlayerSkinPacket::toBytes, CorePlayerSkinPacket::new, CorePlayerSkinPacket::handle);
	}
}
