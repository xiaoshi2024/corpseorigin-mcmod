package com.phagens.corpseorigin.GongFU;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.Sceen.GongFuMenu;
import com.phagens.corpseorigin.client.gui.TechniqueSwapTable.TechniqueSwapTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
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

    /**
     * 功法兑换台菜单
     * 使用 IContainerFactory 支持额外的数据包参数
     * 这样可以传递 BlockEntity 的位置信息
     */
    public static final DeferredHolder<MenuType<?>, MenuType<TechniqueSwapTableMenu>> TECHNIQUE_SWAP_TABLE_MENU =
            MENUS.register("technique_swap_table_menu", () -> new MenuType<>(
                    (IContainerFactory) (containerId, inventory, friendlyByteBuf) -> {
                        // 从网络缓冲区读取 BlockPos
                        var pos = friendlyByteBuf.readBlockPos();
                        // 从客户端世界获取 BlockEntity
                        if (inventory.player.level().getBlockEntity(pos) instanceof com.phagens.corpseorigin.block.entity.TechniqueSwapTableEntity entity) {
                            return new com.phagens.corpseorigin.client.gui.TechniqueSwapTable.TechniqueSwapTableMenu(containerId, inventory, entity);
                        }
                        return null;
                    },
                    FeatureFlags.DEFAULT_FLAGS
            ));
}
