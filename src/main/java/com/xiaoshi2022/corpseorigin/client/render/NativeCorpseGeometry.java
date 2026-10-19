package com.xiaoshi2022.corpseorigin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;

/** Measures baked vertices, including CubeDeformation, in model pixels. */
public final class NativeCorpseGeometry {
    private NativeCorpseGeometry() {}

    public record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {}

    public record Placement(float x, float y, float z, float scale) {}

    /** Match Fabric 26.2: the eye starts at the forehead and its connected stalk grows outward. */
    public static Placement sproutingEye(Bounds head) {
        float width = Math.max(.5F, head.maxX - head.minX);
        float height = Math.max(.5F, head.maxY - head.minY);
        float scale = Math.max(.15F, Math.min(width, height) / 7F);
        return new Placement((head.minX + head.maxX) / 2F,
                head.minY + height * .32F, head.minZ - .15F, scale);
    }
    public static Bounds bounds(ModelPart.Cube cube, PoseStack poses) {
        Extents vertices = new Extents();
        cube.compile(poses.last(), vertices, 0, 0, 1, 1, 1, 1);
        return new Bounds(vertices.minX, vertices.minY, vertices.minZ,
                vertices.maxX, vertices.maxY, vertices.maxZ);
    }

    public static Bounds bounds(ModelPart part) {
        Extents vertices = new Extents();
        part.visit(new PoseStack(), (pose, path, index, cube) ->
                cube.compile(pose, vertices, 0, 0, 1, 1, 1, 1));
        return new Bounds(vertices.minX, vertices.minY, vertices.minZ,
                vertices.maxX, vertices.maxY, vertices.maxZ);
    }

    private static final class Extents implements VertexConsumer {
        private float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;

        @Override public VertexConsumer vertex(double x, double y, double z) {
            minX = Math.min(minX, (float) x * 16); maxX = Math.max(maxX, (float) x * 16);
            minY = Math.min(minY, (float) y * 16); maxY = Math.max(maxY, (float) y * 16);
            minZ = Math.min(minZ, (float) z * 16); maxZ = Math.max(maxZ, (float) z * 16);
            return this;
        }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() {}
        @Override public void defaultColor(int r, int g, int b, int a) {}
        @Override public void unsetDefaultColor() {}
    }
}
