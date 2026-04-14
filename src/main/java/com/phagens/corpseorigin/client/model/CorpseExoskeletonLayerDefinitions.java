package com.phagens.corpseorigin.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/**
 * 尸化外骨骼程序化模型定义
 * 包含：羽翼和鱼尾模型
 */
public class CorpseExoskeletonLayerDefinitions {

    // 羽翼模型层位置
    public static final ModelLayerLocation WING_LAYER = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("corpseorigin", "corpse_wing"), "main");
    
    // 鱼尾模型层位置
    public static final ModelLayerLocation TAIL_LAYER = 
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("corpseorigin", "corpse_tail"), "main");

    /**
     * 创建羽翼模型层定义
     */
    public static LayerDefinition createWingLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition Head = Waist.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F))
        .texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition Body = Waist.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition Corpsewings = Body.addOrReplaceChild("Corpsewings", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition L = Corpsewings.addOrReplaceChild("L", CubeListBuilder.create().texOffs(24, 48).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.8F, 5.8F, 2.9F));

        PartDefinition cube_r1 = L.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 48).addBox(-0.5F, -9.2F, -1.0F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -0.8F, 1.6F, 0.0F, 0.0F, 0.6545F));

        PartDefinition bone = L.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offsetAndRotation(6.1568F, -7.4321F, 1.1F, 1.2877F, 1.2535F, -0.2697F));

        PartDefinition cube_r2 = bone.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(44, 48).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.3F, -6.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r3 = bone.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(48, 39).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.3F, -5.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r4 = bone.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(48, 32).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.3F, -4.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r5 = bone.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(48, 32).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.3F, -3.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r6 = bone.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(32, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.3F, -2.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r7 = bone.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(32, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.3F, -1.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r8 = bone.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(20, 48).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.3F, -0.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r9 = bone.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(16, 48).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.3F, 0.0F, 0.0F, 0.0F, 0.6545F));

        PartDefinition bone4 = bone.addOrReplaceChild("bone4", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.3F, -0.4306F, -0.0091F));

        PartDefinition cube_r10 = bone4.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(32, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.5F, -1.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r11 = bone4.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(20, 48).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.5F, -0.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r12 = bone4.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(32, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.5F, -2.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r13 = bone4.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(36, 48).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.5F, -3.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r14 = bone4.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(48, 44).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.5F, -5.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r15 = bone4.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(40, 48).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.5F, -4.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r16 = bone4.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(48, 47).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.5F, -6.9F, 0.0F, 0.2182F, 0.6545F));

        PartDefinition cube_r17 = bone4.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(16, 48).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.1F, 3.3F, 0.0F, 0.0F, 0.0F, 0.6545F));

        PartDefinition R = Corpsewings.addOrReplaceChild("R", CubeListBuilder.create().texOffs(24, 48).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.1F, 5.8F, 2.9F));

        PartDefinition cube_r18 = R.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(16, 48).addBox(-0.5F, -9.2F, -1.0F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1F, -0.8F, 1.6F, 0.0F, 0.0F, -0.6545F));

        PartDefinition bone2 = R.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offsetAndRotation(-5.0568F, -7.5321F, 1.1F, 0.7777F, -1.3246F, 0.7777F));

        PartDefinition cube_r19 = bone2.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(16, 48).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4F, 3.4F, 0.0F, 0.0F, 0.0F, -0.6545F));

        PartDefinition bone3 = bone2.addOrReplaceChild("bone3", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -2.5772F, 0.5545F, 1.8024F));

        PartDefinition cube_r20 = bone3.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(16, 48).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.4F, 3.4F, 0.0F, 0.0F, 0.0F, -0.6545F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    /**
     * 创建鱼尾模型层定义
     */
    public static LayerDefinition createTailLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Waist = partdefinition.addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition Head = Waist.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F))
        .texOffs(0, 17).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition Body = Waist.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(33, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(33, 17).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition Corpsetail = Body.addOrReplaceChild("Corpsetail", CubeListBuilder.create().texOffs(58, 7).addBox(-2.0F, 9.0F, 2.1F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition dorsal = Corpsetail.addOrReplaceChild("dorsal", CubeListBuilder.create().texOffs(58, 21).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(58, 27).addBox(-1.0F, -1.0F, 0.5F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(58, 27).addBox(-1.0F, -0.5F, 1.5F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.0F, 2.6F));

        PartDefinition bone = Corpsetail.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(58, 7).addBox(-1.95F, -1.375F, 0.05F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(58, 0).addBox(-1.95F, -0.375F, 1.05F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(58, 13).addBox(0.05F, 2.625F, 1.85F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
        .texOffs(58, 17).addBox(-3.15F, 2.625F, 1.85F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.05F, 11.375F, 3.05F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}