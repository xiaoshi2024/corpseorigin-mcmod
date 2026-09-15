package xiaoshi2022.corpseorigin.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.shell.ShellBodyIndex;
import xiaoshi2022.corpseorigin.shell.ShellState;
import xiaoshi2022.corpseorigin.shell.TransferredBody;

import java.util.UUID;

/**
 * 移动备用克隆身体。
 * <p>
 * 由克隆仓培育完成后激活生成，持有玩家的一份完整状态快照 {@link #bodyState}。
 * 玩家可以：
 * <ul>
 *   <li>部分意识转移：分身按自己的 AI 行动（未来扩展）；</li>
 *   <li>完全意识转移：玩家死亡或主动使用存储仓时，把 bodyState apply 到 ServerPlayer。</li>
 * </ul>
 */
public class CloneAvatarEntity extends PathfinderMob implements TransferredBody {

    /** 同步到客户端的 owner UUID */
    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.STRING);

    /** 同步：是否已激活（激活后 BER 不再渲染假人） */
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.BOOLEAN);

    /** 同步：培育进度，用于渲染 */
    private static final EntityDataAccessor<Float> DATA_PROGRESS =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.FLOAT);

    /** 同步：这具身体是不是尸兄克隆体（尸水培育出来的），决定 AI 是否像低阶尸兄 */
    private static final EntityDataAccessor<Boolean> DATA_CORPSE_CLONE =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.BOOLEAN);

    /** 同步：这具身体穿的盔甲（头/胸/腿/脚），客户端渲染要用 */
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_HEAD_EQUIPMENT =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_CHEST_EQUIPMENT =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_LEGS_EQUIPMENT =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DATA_FEET_EQUIPMENT =
            SynchedEntityData.defineId(CloneAvatarEntity.class, EntityDataSerializers.ITEM_STACK);

    /** 服务端持有的完整身体快照，不参与实体同步（走自定义包或 BE 数据） */
    @Nullable
    private ShellState bodyState;

    /** 派生出这具分身的克隆仓（位置）：夺舍后旧身体要还回那座仓，才能来回换 */
    @Nullable
    private BlockPos sourceChamberPos;

    public CloneAvatarEntity(EntityType<? extends CloneAvatarEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, "");
        builder.define(DATA_ACTIVE, false);
        builder.define(DATA_PROGRESS, 0.0F);
        builder.define(DATA_CORPSE_CLONE, false);
        builder.define(DATA_HEAD_EQUIPMENT, net.minecraft.world.item.ItemStack.EMPTY);
        builder.define(DATA_CHEST_EQUIPMENT, net.minecraft.world.item.ItemStack.EMPTY);
        builder.define(DATA_LEGS_EQUIPMENT, net.minecraft.world.item.ItemStack.EMPTY);
        builder.define(DATA_FEET_EQUIPMENT, net.minecraft.world.item.ItemStack.EMPTY);
    }

    // ==================== 同步数据访问 ====================

    public void setOwnerUuid(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, uuid == null ? "" : uuid.toString());
    }

    @Nullable
    public UUID getOwnerUuid() {
        String s = this.entityData.get(DATA_OWNER_UUID);
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setActive(boolean active) {
        this.entityData.set(DATA_ACTIVE, active);
    }

    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }

    public void setProgress(float progress) {
        this.entityData.set(DATA_PROGRESS, progress);
    }

    public float getProgress() {
        return this.entityData.get(DATA_PROGRESS);
    }

    // ==================== 身体快照 ====================

    @Nullable
    public ShellState getBodyState() {
        return this.bodyState;
    }

    public void setBodyState(@Nullable ShellState bodyState) {
        this.bodyState = bodyState;
        this.syncEquipment(bodyState);
        this.syncBodyCorpseData();
    }

    /**
     * 把这具身体自己的尸兄状态发出去。
     * <p>
     * 客户端按分身 uuid 缓存，于是分身的外骨骼/多眼按它自己那份状态渲染，
     * 而不是沿用账号当前那份。
     * <p>
     * ★ 广播给同维度所有玩家：分身别人也看得见，只发给 owner 别人会看不到外骨骼。
     */
    private void syncBodyCorpseData() {
        if (this.level().isClientSide() || this.bodyState == null) {
            return;
        }
        net.minecraft.nbt.CompoundTag tag =
                xiaoshi2022.corpseorigin.shell.ShellState.corpseTagOf(this.bodyState.getComponent());
        // 尸水培育出来的克隆体是尸兄：行为也按低阶尸兄走
        this.entityData.set(DATA_CORPSE_CLONE,
                tag != null && tag.getBoolean("is_corpse").orElse(false));

        if (this.level() instanceof ServerLevel level) {
            CorpseNetwork.broadcastBodyCorpseSync(level, this.ownerPlayer(), this.getUUID(), tag);
        }
    }

    /** 把身体自带的盔甲同步给客户端（渲染克隆人时用） */
    private void syncEquipment(@Nullable ShellState bodyState) {
        java.util.List<net.minecraft.world.item.ItemStack> equipment =
                bodyState == null ? java.util.List.of() : bodyState.getEquipment();
        this.entityData.set(DATA_HEAD_EQUIPMENT, equipmentAt(equipment, 0));
        this.entityData.set(DATA_CHEST_EQUIPMENT, equipmentAt(equipment, 1));
        this.entityData.set(DATA_LEGS_EQUIPMENT, equipmentAt(equipment, 2));
        this.entityData.set(DATA_FEET_EQUIPMENT, equipmentAt(equipment, 3));
    }

    private static net.minecraft.world.item.ItemStack equipmentAt(
            java.util.List<net.minecraft.world.item.ItemStack> equipment, int index) {
        return index < equipment.size() ? equipment.get(index) : net.minecraft.world.item.ItemStack.EMPTY;
    }

    /** 客户端：取出这具身体穿的盔甲（顺序：头/胸/腿/脚） */
    public java.util.List<net.minecraft.world.item.ItemStack> clientEquipment() {
        return java.util.List.of(
                this.entityData.get(DATA_HEAD_EQUIPMENT),
                this.entityData.get(DATA_CHEST_EQUIPMENT),
                this.entityData.get(DATA_LEGS_EQUIPMENT),
                this.entityData.get(DATA_FEET_EQUIPMENT));
    }

    public void setSourceChamber(@Nullable BlockPos pos) {
        this.sourceChamberPos = pos;
    }

    /** 取出生它的克隆仓（在分身自己所在维度里按需加载；仓没了返回 null） */
    @Nullable
    public CloneChamberBlockEntity sourceChamber() {
        if (this.sourceChamberPos == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        level.getChunkAt(this.sourceChamberPos);
        return level.getBlockEntity(this.sourceChamberPos) instanceof CloneChamberBlockEntity chamber
                ? chamber : null;
    }

    /** 从玩家创建一份快照，作为这具分身的"可转移身体" */
    public void captureFrom(ServerPlayer player) {
        this.setOwnerUuid(player.getUUID());
        this.bodyState = ShellState.of(player, this.blockPosition());
        this.setActive(true);
        this.setProgress(1.0F);
    }

    // ==================== 基础行为 ====================

    /**
     * 基础 AI：会浮水、随机漫步（避开水）、看向附近的玩家、原地张望。
     * <p>
     * 攻击与索敌目标在这里一并注册，但是用 {@link #isCorpseClone()} 门控：
     * 清水培育出的干净人形保持被动，尸水培育出的尸兄克隆体才会像低阶尸兄那样主动攻击。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new CorpseMeleeGoal());
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // 索敌同样只在尸兄克隆体上启用
        this.targetSelector.addGoal(0, new CorpseRetaliateGoal());
        this.targetSelector.addGoal(1, new CorpseTargetGoal());
    }

    /** 这具身体是不是尸兄克隆体（尸水培育出来的） */
    public boolean isCorpseClone() {
        return this.entityData.get(DATA_CORPSE_CLONE);
    }

    // ---- 下面三个目标只在尸兄克隆体上生效，干净人形克隆体保持被动 ----

    private final class CorpseMeleeGoal extends MeleeAttackGoal {
        CorpseMeleeGoal() { super(CloneAvatarEntity.this, 1.2D, true); }

        @Override
        public boolean canUse() { return isCorpseClone() && super.canUse(); }

        @Override
        public boolean canContinueToUse() { return isCorpseClone() && super.canContinueToUse(); }
    }

    private final class CorpseRetaliateGoal extends HurtByTargetGoal {
        CorpseRetaliateGoal() { super(CloneAvatarEntity.this); }

        @Override
        public boolean canUse() { return isCorpseClone() && super.canUse(); }
    }

    private final class CorpseTargetGoal extends NearestAttackableTargetGoal<Player> {
        CorpseTargetGoal() { super(CloneAvatarEntity.this, Player.class, true); }

        @Override
        public boolean canUse() { return isCorpseClone() && super.canUse(); }

        @Override
        public boolean canContinueToUse() { return isCorpseClone() && super.canContinueToUse(); }
    }

    /**
     * 备用身体不按距离自然消失。
     * <p>
     * 默认的 Mob 会在附近没有玩家时被清理掉，那样玩家走远一次就丢了这具身体。
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ==================== 属性 ====================

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    // ==================== 持久化 ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        UUID owner = this.getOwnerUuid();
        if (owner != null) {
            out.putString("Owner", owner.toString());
        }
        out.putBoolean("Active", this.isActive());
        out.putFloat("Progress", this.getProgress());
        if (this.sourceChamberPos != null) {
            out.putLong("SourceChamber", this.sourceChamberPos.asLong());
        }
        if (this.bodyState != null) {
            this.bodyState.writeTo(out.child("BodyState"));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        in.getString("Owner").ifPresent(s -> {
            try {
                this.setOwnerUuid(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
            }
        });
        this.setActive(in.getBooleanOr("Active", false));
        this.setProgress(in.getFloatOr("Progress", 0.0F));
        this.sourceChamberPos = in.getLong("SourceChamber").isPresent()
                ? BlockPos.of(in.getLongOr("SourceChamber", 0L)) : null;
        this.bodyState = in.child("BodyState").map(ShellState::read).orElse(null);
    }

    // ==================== 消失处理 ====================

    /**
     * 分身消失（被夺舍 / 死亡 / 丢弃）后：从身体索引里注销，并刷新 owner 的列表。
     */
    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel level)) return;

        ServerPlayer owner = this.ownerPlayer();
        if (owner != null) {
            ShellBodyIndex.remove(owner, new ShellBodyIndex.Entry(
                    level.dimension().identifier(), this.blockPosition(), this.getUUID()));
            CorpseNetwork.refreshShellStates(owner);
        }
    }

    @Nullable
    private ServerPlayer ownerPlayer() {
        UUID ownerUuid = this.getOwnerUuid();
        if (ownerUuid == null || !(this.level() instanceof ServerLevel level) || level.getServer() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    // ==================== 服务端 tick ====================

    /** 最近一次写进身体索引的区块：分身跨区块时更新索引坐标，区块卸载后也能被按需加载找到 */
    private long indexedChunk = Long.MIN_VALUE;

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !this.isActive()) {
            return;
        }

        // 每 5 秒补发一次尸兄状态（客户端重连后自愈）
        if (this.tickCount % 100 == 0) {
            this.syncBodyCorpseData();
        }

        BlockPos pos = this.blockPosition();
        long chunk = ChunkPos.pack(pos);
        if (chunk == this.indexedChunk) {
            return;
        }
        this.indexedChunk = chunk;

        ServerPlayer owner = this.ownerPlayer();
        if (owner != null) {
            ShellBodyIndex.upsert(owner, new ShellBodyIndex.Entry(
                    this.level().dimension().identifier(), pos, this.getUUID()));
        }
    }

    // ==================== 作为转移目标 ====================

    /**
     * 把这具分身的身体应用给玩家（完全意识转移）。
     * 具体 apply 逻辑放在 ServerShell 里，这里只提供数据。
     */
    public boolean canBeTransferred() {
        return this.isActive() && this.bodyState != null;
    }

    /**
     * 转移完成后，分身实体本身应该被移除（身体已被玩家占用）。
     */
    public void consumeOnTransfer(ServerLevel level) {
        this.bodyState = null;
        this.discard();
    }

    @Override
    public UUID ownerUuid() {
        return this.getOwnerUuid();
    }

    @Override
    public ShellState snapshot() {
        return this.bodyState;
    }

    @Override
    public boolean ready() {
        return this.canBeTransferred();
    }

    @Override
    public void consume(ServerLevel level) {
        this.consumeOnTransfer(level);
    }
}