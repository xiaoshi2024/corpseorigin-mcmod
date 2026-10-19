package com.xiaoshi2022.corpseorigin.entity.ai;

import com.xiaoshi2022.corpseorigin.entity.ZombieKin;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * 感染后的尸兄 AI（1.20.1 Forge 简化版）。
 * <p>
 * 分两种处理方式：
 * <ul>
 *   <li><b>友好生物</b>（动物 / 村民 / 水生生物 / 蝙蝠等，且不是敌人）：
 *       取消原版 {@code serverAiStep}，整个替换为尸兄猎手 AI；</li>
 *   <li><b>带机制的生物</b>（Boss、苦力怕、末影人、模组生物等 {@link Enemy}）：
 *       保留自己的 Goal / customServerAiStep / 技能，只在没有目标时叠加一层
 *       "锁定附近猎物"的尸兄行为，不抢导航与攻击。</li>
 * </ul>
 * 两种方式都保留宿主身体、导航、攻击动画（不替换实体类型），
 * 境界属性与吞噬进化照常生效。
 */
public final class NativeCorpseAi {
    private static final double RANGE = 24;
    private static final java.util.Map<Mob, State> STATES = new java.util.WeakHashMap<>();
    private static final class State { int scan, attack, path, wander; }
    private NativeCorpseAi() {}

    public static boolean prey(Mob mob, LivingEntity target) {
        return target != null && target != mob && target.isAlive() && !target.isSpectator()
                && (target instanceof Mob || target instanceof Player)
                && !(target instanceof Player player && player.isCreative())
                // 同类默认不互殴；但高阶尸兄会猎食比自己低阶的尸兄（互相吞噬进化）
                && (!ZombieKin.isZombieKin(target) || canDevour(mob, target))
                && !mob.isAlliedTo(target) && mob.distanceToSqr(target) <= RANGE * RANGE;
    }

    /** 高阶吞噬低阶：只有对方是已转化尸兄且进化等级更低时才允许猎食。 */
    private static boolean canDevour(Mob hunter, LivingEntity target) {
        return target instanceof Mob other
                && NativeCorpseState.infected(other)
                && NativeCorpseState.evolutionLevel(other) < NativeCorpseState.evolutionLevel(hunter);
    }

    /**
     * 是否需要整体替换 AI。
     * <p>
     * 只有"没有自身战斗机制"的友好生物才整只换成猎手；
     * 敌人（Boss / 怪物）与模组机制生物保留原 AI，走 {@link #overlay} 叠加。
     */
    public static boolean shouldReplaceAi(Mob mob) {
        if (mob instanceof Enemy) return false;
        return mob instanceof Animal
                || mob instanceof AbstractVillager
                || mob instanceof WaterAnimal
                || mob instanceof AmbientCreature;
    }

    /**
     * 机制生物的"叠加层"：不碰它自己的 Goal / 导航 / 技能，
     * 只在其没有有效目标时，帮忙锁定附近猎物。
     * 这样巫妖王仍会放法术、苦力怕仍会膨胀、模组 Boss 仍会走自己的阶段。
     */
    public static void overlay(Mob mob, ServerLevel level) {
        if (mob.isNoAi() || !mob.isAlive() || mob.isRemoved()) return;

        LivingEntity current = mob.getTarget();
        if (current != null && current.isAlive() && !current.isSpectator()) {
            // 已经有目标：完全交给它自己的 AI / 技能
            return;
        }

        State state = STATES.computeIfAbsent(mob, ignored -> new State());
        if (mob.tickCount < state.scan) return;
        state.scan = mob.tickCount + 20;

        LivingEntity found = level.getEntitiesOfClass(LivingEntity.class,
                        mob.getBoundingBox().inflate(RANGE),
                        entity -> prey(mob, entity) && mob.hasLineOfSight(entity))
                .stream()
                .min(java.util.Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        if (found != null) {
            mob.setTarget(found);
            mob.setAggressive(true);
        }
    }

    private static LivingEntity target(Mob mob, State state, ServerLevel level) {
        LivingEntity current = mob.getTarget();
        if (!prey(mob, current)) { current = null; mob.setTarget(null); }
        if (mob.tickCount >= state.scan) {
            state.scan = mob.tickCount + 20;
            LivingEntity attacker = mob.getLastHurtByMob();
            if (prey(mob, attacker)) current = attacker;
            else if (current == null) current = level.getEntitiesOfClass(LivingEntity.class,
                    mob.getBoundingBox().inflate(RANGE), entity -> prey(mob, entity) && mob.hasLineOfSight(entity))
                    .stream().min(java.util.Comparator.comparingDouble(entity -> mob.distanceToSqr(entity)
                            + (entity instanceof Player ? 0 : RANGE * RANGE))).orElse(null);
            mob.setTarget(current);
        }
        return current;
    }

    public static void tick(Mob mob, ServerLevel level) {
        if (mob.isNoAi() || !mob.isAlive()) return;
        // serverAiStep 被本模组整体取消，原版 checkDespawn 也随之跳过。
        // 不补这一句的话感染体会永久滞留、越积越多（血雨后尤其明显），最终拖慢服务端与渲染。
        mob.checkDespawn();
        if (!mob.isAlive() || mob.isRemoved()) return;
        State state = STATES.computeIfAbsent(mob, ignored -> new State());
        mob.getSensing().tick();
        LivingEntity target = target(mob, state, level);
        if (target == null) {
            if (mob instanceof Creeper creeper) creeper.setSwellDir(-1);
            mob.setAggressive(false);
            if (mob.tickCount >= state.wander) {
                state.wander = mob.tickCount + 60;
                mob.getNavigation().moveTo(mob.getX() + mob.getRandom().nextInt(13) - 6,
                        mob.getY(), mob.getZ() + mob.getRandom().nextInt(13) - 6, .8);
            }
        } else {
            mob.setAggressive(true);
            mob.getLookControl().setLookAt(target, 30, 30);
            if (mob.tickCount >= state.path) {
                state.path = mob.tickCount + 10;
                mob.getNavigation().moveTo(target, 1.15);
            }
            if (mob.isInWater() || mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation || mob.isNoGravity()) {
                mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.15);
                if (mob.getNavigation().isDone()) {
                    var direction = target.position().subtract(mob.position()).normalize();
                    mob.setDeltaMovement(mob.getDeltaMovement().scale(.9).add(direction.scale(.04)));
                }
            }
            double distance = mob.distanceToSqr(target);
            boolean visible = mob.hasLineOfSight(target);
            if (mob instanceof Creeper creeper) {
                creeper.setSwellDir(visible && distance < 9 ? 1 : -1);
            } else if (visible && mob.tickCount >= state.attack) {
                if (mob instanceof RangedAttackMob ranged && distance <= 16 * 16) {
                    ranged.performRangedAttack(target, 1);
                    state.attack = mob.tickCount + 40;
                } else {
                    double reach = Math.max(1.5, (mob.getBbWidth() + target.getBbWidth()) * .5 + .8);
                    if (distance <= reach * reach) {
                        state.attack = mob.tickCount + 20;
                        mob.swing(InteractionHand.MAIN_HAND);
                        boolean hit = mob.getAttribute(Attributes.ATTACK_DAMAGE) != null
                                ? mob.doHurtTarget(target)
                                : target.hurt(mob.damageSources().mobAttack(mob), 3f);
                    }
                }
            }
        }
        mob.getNavigation().tick();
        mob.getMoveControl().tick();
        mob.getLookControl().tick();
        mob.getJumpControl().tick();
    }
}
