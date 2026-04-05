package com.phagens.corpseorigin.GongFU.ModUtlis;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.GongFaZL.BaseGongFaItem;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;

public class GongFUDataUtlis {
    public static final String CONTAINER_KEY = "GongFuContainer";
    private static final String ATTRIBUTE_MODIFIER_PREFIX = "gong_fa_";


    /**
     * 从玩家对象读取修行容器数据
     */
    public static NonNullList<ItemStack> getGongFuItems(Player player) {
        CompoundTag playerData = player.getPersistentData();    //持久化获取
        CompoundTag containerData = playerData.getCompound(CONTAINER_KEY);//获取容器数据

        NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);

        if (!containerData.isEmpty()) {  // 如果存在保存的数据
            ContainerHelper.loadAllItems(containerData, items, player.registryAccess());
        }

        return items;
    }

    /**
     * 检查特定物品是否在修行容器中
     */
    public static boolean hasItem(Player player, Item item) {
        NonNullList<ItemStack> items = getGongFuItems(player);
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取特定物品的数量
     */
    public static int getItemCount(Player player, Item item) {
        NonNullList<ItemStack> items = getGongFuItems(player);
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * 服务端专用：从服务器玩家读取数据
     */
    public static NonNullList<ItemStack> getGongFuItemsFromServer(ServerPlayer player) {
        return getGongFuItems(player);
    }

    /**
     * 获取容器中所有功法数据的属性总和
     * 只计算容器内的功法，不在容器的功法不生效
     */
    public static Map<String, Double> getTotalAttributes(Player player) {
        NonNullList<ItemStack> items = getGongFuItems(player);
        Map<String, Double> totalAttributes = new HashMap<>();

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BaseGongFaItem gongFaItem) {
                GongFaData data = gongFaItem.getDataFromItem(stack);
                if (data != null) {
                    double cengMultiplier = getCengMultiplier(data.getCeng());
                    data.getAttributes().forEach((attr, value) -> {
                        // 属性值 * 层级倍率
                        double boostedValue = value * cengMultiplier;
                        totalAttributes.merge(attr, boostedValue, Double::sum);
                    });
                }
            }
        }

        return totalAttributes;
    }

    /**
     * 根据层级获取属性倍率
     * @param ceng 层级标识（copy_1 ~ copy_9）
     * @return 属性倍率（1.0 ~ 3.0）
     */
    public static double getCengMultiplier(String ceng) {
        return switch (ceng) {
            case "copy_1" -> 1.0;
            case "copy_2" -> 1.2;
            case "copy_3" -> 1.4;
            case "copy_4" -> 1.6;
            case "copy_5" -> 1.8;
            case "copy_6" -> 2.0;
            case "copy_7" -> 2.2;
            case "copy_8" -> 2.5;
            case "copy_9" -> 3.0;
            default -> 1.0;
        };
    }

    /**
     * 为玩家应用功法属性加成（仅容器内的功法生效）
     */
    public static void applyGongFaAttributes(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        Map<String, Double> totalAttributes = getTotalAttributes(player);

        applyAttribute(player, Attributes.ATTACK_DAMAGE, totalAttributes.getOrDefault("attack_damage", 0.0));
        applyAttribute(player, Attributes.MOVEMENT_SPEED, totalAttributes.getOrDefault("movement_speed", 0.0));
        applyAttribute(player, Attributes.MAX_HEALTH, totalAttributes.getOrDefault("max_health", 0.0));
        applyAttribute(player, Attributes.ARMOR, totalAttributes.getOrDefault("armor", 0.0));
        applyAttribute(player, Attributes.KNOCKBACK_RESISTANCE, totalAttributes.getOrDefault("knockback_resistance", 0.0));
    }

    /**
     * 移除玩家的功法属性加成
     */
    public static void removeGongFaAttributes(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        removeAttributeModifier(player, Attributes.ATTACK_DAMAGE, "attack_damage");
        removeAttributeModifier(player, Attributes.MOVEMENT_SPEED, "movement_speed");
        removeAttributeModifier(player, Attributes.MAX_HEALTH, "max_health");
        removeAttributeModifier(player, Attributes.ARMOR, "armor");
        removeAttributeModifier(player, Attributes.KNOCKBACK_RESISTANCE, "knockback_resistance");
    }

    /**
     * 应用单个属性加成
     */
    private static void applyAttribute(Player player, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID,
                ATTRIBUTE_MODIFIER_PREFIX + attribute.unwrapKey().orElseThrow().location().getPath());

        AttributeModifier oldModifier = instance.getModifier(modifierId);
        if (oldModifier != null) {
            instance.removeModifier(oldModifier);
        }

        if (value != 0.0) {
            AttributeModifier modifier = new AttributeModifier(
                    modifierId,
                    value,
                    AttributeModifier.Operation.ADD_VALUE
            );
            instance.addTransientModifier(modifier);
        }
    }


    /**
     * 移除单个属性加成
     */
    private static void removeAttributeModifier(Player player, Holder<Attribute> attribute, String attributeName) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID,
                ATTRIBUTE_MODIFIER_PREFIX + attributeName);

        AttributeModifier modifier = instance.getModifier(modifierId);
        if (modifier != null) {
            instance.removeModifier(modifier);
        }
    }

    /**
     * 玩家 Tick 事件 - 持续应用功法属性（每 tick 刷新一次，确保容器变化后立即生效）
     */


}


