package xiaoshi2022.corpseorigin.client.model;

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

    // ✅ 动画状态
    private boolean isSwinging = false;
    private float swingTime = 0.0F;

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

    // ==================== ✅ 新增：setupAnim 动画逻辑 ====================

    @Override
    public void setupAnim(AvatarRenderState state) {
        if (shieye == null) return;

        // ✅ 重置所有部件
        resetAllParts();

        // ✅ 设置头部旋转
        if (Head != null) {
            Head.xRot = state.xRot * Mth.DEG_TO_RAD;
            Head.yRot = state.yRot * Mth.DEG_TO_RAD;
        }

        // ✅ 先复制头部完整姿势
        copyFromHead(Head);

        // ✅ 然后在此基础上叠加动画（使用 += 而不是 =）
        applyIdleAnimationAdditive(state.ageInTicks);
        applyWalkAnimationAdditive(state.walkAnimationPos, state.walkAnimationSpeed);

        if (isSwinging) {
            swingTime += 0.05F;
            applyAttackAnimationAdditive(swingTime);
            if (swingTime >= 1.75F) {
                isSwinging = false;
                swingTime = 0.0F;
            }
        }
    }

    /**
     * ✅ 叠加式待机动画
     */
    private void applyIdleAnimationAdditive(float ageInTicks) {
        if (shieye == null) return;
        float time = ageInTicks * 0.05F;

        // 使用 += 叠加
        float breathe = Mth.sin(time * 0.5F) * 0.02F;
        shieye.y += breathe;

        if (group2 != null) {
            group2.xRot += Mth.sin(time * 0.3F) * 0.05F;
            group2.zRot += Mth.cos(time * 0.2F) * 0.03F;
        }
        if (group7 != null) {
            group7.xRot += Mth.sin(time * 0.25F + 0.5F) * 0.08F;
            group7.yRot += Mth.sin(time * 0.15F) * 0.05F;
        }
        if (group3 != null) {
            group3.xRot += Mth.sin(time * 0.2F + 1.0F) * 0.1F;
            group3.zRot += Mth.cos(time * 0.18F) * 0.08F;
        }
        if (group4 != null) {
            group4.yRot += Mth.sin(time * 0.35F + 1.5F) * 0.12F;
        }
        if (group5 != null) {
            group5.zRot += Mth.sin(time * 0.4F) * 0.05F;
        }
        if (group6 != null) {
            group6.zRot += Mth.cos(time * 0.45F) * 0.05F;
        }
    }

    /**
     * ✅ 叠加式行走动画
     */
    private void applyWalkAnimationAdditive(float limbSwing, float limbSwingAmount) {
        if (limbSwingAmount <= 0.01F) return;
        if (shieye == null) return;

        float walkScale = Mth.PI / 8 * limbSwingAmount;

        if (group2 != null) {
            group2.xRot += Mth.cos(limbSwing * 0.5F) * walkScale * 0.5F;
        }
        if (group7 != null) {
            group7.xRot += Mth.sin(limbSwing * 0.5F + 0.5F) * walkScale * 0.7F;
        }
        if (group3 != null) {
            group3.xRot += Mth.cos(limbSwing * 0.5F + 1.0F) * walkScale;
        }
        shieye.y += Mth.sin(limbSwing * 0.5F) * 0.1F * limbSwingAmount;
    }

    /**
     * ✅ 叠加式攻击动画
     */
    private void applyAttackAnimationAdditive(float time) {
        float attackProgress = Math.min(time / 1.75F, 1.0F);
        float attackAngle = Mth.sin(attackProgress * Mth.PI) * 0.5F;

        if (group2 != null) {
            group2.xRot -= attackAngle * 0.8F;
        }
        if (group7 != null) {
            group7.xRot -= attackAngle * 1.2F;
        }
        if (group3 != null) {
            group3.xRot -= attackAngle * 1.5F;
        }
        float spread = Mth.sin(attackProgress * Mth.PI) * 0.3F;
        if (group5 != null) {
            group5.yRot += spread;
        }
        if (group6 != null) {
            group6.yRot -= spread;
        }
    }

    /**
     * ✅ 触发挥砍动画（由外部调用）
     */
    public void triggerSwing() {
        this.isSwinging = true;
        this.swingTime = 0.0F;
        resetAllParts();
    }

    /**
     * 重置所有部件姿势
     */
    private void resetAllParts() {
        safeReset(shieye);
        safeReset(group2);
        safeReset(group7);
        safeReset(group3);
        safeReset(group4);
        safeReset(group5);
        safeReset(group6);
        safeReset(rightItem);
    }

    private void safeReset(ModelPart part) {
        if (part != null) {
            part.resetPose();
        }
    }

    // ==================== 原有的 Getter 和 copyFromHead ====================

    public ModelPart getWaist() {
        return Waist;
    }

    public ModelPart getShieye() {
        return shieye;
    }

    public ModelPart getGroup2() {
        return group2;
    }

    public ModelPart getGroup7() {
        return group7;
    }

    public ModelPart getGroup3() {
        return group3;
    }

    public ModelPart getGroup4() {
        return group4;
    }

    public ModelPart getGroup5() {
        return group5;
    }

    public ModelPart getGroup6() {
        return group6;
    }

    public ModelPart getRightItem() {
        return rightItem;
    }

    public void copyFromHead(ModelPart head) {
        if (this.shieye != null && head != null) {
            PartPose headPose = head.storePose();
            this.shieye.loadPose(headPose);
            this.shieye.x += 1.1F;
            this.shieye.y -= -10.0F;
            this.shieye.z += 2.0F;
        }
    }
}