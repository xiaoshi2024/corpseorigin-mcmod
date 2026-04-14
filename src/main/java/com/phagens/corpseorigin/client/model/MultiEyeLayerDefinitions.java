package com.phagens.corpseorigin.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * 多眼系统模型定义
 * 像盔甲层一样，直接绑定到Head骨骼
 */
public class MultiEyeLayerDefinitions {

    public static final ModelLayerLocation MULTI_EYE_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("corpseorigin", "multi_eye"), "main");

    /**
     * 创建多眼模型层定义
     * 像盔甲层一样，MultipleEyes作为根部件
     * 渲染时会通过 translateAndRotate 应用Head的变换
     */
    public static LayerDefinition createMultiEyeLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // MultipleEyes 作为根部件
        // 位置是相对于Head的偏移，向脸部内陷一些
        PartDefinition MultipleEyes = partdefinition.addOrReplaceChild("MultipleEyes", CubeListBuilder.create(), PartPose.offset(0.0F, -4.0F, -2.0F));

        // === 眼睛组1 (group30) - 额头中央 ===
        PartDefinition group30 = MultipleEyes.addOrReplaceChild("group30", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.7F, 0.0F, 3.1416F, 1.1345F, 2.0508F));

        group30.addOrReplaceChild("group10_r1", CubeListBuilder.create().texOffs(56, 0).addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F, 0.0F, 1.0472F, 0.0F));

        PartDefinition group31 = group30.addOrReplaceChild("group31", CubeListBuilder.create().texOffs(56, 44).addBox(-0.2375F, -1.05F, -0.9875F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(64, 55).addBox(-0.0375F, -0.95F, -0.5875F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7388F, -0.038F, 2.0416F));

        group31.addOrReplaceChild("group32", CubeListBuilder.create().texOffs(68, 9).addBox(-0.8F, -1.05F, -0.65F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, 0.7625F));

        group31.addOrReplaceChild("group33", CubeListBuilder.create().texOffs(58, 64).addBox(-0.8F, -1.05F, -0.25F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, -0.8375F));

        // === 眼睛组2 (group2) - 额头偏左 ===
        PartDefinition group2 = MultipleEyes.addOrReplaceChild("group2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -1.8F, 0.0F, 3.1416F, 1.1345F, 2.0508F));

        group2.addOrReplaceChild("group11_r1", CubeListBuilder.create().texOffs(56, 6).addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F, 0.0F, 1.0472F, 0.0F));

        PartDefinition group3 = group2.addOrReplaceChild("group3", CubeListBuilder.create().texOffs(62, 44).addBox(-0.2375F, -1.05F, -0.9875F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(64, 58).addBox(-0.0375F, -0.95F, -0.5875F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7388F, -0.038F, 2.0416F));

        group3.addOrReplaceChild("group4", CubeListBuilder.create().texOffs(68, 12).addBox(-0.8F, -1.05F, -0.65F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, 0.7625F));

        group3.addOrReplaceChild("group5", CubeListBuilder.create().texOffs(62, 64).addBox(-0.8F, -1.05F, -0.25F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, -0.8375F));

        // === 眼睛组3 (group6) - 额头偏右 ===
        PartDefinition group6 = MultipleEyes.addOrReplaceChild("group6", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.7F, 0.0F, 3.1416F, 1.1345F, 2.0508F));

        group6.addOrReplaceChild("group12_r1", CubeListBuilder.create().texOffs(0, 56).addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F, 0.0F, 1.0472F, 0.0F));

        PartDefinition group7 = group6.addOrReplaceChild("group7", CubeListBuilder.create().texOffs(0, 62).addBox(-0.2375F, -1.05F, -0.9875F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(6, 64).addBox(-0.0375F, -0.95F, -0.5875F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7388F, -0.038F, 2.0416F));

        group7.addOrReplaceChild("group8", CubeListBuilder.create().texOffs(68, 15).addBox(-0.8F, -1.05F, -0.65F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, 0.7625F));

        group7.addOrReplaceChild("group9", CubeListBuilder.create().texOffs(66, 64).addBox(-0.8F, -1.05F, -0.25F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, -0.8375F));

        // === 眼睛组4 (group10) - 左脸颊 ===
        PartDefinition group10 = MultipleEyes.addOrReplaceChild("group10", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.0F, 2.0F, 0.0F, 3.1416F, 1.1345F, 2.0508F));

        group10.addOrReplaceChild("group13_r1", CubeListBuilder.create().texOffs(8, 56).addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F, 0.0F, 1.0472F, 0.0F));

        PartDefinition group11 = group10.addOrReplaceChild("group11", CubeListBuilder.create().texOffs(20, 56).addBox(-0.2375F, -1.05F, -0.9875F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(26, 64).addBox(-0.0375F, -0.95F, -0.5875F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7388F, -0.038F, 2.0416F));

        group11.addOrReplaceChild("group12", CubeListBuilder.create().texOffs(68, 18).addBox(-0.8F, -1.05F, -0.65F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, 0.7625F));

        group11.addOrReplaceChild("group13", CubeListBuilder.create().texOffs(70, 64).addBox(-0.8F, -1.05F, -0.25F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, -0.8375F));

        // === 眼睛组5 (group14) - 右脸颊 ===
        PartDefinition group14 = MultipleEyes.addOrReplaceChild("group14", CubeListBuilder.create(), PartPose.offsetAndRotation(2.0F, 2.0F, 0.0F, 3.1416F, 1.1345F, 2.0508F));

        group14.addOrReplaceChild("group14_r1", CubeListBuilder.create().texOffs(16, 56).addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F, 0.0F, 1.0472F, 0.0F));

        PartDefinition group15 = group14.addOrReplaceChild("group15", CubeListBuilder.create().texOffs(32, 56).addBox(-0.2375F, -1.05F, -0.9875F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(38, 64).addBox(-0.0375F, -0.95F, -0.5875F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7388F, -0.038F, 2.0416F));

        group15.addOrReplaceChild("group16", CubeListBuilder.create().texOffs(68, 21).addBox(-0.8F, -1.05F, -0.65F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, 0.7625F));

        group15.addOrReplaceChild("group17", CubeListBuilder.create().texOffs(74, 64).addBox(-0.8F, -1.05F, -0.25F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, -0.8375F));

        // === 眼睛组6 (group18) - 左太阳穴 ===
        PartDefinition group18 = MultipleEyes.addOrReplaceChild("group18", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.8F, 0.0F, 0.0F, 3.1416F, 1.1345F, 2.0508F));

        group18.addOrReplaceChild("group15_r1", CubeListBuilder.create().texOffs(32, 60).addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F, 0.0F, 1.0472F, 0.0F));

        PartDefinition group19 = group18.addOrReplaceChild("group19", CubeListBuilder.create().texOffs(40, 56).addBox(-0.2375F, -1.05F, -0.9875F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(46, 64).addBox(-0.0375F, -0.95F, -0.5875F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7388F, -0.038F, 2.0416F));

        group19.addOrReplaceChild("group20", CubeListBuilder.create().texOffs(68, 24).addBox(-0.8F, -1.05F, -0.65F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, 0.7625F));

        group19.addOrReplaceChild("group21", CubeListBuilder.create().texOffs(78, 64).addBox(-0.8F, -1.05F, -0.25F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, -0.8375F));

        // === 眼睛组7 (group22) - 右太阳穴 ===
        PartDefinition group22 = MultipleEyes.addOrReplaceChild("group22", CubeListBuilder.create(), PartPose.offsetAndRotation(1.8F, 0.0F, 0.0F, 3.1416F, 1.1345F, 2.0508F));

        group22.addOrReplaceChild("group16_r1", CubeListBuilder.create().texOffs(40, 60).addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F, 0.0F, 1.0472F, 0.0F));

        PartDefinition group23 = group22.addOrReplaceChild("group23", CubeListBuilder.create().texOffs(48, 56).addBox(-0.2375F, -1.05F, -0.9875F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(54, 64).addBox(-0.0375F, -0.95F, -0.5875F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7388F, -0.038F, 2.0416F));

        group23.addOrReplaceChild("group24", CubeListBuilder.create().texOffs(68, 27).addBox(-0.8F, -1.05F, -0.65F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, 0.7625F));

        group23.addOrReplaceChild("group25", CubeListBuilder.create().texOffs(82, 64).addBox(-0.8F, -1.05F, -0.25F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, -0.8375F));

        // === 眼睛组8 (group26) - 下巴左侧 ===
        PartDefinition group26 = MultipleEyes.addOrReplaceChild("group26", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0F, 3.5F, 0.0F, 3.1416F, 1.1345F, 2.0508F));

        group26.addOrReplaceChild("group17_r1", CubeListBuilder.create().texOffs(48, 60).addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F, 0.0F, 1.0472F, 0.0F));

        PartDefinition group27 = group26.addOrReplaceChild("group27", CubeListBuilder.create().texOffs(56, 56).addBox(-0.2375F, -1.05F, -0.9875F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(64, 61).addBox(-0.0375F, -0.95F, -0.5875F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7388F, -0.038F, 2.0416F));

        group27.addOrReplaceChild("group28", CubeListBuilder.create().texOffs(68, 30).addBox(-0.8F, -1.05F, -0.65F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, 0.7625F));

        group27.addOrReplaceChild("group29", CubeListBuilder.create().texOffs(86, 64).addBox(-0.8F, -1.05F, -0.25F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, -0.8375F));

        // === 眼睛组9 (group34) - 下巴右侧 ===
        PartDefinition group34 = MultipleEyes.addOrReplaceChild("group34", CubeListBuilder.create(), PartPose.offsetAndRotation(1.4F, 2.2F, 0.0F, 3.1416F, 1.1345F, 2.0508F));

        group34.addOrReplaceChild("group18_r1", CubeListBuilder.create().texOffs(56, 0).addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F, 0.0F, 1.0472F, 0.0F));

        PartDefinition group35 = group34.addOrReplaceChild("group35", CubeListBuilder.create().texOffs(56, 44).addBox(-0.2375F, -1.05F, -0.9875F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(64, 55).addBox(-0.0375F, -0.95F, -0.5875F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7388F, -0.038F, 2.0416F));

        group35.addOrReplaceChild("group36", CubeListBuilder.create().texOffs(62, 67).addBox(-0.8F, -1.05F, -0.65F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, 0.7625F));

        group35.addOrReplaceChild("group37", CubeListBuilder.create().texOffs(54, 64).addBox(-0.8F, -1.05F, -0.25F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.9625F, 0.0F, -0.8375F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
