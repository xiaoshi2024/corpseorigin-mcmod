package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;

/** Attached shoulder/back layer, rendered in the same pass as the Niunai back mount. */
public final class TianGangHaloRenderer extends GeoReplacedEntityRenderer<PlayerGeoAnimatable,AbstractClientPlayer,AvatarRenderState> {
    private static TianGangHaloRenderer instance;
    private TianGangHaloRenderer(EntityRendererProvider.Context context) {
        super(context,new com.geckolib.model.DefaultedEntityGeoModel<>(CorpseOrigin.id("tiangang_halo")),null);
        shadowRadius=0;
    }
    public static void createIfAbsent(EntityRendererProvider.Context context){if(instance==null)instance=new TianGangHaloRenderer(context);}
    public static TianGangHaloRenderer get(){return instance;}
    public static void writeRenderData(AvatarRenderState state,AbstractClientPlayer player,float partialTick){
        if(instance==null)return;
        state.addGeckolibData(DataTickets.PACKED_LIGHT,state.lightCoords);
        instance.extractRenderState(player,state,partialTick);
    }
    @Override public AvatarRenderState fillRenderState(PlayerGeoAnimatable a,AbstractClientPlayer e,AvatarRenderState s,float p){return super.fillRenderState((PlayerGeoAnimatable)e,e,s,p);}
    @Override public AvatarRenderState createRenderState(PlayerGeoAnimatable a,AbstractClientPlayer e){return super.createRenderState((PlayerGeoAnimatable)e,e);}
}
