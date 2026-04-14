package com.phagens.corpseorigin.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * 尸化外骨骼程序化模型定义
 * 基于“尸眼”模型的精确几何结构
 */
public class ExoskeletonLayerDefinitions {

    public static final ModelLayerLocation EXOSKELETON_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("corpseorigin", "exoskeleton"), "main");

    /**
     * 创建外骨骼模型层定义
     * 包含：主眼部、触须段1-3、末端分叉
     */
    public static LayerDefinition createExoskeleton() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        // 腰部 (Waist) - 作为根部件
        PartDefinition Waist = root.addOrReplaceChild("Waist",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        // 头部 (Head) - 腰部子部件
        PartDefinition Head = Waist.addOrReplaceChild("Head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F))
                        .texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -12.0F, 0.0F));

        // 主眼部 (shieye) - 头部子部件（精确位置）
        PartDefinition shieye = Head.addOrReplaceChild("shieye",
                CubeListBuilder.create()
                        .texOffs(56, 14)
                        .addBox(-0.125F, -0.75F, -0.6F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.725F, 0.05F, 2.6F));

        // 触须第一段 (group2) - 作为空组，包含旋转立方体
        PartDefinition group2 = shieye.addOrReplaceChild("group2",
                CubeListBuilder.create(),
                PartPose.offset(-0.125F, 0.25F, 0.1F));

        // cube_r1 - 实际的触须第一段几何
        group2.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(56, 32)
                        .addBox(-0.3F, -1.0F, 0.3F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
                        0.0F, 1.0472F, 0.0F)); // 60度 Y轴旋转

        // 触须第二段 (group7) - 精确位置和旋转
        PartDefinition group7 = group2.addOrReplaceChild("group7",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(5.1456F, -0.8823F, 2.7108F,
                        0.7777F, -0.1231F, 0.124F)); // 约44.56°, -7.05°, 7.11°

        // cube_r2 - 实际的触须第二段几何
        group7.addOrReplaceChild("cube_r2",
                CubeListBuilder.create()
                        .texOffs(56, 0)
                        .addBox(-0.3F, -1.0F, 0.3F, 1.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.3455F, 0.8823F, 0.0892F,
                        1.309F, 1.0472F, 0.0F)); // 75°, 60°, 0°

        // 触须第三段 (group3)
        PartDefinition group3 = group7.addOrReplaceChild("group3",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.905F, -6.2957F, 0.4988F,
                        0.0F, 1.0472F, -1.0908F)); // 0°, 60°, -62.5°

        // cube_r3 - 实际的触须第三段几何
        group3.addOrReplaceChild("cube_r3",
                CubeListBuilder.create()
                        .texOffs(56, 8)
                        .addBox(-0.55F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.8264F, 0.012F, 1.0791F,
                        0.0F, 1.0472F, 0.0F)); // 60° Y轴

        // 末端分叉 (group4)
        PartDefinition group4 = group3.addOrReplaceChild("group4",
                CubeListBuilder.create()
                        .texOffs(56, 38).addBox(-0.2375F, -1.05F, -0.9875F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 42).addBox(-0.0375F, -0.95F, -0.5875F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(3.7388F, -0.038F, 2.0416F));

        // 小分叉1 (group5)
        group4.addOrReplaceChild("group5",
                CubeListBuilder.create()
                        .texOffs(56, 45)
                        .addBox(-0.8F, -1.05F, -0.65F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.9625F, 0.0F, 0.7625F));

        // 小分叉2 (group6)
        group4.addOrReplaceChild("group6",
                CubeListBuilder.create()
                        .texOffs(60, 42)
                        .addBox(-0.8F, -1.05F, -0.25F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.9625F, 0.0F, -0.8375F));

        // 可选：右手物品 (rightItem) - 如果需要
        // shieye.addOrReplaceChild("rightItem", CubeListBuilder.create(), PartPose.offset(8.475F, -5.45F, -2.0F));

        return LayerDefinition.create(meshDefinition, 128, 128); // 纹理大小更新为 128x128
    }
}