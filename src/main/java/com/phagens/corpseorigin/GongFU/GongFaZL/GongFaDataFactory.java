package com.phagens.corpseorigin.GongFU.GongFaZL;

import com.phagens.corpseorigin.GongFU.JsonLoader.GongFaJsonLoader;
import net.minecraft.world.item.ItemStack;
//功法真数据
public class GongFaDataFactory {

    // 从 JSON 加载的数据创建功法物品
    public static ItemStack createGongFaItem(BaseGongFaItem item, int rarity, String ceng) {
        ItemStack stack = new ItemStack(item);

        // 优先从 JSON 加载器获取数据
        GongFaData data = GongFaJsonLoader.getGongFaData(item.gongFaType, rarity, ceng);

        if (data == null) {
            throw new IllegalArgumentException(String.format(
                    "未找到功法数据 - 类型：%s, 稀有度：%d, 层数：%s\n请检查 JSON 文件是否正确创建",
                    item.gongFaType, rarity, ceng ));
        }

        item.setDataToItem(stack, data);
        return stack;
    }
}
