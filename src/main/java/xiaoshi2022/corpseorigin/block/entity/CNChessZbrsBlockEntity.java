package xiaoshi2022.corpseorigin.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.registry.ModBlockEntities;
import xiaoshi2022.corpseorigin.registry.ModBlocks;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 象棋尸兄方块实体。
 * <p>
 * 本质是"可以让人站在背上"的方块尸兄，拥有实体式属性：
 * <ul>
 *   <li>{@code idle}：循环待机，尖刺轻微开合；</li>
 *   <li>{@code put_death}：处决，尖刺快速夹合，把背上的人夹碎；</li>
 *   <li>{@code die}：死亡，整体散开消失，播完后移除方块。</li>
 * </ul>
 * <p>
 * 【战斗】方块本体不可挖掘（基岩级硬度），击杀只能通过 HP（{@link #hurt}）：
 * 近战（ServerPlayerGameMode mixin）、弹射物（Projectile mixin）、爆炸（wasExploded）、
 * 技能直接调 API。HP 归零播 die、掉落自身方块物品。
 * <p>
 * 【行动】{@link #movePiece} 走子：校验目标格（有乘客时还要求上方净空）→ 方块整体搬迁
 * （HP 等状态保留），模型从旧位置平滑滑到新位置（渲染偏移，无需动画文件配合），
 * 站在它背上的生物按同一条缓动曲线一起滑过去（保持相对位置）。
 * <p>
 * 【GeckoLib 5.5.5 说明】{@code AnimationController} 的回调参数是
 * {@code AnimationTest}，不是旧的 {@code AnimationState}；返回类型也不再是
 * {@code PlayState}，而是直接 {@code test.setAndContinue(...)}。
 */
public class CNChessZbrsBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation PUT_DEATH = RawAnimation.begin().thenPlay("put_death");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlay("die");

    /** 处决伤害占最大生命值的比例：1.0 = 100%，满血也一击毙命 */
    private static final float FATAL_HEALTH_PERCENT = 1.0F;

    /** 免疫即死的进化等级门槛：绝对等级 6 = 地2（地2及以上不被处决致命） */
    private static final int FATAL_EXEMPT_LEVEL = 6;

    /** 免疫即死者只受的普通伤害（仙人掌类型，8 点） */
    private static final float NON_FATAL_DAMAGE = 8.0F;

    /** 默认最大生命值：40（玩家满血 20 的两倍） */
    private static final float DEFAULT_MAX_HEALTH = 40.0F;

    /** 受击无敌帧（tick，同实体） */
    private static final int INVULN_TICKS = 10;

    /** 方块调度间隔（tick） */
    private static final int TICK_STEP = 4;

    /** 走子动画：每格耗时与最小时长（tick） */
    private static final int MOVE_TICKS_PER_BLOCK = 6;
    private static final int MOVE_MIN_TICKS = 4;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 当前手动触发的动画：null = 走 idle */
    private RawAnimation forcedAnimation;
    /** 处决/死亡动画播完后是否要移除方块 */
    private boolean removeAfterAnimation;
    /** 剩余播放 tick（只用于服务端判断何时移除方块 / 何时恢复 idle） */
    private int animationTicks;

    // ==================== 实体式战斗属性 ====================

    /** 当前生命值 */
    private float health = DEFAULT_MAX_HEALTH;
    /** 受击无敌帧剩余 */
    private int invulnTicks;
    /** 走子冷却剩余 */
    private int moveCooldown;

    // ==================== 走子动画 ====================

    /** 起点相对新位置的偏移（格），以及滑动时长（tick） */
    private double moveOffsetX, moveOffsetY, moveOffsetZ;
    private int moveDuration;
    /** 以下两项仅客户端使用 */
    private long moveStartGameTime;
    private boolean moveAnimActive;

    public CNChessZbrsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CN_CHESS_ZBRS, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CNChessZbrsBlockEntity>("main", 0,
                test -> test.setAndContinue(this.forcedAnimation != null ? this.forcedAnimation : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== 战斗：受伤 / 死亡 ====================

    /**
     * 让这个"方块尸兄"受伤。语义对齐 {@link LivingEntity#hurt}：
     * <ul>
     *   <li>正在播死亡/处决动画时不再结算；</li>
     *   <li>无敌帧内免伤（同实体 invulnerableTime）；</li>
     *   <li>HP 归零：掉落自身方块物品，播 die，播完方块消失。</li>
     * </ul>
     *
     * @return 是否真正结算了伤害
     */
    public boolean hurt(ServerLevel level, DamageSource source, float amount) {
        if (amount <= 0.0F || this.forcedAnimation != null) {
            return false;
        }
        if (this.invulnTicks > 0) {
            return false;
        }

        this.invulnTicks = INVULN_TICKS;
        this.health -= amount;
        level.playSound(null, this.worldPosition, SoundEvents.WARDEN_HURT,
                SoundSource.BLOCKS, 1.0F, 0.8F);
        this.setChanged();
        this.sync();

        if (this.health <= 0.0F) {
            this.health = 0.0F;
            // 击杀掉落：放一个自身方块物品，可以重新放置
            Block.popResource(level, this.worldPosition,
                    new ItemStack(ModBlocks.CN_CHESS_ZBRS));
            this.playDie();
        }
        return true;
    }

    // ==================== 行动：走子 ====================

    /**
     * 让象棋尸兄移动到目标格（服务端搬迁）。
     * <p>
     * 校验：不在处决/死亡动画中、走子冷却已结束、目标格在世界内、
     * 目标为空气或可替换方块且无碰撞箱（不能挤进别的方块/实体占位）。
     * <p>
     * 搬迁时 HP 等全部状态原样保留；模型从旧位置平滑滑到新位置，
     * 站在它<b>背上</b>的生物按同一条缓动曲线一起滑过去（保持相对位置、不结算摔落伤害）；
     * 目标格上方放不下这些生物时，整次走子会被拒绝。
     *
     * @return 是否移动成功
     */
    public boolean movePiece(ServerLevel level, BlockPos destination) {
        if (this.forcedAnimation != null || this.moveCooldown > 0) {
            return false;
        }
        if (destination.equals(this.worldPosition) || !level.isInWorldBounds(destination)) {
            return false;
        }
        BlockState destState = level.getBlockState(destination);
        if (!destState.isAir() && !destState.canBeReplaced()) {
            return false;
        }
        if (!destState.getCollisionShape(level, destination).isEmpty()) {
            return false;
        }

        BlockPos oldPos = this.worldPosition;
        BlockState selfState = this.getBlockState();

        // 0. 先记下"站在它背上"的生物（含各自相对方块的局部偏移）
        BackRiders back = ridersOnBack(level, oldPos);
        double dx = destination.getX() - oldPos.getX();
        double dy = destination.getY() - oldPos.getY();
        double dz = destination.getZ() - oldPos.getZ();
        // 0.5 有乘客的话，目标格上方也得放得下它们 —— 否则这次走子直接不成立，
        //     不能把背上的人挤进墙里
        if (!ridersFit(level, back, dx, dy, dz)) {
            return false;
        }

        // 1. 在新位置放方块（新方块实体）
        level.setBlock(destination, selfState, Block.UPDATE_CLIENTS);
        if (!(level.getBlockEntity(destination) instanceof CNChessZbrsBlockEntity moved)) {
            level.removeBlock(destination, false);
            return false;
        }

        // 2. 状态搬迁 + 滑动动画
        moved.copyRuntimeFrom(this);
        moved.beginMoveAnim(level, oldPos, destination);

        // 3. 删掉旧方块（不掉落）
        level.removeBlock(oldPos, false);

        // 4. 背上的生物跟着模型一起滑过去（每 tick 由 registerCarryTick 推进）
        if (!back.isEmpty()) {
            CARRIED_MOVES.add(new CarriedMove(back, oldPos, destination, moved.moveDuration));
        }
        return true;
    }

    /**
     * "站在它背上"的生物，以及它们各自相对方块原点的局部偏移（格）。
     * <p>
     * 判定与 {@link #crushVictims} 同一套：脚底与本方块同格。
     * 骑在别人身上的会由它自己的载具带走，这里跳过，免得被搬两次。
     */
    private record BackRiders(List<LivingEntity> riders, List<Vec3> local) {
        boolean isEmpty() {
            return riders.isEmpty();
        }
    }

    private static BackRiders ridersOnBack(ServerLevel level, BlockPos pos) {
        AABB cell = new AABB(
                pos.getX(),       pos.getY(),       pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0
        );
        List<LivingEntity> riders = new ArrayList<>();
        List<Vec3> local = new ArrayList<>();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, cell)) {
            if (!living.isAlive() || living.isPassenger()) {
                continue;
            }
            // 再确认一次：脚底确实与本方块同高，避免只是身体探进来
            double feetY = living.getY();
            if (feetY < pos.getY() || feetY >= pos.getY() + 1.0) {
                continue;
            }
            riders.add(living);
            local.add(living.position().subtract(pos.getX(), pos.getY(), pos.getZ()));
        }
        return new BackRiders(riders, local);
    }

    /**
     * 有乘客时：把它们按这次位移挪到目标格，检查每只的碰撞箱都放得下。
     * <p>
     * 用各乘客自己的碰撞箱（而不是拍脑袋的固定高度），高矮胖瘦都能算准。
     */
    private static boolean ridersFit(ServerLevel level, BackRiders back, double dx, double dy, double dz) {
        for (LivingEntity living : back.riders()) {
            if (!level.noCollision(living, living.getBoundingBox().move(dx, dy, dz))) {
                return false;
            }
        }
        return true;
    }

    // ==================== 走子：背上的生物随模型滑动 ====================

    /**
     * 正在"背着生物滑行"的走子（运行时状态，不持久化）。
     * <p>
     * 挂在服务端全局 tick 上、而不是方块自己的 4 tick 调度：乘客每 4 tick 才挪一下的话，
     * 模型是每帧插值滑过去的，看着就不像"一起滑"。
     */
    private static final List<CarriedMove> CARRIED_MOVES = new ArrayList<>();

    /** 注册每 tick 推进乘客滑动的回调（由服务端事件注册处调用一次）。 */
    public static void registerCarryTick() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!CARRIED_MOVES.isEmpty()) {
                CARRIED_MOVES.removeIf(CarriedMove::advance);
            }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> CARRIED_MOVES.clear());
    }

    /**
     * 一次"背着生物走子"的滑动：每 tick 把乘客摆到模型当前应在的位置上。
     * <p>
     * 位置 = 终点 + (起点 − 终点) × 剩余比例 + 乘客的局部偏移，
     * 其中剩余比例来自 {@link #remainingFactor}——与客户端画模型用的是同一条曲线，
     * 所以乘客和模型是重叠着滑过去的。
     */
    private static final class CarriedMove {
        private final BackRiders back;
        /** 方块起点相对终点的偏移（格） */
        private final double offsetX, offsetY, offsetZ;
        private final double toX, toY, toZ;
        private final int duration;
        private int elapsed;

        CarriedMove(BackRiders back, BlockPos from, BlockPos to, int duration) {
            this.back = back;
            this.offsetX = from.getX() - to.getX();
            this.offsetY = from.getY() - to.getY();
            this.offsetZ = from.getZ() - to.getZ();
            this.toX = to.getX();
            this.toY = to.getY();
            this.toZ = to.getZ();
            this.duration = Math.max(1, duration);
        }

        /** @return true 表示滑完了，可以从列表里丢掉 */
        boolean advance() {
            float factor = remainingFactor(this.elapsed / (float) this.duration);
            List<LivingEntity> riders = this.back.riders();
            List<Vec3> local = this.back.local();
            for (int i = 0; i < riders.size(); i++) {
                LivingEntity living = riders.get(i);
                if (!living.isAlive() || living.isRemoved()) {
                    continue;
                }
                Vec3 offset = local.get(i);
                living.teleportTo(
                        this.toX + this.offsetX * factor + offset.x,
                        this.toY + this.offsetY * factor + offset.y,
                        this.toZ + this.offsetZ * factor + offset.z);
                // 滑动不是"掉下来"，不该结算摔落伤害
                living.resetFallDistance();
            }
            return ++this.elapsed > this.duration;
        }
    }

    /**
     * smoothstep 缓动后的"剩余比例"：1 = 还在起点，0 = 已经到终点。
     * <p>
     * 客户端画模型偏移（{@link #getRenderOffset}）与服务端搬乘客都用它，
     * 保证两边算的是同一条曲线。
     */
    private static float remainingFactor(float progress) {
        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        float eased = clamped * clamped * (3.0F - 2.0F * clamped);
        return 1.0F - eased;
    }

    /** 搬迁时继承战斗状态（moveCooldown 由 beginMoveAnim 重新设定）。 */
    private void copyRuntimeFrom(CNChessZbrsBlockEntity old) {
        this.health = old.health;
        this.invulnTicks = old.invulnTicks;
    }

    /** 在新位置启动滑动动画：偏移 = 旧位置 − 新位置（格），时长按距离算。 */
    private void beginMoveAnim(ServerLevel level, BlockPos oldPos, BlockPos newPos) {
        this.moveOffsetX = oldPos.getX() - newPos.getX();
        this.moveOffsetY = oldPos.getY() - newPos.getY();
        this.moveOffsetZ = oldPos.getZ() - newPos.getZ();

        double distance = Math.sqrt(
                this.moveOffsetX * this.moveOffsetX
                + this.moveOffsetY * this.moveOffsetY
                + this.moveOffsetZ * this.moveOffsetZ);
        this.moveDuration = Math.max(MOVE_MIN_TICKS,
                Mth.ceil(distance * MOVE_TICKS_PER_BLOCK));
        this.moveCooldown = this.moveDuration;
        this.setChanged();
        this.sync();
    }

    /**
     * 客户端渲染偏移（格）：动画进度 0→1，偏移从起点插值到 0。
     *
     * @param gameTime 客户端当前游戏时间（tick）
     */
    public Vec3 getRenderOffset(long gameTime, float partialTick) {
        if (!this.moveAnimActive) {
            return Vec3.ZERO;
        }
        float progress = (gameTime - this.moveStartGameTime + partialTick) / this.moveDuration;
        if (progress >= 1.0F) {
            this.moveAnimActive = false;
            return Vec3.ZERO;
        }
        // smoothstep 缓动：起步/收尾慢，中段快（与服务端搬乘客共用 remainingFactor）
        float factor = remainingFactor(progress);
        return new Vec3(
                this.moveOffsetX * factor,
                this.moveOffsetY * factor,
                this.moveOffsetZ * factor);
    }

    // ==================== 动画触发 ====================

    /** 处决：播 put_death，并立刻夹碎背上的人（百分比致命伤害） */
    public void playPutDeath() {
        this.forcedAnimation = PUT_DEATH;
        this.removeAfterAnimation = false;
        this.animationTicks = 32;   // put_death 长度 1.6s ≈ 32 tick
        this.setChanged();
        this.sync();
        // 立刻处决：不等动画走到中段，触发当下就造成致命伤害
        if (this.level instanceof ServerLevel serverLevel) {
            this.crushVictims(serverLevel, this.worldPosition);
        }
    }

    /** 死亡：播 die，播完移除方块 */
    public void playDie() {
        this.forcedAnimation = DIE;
        this.removeAfterAnimation = true;
        this.animationTicks = 24;   // die 长度 1.2s ≈ 24 tick
        this.setChanged();
        this.sync();
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    // ==================== 服务端 tick ====================

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.invulnTicks > 0) {
            this.invulnTicks = Math.max(0, this.invulnTicks - TICK_STEP);
        }
        if (this.moveCooldown > 0) {
            this.moveCooldown = Math.max(0, this.moveCooldown - TICK_STEP);
            if (this.moveCooldown == 0) {
                // 滑动结束，清掉瞬时动画字段，避免以后被重复同步
                this.moveDuration = 0;
                this.moveOffsetX = this.moveOffsetY = this.moveOffsetZ = 0.0;
            }
        }

        if (this.forcedAnimation == null) {
            return;
        }

        if (this.animationTicks > 0) {
            this.animationTicks--;
            return;
        }

        // 动画播完
        if (this.removeAfterAnimation) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else {
            // put_death 播完回到 idle
            this.forcedAnimation = null;
            this.setChanged();
            this.sync();
        }
    }

    /**
     * 尖刺夹碎：只对"站在它背上"的活体立刻造成致命伤害。
     * <p>
     * 普通目标：伤害按最大生命值的百分比（FATAL_HEALTH_PERCENT = 100%）计算，
     * 血量多少都一击毙命；genericKill 类型（同 /kill）绕过护甲、附魔和无敌帧。
     * <p>
     * 例外（见 {@link #isExecutionExempt}）：尸王、地2以上玩家不吃致命伤，只受 8 点普通伤害。
     * <p>
     * 判定条件：
     * <ul>
     *   <li>生物脚底与本方块处于同一格高度（pos.y &le; y &lt; pos.y + 1）；</li>
     *   <li>生物水平位置落在方块水平范围内（x/z 在 pos ~ pos+1 之间）。</li>
     * </ul>
     * 尸兄也夹：同类相残，不做任何例外。
     */
    private void crushVictims(ServerLevel level, BlockPos pos) {
        // 站在平板上的生物，脚底与方块同高（平板顶在 +0.25）
        AABB box = new AABB(
                pos.getX(),       pos.getY(),       pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0
        );

        boolean hitAny = false;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!living.isAlive()) {
                continue;
            }
            // 再确认一次：脚底确实与本方块同高，避免只是身体探进来
            double feetY = living.getY();
            if (feetY < pos.getY() || feetY >= pos.getY() + 1.0) {
                continue;
            }
            if (isExecutionExempt(living)) {
                // 尸王 / 地2以上：处决不致命，只受普通伤害
                living.hurt(level.damageSources().cactus(), NON_FATAL_DAMAGE);
            } else {
                // 百分比致命伤害：100% 最大生命值；再加上吸收值，金心也挡不住处决
                float fatalDamage = living.getMaxHealth() * FATAL_HEALTH_PERCENT
                        + living.getAbsorptionAmount();
                living.hurt(level.damageSources().genericKill(), fatalDamage);
            }
            hitAny = true;
        }

        if (hitAny) {
            level.playSound(null, pos, SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.BLOCKS, 1.2F, 0.6F);
        }
    }

    /**
     * 该目标是否免疫处决的致命伤害。
     * <ul>
     *   <li>尸王（龙右身体，含龙右克隆分身）：{@link ZombieKin#isZombieKing}；</li>
     *   <li>玩家绝对进化等级 &ge; 地2（6 级，按进化点数换算）。</li>
     * </ul>
     * 普通尸兄生物等级上限 5 级（人级），到不了地2，不在此列。
     */
    private static boolean isExecutionExempt(LivingEntity living) {
        if (ZombieKin.isZombieKing(living)) {
            return true;
        }
        if (living instanceof ServerPlayer player) {
            PlayerCharacterData data = PlayerCharacterData.get(player);
            int evolutionLevel = EvolutionManager.getLevel(data.getEarnedPoints(player.getUUID()));
            return evolutionLevel >= FATAL_EXEMPT_LEVEL;
        }
        return false;
    }

    // ==================== 同步包 ====================

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        // 默认实现返回 null（sendBlockUpdated 只会发方块状态、不发 tag）。
        // 改成方块实体数据包，getUpdateTag 里的 HP/动画/走子字段才会同步。
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putFloat("Health", this.health);
        if (this.forcedAnimation == PUT_DEATH) {
            tag.putString("Anim", "put_death");
        } else if (this.forcedAnimation == DIE) {
            tag.putString("Anim", "die");
        }
        if (this.moveDuration > 0
                && (this.moveOffsetX != 0.0 || this.moveOffsetY != 0.0 || this.moveOffsetZ != 0.0)) {
            tag.putByte("MoveStart", (byte) 1);
            tag.putDouble("MoveOx", this.moveOffsetX);
            tag.putDouble("MoveOy", this.moveOffsetY);
            tag.putDouble("MoveOz", this.moveOffsetZ);
            tag.putInt("MoveDur", this.moveDuration);
        }
        return tag;
    }

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putFloat("Health", this.health);
        // 只有"正在死亡"需要存：读档后继续移除；put_death 不存，读档回 idle 即可
        out.putBoolean("Dying", this.forcedAnimation == DIE);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.health = in.getFloatOr("Health", DEFAULT_MAX_HEALTH);
        // 同步包里的动画状态：客户端立刻切到对应动画
        String anim = in.getStringOr("Anim", "");
        switch (anim) {
            case "put_death" -> {
                this.forcedAnimation = PUT_DEATH;
                this.removeAfterAnimation = false;
                this.animationTicks = 32;
            }
            case "die" -> {
                this.forcedAnimation = DIE;
                this.removeAfterAnimation = true;
                this.animationTicks = 24;
            }
            default -> { }
        }
        if (in.getBooleanOr("Dying", false)) {
            this.forcedAnimation = DIE;
            this.removeAfterAnimation = true;
            this.animationTicks = 24;
        }
        // 走子动画只走同步包、不写磁盘；在客户端记录起始时间做插值
        if (in.getBooleanOr("MoveStart", false)
                && this.level != null && this.level.isClientSide()) {
            this.moveOffsetX = in.getDoubleOr("MoveOx", 0.0);
            this.moveOffsetY = in.getDoubleOr("MoveOy", 0.0);
            this.moveOffsetZ = in.getDoubleOr("MoveOz", 0.0);
            this.moveDuration = Math.max(1, in.getIntOr("MoveDur", MOVE_MIN_TICKS));
            this.moveStartGameTime = this.level.getGameTime();
            this.moveAnimActive = true;
        }
    }
}
