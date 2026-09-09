package xiaoshi2022.corpseorigin.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.renderer.LowerLevelZbRenderer;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.registry.ModFluids;

/**
 * 客户端初始化 - Fabric 26.2
 */
public class CorpseOriginClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CorpseKeyBindings.register();
        EntityRendererRegistry.register(ModEntities.LOWER_LEVEL_ZB, LowerLevelZbRenderer::new);
        registerFluidTextures();

        ClientPlayNetworking.registerGlobalReceiver(CorpsePayloads.CharacterSyncS2C.TYPE, (payload, context) -> {
            context.client().execute(() ->
                    CharacterManagerBridge.setCharacter(payload.characterId()));
        });

        CorpseOrigin.LOGGER.info("CorpseOrigin client initialized");
    }

    /**
     * 注册流体纹理 - 修复 Material 构造器
     */
    private static void registerFluidTextures() {
        // ✅ 方式1：如果 Material 只需要一个 Identifier
        // 纹理路径：assets/corpseorigin/textures/block/infected_water_still.png
        Identifier stillId = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "block/infected_water_still");
        Identifier flowingId = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "block/infected_water_flow");
        Identifier overlayId = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "block/infected_water_overlay");

        // 尝试只传 Identifier（如果构造器需要）
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