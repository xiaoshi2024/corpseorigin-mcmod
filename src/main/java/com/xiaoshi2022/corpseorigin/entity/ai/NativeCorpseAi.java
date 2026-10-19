package com.xiaoshi2022.corpseorigin.entity.ai;

import com.xiaoshi2022.corpseorigin.entity.ZombieKin;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;

/**
 * 感染后的 AI 替换：所有被感染的原版 Mob 用「尸兄猎手」AI，
 * 但仍使用原宿主的身体、导航、攻击动画（不替换实体类型）。
 * <p>
 * 1.20.1 Forge 简化版：移除 ZbOrganEffects、GourdCapture 等上游依赖，
 * 保留目标搜索、追击、近战攻击核心链路。
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
                && !ZombieKin.isZombieKin(target)
                && !mob.isAlliedTo(target) && mob.distanceToSqr(target) <= RANGE * RANGE;
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
