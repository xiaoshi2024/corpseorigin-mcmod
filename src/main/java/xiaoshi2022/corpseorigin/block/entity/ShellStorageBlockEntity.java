package xiaoshi2022.corpseorigin.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.registry.ModBlockEntities;
import xiaoshi2022.corpseorigin.shell.ShellState;
import xiaoshi2022.corpseorigin.shell.TransferredBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 存储仓：静态存放一具完整的玩家身体。
 * <p>
 * 和 CloneAvatarEntity（移动备用身体）一起，都是 TransferredBody 的实现者。
 * 玩家完全意识转移时，旧身体写进这里，新身体从这里取走。
 */
public class ShellStorageBlockEntity extends BlockEntity implements TransferredBody {

    /** 按 owner 索引所有已存身体的存储仓（仅服务端维护） */
    public static final Map<UUID, List<ShellStorageBlockEntity>> REGISTRY =
            new ConcurrentHashMap<>();

    @Nullable
    private ShellState storedState;

    public ShellStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHELL_STORAGE, pos, state);
    }

    // ==================== TransferredBody ====================

    @Override
    public UUID ownerUuid() {
        return this.storedState == null ? null : this.storedState.getOwnerUuid();
    }

    @Override
    public ShellState snapshot() {
        return this.storedState;
    }

    @Override
    public boolean ready() {
        return this.storedState != null && this.storedState.isReady();
    }

    @Override
    public void consume(ServerLevel level) {
        this.unregisterFromRegistry();
        this.storedState = null;
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    // ==================== 存取 ====================

    @Nullable
    public ShellState getStoredState() {
        return this.storedState;
    }

    public void setStoredState(@Nullable ShellState state) {
        // 客户端不维护 REGISTRY
        if (this.level != null && this.level.isClientSide()) {
            this.storedState = state;
            return;
        }

        this.unregisterFromRegistry();

        this.storedState = state;
        this.setChanged();

        if (state != null && state.getOwnerUuid() != null) {
            REGISTRY.computeIfAbsent(state.getOwnerUuid(), k -> new CopyOnWriteArrayList<>())
                    .add(this);
        }

        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean isEmpty() {
        return this.storedState == null;
    }

    private void unregisterFromRegistry() {
        if (this.storedState == null) {
            return;
        }
        UUID owner = this.storedState.getOwnerUuid();
        if (owner == null) {
            return;
        }
        List<ShellStorageBlockEntity> list = REGISTRY.get(owner);
        if (list != null) {
            list.remove(this);
            if (list.isEmpty()) {
                REGISTRY.remove(owner);
            }
        }
    }

    // ==================== 生命周期 ====================

    @Override
    public void setRemoved() {
        this.unregisterFromRegistry();
        super.setRemoved();
    }

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (this.storedState != null) {
            this.storedState.writeTo(out.child("StoredState"));
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.storedState = in.child("StoredState").map(ShellState::read).orElse(null);

        // 读盘后重新注册进 REGISTRY（服务端）
        if (this.storedState != null
                && this.storedState.getOwnerUuid() != null
                && this.level != null
                && !this.level.isClientSide()) {
            REGISTRY.computeIfAbsent(this.storedState.getOwnerUuid(),
                            k -> new CopyOnWriteArrayList<>())
                    .add(this);
        }
    }

    @Override
    public void receiveOldBody(ServerLevel level, ShellState oldState) {
        this.setStoredState(oldState);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (this.storedState != null && this.storedState.getOwnerUuid() != null) {
            CompoundTag stateTag = new CompoundTag();
            // 只同步渲染需要的轻量信息
            stateTag.putString("Owner", this.storedState.getOwnerUuid().toString());
            stateTag.putFloat("Progress", this.storedState.getProgress());
            tag.put("StoredState", stateTag);
        }
        return tag;
    }
}