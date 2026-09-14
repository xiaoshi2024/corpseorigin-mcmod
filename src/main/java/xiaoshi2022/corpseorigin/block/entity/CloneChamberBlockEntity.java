package xiaoshi2022.corpseorigin.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.block.CloneChamberBlock;
import xiaoshi2022.corpseorigin.clone.CloneState;
import xiaoshi2022.corpseorigin.clone.DoorAnimator;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.registry.ModBlockEntities;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.shell.PlayerBodySnapshot;
import xiaoshi2022.corpseorigin.shell.ShellState;
import xiaoshi2022.corpseorigin.shell.TransferredBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CloneChamberBlockEntity extends BlockEntity implements TransferredBody {
    public static final int GROW_TICKS = 6000;

    /** 按 owner 索引所有克隆仓 */
    public static final Map<UUID, List<CloneChamberBlockEntity>> REGISTRY = new ConcurrentHashMap<>();

    @Nullable
    private CloneState clone;
    private DoorAnimator doorAnimator;
    private int tickCount;

    @Nullable
    private UUID avatarEntityUuid;

    public CloneChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLONE_CHAMBER, pos, state);
        this.doorAnimator = new DoorAnimator(state.getValue(CloneChamberBlock.OPEN));
    }

    public UUID getOwnerUuid() {
        return this.clone != null ? this.clone.getOwner() : null;
    }

    public boolean isAvatarActive() {
        return this.avatarEntityUuid != null;
    }

    public void setAvatarEntityUuid(@Nullable UUID uuid) {
        this.avatarEntityUuid = uuid;
        this.setChanged();
    }

    // ==================== TransferredBody ====================

    @Override
    public UUID ownerUuid() {
        return this.clone == null ? null : this.clone.getOwner();
    }

    @Override
    public ShellState snapshot() {
        if (this.clone == null || this.avatarEntityUuid != null) {
            return null;
        }
        ShellState state = ShellState.blank(
                this.clone.getOwner(),
                this.clone.getOwnerName(),
                this.level != null ? this.level.dimension().registry() : null,
                this.worldPosition);
        state.setBody(PlayerBodySnapshot.fromCompoundTag(this.clone.getBody()));
        state.setProgress(this.clone.getProgress());
        return state;
    }

    @Override
    public boolean ready() {
        return this.clone != null && this.clone.isReady() && this.avatarEntityUuid == null;
    }

    @Override
    public void consume(ServerLevel level) {
        this.unregisterFromRegistry();
        this.clone = null;
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    public void receiveOldBody(ServerLevel level, ShellState oldState) {
        if (oldState.getBody() == null) return;
        this.clone = new CloneState(
                oldState.getOwnerUuid(),
                oldState.getOwnerName(),
                CloneState.COMPLETE_PROGRESS,
                oldState.getBody().asCompoundTag());
        this.registerToRegistry();
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    private void registerToRegistry() {
        if (this.clone != null && this.clone.getOwner() != null) {
            REGISTRY.computeIfAbsent(this.clone.getOwner(), k -> new CopyOnWriteArrayList<>()).add(this);
        }
    }

    private void unregisterFromRegistry() {
        if (this.clone != null && this.clone.getOwner() != null) {
            List<CloneChamberBlockEntity> list = REGISTRY.get(this.clone.getOwner());
            if (list != null) {
                list.remove(this);
                if (list.isEmpty()) REGISTRY.remove(this.clone.getOwner());
            }
        }
    }

    @Override
    public void setRemoved() {
        this.unregisterFromRegistry();
        super.setRemoved();
    }

    // ==================== Tick ====================

    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CloneChamberBlockEntity chamber) {
        if (level.isClientSide()) {
            chamber.doorAnimator.setValue(state.getValue(CloneChamberBlock.OPEN));
            chamber.doorAnimator.step();
        } else if (level instanceof ServerLevel serverLevel) {
            chamber.serverTick(serverLevel, state);
        }
    }

    private void serverTick(ServerLevel level, BlockState state) {
        this.tickCount++;
        boolean changed = false;

        if (this.clone != null && !this.clone.isReady()) {
            float progress = this.clone.getProgress() + CloneState.COMPLETE_PROGRESS / GROW_TICKS;
            if (progress >= CloneState.COMPLETE_PROGRESS) {
                progress = CloneState.COMPLETE_PROGRESS;
                this.onCloneReady(level);
            }
            this.clone.setProgress(progress);
            changed = true;
        }

        boolean shouldOpen = (this.clone != null && this.clone.isReady()) || this.hasPlayerNearby(level);
        if (state.getValue(CloneChamberBlock.OPEN) != shouldOpen) {
            CloneChamberBlock.setOpen(state, level, this.worldPosition, shouldOpen);
        }

        if (changed || this.tickCount % 20 == 0) {
            this.setChanged();
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    private void onCloneReady(ServerLevel level) {
        level.playSound(null, this.worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7F, 0.8F);
        if (this.clone != null) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(this.clone.getOwner());
            if (owner != null) {
                owner.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("message.corpseorigin.clone_chamber.ready")));
            }
        }
    }

    private boolean hasPlayerNearby(ServerLevel level) {
        if (!CloneChamberBlock.isLower(this.getBlockState())) {
            return false;
        }
        AABB box = new AABB(this.worldPosition).inflate(0.35, 0.0, 0.35).expandTowards(0.0, 1.0, 0.0);
        for (Player player : level.players()) {
            if (player.getBoundingBox().intersects(box) && !player.isSpectator()) {
                return true;
            }
        }
        return false;
    }

    // ==================== 玩家交互 ====================

    public InteractionResult useByPlayer(Level level, BlockState state, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        // ★ clone 为 null 时，如果分身已激活，提示；否则开始培育
        if (this.clone == null) {
            if (this.isAvatarActive()) {
                this.actionbar(serverPlayer, Component.translatable(
                        "message.corpseorigin.clone_chamber.avatar_active"));
                return InteractionResult.CONSUME;
            }
            return this.startConstruction(level, serverPlayer);
        }

        if (!this.clone.getOwner().equals(player.getUUID())) {
            this.actionbar(serverPlayer, Component.translatable(
                    "message.corpseorigin.clone_chamber.wrong_owner",
                    Component.literal(this.clone.getOwnerName() == null ? "?" : this.clone.getOwnerName())));
            return InteractionResult.CONSUME;
        }

        // 潜行右键：走 ServerShell.sync（夺舍）
        if (player.isSecondaryUseActive() && this.clone.isReady()) {
            xiaoshi2022.corpseorigin.shell.ServerShell.of(serverPlayer).sync(this);
            return InteractionResult.SUCCESS;
        }

        if (this.clone.isReady() && !this.isAvatarActive()) {
            return this.activateAvatar(level, state, serverPlayer);
        }

        if (this.isAvatarActive()) {
            this.actionbar(serverPlayer, Component.translatable("message.corpseorigin.clone_chamber.avatar_active"));
            return InteractionResult.CONSUME;
        }

        int percent = Mth.clamp((int) (this.clone.getProgress() / CloneState.COMPLETE_PROGRESS * 100.0F), 0, 96);
        this.actionbar(serverPlayer, Component.translatable(
                "message.corpseorigin.clone_chamber.growing", percent));
        return InteractionResult.CONSUME;
    }

    /** 激活分身：生成 CloneAvatarEntity，把身体交给分身，仓内清空 */
    private InteractionResult activateAvatar(Level level, BlockState state, ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        Direction facing = state.getValue(CloneChamberBlock.FACING);

        // ★ 1. 把仓内身体包成 ShellState
        ShellState bodyState = ShellState.blank(player, this.worldPosition);
        bodyState.setBody(PlayerBodySnapshot.fromCompoundTag(this.clone.getBody()));
        bodyState.setProgress(ShellState.PROGRESS_DONE);

        // ★ 2. 生成分身，把身体交给它
        CloneAvatarEntity avatar = new CloneAvatarEntity(ModEntities.CLONE_AVATAR, serverLevel);
        avatar.setOwnerUuid(player.getUUID());
        avatar.setBodyState(bodyState);
        avatar.setActive(true);
        avatar.setProgress(1.0F);

        double x = this.worldPosition.getX() + 0.5 + facing.getStepX() * 1.5;
        double y = this.worldPosition.getY();
        double z = this.worldPosition.getZ() + 0.5 + facing.getStepZ() * 1.5;

        avatar.setPos(x, y, z);
        avatar.setYRot(facing.toYRot());
        avatar.setXRot(0.0F);
        avatar.setYHeadRot(facing.toYRot());

        serverLevel.addFreshEntity(avatar);

        // ★ 3. 记录 UUID，仓内身体清空
        this.setAvatarEntityUuid(avatar.getUUID());
        this.unregisterFromRegistry();
        this.clone = null;
        this.setChanged();
        serverLevel.getChunkSource().blockChanged(this.worldPosition);

        this.actionbar(player, Component.translatable("message.corpseorigin.clone_chamber.avatar_activated"));
        return InteractionResult.SUCCESS;
    }

    private InteractionResult startConstruction(Level level, ServerPlayer player) {
        if (player.getHealth() + player.getAbsorptionAmount() <= 1.0F && !player.isCreative()) {
            this.actionbar(player, Component.translatable("message.corpseorigin.clone_chamber.low_health"));
            return InteractionResult.CONSUME;
        }

        if (!player.isCreative()) {
            player.hurt(level.damageSources().sweetBerryBush(), 1.0F);
        }

        // ★ 空白身体
        ShellState blankBody = ShellState.blank(player, this.worldPosition);
        CompoundTag body = blankBody.getBody().asCompoundTag();
        float startProgress = player.isCreative() ? CloneState.COMPLETE_PROGRESS : 0.0F;
        this.clone = new CloneState(player.getUUID(), player.getScoreboardName(), startProgress, body);
        this.registerToRegistry();

        this.actionbar(player, Component.translatable("message.corpseorigin.clone_chamber.started"));
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }

    private void actionbar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }

    // ==================== 客户端/比较器访问 ====================

    public float getDoorOpenProgress(float partialTick) {
        return this.doorAnimator.getProgress(partialTick);
    }

    public float getCloneProgress() {
        return this.clone == null ? 0.0F : this.clone.getProgress() / CloneState.COMPLETE_PROGRESS;
    }

    public boolean hasClone() {
        return this.clone != null;
    }

    public int getComparatorOutput() {
        if (this.clone == null) return 0;
        int output = (int) (this.clone.getProgress() / CloneState.COMPLETE_PROGRESS * 15.0F);
        return Mth.clamp(output, this.clone.isReady() ? 1 : 0, 15);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (this.clone != null) {
            this.clone.writeTo(out.child("Clone"));
        }
        if (this.avatarEntityUuid != null) {
            out.putString("AvatarEntity", this.avatarEntityUuid.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.clone = in.child("Clone").map(CloneState::read).orElse(null);
        String avatarUuid = in.getStringOr("AvatarEntity", "");
        this.avatarEntityUuid = avatarUuid.isEmpty() ? null : UUID.fromString(avatarUuid);
        if (this.clone != null && this.avatarEntityUuid == null) {
            this.registerToRegistry();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (this.clone != null) {
            CompoundTag cloneTag = new CompoundTag();
            cloneTag.putString("Owner", this.clone.getOwner().toString());
            if (this.clone.getOwnerName() != null) {
                cloneTag.putString("OwnerName", this.clone.getOwnerName());
            }
            cloneTag.putFloat("Progress", this.clone.getProgress());
            tag.put("Clone", cloneTag);
        }
        if (this.avatarEntityUuid != null) {
            tag.putString("AvatarEntity", this.avatarEntityUuid.toString());
        }
        return tag;
    }
}