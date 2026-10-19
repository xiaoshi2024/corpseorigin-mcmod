package com.xiaoshi2022.corpseorigin.registry;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import com.xiaoshi2022.corpseorigin.item.ByWaterBottleItem;
import com.xiaoshi2022.corpseorigin.item.ByWaterBucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 1.20.1 Forge 物品注册表。只包含本次移植涉及的尸变相关物品。 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CorpseOrigin.MODID);

    public static final RegistryObject<Item> BYWATER_BUCKET = ITEMS.register("bywater_bucket",
            () -> new ByWaterBucketItem(new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static final RegistryObject<Item> BYWATER_BOTTLE = ITEMS.register("bywater_bottle",
            () -> new ByWaterBottleItem(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16)));

    public static void register() {}
}
