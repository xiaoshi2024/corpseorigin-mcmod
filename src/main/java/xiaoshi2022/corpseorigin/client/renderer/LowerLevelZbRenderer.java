package xiaoshi2022.corpseorigin.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.model.LowerLevelZbModel;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LowerLevelZbRenderer extends GeoEntityRenderer<LowerLevelZbEntity, LivingEntityRenderState> {

    private static final Map<Integer, Identifier> SKIN_CACHE = new ConcurrentHashMap<>();
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

        Identifier skin = null;
        if (entity.getSkinState() == ZbSkinState.LOADED) {
            skin = entity.getSkinTexture();
        }
        if (skin == null) {
            String playerName = entity.getPlayerSkinName();
            if (playerName != null && !playerName.isEmpty()) {
                UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
                skin = DefaultPlayerSkin.get(fakeUuid).body().texturePath();
            } else {
                skin = DefaultPlayerSkin.getDefaultTexture();
            }
        }
        if (skin != null) {
            SKIN_CACHE.put(entityId, skin);
        }
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState renderState) {
        if (currentEntityId != -1 && SKIN_CACHE.containsKey(currentEntityId)) {
            return SKIN_CACHE.get(currentEntityId);
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        // ✅ 使用 entityTranslucent 替代 entityCutout
        // 这样可以正确处理透明层并保持渲染顺序
        return RenderTypes.entityTranslucent(texture);
    }
}