package xiaoshi2022.corpseorigin.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaoshi2022.corpseorigin.client.model.LowerLevelZbModel;
import xiaoshi2022.corpseorigin.client.skin.CombinedSkinBuilder;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinLoader;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LowerLevelZbRenderer extends GeoEntityRenderer<LowerLevelZbEntity, LivingEntityRenderState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LowerLevelZbRenderer.class);

    private static final ConcurrentHashMap<String, CachedSkinData> SKIN_CACHE = new ConcurrentHashMap<>();

    private static final class CachedSkinData {
        final Identifier texture;
        final ZbSkinState state;
        final int entityId;
        final String skinKey;

        CachedSkinData(Identifier texture, ZbSkinState state, int entityId, String skinKey) {
            this.texture = texture;
            this.state = state;
            this.entityId = entityId;
            this.skinKey = skinKey;
        }
    }

    public LowerLevelZbRenderer(EntityRendererProvider.Context context) {
        super(context, new LowerLevelZbModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public void extractRenderState(LowerLevelZbEntity entity, LivingEntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        int entityId = entity.getId();
        String playerName = entity.getPlayerSkinName();
        String skinKey = playerName != null ? playerName : "default";
        String cacheKey = entityId + ":" + skinKey;

        CachedSkinData cached = SKIN_CACHE.get(cacheKey);

        // ✅ 如果缓存存在且状态一致，直接复用
        if (cached != null && cached.state == entity.getSkinState()) {
            renderState.addGeckolibData(RenderStateData.CUSTOM_SKIN_TEXTURE, cached.texture);
            renderState.addGeckolibData(RenderStateData.SKIN_STATE, cached.state);
            return;
        }

        // 🔍 状态变化或首次加载，打印日志
        if (cached == null) {
            LOGGER.info("🔍 [extractRenderState] 首次加载: entityId={}, player={}, skinState={}",
                    entityId, playerName, entity.getSkinState());
        } else {
            LOGGER.info("🔍 [extractRenderState] 状态变化: entityId={}, {} -> {}",
                    entityId, cached.state, entity.getSkinState());
        }

        // ✅ 获取皮肤纹理
        Identifier skinTexture = null;
        ZbSkinState currentState = entity.getSkinState();

        if (currentState == ZbSkinState.LOADED) {
            skinTexture = entity.getSkinTexture();
            LOGGER.info("🔍 [extractRenderState] 从实体获取皮肤纹理: {}", skinTexture);
        } else if (currentState == ZbSkinState.NOT_LOADED || currentState == ZbSkinState.FAILED) {
            // ✅ 触发皮肤加载
            if (playerName != null && !playerName.isEmpty()) {
                LOGGER.info("🔍 [extractRenderState] 触发皮肤加载: {}", playerName);
                ZbSkinLoader.loadSkinAsync(entity, playerName);
            }
            // 临时使用默认皮肤
            skinTexture = DefaultPlayerSkin.getDefaultTexture();
        } else {
            // LOADING 状态，使用默认皮肤
            skinTexture = DefaultPlayerSkin.getDefaultTexture();
        }

        // 如果还是 null，使用默认
        if (skinTexture == null) {
            if (playerName != null && !playerName.isEmpty()) {
                UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
                skinTexture = DefaultPlayerSkin.get(fakeUuid).body().texturePath();
            } else {
                skinTexture = DefaultPlayerSkin.getDefaultTexture();
            }
            LOGGER.warn("⚠️ [extractRenderState] skinTexture为null，使用默认: {}", skinTexture);
        }

        // ✅ 使用皮肤纹理作为缓存 key（不再使用 entityId）
        Identifier combinedTexture = CombinedSkinBuilder.getOrCreate(skinTexture);
        ZbSkinState state = entity.getSkinState();

        renderState.addGeckolibData(RenderStateData.CUSTOM_SKIN_TEXTURE, combinedTexture);
        renderState.addGeckolibData(RenderStateData.SKIN_STATE, state);

        SKIN_CACHE.put(cacheKey, new CachedSkinData(combinedTexture, state, entityId, skinKey));

        if (state == ZbSkinState.LOADED) {
            LOGGER.info("✅ [extractRenderState] 皮肤加载完成: entityId={}, texture={}, combined={}",
                    entityId, skinTexture, combinedTexture);
        }
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState renderState) {
        Identifier customSkin = renderState.getGeckolibData(RenderStateData.CUSTOM_SKIN_TEXTURE);
        ZbSkinState skinState = renderState.getGeckolibData(RenderStateData.SKIN_STATE);

        if (skinState == ZbSkinState.LOADED && customSkin != null) {
            return customSkin;
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    public static void clearCache() {
        SKIN_CACHE.clear();
        LOGGER.info("🧹 皮肤缓存已清理");
    }
}