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
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;

public class ChameleonHeadLayer extends RenderLayer<AvatarRenderState,PlayerModel> {
    private final ModelPart animal;
    @SuppressWarnings({"rawtypes","unchecked"})
    public ChameleonHeadLayer(RenderLayerParent parent) {
        super(parent);
        MeshDefinition mesh=new MeshDefinition();
        var cubes=CubeListBuilder.create().texOffs(0,0)
                .addBox(-1.5f,-11,-2,3,2,5)
                .addBox(-2,-12,-4,4,3,3)
                .addBox(-2.8f,-11.5f,-4,.8f,1.5f,1.5f)
                .addBox(2,-11.5f,-4,.8f,1.5f,1.5f)
                .addBox(-.5f,-10,3,1,1,3)
                .addBox(-.5f,-12,5,1,2,1)
                .addBox(-.5f,-12,3,1,1,2);
        for(int sign:new int[]{-1,1}) for(int z:new int[]{-1,2})
            cubes.addBox(sign<0?-3:1,-9,z,2,1,1);
        mesh.getRoot().addOrReplaceChild("chameleon",cubes,PartPose.ZERO);
        animal=LayerDefinition.create(mesh,16,16).bakeRoot().getChild("chameleon");
    }
    @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch) {
        var level=Minecraft.getInstance().level; if(level==null || state.isInvisible) return;
        var entity=level.getEntity(state.id); if(entity==null)return;
        if(!"bianselong_zb".equals(entity.getAttachedOrCreate(ChapterActorState.ROLE))
                || !entity.getAttachedOrCreate(ChapterActorState.DISGUISE).isEmpty())return;
        poses.pushPose();
        getParentModel().head.translateAndRotate(poses);
        collector.order(0).submitModelPart(animal,poses,RenderTypes.entityCutout(CorpseOrigin.id("textures/entity/chameleon_head.png")),
                light,OverlayTexture.NO_OVERLAY,null);
        poses.popPose();
    }
}
