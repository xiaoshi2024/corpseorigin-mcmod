package com.xiaoshi2022.corpseorigin.registry;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 创造模式标签页注册（1.20.1 Forge）。
 * <p>
 * 对应 26.2 Fabric 版的 CreativeModeTab 注册；1.20.1 用
 * {@link Registries#CREATIVE_MODE_TAB} + {@link CreativeModeTab.Builder}。
 * <p>
 * 图标用尸水桶，标题走 lang 文件 key：
 * {@code itemGroup.corpseorigin.main}。
 */
public final class ModTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CorpseOrigin.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("corpseorigin_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> ModItems.BYWATER_BUCKET.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.corpseorigin.main"))
                    .displayItems((parameters, output) -> {
                        // 尸水系列
                        output.accept(ModItems.BYWATER_BUCKET.get());
                        output.accept(ModItems.BYWATER_BOTTLE.get());
                        // TODO: 后续新增物品在此追加 output.accept(...)
                    })
                    .build());

    private ModTab() {}
}