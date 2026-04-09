package com.phagens.corpseorigin.GongFU.FaXiang;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class FaxiangMeleeAttackGoal extends MeleeAttackGoal {
    private static final double ATTACK_RANGE = 8.0D;

    public FaxiangMeleeAttackGoal(FaxiangEntity mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        if (this.canPerformAttack(target)) {
            this.resetAttackCooldown();
            this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(target);
        }
    }

    @Override
    protected boolean canPerformAttack(LivingEntity entity) {
        return this.isTimeToAttack()
                && this.mob.distanceToSqr(entity) <= ATTACK_RANGE * ATTACK_RANGE
                && this.mob.getSensing().hasLineOfSight(entity);
    }
}
