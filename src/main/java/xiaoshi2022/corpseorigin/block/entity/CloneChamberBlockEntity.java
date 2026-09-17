package xiaoshi2022.corpseorigin.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.block.CloneChamberBlock;
import xiaoshi2022.corpseorigin.block.FluidKind;
import xiaoshi2022.corpseorigin.clone.CloneState;
import xiaoshi2022.corpseorigin.clone.DoorAnimator;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModBlockEntities;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.shell.*;

import java.nio.charset.StandardCharsets;
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
    /** 就绪后等这么久（tick）再自己走出培养仓，给舱门开合留出动画时间 */
    private static final int AWAKEN_DELAY = 40;
    private int readyTicks;
    private DoorAnimator doorAnimator;
    private int tickCount;
    /** 当前这具身体是否已经写进持久化索引（owner 离线时留到之后重试） */
    private boolean indexed;
    /** 当前这具身体是否已进 owner 的注册表（只在服务端登记，客户端 BE 不许进来） */
    private boolean registered;

    /**
     * 仓内液体的"具体是哪种"。
     * <p>
     * 方块状态的取值表是本模组注册方块那一刻定死的，注册顺序排在本模组之后的模组，它的流体
     * 存不进方块状态（只能记成 {@link xiaoshi2022.corpseorigin.block.FluidKind#OTHER}），
     * 于是把具体流体记在这里——桶、加速倍率、是否尸水都靠它。
     */
    @Nullable
    private Identifier storedFluidId;

    public CloneChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLONE_CHAMBER, pos, state);
        this.doorAnimator = new DoorAnimator(state.getValue(CloneChamberBlock.OPEN));
    }

    public UUID getOwnerUuid() {
        return this.clone != null ? this.clone.getOwner() : null;
    }

    // ==================== TransferredBody ====================

    @Override
    public UUID ownerUuid() {
        return this.clone == null ? null : this.clone.getOwner();
    }

    @Override
    public ShellState snapshot() {
        if (this.clone == null) {
            return null;
        }
        ShellState state = ShellState.blank(
                this.clone.getOwner(),
                this.clone.getOwnerName(),
                this.level != null ? this.level.dimension().identifier() : null,
                this.worldPosition);
        // ★ 稳定标识：同一个克隆仓每次快照都必须返回同一个 UUID，否则 UI 按 UUID 请求转移会匹配不到
        state.setUuid(bodyUuid());
        state.setBody(PlayerBodySnapshot.fromCompoundTag(this.clone.getBody()));
        state.setProgress(this.clone.getProgress());
        // ★ 身体自带的尸兄状态 / 角色数据跟着身体走，不沿用玩家当前的
        state.setComponent(this.clone.getComponent());
        // ★ 盔甲也随身体（客户端渲染克隆人要用）
        state.setEquipment(this.clone.getEquipment());
        return state;
    }

    /** 由维度 + 坐标推导的身体标识：UI 上同一个仓内的身体始终是同一个目标 */
    public UUID bodyUuid() {
        String key = (this.level == null ? "" : this.level.dimension().identifier().toString())
                + "@" + this.worldPosition.asLong();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean ready() {
        return this.clone != null && this.clone.isReady();
    }

    @Override
    public void consume(ServerLevel level) {
        UUID owner = this.ownerUuid();
        ServerLevel chamberLevel = this.ownLevel(level);
        this.unregisterFromRegistry();
        this.clone = null;
        this.setChanged();
        chamberLevel.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        this.unindexBody(chamberLevel, owner);
        refreshOwnerShellStates(chamberLevel, owner);
    }

    @Override
    public void receiveOldBody(ServerLevel level, ShellState oldState) {
        if (oldState.getBody() == null) return;
        ServerLevel chamberLevel = this.ownLevel(level);
        this.clone = new CloneState(
                oldState.getOwnerUuid(),
                oldState.getOwnerName(),
                CloneState.COMPLETE_PROGRESS,
                oldState.getBody().asCompoundTag(),
                oldState.getComponent());
        this.clone.setEquipment(oldState.getEquipment());
        this.registerToRegistry();
        this.setChanged();
        chamberLevel.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        this.indexBody(chamberLevel);
        this.syncBodyCorpseData(chamberLevel);
        refreshOwnerShellStates(chamberLevel, this.ownerUuid());
    }

    /** 方块实体自己所在的维度：跨维度夺舍时调用方传进来的是玩家所在维度，不能用 */
    private ServerLevel ownLevel(ServerLevel fallback) {
        return this.level instanceof ServerLevel serverLevel ? serverLevel : fallback;
    }

    /** 仓内身体变化后，刷新 owner 客户端的可转移身体列表（GUI 用） */
    private static void refreshOwnerShellStates(ServerLevel level, @Nullable UUID owner) {
        if (owner == null || level.getServer() == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player != null) {
            CorpseNetwork.refreshShellStates(player);
        }
    }

    private void registerToRegistry() {
        if (this.registered || this.clone == null || this.clone.getOwner() == null) {
            return;
        }
        REGISTRY.computeIfAbsent(this.clone.getOwner(), k -> new CopyOnWriteArrayList<>()).add(this);
        this.registered = true;
    }

    /** 把仓内身体登记进持久化索引（区块没加载时死亡夺舍也要能找到它） */
    private void indexBody(ServerLevel level) {
        ServerPlayer owner = ownerPlayer(level, this.ownerUuid());
        if (owner == null) {
            return;   // owner 不在线，等它上线后由 tick 重试
        }
        ShellBodyIndex.upsert(owner, new ShellBodyIndex.Entry(
                level.dimension().identifier(), this.worldPosition, null));
        this.indexed = true;
    }

    private void unindexBody(ServerLevel level, @Nullable UUID ownerUuid) {
        this.indexed = false;
        ServerPlayer owner = ownerPlayer(level, ownerUuid);
        if (owner == null) {
            return;
        }
        ShellBodyIndex.remove(owner, new ShellBodyIndex.Entry(
                level.dimension().identifier(), this.worldPosition, null));
    }

    @Nullable
    private static ServerPlayer ownerPlayer(ServerLevel level, @Nullable UUID ownerUuid) {
        if (ownerUuid == null || level.getServer() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    private void unregisterFromRegistry() {
        this.registered = false;
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

        // 每 5 秒补发一次这具身体的尸兄状态：客户端重连、服务器重启后能自愈
        if (this.clone != null && this.tickCount % 100 == 0) {
            this.syncBodyCorpseData(level);
        }

        // ★ 只在服务端登记进注册表（客户端 BE 混进去会让夺舍拿到 ClientLevel 而崩溃）
        if (this.clone != null && !this.registered) {
            this.registerToRegistry();
        }

        // ★ 身体索引：owner 不在线时每秒重试一次，保证老存档里的身体也能补登记
        if (this.clone != null && !this.indexed && this.tickCount % 20 == 0) {
            this.indexBody(level);
        }

        if (this.clone != null && !this.clone.isReady()) {
            // 目标就是这具克隆体自己的完成度（0.80~0.99），不是固定 96%
            float target = this.clone.getCompletion();
            float progress = this.clone.getProgress()
                    + target / GROW_TICKS * this.liquidGrowthMultiplier();
            boolean justReady = progress >= target;
            if (justReady) {
                progress = target;
            }
            this.clone.setProgress(progress);
            changed = true;

            // ★ 必须先把进度写满再通知，否则快照里 isReady() 仍为 false，列表不会包含这具身体
            if (justReady) {
                this.onCloneReady(level);
                // 培育完成会消耗仓内液体，重新读一次状态，别让后面的门逻辑拿旧状态写回去
                state = this.getBlockState();
            }
        }

        // ★ 培育完成后，克隆体自己苏醒、走出培养仓（尸兄克隆体与干净克隆体都会）。
        //   创造模式瞬间放出来的身体、以及夺舍后还回仓里的旧身体不在此列：它们留在仓里等夺舍。
        if (this.clone != null && this.clone.isAutoAwaken() && this.clone.isReady()) {
            this.readyTicks++;
            if (this.readyTicks >= AWAKEN_DELAY) {
                this.awakenClone(level, state);
                state = this.getBlockState();
            }
        } else {
            this.readyTicks = 0;
        }

        // ★ 舱门：玩家进了仓就关门封仓；玩家在门口（外）或克隆体就绪则向外开门；
        //   没人时自动回正（关回默认姿态）。
        boolean shouldOpen = !this.hasPlayerInside(level)
                && (this.hasPlayerNearby(level) || (this.clone != null && this.clone.isReady()));
        if (state.getValue(CloneChamberBlock.OPEN) != shouldOpen) {
            CloneChamberBlock.setOpen(state, level, this.worldPosition, shouldOpen);
        }

        if (changed || this.tickCount % 20 == 0) {
            this.setChanged();
            level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    /** 仓内液体对培育速度的加成（尸水/血水会加速） */
    private float liquidGrowthMultiplier() {
        Fluid stored = this.storedFluid();
        return stored != null
                ? FluidKind.growthMultiplier(stored)
                : this.getBlockState().getValue(CloneChamberBlock.FLUID).growthMultiplier();
    }

    /** 记下仓内液体的具体种类（方块状态存不下的那些用；传 null 表示清空） */
    public void setStoredFluid(@Nullable Fluid fluid) {
        Identifier id = fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY
                ? null
                : BuiltInRegistries.FLUID.getKey(fluid);
        if (java.util.Objects.equals(this.storedFluidId, id)) {
            return;
        }
        this.storedFluidId = id;
        this.setChanged();
    }

    /** 仓内液体的具体种类（方块状态能表达的那种就没记，返回 null 表示"看方块状态"） */
    @Nullable
    public Fluid storedFluid() {
        return this.storedFluidId == null ? null : BuiltInRegistries.FLUID.getValue(this.storedFluidId);
    }

    private void onCloneReady(ServerLevel level) {
        level.playSound(null, this.worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7F, 0.8F);
        this.indexBody(level);
        // ★ 培育完成：仓内液体被这具身体消耗掉
        CloneChamberBlock.setFluid(this.getBlockState(), level, this.worldPosition, FluidKind.NONE);
        if (this.clone != null) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(this.clone.getOwner());
            if (owner != null) {
                owner.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("message.corpseorigin.clone_chamber.ready")));
                CorpseNetwork.refreshShellStates(owner);
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

    /** 玩家是否站在仓内（上/下半格都算）——进仓就关门封仓 */
    private boolean hasPlayerInside(ServerLevel level) {
        BlockPos upper = this.worldPosition.above();
        for (Player player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            BlockPos feet = player.blockPosition();
            if (feet.equals(this.worldPosition) || feet.equals(upper)) {
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

        // ★ 仓里没身体就直接开始培育：分身在外面活动也不影响这座仓复用
        if (this.clone == null) {
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

        if (this.clone.isReady()) {
            return this.activateAvatar(level, state, serverPlayer);
        }

        int percent = Mth.clamp((int) (this.clone.getProgress() / this.clone.getCompletion() * 100.0F), 0, 99);
        this.actionbar(serverPlayer, Component.translatable(
                "message.corpseorigin.clone_chamber.growing", percent));
        return InteractionResult.CONSUME;
    }

    /** 右键激活：立刻让克隆体出仓 */
    private InteractionResult activateAvatar(Level level, BlockState state, ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (this.clone == null) {
            return InteractionResult.PASS;
        }
        this.awakenClone(serverLevel, state);
        this.actionbar(player, Component.translatable("message.corpseorigin.clone_chamber.avatar_activated"));
        return InteractionResult.SUCCESS;
    }

    /**
     * 克隆体苏醒并自己走出培养仓。
     * <p>
     * 右键激活与"培育完成后自动苏醒"共用这段：生成 CloneAvatarEntity 接管这具身体，
     * 之后靠它自己的 AI 走动（尸兄克隆体还会主动攻击）。
     * 这里不能依赖玩家在线，所以只用 owner 的 uuid / 名字。
     */
    private void awakenClone(ServerLevel level, BlockState state) {
        if (this.clone == null) {
            return;
        }
        UUID ownerUuid = this.clone.getOwner();
        String ownerName = this.clone.getOwnerName();
        Direction facing = state.getValue(CloneChamberBlock.FACING);

        // 找一个能站人的位置出仓（正面优先，正面被堵就试其它方向）
        BlockPos spot = this.findAwakenSpot(level, state);
        double x = spot.getX() + 0.5;
        double y = spot.getY();
        double z = spot.getZ() + 0.5;

        // ★ 1. 把仓内身体包成 ShellState（坐标用分身自己的落点，不能借用克隆仓的坐标）
        ShellState bodyState = ShellState.blank(ownerUuid, ownerName,
                level.dimension().identifier(), spot);
        bodyState.setBody(PlayerBodySnapshot.fromCompoundTag(this.clone.getBody()));
        bodyState.setProgress(ShellState.PROGRESS_DONE);
        // ★ 分身带走的也是这具身体自己的尸兄状态 / 角色数据 / 盔甲
        bodyState.setComponent(this.clone.getComponent());
        bodyState.setEquipment(this.clone.getEquipment());

        // ★ 2. 生成分身，把身体交给它（记住出生仓，好让旧身体还得回来）
        CloneAvatarEntity avatar = new CloneAvatarEntity(ModEntities.CLONE_AVATAR, level);
        avatar.setOwnerUuid(ownerUuid);
        // 头顶名字牌显示本体名字，方便分辨哪具分身是谁的
        avatar.setOwnerName(ownerName);
        avatar.setBodyState(bodyState);
        avatar.setSourceChamber(this.worldPosition);
        avatar.setActive(true);
        avatar.setProgress(1.0F);

        avatar.setPos(x, y, z);
        avatar.setYRot(facing.toYRot());
        avatar.setXRot(0.0F);
        avatar.setYHeadRot(facing.toYRot());

        level.addFreshEntity(avatar);

        // ★ 3. 仓内身体清空：分身独立存在，这座仓可以立刻再培育下一具
        this.unindexBody(level, ownerUuid);
        ServerPlayer owner = ownerPlayer(level, ownerUuid);
        if (owner != null) {
            ShellBodyIndex.upsert(owner, new ShellBodyIndex.Entry(
                    level.dimension().identifier(), spot, avatar.getUUID()));
        }
        this.unregisterFromRegistry();
        this.clone = null;
        this.readyTicks = 0;
        this.setChanged();
        // ★ 立刻把"仓空了"下发给客户端，否则客户端还会继续渲染已经走出去的那具克隆体
        level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        if (owner != null) {
            CorpseNetwork.refreshShellStates(owner);
        }
    }

    /** 出仓落点：正面优先，其次左右、背面；都不通就还是正面（交给物理挤出） */
    private BlockPos findAwakenSpot(ServerLevel level, BlockState state) {
        Direction facing = state.getValue(CloneChamberBlock.FACING);
        Direction[] candidates = {
                facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite()
        };
        for (Direction dir : candidates) {
            BlockPos feet = this.worldPosition.relative(dir, 2);
            BlockPos head = feet.above();
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
                return feet;
            }
        }
        return this.worldPosition.relative(facing, 2);
    }

    private InteractionResult startConstruction(Level level, ServerPlayer player) {
        // ★ 生存模式培育需要仓内有培养液（任意液体，液体桶右键注入）
        if (!player.isCreative() && this.getBlockState().getValue(CloneChamberBlock.FLUID).isEmpty()) {
            this.actionbar(player, Component.translatable("message.corpseorigin.clone_chamber.need_liquid"));
            return InteractionResult.CONSUME;
        }

        if (player.getHealth() + player.getAbsorptionAmount() <= 1.0F && !player.isCreative()) {
            this.actionbar(player, Component.translatable("message.corpseorigin.clone_chamber.low_health"));
            return InteractionResult.CONSUME;
        }

        if (!player.isCreative()) {
            player.hurt(level.damageSources().sweetBerryBush(), 1.0F);
        }

        // ★ 培育液决定配方：只有本模组的尸水会把克隆体养成尸兄（继承本体的尸兄特征），
        //   其他模组的血水只加速培育、不改性质；清水养出干净人形
        FluidKind fluid = this.getBlockState().getValue(CloneChamberBlock.FLUID);
        Fluid storedFluid = this.storedFluid();
        boolean corpseClone = storedFluid != null
                ? FluidKind.isCorpseWater(storedFluid)
                : fluid.isCorpseWater();
        net.minecraft.util.RandomSource random = level.getRandom();
        // 尸水长得快但更容易长歪；清水慢但更完整。完成度同时就是"继承度"
        float completion = corpseClone
                ? 0.80F + random.nextFloat() * 0.16F
                : 0.90F + random.nextFloat() * 0.09F;

        // ★ 从本体复制状态，然后按继承度裁掉一部分（记忆/特征缺失、也可能变异）
        ShellStateComponent components = ShellStateComponent.of(player);
        CorpseShellStateComponent corpseComp = components.as(CorpseShellStateComponent.class);
        if (corpseComp != null) {
            corpseComp.applyCloneFormula(random, corpseClone, completion);
        }
        CharacterShellStateComponent charComp = components.as(CharacterShellStateComponent.class);
        if (charComp != null) {
            charComp.applyCloneFormula(random, completion);
        }
        ApsShellStateComponent apsComp = components.as(ApsShellStateComponent.class);
        if (apsComp != null) {
            apsComp.applyCloneFormula(random, completion);
        }

        // ★ 空白身体
        ShellState blankBody = ShellState.blank(player, this.worldPosition);
        CompoundTag body = blankBody.getBody().asCompoundTag();
        float startProgress = player.isCreative() ? completion : 0.0F;
        this.clone = new CloneState(player.getUUID(), player.getScoreboardName(), startProgress, body,
                components);
        this.clone.setCompletion(completion);
        // ★ 只有从零培育出来的身体成熟后会自己走出来；创造模式放下的留在仓里等夺舍
        this.clone.setAutoAwaken(!player.isCreative());
        this.registerToRegistry();
        if (level instanceof ServerLevel serverLevel) {
            this.indexBody(serverLevel);
            this.syncBodyCorpseData(serverLevel);
            if (startProgress >= CloneState.COMPLETE_PROGRESS) {
                // 创造模式一上来就是培育完成，培养液同样要被消耗掉
                CloneChamberBlock.setFluid(this.getBlockState(), serverLevel, this.worldPosition, FluidKind.NONE);
            }
        }

        this.actionbar(player, Component.translatable("message.corpseorigin.clone_chamber.started"));
        if (this.liquidGrowthMultiplier() > 1.0F) {
            player.sendOverlayMessage(Component.translatable("message.corpseorigin.clone_chamber.liquid_boost"));
        }
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        CorpseNetwork.refreshShellStates(player);
        return InteractionResult.SUCCESS;
    }

    private void actionbar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }

    // ==================== 客户端/比较器访问 ====================

    /**
     * 舱门开合进度。
     * <p>
     * 只有下半格才有 ticker（见 {@link #getTicker}），上半格的动画器不会 {@code step}，
     * 所以上半格统一取下半格的进度，保证上下两截门动画同步。
     */
    public float getDoorOpenProgress(float partialTick) {
        if (!CloneChamberBlock.isLower(this.getBlockState()) && this.level != null
                && this.level.getBlockEntity(this.worldPosition.below()) instanceof CloneChamberBlockEntity lower) {
            return lower.getDoorOpenProgress(partialTick);
        }
        return this.doorAnimator.getProgress(partialTick);
    }

    public float getCloneProgress() {
        return this.clone == null ? 0.0F : this.clone.getProgress() / this.clone.getCompletion();
    }

    /** 这具克隆体自己的完成度（渲染用它判断是否已"成熟"） */
    public float getCloneCompletion() {
        return this.clone == null ? CloneState.COMPLETE_PROGRESS : this.clone.getCompletion();
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
        if (this.storedFluidId != null) {
            out.putString("StoredFluid", this.storedFluidId.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.clone = in.child("Clone").map(CloneState::read).orElse(null);
        this.storedFluidId = in.getString("StoredFluid")
                .map(Identifier::tryParse)
                .orElse(null);
        // ★ 这里不要在服务端之外登记：客户端读同一份 NBT 会把 ClientLevel 上的仓塞进 REGISTRY，
        //   夺舍时强转 ServerLevel 就会崩。登记统一放到服务端 tick 里做。
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        // ★ 仓内液体的具体种类也要下发：客户端渲染器要靠它画液面
        if (this.storedFluidId != null) {
            tag.putString("StoredFluid", this.storedFluidId.toString());
        }
        if (this.clone != null) {
            CompoundTag cloneTag = new CompoundTag();
            cloneTag.putString("Owner", this.clone.getOwner().toString());
            if (this.clone.getOwnerName() != null) {
                cloneTag.putString("OwnerName", this.clone.getOwnerName());
            }
            cloneTag.putFloat("Progress", this.clone.getProgress());
            // ★ 这具克隆体自己的完成度：客户端渲染靠它判断是否已"成熟"
            cloneTag.putFloat("Completion", this.clone.getCompletion());
            // ★ 盔甲也要下发，否则客户端画的仓内克隆人光着
            if (!this.clone.getEquipment().isEmpty()) {
                var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                        net.minecraft.util.ProblemReporter.DISCARDING, registries);
                out.store("Equipment", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.listOf(),
                        this.clone.getEquipment());
                CompoundTag extra = out.buildResult();
                for (String key : extra.keySet()) {
                    cloneTag.put(key, extra.get(key));
                }
            }
            tag.put("Clone", cloneTag);
        }
        return tag;
    }

    /** 供渲染取用的盔甲（顺序：头/胸/腿/脚） */
    public List<net.minecraft.world.item.ItemStack> getEquipment() {
        return this.clone == null ? List.of() : this.clone.getEquipment();
    }

    /**
     * 把这具身体自己的尸兄状态发出去。
     * <p>
     * 客户端按"身体 uuid"缓存，于是仓内克隆人的外骨骼/多眼按这具身体的状态渲染，
     * 而不是沿用账号当前那份。
     * <p>
     * ★ 广播给同维度所有玩家：仓里的克隆人不只 owner 看得见，只发给 owner 别人会看不到外骨骼。
     */
    private void syncBodyCorpseData(ServerLevel level) {
        if (this.clone == null) {
            return;
        }
        CorpseNetwork.broadcastBodyCorpseSync(level, ownerPlayer(level, this.ownerUuid()),
                this.bodyUuid(), ShellState.corpseTagOf(this.clone.getComponent()));
    }
}