package xiaoshi2022.corpseorigin.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.block.CloneChamberBlock;
import xiaoshi2022.corpseorigin.clone.CloneState;
import xiaoshi2022.corpseorigin.clone.DoorAnimator;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.registry.ModBlockEntities;
import xiaoshi2022.corpseorigin.registry.ModEntities;

import java.util.UUID;

public class CloneChamberBlockEntity extends BlockEntity {
    public static final int GROW_TICKS = 6000;

    @Nullable
    private CloneState clone;
    private DoorAnimator doorAnimator;
    private int tickCount;

    /** 分身实体 UUID；不为 null 表示分身已激活 */
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

        // 1. 克隆体培育
        if (this.clone != null && !this.clone.isReady()) {
            float progress = this.clone.getProgress() + CloneState.COMPLETE_PROGRESS / GROW_TICKS;
            if (progress >= CloneState.COMPLETE_PROGRESS) {
                progress = CloneState.COMPLETE_PROGRESS;
                this.onCloneReady(level);
            }
            this.clone.setProgress(progress);
            changed = true;
        }

        // 2. 舱门自动打开
        boolean shouldOpen = (this.clone != null && this.clone.isReady()) || this.hasPlayerNearby(level);
        if (state.getValue(CloneChamberBlock.OPEN) != shouldOpen) {
            CloneChamberBlock.setOpen(state, level, this.worldPosition, shouldOpen);
        }

        // 3. 同步给客户端
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

        if (this.clone == null) {
            return this.startConstruction(level, serverPlayer);
        }

        // 只有供体本人能使用自己的克隆仓
        if (!this.clone.getOwner().equals(player.getUUID())) {
            this.actionbar(serverPlayer, Component.translatable(
                    "message.corpseorigin.clone_chamber.wrong_owner",
                    Component.literal(this.clone.getOwnerName() == null ? "?" : this.clone.getOwnerName())));
            return InteractionResult.CONSUME;
        }

        // 潜行右键：意识转换（原来的"完全转移"，先保留）
        if (player.isSecondaryUseActive() && this.clone.isReady()) {
            this.transferConsciousness(level, state, serverPlayer);
            return InteractionResult.SUCCESS;
        }

        // 普通右键 + 已培育完成 + 未激活分身 → 激活分身
        if (this.clone.isReady() && !this.isAvatarActive()) {
            return this.activateAvatar(level, state, serverPlayer);
        }

        // 其余情况：显示进度/提示
        if (this.isAvatarActive()) {
            this.actionbar(serverPlayer, Component.translatable("message.corpseorigin.clone_chamber.avatar_active"));
            return InteractionResult.CONSUME;
        }

        int percent = Mth.clamp((int) (this.clone.getProgress() / CloneState.COMPLETE_PROGRESS * 100.0F), 0, 96);
        this.actionbar(serverPlayer, Component.translatable(
                "message.corpseorigin.clone_chamber.growing", percent));
        return InteractionResult.CONSUME;
    }

    /** 激活分身：生成 CloneAvatarEntity，隐藏 BER 里的假人 */
    private InteractionResult activateAvatar(Level level, BlockState state, ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        Direction facing = state.getValue(CloneChamberBlock.FACING);

        // 1. 生成分身实体
        CloneAvatarEntity avatar = new CloneAvatarEntity(ModEntities.CLONE_AVATAR, serverLevel);
        avatar.setOwnerUuid(player.getUUID());

        double x = this.worldPosition.getX() + 0.5 + facing.getStepX() * 1.5;
        double y = this.worldPosition.getY();
        double z = this.worldPosition.getZ() + 0.5 + facing.getStepZ() * 1.5;

        avatar.setPos(x, y, z);
        avatar.setYRot(facing.toYRot());
        avatar.setXRot(0.0F);
        avatar.setYHeadRot(facing.toYRot());

        serverLevel.addFreshEntity(avatar);

        // 2. 记录 UUID → BER 不再渲染假人
        this.setAvatarEntityUuid(avatar.getUUID());
        this.setChanged();
        if (level instanceof ServerLevel sl) {
            sl.getChunkSource().blockChanged(this.worldPosition);   // ★ 强制重发 BE 数据
        }

        // 3. 通知玩家
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

        CompoundTag body = this.snapshotPlayer(player);
        float startProgress = player.isCreative() ? CloneState.COMPLETE_PROGRESS : 0.0F;
        this.clone = new CloneState(player.getUUID(), player.getScoreboardName(), startProgress, body);

        this.actionbar(player, Component.translatable("message.corpseorigin.clone_chamber.started"));
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }

    private void transferConsciousness(Level level, BlockState state, ServerPlayer player) {
        CompoundTag oldBody = this.snapshotPlayer(player);
        CompoundTag newBody = this.clone.getBody();

        Direction facing = state.getValue(CloneChamberBlock.FACING);
        double x = this.worldPosition.getX() + 0.5 + facing.getStepX() * 1.1;
        double y = this.worldPosition.getY();
        double z = this.worldPosition.getZ() + 0.5 + facing.getStepZ() * 1.1;
        float yaw = facing.toYRot();

        this.applyPlayerSnapshot(player, newBody, x, y, z, yaw);

        this.clone.setBody(oldBody);
        this.clone.setProgress(CloneState.COMPLETE_PROGRESS);

        level.playSound(null, this.worldPosition, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.actionbar(player, Component.translatable("message.corpseorigin.clone_chamber.swapped"));

        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
    }

    // ==================== 玩家身体快照 / 应用 ====================

    private CompoundTag snapshotPlayer(ServerPlayer player) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        player.saveWithoutId(output);
        return output.buildResult();
    }

    private void applyPlayerSnapshot(ServerPlayer player, CompoundTag snapshot, double x, double y, double z, float yaw) {
        CompoundTag tag = snapshot.copy();
        tag.remove("RootVehicle");
        tag.remove("Passengers");

        ListTag pos = new ListTag();
        pos.addTag(0, DoubleTag.valueOf(x));
        pos.addTag(1, DoubleTag.valueOf(y));
        pos.addTag(2, DoubleTag.valueOf(z));
        tag.put("Pos", pos);
        tag.putString("Dimension", player.level().dimension().identifier().toString());

        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, player.level().registryAccess(), tag);
        player.load(input);

        player.connection.teleport(x, y, z, yaw, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        player.onUpdateAbilities();
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
        if (this.clone == null) {
            return 0;
        }
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