package xiaoshi2022.corpseorigin.client.soul;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.client.camera.PersistentCameraEntity;

/**
 * 蓝海「灵魂出窍」的客户端表现。
 * <p>
 * 服务端只负责状态（躯壳钉死 + 躺姿 + 到期/次声波回收），这里负责：
 * 出窍时把视角交给 {@link PersistentCameraEntity}、灵魂残影的位置/朝向/淡出，
 * 以及一路上的幽蓝灵魂粒子。
 */
@Environment(EnvType.CLIENT)
public final class SoulProjectionClient {

    /** 过场上升阶段灵魂残影的透明度（用户要求约 0.4） */
    public static final float GHOST_ALPHA = 0.42F;

    /** 服务端说灵魂正在出窍 */
    private static boolean active;
    /** 灵魂残影是否还在画（回窍后淡出用） */
    private static float ghostAlpha;
    private static Vec3 ghostPos;
    private static float ghostBodyRot;
    private static float ghostPitch;

    /** 出窍前的 noGravity，回窍后还原（翅膀飞行等会把玩家的这个值置 true） */
    private static boolean previousNoGravity;
    private static int trailTimer;

    private SoulProjectionClient() {
    }

    public static boolean isActive() {
        return active;
    }

    public static float ghostAlpha() {
        return ghostAlpha;
    }

    public static Vec3 ghostPosition() {
        return ghostPos;
    }

    public static float ghostBodyRot() {
        return ghostBodyRot;
    }

    public static float ghostPitch() {
        return ghostPitch;
    }

    /** 服务端同步：true = 开始出窍，false = 已回窍 */
    public static void accept(boolean activeNow) {
        Minecraft client = Minecraft.getInstance();
        if (activeNow) {
            if (active) {
                return;
            }
            active = true;
            trailTimer = 0;
            LocalPlayer player = client.player;
            if (player != null) {
                previousNoGravity = player.isNoGravity();
                player.setNoGravity(true);
                player.setDeltaMovement(Vec3.ZERO);
                burstAt(player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 22);
            }
            PersistentCameraEntity.startSoulProjection(client);
            return;
        }

        if (!active) {
            // 客户端还没开始过：也要保证相机被放掉（比如状态包丢了重连）
            PersistentCameraEntity.endSoulProjection(client);
            return;
        }
        active = false;
        PersistentCameraEntity.endSoulProjection(client);
        // 回窍时镜头会一路钻回胸口，残影原地留着就会糊住玩家自己的视角：直接收掉
        ghostAlpha = 0.0F;
        ghostPos = null;

        LocalPlayer player = client.player;
        if (player != null) {
            player.setNoGravity(previousNoGravity);
            player.setDeltaMovement(Vec3.ZERO);
            burstAt(player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 16);
        }
    }

    /** 每客户端 tick：跟随灵魂更新残影位置，并撒灵魂粒子 */
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        if (active) {
            if (client.getCameraEntity() instanceof PersistentCameraEntity camera) {
                ghostPos = camera.position();
                ghostBodyRot = camera.getYRot();
                ghostPitch = camera.getXRot();
                ghostAlpha = Math.min(GHOST_ALPHA, ghostAlpha + 0.06F);
                if (++trailTimer >= 2) {
                    trailTimer = 0;
                    trail(camera);
                }
                return;
            }
            // 相机被别的过场夺走了：直接收摊，别留一具飘在原地的残影
            active = false;
            ghostAlpha = 0.0F;
            ghostPos = null;
            if (client.player != null) {
                client.player.setNoGravity(previousNoGravity);
            }
        }
    }

    private static void trail(PersistentCameraEntity camera) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        RandomSource random = client.level.getRandom();
        double x = camera.getX() + (random.nextDouble() - 0.5) * 0.6;
        double y = camera.getY() + 0.6 + (random.nextDouble() - 0.5) * 0.8;
        double z = camera.getZ() + (random.nextDouble() - 0.5) * 0.6;
        client.level.addParticle(ParticleTypes.SOUL, x, y, z, 0.0, 0.01, 0.0);
    }

    private static void burstAt(double x, double y, double z, int count) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        RandomSource random = client.level.getRandom();
        for (int i = 0; i < count; i++) {
            double dx = (random.nextDouble() - 0.5) * 0.55;
            double dy = (random.nextDouble() - 0.5) * 0.7;
            double dz = (random.nextDouble() - 0.5) * 0.55;
            client.level.addParticle(ParticleTypes.SOUL, x + dx, y + dy, z + dz,
                    dx * 0.05, dy * 0.05, dz * 0.05);
        }
    }
}
