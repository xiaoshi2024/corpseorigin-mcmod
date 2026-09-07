package xiaoshi2022.corpseorigin.client.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class ZbSkinIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean cslLoaded = false;

    static {
        try {
            cslLoaded = FabricLoader.getInstance().isModLoaded("customskinloader");
            if (cslLoaded) {
                LOGGER.info("✅ CustomSkinLoader 已安装（将自动拦截皮肤加载）");
            }
        } catch (Exception e) {
            cslLoaded = false;
            LOGGER.warn("CustomSkinLoader 检测失败: {}", e.getMessage());
        }
    }

    /**
     * 通过 Minecraft 的 SkinManager 触发 CSL 加载皮肤
     */
    public static CompletableFuture<Identifier> getPlayerSkinAsync(String username) {
        if (username == null || username.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
                GameProfile profile = new GameProfile(fakeUuid, username);

                SkinManager skinManager = Minecraft.getInstance().getSkinManager();

                // ✅ 方式1：使用 get() 方法获取皮肤（异步）
                CompletableFuture<Optional<PlayerSkin>> skinFuture = skinManager.get(profile);

                // 等待加载完成
                Optional<PlayerSkin> skinOptional = skinFuture.join();

                if (skinOptional.isPresent()) {
                    PlayerSkin skin = skinOptional.get();
                    Identifier texturePath = skin.body().texturePath();
                    LOGGER.info("📥 SkinManager 加载皮肤: {} -> {}", username, texturePath);
                    return texturePath;
                }

                // 方式2：如果上面的方法失败，尝试直接调用 CSL API
                if (cslLoaded) {
                    try {
                        Class<?> cslClass = Class.forName("customskinloader.CustomSkinLoader");
                        Object userProfile = cslClass.getMethod("loadProfile", GameProfile.class)
                                .invoke(null, profile);

                        if (userProfile != null) {
                            String skinUrl = (String) userProfile.getClass().getField("skinUrl").get(userProfile);
                            if (skinUrl != null && !skinUrl.isEmpty()) {
                                LOGGER.info("📥 CSL API 找到皮肤: {} -> {}", username, skinUrl);
                                return downloadAndRegisterSkin(skinUrl, username);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("CSL API 调用失败: {}", e.getMessage());
                    }
                }

                // 方式3：回退到默认皮肤
                LOGGER.info("⚠️ 使用默认皮肤: {}", username);
                return DefaultPlayerSkin.get(fakeUuid).body().texturePath();

            } catch (Exception e) {
                LOGGER.warn("获取皮肤失败: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * 下载并注册皮肤纹理
     */
    public static Identifier downloadAndRegisterSkin(String skinUrl, String username) {
        try {
            String hash = com.google.common.hash.Hashing.sha1()
                    .hashUnencodedChars(skinUrl)
                    .toString();

            Identifier location = Identifier.fromNamespaceAndPath(
                    "corpseorigin",
                    "skins/" + hash
            );

            TextureManager textureManager = Minecraft.getInstance().getTextureManager();

            if (textureManager.getTexture(location) != null) {
                return location;
            }

            HttpURLConnection connection = (HttpURLConnection) URI.create(skinUrl).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                try (InputStream inputStream = connection.getInputStream()) {
                    NativeImage image = NativeImage.read(inputStream);

                    if (image.getWidth() == 64 && image.getHeight() == 32) {
                        image = SkinProcessor.convertLegacySkin(image);
                    }

                    final NativeImage finalImage = image;
                    DynamicTexture texture = new DynamicTexture(
                            () -> location.toString(),
                            finalImage
                    );

                    textureManager.register(location, texture);
                    LOGGER.info("✅ 皮肤已注册: {}", location);
                    return location;
                }
            }
            connection.disconnect();

        } catch (Exception e) {
            LOGGER.error("注册皮肤失败: {}", e.getMessage(), e);
        }

        return null;
    }

    public static boolean isCslAvailable() {
        return cslLoaded;
    }
}