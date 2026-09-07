package xiaoshi2022.corpseorigin.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.renderer.LowerLevelZbRenderState;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

public class LowerLevelZbModel extends DefaultedEntityGeoModel<LowerLevelZbEntity> {

    public LowerLevelZbModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "lower_level_zb"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        if (renderState instanceof LowerLevelZbRenderState zbState) {
            if (zbState.skinState == ZbSkinState.LOADED && zbState.customSkinTexture != null) {
                return zbState.customSkinTexture;
            }
        }
        return net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
    }
}