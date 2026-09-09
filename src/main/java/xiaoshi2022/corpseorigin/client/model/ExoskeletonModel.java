package xiaoshi2022.corpseorigin.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

public class ExoskeletonModel extends EntityModel<AvatarRenderState> {

    // ✅ 直接使用 Blockbench 导出的部件
    private final ModelPart Waist;
    private final ModelPart Head;
    private final ModelPart shieye;
    private final ModelPart group2;
    private final ModelPart group7;
    private final ModelPart group3;
    private final ModelPart group4;
    private final ModelPart group5;
    private final ModelPart group6;
    private final ModelPart rightItem;

    public ExoskeletonModel(ModelPart root) {
        super(root);
        // ✅ 直接获取，不加安全检查
        this.Waist = root.getChild("Waist");
        this.Head = this.Waist.getChild("Head");
        this.shieye = this.Head.getChild("shieye");
        this.group2 = this.shieye.getChild("group2");
        this.group7 = this.group2.getChild("group7");
        this.group3 = this.group7.getChild("group3");
        this.group4 = this.group3.getChild("group4");
        this.group5 = this.group4.getChild("group5");
        this.group6 = this.group4.getChild("group6");
        this.rightItem = this.shieye.getChild("rightItem");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition root = modelData.getRoot();

        // ✅ 与 Blockbench 导出的完全一致
        PartDefinition Waist = root.addOrReplaceChild(
                "Waist",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 12.0F, 0.0F)
        );

        PartDefinition Head = Waist.addOrReplaceChild(
                "Head",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, -12.0F, 0.0F)
        );

        PartDefinition shieye = Head.addOrReplaceChild(
                "shieye",
                CubeListBuilder.create()
                        .texOffs(49, 46)
                        .addBox(-0.625F, -0.75F, -0.6F, 1.5F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.725F, 0.05F, 2.6F)
        );

        PartDefinition group2 = shieye.addOrReplaceChild(
                "group2",
                CubeListBuilder.create(),
                PartPose.offset(-0.125F, 0.25F, 0.1F)
        );

        group2.addOrReplaceChild(
                "cube_r1",
                CubeListBuilder.create()
                        .texOffs(19, 21)
                        .addBox(-0.3F, -1.0F, 0.3F, 1.0F, 1.0F, 5.1F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.0472F, 0.0F)
        );

        PartDefinition group7 = group2.addOrReplaceChild(
                "group7",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(5.1456F, -0.8823F, 2.7108F, 0.7777F, -0.1231F, 0.124F)
        );

        group7.addOrReplaceChild(
                "cube_r2",
                CubeListBuilder.create()
                        .texOffs(11, 13)
                        .addBox(-0.3F, -1.0F, 0.3F, 1.0F, 1.0F, 7.1F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.3455F, 0.8823F, 0.0892F, 1.309F, 1.0472F, 0.0F)
        );

        PartDefinition group3 = group7.addOrReplaceChild(
                "group3",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.905F, -6.2957F, 0.4988F, 0.0F, 1.0472F, -1.0908F)
        );

        group3.addOrReplaceChild(
                "cube_r3",
                CubeListBuilder.create()
                        .texOffs(25, 27)
                        .addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F, 0.0F, 1.0472F, 0.0F)
        );

        PartDefinition group4 = group3.addOrReplaceChild(
                "group4",
                CubeListBuilder.create()
                        .texOffs(31, 56)
                        .addBox(-0.2375F, -0.95F, -0.9875F, 1.0F, 1.9F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(33, 57)
                        .addBox(-0.0375F, -1.05F, -0.5875F, 1.0F, 2.1F, 1.3F, new CubeDeformation(0.0F)),
                PartPose.offset(3.7388F, -0.038F, 2.0416F)
        );

        group4.addOrReplaceChild(
                "group5",
                CubeListBuilder.create()
                        .texOffs(39, 13)
                        .mirror()
                        .addBox(-0.8F, -0.95F, -0.65F, 1.0F, 1.9F, 1.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offset(0.9625F, 0.0F, 0.7625F)
        );

        group4.addOrReplaceChild(
                "group6",
                CubeListBuilder.create()
                        .texOffs(55, 13)
                        .addBox(-0.8F, -0.95F, -0.25F, 1.0F, 1.9F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.9625F, 0.0F, -0.8375F)
        );

        shieye.addOrReplaceChild(
                "rightItem",
                CubeListBuilder.create(),
                PartPose.offset(8.475F, -5.45F, -2.0F)
        );

        return LayerDefinition.create(modelData, 64, 64);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        // 动画由 ExoskeletonRenderLayer 控制
    }

    public ModelPart getWaist() {
        return Waist;
    }

    public ModelPart getShieye() {
        return shieye;
    }

    public void copyFromHead(ModelPart head) {
        if (this.shieye != null && head != null) {
            PartPose headPose = head.storePose();
            this.shieye.loadPose(headPose);
            this.shieye.x += 1.1F;
            this.shieye.y -= 0.3F;
            this.shieye.z += 2.0F;
        }
    }
}