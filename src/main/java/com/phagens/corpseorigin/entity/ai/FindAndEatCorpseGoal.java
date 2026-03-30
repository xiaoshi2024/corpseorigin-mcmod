package com.phagens.corpseorigin.entity.ai;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.CorpseGibEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * 尸兄寻找并食用残肢的 AI
 *
 * 【功能说明】
 * 当尸兄饥饿时，会主动寻找附近的残肢并食用
 * 优先级低于攻击目标，但高于随机游荡
 *
 * 【参照 Mob-Dismemberment】
 * 合并尸体和残肢后，统一使用 CorpseGibEntity
 *
 * 【触发条件】
 * - 尸兄饥饿值低于阈值
 * - 附近有残肢
 *
 * 【行为】
 * 1. 寻找最近的残肢
 * 2. 移动到目标位置
 * 3. 食用残肢恢复饥饿值
 */
public class FindAndEatCorpseGoal extends Goal {

    // 饥饿阈值 - 低于此值开始寻找食物
    private static final int HUNGER_THRESHOLD = 40;
    // 搜索范围
    private static final double SEARCH_RANGE = 24.0;
    // 食用距离
    private static final double EAT_DISTANCE = 2.0;
    // 食用冷却时间
    private static final int EAT_COOLDOWN = 40;

    private final PathfinderMob mob;
    private final double speedModifier;

    private CorpseGibEntity targetGib = null;
    private int eatCooldown = 0;
    private int hunger = 100; // 当前饥饿值

    public FindAndEatCorpseGoal(PathfinderMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // 检查饥饿值
        if (hunger > HUNGER_THRESHOLD) {
            return false;
        }

        // 寻找最近的残肢
        findNearestGib();

        return targetGib != null;
    }

    @Override
    public boolean canContinueToUse() {
        // 如果已经吃饱了，停止
        if (hunger >= 100) {
            return false;
        }

        // 检查目标是否还存在
        if (targetGib != null && targetGib.isRemoved()) {
            targetGib = null;
        }

        // 如果目标消失，寻找下一个
        if (targetGib == null) {
            findNearestGib();
        }

        return targetGib != null;
    }

    @Override
    public void start() {
        moveToTarget();
    }

    @Override
    public void stop() {
        targetGib = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (eatCooldown > 0) {
            eatCooldown--;
        }

        // 自然降低饥饿值
        if (mob.tickCount % 200 == 0) {
            hunger = Math.max(0, hunger - 1);
        }

        // 检查是否可以食用
        if (targetGib != null) {
            double distance = this.mob.distanceTo(targetGib);

            // 看向目标
            this.mob.getLookControl().setLookAt(targetGib, 30.0F, 30.0F);

            if (distance <= EAT_DISTANCE && eatCooldown <= 0) {
                // 食用残肢
                eatGib();
            } else if (distance > EAT_DISTANCE) {
                // 继续移动
                moveToTarget();
            }
        }
    }

    /**
     * 寻找最近的残肢
     */
    private void findNearestGib() {
        targetGib = null;

        // 搜索残肢 - CorpseGibEntity 不是 LivingEntity，使用 getEntitiesOfClass
        List<CorpseGibEntity> gibs = this.mob.level().getEntitiesOfClass(
                CorpseGibEntity.class,
                this.mob.getBoundingBox().inflate(SEARCH_RANGE)
        );

        // 找到最近的
        double closestDistance = Double.MAX_VALUE;

        for (CorpseGibEntity gib : gibs) {
            double dist = this.mob.distanceToSqr(gib);
            if (dist < closestDistance && dist <= SEARCH_RANGE * SEARCH_RANGE) {
                closestDistance = dist;
                targetGib = gib;
            }
        }
    }

    /**
     * 移动到目标
     */
    private void moveToTarget() {
        if (targetGib == null) return;

        Vec3 targetPos = targetGib.position();

        Path path = this.mob.getNavigation().createPath(
                (int) targetPos.x, (int) targetPos.y, (int) targetPos.z, 0);

        if (path != null) {
            this.mob.getNavigation().moveTo(path, this.speedModifier);
        }
    }

    /**
     * 食用残肢
     */
    private void eatGib() {
        if (targetGib == null || targetGib.isRemoved()) return;

        // 食用
        int nutrition = targetGib.consume(null);

        if (nutrition > 0) {
            // 恢复饥饿值
            hunger = Math.min(100, hunger + nutrition * 5);

            // 播放音效
            this.mob.playSound(net.minecraft.sounds.SoundEvents.PLAYER_BURP,
                    0.5f, 0.8f + this.mob.getRandom().nextFloat() * 0.4f);

            CorpseOrigin.LOGGER.debug("尸兄 {} 食用了残肢 {} (营养值: {})",
                    this.mob.getName().getString(), targetGib.getPartTypeName(), nutrition);
        }

        // 设置冷却
        eatCooldown = EAT_COOLDOWN;
        targetGib = null;
    }

    /**
     * 设置饥饿值 (外部调用)
     */
    public void setHunger(int hunger) {
        this.hunger = Math.max(0, Math.min(100, hunger));
    }

    /**
     * 获取饥饿值
     */
    public int getHunger() {
        return hunger;
    }
}
