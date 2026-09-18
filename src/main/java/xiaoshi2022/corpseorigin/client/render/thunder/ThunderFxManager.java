package xiaoshi2022.corpseorigin.client.render.thunder;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.client.render.laser.BloodLotusLaserRenderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 尸王雷电特效（客户端）。
 * <p>
 * 和血莲宝灯那套激光一样是"纯表现"：服务端只发起点终点，客户端自己画。
 * 几何直接复用 {@link BloodLotusLaserRenderer#submitThunderBolt}（同一套赫兹波形 + 双层发光，
 * 只是换成紫白配色），所以落雷和血链看起来是一家人。
 * <p>
 * 每 tick 换一个 seed，于是弧线会持续抖动 —— 就是闪电那种"噼啪跳"的感觉。
 */
public class ThunderFxManager {

    private static ThunderFxManager instance;

    private final List<Bolt> bolts = new ArrayList<>();

    private ThunderFxManager() {
    }

    public static ThunderFxManager getInstance() {
        if (instance == null) {
            instance = new ThunderFxManager();
        }
        return instance;
    }

    /** 加一道雷电 */
    public void addBolt(Vec3 from, Vec3 to, int durationTicks, float width) {
        bolts.add(new Bolt(from, to, durationTicks, width));
    }

    public void tick() {
        Iterator<Bolt> iterator = bolts.iterator();
        while (iterator.hasNext()) {
            Bolt bolt = iterator.next();
            bolt.lifetime--;
            bolt.seed = System.nanoTime();
            if (bolt.lifetime <= 0) {
                iterator.remove();
            }
        }
    }

    public void render(PoseStack poseStack, SubmitNodeCollector collector) {
        if (bolts.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Vec3 cameraPos = mc.gameRenderer.mainCamera().position();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (Bolt bolt : bolts) {
            float progress = 1.0F - (float) bolt.lifetime / bolt.maxLifetime;
            float alpha = Math.max(0.1F, 1.0F - progress);

            BloodLotusLaserRenderer.submitThunderBolt(
                    poseStack, collector,
                    bolt.from, bolt.to,
                    bolt.width, alpha, bolt.seed
            );
        }

        poseStack.popPose();
    }

    public void clear() {
        bolts.clear();
    }

    private static class Bolt {
        final Vec3 from;
        final Vec3 to;
        final float width;
        int lifetime;
        final int maxLifetime;
        long seed;

        Bolt(Vec3 from, Vec3 to, int lifetime, float width) {
            this.from = from;
            this.to = to;
            this.lifetime = lifetime;
            this.maxLifetime = lifetime;
            this.width = width;
            this.seed = System.nanoTime();
        }
    }
}
