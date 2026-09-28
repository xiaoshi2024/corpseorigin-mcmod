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
import xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes;
import xiaoshi2022.corpseorigin.skill.chapter.CreatureAbilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CreaturePlayerRenderer extends GeoReplacedEntityRenderer<PlayerGeoAnimatable,AbstractClientPlayer,AvatarRenderState> {
    public static final DataTicket<String> MODEL=DataTicket.create("creature_model",String.class);
    public static final DataTicket<String> CLIP=DataTicket.create("creature_clip",String.class);
    public static final DataTicket<Integer> ARMS=DataTicket.create("creature_arms",Integer.class);
    public static final DataTicket<Boolean> INFANT=DataTicket.create("creature_infant",Boolean.class);
    private static final Map<String,CreaturePlayerRenderer> RENDERERS=new HashMap<>();
    private CreaturePlayerRenderer(EntityRendererProvider.Context context,String id){super(context,new DefaultedEntityGeoModel<>(CorpseOrigin.id(id)),null);PlayerHandLayers.attach(context,this);shadowRadius=.7f;}
    public static void create(EntityRendererProvider.Context context){
        for(String id:List.of("qingwa_zb","hujie","chongmu","jingang_zb","jingang_infant","xiongxing_zb","red_fire_ant","bullet_ant","xiaohui_scene"))RENDERERS.put(id,new CreaturePlayerRenderer(context,id));
    }
    public static CreaturePlayerRenderer get(AvatarRenderState state){return RENDERERS.get(state.getGeckolibData(MODEL));}
    public static boolean extract(AbstractClientPlayer p,AvatarRenderState state,float partial){
        String currentRole = p.getAttachedOrCreate(ChapterActorState.ROLE);
        if ((currentRole.equals("jingang_zb") || currentRole.equals("shichaozhizi"))
                && p.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.longyou.BodyPossession.HUMAN)) {
            state.addGeckolibData(MODEL, "");
            return false;
        }
        String role=p.getAttachedOrCreate(ChapterActorState.ROLE),id=role.equals("chongqun")?"bullet_ant":role;
        if(role.equals("jingang_zb") && p.getAttachedOrCreate(CreatureAbilities.INFANT))id="jingang_infant";
        String condition=p.getAttachedOrCreate(ChapterScenes.CONDITION);
        if(role.equals("bianselong_zb") && p.getAttachedOrCreate(ChapterActorState.DISGUISE).equals("xiaohui") && Set.of("bound","fear","black_general").contains(condition))id="xiaohui_scene";
        state.addGeckolibData(MODEL,"");
        var renderer=RENDERERS.get(id);if(renderer==null)return false;
        state.addGeckolibData(MODEL,id);state.addGeckolibData(MutantBodyRenderData.ACTIVE,false);
        state.addGeckolibData(MutantSalmonRenderer.ACTIVE,true);
        state.addGeckolibData(ARMS,p.getAttachedOrCreate(CreatureAbilities.BEAR_ARMS));
        state.addGeckolibData(INFANT,p.getAttachedOrCreate(CreatureAbilities.INFANT));
        String action=p.getAttachedOrCreate(ChapterScenes.ACTION);
        String clip=switch(action){case "cast","threat"->"cast";case "charge"->"charge";case "pounce"->"pounce";case "transform"->"transform";case "attack"->"attack";default->p.hurtTime>0?"hurt":p.swinging?"attack":state.walkAnimationSpeed>.02f?"walk":"idle";};
        if(id.equals("xiaohui_scene"))clip=condition;
        state.addGeckolibData(CLIP,clip);state.addGeckolibData(DataTickets.PACKED_LIGHT,state.lightCoords);
        renderer.extractRenderState(p,state,partial);return true;
    }
    @Override public AvatarRenderState fillRenderState(PlayerGeoAnimatable a,AbstractClientPlayer p,AvatarRenderState state,float partial){return super.fillRenderState((PlayerGeoAnimatable)p,p,state,partial);}
    @Override public AvatarRenderState createRenderState(PlayerGeoAnimatable a,AbstractClientPlayer p){return super.createRenderState((PlayerGeoAnimatable)p,p);}
    @Override public void adjustModelBonesForRender(com.geckolib.renderer.base.RenderPassInfo<AvatarRenderState> pass,com.geckolib.renderer.base.BoneSnapshots snapshots){
        var state=pass.renderState();String id=state.getGeckolibData(MODEL);
        if("xiongxing_zb".equals(id))for(int j=0;j<3;j++)for(int side:new int[]{-1,1}){
            int index=j*2+(side>0?1:0);boolean hidden=state.getOrDefaultGeckolibData(ARMS,0)<=index;
            snapshots.ifPresent("extra_arm_"+j+"_"+side,b->b.skipRender(hidden).skipChildrenRender(hidden));
        }
        if("xiaohui_scene".equals(id)){
            String clip=state.getGeckolibData(CLIP);
            snapshots.ifPresent("chess",b->b.skipRender(!"black_general".equals(clip)).skipChildrenRender(!"black_general".equals(clip)));
        }
    }
}
