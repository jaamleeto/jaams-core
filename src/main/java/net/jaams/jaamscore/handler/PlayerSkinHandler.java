
package net.jaams.jaamscore.handler;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.network.PacketDistributor;

import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.Minecraft;

import net.jaams.jaamscore.packets.SkinSyncPacket;
import net.jaams.jaamscore.JaamsCoreMod;

import javax.annotation.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import java.util.Optional;
import java.util.Map;

import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.net.URL;

import java.io.InputStream;
import java.io.IOException;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.GameProfile;

public class PlayerSkinHandler {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final Map<String, ResourceLocation> skinResourceCache = new ConcurrentHashMap<>();
	public static final Map<String, CompletableFuture<ResourceLocation>> skinCache = new ConcurrentHashMap<>();
	public static final String CONFIG_DIR = "config/jaams/core_textures/";
	public final MinecraftSessionService sessionService = Minecraft.getInstance().getMinecraftSessionService();

	public PlayerSkinHandler() {
		java.nio.file.Path configPath = Paths.get(CONFIG_DIR);
		if (!Files.exists(configPath)) {
			try {
				Files.createDirectories(configPath);
			} catch (IOException e) {
				LOGGER.error("Failed to create config directory: {}", CONFIG_DIR, e);
			}
		}
	}

	public ResourceLocation getSkin(String nameTag, UUID uuid) {
		// Revisa la caché primero
		if (skinResourceCache.containsKey(nameTag)) {
			return skinResourceCache.get(nameTag);
		}
		java.nio.file.Path customSkinPath = Paths.get(CONFIG_DIR, nameTag + ".png");
		if (Files.exists(customSkinPath)) {
			ResourceLocation skin = loadCustomSkin(customSkinPath, nameTag);
			skinResourceCache.put(nameTag, skin); // Almacena en la caché
			return skin;
		}
		CompletableFuture<ResourceLocation> skinFuture = skinCache.computeIfAbsent(nameTag, key -> CompletableFuture.supplyAsync(() -> fetchOrDownloadSkin(nameTag, uuid)));
		ResourceLocation defaultSkin = DefaultPlayerSkin.getDefaultSkin(uuid);
		return skinFuture.getNow(defaultSkin);
	}

	public static ResourceLocation loadCustomSkin(java.nio.file.Path skinPath, String nameTag) {
		try {
			String formattedNameTag = nameTag.toLowerCase();
			NativeImage image = NativeImage.read(Files.newInputStream(skinPath));
			// Procesa la imagen si es del tipo legacy
			if (image.getWidth() == 64 && image.getHeight() == 32) {
				image = processLegacySkin(image);
			}
			DynamicTexture dynamicTexture = new DynamicTexture(image);
			ResourceLocation resourceLocation = new ResourceLocation("jaams_core", "textures/skins/" + formattedNameTag);
			Minecraft.getInstance().getTextureManager().register(resourceLocation, dynamicTexture);
			return resourceLocation;
		} catch (IOException e) {
			LOGGER.error("Error loading custom skin for {}: {}", nameTag, e.getMessage());
		}
		return DefaultPlayerSkin.getDefaultSkin(UUID.randomUUID());
	}

	@Nullable
	private static NativeImage processLegacySkin(NativeImage image) {
		int height = image.getHeight();
		int width = image.getWidth();
		// Comprobar que las dimensiones sean adecuadas (64x32 o 64x64)
		if (width == 64 && (height == 32 || height == 64)) {
			boolean isLegacy = height == 32;
			// Si la imagen es 64x32, crear una nueva imagen 64x64 y copiar píxeles
			if (isLegacy) {
				NativeImage newImage = new NativeImage(64, 64, true);
				for (int y = 0; y < 32; y++) {
					for (int x = 0; x < 64; x++) {
						newImage.setPixelRGBA(x, y, image.getPixelRGBA(x, y));
					}
				}
				image.close(); // Liberar la imagen original
				image = newImage;
				newImage.fillRect(0, 32, 64, 32, 0); // Rellenar parte inferior
				// Copiar regiones de textura de la imagen para convertir a formato 64x64
				newImage.copyRect(4, 16, 16, 32, 4, 4, true, false);
				newImage.copyRect(8, 16, 16, 32, 4, 4, true, false);
				newImage.copyRect(0, 20, 24, 32, 4, 12, true, false);
				newImage.copyRect(4, 20, 16, 32, 4, 12, true, false);
				newImage.copyRect(8, 20, 8, 32, 4, 12, true, false);
				newImage.copyRect(12, 20, 16, 32, 4, 12, true, false);
				newImage.copyRect(44, 16, -8, 32, 4, 4, true, false);
				newImage.copyRect(48, 16, -8, 32, 4, 4, true, false);
				newImage.copyRect(40, 20, 0, 32, 4, 12, true, false);
				newImage.copyRect(44, 20, -8, 32, 4, 12, true, false);
				newImage.copyRect(48, 20, -16, 32, 4, 12, true, false);
				newImage.copyRect(52, 20, -8, 32, 4, 12, true, false);
			}
			// Aplicar transparencia en áreas específicas
			setNoAlpha(image, 0, 0, 32, 16);
			if (isLegacy) {
				doNotchTransparencyHack(image, 32, 0, 64, 32);
			}
			setNoAlpha(image, 0, 16, 64, 32);
			setNoAlpha(image, 16, 48, 48, 64);
		} else {
			// Descartar imágenes que no sean de tamaño adecuado
			image.close();
			LOGGER.warn("Discarding incorrectly sized ({}x{}) skin texture", width, height);
			return null;
		}
		return image;
	}

	private static void setNoAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
		for (int x = x1; x < x2; ++x) {
			for (int y = y1; y < y2; ++y) {
				int color = image.getPixelRGBA(x, y);
				int alpha = (color >> 24) & 0xFF;
				if (alpha < 255) {
					color = 0xFF000000;
				}
				image.setPixelRGBA(x, y, color);
			}
		}
	}

	private static void doNotchTransparencyHack(NativeImage image, int x1, int y1, int x2, int y2) {
		for (int x = x1; x < x2; ++x) {
			for (int y = y1; y < y2; ++y) {
				int alpha = (image.getPixelRGBA(x, y) >> 24) & 0xFF;
				if (alpha < 128) {
					return;
				}
			}
		}
		for (int x = x1; x < x2; ++x) {
			for (int y = y1; y < y2; ++y) {
				image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) & 0x00FFFFFF);
			}
		}
	}

	private DynamicTexture createDynamicTextureFromNativeImage(NativeImage nativeImage) {
		return new DynamicTexture(nativeImage);
	}

	public void syncSkinToAllClients(String nameTag, String skinUrl) {
		SkinSyncPacket packet = new SkinSyncPacket(nameTag, skinUrl);
		JaamsCoreMod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), packet);
	}

	private ResourceLocation fetchOrDownloadSkin(String nameTag, UUID uuid) {
		java.nio.file.Path skinPath = Paths.get(CONFIG_DIR, nameTag.toLowerCase() + ".png");
		if (Files.exists(skinPath)) {
			ResourceLocation skin = loadCustomSkin(skinPath, nameTag);
			skinResourceCache.put(nameTag, skin); // Almacena en la caché
			return skin;
		}
		// Busca texturas en el profile del servidor
		MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
		if (server != null) {
			GameProfileCache profileCache = server.getProfileCache();
			Optional<GameProfile> optionalProfile = profileCache.get(nameTag);
			if (optionalProfile.isPresent()) {
				GameProfile profile = optionalProfile.get();
				sessionService.fillProfileProperties(profile, false);
				Map<Type, MinecraftProfileTexture> textures = sessionService.getTextures(profile, false);
				if (textures.containsKey(Type.SKIN)) {
					MinecraftProfileTexture skinTexture = textures.get(Type.SKIN);
					String skinUrl = skinTexture.getUrl();
					try (InputStream in = new URL(skinUrl).openStream()) {
						Files.copy(in, skinPath, StandardCopyOption.REPLACE_EXISTING);
						ResourceLocation skin = loadCustomSkin(skinPath, nameTag);
						skinResourceCache.put(nameTag, skin); // Almacena en la caché
						// Sincroniza la skin a los clientes
						for (ServerPlayer player : server.getPlayerList().getPlayers()) {
							syncSkinToAllClients(nameTag, skinUrl);
						}
						return skin;
					} catch (IOException e) {
						LOGGER.error("Error downloading skin for {}: {}", nameTag, e.getMessage());
					}
				}
			}
		}
		// Si todo falla, usa la skin por defecto y almacénala en la caché
		ResourceLocation defaultSkin = DefaultPlayerSkin.getDefaultSkin(uuid);
		skinResourceCache.put(nameTag, defaultSkin);
		return defaultSkin;
	}
}
