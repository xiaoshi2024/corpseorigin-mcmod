package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.item.ByWaterBottleItem;
import xiaoshi2022.corpseorigin.item.ByWaterBucketItem;
import xiaoshi2022.corpseorigin.item.sword.JuQue;
import xiaoshi2022.corpseorigin.item.weapon.BloodLotusLamp;

public final class ModItems {

    private ModItems() {
    }

    // ✅ 保持原来的 ID：bywater_bucket
    public static final Item BYWATER_BUCKET = register(
            "bywater_bucket",
            new ByWaterBucketItem(new Item.Properties()
                    .stacksTo(1)
                    .craftRemainder(Items.BUCKET)
                    .setId(itemKey("bywater_bucket")))
    );

    // ✅ 保持原来的 ID：bywater_bottle
    public static final Item BYWATER_BOTTLE = register(
            "bywater_bottle",
            new ByWaterBottleItem(new Item.Properties()
                    .stacksTo(16)
                    .craftRemainder(Items.GLASS_BOTTLE)
                    .setId(itemKey("bywater_bottle")))
    );

    public static final Item JUQUE_TW = register(
            "juque_tw",
            JuQue.create(itemKey("juque_tw"))
    );

    public static final Item BLOOD_LOTUS_LAMP = register(
            "blood_lotus_lantern",
            BloodLotusLamp.create(itemKey("blood_lotus_lantern"))
    );

    /** 黑色火线克隆仓方块物品（名称沿用方块的翻译键） */
    public static final Item CLONE_CHAMBER = register(
            "clone_chamber",
            new BlockItem(ModBlocks.CLONE_CHAMBER, new Item.Properties()
                    .useBlockDescriptionPrefix()
                    .setId(itemKey("clone_chamber")))
    );

    // ==================== 创造物品栏 ====================
    public static final CreativeModeTab CORPSE_ORIGIN_TAB = CreativeModeTab.builder(
                    CreativeModeTab.Row.TOP, 8)
            .title(Component.translatable("itemGroup.corpseorigin.main"))
            .icon(() -> new ItemStack(BYWATER_BOTTLE))
            .displayItems((parameters, output) -> {
                output.accept(BYWATER_BUCKET);
                output.accept(BYWATER_BOTTLE);
                output.accept(JUQUE_TW);
                output.accept(BLOOD_LOTUS_LAMP);
                output.accept(CLONE_CHAMBER);
            })
            .build();

    private static Item register(String name, Item item) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                CorpseOrigin.id(name),
                item
        );
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(BuiltInRegistries.ITEM.key(), CorpseOrigin.id(path));
    }

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin items registered");
    }
}