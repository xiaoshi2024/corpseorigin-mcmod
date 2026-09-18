package xiaoshi2022.corpseorigin.client.camera;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class PersistentCameraEntityGoal {
    public static final double MAX_DISTANCE = 25;
    public static final long PHASE_DELAY = 200;
    public static final double MAX_Y = 320;
    public static final long MIN_PHASE_DURATION = 400;
    public static final long MAX_PHASE_DURATION = 2500;

    public final Vec3 pos;
    public final float yaw;
    public final float pitch;
    public final long delay;
    public final long duration;
    private final Consumer<PersistentCameraEntity> onTransitionFinished;

    private PersistentCameraEntityGoal(Vec3 pos, float yaw, float pitch, long delay, long duration,
                                       Consumer<PersistentCameraEntity> onTransitionFinished) {
        this.pos = pos;
        this.yaw = yaw;
        this.pitch = pitch;
        this.delay = delay;
        this.duration = duration;
        this.onTransitionFinished = onTransitionFinished;
    }

    public void finish(PersistentCameraEntity camera) {
        if (this.onTransitionFinished != null) {
            this.onTransitionFinished.accept(camera);
        }
    }

    public PersistentCameraEntityGoal then(PersistentCameraEntityGoal nextGoal) {
        return this.then(camera -> camera.setGoal(nextGoal));
    }

    public PersistentCameraEntityGoal then(Consumer<PersistentCameraEntity> callback) {
        Consumer<PersistentCameraEntity> combined = callback == null
                ? this.onTransitionFinished
                : this.onTransitionFinished == null
                ? callback
                : this.onTransitionFinished.andThen(callback);
        return new PersistentCameraEntityGoal(this.pos, this.yaw, this.pitch, this.delay, this.duration, combined);
    }

    public static PersistentCameraEntityGoal create(BlockPos pos, float yaw, float pitch, long duration) {
        return create(pos, yaw, pitch, 0, duration, null);
    }

    public static PersistentCameraEntityGoal create(BlockPos pos, float yaw, float pitch, long delay, long duration,
                                                    Consumer<PersistentCameraEntity> onTransitionFinished) {
        Vec3 vecPos = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        return create(vecPos, yaw, pitch, delay, duration, onTransitionFinished);
    }

    public static PersistentCameraEntityGoal create(Vec3 pos, float yaw, float pitch, long duration) {
        return create(pos, yaw, pitch, 0, duration, null);
    }

    public static PersistentCameraEntityGoal create(Vec3 pos, float yaw, float pitch, long delay, long duration,
                                                    Consumer<PersistentCameraEntity> onTransitionFinished) {
        return new PersistentCameraEntityGoal(pos, yaw, pitch, delay, duration, onTransitionFinished);
    }

    public static PersistentCameraEntityGoal tp(BlockPos pos, float yaw, float pitch) {
        return create(pos, yaw, pitch, 0, 0, null);
    }

    public static PersistentCameraEntityGoal tp(BlockPos pos, float yaw, float pitch,
                                                Consumer<PersistentCameraEntity> onTransitionFinished) {
        return create(pos, yaw, pitch, 0, 0, onTransitionFinished);
    }

    public static PersistentCameraEntityGoal limbo(BlockPos start, Direction startFacing, BlockPos target,
                                                   Consumer<PersistentCameraEntity> onTransitionFinished) {
        return limbo(start, startFacing, MAX_Y, target, MIN_PHASE_DURATION, MAX_PHASE_DURATION,
                PHASE_DELAY, MAX_DISTANCE, onTransitionFinished);
    }

    public static PersistentCameraEntityGoal limbo(BlockPos start, Direction startFacing, double y, BlockPos target,
                                                   long firstPhaseDuration, long secondPhaseDuration, long phaseDelay,
                                                   double maxDistance, Consumer<PersistentCameraEntity> onTransitionFinished) {
        double dX = target.getX() - start.getX();
        double dZ = target.getZ() - start.getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);
        if (horizontalDistance > maxDistance) {
            double factor = maxDistance / horizontalDistance;
            target = new BlockPos((int) (start.getX() + dX * factor), start.getY(), (int) (start.getZ() + dZ * factor));
        }

        float yaw = startFacing.toYRot();
        float pitch = 90;
        BlockPos pos0 = start.above();
        Vec3 pos1 = new Vec3(start.getX() + target.getX(), y * 2, start.getZ() + target.getZ()).scale(0.5);

        PersistentCameraEntityGoal goal0 = create(pos0, yaw, pitch, firstPhaseDuration);
        PersistentCameraEntityGoal goal1 = create(pos1, yaw, pitch, phaseDelay, secondPhaseDuration, onTransitionFinished);

        return goal0.then(goal1);
    }

    public static PersistentCameraEntityGoal stairwayToHeaven(BlockPos start, Direction startFacing, BlockPos target,
                                                              Consumer<PersistentCameraEntity> onTransitionFinished) {
        return stairwayToHeaven(start, startFacing, MAX_Y, target, MIN_PHASE_DURATION, MAX_PHASE_DURATION,
                PHASE_DELAY, MAX_DISTANCE, onTransitionFinished);
    }

    public static PersistentCameraEntityGoal stairwayToHeaven(BlockPos start, Direction startFacing, double y, BlockPos target,
                                                              long firstPhaseDuration, long secondPhaseDuration, long phaseDelay,
                                                              double maxDistance, Consumer<PersistentCameraEntity> onTransitionFinished) {
        double dX = target.getX() - start.getX();
        double dZ = target.getZ() - start.getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);
        if (horizontalDistance > maxDistance) {
            double factor = maxDistance / horizontalDistance;
            target = new BlockPos((int) (start.getX() + dX * factor), start.getY(), (int) (start.getZ() + dZ * factor));
        }

        float yaw = startFacing.toYRot();
        float pitch = 90;
        BlockPos pos0 = start.relative(startFacing.getOpposite());
        Vec3 pos1 = new Vec3(start.getX() + target.getX(), y * 2, start.getZ() + target.getZ()).scale(0.5);

        PersistentCameraEntityGoal goal0 = create(pos0, yaw, 0, firstPhaseDuration);
        PersistentCameraEntityGoal goal1 = create(pos1, yaw, pitch, phaseDelay, secondPhaseDuration, onTransitionFinished);

        return goal0.then(goal1);
    }

    public static PersistentCameraEntityGoal highwayToHell(BlockPos start, Direction startFacing, BlockPos target,
                                                           Direction targetFacing,
                                                           Consumer<PersistentCameraEntity> onTransitionFinished) {
        return highwayToHell(start, startFacing, MAX_Y, target, targetFacing, MAX_PHASE_DURATION, MIN_PHASE_DURATION,
                PHASE_DELAY, MAX_DISTANCE, onTransitionFinished);
    }

    /** 直角直出的垂直段高度（格）—— 刻意不抬高太多，这条分支要的是"直出"不是"上天" */
    public static final double RIGHT_ANGLE_RISE = 8.0;
    /** 起点和目标重合（原地换身）时，水平段甩出去的距离（格） */
    public static final double RIGHT_ANGLE_OUT = 8.0;
    public static final long RIGHT_ANGLE_RISE_DURATION = 900;
    public static final long RIGHT_ANGLE_OUT_DURATION = 800;
    public static final long RIGHT_ANGLE_DELAY = 150;

    /**
     * 直角直出：先原地<b>垂直抬起</b>，到位后 90° 拐弯，沿水平方向<b>直甩出去</b>。
     * <p>
     * 和 {@link #stairwayToHeaven} 的区别就在这条直角折线：天梯是先退后飞、飞到 y=320 再落下来，
     * 而这条分支全程只抬 {@link #RIGHT_ANGLE_RISE} 格，然后横着抽走 —— 没有"上天"的观感。
     * <p>
     * 水平段的方向：朝目标（换到远处那具身体时，镜头正好"飞向"它）；起点和目标几乎重合时
     * （金蝉脱壳 / 血肉重塑都是原地换身）沿<b>身体朝向的反方向</b>抽出来，像是在把意识从躯壳里拉出。
     */
    public static PersistentCameraEntityGoal rightAngleExit(BlockPos start, Direction startFacing, BlockPos target,
                                                            Consumer<PersistentCameraEntity> onTransitionFinished) {
        double dX = target.getX() - start.getX();
        double dZ = target.getZ() - start.getZ();
        double horizontal = Math.sqrt(dX * dX + dZ * dZ);
        double outDistance;
        if (horizontal < 1.0) {
            dX = -startFacing.getStepX();
            dZ = -startFacing.getStepZ();
            horizontal = 1.0;
            outDistance = RIGHT_ANGLE_OUT;
        } else {
            outDistance = Math.min(horizontal, MAX_DISTANCE);
        }
        double dirX = dX / horizontal;
        double dirZ = dZ / horizontal;

        double centerX = start.getX() + 0.5;
        double centerZ = start.getZ() + 0.5;
        Vec3 elevated = new Vec3(centerX, start.getY() + RIGHT_ANGLE_RISE, centerZ);
        Vec3 outPos = new Vec3(centerX + dirX * outDistance, elevated.y, centerZ + dirZ * outDistance);
        // 水平段的视线顺着"甩出去"的方向：直出感来自镜头也跟着拐这 90°
        float exitYaw = (float) Math.toDegrees(Math.atan2(-dirX, dirZ));

        PersistentCameraEntityGoal rise = create(elevated, startFacing.toYRot(), 0, RIGHT_ANGLE_RISE_DURATION);
        PersistentCameraEntityGoal out = create(outPos, exitYaw, 0, RIGHT_ANGLE_DELAY,
                RIGHT_ANGLE_OUT_DURATION, onTransitionFinished);
        return rise.then(out);
    }

    public static PersistentCameraEntityGoal highwayToHell(BlockPos start, Direction startFacing, double y, BlockPos target,
                                                           Direction targetFacing, long firstPhaseDuration, long secondPhaseDuration,
                                                           long phaseDelay, double maxDistance,
                                                           Consumer<PersistentCameraEntity> onTransitionFinished) {
        BlockPos pos0 = target.relative(targetFacing.getOpposite());
        float yaw0 = targetFacing.toYRot();
        float yaw1 = targetFacing.getOpposite().toYRot();

        Vec3 centerPoint = new Vec3(start.getX() + target.getX(), y * 2, start.getZ() + target.getZ()).scale(0.5);
        PersistentCameraEntityGoal tpGoal = null;
        double dX = centerPoint.x - target.getX();
        double dZ = centerPoint.z - target.getZ();
        double horizontalDistance = Math.sqrt(dX * dX + dZ * dZ);
        if (horizontalDistance > maxDistance) {
            double factor = maxDistance / horizontalDistance;
            BlockPos centerPointPos = new BlockPos(
                    (int) (target.getX() + dX * factor),
                    (int) centerPoint.y,
                    (int) (target.getZ() + dZ * factor));
            tpGoal = PersistentCameraEntityGoal.tp(centerPointPos, startFacing.toYRot(), 90);
        }

        PersistentCameraEntityGoal goal0 = create(pos0, yaw0, 0, firstPhaseDuration);
        PersistentCameraEntityGoal goal1 = create(target, yaw1, 0, phaseDelay, secondPhaseDuration, onTransitionFinished);

        return tpGoal == null ? goal0.then(goal1) : tpGoal.then(goal0.then(goal1));
    }
}