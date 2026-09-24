package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.renderer.player.EvolutionGeoRenderer;

public final class EvolutionGeoModel extends DefaultedEntityGeoModel<PlayerGeoAnimatable> {
    public EvolutionGeoModel() { super(CorpseOrigin.id("evolution_bat")); }
    @Override public Identifier getModelResource(GeoRenderState state) {
        return CorpseOrigin.id("entity/" + state.getGeckolibData(EvolutionGeoRenderer.MODEL));
    }
    @Override public Identifier getAnimationResource(PlayerGeoAnimatable animatable) {
        return CorpseOrigin.id("entity/evolution_parts");
    }
    @Override public Identifier getTextureResource(GeoRenderState state) {
        return CorpseOrigin.id("textures/entity/evolution/" + state.getGeckolibData(EvolutionGeoRenderer.COLOR) + ".png");
    }
}
