package xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/** Winged bodies can pursue elevated targets while retaining collision and a bounded flight ceiling. */
public final class CloneFlightGoal extends Goal {
    private final CloneAvatarEntity clone;
    public CloneFlightGoal(CloneAvatarEntity clone) {
        this.clone = clone;
        setFlags(EnumSet.of(Flag.MOVE));
    }
    @Override public boolean canUse() {
        var target = clone.getTarget();
        return clone.isActive() && !clone.isPassenger() && !clone.isInWater()
                && target != null && target.isAlive() && CloneOrganEffects.hasTrait(clone, "wings")
                && clone.distanceToSqr(target) < 40 * 40
                && (target.getY() > clone.getY() + 1.2 || clone.distanceToSqr(target) > 36 || !clone.onGround());
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() {
        clone.getNavigation().stop();
        clone.setOrganFlying(true);
    }
    @Override public void stop() {
        clone.setOrganFlying(false);
        clone.setNoGravity(false);
        clone.fallDistance = 0;
    }
    @Override public void tick() {
        var target = clone.getTarget();
        if (target == null) return;
        clone.setNoGravity(true);
        clone.fallDistance = 0;
        double hover = CloneWeaponArts.isRangedWeapon(clone.getMainHandItem()) ? 2 : .4;
        Vec3 desired = target.position().add(0, hover, 0).subtract(clone.position());
        double speed = Math.min(.48, .22 + clone.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
        Vec3 motion = desired.lengthSqr() < 1 ? desired.scale(.12) : desired.normalize().scale(speed);
        // Never teleport through walls or force-load terrain; a blocked ascent falls back to ground navigation.
        if (!clone.level().noCollision(clone, clone.getBoundingBox().move(motion))) {
            Vec3 up = new Vec3(0, .22, 0);
            motion = clone.getY() < target.getY() + 6 && clone.level().noCollision(clone, clone.getBoundingBox().move(up))
                    ? up : Vec3.ZERO;
        }
        clone.setDeltaMovement(clone.getDeltaMovement().scale(.4).add(motion.scale(.6)));
        clone.hurtMarked = true;
    }
}
