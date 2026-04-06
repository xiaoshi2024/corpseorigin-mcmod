package com.phagens.corpseorigin.entity.EntityAI.JLAI;

import com.phagens.corpseorigin.entity.CorpseGibEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * 水生生物寻找并吞噬尸体的AI目标
 * 适用于尸兄鱼等水生尸兄
 */
public class AquaticSeekCorpseGibGoal extends Goal {
    private final PathfinderMob mob;
    private final Consumer<CorpseGibEntity> eatCallback;
    private final java.util.function.IntSupplier corpseHungerGetter;
    private CorpseGibEntity targetCorpse;
    private static final double SEARCH_RANGE = 12.0D;
    private static final double EAT_RANGE = 2.0D;
    private int cooldown = 0;
    
    public AquaticSeekCorpseGibGoal(PathfinderMob mob, Consumer<CorpseGibEntity> eatCallback, java.util.function.IntSupplier corpseHungerGetter) {
        this.mob = mob;
        this.eatCallback = eatCallback;
        this.corpseHungerGetter = corpseHungerGetter;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        
        if (corpseHungerGetter.getAsInt() >= 3) {
            return false;
        }
        
        if (mob.getTarget() != null && mob.getTarget().isAlive()) {
            return false;
        }
        
        return findNearestCorpse();
    }
    
    @Override
    public boolean canContinueToUse() {
        if (targetCorpse == null || !targetCorpse.isAlive()) {
            return false;
        }
        
        if (corpseHungerGetter.getAsInt() >= 3) {
            return false;
        }
        
        if (mob.getTarget() != null && mob.getTarget().isAlive()) {
            return false;
        }
        
        return mob.distanceTo(targetCorpse) <= SEARCH_RANGE;
    }
    
    @Override
    public void start() {
        if (targetCorpse != null) {
            mob.getNavigation().moveTo(targetCorpse, 1.2D);
        }
    }
    
    @Override
    public void stop() {
        targetCorpse = null;
        cooldown = 20;
        mob.getNavigation().stop();
    }
    
    @Override
    public void tick() {
        if (targetCorpse == null || !targetCorpse.isAlive()) {
            return;
        }
        
        mob.getLookControl().setLookAt(targetCorpse, 30.0F, 30.0F);
        
        double distance = mob.distanceTo(targetCorpse);
        
        if (distance <= EAT_RANGE) {
            eatCallback.accept(targetCorpse);
            targetCorpse = null;
        } else {
            mob.getNavigation().moveTo(targetCorpse, 1.2D);
        }
    }
    
    private boolean findNearestCorpse() {
        Level level = mob.level();
        if (!(level instanceof ServerLevel serverLevel)) return false;
        
        AABB searchBox = mob.getBoundingBox().inflate(SEARCH_RANGE);
        List<CorpseGibEntity> nearbyCorpses = serverLevel.getEntitiesOfClass(
            CorpseGibEntity.class, 
            searchBox,
            corpse -> corpse.isAlive()
        );
        
        if (nearbyCorpses.isEmpty()) {
            return false;
        }
        
        CorpseGibEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (CorpseGibEntity corpse : nearbyCorpses) {
            double dist = mob.distanceToSqr(corpse);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = corpse;
            }
        }
        
        if (nearest != null) {
            targetCorpse = nearest;
            return true;
        }
        
        return false;
    }
}
