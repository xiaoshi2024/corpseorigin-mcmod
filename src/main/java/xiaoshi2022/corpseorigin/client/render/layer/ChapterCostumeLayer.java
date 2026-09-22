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
    private final ModelPart cap,vest,blindfold,wound,seed,cloak,
            scalesBody,scalesArmLeft,scalesArmRight,scalesLegLeft,scalesLegRight,
            embeddedSword,fullShell,goldHead,lotus,upperArm,upperArmSlim;

    private static ModelPart part(CubeListBuilder cubes){
        var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("part",cubes,PartPose.ZERO);
        return LayerDefinition.create(mesh,16,16).bakeRoot().getChild("part");
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    public ChapterCostumeLayer(RenderLayerParent parent){
        super(parent);
        var armMesh=new MeshDefinition();
        armMesh.getRoot().addOrReplaceChild("arm",CubeListBuilder.create().texOffs(40,16).addBox(-3,-2,-2,4,6,4),PartPose.ZERO);
        upperArm=LayerDefinition.create(armMesh,64,64).bakeRoot().getChild("arm");
        var slimMesh=new MeshDefinition();
        slimMesh.getRoot().addOrReplaceChild("arm",CubeListBuilder.create().texOffs(40,16).addBox(-2,-2,-2,3,6,4),PartPose.ZERO);
        upperArmSlim=LayerDefinition.create(slimMesh,64,64).bakeRoot().getChild("arm");
        goldHead=part(CubeListBuilder.create().addBox(-4.12f,-8.12f,-4.12f,8.24f,8.24f,8.24f));
        lotus=part(CubeListBuilder.create().addBox(-1.2f,2,-2.7f,2.4f,7,.4f)
                .addBox(-3.2f,4,-2.7f,2,4,.4f).addBox(1.2f,4,-2.7f,2,4,.4f)
                .addBox(-4,6,-2.7f,1,2,.4f).addBox(3,6,-2.7f,1,2,.4f));

        // 躯干：比原版 8 宽略窄一点，免得手臂被挤得贴身体
        scalesBody=part(CubeListBuilder.create().addBox(-3.9f,-.15f,-2.15f,7.8f,12.3f,4.3f));

        // ==================== 手臂鳞甲 ====================
        // 原版玩家手臂枢轴在肩膀内侧：
        //   leftArm  枢轴 (5, 2, 0)，几何偏向 +X
        //   rightArm 枢轴 (-5, 2, 0)，几何偏向 -X
        // 一个对称盒子挂两条臂，必然有一边贴身体 —— 所以拆成左右两个盒子，
        // 各朝外侧偏 1 像素，让鳞甲真正"包住"手臂而不是缩进躯干里。
        scalesArmLeft =part(CubeListBuilder.create().addBox(-1.15f,-2.28f,-2.38f,4.76f,12.56f,4.76f));
        scalesArmRight=part(CubeListBuilder.create().addBox(-3.61f,-2.28f,-2.38f,4.76f,12.56f,4.76f));

        // 腿同样拆左右，各朝外侧偏一点
        scalesLegLeft =part(CubeListBuilder.create().addBox(-1.95f,-.15f,-2.15f,4.3f,12.3f,4.3f));
        scalesLegRight=part(CubeListBuilder.create().addBox(-2.35f,-.15f,-2.15f,4.3f,12.3f,4.3f));

        embeddedSword=part(CubeListBuilder.create().addBox(-1.4f,3,-15,2.8f,1,14)
                .addBox(-1,3,-19,2,1,4).addBox(-.5f,3,-22,1,1,3)
                .addBox(-3,2.5f,-7,6,2,1).addBox(-.5f,3,-6,1,1,5));
        fullShell=part(CubeListBuilder.create()
                .addBox(-5.2f,-1,-3.4f,10.4f,13.5f,6.8f)
                .addBox(-5.6f,0,-3,1.2f,12.5f,6)
                .addBox(4.4f,0,-3,1.2f,12.5f,6)
                .addBox(-4.8f,11,-3.1f,9.6f,2,6.2f));
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
        if(BodySkillState.missingForearm(entity)){
            var mask=state.getGeckolibData(xiaoshi2022.corpseorigin.client.limb.LimbRenderData.LIMB_MASK);
            if(mask==null || (mask & (1<<xiaoshi2022.corpseorigin.limb.LimbSlots.RIGHT_ARM))==0){
                poses.pushPose();model.rightArm.translateAndRotate(poses);
                collector.order(0).submitModelPart(state.skin.model()==net.minecraft.world.entity.player.PlayerModelType.SLIM?upperArmSlim:upperArm,poses,RenderTypes.entityCutout(state.skin.body().texturePath()),
                        light,OverlayTexture.NO_OVERLAY,null);
                poses.popPose();
            }
        }
        if(entity.getAttachedOrCreate(SkillRework.GOLD)>level.getGameTime()) {
            draw(goldHead,model.head,"gold",poses,collector,light);
            draw(scalesBody,model.body,"gold",poses,collector,light);
            draw(scalesArmLeft,model.leftArm,"gold",poses,collector,light);
            draw(scalesArmRight,model.rightArm,"gold",poses,collector,light);
            draw(scalesLegLeft,model.leftLeg,"gold",poses,collector,light);
            draw(scalesLegRight,model.rightLeg,"gold",poses,collector,light);
        }
        if(entity.getAttachedOrCreate(SkillRework.LOTUS_ARMOR)>level.getGameTime())
            draw(lotus,model.body,"blood",poses,collector,15728880);
        int bodyState=entity.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.longyou.UndeadBodyState.STATE);
        if(bodyState==1 && entity.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.longyou.BodyPossession.HUMAN))
            draw(fullShell,model.body,"xuanwu",poses,collector,light);
        if(bodyState==1){
            draw(scalesBody,model.body,"xuanwu",poses,collector,light);
            draw(scalesArmLeft ,model.leftArm ,"xuanwu",poses,collector,light);
            draw(scalesArmRight,model.rightArm,"xuanwu",poses,collector,light);
            draw(scalesLegLeft ,model.leftLeg ,"xuanwu",poses,collector,light);
            draw(scalesLegRight,model.rightLeg,"xuanwu",poses,collector,light);
        }
        if(bodyState==2)draw(embeddedSword,model.body,"meteor_seal",poses,collector,light);
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
