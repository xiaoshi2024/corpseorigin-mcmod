package com.phagens.corpseorigin.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.phagens.corpseorigin.entity.CorpseGibEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/**
 * 尸体残肢模型
 *
 * 【功能说明】
 * 完全参照 Mob-Dismemberment 1.12.2 的 ModelGib 设计
 * 使用预定义的模型部件渲染不同类型的残肢
 *
 * 【UV贴图参照 ModelGib】
 * 64x64 纹理 (僵尸等):
 * - head64: (0, 0), addBox(-4F, -4F, -4F, 8, 8, 8), setRotationPoint(0F, 20F, 0F)
 * - body64: (16, 16), addBox(-4F, -6F, -2F, 8, 12, 4), setRotationPoint(0F, 22F, 0F)
 * - arm64: (40, 16), addBox(-2F, -6F, -2F, 4, 12, 4), setRotationPoint(0F, 22F, 0F)
 * - leg64: (0, 16), addBox(-2F, -6F, -2F, 4, 12, 4), setRotationPoint(0F, 24F, 0F)
 *
 * 64x32 纹理 (骷髅等):
 * - head32: (0, 0), addBox(-4F, -4F, -4F, 8, 8, 8), setRotationPoint(0F, 20F, 0F)
 * - body32: (16, 16), addBox(-4F, -6F, -2F, 8, 12, 4), setRotationPoint(0F, 22F, 0F)
 * - skeleArm: (40, 16), addBox(-1F, -6F, -1F, 2, 12, 2), setRotationPoint(0F, 24F, 0F)
 * - skeleLeg: (0, 16), addBox(-1F, -6F, -1F, 2, 12, 2), setRotationPoint(0F, 24F, 0F)
 *
 * 苦力怕脚:
 * - creeperFoot: (0, 16), addBox(-2F, -3F, -2F, 4, 6, 4), setRotationPoint(0F, 24F, 0F)
 */
public class CorpseGibModel extends EntityModel<CorpseGibEntity> {

    // 64x64 纹理的部件 (僵尸等)
    private final ModelPart head64;
    private final ModelPart body64;
    private final ModelPart arm64;
    private final ModelPart leg64;

    // 64x32 纹理的部件 (骷髅等)
    private final ModelPart head32;
    private final ModelPart body32;
    private final ModelPart skeleArm;
    private final ModelPart skeleLeg;

    // 苦力怕脚
    private final ModelPart creeperFoot;

    // 当前渲染状态
    private int currentType = 0;
    private boolean use64x64 = true;

    public CorpseGibModel() {
        // 创建 64x64 模型部件 - 完全参照 ModelGib
        MeshDefinition mesh64 = new MeshDefinition();
        PartDefinition root64 = mesh64.getRoot();

        // head64 = new ModelRenderer(this, 0, 0);
        // head64.addBox(-4F, -4F, -4F, 8, 8, 8);
        // head64.setRotationPoint(0F, 20F, 0F);
        root64.addOrReplaceChild("head64",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));

        // body64 = new ModelRenderer(this, 16, 16);
        // body64.addBox(-4F, -6F, -2F, 8, 12, 4);
        // body64.setRotationPoint(0F, 22F, 0F);
        root64.addOrReplaceChild("body64",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, -6.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));

        // arm64 = new ModelRenderer(this, 40, 16);
        // arm64.addBox(-2F, -6F, -2F, 4, 12, 4);
        // arm64.setRotationPoint(0F, 22F, 0F);
        root64.addOrReplaceChild("arm64",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-2.0F, -6.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));

        // leg64 = new ModelRenderer(this, 0, 16);
        // leg64.addBox(-2F, -6F, -2F, 4, 12, 4);
        // leg64.setRotationPoint(0F, 24F, 0F);
        root64.addOrReplaceChild("leg64",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, -6.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        LayerDefinition layer64 = LayerDefinition.create(mesh64, 64, 64);
        ModelPart root64Part = layer64.bakeRoot();
        this.head64 = root64Part.getChild("head64");
        this.body64 = root64Part.getChild("body64");
        this.arm64 = root64Part.getChild("arm64");
        this.leg64 = root64Part.getChild("leg64");

        // 创建 64x32 模型部件 (骷髅等) - 完全参照 ModelGib
        MeshDefinition mesh32 = new MeshDefinition();
        PartDefinition root32 = mesh32.getRoot();

        // head32 = new ModelRenderer(this, 0, 0);
        // head32.addBox(-4F, -4F, -4F, 8, 8, 8);
        // head32.setRotationPoint(0F, 20F, 0F);
        root32.addOrReplaceChild("head32",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));

        // body32 = new ModelRenderer(this, 16, 16);
        // body32.addBox(-4F, -6F, -2F, 8, 12, 4);
        // body32.setRotationPoint(0F, 22F, 0F);
        root32.addOrReplaceChild("body32",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, -6.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));

        // skeleArm = new ModelRenderer(this, 40, 16);
        // skeleArm.addBox(-1F, -6F, -1F, 2, 12, 2);
        // skeleArm.setRotationPoint(0F, 24F, 0F);
        root32.addOrReplaceChild("skeleArm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-1.0F, -6.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // skeleLeg = new ModelRenderer(this, 0, 16);
        // skeleLeg.addBox(-1F, -6F, -1F, 2, 12, 2);
        // skeleLeg.setRotationPoint(0F, 24F, 0F);
        root32.addOrReplaceChild("skeleLeg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-1.0F, -6.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        LayerDefinition layer32 = LayerDefinition.create(mesh32, 64, 32);
        ModelPart root32Part = layer32.bakeRoot();
        this.head32 = root32Part.getChild("head32");
        this.body32 = root32Part.getChild("body32");
        this.skeleArm = root32Part.getChild("skeleArm");
        this.skeleLeg = root32Part.getChild("skeleLeg");

        // 创建苦力怕脚 - 参照 ModelGib
        MeshDefinition meshCreeper = new MeshDefinition();
        PartDefinition rootCreeper = meshCreeper.getRoot();

        // creeperFoot = new ModelRenderer(this, 0, 16);
        // creeperFoot.addBox(-2F, -3F, -2F, 4, 6, 4);
        // creeperFoot.setRotationPoint(0F, 24F, 0F);
        rootCreeper.addOrReplaceChild("creeperFoot",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, -3.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        LayerDefinition layerCreeper = LayerDefinition.create(meshCreeper, 64, 32);
        ModelPart rootCreeperPart = layerCreeper.bakeRoot();
        this.creeperFoot = rootCreeperPart.getChild("creeperFoot");
    }

    /**
     * 设置渲染参数
     * @param type 残肢类型
     * @param use64x64 是否使用 64x64 纹理
     */
    public void setRenderParams(int type, boolean use64x64) {
        this.currentType = type;
        this.use64x64 = use64x64;
    }

    @Override
    public void setupAnim(CorpseGibEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // 残肢不需要动画，由实体自身控制旋转
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        // 根据类型和纹理格式渲染对应的部件
        // 参照 ModelGib.render 的逻辑

        switch (currentType) {
            case CorpseGibEntity.GIB_TYPE_HEAD -> {
                if (use64x64) {
                    head64.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                } else {
                    head32.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                }
            }
            case CorpseGibEntity.GIB_TYPE_LEFT_ARM, CorpseGibEntity.GIB_TYPE_RIGHT_ARM -> {
                if (use64x64) {
                    arm64.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                } else {
                    skeleArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                }
            }
            case CorpseGibEntity.GIB_TYPE_BODY -> {
                if (use64x64) {
                    body64.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                } else {
                    body32.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                }
            }
            case CorpseGibEntity.GIB_TYPE_LEFT_LEG, CorpseGibEntity.GIB_TYPE_RIGHT_LEG -> {
                if (use64x64) {
                    leg64.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                } else {
                    skeleLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                }
            }
            default -> {
                // 苦力怕脚或其他类型
                creeperFoot.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
            }
        }
    }
}
