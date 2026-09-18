package xiaoshi2022.corpseorigin.client.camera;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.client.gui.CameraBlackoutScreen;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public class PersistentCameraEntity extends LocalPlayer {

    /**
     * 假人实体 ID 计数器。
     * <p>
     * 26.2 起实体的 id 不能为 0：{@link net.minecraft.world.entity.Entity#getId()} 在 id 未分配时会抛
     * IllegalStateException。本类是在客户端 {@code new} 出来的假人（不走 EntityType.create，也不会
     * 被加进世界），id 一直是 0，于是 Iris 阴影 pass 等任何调用 getId 的地方都会崩客户端。
     * 这里自己发一个 ID，用负数避开服务端分配的真实 ID。
     */
    private static final java.util.concurrent.atomic.AtomicInteger NEXT_FAKE_ID =
            new java.util.concurrent.atomic.AtomicInteger(-1);

    private long lastMovementTime;
    private float initialYaw;
    private float initialPitch;
    private double initialDistance;
    private PersistentCameraEntityGoal goal;

    private static boolean hudWasHiddenBeforeCamera;

    /** 复用的假人相机：过场第一帧现造一个完整的 LocalPlayer 会卡一帧，所以提前造好反复用。 */
    private static PersistentCameraEntity cachedCamera;

    /** 换身体落位检测 + 下落附身：等客户端真的落到目标位置，再让镜头扎进新身体（见 {@link #beginHandoff}） */
    private static final int HANDOFF_MAX_TICKS = 60;    // 最多在天上等 3 秒
    private static final int HANDOFF_SETTLE_TICKS = 3;  // 落位后再等几 tick，让区块/实体同步完

    private static BlockPos handoffStartPos;
    private static Direction handoffStartFacing;
    private static BlockPos handoffTargetPos;
    private static Direction handoffTargetFacing;
    private static Identifier handoffTargetWorld;
    private static int handoffWaitTicks;
    private static int handoffSettleTicks;
    /** true = 直角分支：不播"下落附身"，落位后直接在黑幕里把视角交还 */
    private static boolean handoffDirectRelease;

    private PersistentCameraEntity(Minecraft client) {
        super(client,
                Objects.requireNonNull(client.level),
                Objects.requireNonNull(client.getConnection()),
                client.player.getStats(),
                client.player.getRecipeBook(),
                Input.EMPTY,
                false,
                client.player.chatAbilities());

        // ★ 必须先分配 ID，否则任何 getId() 都会抛异常（26.2 的新校验）
        this.setId(NEXT_FAKE_ID.getAndDecrement());

        this.snapTo(client.player);
        this.noPhysics = true;

        // 相机实体只用来算摄像机，不该被画出来：不隐藏的话光影的阴影 pass 会把它当成一个
        // 站在相机位置上的玩家来提交，白白多一套渲染状态
        this.setInvisible(true);
    }

    /** 把相机对齐到玩家当前位置/朝向（作为过场起点） */
    private void snapTo(LocalPlayer player) {
        this.setPos(player.getX(), player.getY(), player.getZ());
        this.setYRot(player.getYRot());
        this.setXRot(player.getXRot());
        this.setYHeadRot(player.getYRot());
        this.setYBodyRot(player.getYRot());
        this.updateLastTickValues();
    }

    /** 取当前世界对应的假人相机，没有就现造一个（换维度/换世界后重建） */
    private static PersistentCameraEntity cameraFor(Minecraft client) {
        if (cachedCamera == null || cachedCamera.level() != client.level) {
            cachedCamera = new PersistentCameraEntity(client);
        }
        return cachedCamera;
    }

    /**
     * 过场第一段（灵魂上天）结束、已通知服务端换身体：镜头先留在天上，等客户端真正落到目标位置，
     * 再播第二段「下落附身」——从高空扎进新身体，到位后才把视角交还给玩家。
     * <p>
     * 这样做有两个好处：一是还镜头的那一刻正好撞上服务端传送（跨维度时客户端还会整个重建世界、
     * 实体重新同步），当场交还视角会看到"其它实体消失一帧"；二是附身本来该有个落下来的过程，
     * 直接从天上瞬移回玩家视角会很跳。
     */
    public static void beginHandoff(BlockPos startPos, Direction startFacing,
                                    BlockPos targetPos, Direction targetFacing, Identifier targetWorld) {
        handoffStartPos = startPos;
        handoffStartFacing = startFacing;
        handoffTargetPos = targetPos;
        handoffTargetFacing = targetFacing;
        handoffTargetWorld = targetWorld;
        handoffWaitTicks = HANDOFF_MAX_TICKS;
        handoffSettleTicks = HANDOFF_SETTLE_TICKS;
    }

    private static void clearHandoff() {
        handoffStartPos = null;
        handoffStartFacing = null;
        handoffTargetPos = null;
        handoffTargetFacing = null;
        handoffTargetWorld = null;
        handoffWaitTicks = 0;
        handoffSettleTicks = 0;
        handoffDirectRelease = false;
    }

    /**
     * 直角分支（尸王换身）的收尾：不下落也不附身，等服务端换完、玩家真的落到目标位置后，
     * 直接在黑幕里把视角交还 —— 这一段玩家本来就被黑场盖着，"睁眼"时人已经在新身体里了。
     */
    public static void beginDirectRelease(BlockPos targetPos, Identifier targetWorld) {
        handoffStartPos = null;
        handoffStartFacing = null;
        handoffTargetPos = targetPos;
        handoffTargetFacing = null;
        handoffTargetWorld = targetWorld;
        handoffWaitTicks = HANDOFF_MAX_TICKS;
        handoffSettleTicks = HANDOFF_SETTLE_TICKS;
        handoffDirectRelease = true;
    }

    /** @return true 表示镜头还在等落位（本次 tick 已经处理过了） */
    private static boolean tickHandoff(Minecraft client) {
        if (handoffTargetPos == null) {
            return false;
        }

        LocalPlayer player = client.player;
        boolean landed = player != null
                && client.level != null
                && (handoffTargetWorld == null
                || handoffTargetWorld.equals(client.level.dimension().identifier()))
                && player.blockPosition().closerThan(handoffTargetPos, 16.0);

        if (landed && handoffSettleTicks > 0) {
            // 落位了也不马上下落，再等几 tick 让区块和实体同步完
            handoffSettleTicks--;
            landed = false;
        }

        if (!landed && --handoffWaitTicks > 0) {
            return true;
        }

        // ★ 直角分支：换身已完成、人也落位了 —— 不搞"从天上扎下来"，就在黑幕里把视角还回去，然后睁眼
        if (handoffDirectRelease) {
            clearHandoff();
            unset(client);
            CameraBlackoutScreen.fadeOut();
            return true;
        }

        // 落位（或等超时）：镜头接着从高空扎进新身体，到位后再交还视角
        BlockPos startPos = handoffStartPos;
        Direction startFacing = handoffStartFacing;
        BlockPos targetPos = handoffTargetPos;
        Direction targetFacing = handoffTargetFacing;
        PersistentCameraEntity camera = client.getCameraEntity() instanceof PersistentCameraEntity c ? c : null;
        clearHandoff();

        if (camera == null || startPos == null) {
            unset(client);   // 镜头本来就不在我们手上：直接结束
            return true;
        }

        camera.setGoal(PersistentCameraEntityGoal.highwayToHell(startPos, startFacing,
                targetPos.above(), targetFacing, __ -> unset(client)));
        return true;
    }

    @Override
    public boolean isSpectator() {
        return true;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.tickMovement();
    }

    public void tickMovement() {
        PersistentCameraEntityGoal goal = this.goal;
        long currentTime = System.currentTimeMillis();
        if (goal == null || this.lastMovementTime < 0) {
            this.lastMovementTime = currentTime;
            return;
        }
        if (this.lastMovementTime > currentTime) {
            return;
        }
        this.updateLastTickValues();

        Vec3 currentPos = this.position();
        Vec3 currentVelocity = this.getDeltaMovement();
        Vec3 newPos = currentPos.add(currentVelocity.scale(currentTime - this.lastMovementTime));
        Vec3 currentDiff = goal.pos.subtract(currentPos);
        Vec3 newDiff = goal.pos.subtract(newPos);

        if (Math.signum(currentDiff.x) != Math.signum(newDiff.x)) {
            newPos = new Vec3(goal.pos.x, newPos.y, newPos.z);
            currentVelocity = new Vec3(0, currentVelocity.y, currentVelocity.z);
        }
        if (Math.signum(currentDiff.y) != Math.signum(newDiff.y)) {
            newPos = new Vec3(newPos.x, goal.pos.y, newPos.z);
            currentVelocity = new Vec3(currentVelocity.x, 0, currentVelocity.z);
        }
        if (Math.signum(currentDiff.z) != Math.signum(newDiff.z)) {
            newPos = new Vec3(newPos.x, newPos.y, goal.pos.z);
            currentVelocity = new Vec3(currentVelocity.x, currentVelocity.y, 0);
        }

        this.setPos(newPos.x, newPos.y, newPos.z);
        this.setDeltaMovement(currentVelocity);

        // 起点和目标重合时 initialDistance 为 0，直接除会得到 NaN 角度 → 一帧里所有实体都被裁掉
        float factor = this.initialDistance < 1.0E-4
                ? 1.0F
                : 1F - (float) (goal.pos.distanceTo(newPos) / this.initialDistance);
        float newYaw = this.initialYaw + (goal.yaw - this.initialYaw) * factor;
        float newPitch = this.initialPitch + (goal.pitch - this.initialPitch) * factor;
        this.setYRot(newYaw);
        this.setXRot(newPitch);
        this.setYHeadRot(newYaw);
        this.setYBodyRot(newYaw);
        this.yRotO = this.yRotO + (newYaw - this.yRotO) * 0.5F;
        this.xRotO = this.xRotO + (newPitch - this.xRotO) * 0.5F;

        this.lastMovementTime = currentTime;
        if (this.position().equals(goal.pos)) {
            this.updateLastTickValues();
            this.setGoal(null);
            goal.finish(this);
        }
    }

    private void updateLastTickValues() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();

        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();

        this.yHeadRotO = this.yHeadRot;
    }

    public void setGoal(PersistentCameraEntityGoal goal) {
        this.goal = goal;
        this.initialYaw = this.getYRot();
        this.initialPitch = this.getXRot();
        this.lastMovementTime = -1;

        double dX = 0;
        double dY = 0;
        double dZ = 0;
        double duration = 0;
        if (goal != null) {
            Vec3 pos = this.position();
            dX = goal.pos.x - pos.x;
            dY = goal.pos.y - pos.y;
            dZ = goal.pos.z - pos.z;
            duration = goal.duration;
            if (goal.delay > 0) {
                this.lastMovementTime = System.currentTimeMillis() + goal.delay;
            }
        }

        this.initialDistance = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        this.setDeltaMovement(new Vec3(dX, dY, dZ).scale(1.0 / Math.max(1, duration)));
    }

    public PersistentCameraEntityGoal getGoal() {
        return this.goal;
    }

    public static void setup(Minecraft client, PersistentCameraEntityGoal goal) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return;
        }

        clearHandoff();

        PersistentCameraEntity camera;
        if (client.getCameraEntity() instanceof PersistentCameraEntity existing) {
            camera = existing;
        } else {
            camera = cameraFor(client);
            client.setCameraEntity(camera);
        }

        // ★ 记录并隐藏 HUD + 手臂
        hudWasHiddenBeforeCamera = client.gui.hud.isHidden();
        if (!hudWasHiddenBeforeCamera) {
            client.gui.hud.toggle();
        }

        // 复用的相机会停在上一场过场结束的位置，这里重新对齐到玩家作为起点
        camera.snapTo(player);
        camera.setGoal(goal);
    }

    public static void unset(Minecraft client) {
        clearHandoff();

        if (client.getCameraEntity() instanceof PersistentCameraEntity camera) {
            camera.setGoal(null);
            client.setCameraEntity(client.player);

            // ★ 恢复 HUD
            if (!hudWasHiddenBeforeCamera) {
                client.gui.hud.toggle();
            }
        }
    }

    /**
     * 当前是不是「本机玩家自己的第一人称视角」。
     * <p>
     * 过场相机只会接管这种情况下的视角：相机被别的东西接管（旁观其他实体、回放里切换视角）时不接管。
     */
    public static boolean isLocalPlayerFirstPersonView(Minecraft client) {
        if (client.player == null || client.level == null) {
            return false;
        }
        if (client.getCameraEntity() != client.player) {
            return false;
        }
        return client.options.getCameraType().isFirstPerson();
    }

    /**
     * 当前是否处于回放中（Flashback 兼容）。
     * <p>
     * 回放的原理是把录制到的包原样重放一遍，其中也包括本模组的同步响应包；如果放任它触发过场相机，
     * 回放视角就会被拽进意识转移动画（哪怕被录制的那位玩家并没有被附身）。
     * <p>
     * 软依赖：没装 Flashback 时永远返回 false；装了就用它自己的公开 API（反射调用，避免硬依赖）：
     * 优先 {@code Flashback.isInReplay()}，没有就退回 {@code Flashback.getReplayServer() != null}。
     */
    public static boolean isReplayPlaying() {
        if (!FabricLoader.getInstance().isModLoaded("flashback")) {
            return false;
        }
        try {
            Class<?> flashback = Class.forName("com.moulberry.flashback.Flashback");
            try {
                Object inReplay = flashback.getMethod("isInReplay").invoke(null);
                return inReplay instanceof Boolean b && b;
            } catch (NoSuchMethodException ignored) {
                return flashback.getMethod("getReplayServer").invoke(null) != null;
            }
        } catch (Throwable t) {
            // 反射失败（版本不匹配等）按“不在回放”处理，不影响正常游戏
            return false;
        }
    }

    private static void onTick(Minecraft client) {
        // 预热：进世界后就把假人相机造好，免得过场开始时现造而卡一帧
        //（换身体期间不造：跨维度时这一刻世界刚重建，别在这里再叠一次构造开销）
        if (client.player != null && client.level != null && handoffTargetPos == null) {
            cameraFor(client);
        }

        // 换身体期间镜头留在天上，等玩家落位后再交还
        if (tickHandoff(client)) {
            return;
        }

        if (!(client.getCameraEntity() instanceof PersistentCameraEntity camera) || camera.goal == null) {
            return;
        }
        camera.tickMovement();
    }

    static {
        ClientTickEvents.START_CLIENT_TICK.register(PersistentCameraEntity::onTick);
    }
}