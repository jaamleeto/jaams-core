package net.jaams.jaamscore.handler;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.Minecraft;

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

@OnlyIn(Dist.CLIENT)
public class CorePlayerSkinHandler {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Map<String, CompletableFuture<ResourceLocation>> skinCache = new ConcurrentHashMap<>();
	private static final String CONFIG_DIR = "config/jaams/core_textures/";
	private final MinecraftSessionService sessionService = Minecraft.getInstance().getMinecraftSessionService();

	public CorePlayerSkinHandler() {
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
		java.nio.file.Path customSkinPath = Paths.get(CONFIG_DIR, nameTag + ".png");
		if (Files.exists(customSkinPath)) {
			return loadCustomSkin(customSkinPath, nameTag);
		}
		CompletableFuture<ResourceLocation> skinFuture = skinCache.computeIfAbsent(nameTag, key -> CompletableFuture.supplyAsync(() -> fetchOrDownloadSkin(nameTag, uuid)));
		return skinFuture.getNow(DefaultPlayerSkin.getDefaultSkin(uuid));
	}

	private ResourceLocation loadCustomSkin(java.nio.file.Path skinPath, String nameTag) {
		try {
			String formattedNameTag = nameTag.toLowerCase();
			NativeImage image = NativeImage.read(Files.newInputStream(skinPath));
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
		if (width == 64 && (height == 32 || height == 64)) {
			boolean isLegacy = height == 32;
			if (isLegacy) {
				NativeImage newImage = new NativeImage(64, 64, true);
				for (int y = 0; y < 32; y++) {
					for (int x = 0; x < 64; x++) {
						newImage.setPixelRGBA(x, y, image.getPixelRGBA(x, y));
					}
				}
				image.close();
				image = newImage;
				newImage.fillRect(0, 32, 64, 32, 0);
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
			setNoAlpha(image, 0, 0, 32, 16);
			if (isLegacy) {
				doNotchTransparencyHack(image, 32, 0, 64, 32);
			}
			setNoAlpha(image, 0, 16, 64, 32);
			setNoAlpha(image, 16, 48, 48, 64);
		} else {
			image.close();
			LOGGER.warn("Discarding incorrectly sized ({}x{}) skin texture", width, height);
			return null;
		}
		return image;
	}

	private static void setNoAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
		for (int x = x1; x < x2; ++x) {
			for (int y = y1; y < y2; ++y) {
				image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) | 0xFF000000);
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

	private ResourceLocation fetchOrDownloadSkin(String nameTag, UUID uuid) {
		java.nio.file.Path skinPath = Paths.get(CONFIG_DIR, nameTag.toLowerCase() + ".png");
		if (Files.exists(skinPath)) {
			return loadCustomSkin(skinPath, uuid.toString());
		}
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
					ResourceLocation skinLocation = Minecraft.getInstance().getSkinManager().registerTexture(skinTexture, Type.SKIN);
					try (InputStream in = new URL(skinTexture.getUrl()).openStream()) {
						Files.copy(in, skinPath, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						LOGGER.error("Error downloading skin for {}: {}", nameTag, e.getMessage());
					}
					return skinLocation;
				}
			}
		}
		return DefaultPlayerSkin.getDefaultSkin(uuid);
	}
}
