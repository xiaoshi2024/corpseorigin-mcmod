package xiaoshi2022.corpseorigin.client.render.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 血莲宝灯雷电链条管理器（Fabric 26.2）
 */
public class BloodLotusLaserManager {

    private static BloodLotusLaserManager instance;

    private final List<ActiveChain> chains = new ArrayList<>();

    private BloodLotusLaserManager() {
    }

    public static BloodLotusLaserManager getInstance() {
        if (instance == null) {
            instance = new BloodLotusLaserManager();
        }
        return instance;
    }

    public void addChain(Vec3 start, UUID targetUuid, int durationTicks) {
        chains.add(new ActiveChain(start, targetUuid, durationTicks));
    }

    public void tick() {
        Iterator<ActiveChain> it = chains.iterator();
        while (it.hasNext()) {
            ActiveChain chain = it.next();
            chain.lifetime--;
            chain.seed = System.nanoTime();  // ✅ 每 tick 换 seed，让雷电抖动
            if (chain.lifetime <= 0) {
                it.remove();
            }
        }
    }

    public void render(PoseStack poseStack, SubmitNodeCollector collector) {
        if (chains.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 cameraPos = mc.gameRenderer.mainCamera().position();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (ActiveChain chain : chains) {
            Entity target = mc.level.getEntity(chain.targetUuid);
            if (!(target instanceof LivingEntity living)) continue;

            // ✅ 目标当前位置（胸口高度）
            Vec3 targetPos = living.position().add(0, living.getBbHeight() * 0.5, 0);

            float progress = 1.0F - (float) chain.lifetime / chain.maxLifetime;
            float alpha = Math.max(0.1F, 1.0F - progress);

            // ✅ 画从宝莲灯到目标的雷电链条
            BloodLotusLaserRenderer.submitLaser(
                    poseStack, collector,
                    chain.start, targetPos,
                    0.13F, alpha, chain.seed
            );

            // ✅ 在目标身上画缠绕环
            BloodLotusLaserRenderer.submitCoil(
                    poseStack, collector,
                    living.position(),
                    living.getBbWidth() * 0.6 + 0.2,
                    alpha, chain.seed
            );
        }

        poseStack.popPose();
    }

    public void clear() {
        chains.clear();
    }

    private static class ActiveChain {
        final Vec3 start;
        final UUID targetUuid;
        int lifetime;
        final int maxLifetime;
        long seed;

        ActiveChain(Vec3 start, UUID targetUuid, int lifetime) {
            this.start = start;
            this.targetUuid = targetUuid;
            this.lifetime = lifetime;
            this.maxLifetime = lifetime;
            this.seed = System.nanoTime();
        }
    }
}