package com.phagens.corpseorigin.entity.EntityAI.JLAI;

import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 飞扑攻击AI目标
 * 尸兄在距离目标3-5格时飞扑向目标，并给予3秒缓慢I效果
 */
public class PounceAttackGoal extends Goal {
    private final LowerLevelZbEntity mob;
    private LivingEntity target;
    private int cooldown = 0;
    private static final int COOLDOWN_TICKS = 100; // 5秒冷却
    private static final double MIN_POUNCE_DISTANCE = 3.0D;
    private static final double MAX_POUNCE_DISTANCE = 5.0D;
    private static final double POUNCE_STRENGTH = 0.8D;
    private static final int SLOWNESS_DURATION = 60; // 3秒 = 60 ticks

    public PounceAttackGoal(LowerLevelZbEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            return false;
        }
        
        if (mob.isPouncing()) {
            return false;
        }
        
        target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        
        double distance = mob.distanceTo(target);
        return distance >= MIN_POUNCE_DISTANCE && distance <= MAX_POUNCE_DISTANCE && mob.onGround();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.isPouncing();
    }

    @Override
    public void start() {
        if (target == null) return;
        
        mob.setPouncing(true);
        
        Vec3 direction = new Vec3(
            target.getX() - mob.getX(),
            target.getY() - mob.getY(),
            target.getZ() - mob.getZ()
        );
        
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (horizontalDistance > 0.001D) {
            double verticalStrength = 0.4D;
            double horizontalStrength = POUNCE_STRENGTH;
            
            mob.setDeltaMovement(
                (direction.x / horizontalDistance) * horizontalStrength,
                verticalStrength,
                (direction.z / horizontalDistance) * horizontalStrength
            );
            
            mob.hasImpulse = true;
        }
        
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                mob.getX(), mob.getY() + 0.5, mob.getZ(),
                10, 0.3, 0.1, 0.3, 0.05);
        }
    }

    @Override
    public void stop() {
        mob.setPouncing(false);
        cooldown = COOLDOWN_TICKS;
        target = null;
    }

    @Override
    public void tick() {
        if (target == null) {
            mob.setPouncing(false);
            return;
        }
        
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        
        if (mob.onGround() && mob.isPouncing()) {
            mob.setPouncing(false);
        }
        
        double distance = mob.distanceTo(target);
        if (distance < 2.0D && mob.isPouncing()) {
            applySlownessEffect(target);
            mob.doHurtTarget(target);
            mob.setPouncing(false);
        }
    }
    
    private void applySlownessEffect(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_DURATION, 0, false, true));
        
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                target.getX(), target.getY() + 1, target.getZ(),
                5, 0.3, 0.3, 0.3, 0.1);
        }
    }
    
    public void tickCooldown() {
        if (cooldown > 0) {
            cooldown--;
        }
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    public void resetCooldown() {
        cooldown = COOLDOWN_TICKS;
    }
}
