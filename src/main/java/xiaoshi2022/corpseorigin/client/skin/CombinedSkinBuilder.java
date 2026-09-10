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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 尸兄组合纹理构建器
 * <p>
 * - 使用 LRU 缓存限制纹理数量
 * - 引用计数管理纹理生命周期
 * - 皮肤路径作 key，同一皮肤复用
 */
public class CombinedSkinBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CombinedSkinBuilder.class);

    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 64;

    /** ✅ LRU 缓存最大容量 */
    private static final int MAX_CACHE_SIZE = 64;

    /**
     * ✅ LRU 缓存
     * key = 皮肤路径字符串
     * value = 组合纹理 + 引用计数
     */
    private static final LinkedHashMap<String, CacheEntry> CACHE =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                    if (size() > MAX_CACHE_SIZE && eldest.getValue().refCount <= 0) {
                        // ✅ 引用计数为 0 才释放
                        releaseTexture(eldest.getValue().texture);
                        LOGGER.info("🗑️ LRU 释放纹理: {}", eldest.getValue().texture);
                        return true;
                    }
                    return false;
                }
            };

    private static final Identifier DEFAULT_SKIN = DefaultPlayerSkin.getDefaultTexture();

    private static final Identifier SKELETON_OVERLAY =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/lower_level_zb_render.png");

    private static Identifier defaultCombined = null;

    /** 缓存的条目：组合纹理 + 引用计数 */
    private static class CacheEntry {
        final Identifier texture;
        int refCount;

        CacheEntry(Identifier texture) {
            this.texture = texture;
            this.refCount = 1;
        }
    }

    // ==================== 公共 API ====================

    /**
     * ✅ 获取组合纹理（增加引用计数）
     * <p>
     * 相同皮肤路径复用同一个组合纹理。
     */
    public static synchronized Identifier acquire(Identifier skinTexture) {
        if (skinTexture == null) {
            return getDefaultCombined();
        }

        String cacheKey = skinTexture.toString();
        CacheEntry entry = CACHE.get(cacheKey);

        if (entry != null) {
            entry.refCount++;
            LOGGER.debug("♻️ 复用组合纹理: {} (ref={})", entry.texture, entry.refCount);
            return entry.texture;
        }

        // 创建新纹理
        try {
            Identifier combined = buildCombinedSkin(skinTexture);
            CACHE.put(cacheKey, new CacheEntry(combined));
            LOGGER.info("✅ 组合纹理已创建: {} -> {} (ref=1)", skinTexture, combined);
            return combined;
        } catch (Exception e) {
            LOGGER.error("❌ 创建组合纹理失败: {}", cacheKey, e);
            return getDefaultCombined();
        }
    }

    /**
     * ✅ 释放组合纹理（减少引用计数）
     * <p>
     * 当引用计数为 0 时，纹理保留在缓存中，等 LRU 淘汰时释放。
     */
    public static synchronized void release(Identifier skinTexture) {
        if (skinTexture == null) return;

        String cacheKey = skinTexture.toString();
        CacheEntry entry = CACHE.get(cacheKey);
        if (entry != null) {
            entry.refCount = Math.max(0, entry.refCount - 1);
            LOGGER.debug("➖ 释放组合纹理引用: {} (ref={})", entry.texture, entry.refCount);
        }
    }

    /**
     * ✅ 手动释放某个皮肤的组合纹理（强制）
     * <p>
     * 用于实体死亡时清理。
     */
    public static synchronized void forceRelease(Identifier skinTexture) {
        if (skinTexture == null) return;

        String cacheKey = skinTexture.toString();
        CacheEntry entry = CACHE.remove(cacheKey);
        if (entry != null) {
            releaseTexture(entry.texture);
            LOGGER.info("🗑️ 强制释放组合纹理: {}", entry.texture);
        }
    }

    /**
     * ✅ 清空所有缓存
     */
    public static synchronized void clearCache() {
        for (CacheEntry entry : CACHE.values()) {
            releaseTexture(entry.texture);
        }
        CACHE.clear();
        defaultCombined = null;
        LOGGER.info("🧹 已清除所有组合纹理缓存");
    }

    /**
     * ✅ 获取当前缓存大小（调试用）
     */
    public static synchronized int getCacheSize() {
        return CACHE.size();
    }

    // ==================== 内部实现 ====================

    /**
     * 释放纹理（从 TextureManager 注销）
     */
    private static void releaseTexture(Identifier location) {
        if (location == null) return;
        try {
            Minecraft.getInstance().getTextureManager().release(location);
        } catch (Exception e) {
            LOGGER.warn("释放纹理失败: {} - {}", location, e.getMessage());
        }
    }

    /**
     * 构建组合纹理：玩家皮肤 + 骨骼叠加
     */
    private static Identifier buildCombinedSkin(Identifier skinTexture) throws IOException {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        NativeImage combined = new NativeImage(TEXTURE_WIDTH, TEXTURE_HEIGHT, true);

        // 1. 加载玩家皮肤
        NativeImage skinImage = loadSkinImage(skinTexture, resourceManager);
        if (skinImage != null) {
            copyImageToRegion(combined, skinImage, 0, 0);
            skinImage.close();
        } else {
            loadDefaultSkinToRegion(combined, resourceManager);
            LOGGER.warn("⚠️ 使用默认皮肤替代: {}", skinTexture);
        }

        // 2. 叠加尸化骨骼纹理
        NativeImage skeletonImage = loadSkeletonTexture(resourceManager);
        if (skeletonImage != null) {
            overlaySkeletonTexture(combined, skeletonImage);
            skeletonImage.close();
        }

        // 3. 注册组合纹理（用确定性 hash，避免随机 UUID）
        String hash = Integer.toHexString(skinTexture.toString().hashCode());
        Identifier location = Identifier.fromNamespaceAndPath(
                CorpseOrigin.MOD_ID,
                "skins/zb_combined_" + hash
        );

        // ✅ 如果已存在同名纹理，先释放（避免泄漏）
        if (textureManager.getTexture(location) != null) {
            textureManager.release(location);
        }

        DynamicTexture texture = new DynamicTexture(
                () -> location.toString(),
                combined
        );

        textureManager.register(location, texture);
        return location;
    }

    private static NativeImage loadSkinImage(Identifier skin, ResourceManager resourceManager) {
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

        Path cachedPath = getSkinFilePath(skin);
        if (cachedPath != null && Files.exists(cachedPath)) {
            try (InputStream input = Files.newInputStream(cachedPath)) {
                return NativeImage.read(input);
            } catch (Exception e) {
                LOGGER.debug("从缓存路径加载失败: {}", e.getMessage());
            }
        }

        Object texture = Minecraft.getInstance().getTextureManager().getTexture(skin);
        if (texture instanceof DynamicTexture dynamicTexture) {
            NativeImage pixels = dynamicTexture.getPixels();
            if (pixels != null && !pixels.isClosed()) {
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

    private static void overlaySkeletonTexture(NativeImage combined, NativeImage skeletonImage) {
        int width = Math.min(skeletonImage.getWidth(), TEXTURE_WIDTH);
        int height = Math.min(skeletonImage.getHeight(), TEXTURE_HEIGHT);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
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

    private static void copyImageToRegion(NativeImage dest, NativeImage src, int offsetX, int offsetY) {
        int width = Math.min(src.getWidth(), dest.getWidth() - offsetX);
        int height = Math.min(src.getHeight(), dest.getHeight() - offsetY);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                dest.setPixel(offsetX + x, offsetY + y, src.getPixel(x, y));
            }
        }
    }

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

    /** 兼容旧 API */
    @Deprecated
    public static Identifier getOrCreate(Identifier skinTexture) {
        return acquire(skinTexture);
    }
}