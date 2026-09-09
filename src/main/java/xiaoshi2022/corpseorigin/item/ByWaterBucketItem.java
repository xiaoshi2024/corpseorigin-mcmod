package xiaoshi2022.corpseorigin.item;

import net.minecraft.world.item.BucketItem;
import xiaoshi2022.corpseorigin.registry.ModFluids;

public class ByWaterBucketItem extends BucketItem {

    public ByWaterBucketItem(Properties properties) {
        // ✅ 传入尸水作为内容，父类会处理所有逻辑
        super(ModFluids.INFECTED_WATER, properties);
    }

    // ❌ 不需要重写 use 方法！
    // ❌ 不需要重写 emptyContents 方法！
    // 父类已经完整实现了所有逻辑
}