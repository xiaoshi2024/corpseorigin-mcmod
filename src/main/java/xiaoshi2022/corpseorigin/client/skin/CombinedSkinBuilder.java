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
import java.util.concurrent.ConcurrentHashMap;

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
     * ✅ 色调变体分桶数。
     * <p>
     * 以前每个尸兄的名字 hash 直接当变体号 → 每只尸兄一张专属组合纹理，全部在渲染线程同步构建；
     * 切屏回来的一帧里新进视野的尸兄集体首次渲染，集体建纹理，造成明显卡顿。
     * <p>
     * 现在把变体收敛到有限个桶（{@link #tintBucket}）：同名永远同桶同色调（确定性不变），
     * 不同名字落入有限桶 —— 暖机（最多 桶数×2 皮肤×2 cracked 种组合）之后新尸兄全是缓存命中，
     * 一张纹理都不用再建，切屏回来不再有纹理构建尖峰。
     */
    public static final int TINT_BUCKETS = 24;

    /** 名字 hash → 变体桶号（0..TINT_BUCKETS-1）。所有尸系渲染器共用，保证头和身体色调一致。 */
    public static int tintBucket(int nameHash) {
        return Math.floorMod(nameHash, TINT_BUCKETS);
    }

    /**
     * ✅ LRU 缓存
     * key = 皮肤路径 + "#" + 变体号
     * value = 组合纹理 + 引用计数
     * <p>
     * 变体号由 {@link #tintBucket} 收敛到有限桶，条目总数有上界（皮肤数 × 桶数 × cracked），
     * 不再随尸兽数量线性增长。
     */
    private static final LinkedHashMap<String, CacheEntry> CACHE =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                    if (size() > MAX_CACHE_SIZE && eldest.getValue().refCount <= 0) {
                        // ✅ 引用计数为 0 才释放
                        releaseTexture(eldest.getValue().texture);
                        return true;
                    }
                    return false;
                }
            };

    private static final Identifier DEFAULT_SKIN = DefaultPlayerSkin.getDefaultTexture();

    private static final Identifier SKELETON_OVERLAY =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/lower_level_zb_render.png");

    private static final Identifier CRACKED_OVERLAY = CorpseOrigin.id("textures/entity/lower_level_zb_cracked.png");

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
    public static synchronized Identifier acquire(Identifier skinTexture, int variant) { return acquire(skinTexture,variant,false); }

    public static synchronized Identifier acquire(Identifier skinTexture, int variant, boolean cracked) {
        if (skinTexture == null) {
            return getDefaultCombined();
        }

        String cacheKey = cacheKey(skinTexture, variant) + (cracked ? "#cracked" : "");
        CacheEntry entry = CACHE.get(cacheKey);

        if (entry != null) {
            entry.refCount++;
            LOGGER.debug("♻️ 复用组合纹理: {} (ref={})", entry.texture, entry.refCount);
            return entry.texture;
        }

        // 创建新纹理
        try {
            Identifier combined = buildCombinedSkin(skinTexture, variant, cracked);
            CACHE.put(cacheKey, new CacheEntry(combined));
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
    public static synchronized void release(Identifier skinTexture, int variant) { release(skinTexture,variant,false); }

    public static synchronized void release(Identifier skinTexture, int variant, boolean cracked) {
        if (skinTexture == null) return;

        String cacheKey = cacheKey(skinTexture, variant) + (cracked ? "#cracked" : "");
        CacheEntry entry = CACHE.get(cacheKey);
        if (entry != null) {
            entry.refCount = Math.max(0, entry.refCount - 1);
            // ✅ 引用归零且缓存超容：立即从 TextureManager 注销。
            //    LRU 只在 put 时逐出且每次最多逐一个 —— 若不在这里补一刀，
            //    大量已卸载实体的纹理要等下一次"新建"才被慢慢挤出去，纹理数只增不减。
            if (entry.refCount == 0 && CACHE.size() > MAX_CACHE_SIZE) {
                CACHE.remove(cacheKey);
                releaseTexture(entry.texture);
                LOGGER.debug("🧹 缓存超容，立即释放无引用纹理: {}", entry.texture);
            } else {
                LOGGER.debug("➖ 释放组合纹理引用: {} (ref={})", entry.texture, entry.refCount);
            }
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
    private static Identifier buildCombinedSkin(Identifier skinTexture, int variant) throws IOException { return buildCombinedSkin(skinTexture,variant,false); }

    /**
     * ✅ 构建组合纹理。
     * <p>
     * 构建在渲染线程同步执行，以前每建一张都要重新读两张 PNG（默认皮肤 + 尸化骨骼层），
     * 切屏回来一批新尸兄首次渲染时在同一帧里集体走这条路径，卡顿明显。
     * 现在这些源图像经 {@link #loadSharedResourceImage} 进程级缓存，重复构建只剩像素拷贝与 GL 上传。
     */
    private static Identifier buildCombinedSkin(Identifier skinTexture, int variant, boolean cracked) throws IOException {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        NativeImage combined = new NativeImage(TEXTURE_WIDTH, TEXTURE_HEIGHT, true);

        // 1. 加载玩家皮肤：资源包命中走共享缓存（禁 close）；否则读磁盘缓存文件 / 拷贝动态纹理像素（自持，用完 close）
        NativeImage sharedSkin = loadSharedResourceImage(resourceManager, skinTexture);
        if (sharedSkin != null) {
            copyImageToRegion(combined, sharedSkin, 0, 0);
        } else {
            NativeImage ownedSkin = loadOwnedSkinImage(skinTexture, resourceManager);
            if (ownedSkin != null) {
                copyImageToRegion(combined, ownedSkin, 0, 0);
                ownedSkin.close();
            } else {
                loadDefaultSkinToRegion(combined, resourceManager);
                LOGGER.warn("⚠️ 使用默认皮肤替代: {}", skinTexture);
            }
        }

        // 1.5 按变体给基础皮肤上"尸化色调"（必须在叠骨骼层之前，否则骨骼也会被染色）
        applyVariantTint(combined, variant);

        // 2. 叠加尸化骨骼纹理（共享缓存图像，禁 close）
        NativeImage skeletonImage = loadSharedResourceImage(resourceManager,
                cracked ? CRACKED_OVERLAY : SKELETON_OVERLAY);
        if (skeletonImage != null) {
            overlaySkeletonTexture(combined, skeletonImage);
        }

        // 2.5 血管贴图：掉落头模型三根 vein cube 共用 UV[16,16]，该区域原本是玩家
        //     皮肤的帽子层 —— 必须重画成暗红血管肉丝，否则血管渲染成皮肤色
        paintVeinTexture(combined);

        // 3. 注册组合纹理（用确定性 hash，避免随机 UUID）
        String hash = Integer.toHexString(skinTexture.toString().hashCode())
                + "_" + Integer.toHexString(variant) + (cracked ? "_cracked" : "");
        Identifier location = Identifier.fromNamespaceAndPath(
                CorpseOrigin.MOD_ID,
                "skins/zb_combined_" + hash
        );

        // register replaces and closes an existing texture. getTexture would instead
        // try loading this generated identifier from disk and emit missing-resource warnings.

        DynamicTexture texture = new DynamicTexture(
                () -> location.toString(),
                combined
        );

        textureManager.register(location, texture);
        return location;
    }

    /**
     * 掉落头实体（severed_zb_head.geo.json）脖子下的三根血管 cube（vein_left/right/back）
     * 共用 UV[16,16] 的 4×5 像素区域（1×4×1 box UV）—— 该区域在玩家皮肤里是帽子层，
     * 不重画的话血管会渲染成皮肤色。这里画出暗红肉质底 + 亮红血丝 + 焦黑血痂的肉丝纹理。
     */
    private static void paintVeinTexture(NativeImage combined) {
        for (int x = 16; x < 20; x++) {
            for (int y = 16; y < 21; y++) {
                int shade = (x * 7 + y * 13 + (x ^ y)) % 5; // 确定性伪随机，避免每次合成纹理不同
                int abgr = switch (shade) {
                    case 0 -> 0xFF3A0A04; // 焦黑血痂
                    case 1, 2 -> 0xFF6B0F08; // 暗红肉质
                    default -> 0xFFB01A10; // 亮红血丝
                };
                combined.setPixel(x, y, abgr);
            }
        }
    }

    /**
     * ✅ 资源包图像缓存：骨骼叠加层、默认皮肤这类每次构建都要用的 PNG 只解码一次。
     * <p>
     * 只缓存能从 {@link ResourceManager} 命中的键（原版皮肤 steve/alex、模组贴图），
     * 这类键天然只有个位数；HTTP 下载 / 动态纹理不进这里。
     * 缓存里的图像为<b>共享只读</b>，调用方一律不得 close。
     */
    private static final Map<Identifier, NativeImage> RESOURCE_IMAGES = new ConcurrentHashMap<>();

    /** 从共享缓存取资源包图像，未命中返回 {@code null}（调用方自行回退）。返回图像禁 close。 */
    private static NativeImage loadSharedResourceImage(ResourceManager resourceManager, Identifier id) {
        if (id == null) return null;
        return RESOURCE_IMAGES.computeIfAbsent(id, key -> {
            try {
                var resource = resourceManager.getResource(key);
                if (resource.isPresent()) {
                    try (var input = resource.get().open()) {
                        return NativeImage.read(input);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("从资源管理器加载图像失败: {}", key);
            }
            return null;   // computeIfAbsent 不缓存 null，下次仍会重试
        });
    }

    /**
     * 加载"自持"皮肤图像（磁盘皮肤缓存文件 / 已注册动态纹理的像素拷贝），调用方负责 close。
     * 资源包命中的情况已在 {@link #loadSharedResourceImage} 处理。
     */
    private static NativeImage loadOwnedSkinImage(Identifier skin, ResourceManager resourceManager) {
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

    /**
     * 按变体号给基础皮肤上一层"尸化色调"。
     * <p>
     * 变体号是名字 hash 经 {@link #tintBucket} 收敛出的<b>有限桶号</b>（见 {@code LowerLevelZbRenderer}）：
     * 同一个名字永远同一个色调，桶内共享同一张组合纹理 —— 这样缓存才能真正命中，
     * 不会每只尸兄都现建一张。三个通道各乘一个由变体派生的系数（并整体偏"失血发青"一点），
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
        NativeImage skinImage = loadSharedResourceImage(resourceManager, DEFAULT_SKIN);
        if (skinImage != null) {
            copyImageToRegion(combined, skinImage, 0, 0);
        } else {
            LOGGER.error("加载默认皮肤失败");
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
