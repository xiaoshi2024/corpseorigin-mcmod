package xiaoshi2022.corpseorigin.client.skin;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 左护法变异体纹理构建器：{@code zuo_guardian.png} 当底图，骑手处叠上玩家皮肤。
 * <p>
 * 做法和低阶尸兄的 {@link CombinedSkinBuilder} 是同一套路（两张图合一张、注册成 DynamicTexture、
 * 按皮肤路径缓存复用），只是叠加方式不同：
 * <ul>
 *   <li>底图 512×512（{@code zuo_guardian.png}）—— 巨蛇、胡须、牙齿等全部照旧；</li>
 *   <li>{@code zuo_guardian.geo.json} 里的骑手
 *       {@code riderx → Waist/Head/Body/Right Arm/Left Arm/Right Leg/Left Leg} 是<b>按原版玩家盒尺寸</b>
 *       做的（8×8×8、8×12×4、4×12×4），但它的 UV 位置<b>不是</b>原版皮肤布局
 *       （躯干取样在 (0,32)、右臂在 (32,16)……底图上画的内容正好落在这套 UV 上）。
 *       所以不能像尸兄那样"整张 64×64 原地叠"，而是<b>按部件搬</b>：
 *       把原版皮肤的头 / 躯干 / 左右臂 / 左右腿（含外层）各自那张展开网格，
 *       整块搬到骑手对应部件的 UV 原点（见 {@link #RIDER_PARTS}）。</li>
 *   <li>只覆盖皮肤上<b>不透明</b>的像素（alpha 混合），全透明像素保留底图 ——
 *       玩家皮肤没画到的地方不会出现空洞。</li>
 * </ul>
 * 因为搬的是"整块展开网格"，源和目标的盒尺寸又完全一致，所以面朝哪边之类的映射约定两边天然相同，
 * 不需要逐面算 UV。
 * <p>
 * ⚠️ 在 Blockbench 里改了骑手的 UV 之后，务必回来核对 {@link #RIDER_PARTS}（和 {@code LimbSlots} 一个道理：
 * 对不上不会报错，只是贴花错位）。
 * <p>
 * 玩家的皮肤路径对本人是零延迟现成的（{@code AvatarRenderState.skin}），别的玩家也由原版
 * {@code AvatarRenderer} 填好了，所以这里不需要异步皮肤链。
 */
@Environment(EnvType.CLIENT)
public final class ZuoGuardianSkinBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZuoGuardianSkinBuilder.class);

    /**
     * 骑手的一个部件：往底图的哪个 UV 原点搬、从原版皮肤的哪个 UV 原点取、盒子多大。
     * <p>
     * 源是原版 64×64 皮肤的标准布局（1.8+）：头 (0,0)、帽层 (32,0)、躯干 (16,16)、衣层 (16,32)、
     * 右臂 (40,16)、右袖 (40,32)、左臂 (32,48)、左袖 (48,48)、右腿 (0,16)、右裤 (0,32)、
     * 左腿 (16,48)、左裤 (0,48)。
     *
     * @param dstU      底图上的目标 UV 原点 X
     * @param dstV      底图上的目标 UV 原点 Y
     * @param srcU      原版皮肤上的来源 UV 原点 X
     * @param srcV      原版皮肤上的来源 UV 原点 Y
     * @param boxWidth  盒子宽（像素）
     * @param boxHeight 盒子高
     * @param boxDepth  盒子厚
     */
    private record RiderPart(int dstU, int dstV, int srcU, int srcV,
                             int boxWidth, int boxHeight, int boxDepth) {

        /** Bedrock 盒展开网格：宽 = 2×(宽+厚)，高 = 厚+高（角上空出来的两格本来就不参与取样） */
        int netWidth() {
            return 2 * (boxWidth + boxDepth);
        }

        int netHeight() {
            return boxHeight + boxDepth;
        }
    }

    /** 骑手各部件在底图上的位置 —— 对应 {@code zuo_guardian.geo.json} 里 riderx 那套骨头的 cube uv */
    private static final RiderPart[] RIDER_PARTS = {
            new RiderPart(0, 0, 0, 0, 8, 8, 8),        // Head
            new RiderPart(31, 0, 32, 0, 8, 8, 8),      // Head（外层帽子，inflate 0.5）
            new RiderPart(0, 32, 16, 16, 8, 12, 4),    // Body
            new RiderPart(32, 0, 16, 32, 8, 12, 4),    // Body（外层衣服，inflate 0.25）
            new RiderPart(32, 16, 40, 16, 4, 12, 4),   // Right Arm
            new RiderPart(24, 32, 40, 32, 4, 12, 4),   // Right Arm（外层袖子）
            new RiderPart(40, 32, 32, 48, 4, 12, 4),   // Left Arm
            new RiderPart(0, 48, 48, 48, 4, 12, 4),    // Left Arm（外层袖子）
            new RiderPart(32, 48, 0, 16, 4, 12, 4),    // Right Leg
            new RiderPart(48, 48, 0, 32, 4, 12, 4),    // Right Leg（外层裤子）
            new RiderPart(16, 48, 16, 48, 4, 12, 4),   // Left Leg
            new RiderPart(48, 16, 0, 48, 4, 12, 4),    // Left Leg（外层裤子）
    };

    private static final Identifier BASE_TEXTURE =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/entity/zuo_guardian.png");

    /** 组合纹理缓存上限：每条 512×512 RGBA ≈ 1MB，别开太大 */
    private static final int MAX_CACHE_SIZE = 32;

    /** key = 玩家皮肤路径，value = 组合纹理；LRU，满了淘汰最久未用的（并注销它的纹理） */
    private static final LinkedHashMap<String, Identifier> CACHE =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Identifier> eldest) {
                    if (size() > MAX_CACHE_SIZE) {
                        releaseTexture(eldest.getValue());
                        LOGGER.info("🗑️ LRU 释放变异体纹理: {}", eldest.getValue());
                        return true;
                    }
                    return false;
                }
            };

    private ZuoGuardianSkinBuilder() {
    }

    /**
     * 取这位玩家皮肤对应的变异体纹理；底图加载失败 / 皮肤为空时返回 {@code null}
     * （调用方据此退回原版渲染，而不是画出个没贴图的东西）。
     */
    public static synchronized Identifier acquire(Identifier skinTexture) {
        if (skinTexture == null) {
            skinTexture = DefaultPlayerSkin.getDefaultTexture();
        }

        String key = skinTexture.toString();
        Identifier cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            Identifier built = build(skinTexture);
            if (built != null) {
                CACHE.put(key, built);
            }
            return built;
        } catch (Exception e) {
            LOGGER.error("❌ 构建左护法变异体纹理失败: {}", key, e);
            return null;
        }
    }

    public static synchronized void clearCache() {
        for (Identifier texture : CACHE.values()) {
            releaseTexture(texture);
        }
        CACHE.clear();
    }

    // ==================== 内部实现 ====================

    private static Identifier build(Identifier skinTexture) throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resourceManager = minecraft.getResourceManager();

        NativeImage base = loadImage(BASE_TEXTURE, resourceManager);
        if (base == null) {
            LOGGER.warn("⚠️ 找不到变异体底图: {}", BASE_TEXTURE);
            return null;
        }

        NativeImage skin = loadImage(skinTexture, resourceManager);
        if (skin != null) {
            // 老版 64×32 皮肤先补成 64×64，否则取不到下半张（左臂 / 左腿都在下半张）
            if (skin.getWidth() == 64 && skin.getHeight() == 32) {
                skin = SkinProcessor.convertLegacySkin(skin);
            }
            copyRiderParts(base, skin);
            skin.close();
        } else {
            LOGGER.warn("⚠️ 取不到玩家皮肤（{}），左护法变异体只画底图", skinTexture);
        }

        Identifier location = Identifier.fromNamespaceAndPath(
                CorpseOrigin.MOD_ID,
                "skins/zuo_guardian_" + Integer.toHexString(skinTexture.toString().hashCode()));

        TextureManager textureManager = minecraft.getTextureManager();
        // 同名纹理已存在 → 先注销，避免泄漏
        if (textureManager.getTexture(location) != null) {
            textureManager.release(location);
        }
        textureManager.register(location, new DynamicTexture(() -> location.toString(), base));
        LOGGER.info("✅ 左护法变异体纹理已创建: {} (皮肤 {})", location, skinTexture);
        return location;
    }

    /** 把玩家皮肤按部件搬到骑手对应的 UV 原点（源/目标都按 alpha 混合，透明像素保留底图） */
    private static void copyRiderParts(NativeImage base, NativeImage skin) {
        for (RiderPart part : RIDER_PARTS) {
            copyNet(base, skin, part.dstU(), part.dstV(), part.srcU(), part.srcV(),
                    part.netWidth(), part.netHeight());
        }
    }

    /**
     * 搬一块"盒子展开网格"。
     * <p>
     * 整块搬而不是逐面搬：源（原版皮肤）和目标（底图）的盒尺寸完全一致，展开网格的形状也就一致，
     * 面朝哪边之类的映射约定两边天然相同。
     */
    private static void copyNet(NativeImage base, NativeImage skin,
                                int dstU, int dstV, int srcU, int srcV, int width, int height) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int srcX = srcU + x;
                int srcY = srcV + y;
                int dstX = dstU + x;
                int dstY = dstV + y;
                if (srcX < 0 || srcY < 0 || srcX >= skin.getWidth() || srcY >= skin.getHeight()
                        || dstX < 0 || dstY < 0 || dstX >= base.getWidth() || dstY >= base.getHeight()) {
                    continue;
                }

                int skinColor = skin.getPixel(srcX, srcY);
                int alpha = (skinColor >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;   // 皮肤没画到的地方保留底图
                }
                base.setPixel(dstX, dstY, blend(base.getPixel(dstX, dstY), skinColor, alpha / 255.0F));
            }
        }
    }

    private static int blend(int base, int overlay, float alpha) {
        int ba = (base >>> 24) & 0xFF;
        int br = (base >>> 16) & 0xFF;
        int bg = (base >>> 8) & 0xFF;
        int bb = base & 0xFF;

        int or = (overlay >>> 16) & 0xFF;
        int og = (overlay >>> 8) & 0xFF;
        int ob = overlay & 0xFF;

        int r = (int) (br * (1.0F - alpha) + or * alpha);
        int g = (int) (bg * (1.0F - alpha) + og * alpha);
        int b = (int) (bb * (1.0F - alpha) + ob * alpha);
        return (ba << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 读一张贴图。
     * <p>
     * 两条路：静态资源（底图、默认皮肤）走资源管理器；玩家在线上传的皮肤是
     * {@code SkinManager} 注册的 {@link DynamicTexture}，直接把它当前像素拷一份出来。
     */
    private static NativeImage loadImage(Identifier texture, ResourceManager resourceManager) {
        try {
            var resource = resourceManager.getResource(texture);
            if (resource.isPresent()) {
                try (InputStream input = resource.get().open()) {
                    return NativeImage.read(input);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("从资源管理器读取贴图失败: {}", texture);
        }

        Object registered = Minecraft.getInstance().getTextureManager().getTexture(texture);
        if (registered instanceof DynamicTexture dynamicTexture) {
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

    private static void releaseTexture(Identifier location) {
        try {
            Minecraft.getInstance().getTextureManager().release(location);
        } catch (Exception e) {
            LOGGER.warn("释放变异体纹理失败: {} - {}", location, e.getMessage());
        }
    }
}
