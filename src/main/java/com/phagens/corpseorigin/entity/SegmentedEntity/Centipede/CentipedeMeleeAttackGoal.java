package com.phagens.corpseorigin.entity.SegmentedEntity.Centipede;

import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class CentipedeMeleeAttackGoal extends MeleeAttackGoal {
    private final CentipedeHead centipede;

    public CentipedeMeleeAttackGoal(CentipedeHead mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
        this.centipede = mob;
    }

    @Override
    public void start() {
        super.start();
        centipede.openMouth();
    }

    @Override
    public void stop() {
        super.stop();
        centipede.closeMouth();
    }
}