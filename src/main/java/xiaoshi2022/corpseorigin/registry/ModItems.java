//package xiaoshi2022.corpseorigin.registry;
//
//import net.minecraft.core.Registry;
//import net.minecraft.core.registries.BuiltInRegistries;
//import net.minecraft.network.chat.Component;
//import net.minecraft.resources.ResourceKey;
//import net.minecraft.world.item.CreativeModeTab;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.Items;
//import xiaoshi2022.corpseorigin.CorpseOrigin;
//import xiaoshi2022.corpseorigin.item.ByWaterBucketItem;
//import xiaoshi2022.corpseorigin.item.ByWaterBottleItem;
//
//public final class ModItems {
//
//    private ModItems() {
//    }
//
//    // ==================== 物品引用 ====================
//    // ⚠️ 声明为 public static，但不初始化（在 init() 中赋值）
//    public static Item BYWATER_BUCKET;
//    public static Item BYWATER_BOTTLE;
//    public static Item PURIFIER;
//
//    /**
//     * 在模组初始化时调用
//     * ⚠️ 26.2 起：Item 构造器会调用 effectiveDescriptionId() -> itemIdOrThrow()，
//     * 必须先在 Item.Properties 上调用 setId(ResourceKey) 设置物品 id，否则抛
//     * NullPointerException: Item id not set。
//     * （Registry.register 的 id 重载不会回填 Properties.id，因为 new Item(...) 在它之前求值）
//     */
//    public static void init() {
//        // ✅ 正确：在 Registry.register() 中直接创建，且 Properties 上调用 setId
//        BYWATER_BUCKET = Registry.register(
//                BuiltInRegistries.ITEM,
//                CorpseOrigin.id("bywater_bucket"),
//                new ByWaterBucketItem(
//                        new Item.Properties()
//                                .stacksTo(1)
//                                .setId(itemKey("bywater_bucket"))
//                )
//        );
//
//        BYWATER_BOTTLE = Registry.register(
//                BuiltInRegistries.ITEM,
//                CorpseOrigin.id("bywater_bottle"),
//                new ByWaterBottleItem(
//                        new Item.Properties()
//                                .stacksTo(1)
//                                .craftRemainder(Items.GLASS_BOTTLE)
//                                .setId(itemKey("bywater_bottle"))
//                )
//        );
//
//        PURIFIER = Registry.register(
//                BuiltInRegistries.ITEM,
//                CorpseOrigin.id("purifier"),
//                new PurifierItem(
//                        new Item.Properties()
//                                .stacksTo(16)
//                                .craftRemainder(Items.GLASS_BOTTLE)
//                                .setId(itemKey("purifier"))
//                )
//        );
//
//        CorpseOrigin.LOGGER.info("CorpseOrigin items registered");
//    }
//
//    private static ResourceKey<Item> itemKey(String path) {
//        return ResourceKey.create(BuiltInRegistries.ITEM.key(), CorpseOrigin.id(path));
//    }
//
//    // ==================== 创造物品栏 ====================
//    public static final CreativeModeTab CORPSE_ORIGIN_TAB = CreativeModeTab.builder(
//                    CreativeModeTab.Row.TOP, 8)
//            .title(Component.translatable("itemGroup.corpseorigin.main"))
//            .icon(() -> new ItemStack(BYWATER_BOTTLE))
//            .displayItems((parameters, output) -> {
//                output.accept(BYWATER_BUCKET);
//                output.accept(BYWATER_BOTTLE);
//                output.accept(PURIFIER);
//            })
//            .build();
//}