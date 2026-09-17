package xiaoshi2022.corpseorigin.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.block.CloneChamberBlock;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.shell.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerShellMixin implements ServerShell {

    @Override
    public Stream<TransferredBody> getAvailableBodies() {
        ServerPlayer self = (ServerPlayer) (Object) this;
        MinecraftServer server = self.level().getServer();
        if (server == null) {
            return Stream.empty();
        }

        List<TransferredBody> bodies = new ArrayList<>();

        // 1. 移动分身
        for (ServerLevel level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof CloneAvatarEntity avatar
                        && self.getUUID().equals(avatar.getOwnerUuid())
                        && avatar.ready()) {
                    bodies.add(avatar);
                }
            }
        }

        // 2. 克隆仓
        for (CloneChamberBlockEntity chamber : CloneChamberBlockEntity.REGISTRY
                .getOrDefault(self.getUUID(), List.of())) {
            if (chamber.ready()) {
                bodies.add(chamber);
            }
        }

        return bodies.stream();
    }

    @Override
    public Either<ShellState, String> sync(TransferredBody target) {
        return this.sync(target, false);
    }

    @Override
    public Either<ShellState, String> syncFromDeath(TransferredBody target) {
        return this.sync(target, true);
    }

    private Either<ShellState, String> sync(TransferredBody target, boolean fromDeath) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (target == null || !target.ready()) {
            return Either.right("目标身体尚未就绪");
        }
        if (!self.getUUID().equals(target.ownerUuid())) {
            return Either.right("这不是你的身体");
        }

        ShellState targetState = target.snapshot();
        if (targetState == null) {
            return Either.right("目标身体没有快照");
        }

        if (target instanceof CloneAvatarEntity avatar) {
            targetState.setPos(avatar.blockPosition());
            targetState.setWorld(avatar.level().dimension().identifier());
        }

        ServerLevel level = (ServerLevel) self.level();
        BlockPos currentPos = self.blockPosition();
        ShellState oldBody = ShellState.of(self, currentPos);

        // 取身体之前先记下"这具身体原本放在哪"，好把旧身体还回同一座仓
        // （分身可能在世界另一头甚至别的维度，交给它自己按所在维度去找出生仓）
        CloneChamberBlockEntity sourceChamber =
                target instanceof CloneChamberBlockEntity chamber ? chamber : null;
        CloneChamberBlockEntity avatarChamber =
                target instanceof CloneAvatarEntity avatar ? avatar.sourceChamber() : null;

        // ★ 取出目标身体（方块容器会被清空，分身实体被移除）
        target.consume(level);

        // ★ 旧身体的去处：
        //   ① 身边的空仓（手动/死亡都可用）
        //   ② 手动转移时再兜底到"刚被取走身体的那座仓" / "分身的出生仓"，好来回换
        //   死亡夺舍不兜底：附近没有仓就直接丢弃（掉落物品），不会把尸体塞到远处的仓里
        CloneChamberBlockEntity container = nearbyEmptyChamber(self);
        if (!fromDeath) {
            if (container == null && sourceChamber != null && !sourceChamber.hasClone()) {
                container = sourceChamber;
            }
            if (container == null && avatarChamber != null && !avatarChamber.hasClone()) {
                container = avatarChamber;
            }
        }

        if (container != null) {
            container.receiveOldBody(level, oldBody);
            self.sendOverlayMessage(Component.translatable("message.corpseorigin.clone_chamber.body_stored"));
        } else if (!self.isSpectator()) {
            dropInventory(self);
            self.sendOverlayMessage(Component.translatable("message.corpseorigin.clone_chamber.body_dropped"));
        }

        this.apply(targetState);
        return Either.left(oldBody);
    }

    /**
     * 玩家身边（脚下 + 1.5 格内）的**空**克隆仓，用于回收旧身体。
     * <p>
     * 必须扫世界方块：REGISTRY 只登记"有身体"的仓，空仓不在里面，查它永远查不到。
     */
    @Nullable
    private static CloneChamberBlockEntity nearbyEmptyChamber(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos feet = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        CloneChamberBlockEntity best = null;
        double bestDist = 2.25;   // 1.5 格

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    pos.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
                    CloneChamberBlockEntity chamber = chamberAt(level, pos);
                    if (chamber == null || chamber.hasClone()) {
                        continue;
                    }
                    double dist = feet.distSqr(chamber.getBlockPos());
                    if (dist <= bestDist) {
                        bestDist = dist;
                        best = chamber;
                    }
                }
            }
        }
        return best;
    }

    /** 该位置上的克隆仓方块实体（上/下半格都认，返回下半格那个） */
    @Nullable
    private static CloneChamberBlockEntity chamberAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CloneChamberBlock)) {
            return null;
        }
        BlockPos lowerPos = CloneChamberBlock.isLower(state) ? pos : pos.below();
        return level.getBlockEntity(lowerPos) instanceof CloneChamberBlockEntity chamber ? chamber : null;
    }

    private static void dropInventory(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                player.drop(stack, true, false);
                inventory.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        player.giveExperiencePoints(-player.totalExperience);
    }

    @Override
    public void apply(ShellState state) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (state == null) return;

        // ★ 身体不携带游戏模式：先记住玩家当前模式，载入身体后必须还原。
        //   身体 NBT 里没有 playerGameType 时，原版 load 会回退到服务器默认模式（如创造）。
        GameType gameType = self.gameMode();

        self.stopRiding();
        self.clearFire();
        self.removeAllEffects();
        self.setDeltaMovement(Vec3.ZERO);

        // ★ 组件：把这具身体自带的尸兄状态 / 角色数据 / 技能状态覆盖到玩家身上
        state.getComponent().applyTo(self);
        // 角色变了要重新同步给客户端，否则客户端的角色与技能树还是旧的
        CorpseNetwork.sendCharacterSync(self,
                PlayerCharacterData.get(self).getCharacterId(self.getUUID()));
        // ★ 进化点数 / 进化等级（由点数换算）/ 击杀数 / 已学技能也跟着身体走：
        //   补一次同步，否则客户端 HUD 与技能树还停在上一具身体的数据上
        CorpseNetwork.sendEvolutionSync(self);
        // ★ 这具身体的尸兄外观（含尸兄进化等级 1-5）跟上一具不一定一样，广播刷新
        CorpseNetwork.broadcastPlayerCorpseSync(self);

        // 传送
        if (state.getPos() != null) {
            ServerLevel targetLevel = null;
            if (state.getWorld() != null) {
                MinecraftServer server = self.level().getServer();
                if (server != null) {
                    targetLevel = server.getLevel(
                            ResourceKey.create(Registries.DIMENSION, state.getWorld()));
                }
            }
            if (targetLevel != null && targetLevel != self.level()) {
                self.teleportTo(targetLevel,
                        state.getPos().getX() + 0.5, state.getPos().getY(), state.getPos().getZ() + 0.5,
                        Set.of(), self.getYRot(), self.getXRot(), false);
            } else {
                self.teleportTo(state.getPos().getX() + 0.5, state.getPos().getY(), state.getPos().getZ() + 0.5);
            }
        }

        // 背包：从 state.body 里的 Inventory 加载
        CompoundTag body = state.getBody() == null ? new CompoundTag()
                : state.getBody().asCompoundTag().copy();
        body.remove("UUID");
        body.remove("Pos");
        body.remove("Dimension");
        body.remove("Rotation");
        body.remove("EnderItems");   // 末影箱不跟身体
        body.remove("RootVehicle");
        body.remove("Passengers");

        ValueInput input = TagValueInput.create(
                ProblemReporter.DISCARDING,
                self.level().registryAccess(),
                body);
        self.load(input);

        // ★ 还原玩家自己的游戏模式，生存/创造不随身体走
        if (self.gameMode() != gameType) {
            self.setGameMode(gameType);
        }

        if (self.getHealth() <= 0.0F) {
            self.setHealth(1.0F);
        }
        self.deathTime = 0;

        self.inventoryMenu.broadcastChanges();
        self.containerMenu.broadcastChanges();
        self.onUpdateAbilities();
    }

    @Override
    public TransferredBody findNearestBody() {
        ServerPlayer self = (ServerPlayer) (Object) this;
        MinecraftServer server = self.level().getServer();
        if (server == null) return null;

        ServerLevel selfLevel = (ServerLevel) self.level();

        List<BodyCandidate> sameDim = new ArrayList<>();   // 同维度（按距离优先）
        List<BodyCandidate> otherDim = new ArrayList<>();  // 其他维度

        // ---- 1. 内存里能直接看到的身体 ----
        for (ServerLevel level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof CloneAvatarEntity avatar
                        && self.getUUID().equals(avatar.getOwnerUuid())
                        && avatar.ready()) {
                    BodyCandidate candidate = new BodyCandidate(
                            level, avatar.blockPosition(), avatar, null);
                    (level == selfLevel ? sameDim : otherDim).add(candidate);
                }
            }
        }
        for (CloneChamberBlockEntity chamber : CloneChamberBlockEntity.REGISTRY
                .getOrDefault(self.getUUID(), List.of())) {
            if (!chamber.ready() || chamber.getLevel() == null) continue;
            BodyCandidate candidate = new BodyCandidate(
                    (ServerLevel) chamber.getLevel(), chamber.getBlockPos(), chamber, null);
            (chamber.getLevel() == selfLevel ? sameDim : otherDim).add(candidate);
        }

        // ---- 2. 持久化索引：区块没加载的身体也要算进来（稍后按需加载） ----
        List<ShellBodyIndex.Entry> stale = new ArrayList<>();
        for (ShellBodyIndex.Entry entry : ShellBodyIndex.read(self)) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, entry.dim()));
            if (level == null) {
                stale.add(entry);
                continue;
            }
            BodyCandidate candidate = new BodyCandidate(level, entry.pos(), null, entry);
            (level == selfLevel ? sameDim : otherDim).add(candidate);
        }

        sameDim.sort(Comparator.comparingDouble(c -> self.blockPosition().distSqr(c.pos())));

        // ★ 同维度优先；同维度没有就跨维度
        TransferredBody found = resolveFirst(sameDim, stale);
        if (found == null) {
            found = resolveFirst(otherDim, stale);
        }
        ShellBodyIndex.removeAll(self, stale);
        return found;
    }

    @Nullable
    private static TransferredBody resolveFirst(List<BodyCandidate> candidates,
                                                List<ShellBodyIndex.Entry> stale) {
        for (BodyCandidate candidate : candidates) {
            TransferredBody body = candidate.resolve(stale);
            if (body != null) {
                return body;
            }
        }
        return null;
    }

    /** 一具候选身体：内存里已拿到的直接用；索引里的先按需加载区块再解析 */
    private record BodyCandidate(ServerLevel level, BlockPos pos,
                                 @Nullable TransferredBody loaded,
                                 @Nullable ShellBodyIndex.Entry entry) {

        @Nullable
        TransferredBody resolve(List<ShellBodyIndex.Entry> stale) {
            if (this.loaded != null) {
                return this.loaded;
            }
            // ★ 按需加载所在区块，远处的/其他维度的身体也能取到
            this.level.getChunkAt(this.pos);

            if (this.entry.isAvatar()) {
                if (this.level.getEntity(this.entry.avatarId()) instanceof CloneAvatarEntity avatar
                        && avatar.ready()) {
                    return avatar;
                }
                stale.add(this.entry);   // 分身已经不在了
                return null;
            }
            if (this.level.getBlockEntity(this.pos) instanceof CloneChamberBlockEntity chamber) {
                // 仓还在：身体可能还在培育中或已被取走，索引保留
                return chamber.ready() ? chamber : null;
            }
            stale.add(this.entry);       // 仓都不在了
            return null;
        }
    }
}