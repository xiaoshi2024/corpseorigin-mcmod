package com.phagens.corpseorigin;

import com.mojang.logging.LogUtils;
import com.phagens.corpseorigin.GongFU.GongFaZL.BaseGongFaItem;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaDataFactory;
import com.phagens.corpseorigin.GongFU.JsonLoader.GongFaJsonLoader;
import com.phagens.corpseorigin.GongFU.MenuTypeRegister;
import com.phagens.corpseorigin.GongFU.PackGongFu.NetworkPaketGL;
import com.phagens.corpseorigin.advancement.AdvancementEventHandler;
import com.phagens.corpseorigin.advancement.CriterionTriggerRegister;
import com.phagens.corpseorigin.event.player.playerDie;
import com.phagens.corpseorigin.player.CorpsePlayerAttachment;
import com.phagens.corpseorigin.register.*;
import com.phagens.corpseorigin.skill.CorpseEvolutionTree;
import com.phagens.corpseorigin.skill.CorpseSkillTree;
import com.phagens.corpseorigin.skill.CorpseSkills;
import com.phagens.corpseorigin.skill.SkillAttachment;
import com.phagens.corpseorigin.skill.SkillEventHandler;
import com.phagens.corpseorigin.voice.VoiceCommandRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.phagens.corpseorigin.register.Moditems.QI_XING_GUAN_ITEM;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(CorpseOrigin.MODID)
public class CorpseOrigin {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "corpseorigin";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

   // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "corpseorigin" namespace
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);


    // Creates a creative tab with the id "corpseorigin:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CORPSE_ORIGIN_TAB = CREATIVE_MODE_TABS.register("corpse_origin_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.corpseorigin")) //The language key for title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> QI_XING_GUAN_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(QI_XING_GUAN_ITEM.get());
                output.accept(Moditems.ZBR_FLESH_ITEM.get());
                output.accept(Moditems.BYWATER_BUCKET.get());
                output.accept(Moditems.BYWATER_BOTTLE.get());
                output.accept(Moditems.S_AGENT.get());
                output.accept(Moditems.BLUE_AGENT.get());
                output.accept(Moditems.NULL_S_AGENT.get());
                output.accept(Moditems.MING_JUQUE.get());

                // 添加刷怪蛋
                output.accept(Moditems.LOWER_LEVEL_ZB_SPAWN_EGG.get());
                output.accept(Moditems.LONGYOU_SPAWN_EGG.get());
                output.accept(Moditems.ZBR_FISH_SPAWN_EGG.get());
                output.accept(Moditems.KAIWEINAI_SPAWN_EGG.get());
                output.accept(Moditems.GUIGUN_SPAWN_EGG.get());

                // 添加尸兄器官掉落物
                output.accept(Moditems.DR_MU_EYE.get());
                output.accept(Moditems.ORDINARY_ZB_EYE.get());

            }).build());

    // 新增：功法专属标签页
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GONG_FA_TAB =
            CREATIVE_MODE_TABS.register("gong_fa_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.corpseorigin.gongfa"))
                    .withTabsBefore(CorpseOrigin.CORPSE_ORIGIN_TAB.getKey())
                    .icon(() -> {
                        // 延迟获取，避免类加载时出错
                        try {
                            return Moditems.BASE_GONG_FA.get().getDefaultInstance();
                        } catch (Exception e) {
                            CorpseOrigin.LOGGER.error("无法获取功法标签页图标", e);
                            // 返回一个备用图标
                            return Moditems.QI_XING_GUAN_ITEM.get().getDefaultInstance();
                        }
                    })
                    .displayItems((parameters, output) -> {
                        // 添加所有功法物品，同样使用 try-catch
                        addGongFaItemsToCreativeTab(output);
                    })
                    .build());

    // 单独提取方法，便于管理
    private static void addGongFaItemsToCreativeTab(CreativeModeTab.Output output) {
        if (GongFaJsonLoader.getInstance() == null) {
            CorpseOrigin.LOGGER.warn("⚠ GongFaJsonLoader 未初始化，跳过功法物品添加");
            CorpseOrigin.LOGGER.warn("⚠ 这可能是因为资源重载尚未完成");
            return;
        }
        Map<String, GongFaData> allData = GongFaJsonLoader.getAllGongFaData();
        if (allData.isEmpty()) {
            CorpseOrigin.LOGGER.warn("⚠ 功法数据为空，跳过添加");
            CorpseOrigin.LOGGER.warn("⚠ 请确保 JSON 文件位于 data/corpseorigin/gf_data/ 目录下");
            CorpseOrigin.LOGGER.warn("⚠ 或在游戏中使用 /reload 命令重新加载资源");
            return;
        }
        CorpseOrigin.LOGGER.info("开始添加 {} 个功法物品到创造模式标签页", allData.size());
        BaseGongFaItem baseItem = (BaseGongFaItem) Moditems.BASE_GONG_FA.get();
        List<GongFaData> sortedData = new ArrayList<>(allData.values());
        sortedData.sort(Comparator.comparingInt(GongFaData::getRarity)
                .thenComparing(GongFaData::getCeng));

        int addedCount = 0;
        for (GongFaData data : sortedData) {
            try {
                ItemStack stack = new ItemStack(baseItem);
                baseItem.setDataToItem(stack, data);
                output.accept(stack);
                addedCount++;
                CorpseOrigin.LOGGER.debug("✓ 添加功法: {} (稀有度:{}, 层数:{})",
                        data.getName(), data.getRarity(), data.getCeng());
            } catch (Exception e) {
                CorpseOrigin.LOGGER.error("✗ 创建功法物品失败：{}", data.getTypeId(), e);
            }
        }

        CorpseOrigin.LOGGER.info("功法物品添加完成，成功添加 {} 个物品", addedCount);
    }

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public CorpseOrigin(IEventBus modEventBus, ModContainer modContainer) {

        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                CorpseOrigin.LOGGER.info("【客户端构造】开始强制加载功法JSON数据...");
                com.phagens.corpseorigin.GongFU.JsonLoader.GongFaJsonLoader.forceLoadFromResources();
                CorpseOrigin.LOGGER.info("【客户端构造】功法数据强制加载完成");
            } catch (Exception e) {
                CorpseOrigin.LOGGER.error("【客户端构造】功法数据强制加载失败", e);
            }
        }
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        //json解析
        NeoForge.EVENT_BUS.addListener(CorpseOrigin::addReloadListeners);

        // 先注册附件系统 - 重要！
        SkillAttachment.ATTACHMENT_TYPES.register(modEventBus);

        // 注册成就触发器
        CriterionTriggerRegister.TRIGGER_TYPES.register(modEventBus);

        Moditems.ITEMS.register(modEventBus);

        // 先注册实体，因为方块可能依赖实体
        EntityRegistry.ENTITIES.register(modEventBus);

        BlockRegistry.Blocks.register(modEventBus);
        BlockEntityRegistry.BLOCK_ENTITIES.register(modEventBus);
        EffectRegister.MOB_EFFECTS.register(modEventBus);
        MenuTypeRegister.MENUS.register(modEventBus);
        CorpsePlayerAttachment.ATTACHMENT_TYPES.register(modEventBus);

        // 注册音效
        ModSounds.register(modEventBus);

        // 初始化技能系统
        CorpseSkills.init();
        CorpseEvolutionTree.init();
        CorpseSkillTree.init();

        NetworkPaketGL.registerPackets(modEventBus);

        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        //死亡移除
        NeoForge.EVENT_BUS.register(playerDie.class);

        // 注册技能事件处理器
        NeoForge.EVENT_BUS.register(SkillEventHandler.class);

        // 注册成就事件处理器
        NeoForge.EVENT_BUS.register(AdvancementEventHandler.class);

        // 注册语音命令
        NeoForge.EVENT_BUS.register(VoiceCommandRegistration.class);

        // 初始化内置语音触发系统（仅在客户端）
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.phagens.corpseorigin.voice.VoiceTriggerIntegration.init(modEventBus, modContainer);
        }

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::onClientSetup);

        CorpseOrigin.LOGGER.info("CorpseOrigin 模组初始化完成 [环境: {}]", FMLEnvironment.dist);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }



    /**
     * 客户端设置事件 - 确保功法数据在连接服务器前已加载
     */
    private void onClientSetup(FMLClientSetupEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CorpseOrigin.LOGGER.info("【客户端初始化】触发功法数据预加载");

            // 延迟执行，确保资源管理器已就绪
            event.enqueueWork(() -> {
                try {
                    // 强制触发一次资源重载
                    if (net.minecraft.client.Minecraft.getInstance().getResourceManager() != null) {
                        CorpseOrigin.LOGGER.info("【客户端初始化】资源管理器已就绪，等待首次资源重载...");
                    }
                } catch (Exception e) {
                    CorpseOrigin.LOGGER.error("【客户端初始化】检查资源管理器失败", e);
                }
            });
        }
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        GongFaJsonLoader.register(event);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        // Config fields removed - add them back in Config.java if needed
        // if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
        //     LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        // }
        // LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());
        // Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }


//    // 将示例块项添加到构建块标签页中
//    private void addCreative(BuildCreativeModeTabContentsEvent event) {
//        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
//            event.accept(QI_XING_GUAN_ITEM);
//        }
//    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
