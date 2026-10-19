package com.xiaoshi2022.corpseorigin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * 尸兄模组 1.20.1 Forge 版主入口（移植骨架）。
 * <p>
 * 本版本只移植尸变/感染系统的后端逻辑：
 * <ul>
 *   <li>原版 Mob 被尸族近战攻击或接触尸水 → 感染（QIANS 效果）</li>
 *   <li>incubation 倒计时结束 → 原 Mob 实体保留，NativeCorpseState 标记感染状态</li>
 *   <li>感染后 AI 切换为尸兄猎手（NativeCorpseTickMixin / NativeCorpseAiMixin）</li>
 *   <li>客户端叠加骨骼/器官渲染（NativeCorpseLayer）</li>
 * </ul>
 * 完整内容（LowerLevelZbEntity、技能系统、角色书等）后续补入。
 */
@Mod(CorpseOrigin.MODID)
public class CorpseOrigin {

    public static final String MODID = "corpseorigin";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CorpseOrigin(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // DeferredRegister 注册
        com.xiaoshi2022.corpseorigin.registry.ModEffects.EFFECTS.register(modEventBus);
        com.xiaoshi2022.corpseorigin.registry.ModSounds.SOUNDS.register(modEventBus);
        com.xiaoshi2022.corpseorigin.registry.ModFluids.FLUIDS.register(modEventBus);
        com.xiaoshi2022.corpseorigin.registry.ModFluids.BLOCKS.register(modEventBus);
        com.xiaoshi2022.corpseorigin.registry.ModItems.ITEMS.register(modEventBus);
        com.xiaoshi2022.corpseorigin.registry.ModTab.TABS.register(modEventBus);   // ← 新增这一行


        // Forge 事件总线：感染触发、Tick、AI 替换等生命周期事件
        MinecraftForge.EVENT_BUS.register(com.xiaoshi2022.corpseorigin.event.ModEvents.class);

        // 网络同步（尸变状态 -> 客户端渲染层）
        com.xiaoshi2022.corpseorigin.network.CorpseOriginNetwork.register();
        com.xiaoshi2022.corpseorigin.network.OrganNetwork.register();

        // 客户端配置：尸水渲染色调等
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                com.xiaoshi2022.corpseorigin.config.CorpseOriginConfig.SPEC);

        // 服务端配置：尸水浸泡感染 / 尸兄血雨（运行中改 serverconfig/corpseorigin-server.toml 即时生效）
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                com.xiaoshi2022.corpseorigin.config.CorpseOriginServerConfig.SPEC);

        LOGGER.info("CorpseOrigin 1.20.1 Forge skeleton loaded");
    }
}
