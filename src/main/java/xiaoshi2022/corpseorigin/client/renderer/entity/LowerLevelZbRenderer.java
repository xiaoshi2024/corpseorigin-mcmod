package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaoshi2022.corpseorigin.client.model.entity.LowerLevelZbModel;
import xiaoshi2022.corpseorigin.client.skin.CombinedSkinBuilder;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinLoader;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LowerLevelZbRenderer extends GeoEntityRenderer<LowerLevelZbEntity, LivingEntityRenderState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LowerLevelZbRenderer.class);

    /** ✅ 每个实体 → 绑定信息 */
    private static final Map<Integer, EntitySkinBinding> ENTITY_BINDING = new ConcurrentHashMap<>();

    private static final class EntitySkinBinding {
        final Identifier skinTexture;      // ✅ 原始皮肤路径（用于变化检测）
        final Identifier combinedTexture;  // ✅ 组合纹理（用于渲染复用）

        EntitySkinBinding(Identifier skinTexture, Identifier combinedTexture) {
            this.skinTexture = skinTexture;
            this.combinedTexture = combinedTexture;
        }
    }

    public LowerLevelZbRenderer(EntityRendererProvider.Context context) {
        super(context, new LowerLevelZbModel());
        this.shadowRadius = 0.5f;
        // ✅ 添加手持物品渲染层
        this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
    }

    @Override
    public void extractRenderState(LowerLevelZbEntity entity, LivingEntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        int entityId = entity.getId();
        String playerName = entity.getPlayerSkinName();
        ZbSkinState currentState = entity.getSkinState();

        // 1. 解析皮肤路径
        Identifier skinTexture = resolveSkinTexture(entity, playerName, currentState);
        if (skinTexture == null) {
            skinTexture = DefaultPlayerSkin.getDefaultTexture();
        }

        // 2. 检查绑定
        EntitySkinBinding binding = ENTITY_BINDING.get(entityId);

        if (binding != null && binding.skinTexture.equals(skinTexture)) {
            // ✅ 皮肤没变，直接复用组合纹理（绝不 acquire！）
            renderState.addGeckolibData(RenderStateData.CUSTOM_SKIN_TEXTURE, binding.combinedTexture);
            renderState.addGeckolibData(RenderStateData.SKIN_STATE, currentState);
            return;
        }

        // 3. 皮肤变了（首次/切换），释放旧的
        if (binding != null) {
            CombinedSkinBuilder.release(binding.skinTexture);
            LOGGER.debug("🔄 实体 {} 皮肤变化: {} -> {}", entityId, binding.skinTexture, skinTexture);
        }

        // 4. 获取新组合纹理
        Identifier combinedTexture = CombinedSkinBuilder.acquire(skinTexture);
        ENTITY_BINDING.put(entityId, new EntitySkinBinding(skinTexture, combinedTexture));

        // 5. 写入
        renderState.addGeckolibData(RenderStateData.CUSTOM_SKIN_TEXTURE, combinedTexture);
        renderState.addGeckolibData(RenderStateData.SKIN_STATE, currentState);
    }

    private Identifier resolveSkinTexture(LowerLevelZbEntity entity, String playerName, ZbSkinState state) {
        if (state == ZbSkinState.LOADED) {
            Identifier texture = entity.getSkinTexture();
            if (texture != null) {
                return texture;
            }
        }

        if ((state == ZbSkinState.NOT_LOADED || state == ZbSkinState.FAILED)
                && playerName != null && !playerName.isEmpty()) {
            ZbSkinLoader.loadSkinAsync(entity, playerName);
        }

        if (playerName != null && !playerName.isEmpty()) {
            UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
            return DefaultPlayerSkin.get(fakeUuid).body().texturePath();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }

    public static void onEntityRemoved(int entityId) {
        EntitySkinBinding binding = ENTITY_BINDING.remove(entityId);
        if (binding != null) {
            CombinedSkinBuilder.release(binding.skinTexture);
            LOGGER.debug("🗑️ 实体 {} 已移除，释放皮肤纹理引用: {}", entityId, binding.skinTexture);
        }
    }

    public static void clearCache() {
        for (EntitySkinBinding binding : ENTITY_BINDING.values()) {
            CombinedSkinBuilder.release(binding.skinTexture);
        }
        ENTITY_BINDING.clear();
        CombinedSkinBuilder.clearCache();
        LOGGER.info("🧹 所有皮肤缓存已清理");
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
}