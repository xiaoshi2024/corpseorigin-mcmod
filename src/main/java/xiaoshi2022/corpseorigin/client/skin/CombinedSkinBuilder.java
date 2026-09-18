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
import xiaoshi2022.corpseorigin.config.CorpseConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

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

    /**
     * ✅ LRU 缓存最大容量
     * <p>
     * 注意现在的 key 是"皮肤路径 + 变体号"：每个尸兄的 ID 都会派生出一个变体，
     * 所以条目数比"一种皮肤一条"多得多，这里放宽一些（每条只有 64×64 RGBA ≈ 16KB）。
     */
    private static final int MAX_CACHE_SIZE = 128;

    /**
     * ✅ LRU 缓存
     * key = 皮肤路径 + "#" + 变体号
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
        return acquire(skinTexture, 0);
    }

    /**
     * ✅ 获取组合纹理，并指定"变体号"（按玩家 ID 派生）。
     * <p>
     * 变体号的作用：<b>查不到真实皮肤时，让每只尸兄长得都不一样</b> ——
     * 同一个变体永远产出同一个色调（可缓存、可复用），不同变体各不同。
     * 变体 0 表示"不染色"（默认皮肤、兜底纹理走这条）。
     */
    public static synchronized Identifier acquire(Identifier skinTexture, int variant) {
        if (skinTexture == null) {
            return getDefaultCombined();
        }

        String cacheKey = cacheKey(skinTexture, variant);
        CacheEntry entry = CACHE.get(cacheKey);

        if (entry != null) {
            entry.refCount++;
            LOGGER.debug("♻️ 复用组合纹理: {} (ref={})", entry.texture, entry.refCount);
            return entry.texture;
        }

        // 创建新纹理
        try {
            Identifier combined = buildCombinedSkin(skinTexture, variant);
            CACHE.put(cacheKey, new CacheEntry(combined));
            LOGGER.info("✅ 组合纹理已创建: {} (variant={}) -> {} (ref=1)", skinTexture, variant, combined);
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
        release(skinTexture, 0);
    }

    /** ✅ 释放组合纹理（带变体号，和 {@link #acquire(Identifier, int)} 配对使用） */
    public static synchronized void release(Identifier skinTexture, int variant) {
        if (skinTexture == null) return;

        String cacheKey = cacheKey(skinTexture, variant);
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
        forceRelease(skinTexture, 0);
    }

    /** ✅ 强制释放（带变体号） */
    public static synchronized void forceRelease(Identifier skinTexture, int variant) {
        if (skinTexture == null) return;

        CacheEntry entry = CACHE.remove(cacheKey(skinTexture, variant));
        if (entry != null) {
            releaseTexture(entry.texture);
            LOGGER.info("🗑️ 强制释放组合纹理: {}", entry.texture);
        }
    }

    /** 缓存 key：皮肤路径 + 变体号 */
    private static String cacheKey(Identifier skinTexture, int variant) {
        return skinTexture + "#" + variant;
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
     * 构建组合纹理：玩家皮肤（按变体上色）+ 骨骼叠加
     */
    private static Identifier buildCombinedSkin(Identifier skinTexture, int variant) throws IOException {
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

        // 1.5 按变体给基础皮肤上"尸化色调"（必须在叠骨骼层之前，否则骨骼也会被染色）
        applyVariantTint(combined, variant);

        // 2. 叠加尸化骨骼纹理
        NativeImage skeletonImage = loadSkeletonTexture(resourceManager);
        if (skeletonImage != null) {
            overlaySkeletonTexture(combined, skeletonImage);
            skeletonImage.close();
        }

        // 3. 注册组合纹理（用确定性 hash，避免随机 UUID）
        String hash = Integer.toHexString(skinTexture.toString().hashCode())
                + "_" + Integer.toHexString(variant);
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

    /**
     * 按变体号给基础皮肤上一层"尸化色调"，让每只尸兄长得都不一样。
     * <p>
     * 变体号是从尸兄的玩家 ID 派生的（见 {@code LowerLevelZbRenderer}）：同一个 ID 永远同一个色调，
     * 不同 ID 各不相同。三个通道各乘一个由变体派生的系数（并整体偏"失血发青"一点），
     * 所以出来的是<b>稳定的、可缓存的</b>差异，而不是每次渲染都变。
     * <p>
     * 变体 0 = 不染色：默认皮肤与兜底纹理走这条，保持原样。
     */
    private static void applyVariantTint(NativeImage image, int variant) {
        if (variant == 0) {
            return;
        }
        // 强度来自配置（skin.tintStrength）：1 = 默认，0 = 不染，2 = 更夸张
        float strength = CorpseConfig.get().skin.tintStrength;
        if (strength <= 0.0F) {
            return;
        }

        // 先打散，避免相邻的变体号出来太像
        int hash = variant * 0x9E3779B9;
        float redScale = tint(0.78F + ((hash >>> 8) & 0xFF) / 255F * 0.25F, strength);
        float greenScale = tint(0.80F + ((hash >>> 16) & 0xFF) / 255F * 0.30F, strength);
        float blueScale = tint(0.78F + ((hash >>> 24) & 0xFF) / 255F * 0.30F, strength);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int color = image.getPixel(x, y);
                int alpha = (color >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;   // 透明像素不动
                }
                int r = Math.min(255, (int) (((color >>> 16) & 0xFF) * redScale));
                int g = Math.min(255, (int) (((color >>> 8) & 0xFF) * greenScale));
                int b = Math.min(255, (int) ((color & 0xFF) * blueScale));
                image.setPixel(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    /** 把"相对 1.0 的偏移量"按强度缩放：strength=1 保持原样，0 等于不染，>1 更夸张 */
    private static float tint(float baseScale, float strength) {
        return 1.0F + (baseScale - 1.0F) * strength;
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
            defaultCombined = buildCombinedSkin(DEFAULT_SKIN, 0);
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