package xiaoshi2022.corpseorigin.skill.shichaozhizi;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「千眼万目」的凝视场（服务端每 tick）。
 * <p>
 * 做两件事：
 * <ol>
 *   <li>维护"谁正在放千眼万目"的窗口（{@link ThousandEyesSkill#DURATION} tick，与 {@code special}
 *       动画等长）；</li>
 *   <li>每 tick 扫一遍施术者周围：<b>视线落在"望向施术者"的锥内、且中间没有方块挡着</b>的生物 / 玩家，
 *       续一次定身（见 {@link #stun}）。转头、躲到墙后、或者离开半径，约半秒后定身自然解除。</li>
 * </ol>
 * ⚠️ 定身用的是<b>缓慢 6 级</b>（移动速度 -105%，属性下限是 0）：走不动、也迈不开步，
 * 但仍然是原版效果，玩家客户端自己就会同步生效 —— 不需要额外写客户端"冻结输入"的代码。
 * 代价是生物还站得住、玩家还能原地起跳，这对"定住"来说够用。
 */
public final class ThousandEyesHandler {

    /** 施术者 uuid → 凝视窗口的结束时刻（存世界的 gameTime，别用 tickCount，重生会归零） */
    private static final Map<UUID, Long> GAZING_UNTIL = new ConcurrentHashMap<>();

    private ThousandEyesHandler() {
    }

    /** 技能发动：开一个覆盖整段 {@code special} 动画的凝视窗口 */
    public static void start(ServerPlayer caster) {
        GAZING_UNTIL.put(caster.getUUID(),
                caster.level().getGameTime() + ThousandEyesSkill.DURATION);
    }

    public static void tick(MinecraftServer server) {
        if (GAZING_UNTIL.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Long>> iterator = GAZING_UNTIL.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
            // 下线 / 阵亡 / 窗口结束 → 收工（effect 自己会过期，不用在这里清）
            if (caster == null || !caster.isAlive()
                    || caster.level().getGameTime() >= entry.getValue()) {
                iterator.remove();
                continue;
            }
            stareAt(caster);
        }
    }

    /** 把这一 tick 正看着施术者的生物 / 玩家定住 */
    private static void stareAt(ServerPlayer caster) {
        Vec3 eyes = caster.getEyePosition();
        // 以"眼睛"为球心的立方搜索区（直接拿身体盒子 inflate 会连脚底下方一大片也算进来）
        AABB area = new AABB(eyes, eyes).inflate(ThousandEyesSkill.RANGE);
        for (Entity target : caster.level().getEntities(caster, area)) {
            if (!(target instanceof LivingEntity watcher) || watcher == caster) {
                continue;
            }
            // 旁观者本来就能穿墙飞、定住没意义；创造模式不定，省得自己测试时被自己卡住
            if (watcher instanceof Player player
                    && (player.isSpectator() || player.isCreative())) {
                continue;
            }
            if (isGazingAt(watcher, caster, eyes)) {
                stun(watcher);
            }
        }
    }

    /** 对方是不是正"看着"施术者：视线落在锥内，且没有方块挡在中间 */
    private static boolean isGazingAt(LivingEntity watcher, ServerPlayer caster, Vec3 casterEyes) {
        Vec3 toCaster = casterEyes.subtract(watcher.getEyePosition());
        // 贴脸时方向没有意义（normalize 会退化成零向量），直接放过
        if (toCaster.lengthSqr() < 0.25) {
            return true;
        }
        if (watcher.getViewVector(1.0F).dot(toCaster.normalize()) < ThousandEyesSkill.GAZE_DOT) {
            return false;
        }
        // 隔着墙"看"不算：射线被地形挡住时不能定住人
        return watcher.hasLineOfSight(caster);
    }

    /** 续一次定身：缓慢 6 级（-105% 移速，走不动），多给几 tick 让窗口内接得上 */
    private static void stun(LivingEntity victim) {
        victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                ThousandEyesSkill.STUN_REFRESH + 4, 6, false, true, true));
        // 生物是服务端说了算的：顺手清掉残余动量，免得它带着发动前的速度滑出去
        if (!(victim instanceof Player)) {
            Vec3 velocity = victim.getDeltaMovement();
            victim.setDeltaMovement(0.0, velocity.y, 0.0);
        }
    }
}
