package com.xiaoshi2022.corpseorigin.item;

import net.minecraft.world.item.BucketItem;
import com.xiaoshi2022.corpseorigin.registry.ModFluids;

/** 尸水桶：倒入水源 = 转化为尸水流体。父类 BucketItem 已完整实现逻辑。 */
public class ByWaterBucketItem extends BucketItem {
    public ByWaterBucketItem(Properties properties) {
        super(ModFluids.INFECTED_WATER.get(), properties);
    }
}
