package xiaoshi2022.corpseorigin.client.model.clone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 把 {@link PlayerModel} 拆成 1×1×1（模型像素）体素，按 {@link #completeness} 从脚往头逐块生长。
 * <p>
 * 注意模型空间是"头在负 y、脚在正 y"，而渲染时会对 Y 翻转，
 * 所以必须按模型 y 降序排才是从脚下往上长。
 */
public class VoxelModel {

    private static final float VOXEL_SIZE = 1.0F;

    private final List<Voxel> voxels = new ArrayList<>();

    /** 0~1，决定显示前多少比例的体素 */
    public float completeness = 1.0F;

    public VoxelModel(PlayerModel parentModel) {
        PoseStack pose = new PoseStack();
        parentModel.root().visit(pose, (poseEntry, path, cubeIndex, cube) ->
                collectVoxelsFromCube(poseEntry, cube));
        // 模型 y 越大越靠下（脚），降序 => 先长脚，再往上长到头
        this.voxels.sort(Comparator.comparingDouble((Voxel voxel) -> voxel.modelY).reversed());
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

                    float centerY = pose.pose().transformPosition(
                            (x + cx) * 0.5F / 16.0F,
                            (y + cy) * 0.5F / 16.0F,
                            (z + cz) * 0.5F / 16.0F,
                            new Vector3f()).y;

                    this.voxels.add(new Voxel(
                            new Matrix4f(pose.pose()),
                            cube,
                            x, y, z, cx, cy, cz,
                            centerY));
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

    private static final class Voxel {
        private final Matrix4f cubeTransform;
        private final ModelPart.Cube sourceCube;
        private final float minX, minY, minZ, maxX, maxY, maxZ;
        /** 体素中心在模型空间里的 y（模型是头在上、脚在下） */
        final float modelY;

        Voxel(Matrix4f cubeTransform, ModelPart.Cube sourceCube,
              float minX, float minY, float minZ,
              float maxX, float maxY, float maxZ,
              float modelY) {
            this.cubeTransform = cubeTransform;
            this.sourceCube = sourceCube;
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
            this.modelY = modelY;
        }

        void render(PoseStack pose, VertexConsumer consumer,
                    int lightCoords, int overlayCoords, int color) {
            for (ModelPart.Polygon polygon : this.sourceCube.polygons) {
                this.emitSlicedFace(pose, consumer, polygon, lightCoords, overlayCoords, color);
            }
        }

        /**
         * 只画这个体素覆盖的那一小块面，贴图按同样比例取。
         * <p>
         * 直接把整面的四个 UV 贴到每块体素上是不行的：那样每块体素都会显示一整张脸，
         * 满屏拼起来就完全不是原来的皮肤了。
         */
        private void emitSlicedFace(PoseStack pose, VertexConsumer consumer, ModelPart.Polygon polygon,
                                    int light, int overlay, int color) {
            ModelPart.Vertex[] verts = polygon.vertices();
            Vector3f normal = new Vector3f(polygon.normal());
            int normalAxis = Math.abs(normal.x()) > 0.5F ? 0 : (Math.abs(normal.y()) > 0.5F ? 1 : 2);
            int axisA = (normalAxis + 1) % 3;
            int axisB = (normalAxis + 2) % 3;

            // 面内两轴的范围（cube 局部像素）
            float aMin = Float.MAX_VALUE, aMax = -Float.MAX_VALUE;
            float bMin = Float.MAX_VALUE, bMax = -Float.MAX_VALUE;
            for (ModelPart.Vertex vertex : verts) {
                aMin = Math.min(aMin, vertexCoord(vertex, axisA));
                aMax = Math.max(aMax, vertexCoord(vertex, axisA));
                bMin = Math.min(bMin, vertexCoord(vertex, axisB));
                bMax = Math.max(bMax, vertexCoord(vertex, axisB));
            }
            float aSpan = Math.max(aMax - aMin, 1.0F);
            float bSpan = Math.max(bMax - bMin, 1.0F);

            // 四个角的贴图，按角在面内的 (s,t) 归位（镜像/翻转都能兼容）
            float[] cornerU = new float[4];
            float[] cornerV = new float[4];
            for (ModelPart.Vertex vertex : verts) {
                float s = (vertexCoord(vertex, axisA) - aMin) / aSpan;
                float t = (vertexCoord(vertex, axisB) - bMin) / bSpan;
                int index = (s > 0.5F ? 1 : 0) | (t > 0.5F ? 2 : 0);
                cornerU[index] = vertex.u();
                cornerV[index] = vertex.v();
            }

            // 体素覆盖的 (s,t) 区间
            float s0 = (this.localMin(axisA) - aMin) / aSpan;
            float s1 = (this.localMax(axisA) - aMin) / aSpan;
            float t0 = (this.localMin(axisB) - bMin) / bSpan;
            float t1 = (this.localMax(axisB) - bMin) / bSpan;

            pose.pushPose();
            pose.last().pose().mul(this.cubeTransform);
            // 按源顶点顺序输出，绕序与正面朝向跟原模型一致
            for (ModelPart.Vertex vertex : verts) {
                float s = vertexCoord(vertex, axisA) > (aMin + aMax) * 0.5F ? s1 : s0;
                float t = vertexCoord(vertex, axisB) > (bMin + bMax) * 0.5F ? t1 : t0;
                float a = s * aSpan + aMin;
                float b = t * bSpan + bMin;

                Vector3f local = new Vector3f();
                setCoord(local, axisA, a / 16.0F);
                setCoord(local, axisB, b / 16.0F);
                setCoord(local, normalAxis, vertexCoord(vertex, normalAxis) / 16.0F);

                consumer.addVertex(pose.last(), local.x(), local.y(), local.z())
                        .setColor(color)
                        .setUv(bilinear(cornerU, s, t), bilinear(cornerV, s, t))
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(pose.last(), normal.x(), normal.y(), normal.z());
            }
            pose.popPose();
        }

        /** 该体素在 cube 局部（相对 cube 最小角）像素下的范围 */
        private float localMin(int axis) {
            return switch (axis) {
                case 0 -> this.minX - this.sourceCube.minX;
                case 1 -> this.minY - this.sourceCube.minY;
                default -> this.minZ - this.sourceCube.minZ;
            };
        }

        private float localMax(int axis) {
            return switch (axis) {
                case 0 -> this.maxX - this.sourceCube.minX;
                case 1 -> this.maxY - this.sourceCube.minY;
                default -> this.maxZ - this.sourceCube.minZ;
            };
        }

        private static float vertexCoord(ModelPart.Vertex vertex, int axis) {
            return switch (axis) {
                case 0 -> vertex.x();
                case 1 -> vertex.y();
                default -> vertex.z();
            };
        }

        private static void setCoord(Vector3f vector, int axis, float value) {
            switch (axis) {
                case 0 -> vector.x = value;
                case 1 -> vector.y = value;
                default -> vector.z = value;
            }
        }

        private static float bilinear(float[] corners, float s, float t) {
            return corners[0] * (1 - s) * (1 - t)
                    + corners[1] * s * (1 - t)
                    + corners[2] * (1 - s) * t
                    + corners[3] * s * t;
        }
    }
}
