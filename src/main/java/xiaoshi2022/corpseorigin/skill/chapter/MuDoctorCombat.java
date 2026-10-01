package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.entity.MuDoctorEntity;
import xiaoshi2022.corpseorigin.entity.MuNeedleEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 穆博士 BOSS 行为状态机 —— 服务端权威。
 * <p>
 * 按 {@link EldorKingZbrCombat} 的模式：每个 {@link MuDoctorEntity#tick()} 在服务端自己调
 * {@link #tickBoss}，零全局扫描、无 spatial hash 开销。
 * <p>
 * <b>一阶段（风筝）</b>：
 * <ul>
 *   <li>距离 &lt;8 格：立刻后撤（速度提升），并补投一枚针；</li>
 *   <li>距离 8~20 格：缓慢横向移动，持续投针；</li>
 *   <li>距离 &gt;20 格：主动靠近到 15 格左右，再开始投针。</li>
 * </ul>
 * 三针（毒 / 虚弱 / 迟缓）随机轮换，带隐藏优先：目标已被减速优先补毒针；目标血量 &lt;50% 优先补虚弱针。
 * <p>
 * <b>二阶段（金属）</b>：不再投针，缓慢逼近到 4 格内抛投玩家（像铁傀儡把人扔出去）；
 * 抛投后金属软化 3 秒（唯一输出窗口）→ 恢复 → 继续追。阶段切换（扎针）由实体自己处理，本类不参与。
 */
public final class MuDoctorCombat {

    // ==================== 状态 ====================

    private enum Mode { IDLE, NEEDLE, GRAB, RECOVER }

    private static final class State {
        Mode mode = Mode.IDLE;
        int ticks;
        int needleCooldown;
        int grabCooldown;
        int softTicks;       // 二阶段金属软化剩余 tick
        int strafeTimer;
        int strafeDir = 1;
        byte needleKind;
    }

    private static final Map<UUID, State> DOCTORS = new HashMap<>();

    // ==================== 时序常量 ====================

    /** 投针动作：抬手帧出手 */
    private static final int NEEDLE_WINDUP = 10;
    /** 投针动作总时长 */
    private static final int NEEDLE_DURATION = 18;
    /** 投针冷却（约 1.4 秒一发） */
    private static final int NEEDLE_COOLDOWN = 28;
    /** 出针弹速（格 / tick） */
    private static final double NEEDLE_SPEED = 1.25D;
    /** 投针最大射程（平方） */
    private static final double NEEDLE_MAX_DIST_SQR = 34.0D * 34.0D;

    /** 抓取抛投：命中帧（把玩家扔出去） */
    private static final int GRAB_IMPACT = 12;
    /** 抓取动作总时长 */
    private static final int GRAB_DURATION = 24;
    /** 抓取触发距离 */
    private static final double GRAB_RANGE = 4.0D;
    /** 抓取冷却（软化结束后再等这么久才允许下一次抓取） */
    private static final int GRAB_COOLDOWN_AFTER = 30;
    /** 金属软化时长（3 秒，唯一输出窗口） */
    private static final int SOFT_TICKS = 60;

    /** 动作结束后的恢复期 */
    private static final int RECOVER_TICKS = 6;
    /** 横向漂移换向间隔 */
    private static final int STRAFE_INTERVAL = 40;

    /** 抛投伤害 */
    private static final float GRAB_DAMAGE = 12.0F;

    // ==================== 注册 ====================

    /** 空实现（不做全局扫描），只在服务器停止时清缓存 —— 见 {@link EldorKingZbrCombat#register()}。 */
    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DOCTORS.clear());
    }

    public static void forget(UUID id) {
        DOCTORS.remove(id);
    }

    // ==================== 主循环 ====================

    public static void tickBoss(ServerLevel level, MuDoctorEntity boss) {
        State st = DOCTORS.computeIfAbsent(boss.getUUID(), k -> new State());

        if (st.needleCooldown > 0) st.needleCooldown--;
        if (st.grabCooldown > 0) st.grabCooldown--;
        if (st.softTicks > 0 && --st.softTicks <= 0) {
            boss.setMetalSoft(false);
        }

        LivingEntity target = boss.getTarget();
        if (boss.getPhase() == MuDoctorEntity.PHASE_METAL) {
            tickMetal(level, boss, target, st);
        } else {
            tickKite(level, boss, target, st);
        }
    }

    // ==================== 一阶段：风筝 + 投针 ====================

    private static void tickKite(ServerLevel level, MuDoctorEntity boss, LivingEntity target, State st) {
        if (st.mode == Mode.NEEDLE) {
            st.ticks++;
            boss.incrementActionTicks();
            if (st.ticks == NEEDLE_WINDUP) {
                releaseNeedle(level, boss, target, st.needleKind);
            }
            if (st.ticks >= NEEDLE_DURATION) {
                finishAction(boss, st, NEEDLE_COOLDOWN);
            }
            return;
        }
        if (st.mode == Mode.RECOVER) {
            if (++st.ticks >= RECOVER_TICKS) {
                st.mode = Mode.IDLE;
            }
            return;
        }

        // IDLE
        if (target == null || !target.isAlive()) {
            boss.setAction(MuDoctorEntity.ACTION_IDLE);
            return;
        }

        double distSqr = boss.distanceToSqr(target);
        double dist = Math.sqrt(distSqr);

        // 保持距离（风筝 AI）
        if (dist < 8.0D) {
            // 立刻后撤（速度临时提升）
            Vec3 away = boss.position().subtract(target.position());
            Vec3 flat = new Vec3(away.x, 0, away.z);
            if (flat.lengthSqr() < 0.01D) flat = new Vec3(1, 0, 0);
            Vec3 dest = boss.position().add(flat.normalize().scale(6.0D));
            boss.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.4D);
        } else if (dist > 20.0D) {
            // 主动靠近到 15 格左右（对齐后由 8~20 分支接手）
            boss.getNavigation().moveTo(target, 1.2D);
        } else {
            // 缓慢横向移动
            if (--st.strafeTimer <= 0) {
                st.strafeTimer = STRAFE_INTERVAL;
                st.strafeDir = boss.getRandom().nextBoolean() ? 1 : -1;
            }
            Vec3 toTarget = target.position().subtract(boss.position());
            Vec3 perp = new Vec3(-toTarget.z, 0, toTarget.x);
            if (perp.lengthSqr() < 0.01D) perp = new Vec3(1, 0, 0);
            Vec3 dest = boss.position().add(perp.normalize().scale(4.0D * st.strafeDir));
            boss.getNavigation().moveTo(dest.x, dest.y, dest.z, 0.7D);
        }

        // 投针：冷却好了 + 看得见 + 在射程内
        if (st.needleCooldown <= 0 && distSqr <= NEEDLE_MAX_DIST_SQR && boss.hasLineOfSight(target)) {
            st.mode = Mode.NEEDLE;
            st.ticks = 0;
            st.needleKind = chooseNeedleKind(target);
            boss.setAction(MuDoctorEntity.ACTION_NEEDLE);
        }
    }

    /** 隐藏优先规则：被减速 → 毒针；血量 &lt;50% → 虚弱针；否则三针随机 */
    private static byte chooseNeedleKind(LivingEntity target) {
        if (target.hasEffect(MobEffects.SLOWNESS)) {
            return MuNeedleEntity.KIND_POISON;
        }
        if (target.getHealth() < target.getMaxHealth() * 0.5F) {
            return MuNeedleEntity.KIND_WEAKNESS;
        }
        return switch (target.getRandom().nextInt(3)) {
            case 0 -> MuNeedleEntity.KIND_POISON;
            case 1 -> MuNeedleEntity.KIND_WEAKNESS;
            default -> MuNeedleEntity.KIND_SLOWNESS;
        };
    }

    private static void releaseNeedle(ServerLevel level, MuDoctorEntity boss, LivingEntity target, byte kind) {
        if (target == null || !target.isAlive()) return;
        Vec3 aim = target.getEyePosition().add(target.getDeltaMovement().scale(6.0D));
        Vec3 from = boss.getEyePosition().add(0, -0.1D, 0);
        Vec3 dir = aim.subtract(from);
        if (dir.lengthSqr() < 0.01D) dir = boss.getLookAngle();
        dir = dir.normalize();

        MuNeedleEntity needle = new MuNeedleEntity(level, boss, kind);
        needle.setPos(from.x + dir.x * 0.8D, from.y + dir.y * 0.8D, from.z + dir.z * 0.8D);
        needle.setDeltaMovement(dir.scale(NEEDLE_SPEED));
        level.addFreshEntity(needle);

        boss.feedbackSound(SoundEvents.SNOWBALL_THROW, 1.0F, 1.3F);
        boss.setYRot((float) (Math.toDegrees(Math.atan2(-dir.x, dir.z))));
    }

    // ==================== 二阶段：追击 + 抛投 ====================

    private static void tickMetal(ServerLevel level, MuDoctorEntity boss, LivingEntity target, State st) {
        if (st.mode == Mode.GRAB) {
            st.ticks++;
            boss.incrementActionTicks();
            boss.getNavigation().stop();
            if (st.ticks == GRAB_IMPACT) {
                flingTarget(level, boss, target);
                // 抛完金属短暂软化 3 秒（唯一输出窗口）
                boss.setMetalSoft(true);
                st.softTicks = SOFT_TICKS;
                st.grabCooldown = SOFT_TICKS + GRAB_COOLDOWN_AFTER;
            }
            if (st.ticks >= GRAB_DURATION) {
                st.mode = Mode.IDLE;
                boss.setAction(MuDoctorEntity.ACTION_IDLE);
            }
            return;
        }
        if (st.mode == Mode.RECOVER) {
            if (++st.ticks >= RECOVER_TICKS) {
                st.mode = Mode.IDLE;
            }
            return;
        }

        if (target == null || !target.isAlive()) {
            boss.setAction(MuDoctorEntity.ACTION_IDLE);
            return;
        }

        double dist = Math.sqrt(boss.distanceToSqr(target));
        if (dist > GRAB_RANGE) {
            // 金属太重，走得比一阶段慢
            boss.getNavigation().moveTo(target, 1.0D);
        } else if (st.grabCooldown <= 0) {
            st.mode = Mode.GRAB;
            st.ticks = 0;
            boss.setAction(MuDoctorEntity.ACTION_GRAB);
            boss.getNavigation().stop();
        }
    }

    /** 把玩家像铁傀儡那样扔出去：伤害 + 强击飞 + 落地减速 */
    private static void flingTarget(ServerLevel level, MuDoctorEntity boss, LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        if (boss.distanceToSqr(target) > 6.0D * 6.0D) return; // 早跑开了就抓空

        Vec3 away = target.position().subtract(boss.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        if (flat.lengthSqr() < 0.01D) flat = new Vec3(0, 0, 1);
        flat = flat.normalize();

        target.hurtServer(level, level.damageSources().mobAttack(boss), GRAB_DAMAGE);
        target.push(flat.x * 2.2D, 1.1D, flat.z * 2.2D);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, true, true));

        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY(0.5D), target.getZ(), 16, 0.4D, 0.4D, 0.4D, 0.3D);
        boss.feedbackSound(SoundEvents.IRON_GOLEM_ATTACK, 1.4F, 0.8F);
    }

    // ==================== 工具 ====================

    private static void finishAction(MuDoctorEntity boss, State st, int cooldown) {
        st.mode = Mode.RECOVER;
        st.ticks = 0;
        st.needleCooldown = cooldown;
        boss.setAction(MuDoctorEntity.ACTION_IDLE);
    }
}