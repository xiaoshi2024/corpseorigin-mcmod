package com.phagens.corpseorigin.GongFU;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.Sceen.GongFuMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MenuTypeRegister {
    // 在 ITEMS 注册后面添加菜单注册
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CorpseOrigin.MODID);
    // 正确的注册方式 - 使用两个参数的构造函数
    public static final DeferredHolder<MenuType<?>, MenuType<GongFuMenu>> GONG_FU_MENU =
            MENUS.register("null", () -> new MenuType<>(
                    GongFuMenu::new,  // MenuSupplier - 匹配 (int, Inventory) -> GongFuMenu
                    FeatureFlags.DEFAULT_FLAGS  // FeatureFlagSet
            ));


}
