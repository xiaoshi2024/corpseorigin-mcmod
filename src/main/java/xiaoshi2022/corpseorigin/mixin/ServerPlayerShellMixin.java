package xiaoshi2022.corpseorigin.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.block.entity.ShellStorageBlockEntity;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.shell.*;

import java.util.ArrayList;
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

        // 2. 存储仓
        for (ShellStorageBlockEntity storage : ShellStorageBlockEntity.REGISTRY
                .getOrDefault(self.getUUID(), List.of())) {
            if (storage.ready()) {
                bodies.add(storage);
            }
        }

        // 3. 克隆仓
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
            targetState.setWorld(avatar.level().dimension().registry());
        }

        ServerLevel level = (ServerLevel) self.level();
        BlockPos currentPos = self.blockPosition();
        ShellState oldBody = ShellState.of(self, currentPos);

        if (target instanceof CloneAvatarEntity) {
            // ★ 目标是分身：旧身体丢弃 + 物品掉落
            if (!self.isSpectator()) {
                dropInventory(self);
            }
            target.consume(level);
        } else {
            // ★ 目标是方块容器：旧身体进目标容器
            target.consume(level);
            target.receiveOldBody(level, oldBody);
        }

        this.apply(targetState);
        return Either.left(oldBody);
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

        self.stopRiding();
        self.clearFire();
        self.removeAllEffects();
        self.setDeltaMovement(Vec3.ZERO);

        // 组件先抓当前实体的，然后 clone 目标身体的
        ShellStateComponent currentComponent = ShellStateComponent.of(self);
        currentComponent.clone(state.getComponent());

        // 如果目标组件是 CorpseShellStateComponent，显式 apply 到实体
        CorpseShellStateComponent corpseComp = state.getComponent().as(CorpseShellStateComponent.class);
        if (corpseComp != null) corpseComp.applyTo(self);

        CharacterShellStateComponent charComp = state.getComponent().as(CharacterShellStateComponent.class);
        if (charComp != null) charComp.applyTo(self);

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
        TransferredBody nearest = null;
        boolean nearestSameDim = false;
        double bestDist = Double.MAX_VALUE;

        // 1. 分身
        for (ServerLevel level : server.getAllLevels()) {
            boolean sameDim = (level == selfLevel);
            for (var entity : level.getAllEntities()) {
                if (entity instanceof CloneAvatarEntity avatar
                        && self.getUUID().equals(avatar.getOwnerUuid())
                        && avatar.ready()) {

                    double dist = sameDim ? self.distanceToSqr(avatar) : Double.MAX_VALUE;
                    if (isBetter(sameDim, dist, nearestSameDim, bestDist)) {
                        nearestSameDim = sameDim;
                        bestDist = dist;
                        nearest = avatar;
                    }
                }
            }
        }

        // 2. 存储仓
        for (ShellStorageBlockEntity storage : ShellStorageBlockEntity.REGISTRY
                .getOrDefault(self.getUUID(), List.of())) {
            if (!storage.ready()) continue;
            if (storage.getLevel() == null) continue;

            boolean sameDim = (storage.getLevel() == selfLevel);
            double dist = sameDim
                    ? self.blockPosition().distSqr(storage.getBlockPos())
                    : Double.MAX_VALUE;

            if (isBetter(sameDim, dist, nearestSameDim, bestDist)) {
                nearestSameDim = sameDim;
                bestDist = dist;
                nearest = storage;
            }
        }

        // 3. 克隆仓
        for (CloneChamberBlockEntity chamber : CloneChamberBlockEntity.REGISTRY
                .getOrDefault(self.getUUID(), List.of())) {
            if (!chamber.ready()) continue;
            if (chamber.getLevel() == null) continue;

            boolean sameDim = (chamber.getLevel() == selfLevel);
            double dist = sameDim
                    ? self.blockPosition().distSqr(chamber.getBlockPos())
                    : Double.MAX_VALUE;

            if (isBetter(sameDim, dist, nearestSameDim, bestDist)) {
                nearestSameDim = sameDim;
                bestDist = dist;
                nearest = chamber;
            }
        }

        return nearest;
    }

    private static boolean isBetter(boolean sameDim, double dist,
                                    boolean bestSameDim, double bestDist) {
        if (sameDim != bestSameDim) return sameDim;
        return dist < bestDist;
    }
}