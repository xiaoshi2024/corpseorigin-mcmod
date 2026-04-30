package com.phagens.corpseorigin.entity.EntityAI.JLAI;

import com.phagens.corpseorigin.entity.CorpseBrotherHiveMind;
import com.phagens.corpseorigin.entity.ICorpseBrother;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class CorpseBrotherHiveMindGoal extends Goal {

    private final Mob mob;
    private final ICorpseBrother brother;
    private int broadcastCooldown;

    public CorpseBrotherHiveMindGoal(Mob mob) {
        this.mob = mob;
        this.brother = (ICorpseBrother) mob;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!(this.mob.level() instanceof ServerLevel)) return false;
        
        // 检查是否有龙右存在
        boolean hasLongyou = this.mob.level().getEntitiesOfClass(
                com.phagens.corpseorigin.entity.LongyouEntity.class,
                this.mob.getBoundingBox().inflate(100.0D)
        ).size() > 0;
        
        if (!hasLongyou) return false;
        
        // 只有在没有当前目标但有共享目标时才激活
        // 这样不会阻止其他目标选择器（如玩家检测）运行
        LivingEntity currentTarget = this.mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive()) {
            return false; // 已有目标，让其他选择器处理
        }
        
        LivingEntity hiveTarget = this.brother.getHiveMindTarget();
        return hiveTarget != null && hiveTarget.isAlive();
    }

    @Override
    public void start() {
        this.broadcastCooldown = 0;
    }

    @Override
    public void tick() {
        if (--this.broadcastCooldown > 0) return;
        this.broadcastCooldown = 20;

        if (!(this.mob.level() instanceof ServerLevel serverLevel)) return;

        LivingEntity currentTarget = this.mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive() && !CorpseBrotherHiveMind.isCorpseBrother(currentTarget)) {
            CorpseBrotherHiveMind.broadcastTarget(serverLevel, this.mob, currentTarget);
            return;
        }

        LivingEntity hiveTarget = this.brother.getHiveMindTarget();
        if (hiveTarget != null && hiveTarget.isAlive() && this.mob.getTarget() == null) {
            if (!CorpseBrotherHiveMind.isCorpseBrother(hiveTarget)) {
                this.mob.setTarget(hiveTarget);
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        // 只有在没有当前目标且有共享目标时才继续激活
        if (!(this.mob.level() instanceof ServerLevel)) return false;
        if (this.mob.getTarget() != null && this.mob.getTarget().isAlive()) return false;
        
        LivingEntity hiveTarget = this.brother.getHiveMindTarget();
        return hiveTarget != null && hiveTarget.isAlive();
    }
}
