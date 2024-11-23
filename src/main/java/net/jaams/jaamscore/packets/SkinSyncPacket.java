package net.jaams.jaamscore.packets;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.network.FriendlyByteBuf;

import net.jaams.jaamscore.handler.PlayerSkinHandler;
import net.jaams.jaamscore.JaamsCoreMod;

import java.util.function.Supplier;

import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.net.URL;

import java.io.InputStream;
import java.io.IOException;

@Mod.EventBusSubscriber
public class SkinSyncPacket {
	private final String nameTag;
	private final String skinUrl;

	public SkinSyncPacket(String nameTag, String skinUrl) {
		this.nameTag = nameTag;
		this.skinUrl = skinUrl;
	}

	public static void encode(SkinSyncPacket packet, FriendlyByteBuf buffer) {
		buffer.writeUtf(packet.nameTag);
		buffer.writeUtf(packet.skinUrl);
	}

	public static SkinSyncPacket decode(FriendlyByteBuf buffer) {
		String nameTag = buffer.readUtf();
		String skinUrl = buffer.readUtf();
		return new SkinSyncPacket(nameTag, skinUrl);
	}

	public static void handle(SkinSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			try {
				// Descarga la skin en el cliente
				java.nio.file.Path skinPath = Paths.get(PlayerSkinHandler.CONFIG_DIR, packet.nameTag.toLowerCase() + ".png");
				try (InputStream in = new URL(packet.skinUrl).openStream()) {
					Files.copy(in, skinPath, StandardCopyOption.REPLACE_EXISTING);
				}
				// Carga la skin en el cliente
				PlayerSkinHandler.loadCustomSkin(skinPath, packet.nameTag);
			} catch (IOException e) {
			}
		});
		ctx.get().setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			JaamsCoreMod.PACKET_HANDLER.registerMessage(0, SkinSyncPacket.class, SkinSyncPacket::encode, SkinSyncPacket::decode, SkinSyncPacket::handle);
		});
	}
}
