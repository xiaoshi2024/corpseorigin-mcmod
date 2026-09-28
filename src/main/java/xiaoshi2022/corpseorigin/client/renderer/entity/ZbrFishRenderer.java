package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.ZbrFishEntity;

public final class ZbrFishRenderer extends GeoEntityRenderer<ZbrFishEntity, LivingEntityRenderState> {
    public ZbrFishRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(CorpseOrigin.id("zbr_fish")));
        shadowRadius = .3f;
    }
}
