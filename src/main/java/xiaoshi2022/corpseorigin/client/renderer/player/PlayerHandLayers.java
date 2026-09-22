package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;

final class PlayerHandLayers {
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void attach(EntityRendererProvider.Context context,
            GeoReplacedEntityRenderer<PlayerGeoAnimatable, AbstractClientPlayer, AvatarRenderState> renderer) {
        // The runtime animatable is the player, which implements both required interfaces.
        GeoRenderLayer<PlayerGeoAnimatable, AbstractClientPlayer, AvatarRenderState> layer =
                (GeoRenderLayer) new ItemInHandGeoLayer(context, renderer, "RightHandItem", "LeftHandItem");
        renderer.withRenderLayer(layer);
    }
}
