package xiaoshi2022.corpseorigin.client.model.clone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 把 {@link PlayerModel} 拆成 1×1×1 体素，按 {@link #completeness} 逐块生长。
 */
public class VoxelModel {

    private static final float VOXEL_SIZE = 1.0F;

    private final List<Voxel> voxels = new ArrayList<>();

    /** 0~1，决定显示前多少比例的体素 */
    public float completeness = 1.0F;

    public VoxelModel(PlayerModel parentModel) {
        PoseStack pose = new PoseStack();
        parentModel.root().visit(pose, (poseEntry, path, cubeIndex, cube) -> {
            collectVoxelsFromCube(poseEntry, cube);
        });
        this.voxels.sort(Comparator.comparingDouble(v -> v.worldY));
    }

    private void collectVoxelsFromCube(PoseStack.Pose pose, ModelPart.Cube cube) {
        float minX = cube.minX, minY = cube.minY, minZ = cube.minZ;
        float maxX = cube.maxX, maxY = cube.maxY, maxZ = cube.maxZ;

        for (float x = minX; x < maxX; x += VOXEL_SIZE) {
            for (float y = minY; y < maxY; y += VOXEL_SIZE) {
                for (float z = minZ; z < maxZ; z += VOXEL_SIZE) {
                    float cx = Math.min(x + VOXEL_SIZE, maxX);
                    float cy = Math.min(y + VOXEL_SIZE, maxY);
                    float cz = Math.min(z + VOXEL_SIZE, maxZ);

                    Vector3f center = pose.pose().transformPosition(
                            (x + cx) * 0.5F / 16.0F,
                            (y + cy) * 0.5F / 16.0F,
                            (z + cz) * 0.5F / 16.0F,
                            new Vector3f());

                    this.voxels.add(new Voxel(
                            new Matrix4f(pose.pose()),
                            cube,
                            x, y, z, cx, cy, cz,
                            center.y));
                }
            }
        }
    }

    public void render(PoseStack pose, VertexConsumer consumer,
                       int lightCoords, int overlayCoords, int color) {
        int visible = (int) (this.voxels.size() * this.completeness);
        for (int i = 0; i < visible; i++) {
            this.voxels.get(i).render(pose, consumer, lightCoords, overlayCoords, color);
        }
    }

    // ==================== 体素 ====================

    private static class Voxel {
        private final Matrix4f cubeTransform;
        private final ModelPart.Cube sourceCube;
        private final float minX, minY, minZ, maxX, maxY, maxZ;
        final float worldY;

        Voxel(Matrix4f cubeTransform, ModelPart.Cube sourceCube,
              float minX, float minY, float minZ,
              float maxX, float maxY, float maxZ,
              float worldY) {
            this.cubeTransform = cubeTransform;
            this.sourceCube = sourceCube;
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
            this.worldY = worldY;
        }

        void render(PoseStack pose, VertexConsumer consumer,
                    int lightCoords, int overlayCoords, int color) {
            for (Direction dir : Direction.values()) {
                ModelPart.Polygon polygon = findPolygon(dir);
                if (polygon == null) continue;

                ModelPart.Vertex[] verts = polygon.vertices();
                float u0 = verts[0].u(), v0 = verts[0].v();
                float u1 = verts[1].u(), v1 = verts[1].v();
                float u2 = verts[2].u(), v2 = verts[2].v();
                float u3 = verts[3].u(), v3 = verts[3].v();

                float cx = (this.minX - this.sourceCube.minX) / 16.0F;
                float cy = (this.minY - this.sourceCube.minY) / 16.0F;
                float cz = (this.minZ - this.sourceCube.minZ) / 16.0F;
                float sx = (this.maxX - this.minX) / 16.0F;
                float sy = (this.maxY - this.minY) / 16.0F;
                float sz = (this.maxZ - this.minZ) / 16.0F;

                pose.pushPose();
                pose.last().pose().mul(this.cubeTransform);
                pose.translate(cx, cy, cz);
                emitCubeFace(pose, consumer, dir, sx, sy, sz,
                        u0, v0, u1, v1, u2, v2, u3, v3,
                        lightCoords, overlayCoords, color);
                pose.popPose();
            }
        }

        private ModelPart.Polygon findPolygon(Direction dir) {
            for (ModelPart.Polygon p : this.sourceCube.polygons) {
                Vector3f n = new Vector3f(p.normal());
                Vector3f d = new Vector3f(dir.getStepX(), dir.getStepY(), dir.getStepZ());
                if (n.dot(d) > 0.9F) return p;
            }
            return null;
        }

        private static void emitCubeFace(PoseStack pose, VertexConsumer c, Direction dir,
                                         float sx, float sy, float sz,
                                         float u0, float v0, float u1, float v1,
                                         float u2, float v2, float u3, float v3,
                                         int light, int overlay, int color) {
            float x0, y0, z0, x1, y1, z1;
            switch (dir) {
                case DOWN  -> { x0 = 0;  y0 = 0;  z0 = 0;  x1 = sx; y1 = 0;  z1 = sz; }
                case UP    -> { x0 = 0;  y0 = sy; z0 = 0;  x1 = sx; y1 = sy; z1 = sz; }
                case NORTH -> { x0 = 0;  y0 = 0;  z0 = 0;  x1 = sx; y1 = sy; z1 = 0;  }
                case SOUTH -> { x0 = 0;  y0 = 0;  z0 = sz; x1 = sx; y1 = sy; z1 = sz; }
                case WEST  -> { x0 = 0;  y0 = 0;  z0 = 0;  x1 = 0;  y1 = sy; z1 = sz; }
                case EAST  -> { x0 = sx; y0 = 0;  z0 = 0;  x1 = sx; y1 = sy; z1 = sz; }
                default -> { return; }
            }
            vertex(pose, c, x0, y0, z0, u0, v0, light, overlay, color, dir);
            vertex(pose, c, x1, y0, z0, u1, v1, light, overlay, color, dir);
            vertex(pose, c, x1, y1, z1, u2, v2, light, overlay, color, dir);
            vertex(pose, c, x0, y1, z1, u3, v3, light, overlay, color, dir);
        }

        private static void vertex(PoseStack pose, VertexConsumer c,
                                   float x, float y, float z,
                                   float u, float v,
                                   int light, int overlay, int color,
                                   Direction dir) {
            c.addVertex(pose.last(), x, y, z)
                    .setColor(color)
                    .setUv(u, v)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose.last(), dir.getStepX(), dir.getStepY(), dir.getStepZ());
        }
    }
}