package xiaoshi2022.corpseorigin.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 尸兄组合纹理构建器
 * 将玩家皮肤 + 尸化骨骼纹理组合成一张纹理
 */
public class CombinedSkinBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CombinedSkinBuilder.class);

    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 64;

    private static final Map<String, Identifier> CACHE = new ConcurrentHashMap<>();

    // ✅ 使用 fromNamespaceAndPath 创建 Identifier
    private static final Identifier DEFAULT_SKIN = DefaultPlayerSkin.getDefaultTexture();

    // 尸化骨骼叠加纹理
    private static final Identifier SKELETON_OVERLAY =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/lower_level_zb_render.png");

    private static Identifier defaultCombined = null;

    /**
     * 获取组合纹理（皮肤 + 骨骼叠加）
     */
    public static Identifier getOrCreate(String key, Identifier skinTexture) {
        if (key == null || key.isEmpty()) {
            return getDefaultCombined();
        }

        Identifier cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            Identifier combined = buildCombinedSkin(skinTexture);
            CACHE.put(key, combined);
            return combined;
        } catch (Exception e) {
            LOGGER.error("❌ 创建组合纹理失败: {}", key, e);
            return getDefaultCombined();
        }
    }

    /**
     * 构建组合纹理：玩家皮肤 + 骨骼叠加
     */
    private static Identifier buildCombinedSkin(Identifier skinTexture) throws IOException {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        // ✅ 使用正确的 NativeImage 构造方法 (width, height, zero)
        NativeImage combined = new NativeImage(TEXTURE_WIDTH, TEXTURE_HEIGHT, true);

        // 1. 加载玩家皮肤
        NativeImage skinImage = loadSkinImage(skinTexture, resourceManager);
        if (skinImage != null) {
            copyImageToRegion(combined, skinImage, 0, 0);
            skinImage.close();
            LOGGER.debug("✅ 玩家皮肤已加载: {}", skinTexture);
        } else {
            loadDefaultSkinToRegion(combined, resourceManager);
            LOGGER.warn("⚠️ 使用默认皮肤替代: {}", skinTexture);
        }

        // 2. 叠加尸化骨骼纹理（80%透明度）
        NativeImage skeletonImage = loadSkeletonTexture(resourceManager);
        if (skeletonImage != null) {
            overlaySkeletonTexture(combined, skeletonImage);
            skeletonImage.close();
            LOGGER.debug("✅ 骨骼纹理已叠加");
        }

        // 3. 注册组合纹理
        String hash = UUID.randomUUID().toString().substring(0, 8);
        Identifier location = Identifier.fromNamespaceAndPath(
                CorpseOrigin.MOD_ID,
                "skins/zb_combined_" + hash
        );

        // ✅ 修复：使用正确的 DynamicTexture 构造方法 (Supplier<String>, NativeImage)
        DynamicTexture texture = new DynamicTexture(
                () -> location.toString(),
                combined
        );

        // ✅ 修复：TextureManager.register 接收 Identifier 和 AbstractTexture
        textureManager.register(location, texture);
        LOGGER.info("✅ 组合纹理已创建: {}", location);
        return location;
    }

    /**
     * 加载玩家皮肤纹理（支持 CSL 缓存）
     */
    private static NativeImage loadSkinImage(Identifier skin, ResourceManager resourceManager) {
        // 尝试从资源管理器加载
        try {
            var resource = resourceManager.getResource(skin);
            if (resource.isPresent()) {
                try (var input = resource.get().open()) {
                    return NativeImage.read(input);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("从资源管理器加载皮肤失败: {}", skin);
        }

        // 尝试从缓存路径加载（CSL 下载的皮肤）
        Path cachedPath = getSkinFilePath(skin);
        if (cachedPath != null && Files.exists(cachedPath)) {
            try (InputStream input = Files.newInputStream(cachedPath)) {
                LOGGER.debug("从缓存路径加载皮肤: {}", skin);
                return NativeImage.read(input);
            } catch (Exception e) {
                LOGGER.debug("从缓存路径加载失败: {}", e.getMessage());
            }
        }

        // 尝试从纹理管理器获取
        Object texture = Minecraft.getInstance().getTextureManager().getTexture(skin);
        if (texture instanceof DynamicTexture dynamicTexture) {
            NativeImage pixels = dynamicTexture.getPixels();
            if (pixels != null && !pixels.isClosed()) {
                // ✅ 使用 getPixel 和 setPixel（不是 getPixelRGBA/setPixelRGBA）
                NativeImage copy = new NativeImage(pixels.getWidth(), pixels.getHeight(), true);
                for (int x = 0; x < pixels.getWidth(); x++) {
                    for (int y = 0; y < pixels.getHeight(); y++) {
                        copy.setPixel(x, y, pixels.getPixel(x, y));
                    }
                }
                return copy;
            }
        }

        return null;
    }

    /**
     * 加载尸化骨骼纹理
     */
    private static NativeImage loadSkeletonTexture(ResourceManager resourceManager) {
        try {
            var resource = resourceManager.getResource(SKELETON_OVERLAY);
            if (resource.isPresent()) {
                try (var input = resource.get().open()) {
                    return NativeImage.read(input);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("加载骨骼纹理失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 叠加骨骼纹理到皮肤上
     */
    private static void overlaySkeletonTexture(NativeImage combined, NativeImage skeletonImage) {
        int width = Math.min(skeletonImage.getWidth(), TEXTURE_WIDTH);
        int height = Math.min(skeletonImage.getHeight(), TEXTURE_HEIGHT);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // ✅ 使用 getPixel 获取像素（返回 ARGB 格式）
                int skeletonColor = skeletonImage.getPixel(x, y);
                int alpha = (skeletonColor >> 24) & 0xFF;

                if (alpha > 0) {
                    int skinColor = combined.getPixel(x, y);
                    int blendedColor = blendColors(skinColor, skeletonColor, 0.8f);
                    combined.setPixel(x, y, blendedColor);
                }
            }
        }
    }

    /**
     * 颜色混合
     */
    private static int blendColors(int base, int overlay, float alpha) {
        int ba = (base >> 24) & 0xFF;
        int br = (base >> 16) & 0xFF;
        int bg = (base >> 8) & 0xFF;
        int bb = base & 0xFF;

        int oa = (overlay >> 24) & 0xFF;
        int or = (overlay >> 16) & 0xFF;
        int og = (overlay >> 8) & 0xFF;
        int ob = overlay & 0xFF;

        float a = alpha * (oa / 255f);
        int r = (int) (br * (1 - a) + or * a);
        int g = (int) (bg * (1 - a) + og * a);
        int b = (int) (bb * (1 - a) + ob * a);

        return (ba << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 复制图片到区域
     */
    private static void copyImageToRegion(NativeImage dest, NativeImage src, int offsetX, int offsetY) {
        int width = Math.min(src.getWidth(), dest.getWidth() - offsetX);
        int height = Math.min(src.getHeight(), dest.getHeight() - offsetY);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // ✅ 使用 getPixel 和 setPixel
                dest.setPixel(offsetX + x, offsetY + y, src.getPixel(x, y));
            }
        }
    }

    /**
     * 加载默认史蒂夫皮肤到区域
     */
    private static void loadDefaultSkinToRegion(NativeImage combined, ResourceManager resourceManager) {
        try {
            var resource = resourceManager.getResource(DEFAULT_SKIN);
            if (resource.isPresent()) {
                try (var input = resource.get().open()) {
                    NativeImage skinImage = NativeImage.read(input);
                    copyImageToRegion(combined, skinImage, 0, 0);
                    skinImage.close();
                }
            }
        } catch (Exception e) {
            LOGGER.error("加载默认皮肤失败", e);
        }
    }

    /**
     * 获取皮肤文件缓存路径（CSL）
     */
    private static Path getSkinFilePath(Identifier skinLocation) {
        try {
            Path skinRoot = Minecraft.getInstance().getResourcePackDirectory().getParent()
                    .resolve("assets").resolve("skins");
            String path = skinLocation.getPath();
            String hash = path.substring(path.lastIndexOf("/") + 1);
            if (hash.length() > 2) {
                return skinRoot.resolve(hash.substring(0, 2)).resolve(hash);
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }

    /**
     * 获取默认组合纹理
     */
    private static Identifier getDefaultCombined() {
        if (defaultCombined != null) {
            return defaultCombined;
        }

        try {
            defaultCombined = buildCombinedSkin(DEFAULT_SKIN);
            LOGGER.info("✅ 默认组合纹理已创建");
            return defaultCombined;
        } catch (Exception e) {
            LOGGER.error("❌ 创建默认组合纹理失败", e);
            return DEFAULT_SKIN;
        }
    }

    public static void clearCache() {
        CACHE.clear();
        defaultCombined = null;
        LOGGER.info("🧹 已清除组合纹理缓存");
    }
}