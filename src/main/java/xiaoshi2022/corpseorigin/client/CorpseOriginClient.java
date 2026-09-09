package xiaoshi2022.corpseorigin.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;
import xiaoshi2022.corpseorigin.client.renderer.entity.LowerLevelZbRenderer;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.registry.ModFluids;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

public class CorpseOriginClient implements ClientModInitializer {

    // 客户端尸兄数据缓存
    public static final java.util.Map<Integer, ClientCorpseData> corpseDataCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void onInitializeClient() {
        // 1. 按键绑定
        CorpseKeyBindings.register();

        // 2. 实体渲染器
        EntityRendererRegistry.register(ModEntities.LOWER_LEVEL_ZB, LowerLevelZbRenderer::new);

        // 3. 模型层注册
        ModModelLayers.register();

        // 4. 流体纹理
        registerFluidTextures();

        // 5. 玩家尸兄渲染层
        CorpsePlayerRenderHandler.register();

        // 6. 网络接收
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.CharacterSyncS2C.TYPE, (payload, context) -> {
            context.client().execute(() ->
                    CharacterManagerBridge.setCharacter(payload.characterId()));
        });

        // ✅ 接收玩家尸兄数据同步
        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.PlayerCorpseSyncS2C.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                // 缓存数据
                ClientCorpseData data = new ClientCorpseData(
                        payload.isCorpse(),
                        payload.corpseType(),
                        payload.corpseData()
                );
                corpseDataCache.put(payload.playerId(), data);
                CorpseOrigin.LOGGER.debug("收到玩家尸兄数据: playerId={}, isCorpse={}",
                        payload.playerId(), payload.isCorpse());
            });
        });

        CorpseOrigin.LOGGER.info("CorpseOrigin client initialized");
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

        CorpseOrigin.LOGGER.info("✅ 尸水纹理已注册");
    }
}