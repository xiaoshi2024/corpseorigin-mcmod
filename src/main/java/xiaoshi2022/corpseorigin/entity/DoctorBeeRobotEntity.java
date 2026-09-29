package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Tian Doctor's small autonomous anti-worm drone. */
public final class DoctorBeeRobotEntity extends PathfinderMob implements GeoEntity {
    private static final int ORBIT_TICKS = 100;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int orbitTicks;
    private double orbitStartAngle;
    private int diveTicks;

    public DoctorBeeRobotEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, .4)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override protected void registerGoals() {
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this,
                MultiHeadCorpseWormEntity.class, true));
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;
        if (!(getTarget() instanceof MultiHeadCorpseWormEntity worm) || !worm.isAlive()) {
            setDeltaMovement(Vec3.ZERO);
            if (tickCount > 600) discard();
            return;
        }

        getNavigation().stop();
        Vec3 center = worm.getBoundingBox().getCenter();
        if (orbitTicks == 0) orbitStartAngle = Math.atan2(getZ() - center.z, getX() - center.x);
        Vec3 destination;
        if (orbitTicks < ORBIT_TICKS) {
            double angle = orbitStartAngle + orbitTicks * (Math.PI * 4 / ORBIT_TICKS);
            double radius = Math.max(2.5, worm.getBbWidth() * .7 + 1.5);
            destination = center.add(Math.cos(angle) * radius, 1.0, Math.sin(angle) * radius);
            if (position().distanceToSqr(destination) < 2.25) orbitTicks++;
        } else {
            destination = center;
            diveTicks++;
            if (getBoundingBox().inflate(.4).intersects(worm.getBoundingBox())) {
                doHurtTarget(level, worm);
                return;
            }
            if (diveTicks > 80) { discard(); return; }
        }
        Vec3 travel = destination.subtract(position());
        double speed = orbitTicks < ORBIT_TICKS ? .55 : 1.0;
        if (travel.lengthSqr() > speed * speed) travel = travel.normalize().scale(speed);
        setDeltaMovement(Vec3.ZERO);
        setPos(position().add(travel));
        if (travel.horizontalDistanceSqr() > 0.001) {
            setYRot((float) (Math.toDegrees(Math.atan2(-travel.x, travel.z))));
            setYBodyRot(getYRot());
        }
    }

    @Override public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        if (target instanceof MultiHeadCorpseWormEntity worm) {
            boolean hit = worm.hurtServer(level, damageSources().mobAttack(this),
                    Math.max(worm.getHealth(), worm.getMaxHealth()));
            if (hit) discard();
            return hit;
        }
        return super.doHurtTarget(level, target);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DoctorBeeRobotEntity>("main", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
