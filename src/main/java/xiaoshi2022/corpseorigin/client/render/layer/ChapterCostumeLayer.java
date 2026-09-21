package xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.chapter.*;

/** Role garments and injury geometry keep the player's face and normal limb animation. */
public class ChapterCostumeLayer extends RenderLayer<AvatarRenderState,PlayerModel> {
    private final ModelPart cap,vest,blindfold,wound,seed,cloak;
    private static ModelPart part(CubeListBuilder cubes){
        var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("part",cubes,PartPose.ZERO);
        return LayerDefinition.create(mesh,16,16).bakeRoot().getChild("part");
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public ChapterCostumeLayer(RenderLayerParent parent){
        super(parent);
        cap=part(CubeListBuilder.create().addBox(-4.3f,-8.6f,-4.3f,8.6f,2,8.6f).addBox(-4,-7,-6,8,1,2));
        vest=part(CubeListBuilder.create().addBox(-4.2f,0,-2.2f,8.4f,11,4.4f));
        blindfold=part(CubeListBuilder.create().addBox(-4.2f,-5.5f,-4.2f,8.4f,2,8.4f)
                .addBox(4,-5,-1,1,1,2));
        wound=part(CubeListBuilder.create().addBox(-3,2,-2.3f,2,5,.4f).addBox(0,5,-2.3f,3,1,.4f));
        seed=part(CubeListBuilder.create().addBox(-1,1,-3,2,2,1.2f).addBox(-3,2,-2.3f,6,.4f,.4f)
                .addBox(-2,2,-2.3f,.4f,5,.4f).addBox(2,2,-2.3f,.4f,5,.4f));
        cloak=part(CubeListBuilder.create().addBox(-5,0,2.3f,10,21,.7f)
                .addBox(-6,0,2,12,4,1).addBox(-5,20,2.3f,2,3,.7f).addBox(3,20,2.3f,2,3,.7f));
    }
    private void draw(ModelPart part,ModelPart anchor,String texture,PoseStack poses,SubmitNodeCollector collector,int light){
        poses.pushPose();anchor.translateAndRotate(poses);
        collector.order(0).submitModelPart(part,poses,RenderTypes.entityCutout(CorpseOrigin.id("textures/entity/chapter_"+texture+".png")),light,OverlayTexture.NO_OVERLAY,null);
        poses.popPose();
    }
    @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch){
        var level=Minecraft.getInstance().level;if(level==null || state.isInvisible)return;
        var entity=level.getEntity(state.id);if(entity==null)return;
        String role=entity.getAttachedOrCreate(ChapterActorState.ROLE), condition=entity.getAttachedOrCreate(ChapterScenes.CONDITION);
        var model=getParentModel();
        if(role.equals("siyangyuan_zb") || role.equals("kuaidiyuan_zb")){
            String texture=role.equals("siyangyuan_zb")?"keeper":"courier";
            draw(vest,model.body,texture,poses,collector,light);
            draw(cap,model.head,texture,poses,collector,light);
        }
        if(role.equals("k"))draw(cloak,model.body,"cloak",poses,collector,light);
        if(condition.equals("injured")) {
            if(role.equals("muxi"))draw(blindfold,model.head,"bandage",poses,collector,light);
            draw(wound,model.body,"blood",poses,collector,light);
        }
        if(condition.equals("parasitized"))draw(seed,model.body,"blood",poses,collector,light);
    }
}
