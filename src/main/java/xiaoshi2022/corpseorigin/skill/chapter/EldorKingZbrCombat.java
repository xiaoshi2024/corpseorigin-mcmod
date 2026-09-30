package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.EldorKingZbrEntity;
import xiaoshi2022.corpseorigin.registry.ModEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 尸兄·尔多兽王 BOSS 攻击状态机 —— 服务端权威。
 * <p>
 * 按 Asterion Minotaur 的 {@code tickBossAttack} 模式：每服务端 tick 推进当前实体的 attack tick，
 * 在 hit frame 处结算扇形伤害 / 范围击退 / 粒子音效；攻击结束进入 RECOVER 阶段
 * （恢复期仅 8 tick，二阶段再减半 → 4 tick，"无喘息" Sans 式压迫）。
 * <p>
 * 三招：BITE（{@code attack} 动画 → 獠牙撕咬 + 流血 + 禁食）、SLAM（{@code hammer} 动画 →
 * 震地波 + 不可格挡 + 击退 + 禁足）、IDLE / RECOVER。
 * <p>
 * 客户端表现：本类只调 {@link EldorKingZbrEntity#setAttackState} / {@link EldorKingZbrEntity#incrementAttackTicks}
 * 改同步数据，GeckoLib 的动画控制器 lambda 读同步数据切动画，**不直接调用 tryTriggerAnimation**
 * （那是客户端调用）。
 */
public final class EldorKingZbrCombat {

    // ==================== 状态 ====================

    private enum AttackState { IDLE, BITE, SLAM, THROW, RECOVER }

    private static final class BossState {
        AttackState state = AttackState.IDLE;
        int ticks;
        int recoveryTicks;
        int slamCooldownTicks;
        int throwCooldownTicks;
        int throwBurstLeft;      // 二阶段连投剩余数
        int throwBurstTimer;     // 连发间隔计时
        AttackState lastAttack;   // 上次出招，用于"无喘息连击"权重随机下招
    }

    private static final Map<UUID, BossState> BOSSES = new HashMap<>();

    // ==================== 时序常量 ====================

    /** BITE 命中帧（{@code attack} 动画的释放点） */
    public static final int BITE_HIT_TICK = 12;
    /** SLAM 落地帧（{@code hammer} 动画的砸地点） */
    public static final int SLAM_HIT_TICK = 16;
    /** BITE 总时长（hit frame + 后摇） */
    private static final int BITE_DURATION = 22;
    /** SLAM 总时长（落地 + 扩散波 + 后摇） */
    private static final int SLAM_DURATION = 28;
    /** 震地波扩散的 tick 数（落地后每 tick 半径 +1） */
    private static final int SLAM_RING_TICKS = 6;
    /** IDLE → 攻击结束后的恢复期（Sans 式"无喘息"，远短于原版 40 tick） */
    private static final int RECOVER_TICKS = 8;
    /** 二阶段恢复期（再减半） */
    private static final int RECOVER_TICKS_BERSERK = 4;
    /** SLAM 冷却（避免连续两下震地波） */
    private static final int SLAM_COOLDOWN_TICKS = 60;
    /** 二阶段 SLAM 冷却 */
    private static final int SLAM_COOLDOWN_TICKS_BERSERK = 30;
    /** BITE 命中范围（格，扇形 arc 半径） */
    private static final double BITE_RANGE = 2.5D;
    /** SLAM 命中范围（格，扩散波最大半径） */
    private static final double SLAM_MAX_RADIUS = 6.0D;
    /** SLAM 范围判定（开始追击距离，BOSS 与目标距离 < 此值时改出 BITE） */
    private static final double MELEE_DIST_SQR = 2.5D * 2.5D;
    /** SLAM 触发距离上限（超过则不打 SLAM，等目标靠近） */
    private static final double SLAM_MAX_DIST_SQR = 12.0D * 12.0D;
    /** THROW 抬手帧（复用 {@code attack} 动画的抬臂点，投掷物此刻出手） */
    private static final int THROW_WINDUP_TICK = 12;
    /** THROW 总时长 */
    private static final int THROW_DURATION = 30;
    /** 二阶段连投间隔 tick */
    private static final int THROW_BURST_INTERVAL = 8;
    /** THROW 冷却（防止无限弹幕） */
    private static final int THROW_COOLDOWN_TICKS = 50;
    /** 二阶段 THROW 冷却 */
    private static final int THROW_COOLDOWN_TICKS_BERSERK = 25;
    /** THROW 最大攻击距离（远超 SLAM 的 12 格，专治拉远了躲） */
    private static final double THROW_MAX_DIST_SQR = 30.0D * 30.0D;
    /** 目标比 BOSS 眼睛高出这么多格 = 判定"飞天赖皮"，无视距离优先投石 */
    private static final double THROW_HEIGHT_TRIGGER = 3.0D;
    /** 投掷物初速（格 / tick，1.2 = 24 格/秒匀速直线，30 格外约 25 tick 命中） */
    private static final double THROW_LAUNCH_SPEED = 1.2D;
    /** 飞行弹道预测提前量（tick）：按目标当前速度往前算 */
    private static final double THROW_LEAD_TICKS = 10.0D;
    /** 二阶段速度 / 攻击 multiplier（参考 Asterion phase transition） */
    private static final double BITE_DAMAGE = 14.0D;
    private static final double SLAM_DAMAGE = 22.0D;
    private static final double BITE_KNOCKBACK = 1.5D;
    private static final double SLAM_KNOCKBACK = 1.1D;
    /** 流血持续 tick（8 秒） */
    private static final int BLEED_DURATION = 160;
    /** 禁足持续 tick（3 秒） */
    private static final int ROOT_DURATION = 60;
    /** 禁足强度：SLOWNESS amp 6 → 水平移动乘数 0 */
    private static final int ROOT_AMPLIFIER = 6;

    // ==================== 不可格挡伤害源 ====================

    public static final ResourceKey<net.minecraft.world.damagesource.DamageType> ELDOR_SLAM_KEY =
            ResourceKey.create(Registries.DAMAGE_TYPE, CorpseOrigin.id("eldor_slam"));

    // ==================== 注册 ====================

    /**
     * 空实现 —— 本类不做全局扫描（那是 Asterion Minotaur 的坏习惯，695 tick behind 就是它造成的）。
     * 取而代之：每个 {@link EldorKingZbrEntity#tick()} 在服务端自己调 {@link #tickBoss}，
     * 零开销、无需 HashMap 登记、无需 spatial hash 遍历。
     */
    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> BOSSES.clear());
    }

    /**
     * 由 {@link EldorKingZbrEntity#tick()} 服务端侧直接调用 —— 每个 BOSS 自己推进自己的状态机，
     * 彻底规避 {@code getEntitiesOfClass(Class, 超大AABB)} 触发的 spatial hash 全表扫描。
     */
    public static void tickBoss(ServerLevel level, EldorKingZbrEntity boss) {
        BossState st = BOSSES.computeIfAbsent(boss.getUUID(), k -> new BossState());
        LivingEntity target = boss.getTarget();

        // SLAM / THROW 冷却
        if (st.slamCooldownTicks > 0) st.slamCooldownTicks--;
        if (st.throwCooldownTicks > 0) st.throwCooldownTicks--;

        if (st.state == AttackState.IDLE) {
            // 没目标 → 啥也不干（让 BossApproachGoal 处理找路）
            if (target == null || !target.isAlive()) return;
            double distSqr = boss.distanceToSqr(target);
            // 高空赖皮检测：目标比 BOSS 眼睛高出 3 格（鞘翅 / 缓降 / 方块搭高），
            // 近战和震地波全够不着 —— 无视距离冷却直接投石招呼
            double heightAboveBoss = target.getEyeY() - boss.getEyeY();
            boolean airborneCheese = heightAboveBoss > THROW_HEIGHT_TRIGGER;
            // 50/30/20 权重下招（近身优先 BITE，远距优先 SLAM，二阶段 SLAM 冷却减半）
            if (distSqr <= MELEE_DIST_SQR && !airborneCheese) {
                startAttack(boss, st, AttackState.BITE);
            } else if (!airborneCheese && distSqr <= SLAM_MAX_DIST_SQR && st.slamCooldownTicks <= 0
                    && boss.getRandom().nextFloat() < (boss.isBerserk() ? 0.6F : 0.45F)) {
                startAttack(boss, st, AttackState.SLAM);
            } else if (st.throwCooldownTicks <= 0 && distSqr <= THROW_MAX_DIST_SQR
                    && boss.hasLineOfSight(target)
                    && (airborneCheese || boss.getRandom().nextFloat() < (boss.isBerserk() ? 0.5F : 0.3F))) {
                // 高空必投；中远距按概率补一发（二阶段更频繁）
                startAttack(boss, st, AttackState.THROW);
            }
            // 目标在 THROW_MAX_DIST_SQR 之外：让 AI goal 先接近，等下一 tick 再判断
            return;
        }

        if (st.state == AttackState.RECOVER) {
            st.recoveryTicks++;
            int recoverNeeded = boss.isBerserk() ? RECOVER_TICKS_BERSERK : RECOVER_TICKS;
            if (st.recoveryTicks >= recoverNeeded) {
                st.state = AttackState.IDLE;
                st.recoveryTicks = 0;
                boss.setAttackState(0);   // 同步给客户端：attack 控制器 STOP，让 movement 接管
            }
            return;
        }

        // BITE / SLAM 进行中
        st.ticks++;
        boss.incrementAttackTicks();   // 同步给客户端用于命中表现

        if (st.state == AttackState.BITE) {
            if (st.ticks == BITE_HIT_TICK) {
                doBiteHit(level, boss, target);
            }
            if (st.ticks >= BITE_DURATION) {
                finishAttack(boss, st);
            }
        } else if (st.state == AttackState.SLAM) {
            // 落地帧 + 随后 SLAM_RING_TICKS 内扩散
            int ringTick = st.ticks - SLAM_HIT_TICK;
            if (st.ticks == SLAM_HIT_TICK) {
                doSlamLand(level, boss);
            } else if (ringTick > 0 && ringTick <= SLAM_RING_TICKS) {
                doSlamRingExpand(level, boss, ringTick);
            }
            if (st.ticks >= SLAM_DURATION) {
                finishAttack(boss, st);
            }
        } else if (st.state == AttackState.THROW) {
            // 抬手帧出手第一发；二阶段每 THROW_BURST_INTERVAL tick 追投，共 3 连发
            if (st.ticks == THROW_WINDUP_TICK) {
                doThrowRelease(level, boss, target);
                st.throwBurstLeft = boss.isBerserk() ? 2 : 0;
                st.throwBurstTimer = 0;
            } else if (st.throwBurstLeft > 0 && st.ticks > THROW_WINDUP_TICK) {
                st.throwBurstTimer++;
                if (st.throwBurstTimer >= THROW_BURST_INTERVAL) {
                    st.throwBurstTimer = 0;
                    st.throwBurstLeft--;
                    doThrowRelease(level, boss, target);
                }
            }
            if (st.ticks >= THROW_DURATION) {
                finishAttack(boss, st);
            }
        }
    }

    private static void startAttack(EldorKingZbrEntity boss, BossState st, AttackState attack) {
        st.state = attack;
        st.ticks = 0;
        st.lastAttack = attack;
        if (attack == AttackState.SLAM) {
            st.slamCooldownTicks = boss.isBerserk() ? SLAM_COOLDOWN_TICKS_BERSERK : SLAM_COOLDOWN_TICKS;
        }
        if (attack == AttackState.THROW) {
            st.throwCooldownTicks = boss.isBerserk() ? THROW_COOLDOWN_TICKS_BERSERK : THROW_COOLDOWN_TICKS;
        }
        // 同步给客户端：attack 控制器读 DATA_ATTACK_STATE 切动画。
        // THROW 复用 bite 槽位（1）→ 客户端播 attack 抬臂动画，正好是投掷姿势，无需新动画。
        boss.setAttackState(attack == AttackState.SLAM ? 2 : 1);
    }

    private static void finishAttack(EldorKingZbrEntity boss, BossState st) {
        st.state = AttackState.RECOVER;
        st.recoveryTicks = 0;
        // 攻击态置回 IDLE 让客户端 attack 控制器 STOP（再由 RECOVER 倒计时进 IDLE）
        boss.setAttackState(0);
    }

    // ==================== BITE：獠牙撕咬 ====================

    private static void doBiteHit(ServerLevel level, EldorKingZbrEntity boss, LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        Vec3 face = boss.getLookAngle();
        // 扇形近战：2.5 格内、35° 半角内、14 伤 + 1.5 击退（ChapterCombat.arc 内部已发 hurtServer）
        int hit = ChapterCombat.arc(boss, face, BITE_RANGE, 35D,
                (float) BITE_DAMAGE, BITE_KNOCKBACK);
        // 命中的目标加流血（额外遍历一次取实际命中的玩家；arc 返回 hit 数量，
        // 但流血效果要直接挂人 —— 简化用视线上的目标 fallback）
        if (hit > 0 && target.hasLineOfSight(boss)) {
            target.addEffect(new MobEffectInstance(ModEffects.BLEED, BLEED_DURATION, 0, false, true, true));
        }
        // 撕咬粒子 / 音效（仿 Asterion 命中表现）
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                target.getX(), target.getY(0.5D), target.getZ(), 8, 0.4D, 0.3D, 0.4D, 0.2D);
        boss.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 0.8F);
    }

    // ==================== THROW：投石（防飞天赖皮） ====================

    /**
     * 抬手帧出手：从 BOSS 眼前掷出一团「燃烧尸石」（原版恶魂小火球 ——
     * 命中 5 伤 + 着火 5 秒、不炸地形），弹道带目标速度预判，
     * 专治飞天 / 缓降 / 拉远距离赖皮的玩家。
     */
    private static void doThrowRelease(ServerLevel level, EldorKingZbrEntity boss, LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        // 弹道预判：目标当前位置 + 未来 THROW_LEAD_TICKS tick 的位移
        Vec3 aimPoint = target.getEyePosition()
                .add(target.getDeltaMovement().scale(THROW_LEAD_TICKS));
        Vec3 from = boss.getEyePosition().add(boss.getLookAngle().scale(1.2D));
        Vec3 dir = aimPoint.subtract(from);
        if (dir.lengthSqr() < 0.01D) dir = new Vec3(0, 1, 0);
        dir = dir.normalize();

        // 26.2 映射：SmallFireball 挪到了 projectile.hurtingprojectile 子包。
        // Vec3 构造参数语义是"每 tick 持续加速度"—— 传 ZERO，改用手动初速做匀速直线弹道，可控。
        net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball rock =
                new net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball(level, boss, Vec3.ZERO);
        rock.setPos(from.x, from.y, from.z);
        rock.setDeltaMovement(dir.scale(THROW_LAUNCH_SPEED));
        level.addFreshEntity(rock);

        // 出手表现：手部烟尘 + 投掷音效
        level.sendParticles(ParticleTypes.FLAME,
                from.x, from.y, from.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
        boss.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.7F);
    }

    // ==================== SLAM：震地波 ====================

    private static void doSlamLand(ServerLevel level, EldorKingZbrEntity boss) {
        // 落地：起波粒子 + 大爆炸粒子 + 落地音效（仿 Asterion tickRagdollStomp 落地段）
        ChapterCombat.ring(level, boss.position(), 1.0D, 0xAA0000, 16);
        level.sendParticles(ParticleTypes.EXPLOSION,
                boss.getX(), boss.getY(0.1D), boss.getZ(), 9, 2.2D, 0.22D, 2.2D, 0.05D);
        // GENERIC_EXPLODE 在 26.2 是 Holder<SoundEvent>，需 .value() 取 SoundEvent
        boss.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.6F);
    }

    private static void doSlamRingExpand(ServerLevel level, EldorKingZbrEntity boss, int ringTick) {
        // 扩散波：ringTick=1..6 → 半径 1+ringTick
        double radius = 1.0D + ringTick;
        ChapterCombat.ring(level, boss.position(), radius, 0xAA0000, 12);
        // 命中范围内的所有活物：不可格挡伤害 + 击退 + 禁足（SLOWNESS amp 6）
        net.minecraft.world.phys.AABB box = boss.getBoundingBox().inflate(radius);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box,
                v -> v != boss && v.isAlive() && !v.isRemoved()
                        && boss.distanceToSqr(v) <= radius * radius)) {
            if (victim instanceof Player p && p.isCreative()) continue;
            // 不可格挡 / 不减甲伤害源（DamageType JSON 标 is_bypass_shield + scaling never）
            if (victim.hurtServer(level, level.damageSources().source(ELDOR_SLAM_KEY, boss, boss),
                    (float) SLAM_DAMAGE)) {
                Vec3 away = victim.position().subtract(boss.position());
                Vec3 horizontal = new Vec3(away.x, 0, away.z);
                if (horizontal.lengthSqr() < 0.01D) horizontal = new Vec3(0, 0, 1);
                horizontal = horizontal.normalize();
                victim.push(horizontal.x * SLAM_KNOCKBACK, 0.5D, horizontal.z * SLAM_KNOCKBACK);
                victim.hurtMarked = true;
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                        ROOT_DURATION, ROOT_AMPLIFIER, false, true, true));
            }
        }
    }
}
