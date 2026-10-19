package com.xiaoshi2022.corpseorigin.client.render;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 感染体叠加器官的共享资源（静脉 / 肋骨 / 眼睛贴图与烘焙模型）。
 * <p>
 * 同时供原版 {@code NativeCorpseLayer} 和 GeckoLib {@code NativeCorpseGeoLayer} 使用，
 * 保证两种渲染管线下器官外观一致。
 */
public final class NativeCorpseOrgans {

    public static final ResourceLocation VEINS =
            new ResourceLocation(CorpseOrigin.MODID, "textures/entity/native_corpse_veins.png");
    public static final ResourceLocation TISSUE =
            new ResourceLocation(CorpseOrigin.MODID, "textures/entity/native_corpse_organs.png");
    public static final ResourceLocation EYE =
            new ResourceLocation(CorpseOrigin.MODID, "textures/entity/native_corpse_eye.png");

    private static ModelPart ribs;
    private static ModelPart eye;

    private NativeCorpseOrgans() {}

    /** 肋骨模型（惰性烘焙，只在客户端首次渲染时创建）。 */
    public static ModelPart ribs() {
        if (ribs == null) ribs = bakeRibs();
        return ribs;
    }

    /** 眼睛模型（惰性烘焙）。 */
    public static ModelPart eye() {
        if (eye == null) eye = bakeEye();
        return eye;
    }

    private static ModelPart bakeRibs() {
        CubeListBuilder cubes = CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3.3F, -4.5F, -0.5F, 6.6F, 9.0F, 0.8F)
                .texOffs(0, 0).addBox(-0.4F, -4.5F, -1.3F, 0.8F, 9.0F, 0.8F);
        for (int i = 0; i < 4; i++) {
            float y = -3.8F + i * 2.2F;
            cubes.texOffs(0, 0).addBox(-3.7F, y, -1.2F, 3.4F, 0.65F, 0.8F)
                    .addBox(0.3F, y, -1.2F, 3.4F, 0.65F, 0.8F)
                    .addBox(-4.0F, y, -1.0F, 0.6F, 1.3F, 1.5F)
                    .addBox(3.4F, y, -1.0F, 0.6F, 1.3F, 1.5F);
        }
        return bake(cubes, "ribs", 64, 64);
    }

    private static ModelPart bakeEye() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition shieye = root.addOrReplaceChild(
                "shieye",
                CubeListBuilder.create()
                        .texOffs(49, 46)
                        .addBox(-0.625F, -0.75F, -0.6F, 1.5F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO
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

        return LayerDefinition.create(mesh, 64, 64).bakeRoot().getChild("shieye");
    }

    private static ModelPart bake(CubeListBuilder cubes, String name, int texWidth, int texHeight) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(name, cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, texWidth, texHeight).bakeRoot().getChild(name);
    }
}
