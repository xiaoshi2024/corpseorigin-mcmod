package xiaoshi2022.corpseorigin.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
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

    /** 服务端持有的完整身体快照，不参与实体同步（走自定义包或 BE 数据） */
    @Nullable
    private ShellState bodyState;

    public CloneAvatarEntity(EntityType<? extends CloneAvatarEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, "");
        builder.define(DATA_ACTIVE, false);
        builder.define(DATA_PROGRESS, 0.0F);
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
    }

    /** 从玩家创建一份快照，作为这具分身的"可转移身体" */
    public void captureFrom(ServerPlayer player) {
        this.setOwnerUuid(player.getUUID());
        this.bodyState = ShellState.of(player, this.blockPosition());
        this.setActive(true);
        this.setProgress(1.0F);
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
        this.bodyState = in.child("BodyState").map(ShellState::read).orElse(null);
    }

    // ==================== 服务端 tick ====================

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.isActive()) {
            // 未来：分身的 AI 行为、跟随 owner、被 sync 选中时的处理
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