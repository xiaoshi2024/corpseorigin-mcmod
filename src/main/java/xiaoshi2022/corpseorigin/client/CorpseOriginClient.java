package xiaoshi2022.corpseorigin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.CloneChamberBlock;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.client.aps.APSInkSceneManager;
import xiaoshi2022.corpseorigin.client.aps.APSInkSceneRenderer;
import xiaoshi2022.corpseorigin.client.camera.PersistentCameraEntity;
import xiaoshi2022.corpseorigin.client.camera.PersistentCameraEntityGoal;
import xiaoshi2022.corpseorigin.client.gui.CloneChamberScreen;
import xiaoshi2022.corpseorigin.client.hud.InfectionHudOverlay;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;
import xiaoshi2022.corpseorigin.client.render.laser.BloodLotusLaserManager;
import xiaoshi2022.corpseorigin.client.renderer.blockentity.CloneChamberRenderer;
import xiaoshi2022.corpseorigin.client.renderer.entity.CloneAvatarRenderer;
import xiaoshi2022.corpseorigin.client.renderer.entity.FlyingGreatSwordRenderer;
import xiaoshi2022.corpseorigin.client.renderer.entity.JuQueBeamRenderer;
import xiaoshi2022.corpseorigin.client.renderer.entity.LowerLevelZbRenderer;
import xiaoshi2022.corpseorigin.client.skin.clone.ClientSkinCache;
import xiaoshi2022.corpseorigin.event.client.AttackAnimationHandler;
import xiaoshi2022.corpseorigin.event.client.ClientEntityEventHandler;
import xiaoshi2022.corpseorigin.network.*;
import xiaoshi2022.corpseorigin.registry.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CorpseOriginClient implements ClientModInitializer {

    /** 客户端可转移身体列表（UI 显示用，只含轻量信息） */
    public static final java.util.List<ClientShellEntry> clientShellEntries =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    /** 客户端轻量身体条目 */
    public record ClientShellEntry(
            java.util.UUID uuid,
            java.util.UUID ownerUuid,
            String world,
            int x, int y, int z,
            float progress
    ) {
    }

    // ✅ 客户端尸兄数据缓存（用 UUID 作为键）
    public static final java.util.Map<UUID, ClientCorpseData> corpseDataCache = new ConcurrentHashMap<>();

    /** ✅ 临时红眼状态：UUID → 剩余 tick */
    public static final Map<UUID, Integer> tempRedEyeTicks = new ConcurrentHashMap<>();

    /** ✅ 天线宝宝尸兄吸食状态：施术者 UUID → 正在吸的对象与剩余 tick */
    public static final Map<UUID, AntennaSuck> antennaSucks = new ConcurrentHashMap<>();

    /** 一次进行中的吸食：目标实体 id + 剩余 tick（{@code targetEntityId < 0} = 目标未知，只播动画不转向） */
    public record AntennaSuck(int targetEntityId, int ticks) {
    }

    /** 这位玩家现在是否正在吸食（盔甲渲染时读它决定播不播 absorb） */
    public static boolean isAntennaSucking(UUID uuid) {
        AntennaSuck suck = uuid == null ? null : antennaSucks.get(uuid);
        return suck != null && suck.ticks() > 0;
    }

    /**
     * 取「正在被这位玩家吸食的目标实体」，没有 / 不在客户端（未加载、已死）时返回 null。
     * <p>
     * 盔甲渲染要靠它算触手转向的角度，所以这里只做解析，不做任何逻辑判定。
     */
    public static LivingEntity getAntennaSuckTarget(UUID casterUuid) {
        AntennaSuck suck = casterUuid == null ? null : antennaSucks.get(casterUuid);
        if (suck == null || suck.ticks() <= 0 || suck.targetEntityId() < 0) {
            return null;
        }

        var level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }

        Entity entity = level.getEntity(suck.targetEntityId());
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }


    @Override
    public void onInitializeClient() {
        // 1. 按键绑定
        CorpseKeyBindings.register();

        // 2. 实体渲染器
        EntityRendererRegistry.register(ModEntities.LOWER_LEVEL_ZB, LowerLevelZbRenderer::new);
        EntityRendererRegistry.register(ModEntities.JUQUE_BEAM, JuQueBeamRenderer::new);
        EntityRendererRegistry.register(ModEntities.FLYING_GREAT_SWORD, FlyingGreatSwordRenderer::new);
        EntityRendererRegistry.register(ModEntities.CLONE_AVATAR,
                context -> new CloneAvatarRenderer(context, false));
        // 黑色火线克隆仓方块实体渲染器
        BlockEntityRendererRegistry.register(
                ModBlockEntities.CLONE_CHAMBER,
                CloneChamberRenderer::new
        );
        
        // 3. 模型层注册
        ModModelLayers.register();

        // ✅ 天线宝宝盔甲动画用到的 query.target_*_rotation（GeckoLib 没内置，得自己注册）
        xiaoshi2022.corpseorigin.client.renderer.armor.AntennaZBRitemRenderer.registerMolangQueries();

        // 4. 流体纹理
        registerFluidTextures();

        // 5. 玩家尸兄渲染层
        CorpsePlayerRenderHandler.register();

        // ✅ 注册客户端攻击事件监听
        AttackAnimationHandler.register();
        // ✅ 注册客户端实体事件
        ClientEntityEventHandler.register();

        // ✅ 注册 HUD
        InfectionHudOverlay.register();

        // 6. 网络接收
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.CharacterSyncS2C.TYPE, (payload, context) -> {
            context.client().execute(() ->
                    CharacterManagerBridge.setCharacter(payload.characterId()));
        });

        ClientPlayNetworking.registerGlobalReceiver(ShellStateSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    CorpseOriginClient.clientShellEntries.clear();
                    for (ShellStateSyncS2C.Entry e : payload.entries()) {
                        CorpseOriginClient.clientShellEntries.add(new ClientShellEntry(
                                e.uuid(), e.ownerUuid(), e.world(),
                                e.x(), e.y(), e.z(), e.progress()));
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(SynchronizationResponsePacket.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();

                    // 文字提示（失败原因、死亡自动夺舍的提示之类）
                    if (!payload.message().isEmpty() && client.player != null) {
                        client.player.sendSystemMessage(Component.literal(payload.message()));
                    }

                    if (!payload.success()) {
                        // 换身体失败：别再等落位了，把镜头交还给玩家
                        PersistentCameraEntity.unset(client);
                        return;
                    }

                    // 只提示不过场的包（没有目标身体数据），到此为止
                    if (!payload.cameraCutscene()) {
                        return;
                    }

                    var player = client.player;
                    if (player == null) return;

                    // ★ Flashback 兼容：回放会把录制到的包原样重放一遍（包括这个同步响应包），
                    //   回放中绝不能抢相机，否则视角会被拽进意识转移动画
                    if (PersistentCameraEntity.isReplayPlaying()) {
                        CorpseOrigin.LOGGER.debug("回放中，跳过意识转移相机过场");
                        return;
                    }

                    BlockPos startPos = payload.fromPos();
                    Direction startFacing = payload.fromFacing();
                    BlockPos targetPos = payload.toPos();
                    Direction targetFacing = payload.toFacing();

                    // ★ 过场只针对「当前玩家自己的第一人称视角」：相机被别人接管（旁观/切视角）或第三人称时
                    //   不播过场，但仍然立刻回包，否则服务端会一直等 CameraDonePacket，身体永远换不过来
                    if (!PersistentCameraEntity.isLocalPlayerFirstPersonView(client)) {
                        finishCamera(payload.targetStateUuid(), startPos, startFacing, targetPos, targetFacing,
                                payload.toWorld());
                        return;
                    }

                    boolean sameWorld = payload.fromWorld().equals(payload.toWorld());

                    PersistentCameraEntityGoal cameraGoal = player.isDeadOrDying()
                            ? PersistentCameraEntityGoal.limbo(startPos, startFacing, targetPos,
                            __ -> finishCamera(payload.targetStateUuid(), startPos, startFacing, targetPos,
                                    targetFacing, payload.toWorld()))
                            : PersistentCameraEntityGoal.stairwayToHeaven(startPos, startFacing, targetPos,
                            __ -> finishCamera(payload.targetStateUuid(), startPos, startFacing, targetPos,
                                    targetFacing, payload.toWorld()));

                    PersistentCameraEntity.setup(client, cameraGoal);
                }));
        
        // ✅ 接收玩家尸兄数据同步（用 UUID）
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.PlayerCorpseSyncS2C.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                // ★ 顺便刷新一次皮肤缓存
                ClientSkinCache.resolve(payload.playerUuid());

                ClientCorpseData data = new ClientCorpseData(
                        payload.isCorpse(),
                        payload.corpseType(),
                        payload.corpseData()
                );
                corpseDataCache.put(payload.playerUuid(), data);  // ✅ 用 UUID
                CorpseOrigin.LOGGER.debug("收到玩家尸兄数据: uuid={}, isCorpse={}",
                        payload.playerUuid(), payload.isCorpse());
            });
        });

        // ✅ 接收进化/已学技能同步
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.EvolutionSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() ->
                        ClientState.applyEvolution(
                                payload.earnedPoints(),
                                payload.availablePoints(),
                                payload.kills(),
                                payload.learnedSkills())));

        // ✅ 接收技能冷却同步
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.CooldownSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() ->
                        ClientState.applyCooldown(payload.skillPath(), payload.ticks())));

        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.InfectionSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() ->
                        ClientState.infection = payload.infection()));

        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.TempRedEyeSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() ->
                        tempRedEyeTicks.put(payload.playerUuid(), payload.durationTicks())));

        // ✅ 天线宝宝尸兄吸食状态（0 及以下 = 立刻结束，用于被打断）
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.AntennaSuckSyncS2C.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.durationTicks() <= 0) {
                        antennaSucks.remove(payload.playerUuid());
                    } else {
                        antennaSucks.put(payload.playerUuid(),
                                new AntennaSuck(payload.targetEntityId(), payload.durationTicks()));
                    }
                }));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CorpseKeyBindings.openSkillWheel.consumeClick()) {
                // 1. 周围有克隆仓 → 打开克隆仓 UI
                BlockPos nearby = findNearbyCloneChamber();
                if (nearby != null) {
                    client.gui.setScreen(new CloneChamberScreen(nearby));
                    continue;
                }

                // 2. 否则走技能轮盘
                if (client.gui.screen() instanceof SkillWheelScreen) {
                    client.gui.setScreen(null);
                } else if (client.gui.screen() == null) {
                    client.gui.setScreen(new SkillWheelScreen());
                }
            }

            // 技能树保持"按一下打开"
            while (CorpseKeyBindings.openSkillTree.consumeClick()) {
                client.gui.setScreen(new SkillTreeScreen());
            }
            while (CorpseKeyBindings.toggleHud.consumeClick()) {
                ClientState.hudVisible = !ClientState.hudVisible;
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(
                            Component.translatable(ClientState.hudVisible
                                    ? "hud.corpseorigin.toggle.on"
                                    : "hud.corpseorigin.toggle.off")
                    );
                }
            }

            // ✅ 红眼计时自减（安全写法）
            if (!tempRedEyeTicks.isEmpty()) {
                tempRedEyeTicks.replaceAll((k, v) -> v - 1);
                tempRedEyeTicks.entrySet().removeIf(e -> e.getValue() <= 0);
            }

            // ✅ 吸食计时自减
            if (!antennaSucks.isEmpty()) {
                antennaSucks.replaceAll((k, v) -> new AntennaSuck(v.targetEntityId(), v.ticks() - 1));
                antennaSucks.entrySet().removeIf(e -> e.getValue().ticks() <= 0);
            }
        });

        // 注册激光 + 水墨渲染
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            PoseStack poseStack = context.poseStack();
            SubmitNodeCollector collector = context.submitNodeCollector();
            BloodLotusLaserManager.getInstance().render(poseStack, collector);

            // ✅ 水墨意境
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameRenderer != null && mc.gameRenderer.mainCamera() != null) {
                Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
                APSInkSceneRenderer.render(poseStack, collector, cameraPos);
            }
        });

// ✅ 接收多目标链条包
        ClientPlayNetworking.registerGlobalReceiver(BloodLotusLaserMultiPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                BloodLotusLaserManager.getInstance().addChains(
                        payload.getStart(),
                        payload.targetUuids(),
                        payload.durationTicks()
                );
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(BloodLotusAuraPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                BloodLotusLaserManager.getInstance().addAura(
                        payload.playerUuid(),
                        payload.durationTicks()
                );
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(APSInkScenePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.open()) {
                    APSInkSceneManager.open(
                            payload.casterId(),
                            payload.x(), payload.y(), payload.z(),
                            payload.riverDirX(), payload.riverDirZ());
                } else {
                    APSInkSceneManager.close();
                }
            });
        });
        // ✅ 新增：接收诗牌落下
        ClientPlayNetworking.registerGlobalReceiver(APSInkPoemPayload.TYPE, (payload, context) -> {
            context.client().execute(() ->
                    APSInkSceneManager.dropPoem(payload.lineIndex()));
        });

// 每 tick 更新
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BloodLotusLaserManager.getInstance().tick();
        });

        CorpseOrigin.LOGGER.debug("CorpseOrigin client initialized");
    }

    private static void finishCamera(java.util.UUID targetUuid, BlockPos startPos, Direction startFacing,
                                     BlockPos targetPos, Direction targetFacing, Identifier targetWorld) {
        // 第一段（灵魂上天）放完：通知服务端开始换身体，镜头先留在天上，
        // 等玩家真正落到新身体后再播第二段「下落附身」（见 beginHandoff）
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                new CameraDonePacket(targetUuid));
        PersistentCameraEntity.beginHandoff(startPos, startFacing, targetPos, targetFacing, targetWorld);
    }

    /** 找玩家周围 3 格内最近的克隆仓（只认下半格），返回其方块坐标 */
    public static BlockPos findNearbyCloneChamber() {
        var player = Minecraft.getInstance().player;
        if (player == null || player.level() == null) {
            return null;
        }

        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (player.level().getBlockEntity(pos)
                            instanceof CloneChamberBlockEntity chamber
                            && CloneChamberBlock.isLower(chamber.getBlockState())) {
                        return chamber.getBlockPos().immutable();
                    }
                }
            }
        }
        return null;
    }

    // ==================== 客户端数据类 ====================

    public static class ClientCorpseData {
        public final boolean isCorpse;
        public final int corpseType;
        public final CompoundTag data;

        public ClientCorpseData(boolean isCorpse, int corpseType, CompoundTag data) {
            this.isCorpse = isCorpse;
            this.corpseType = corpseType;
            this.data = data;
        }

        public boolean isDisguised() {
            return data.getBoolean("is_disguised").orElse(false);
        }

        public boolean hasConsciousness() {
            return data.getBoolean("has_consciousness").orElse(false);
        }

        public int getExtraEyeCount() {
            return data.getInt("extra_eye_count").orElse(0);
        }

        public boolean hasWing() {
            return data.getBoolean("has_wing").orElse(false);
        }

        public boolean hasTail() {
            return data.getBoolean("has_tail").orElse(false);
        }

        /** 尸兄变种：{@code 2} = 无外骨骼通用变种（不长尸眼骨骼） */
        public int getVariant() {
            return data.getInt("variant").orElse(0);
        }
    }

    private static void registerFluidTextures() {
        Identifier stillId = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "block/infected_water_still");
        Identifier flowingId = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "block/infected_water_flow");
        Identifier overlayId = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "block/infected_water_overlay");

        Material still = new Material(stillId);
        Material flowing = new Material(flowingId);
        Material overlay = new Material(overlayId);

        FluidRenderingRegistry.register(
                ModFluids.INFECTED_WATER,
                ModFluids.FLOWING_INFECTED_WATER,
                new FluidModel.Unbaked(
                        still,
                        flowing,
                        overlay,
                        BlockTintSources.constant(ARGB.opaque(0x884422))
                )
        );

        CorpseOrigin.LOGGER.debug("✅ 尸水纹理已注册");
    }
}