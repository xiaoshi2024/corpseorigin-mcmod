package xiaoshi2022.corpseorigin.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.model.LowerLevelZbModel;
import xiaoshi2022.corpseorigin.client.skin.CombinedSkinBuilder;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LowerLevelZbRenderer extends GeoEntityRenderer<LowerLevelZbEntity, LivingEntityRenderState> {

    // ✅ 使用静态缓存存储组合纹理
    private static final Map<Integer, Identifier> COMBINED_SKIN_CACHE = new ConcurrentHashMap<>();

    // 当前实体ID（用于 getTextureLocation）
    private int currentEntityId = -1;

    public LowerLevelZbRenderer(EntityRendererProvider.Context context) {
        super(context, new LowerLevelZbModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public void extractRenderState(LowerLevelZbEntity entity, LivingEntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        int entityId = entity.getId();
        currentEntityId = entityId;

        // 检查缓存
        if (COMBINED_SKIN_CACHE.containsKey(entityId)) {
            return;
        }

        // 获取皮肤纹理
        Identifier skinTexture = null;
        if (entity.getSkinState() == ZbSkinState.LOADED) {
            skinTexture = entity.getSkinTexture();
        }
        if (skinTexture == null) {
            String playerName = entity.getPlayerSkinName();
            if (playerName != null && !playerName.isEmpty()) {
                UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
                skinTexture = DefaultPlayerSkin.get(fakeUuid).body().texturePath();
            } else {
                skinTexture = DefaultPlayerSkin.getDefaultTexture();
            }
        }

        // 构建组合纹理并缓存
        if (skinTexture != null) {
            String cacheKey = entity.getCustomId() + "_" + entityId;
            Identifier combinedTexture = CombinedSkinBuilder.getOrCreate(cacheKey, skinTexture);
            COMBINED_SKIN_CACHE.put(entityId, combinedTexture);
        }
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState renderState) {
        // 从缓存获取组合纹理
        if (currentEntityId != -1 && COMBINED_SKIN_CACHE.containsKey(currentEntityId)) {
            return COMBINED_SKIN_CACHE.get(currentEntityId);
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        // ✅ 使用 entityCutout 正确处理透明区域
        return RenderTypes.entityCutout(texture);
    }
}