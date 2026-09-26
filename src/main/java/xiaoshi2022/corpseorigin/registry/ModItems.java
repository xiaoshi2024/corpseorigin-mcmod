package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.equipment.ArmorType;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.item.*;
import xiaoshi2022.corpseorigin.item.armor.AntennaZBRitem;
import xiaoshi2022.corpseorigin.item.armor.LongYouClothItem;
import xiaoshi2022.corpseorigin.item.armor.XiaoluArmorItem;
import xiaoshi2022.corpseorigin.item.sword.JuQue;
import xiaoshi2022.corpseorigin.item.weapon.BloodLotusLamp;

import java.util.function.Supplier;

public final class ModItems {
    public static final Item TIAN_GANG_KEY = register("tian_gang_key",
            new xiaoshi2022.corpseorigin.item.weapon.TianGangKeyItem(new Item.Properties()
                    .stacksTo(1).rarity(Rarity.EPIC).setId(itemKey("tian_gang_key"))));
    public static final Item BLACK_GOLD_HEART = register("black_gold_heart", new BlackGoldHeartItem(
            new Item.Properties().stacksTo(1).setId(itemKey("black_gold_heart"))));
    public static final Item BEE_WHEEL=register("bee_wheel",new BeeWheelItem(new Item.Properties()
            .sword(ToolMaterial.DIAMOND,8,-2.4f).stacksTo(1).setId(itemKey("bee_wheel"))));
    public static final Item DR_MU_EYE = register("dr_mu_eye", new Item(new Item.Properties().setId(itemKey("dr_mu_eye"))));
    public static final Item RED_METEOR_SWORD = register("red_meteor_sword",new Item(new Item.Properties().sword(ToolMaterial.DIAMOND,3,-2.4f).setId(itemKey("red_meteor_sword"))));
    public static final Item PARCEL_BOMB = register("parcel_bomb",new Item(new Item.Properties().setId(itemKey("parcel_bomb"))));
    public static final Item BILLIARD_EIGHT = register("billiard_eight",new BilliardEightItem(new Item.Properties().setId(itemKey("billiard_eight"))));
    public static final Item BLOOD_WING_BLADE = register("blood_wing_blade",
            new Item(new Item.Properties().sword(ToolMaterial.DIAMOND,22f,-2.4f)
                    .rarity(Rarity.EPIC).setId(itemKey("blood_wing_blade"))));

    public static final Item CN_CHESS_ZBRS = register(
            "cn_chess_zbrs",
            new BlockItem(ModBlocks.CN_CHESS_ZBRS, new Item.Properties()
                    .setId(itemKey("cn_chess_zbrs")))
    );

    /** 鬼棍·人类的三节棍：五六米长的三段棍身，GeoItem 三维模型 */
    public static final Item GUIGUN_WEAP = register("guigun_weap",
            new xiaoshi2022.corpseorigin.item.weapon.GuigunWeapItem(new Item.Properties()
                    .sword(ToolMaterial.DIAMOND,6f,-2.8f)
                    .rarity(Rarity.EPIC).setId(itemKey("guigun_weap"))));

    /** 鬼棍·尸兄的棍棒：布满骷髅头的次声波尸棍，GeoItem 三维模型 */
    public static final Item GUIGUN_CLUB = register("guigun_club",
            new xiaoshi2022.corpseorigin.item.weapon.GuigunClubItem(new Item.Properties()
                    .sword(ToolMaterial.DIAMOND,5f,-3f)
                    .rarity(Rarity.EPIC).setId(itemKey("guigun_club"))));

    // ==================== 黑色火线·强化药剂（插入心脏注射） ====================

    /** 黄色强化剂：注射后永久 +50% 最大生命，境界随机冲上人3 或 人4；副作用照常累积 */
    public static final Item S_AGENT = register("s_agent",
            new SagentItem(new Item.Properties().rarity(Rarity.RARE).setId(itemKey("s_agent")), SagentItem.YELLOW)
                    .addAttributeModifier(Attributes.MAX_HEALTH,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.5, SagentItem.MODIFIER_YELLOW)
                    .promoteChance(3, 50)
                    .promoteChance(4, 50));

    /** 蓝色中和剂：下调黄色强化剂的副作用等级 */
    public static final Item BLUE_S_AGENT = register("blue_s_agent",
            new SagentItem(new Item.Properties().rarity(Rarity.RARE).setId(itemKey("blue_s_agent")), SagentItem.BLUE));

    /** 空药剂：注射后留下的空瓶，也是后续黑色火线仪器的原料 */
    public static final Item NULL_S_AGENT = register("null_s_agent",
            new SagentItem(new Item.Properties().setId(itemKey("null_s_agent")), SagentItem.EMPTY)
                    .addAttributeModifier(Attributes.MAX_HEALTH,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.3, SagentItem.MODIFIER_EMPTY));

    public static final Item MEDUSA_EYE = register("medusa_eye",
            new MedusaEyeItem(new Item.Properties()
                    .setId(itemKey("medusa_eye"))
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(0)
                            .saturationModifier(0f)
                            .alwaysEdible()
                            .build())));

    public static final Item MAGICIAN_RABBIT = register("magician_rabbit",
            new xiaoshi2022.corpseorigin.item.MagicianRabbitItem(
                    new Item.Properties().stacksTo(16).setId(itemKey("magician_rabbit"))));
    public static final Item DOG_CAGE = register("dog_cage",
            new xiaoshi2022.corpseorigin.item.DogCageItem(new Item.Properties().stacksTo(1).setId(itemKey("dog_cage"))));

    public static final Item HAM_SPAWN_EGG = register("ham_spawn_egg",
            new SpawnEggItem(spawnEggProperties("ham_spawn_egg", ModEntities.HAM)));

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

    /** 尸兄肉块（尸巢的基本建筑方块，外观由 GeckoLib 的 BER 渲染） */
    public static final Item ZBR_FLESH = register(
            "zbr_flesh",
            new BlockItem(ModBlocks.ZBR_FLESH, new Item.Properties()
                    .useBlockDescriptionPrefix()
                    .setId(itemKey("zbr_flesh")))
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

    // ==================== 尸王专属服装（龙右） ====================
    // 模型/动画/贴图见 assets/corpseorigin/geckolib/.../armor/longyoucloth.*

    public static final Supplier<LongYouClothItem> LONGYOU_CLOTH_HELMET = register(
            "longyou_cloth_helmet",
            () -> new LongYouClothItem(
                    ArmorMaterialRegistry.LONGYOU_ARMOR_MATERIAL,
                    ArmorType.HELMET,
                    new Item.Properties().setId(itemKey("longyou_cloth_helmet"))
            )
    );

    public static final Supplier<LongYouClothItem> LONGYOU_CLOTH_CHESTPLATE = register(
            "longyou_cloth_chestplate",
            () -> new LongYouClothItem(
                    ArmorMaterialRegistry.LONGYOU_ARMOR_MATERIAL,
                    ArmorType.CHESTPLATE,
                    new Item.Properties().setId(itemKey("longyou_cloth_chestplate"))
            )
    );

    public static final Supplier<LongYouClothItem> LONGYOU_CLOTH_LEGGINGS = register(
            "longyou_cloth_leggings",
            () -> new LongYouClothItem(
                    ArmorMaterialRegistry.LONGYOU_ARMOR_MATERIAL,
                    ArmorType.LEGGINGS,
                    new Item.Properties().setId(itemKey("longyou_cloth_leggings"))
            )
    );

    /** 角色选择书（所有角色共用，靠 character_id 数据组件区分） */
    public static final Item CHARACTER_BOOK = register(
            "character_book",
            new CharacterBookItem(new Item.Properties()
                    .stacksTo(1)
                    .setId(itemKey("character_book")))
    );

    /**
     * 角色记忆书 —— 死亡夺舍时旧身体无处安放，就把它的角色数据（身份 / 已学技能 / 进化点）
     * 封成这本书掉在死亡点。只能由原主人使用，详见 {@code CharacterMemoryItem}。
     */
    public static final Item CHARACTER_MEMORY = register(
            "character_memory",
            new xiaoshi2022.corpseorigin.item.CharacterMemoryItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE)
                    .setId(itemKey("character_memory")))
    );

    // ==================== CoCo 企鹅系刷怪蛋 ====================

    public static final Item COCO_PENGUIN_SPAWN_EGG = register(
            "coco_penguin_spawn_egg",
            new SpawnEggItem(spawnEggProperties("coco_penguin_spawn_egg", ModEntities.COCO_PENGUIN))
    );

    public static final Item COCO_ZOMBIE_SPAWN_EGG = register(
            "coco_zombie_spawn_egg",
            new SpawnEggItem(spawnEggProperties("coco_zombie_spawn_egg", ModEntities.COCO_ZOMBIE))
    );

    public static final Item COCO_ZOMBIE_X_SPAWN_EGG = register(
            "coco_zombie_x_spawn_egg",
            new SpawnEggItem(spawnEggProperties("coco_zombie_x_spawn_egg", ModEntities.COCO_ZOMBIE_X))
    );

    public static final Item UNCLE_SPAWN_EGG = register(
            "uncle_spawn_egg",
            new SpawnEggItem(spawnEggProperties("uncle_spawn_egg", ModEntities.UNCLE))
    );

    public static final Item ZB_WORM_SPAWN_EGG = register(
            "zb_worm_spawn_egg",
            new SpawnEggItem(spawnEggProperties("zb_worm_spawn_egg", ModEntities.ZB_WORM))
    );

    public static final Item MIKU_ZB_SPAWN_EGG = register(
            "miku_zb_spawn_egg",
            new SpawnEggItem(spawnEggProperties("miku_zb_spawn_egg", ModEntities.MIKU_ZB))
    );
    public static final Supplier<XiaoluArmorItem> XIAOLU_ARMOR_HELMET = register("xiaolu_armor_helmet", () -> new XiaoluArmorItem(ArmorMaterialRegistry.LONGYOU_ARMOR_MATERIAL, ArmorType.HELMET, new Item.Properties().setId(itemKey("xiaolu_armor_helmet"))));
    public static final Supplier<XiaoluArmorItem> XIAOLU_ARMOR_CHESTPLATE = register("xiaolu_armor_chestplate", () -> new XiaoluArmorItem(ArmorMaterialRegistry.LONGYOU_ARMOR_MATERIAL, ArmorType.CHESTPLATE, new Item.Properties().setId(itemKey("xiaolu_armor_chestplate"))));
    public static final Supplier<XiaoluArmorItem> XIAOLU_ARMOR_LEGGINGS = register("xiaolu_armor_leggings", () -> new XiaoluArmorItem(ArmorMaterialRegistry.LONGYOU_ARMOR_MATERIAL, ArmorType.LEGGINGS, new Item.Properties().setId(itemKey("xiaolu_armor_leggings"))));

    public static final Item LOWER_LEVEL_ZB_SPAWN_EGG = register(
            "lower_level_zb_spawn_egg",
            new SpawnEggItem(spawnEggProperties("lower_level_zb_spawn_egg", ModEntities.LOWER_LEVEL_ZB))
    );

    /** 尸兄虫 - 可食用，也是企鹅变尸兄的引子 */
    public static final Item ZB_WORM_ITEM = register(
            "zb_worm_item",
            new ZbWormItem(new Item.Properties().setId(itemKey("zb_worm_item")))
    );

    /** 大葱 - 初音尸兄的远程投掷物材料，可作食物 */
    public static final Item LEEK = register(
            "leek",
            new Item(new Item.Properties()
                    .setId(itemKey("leek"))
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(2).saturationModifier(0.3F).build()))
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
                output.accept(BLOOD_WING_BLADE);
                output.accept(GUIGUN_WEAP);
                output.accept(GUIGUN_CLUB);
                output.accept(S_AGENT);
                output.accept(BLUE_S_AGENT);
                output.accept(NULL_S_AGENT);
                output.accept(BEE_WHEEL);
                output.accept(BLACK_GOLD_HEART);
                output.accept(RED_METEOR_SWORD);
                output.accept(PARCEL_BOMB);
                output.accept(BILLIARD_EIGHT);
                output.accept(BLOOD_LOTUS_LAMP);
                output.accept(TIAN_GANG_KEY);
                output.accept(CLONE_CHAMBER);
                output.accept(ZBR_FLESH);
                output.accept(ANTENNA_ZBR_ARMOR_HELMET.get());
                output.accept(ANTENNA_ZBR_ARMOR_CHESTPLATE.get());
                output.accept(ANTENNA_ZBR_ARMOR_LEGGINGS.get());
                output.accept(LONGYOU_CLOTH_HELMET.get());
                output.accept(LONGYOU_CLOTH_CHESTPLATE.get());
                output.accept(LONGYOU_CLOTH_LEGGINGS.get());
                output.accept(XIAOLU_ARMOR_HELMET.get());
                output.accept(XIAOLU_ARMOR_CHESTPLATE.get());
                output.accept(XIAOLU_ARMOR_LEGGINGS.get());
                output.accept(COCO_PENGUIN_SPAWN_EGG);
                output.accept(HAM_SPAWN_EGG);
                output.accept(DOG_CAGE);
                output.accept(MAGICIAN_RABBIT);
                output.accept(MEDUSA_EYE);
                output.accept(DR_MU_EYE);
                output.accept(COCO_ZOMBIE_SPAWN_EGG);
                output.accept(COCO_ZOMBIE_X_SPAWN_EGG);
                output.accept(UNCLE_SPAWN_EGG);
                output.accept(ZB_WORM_SPAWN_EGG);
                output.accept(MIKU_ZB_SPAWN_EGG);
                output.accept(LOWER_LEVEL_ZB_SPAWN_EGG);
                output.accept(ZB_WORM_ITEM);
                output.accept(CHARACTER_MEMORY);
                output.accept(LEEK);
                output.accept(CN_CHESS_ZBRS);
            })
            .build();

    /**
     * 角色选择书页签：第一本是<b>统一角色书</b>（右键打开选人界面，选完消失），
     * 之后是各角色的<b>定向书</b>（右键直接切成书里写的那个角色）。
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
            .icon(CharacterBookItem::createUnboundStack)
            .displayItems((parameters, output) -> {
                // 第一本：统一角色书 —— 右键打开选人界面，从全部已注册角色里挑一位，选完书就消失
                output.accept(CharacterBookItem.createUnboundStack());
                // 之后是"零散"的定向书：右键直接切成书里写的那个角色（成就奖励 / 指令 / 整合包发单角色用）
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

    /**
     * 26.2 的刷怪蛋不再自带颜色，实体类型改由 entity_data 组件携带。
     */
    private static Item.Properties spawnEggProperties(String id, EntityType<?> type) {
        return new Item.Properties()
                .setId(itemKey(id))
                .component(DataComponents.ENTITY_DATA, TypedEntityData.of(type, new CompoundTag()));
    }

    public static void init() {
        CorpseOrigin.LOGGER.info("CorpseOrigin items registered");
    }
}
