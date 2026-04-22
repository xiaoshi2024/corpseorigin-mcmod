package com.phagens.corpseorigin;

import com.phagens.corpseorigin.GongFU.FaXiang.Geo.Renderer.FaxiangRenderer;
import com.phagens.corpseorigin.client.Renderer.block.QiXingGuanRenderer;
import com.phagens.corpseorigin.client.Renderer.block.ZBRFleshRenderer;
import com.phagens.corpseorigin.client.Renderer.entity.*;
import com.phagens.corpseorigin.entity.AlienatedSporeEntity;
import com.phagens.corpseorigin.entity.CorpseGibEntity;

import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.Renderer.CentipedeHeadRenderer;
import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.Renderer.CentipedeJointRenderer;
import com.phagens.corpseorigin.entity.skills.LongyouEarthquakeRenderer;
import com.phagens.corpseorigin.register.BlockEntityRegistry;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = CorpseOrigin.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public class CorpseOriginClient {
    public CorpseOriginClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }





    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CorpseOrigin.LOGGER.info("HELLO FROM CLIENT SETUP");
        CorpseOrigin.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.QI_XING_GUANS.get(), QiXingGuanRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.ZBR_FLESH.get(), ZBRFleshRenderer::new);
        event.registerEntityRenderer((EntityType<com.phagens.corpseorigin.entity.ZbrFishEntity>) EntityRegistry.ZBR_FISH.get(), ZbrFishRenderer::new);
        event.registerEntityRenderer((EntityType<com.phagens.corpseorigin.entity.LowerLevelZbEntity>) EntityRegistry.LOWER_LEVEL_ZB.get(), LowerLevelZbRenderer::new);
        event.registerEntityRenderer((EntityType<com.phagens.corpseorigin.entity.LongyouEntity>) EntityRegistry.LONGYOU.get(), LongyouRenderer::new);
        event.registerEntityRenderer((EntityType<com.phagens.corpseorigin.entity.GuigunEntity>) EntityRegistry.GUIGUN.get(), GuigunRenderer::new);
        event.registerEntityRenderer((EntityType<com.phagens.corpseorigin.entity.npc.KaiWeiNaiEntity>) EntityRegistry.KAIWEINAI.get(), KaiWeiNaiRenderer::new);
        event.registerEntityRenderer((EntityType<com.phagens.corpseorigin.entity.skills.LongyouEarthquakeEntity>) EntityRegistry.LONGYOU_EARTHQUAKE.get(), LongyouEarthquakeRenderer::new);
        // 尸体残肢渲染器 - 完全参照 Mob-Dismemberment 的 RenderGib
        event.registerEntityRenderer((EntityType<CorpseGibEntity>) EntityRegistry.CORPSE_GIB.get(), CorpseGibRenderer::new);
        event.registerEntityRenderer((EntityType<AlienatedSporeEntity>) EntityRegistry.ALIENATED_SPORE.get(), AlienatedSporeRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COCO_PENGUIN.get(), CocoPenguinRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COCO_ZOMBIE.get(), CocoZombieRenderer::new);

        event.registerEntityRenderer((EntityType<com.phagens.corpseorigin.GongFU.FaXiang.FaxiangEntity>) EntityRegistry.FAXIANG.get(), FaxiangRenderer::new);
        event.registerEntityRenderer(EntityRegistry.ZB_WORM.get(), ZbWormRenderer::new);
        event.registerEntityRenderer(EntityRegistry.UNCLE.get(), UncleRenderer::new);
        event.registerEntityRenderer(EntityRegistry.COCO_ZOMBIE_X.get(), CocoZombieXRenderer::new);

        // 注册蜈蚣实体渲染器（GeckoLib）
        event.registerEntityRenderer(EntityRegistry.CENTIPEDE_HEAD.get(), CentipedeHeadRenderer::new);
        event.registerEntityRenderer(EntityRegistry.CENTIPEDE_JOINT.get(), CentipedeJointRenderer::new);
    }
}
