package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.renderer.entity.RenderStateData;
import xiaoshi2022.corpseorigin.entity.MikuZbEntity;

/**
 * 初音尸兄模型。
 * <p>
 * 常态用 {@code miku_zb}；饱食度过高（消化不良）时切换为 {@code miku_zb_flatulence} 大肚子模型，
 * 贴图同步切换为 {@code miku_zb_flatulence.png}。两套模型共享同一套骨骼动画（idle/walk/attack）。
 */
public class MikuZbModel extends DefaultedEntityGeoModel<MikuZbEntity> {

    public MikuZbModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "miku_zb"));
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        if (isOverfull(renderState)) {
            return buildFormattedModelPath(
                    Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "miku_zb_flatulence"));
        }
        return super.getModelResource(renderState);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        if (isOverfull(renderState)) {
            return buildFormattedTexturePath(
                    Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "miku_zb_flatulence"));
        }
        return super.getTextureResource(renderState);
    }

    private static boolean isOverfull(GeoRenderState renderState) {
        Boolean overfull = renderState.getGeckolibData(RenderStateData.MIKU_OVERFULL);
        return Boolean.TRUE.equals(overfull);
    }
}
