package com.phagens.corpseorigin.client.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.UUID;

public class ZbSkinIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean cslLoaded = false;

    static {
        try {
            cslLoaded = ModList.get().isLoaded("customskinloader");
            if (cslLoaded) {
                LOGGER.info("✅ CustomSkinLoader 已安装");
            }
        } catch (Exception e) {
            cslLoaded = false;
        }
    }

    public static ResourceLocation getPlayerSkin(String username) {
        if (!cslLoaded || username == null || username.isEmpty()) {
            return null;
        }

        try {
            UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
            GameProfile profile = new GameProfile(fakeUuid, username);

            Class<?> customSkinLoaderClass = Class.forName("customskinloader.CustomSkinLoader");
            Object userProfile = customSkinLoaderClass.getMethod("loadProfile", GameProfile.class)
                    .invoke(null, profile);

            if (userProfile != null) {
                String skinUrl = (String) userProfile.getClass().getField("skinUrl").get(userProfile);

                if (skinUrl != null && !skinUrl.isEmpty()) {
                    LOGGER.info("📥 CSL 找到皮肤: {} -> {}", username, skinUrl);

                    // 创建纹理对象
                    MinecraftProfileTexture texture = new MinecraftProfileTexture(skinUrl, null);

                    // 使用与 SkinManager 相同的方式注册纹理
                    return registerTextureDirectly(texture, username);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("从 CSL 获取皮肤失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 直接注册纹理，模仿 SkinManager.TextureCache 的方式
     */
    private static ResourceLocation registerTextureDirectly(MinecraftProfileTexture texture, String username) {
        try {
            // 生成哈希和资源路径
            String hash = com.google.common.hash.Hashing.sha1()
                    .hashUnencodedChars(texture.getHash() != null ? texture.getHash() : username)
                    .toString();

            // 创建资源位置 - 使用自定义命名空间
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    "corpseorigin",
                    "skins/" + hash
            );

            // 获取纹理管理器和皮肤根目录
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            Path skinRoot = Minecraft.getInstance().getResourcePackDirectory().getParent()
                    .resolve("assets").resolve("skins");

            // 创建路径 (类似 SkinManager 的路径结构)
            Path skinPath = skinRoot.resolve(hash.length() > 2 ? hash.substring(0, 2) : "xx").resolve(hash);

            // 确保目录存在
            skinPath.toFile().getParentFile().mkdirs();

            // 创建 HttpTexture 并注册
            HttpTexture httpTexture = new HttpTexture(
                    skinPath.toFile(),
                    texture.getUrl(),
                    DefaultPlayerSkin.getDefaultTexture(),
                    true,
                    null  // 不需要回调，因为是同步注册
            );

            textureManager.register(location, httpTexture);

            LOGGER.info("✅ 皮肤已注册: {}", location);
            return location;

        } catch (Exception e) {
            LOGGER.error("注册皮肤失败", e);
            return null;
        }
    }

    public static boolean isCslAvailable() {
        return cslLoaded;
    }
}