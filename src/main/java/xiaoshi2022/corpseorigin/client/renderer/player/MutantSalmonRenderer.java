package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;

public class MutantSalmonRenderer extends GeoReplacedEntityRenderer<PlayerGeoAnimatable,AbstractClientPlayer,AvatarRenderState> {
    public static final DataTicket<Boolean> ACTIVE=DataTicket.create("mutant_salmon_active",Boolean.class);
    public static final DataTicket<Boolean> BITING=DataTicket.create("mutant_salmon_biting",Boolean.class);
    public static final DataTicket<Boolean> SWIMMING=DataTicket.create("mutant_salmon_swimming",Boolean.class);
    private static MutantSalmonRenderer instance;
    public MutantSalmonRenderer(EntityRendererProvider.Context context) {
        super(context,new DefaultedEntityGeoModel<>(CorpseOrigin.id("mutant_salmon")),null);
        PlayerHandLayers.attach(context, this);
        shadowRadius=.6f;
    }
    public static void create(EntityRendererProvider.Context context) { if(instance==null) instance=new MutantSalmonRenderer(context); }
    public static MutantSalmonRenderer get() { return instance; }
    public static boolean extract(AbstractClientPlayer player, AvatarRenderState state, float partialTick) {
        boolean active=instance!=null && "bianyi_guiyu".equals(player.getAttachedOrCreate(ChapterActorState.ROLE));
        state.addGeckolibData(ACTIVE,active);
        if(!active) return false;
        state.addGeckolibData(MutantBodyRenderData.ACTIVE,false);
        state.addGeckolibData(BITING,player.getAttachedOrCreate(ChapterActorState.BITE_UNTIL)>player.level().getGameTime());
        state.addGeckolibData(SWIMMING,player.isInWater() || state.walkAnimationSpeed>.02f);
        state.addGeckolibData(DataTickets.PACKED_LIGHT,state.lightCoords);
        instance.extractRenderState(player,state,partialTick);
        return true;
    }
    @Override public AvatarRenderState fillRenderState(PlayerGeoAnimatable animatable,AbstractClientPlayer entity,AvatarRenderState state,float partialTick) {
        return super.fillRenderState((PlayerGeoAnimatable)entity,entity,state,partialTick);
    }
    @Override public AvatarRenderState createRenderState(PlayerGeoAnimatable animatable,AbstractClientPlayer entity) {
        return super.createRenderState((PlayerGeoAnimatable)entity,entity);
    }
}
