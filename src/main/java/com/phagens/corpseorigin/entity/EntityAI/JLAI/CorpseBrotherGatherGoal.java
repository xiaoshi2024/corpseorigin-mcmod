package com.phagens.corpseorigin.entity.EntityAI.JLAI;

import com.phagens.corpseorigin.entity.CorpseBrotherHiveMind;
import com.phagens.corpseorigin.entity.ICorpseBrother;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

import java.util.EnumSet;

public class CorpseBrotherGatherGoal extends Goal {

    private final Mob mob;
    private final ICorpseBrother brother;
    private final double speedModifier;
    private LivingEntity gatherTarget;
    private int recalcPathTimer;

    public CorpseBrotherGatherGoal(Mob mob, double speedModifier) {
        this.mob = mob;
        this.brother = (ICorpseBrother) mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.brother.hasAttackTarget()) return false;
        if (this.mob.getTarget() != null) return false;
        if (!(this.mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return false;

        this.gatherTarget = CorpseBrotherHiveMind.findGatherTarget(serverLevel, this.mob);
        return this.gatherTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.brother.hasAttackTarget()) return false;
        if (this.mob.getTarget() != null) return false;
        if (this.gatherTarget == null || !this.gatherTarget.isAlive()) return false;
        if (this.mob.distanceTo(this.gatherTarget) < 3.0D) return false;
        return true;
    }

    @Override
    public void start() {
        this.recalcPathTimer = 0;
    }

    @Override
    public void stop() {
        this.gatherTarget = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.gatherTarget == null) return;

        this.mob.getLookControl().setLookAt(this.gatherTarget, 30.0F, 30.0F);

        if (--this.recalcPathTimer <= 0) {
            this.recalcPathTimer = 10;
            PathNavigation nav = this.mob.getNavigation();
            if (!nav.moveTo(this.gatherTarget, this.speedModifier)) {
                if (this.mob.distanceTo(this.gatherTarget) > 16.0D) {
                    this.mob.moveRelative((float)this.speedModifier,
                            new net.minecraft.world.phys.Vec3(
                                    this.gatherTarget.getX() - this.mob.getX(),
                                    0,
                                    this.gatherTarget.getZ() - this.mob.getZ()
                            ).normalize()
                    );
                }
            }
        }
    }
}
