package com.phagens.corpseorigin.register;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.Item.*;
import com.phagens.corpseorigin.Item.Organic.DrMuEyeItem;
import com.phagens.corpseorigin.Item.Organic.OrdinaryZbEyeItem;
import com.phagens.corpseorigin.Item.YaoJi.Sagent;
import com.phagens.corpseorigin.Item.tier.Modtiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class Moditems {
    public static final ResourceLocation BASE_ATTACK_GRA_ID = ResourceLocation.withDefaultNamespace("base_attack_gra");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CorpseOrigin.MODID);

    public static final DeferredItem<Item> MING_JUQUE = ITEMS.register("ming_juque", () -> new JuQue(Modtiers.MingJian,3,-2.4f, new Item.Properties()));
    
    // 巨阙2阶
    public static final DeferredItem<Item> MING_JUQUE_TW = ITEMS.register("ming_juque_tw", () -> new JuQue(Modtiers.MingJian,5,-2.2f, new Item.Properties(), "tw"));

    public static final DeferredItem<BlockItem> QI_XING_GUAN_ITEM = ITEMS.register("qi_xings_guan_item",
            () -> new BlockItem(BlockRegistry.QI_XING_GUAN.get(), new Item.Properties()));

    // 尸兄肉块物品
    public static final DeferredItem<BlockItem> ZBR_FLESH_ITEM = ITEMS.register("zbr_flesh_item",
            () -> new BlockItem(BlockRegistry.ZBR_FLESH.get(), new Item.Properties()));

    // 异化碎块物品
    public static final DeferredItem<BlockItem> ALIENATED_FRAGMENT_ITEM = ITEMS.register("alienated_fragment_item",
            () -> new BlockItem(BlockRegistry.ALIENATED_FRAGMENT.get(), new Item.Properties()));

    public static final DeferredItem<Item> BYWATER_BUCKET = ITEMS.register("bywater_bucket",
            () -> new ByWaterBucketItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> BYWATER_BOTTLE = ITEMS.register("bywater_bottle",
            () -> new ByWaterBottleItem(new Item.Properties().stacksTo(1)));
    // 黄色强化剂 - 有副作用，需要蓝色中和剂
    public static final DeferredItem<Item> S_AGENT= ITEMS.register("s_agent",
            () -> new Sagent(new Item.Properties(), "yellow")
                    .addAttributeModifier(Attributes.MAX_HEALTH,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    , 0.5,"yellow"));

    // 蓝色中和剂 - 用于清除黄色强化剂的副作用
    public static final DeferredItem<Item> BLUE_AGENT= ITEMS.register("blue_s_agent",
            () -> new Sagent(new Item.Properties(), "blue"));

    // 其他变种药剂
    public static final DeferredItem<Item> NULL_S_AGENT= ITEMS.register("null_s_agent",
            () -> new Sagent(new Item.Properties(), "null")
                    .addAttributeModifier(Attributes.MAX_HEALTH,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    , 0.3,"null"));
    //这里 你去看item包里的Yaoji  然后 你看addAttributeModifier方法里面 传入参数 即可



    // 原著功法体系
    public static final DeferredItem<Item> BASE_GONG_FA = ITEMS.register("base_gong_fa",
            () -> new BaseGongFa(new Item.Properties(), "BASE"));

    // 刷怪蛋
    public static final DeferredItem<Item> LOWER_LEVEL_ZB_SPAWN_EGG = ITEMS.register("lower_level_zb_spawn_egg",
            () -> new SpawnEggItem(EntityRegistry.LOWER_LEVEL_ZB.get(), 0x8B4513, 0xFF0000, new Item.Properties()));

    public static final DeferredItem<Item> LONGYOU_SPAWN_EGG = ITEMS.register("longyou_spawn_egg",
            () -> new SpawnEggItem(EntityRegistry.LONGYOU.get(), 0x000000, 0xFFD700, new Item.Properties()));

    public static final DeferredItem<Item> ZBR_FISH_SPAWN_EGG = ITEMS.register("zbr_fish_spawn_egg",
            () -> new SpawnEggItem(EntityRegistry.ZBR_FISH.get(), 0x4682B4, 0x8B0000, new Item.Properties()));

    // 开胃奶刷怪蛋
    public static final DeferredItem<Item> KAIWEINAI_SPAWN_EGG = ITEMS.register("kaiweinai_spawn_egg",
            () -> new SpawnEggItem(EntityRegistry.KAIWEINAI.get(), 0xFF0000, 0x8B0000, new Item.Properties()));

    // 鬼棍刷怪蛋
    public static final DeferredItem<Item> GUIGUN_SPAWN_EGG = ITEMS.register("guigun_spawn_egg",
            () -> new SpawnEggItem(EntityRegistry.GUIGUN.get(), 0x800080, 0x00FF00, new Item.Properties()));

    // ========== 尸兄器官掉落物 ==========
    // 普通尸眼 - 尸兄掉落物，食用可进化夜视能力
    public static final DeferredItem<Item> ORDINARY_ZB_EYE = ITEMS.register("ordinary_zb_eye",
            () -> new OrdinaryZbEyeItem());

    // 穆博士的眼睛 - 稀有掉落物，失去意识的尸兄食用后可恢复人类智慧
    public static final DeferredItem<Item> DR_MU_EYE = ITEMS.register("dr_mu_eye",
            () -> new DrMuEyeItem());

    // ========== 任务系统物品 ==========
    // 任务纸条 - 用于追踪任务进度
    public static final DeferredItem<Item> MISSION_SCROLL = ITEMS.register("mission_scroll",
            () -> new MissionScrollItem());

    // ========== 尸兄主题物品 ==========
    // 吹风机 - 尸兄模组第一个主题物品，丢进水里会放电，右键使用给幸运buff
    public static final DeferredItem<Item> HAIR_DRYER = ITEMS.register("hair_dryer",
            () -> new HairDryerItem(new Item.Properties().durability(90)));
}




















