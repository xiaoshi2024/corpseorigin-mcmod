package xiaoshi2022.corpseorigin.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.renderer.RenderStateData;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

public class LowerLevelZbModel extends DefaultedEntityGeoModel<LowerLevelZbEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LowerLevelZbModel.class);

    public LowerLevelZbModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "lower_level_zb"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        LOGGER.info("🎨 [Model.getTextureResource] 开始获取纹理");
        LOGGER.info("🎨 [Model.getTextureResource] renderState 类型: {}", renderState.getClass().getName());

        // 从 RenderState 获取
        Identifier customSkin = renderState.getGeckolibData(RenderStateData.CUSTOM_SKIN_TEXTURE);
        ZbSkinState skinState = renderState.getGeckolibData(RenderStateData.SKIN_STATE);

        LOGGER.info("🎨 [Model.getTextureResource] customSkin={}, skinState={}", customSkin, skinState);

        if (skinState == ZbSkinState.LOADED && customSkin != null) {
            LOGGER.info("✅ [Model.getTextureResource] 使用组合纹理: {}", customSkin);
            // 设置备用纹理
            this.withAltTexture(customSkin);
            return customSkin;
        }

        Identifier defaultSkin = net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
        LOGGER.info("⚠️ [Model.getTextureResource] 使用默认皮肤: {}", defaultSkin);
        return defaultSkin;
    }
}