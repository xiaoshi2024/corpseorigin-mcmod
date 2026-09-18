package xiaoshi2022.corpseorigin.network;

import com.mojang.datafixers.util.Either;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.effect.BYeffect;
import xiaoshi2022.corpseorigin.entity.JuQueBeamEntity;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.item.sword.JuQue;
import xiaoshi2022.corpseorigin.shell.ServerShell;
import xiaoshi2022.corpseorigin.shell.ShellState;
import xiaoshi2022.corpseorigin.shell.TransferredBody;
import xiaoshi2022.corpseorigin.skill.SkillManager;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CorpseNetwork {

    // ✅ 待同步队列
    private static final Set<UUID> PENDING_SYNC = new HashSet<>();

    // 待执行的 sync（相机动画结束后再 apply）
    public record PendingSync(ShellState targetState, TransferredBody target) {}

    private static final Map<UUID, PendingSync> PENDING_SYNCS = new ConcurrentHashMap<>();

    private CorpseNetwork() {
    }

    public static void register() {
        // ==================== 角色选择系统 ====================
        PayloadTypeRegistry.serverboundPlay().register(CorpsePayloads.SelectCharacterC2S.TYPE, CorpsePayloads.SelectCharacterC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CorpsePayloads.CharacterSyncS2C.TYPE, CorpsePayloads.CharacterSyncS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CorpsePayloads.PlayerCorpseSyncS2C.TYPE, CorpsePayloads.PlayerCorpseSyncS2C.CODEC);

        // ✅ 技能系统：激活（C2S）+ 进化/冷却同步（S2C）
        PayloadTypeRegistry.serverboundPlay().register(CorpsePayloads.ActivateSkillC2S.TYPE, CorpsePayloads.ActivateSkillC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CorpsePayloads.EvolutionSyncS2C.TYPE, CorpsePayloads.EvolutionSyncS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CorpsePayloads.CooldownSyncS2C.TYPE, CorpsePayloads.CooldownSyncS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CorpsePayloads.InnerPowerSyncS2C.TYPE, CorpsePayloads.InnerPowerSyncS2C.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CorpsePayloads.SelectCharacterC2S.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                xiaoshi2022.corpseorigin.character.CharacterManager.getInstance()
                        .setPlayerCharacter(player, payload.characterId());
            });
        });

        PayloadTypeRegistry.clientboundPlay().register(
                CorpsePayloads.TempRedEyeSyncS2C.TYPE,
                CorpsePayloads.TempRedEyeSyncS2C.CODEC);

        // ✅ 天线宝宝尸兄吸食状态（S2C）
        PayloadTypeRegistry.clientboundPlay().register(
                CorpsePayloads.AntennaSuckSyncS2C.TYPE,
                CorpsePayloads.AntennaSuckSyncS2C.CODEC);

        // ✅ 天线宝宝尸兄「格挡」动画信号（S2C）
        PayloadTypeRegistry.clientboundPlay().register(
                CorpsePayloads.AntennaBlockSyncS2C.TYPE,
                CorpsePayloads.AntennaBlockSyncS2C.CODEC);


        // ✅ 学习技能（C2S）
        PayloadTypeRegistry.serverboundPlay().register(
                CorpsePayloads.LearnSkillC2S.TYPE,
                CorpsePayloads.LearnSkillC2S.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CorpsePayloads.LearnSkillC2S.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() ->
                    SkillManager.learn(player, payload.skillPath()));
        });

        // ✅ 技能激活（服务端校验 + 效果 + 冷却同步）
        ServerPlayNetworking.registerGlobalReceiver(CorpsePayloads.ActivateSkillC2S.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() ->
                    SkillManager.activate(player, payload.skillPath()));
        });

        // ==================== 皮肤更新系统 ====================
        PayloadTypeRegistry.serverboundPlay().register(ZbSkinUpdatePacket.TYPE, ZbSkinUpdatePacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ZbSkinUpdatePacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (!(player.level() instanceof ServerLevel level)) return;

                Entity entity = level.getEntity(payload.entityId());
                if (entity instanceof LowerLevelZbEntity zbEntity) {
                    String textureStr = payload.skinTexture();
                    if (textureStr != null && !textureStr.isEmpty()) {
                        try {
                            Identifier texture = Identifier.fromNamespaceAndPath(
                                    textureStr.contains(":") ? textureStr.split(":")[0] : "corpseorigin",
                                    textureStr.contains(":") ? textureStr.split(":")[1] : textureStr
                            );
                            zbEntity.setSkinTextureFromServer(texture);
                        } catch (Exception e) {
                            CorpseOrigin.LOGGER.warn("无效的皮肤纹理路径: {}", textureStr);
                        }
                    }
                    zbEntity.setSkinStateFromServer(ZbSkinState.fromCode(payload.skinStateCode()));

                    CorpseOrigin.LOGGER.debug("服务端收到皮肤更新: 实体 {} 状态 {}",
                            payload.entityId(), payload.skinStateCode());
                }
            });
        });

        PayloadTypeRegistry.clientboundPlay().register(
                CorpsePayloads.InfectionSyncS2C.TYPE,
                CorpsePayloads.InfectionSyncS2C.CODEC);

        // ==================== ✅ 玩家加入时加入待同步队列 ====================
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer joiningPlayer = handler.getPlayer();
            PENDING_SYNC.add(joiningPlayer.getUUID());
            CorpseOrigin.LOGGER.debug("玩家 {} 加入，加入待同步队列",
                    joiningPlayer.getName().getString());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            UUID uuid = player.getUUID();

            PENDING_SYNC.remove(uuid);
            PENDING_SYNCS.remove(uuid);
            SkillManager.cleanupDisconnect(uuid);
            xiaoshi2022.corpseorigin.character.InnerPowerManager.cleanupDisconnect(uuid);
            xiaoshi2022.corpseorigin.event.HeiXiaoFeiEventHandler.cleanupDisconnect(uuid);
            BYeffect.clearTotalDuration(uuid);
            BYeffect.clearInfectionSource(uuid);

            // ✅ 退出时回收剑意
            if (player.level() instanceof ServerLevel level) {
                xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager
                        .forceRestoreOnDisconnect(player, level);
            }
        });

        // ✅ 每 tick 检查待同步队列（延迟 20 tick 后同步）+ 内力自然回复
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 内力回复
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                xiaoshi2022.corpseorigin.character.InnerPowerManager.tickRegen(player);
            }

            if (PENDING_SYNC.isEmpty()) return;

            var iterator = PENDING_SYNC.iterator();
            while (iterator.hasNext()) {
                UUID uuid = iterator.next();
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);

                if (player == null) {
                    iterator.remove();
                    continue;
                }

                // ✅ 玩家 tickCount >= 20 后同步
                if (player.tickCount >= 20) {
                    iterator.remove();
                    syncAllCorpseDataToPlayer(player, server);
                    CorpseOrigin.LOGGER.debug("✅ 延迟同步完成: {}",
                            player.getName().getString());
                }
            }
        });

        // ==================== ✅ 巨阙剑气（C2S） ====================
        PayloadTypeRegistry.serverboundPlay().register(
                JuQueBeamPacket.TYPE,
                JuQueBeamPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(JuQueBeamPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                ItemStack stack = player.getMainHandItem();

                // ✅ 和 use() 保持一致的判断
                if (APSTerrainManager.hasActiveAPS(player)) {
                    // 需要把 releaseGreatSword 改成 public static 或从外部调用
                    JuQue.releaseGreatSwordStatic(player, stack, InteractionHand.MAIN_HAND);
                } else {
                    JuQue.releaseBeamStatic(player, stack);
                }
            });
        });

        // ==================== ✅ 血莲宝灯激光（S2C，多目标合并包） ====================
        PayloadTypeRegistry.clientboundPlay().register(
                BloodLotusLaserMultiPayload.TYPE,
                BloodLotusLaserMultiPayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(
                BloodLotusAuraPayload.TYPE,
                BloodLotusAuraPayload.CODEC);

        // ✅ 水墨意境开关（S2C）
        PayloadTypeRegistry.clientboundPlay().register(
                APSInkScenePayload.TYPE,
                APSInkScenePayload.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(
                APSInkPoemPayload.TYPE,
                APSInkPoemPayload.CODEC);

        // ==================== ✅ 同步/相机系统 ====================
        PayloadTypeRegistry.serverboundPlay().register(
                SynchronizationRequestPacket.TYPE, SynchronizationRequestPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                SynchronizationResponsePacket.TYPE, SynchronizationResponsePacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                ShellStateSyncS2C.TYPE, ShellStateSyncS2C.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                CameraDonePacket.TYPE, CameraDonePacket.CODEC);

        // ---- 客户端请求转移 ----
        ServerPlayNetworking.registerGlobalReceiver(SynchronizationRequestPacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                ServerShell shell = ServerShell.of(player);

                // ★ 先按坐标（玩家看到的那具身体），再按 UUID 兜底
                BlockPos targetPos = payload.targetPos();
                TransferredBody target = shell.getAvailableBodies()
                        .filter(b -> {
                            ShellState s = b.snapshot();
                            if (s == null) return false;
                            return (targetPos != null && targetPos.equals(s.getPos()))
                                    || s.getUuid().equals(payload.targetStateUuid());
                        })
                        .findFirst().orElse(null);

                if (target == null) {
                    // ★ 说清楚为什么不行，并顺手刷新过期列表，避免反复报同一个错
                    ServerPlayNetworking.send(player,
                            SynchronizationResponsePacket.failure(noTargetReason(player, targetPos)));
                    syncShellStates(player);
                    return;
                }

                ShellState targetState = target.snapshot();
                if (targetState == null) {
                    ServerPlayNetworking.send(player,
                            SynchronizationResponsePacket.failure("目标身体没有快照"));
                    return;
                }

                BlockPos fromPos = player.blockPosition();
                BlockPos toPos = targetState.getPos() == null ? fromPos : targetState.getPos();
                Identifier fromWorld = player.level().dimension().identifier();
                Identifier toWorld = targetState.getWorld() != null
                        ? targetState.getWorld()
                        : fromWorld;

                PENDING_SYNCS.put(player.getUUID(), new PendingSync(targetState, target));

                ServerPlayNetworking.send(player, new SynchronizationResponsePacket(
                        true, true, "",
                        payload.targetStateUuid(),
                        fromWorld, fromPos, player.getDirection(),
                        toWorld, toPos, Direction.NORTH));
            });
        });

        // ---- 客户端相机动画结束，服务端执行 sync ----
        ServerPlayNetworking.registerGlobalReceiver(CameraDonePacket.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                PendingSync pending = PENDING_SYNCS.remove(player.getUUID());
                if (pending == null || pending.target() == null) return;

                Either<ShellState, String> result = ServerShell.of(player).sync(pending.target());
                if (result.right().isPresent()) {
                    ServerPlayNetworking.send(player,
                            SynchronizationResponsePacket.failure(result.right().get()));
                }

                syncShellStates(player);
            });
        });

        CorpseOrigin.LOGGER.debug("CorpseOrigin network registered (Fabric 26.2)");
    }

    private static void syncShellStates(ServerPlayer player) {
        ServerShell shell = ServerShell.of(player);
        List<ShellStateSyncS2C.Entry> entries = shell.getAvailableBodies()
                .map(TransferredBody::snapshot)
                .filter(Objects::nonNull)
                .map(s -> new ShellStateSyncS2C.Entry(
                        s.getUuid(), s.getOwnerUuid(),
                        s.getWorld() == null ? "" : s.getWorld().toString(),
                        s.getPos() == null ? 0 : s.getPos().getX(),
                        s.getPos() == null ? 0 : s.getPos().getY(),
                        s.getPos() == null ? 0 : s.getPos().getZ(),
                        s.getProgress()))
                .toList();
        ServerPlayNetworking.send(player, new ShellStateSyncS2C(entries));
    }

    public static void broadcastInkPoem(ServerPlayer caster, int lineIndex) {
        APSInkPoemPayload payload = new APSInkPoemPayload(caster.getUUID(), lineIndex);
        for (ServerPlayer p : caster.level().players()) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    public static void refreshShellStates(ServerPlayer player) {
        syncShellStates(player); // 现有 private 方法
    }

    /** 转移目标解析失败时给出具体原因（列表过期 / 身体已被取走 / 还没培育完） */
    private static String noTargetReason(ServerPlayer player, BlockPos pos) {
        if (pos != null && player.level().getBlockEntity(pos) instanceof CloneChamberBlockEntity chamber) {
            if (!chamber.hasClone()) {
                String hint = otherBodyHint(player);
                return hint == null
                        ? "这座克隆仓里没有可转移的身体"
                        : "这座克隆仓里没有可转移的身体（你还有身体在" + hint + "）";
            }
            if (!chamber.ready()) {
                int percent = Math.min(99, (int) (chamber.getCloneProgress() * 100.0F));
                return "克隆体还在培育中：" + percent + "%";
            }
        }
        return "找不到目标身体";
    }

    /**
     * 提示 owner 其他可转移身体的位置。
     * <p>
     * "仓里没有身体"最常见的原因是那具身体已经被取走、留在了别的仓里（或死亡时被丢弃），
     * 直接告诉玩家它现在在哪，免得误以为是功能坏了。
     */
    private static String otherBodyHint(ServerPlayer player) {
        String selfWorld = player.level().dimension().identifier().toString();
        return ServerShell.of(player).getAvailableBodies()
                .map(TransferredBody::snapshot)
                .filter(Objects::nonNull)
                .filter(state -> state.getPos() != null)
                .min(Comparator.comparingDouble(state -> {
                    boolean sameWorld = selfWorld.equals(state.getWorld());
                    return sameWorld
                            ? player.blockPosition().distSqr(state.getPos())
                            : Double.MAX_VALUE;
                }))
                .map(state -> {
                    boolean sameWorld = selfWorld.equals(state.getWorld());
                    String world = sameWorld ? "" : (state.getWorld() == null ? "其他维度" : state.getWorld()) + " ";
                    BlockPos bodyPos = state.getPos();
                    return world + "(" + bodyPos.getX() + ", " + bodyPos.getY() + ", " + bodyPos.getZ() + ")";
                })
                .orElse(null);
    }

    private static void handleJuQueBeam(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof JuQue juQue)) return;
        if (player.getCooldowns().isOnCooldown(stack)) return;

        // 消耗耐久
        if (!player.isCreative()) {
            stack.hurtAndBreak(1, player,
                    player.getUsedItemHand() == InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        // 音效
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4F, 0.5F);

        // 计算伤害
        float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = baseDamage * JuQue.BEAM_DAMAGE_MULT;

        // 创建剑气
        JuQueBeamEntity beam = new JuQueBeamEntity(player.level(), player);
        beam.setDamage(damage);
        beam.setLevel(JuQue.BEAM_LEVEL);
        beam.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                beam.getVelocity(), 1.0F);
        player.level().addFreshEntity(beam);

        // 冷却
        player.getCooldowns().addCooldown(stack, JuQue.COOLDOWN);
    }

    public static void sendInfectionSync(ServerPlayer player) {
        int infection = PlayerCorpseComponent.get(player).getInfection();
        ServerPlayNetworking.send(player, new CorpsePayloads.InfectionSyncS2C(infection));
    }


    public static void broadcastTempRedEye(ServerPlayer player, int durationTicks) {
        CorpsePayloads.TempRedEyeSyncS2C packet =
                new CorpsePayloads.TempRedEyeSyncS2C(player.getUUID(), durationTicks);
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(p, packet);
            }
        } else {
            ServerPlayNetworking.send(player, packet);
        }
    }

    /**
     * 广播「某位穿戴着天线宝宝盔甲的实体正在吸食谁」给全服。
     * <p>
     * 盔甲的 absorb 动画和触手朝向都是渲染时看的，所以要发给所有玩家；
     * {@code durationTicks <= 0} 表示立刻结束（松手 / 被打断），此时 targetEntityId 传 -1。
     * <p>
     * 支持玩家和任意穿戴该套装的生物：caster 可以是 ServerPlayer 或 LivingEntity。
     */
    public static void broadcastAntennaSuck(net.minecraft.world.entity.LivingEntity caster, int targetEntityId, int durationTicks) {
        CorpsePayloads.AntennaSuckSyncS2C packet =
                new CorpsePayloads.AntennaSuckSyncS2C(caster.getUUID(), targetEntityId, durationTicks);
        MinecraftServer server = caster.level().getServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(p, packet);
            }
        }
    }

    /**
     * ✅ 广播"这位天线宝宝尸兄正在格挡"，让穿在他身上的盔甲播格挡动画。
     * <p>
     * 被动挡下一击时给一个短窗口（够播完借用的那条 clip），主动格挡时给整个持续时间。
     */
    public static void broadcastAntennaBlock(ServerPlayer player, int durationTicks) {
        CorpsePayloads.AntennaBlockSyncS2C packet =
                new CorpsePayloads.AntennaBlockSyncS2C(player.getUUID(), durationTicks);
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(p, packet);
            }
        } else {
            ServerPlayNetworking.send(player, packet);
        }
    }

    /**
     * ✅ 同步所有尸兄玩家数据给新加入的玩家（包括自己）
     */
    private static void syncAllCorpseDataToPlayer(ServerPlayer joiningPlayer, MinecraftServer server) {
        // ✅ 1. 先给自己发自己的数据
        PlayerCorpseComponent selfComp = PlayerCorpseComponent.get(joiningPlayer);
        if (selfComp.isCorpse()) {
            CompoundTag selfData = selfComp.getDataPublic();
            CorpsePayloads.PlayerCorpseSyncS2C selfPacket = new CorpsePayloads.PlayerCorpseSyncS2C(
                    joiningPlayer.getUUID(),
                    selfComp.isCorpse(),
                    selfComp.getCorpseType(),
                    selfData
            );
            ServerPlayNetworking.send(joiningPlayer, selfPacket);
            CorpseOrigin.LOGGER.debug("✅ 已发送自己的尸兄数据给 {}",
                    joiningPlayer.getName().getString());
        }

        // ✅ 2. 发其他尸兄玩家的数据
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == joiningPlayer) continue;

            PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
            if (comp.isCorpse()) {
                CompoundTag data = comp.getDataPublic();
                CorpsePayloads.PlayerCorpseSyncS2C packet = new CorpsePayloads.PlayerCorpseSyncS2C(
                        player.getUUID(),
                        comp.isCorpse(),
                        comp.getCorpseType(),
                        data
                );
                ServerPlayNetworking.send(joiningPlayer, packet);
                count++;
            }
        }

        // ✅ 3. 如果自己是尸兄，广播给所有人
        if (selfComp.isCorpse()) {
            broadcastPlayerCorpseSync(joiningPlayer);
        }

        // ✅ 4. 同步角色进化/技能数据
        sendEvolutionSync(joiningPlayer);

        // ✅ 5. 同步感染度
        sendInfectionSync(joiningPlayer);

        // ✅ 6. 刷新可转移身体列表
        syncShellStates(joiningPlayer);

        CorpseOrigin.LOGGER.debug("✅ 已向玩家 {} 同步 {} 个其他尸兄玩家的数据",
                joiningPlayer.getName().getString(), count);
    }

    // ==================== ✅ 玩家尸兄数据同步 ====================

    public static void sendPlayerCorpseSync(ServerPlayer player) {
        sendPlayerCorpseSyncTo(player, player);
    }

    /**
     * 把某位玩家的尸兄状态发给<b>指定</b>接收者。
     * <p>
     * 用于点对点补发：例如新玩家刚进服时，把在线其他玩家的尸兄状态补给他 ——
     * 尸兄外观（多眼 / 外骨骼 / 皮肤）是"别人看你"时才渲染的，只给你自己发的话，
     * 在别人眼里你永远是个普通人。
     */
    public static void sendPlayerCorpseSyncTo(ServerPlayer player, ServerPlayer receiver) {
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
        CompoundTag data = comp.getDataPublic();

        CorpsePayloads.PlayerCorpseSyncS2C packet = new CorpsePayloads.PlayerCorpseSyncS2C(
                player.getUUID(),
                comp.isCorpse(),
                comp.getCorpseType(),
                data
        );

        ServerPlayNetworking.send(receiver, packet);
        CorpseOrigin.LOGGER.debug("同步玩家尸兄数据: {} → {}",
                player.getName().getString(), receiver.getName().getString());
    }

    /**
     * 按"身体 uuid"把这具身体的尸兄状态广播给同维度所有玩家（外加可能不在这个维度的 owner）。
     * <p>
     * 仓里的克隆人、放在外面的克隆分身别的玩家也看得见，只发给 owner 的话别人客户端没有这份数据，
     * 就看不到外骨骼。corpseTag 为 null 时按"不是尸兄"下发，用来清掉同一位置上一具身体留下的旧外观。
     */
    public static void broadcastBodyCorpseSync(ServerLevel level, ServerPlayer owner,
                                               java.util.UUID bodyUuid, CompoundTag corpseTag) {
        if (level == null || bodyUuid == null) {
            return;
        }
        CompoundTag data = corpseTag == null ? new CompoundTag() : corpseTag;
        CorpsePayloads.PlayerCorpseSyncS2C packet = new CorpsePayloads.PlayerCorpseSyncS2C(
                bodyUuid,
                data.getBoolean("is_corpse").orElse(false),
                data.getInt("corpse_type").orElse(0),
                data.copy());
        for (ServerPlayer receiver : level.players()) {
            ServerPlayNetworking.send(receiver, packet);
        }
        // owner 可能在别的维度（跨维度夺舍），单独补一份
        if (owner != null && owner.level() != level) {
            ServerPlayNetworking.send(owner, packet);
        }
    }

    public static void broadcastPlayerCorpseSync(ServerPlayer player) {
        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
        CompoundTag data = comp.getDataPublic();

        CorpsePayloads.PlayerCorpseSyncS2C packet = new CorpsePayloads.PlayerCorpseSyncS2C(
                player.getUUID(),
                comp.isCorpse(),
                comp.getCorpseType(),
                data
        );

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(p, packet);
            }
            CorpseOrigin.LOGGER.debug("广播玩家尸兄数据: {} 给 {} 个玩家",
                    player.getName().getString(),
                    server.getPlayerList().getPlayers().size());
        } else {
            ServerPlayNetworking.send(player, packet);
        }
    }

    public static void sendCharacterSync(ServerPlayer player, String characterId) {
        ServerPlayNetworking.send(player, new CorpsePayloads.CharacterSyncS2C(characterId));
    }

    // ==================== ✅ 技能/进化数据同步 ====================

    /**
     * 同步进化状态 + 已学技能给指定玩家（S2C）。
     */
    public static void sendEvolutionSync(ServerPlayer player) {
        PlayerCharacterData data = PlayerCharacterData.get(player);
        Set<String> learned = data.getLearnedSkills(player.getUUID());
        String joined = String.join("\n", learned);
        byte[] bytes = joined.getBytes(StandardCharsets.UTF_8);

        int kills = PlayerCorpseComponent.get(player).getKills();
        int earned = data.getEarnedPoints(player.getUUID());
        int available = data.getAvailablePoints(player.getUUID());

        ServerPlayNetworking.send(player,
                new CorpsePayloads.EvolutionSyncS2C(earned, available, kills, bytes));
    }

    /**
     * 同步某技能冷却给指定玩家（S2C）。
     */
    public static void sendCooldownSync(ServerPlayer player, String skillPath, int ticks) {
        ServerPlayNetworking.send(player, new CorpsePayloads.CooldownSyncS2C(skillPath, ticks));
    }

    /** 同步内力值给客户端（S2C） */
    public static void sendInnerPowerSync(ServerPlayer player) {
        int current = xiaoshi2022.corpseorigin.character.InnerPowerManager.getInnerPower(player);
        int max = xiaoshi2022.corpseorigin.character.InnerPowerManager.getMaxInnerPower(player);
        ServerPlayNetworking.send(player, new CorpsePayloads.InnerPowerSyncS2C(current, max));
    }
}