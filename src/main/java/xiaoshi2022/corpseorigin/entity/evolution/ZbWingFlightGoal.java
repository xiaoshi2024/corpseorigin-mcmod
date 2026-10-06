package xiaoshi2022.corpseorigin.entity.evolution;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

import java.util.EnumSet;

/**
 * 长了翅膀的尸兄可以主动起飞追逐目标（含空中的），保留碰撞与天花板高度。
 * 移植自克隆体的 {@link xiaoshi2022.corpseorigin.entity.CloneFlightGoal}，去掉克隆体专属字段。
 * 空战逻辑：优先飞到目标头顶 hover 高度，被墙体挡住时垂直爬升或悬停，绝不穿墙传送。
 */
public final class ZbWingFlightGoal extends Goal {
    private final LowerLevelZbEntity zb;

    public ZbWingFlightGoal(LowerLevelZbEntity zb) {
        this.zb = zb;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = zb.getTarget();
        return target != null && target.isAlive()
                && !zb.isPassenger() && !zb.isInWater()
                && ZbOrganEffects.hasTrait(zb, "wings")
                && zb.distanceToSqr(target) < 40 * 40
                && (target.getY() > zb.getY() + 1.2 || zb.distanceToSqr(target) > 36 || !zb.onGround());
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        zb.getNavigation().stop();
    }

    @Override
    public void stop() {
        zb.setNoGravity(false);
        zb.fallDistance = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = zb.getTarget();
        if (target == null) return;
        zb.setNoGravity(true);
        zb.fallDistance = 0;
        Vec3 desired = target.position().add(0, 0.4, 0).subtract(zb.position());
        double speed = Math.min(.48, .22 + zb.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
        Vec3 motion = desired.lengthSqr() < 1 ? desired.scale(.12) : desired.normalize().scale(speed);
        // 不穿墙、不强制加载区块：上升被挡就先垂直爬升，再不行就地悬停
        if (!zb.level().noCollision(zb, zb.getBoundingBox().move(motion))) {
            Vec3 up = new Vec3(0, .22, 0);
            motion = zb.getY() < target.getY() + 6 && zb.level().noCollision(zb, zb.getBoundingBox().move(up))
                    ? up : Vec3.ZERO;
        }
        zb.setDeltaMovement(zb.getDeltaMovement().scale(.4).add(motion.scale(.6)));
        zb.hurtMarked = true;
    }
}
