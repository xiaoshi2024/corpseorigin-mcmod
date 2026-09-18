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
import xiaoshi2022.corpseorigin.config.CorpseConfig;
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
        final int variant;                 // ✅ 色调变体（由玩家 ID 派生）

        EntitySkinBinding(Identifier skinTexture, Identifier combinedTexture, int variant) {
            this.skinTexture = skinTexture;
            this.combinedTexture = combinedTexture;
            this.variant = variant;
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

        // 色调变体：由玩家 ID 派生。
        //   - 名字是随机拼的，去 api.mojang.com 查多半查不到 → 退回默认皮肤，这时靠变体让每只尸兄都不一样；
        //   - 万一运气好真查到了（或者名字是玩家本人），就是一张有辨识度的真皮肤，此时<b>不染色</b>，原样显示；
        //   - 整体开关与强度在 config/corpseorigin.json 的 skin 段。
        boolean tinted = playerName != null && !playerName.isEmpty()
                && !corpseorigin$isResolvedSkin(playerName, skinTexture)
                && CorpseConfig.get().skin.tintUnresolvedSkins;
        int variant = tinted ? playerName.hashCode() : 0;

        // 2. 检查绑定
        EntitySkinBinding binding = ENTITY_BINDING.get(entityId);

        if (binding != null && binding.skinTexture.equals(skinTexture) && binding.variant == variant) {
            // ✅ 皮肤/色调都没变，直接复用组合纹理（绝不 acquire！）
            renderState.addGeckolibData(RenderStateData.CUSTOM_SKIN_TEXTURE, binding.combinedTexture);
            renderState.addGeckolibData(RenderStateData.SKIN_STATE, currentState);
            return;
        }

        // 3. 皮肤或色调变了（首次/切换），释放旧的
        if (binding != null) {
            CombinedSkinBuilder.release(binding.skinTexture, binding.variant);
            LOGGER.debug("🔄 实体 {} 皮肤变化: {} -> {}", entityId, binding.skinTexture, skinTexture);
        }

        // 4. 获取新组合纹理
        Identifier combinedTexture = CombinedSkinBuilder.acquire(skinTexture, variant);
        ENTITY_BINDING.put(entityId, new EntitySkinBinding(skinTexture, combinedTexture, variant));

        // 5. 写入
        renderState.addGeckolibData(RenderStateData.CUSTOM_SKIN_TEXTURE, combinedTexture);
        renderState.addGeckolibData(RenderStateData.SKIN_STATE, currentState);
    }

    /**
     * 这个名字到底是"查到了真实皮肤"还是"退回默认皮肤了"。
     * <p>
     * 判据就是拿它的离线 UUID 算出应有的默认皮肤路径比一比 —— {@code ZbSkinLoader} 查不到时
     * 走的就是 {@code DefaultPlayerSkin.get(离线UUID)}，所以路径一致即为"没查到"。
     */
    private static boolean corpseorigin$isResolvedSkin(String playerName, Identifier skinTexture) {
        if (playerName == null || playerName.isEmpty() || skinTexture == null) {
            return false;
        }
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
        return !skinTexture.equals(DefaultPlayerSkin.get(offlineUuid).body().texturePath());
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
            CombinedSkinBuilder.release(binding.skinTexture, binding.variant);
            LOGGER.debug("🗑️ 实体 {} 已移除，释放皮肤纹理引用: {}", entityId, binding.skinTexture);
        }
    }

    public static void clearCache() {
        for (EntitySkinBinding binding : ENTITY_BINDING.values()) {
            CombinedSkinBuilder.release(binding.skinTexture, binding.variant);
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