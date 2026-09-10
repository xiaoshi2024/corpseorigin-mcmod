package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.renderer.entity.RenderStateData;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

public class LowerLevelZbModel extends DefaultedEntityGeoModel<LowerLevelZbEntity> {

    public LowerLevelZbModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "lower_level_zb"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Identifier customSkin = renderState.getGeckolibData(RenderStateData.CUSTOM_SKIN_TEXTURE);
        ZbSkinState skinState = renderState.getGeckolibData(RenderStateData.SKIN_STATE);

        // ✅ 加载完成 + 有皮肤 → 用组合纹理
        if (skinState == ZbSkinState.LOADED && customSkin != null) {
            return customSkin;
        }

        // ✅ 加载中 → 用默认皮肤
        return DefaultPlayerSkin.getDefaultTexture();
    }
}