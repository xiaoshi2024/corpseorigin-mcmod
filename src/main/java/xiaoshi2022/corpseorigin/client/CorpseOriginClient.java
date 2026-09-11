package xiaoshi2022.corpseorigin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.hud.InfectionHudOverlay;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;
import xiaoshi2022.corpseorigin.client.render.laser.BloodLotusLaserManager;
import xiaoshi2022.corpseorigin.client.renderer.entity.JuQueBeamRenderer;
import xiaoshi2022.corpseorigin.client.renderer.entity.LowerLevelZbRenderer;
import xiaoshi2022.corpseorigin.event.client.AttackAnimationHandler;
import xiaoshi2022.corpseorigin.event.client.ClientEntityEventHandler;
import xiaoshi2022.corpseorigin.network.BloodLotusLaserPayload;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.registry.ModFluids;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CorpseOriginClient implements ClientModInitializer {

    // ✅ 客户端尸兄数据缓存（用 UUID 作为键）
    public static final java.util.Map<UUID, ClientCorpseData> corpseDataCache = new ConcurrentHashMap<>();

    /** ✅ 临时红眼状态：UUID → 剩余 tick */
    public static final Map<UUID, Integer> tempRedEyeTicks = new ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        // 1. 按键绑定
        CorpseKeyBindings.register();

        // 2. 实体渲染器
        EntityRendererRegistry.register(ModEntities.LOWER_LEVEL_ZB, LowerLevelZbRenderer::new);
        EntityRendererRegistry.register(ModEntities.JUQUE_BEAM, JuQueBeamRenderer::new);

        // 3. 模型层注册
        ModModelLayers.register();

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

        // ✅ 接收玩家尸兄数据同步（用 UUID）
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.PlayerCorpseSyncS2C.TYPE, (payload, context) -> {
            context.client().execute(() -> {
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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CorpseKeyBindings.openSkillWheel.consumeClick()) {
                Minecraft.getInstance().gui.setScreen(new SkillWheelScreen());
            }
            while (CorpseKeyBindings.openSkillTree.consumeClick()) {
                Minecraft.getInstance().gui.setScreen(new SkillTreeScreen());
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
        });


        // 注册激光渲染
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            PoseStack poseStack = context.poseStack();
            SubmitNodeCollector collector = context.submitNodeCollector();
            BloodLotusLaserManager.getInstance().render(poseStack, collector);
        });

// 注册网络接收
        ClientPlayNetworking.registerGlobalReceiver(BloodLotusLaserPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                BloodLotusLaserManager.getInstance().addChain(
                        payload.getStart(),
                        payload.targetUuid(),
                        payload.durationTicks()
                );
            });
        });

// 每 tick 更新
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BloodLotusLaserManager.getInstance().tick();
        });

        CorpseOrigin.LOGGER.debug("CorpseOrigin client initialized");
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