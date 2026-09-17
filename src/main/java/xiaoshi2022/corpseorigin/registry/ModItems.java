package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.ArmorType;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.HeiXiaoFei;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.item.ByWaterBottleItem;
import xiaoshi2022.corpseorigin.item.ByWaterBucketItem;
import xiaoshi2022.corpseorigin.item.CharacterBookItem;
import xiaoshi2022.corpseorigin.item.armor.AntennaZBRitem;
import xiaoshi2022.corpseorigin.item.sword.JuQue;
import xiaoshi2022.corpseorigin.item.weapon.BloodLotusLamp;

import java.util.function.Supplier;

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
    
    //尸兄阵营天线宝宝尸兄盔甲
    public static final Supplier<AntennaZBRitem> ANTENNA_ZBR_ARMOR_HELMET = register(
            "antenna_zbr_armor_helmet",
            () -> new AntennaZBRitem(
                    ArmorMaterialRegistry.ZBR_ARMOR_MATERIAL,
                    ArmorType.HELMET,
                    new Item.Properties().setId(itemKey("antenna_zbr_armor_helmet"))
            )
    );

    public static final Supplier<AntennaZBRitem> ANTENNA_ZBR_ARMOR_CHESTPLATE = register(
            "antenna_zbr_armor_chestplate",
            () -> new AntennaZBRitem(
                    ArmorMaterialRegistry.ZBR_ARMOR_MATERIAL,
                    ArmorType.CHESTPLATE,
                    new Item.Properties().setId(itemKey("antenna_zbr_armor_chestplate"))
            )
    );

    public static final Supplier<AntennaZBRitem> ANTENNA_ZBR_ARMOR_LEGGINGS = register(
            "antenna_zbr_armor_leggings",
            () -> new AntennaZBRitem(
                    ArmorMaterialRegistry.ZBR_ARMOR_MATERIAL,
                    ArmorType.LEGGINGS,
                    new Item.Properties().setId(itemKey("antenna_zbr_armor_leggings"))
            )
    );

    /** 角色选择书（所有角色共用，靠 character_id 数据组件区分） */
    public static final Item CHARACTER_BOOK = register(
            "character_book",
            new CharacterBookItem(new Item.Properties()
                    .stacksTo(1)
                    .setId(itemKey("character_book")))
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
                output.accept(ANTENNA_ZBR_ARMOR_HELMET.get());
                output.accept(ANTENNA_ZBR_ARMOR_CHESTPLATE.get());
                output.accept(ANTENNA_ZBR_ARMOR_LEGGINGS.get());
            })
            .build();

    /**
     * 角色选择书页签：装入了全部已注册角色的书。
     * <p>
     * ⚠️ 必须保持默认的 {@link CreativeModeTab.Type#CATEGORY} 类型。
     * 原版搜索框的索引（{@code SessionSearchTrees.updateCreativeTooltips}）只从「搜索」页签的
     * displayItems 构建，而「搜索」页签的内容生成器会跳过所有 SEARCH 类型页签；
     * 同时 Fabric 的 {@code CreativeModeTabEvents} 也明确跳过 alignedRight 的页签（搜索页签就是），
     * 没法往里面补物品。所以书必须放在普通页签里，才能被搜索到。
     */
    public static final CreativeModeTab CHARACTER_BOOK_TAB = CreativeModeTab.builder(
                    CreativeModeTab.Row.TOP, 7)
            .title(Component.translatable("itemGroup.corpseorigin.character_books"))
            .icon(() -> CharacterBookItem.createStack(HeiXiaoFei.ID))
            .displayItems((parameters, output) -> {
                for (ICharacter character : CharacterManager.getInstance().getRegisteredCharacters()) {
                    output.accept(CharacterBookItem.createStack(character.getId()));
                }
            })
            .build();

    private static Item register(String name, Item item) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                CorpseOrigin.id(name),
                item
        );
    }

    // 新增这个重载
    private static <T extends Item> Supplier<T> register(String name, Supplier<T> itemSupplier) {
        T item = itemSupplier.get();
        Registry.register(BuiltInRegistries.ITEM, CorpseOrigin.id(name), item);
        return () -> item;
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(BuiltInRegistries.ITEM.key(), CorpseOrigin.id(path));
    }

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin items registered");
    }
}