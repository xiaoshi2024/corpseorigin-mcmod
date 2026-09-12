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

public class BloodLotusLaserManager {

    private static BloodLotusLaserManager instance;

    private final List<ActiveChain> chains = new ArrayList<>();
    private final List<ActiveAura> auras = new ArrayList<>();

    private BloodLotusLaserManager() {
    }

    public static BloodLotusLaserManager getInstance() {
        if (instance == null) {
            instance = new BloodLotusLaserManager();
        }
        return instance;
    }

    // ==================== 添加 ====================

    public void addChains(Vec3 start, List<UUID> targetUuids, int durationTicks) {
        for (UUID uuid : targetUuids) {
            Vec3 offsetStart = start.add(
                    (Math.random() - 0.5) * 0.2,
                    (Math.random() - 0.5) * 0.2,
                    (Math.random() - 0.5) * 0.2
            );
            chains.add(new ActiveChain(offsetStart, uuid, durationTicks));
        }
    }

    public void addAura(UUID playerUuid, int durationTicks) {
        auras.add(new ActiveAura(playerUuid, durationTicks));
    }

    // ==================== tick ====================

    public void tick() {
        Iterator<ActiveChain> chainIt = chains.iterator();
        while (chainIt.hasNext()) {
            ActiveChain chain = chainIt.next();
            chain.lifetime--;
            chain.seed = System.nanoTime();
            if (chain.lifetime <= 0) {
                chainIt.remove();
            }
        }

        Iterator<ActiveAura> auraIt = auras.iterator();
        while (auraIt.hasNext()) {
            ActiveAura aura = auraIt.next();
            aura.lifetime--;
            aura.seed = System.nanoTime();
            if (aura.lifetime <= 0) {
                auraIt.remove();
            }
        }
    }

    // ==================== render ====================

    public void render(PoseStack poseStack, SubmitNodeCollector collector) {
        if (chains.isEmpty() && auras.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 cameraPos = mc.gameRenderer.mainCamera().position();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // ✅ 链条渲染
        for (ActiveChain chain : chains) {
            Entity target = mc.level.getEntity(chain.targetUuid);
            if (!(target instanceof LivingEntity living)) continue;

            Vec3 targetPos = living.position().add(0, living.getBbHeight() * 0.5, 0);

            float progress = 1.0F - (float) chain.lifetime / chain.maxLifetime;
            float alpha = Math.max(0.1F, 1.0F - progress);

            BloodLotusLaserRenderer.submitLaser(
                    poseStack, collector,
                    chain.start, targetPos,
                    0.13F, alpha, chain.seed
            );

            BloodLotusLaserRenderer.submitCoil(
                    poseStack, collector,
                    living.position(),
                    living.getBbWidth() * 0.6 + 0.2,
                    alpha, chain.seed
            );
        }

        // ✅ 自身环绕渲染
        for (ActiveAura aura : auras) {
            Entity entity = mc.level.getEntity(aura.playerUuid);
            if (!(entity instanceof LivingEntity living)) continue;

            float progress = 1.0F - (float) aura.lifetime / aura.maxLifetime;
            float alpha = Math.max(0.1F, 1.0F - progress);

            // ✅ 用身体中心作为环绕中心
            Vec3 center = living.position().add(0, living.getBbHeight() * 0.5, 0);
            double radius = living.getBbWidth() * 0.8 + 0.4;

            BloodLotusLaserRenderer.submitCoil(
                    poseStack, collector,
                    center.add(0, -0.3, 0), radius, alpha, aura.seed
            );
            BloodLotusLaserRenderer.submitCoil(
                    poseStack, collector,
                    center.add(0, 0.2, 0), radius * 0.9, alpha * 0.9F, aura.seed + 1
            );
            BloodLotusLaserRenderer.submitCoil(
                    poseStack, collector,
                    center.add(0, 0.7, 0), radius * 0.7, alpha * 0.7F, aura.seed + 2
            );
        }

        poseStack.popPose();
    }

    public void clear() {
        chains.clear();
        auras.clear();
    }

    // ==================== 内部类 ====================

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

    private static class ActiveAura {
        final UUID playerUuid;
        int lifetime;
        final int maxLifetime;
        long seed;

        ActiveAura(UUID playerUuid, int lifetime) {
            this.playerUuid = playerUuid;
            this.lifetime = lifetime;
            this.maxLifetime = lifetime;
            this.seed = System.nanoTime();
        }
    }
}