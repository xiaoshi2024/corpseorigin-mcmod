package com.phagens.corpseorigin.GongFU.GongFaZL;


import com.phagens.corpseorigin.GongFU.GongFaZL.BaseGongFaItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.units.qual.C;

import java.util.*;

public class SlotConfigManager {

    private static final String CATEGORY_SLOTS_PREFIX = "CategorySlots_";
    private static final int MAX_SLOTS = 36;
    private static final int INITIAL_SLOTS = 1;

    private static final List<GongFaCategory> DEFAULT_SLOT_SEQUENCE = Arrays.asList(
            GongFaCategory.GF,
            GongFaCategory.XM,
            GongFaCategory.YN,
            GongFaCategory.FB,
            GongFaCategory.ST,
            GongFaCategory.SG,
            GongFaCategory.SZ,
            GongFaCategory.TFST,
            GongFaCategory.QY,
            GongFaCategory.UNIVERSAL
    );

    public static int getInitialSlotCount() {
        return INITIAL_SLOTS;
    }

    public static int getMaxSlotCount() {
        return MAX_SLOTS;
    }

    /**
     * 获取指定类别的槽位数
     */
    public static int getCategorySlotCount(Player player, GongFaCategory category) {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag slotData = playerData.getCompound("GongFuSlotData");

        String key = CATEGORY_SLOTS_PREFIX + category.getName();
        if (!slotData.contains(key)) {
            if (category == GongFaCategory.UNIVERSAL && !slotData.contains(CATEGORY_SLOTS_PREFIX + "initialized")) {
                return INITIAL_SLOTS;
            }
            return 0;
        }

        return slotData.getInt(key);
    }
    /**
     * 为指定类别增加槽位
     * @return 实际增加的槽位数
     */
    public static int addCategorySlot(Player player, GongFaCategory category, int count) {
        int currentTotal = getUnlockedSlotCount(player);
        int canAdd = Math.min(count,MAX_SLOTS - currentTotal);

        if (canAdd <= 0) {
            return 0;
        }

        int currentCount = getCategorySlotCount(player, category);
        int newCount = currentCount + canAdd;

        CompoundTag playerData = player.getPersistentData();
        CompoundTag slotData = playerData.getCompound("GongFuSlotData");

        String key = CATEGORY_SLOTS_PREFIX + category.getName();
        slotData.putInt(key, newCount);
        playerData.put("GongFuSlotData", slotData);

        return canAdd;
    }

    /**
     * 为所有类别平均增加槽位
     */
    public static void addSlotsToAllCategories(Player player, int count) {
        for (GongFaCategory category : DEFAULT_SLOT_SEQUENCE) {
            addCategorySlot(player, category, count);
        }
    }

    /**
     * 获取玩家的总槽位数
     */
    public static int getUnlockedSlotCount(Player player) {
        int total = 0;
        for (GongFaCategory category : DEFAULT_SLOT_SEQUENCE) {
            total += getCategorySlotCount(player, category);
        }
        return total;
    }



    /**
     * 设置指定类别的槽位数
     */
    public static void setCategorySlotCount(Player player, GongFaCategory category, int count) {
        CompoundTag playerData = player.getPersistentData();
        CompoundTag slotData = playerData.getCompound("GongFuSlotData");

        int currentTotal = getUnlockedSlotCount(player);
        int currentCategoryCount = getCategorySlotCount(player, category);
        int diff = count - currentCategoryCount;

        if (diff > 0 && currentTotal + diff > MAX_SLOTS) {
            count = currentCategoryCount + (MAX_SLOTS - currentTotal);
        }

        int validCount = Math.max(0, count);

        String key = CATEGORY_SLOTS_PREFIX + category.getName();
        slotData.putInt(key, validCount);
        slotData.putBoolean(CATEGORY_SLOTS_PREFIX + "initialized", true);
        playerData.put("GongFuSlotData", slotData);
    }

    /**
     * 根据槽位索引获取对应的类别
     * 按类别顺序排列：GF槽位 -> XM槽位 -> YN槽位 -> ... -> UNIVERSAL槽位
     */
    public static GongFaCategory getSlotCategory(Player player, int slotIndex) {
        if (slotIndex < 0) {
            return GongFaCategory.UNIVERSAL;
        }

        int currentIndex = 0;
        for (GongFaCategory category : DEFAULT_SLOT_SEQUENCE) {
            int categorySlots = getCategorySlotCount(player, category);

            if (slotIndex < currentIndex + categorySlots) {
                return category;
            }
            currentIndex += categorySlots;
        }

        return GongFaCategory.UNIVERSAL;
    }



    /**
     * 检查是否可以继续解锁槽位
     */
    public static boolean canUnlockMoreSlots(Player player) {
        return getUnlockedSlotCount(player) < MAX_SLOTS;
    }

    /**
     * 检查指定类别是否可以继续增加槽位
     */
    public static boolean canAddCategorySlots(Player player, GongFaCategory category) {
        return getUnlockedSlotCount(player) < MAX_SLOTS;
    }

    public static List<ItemStack> getItemsByCategory(Player player, GongFaCategory category, List<ItemStack> containerItems) {
        List<ItemStack> result = new ArrayList<>();

        for (ItemStack stack : containerItems) {
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof BaseGongFaItem gongFaItem) {
                GongFaCategory itemCategory = gongFaItem.getItemCategory(stack);
                if (itemCategory == category) {
                    result.add(stack.copy());
                }
            }
        }

        return result;

        }

    public static Map<GongFaCategory, List<ItemStack>> getAllItemsByCategory(Player player, List<ItemStack> containerItems) {
        Map<GongFaCategory, List<ItemStack>> result = new HashMap<>();

        for (GongFaCategory category : GongFaCategory.values()) {
            result.put(category, new ArrayList<>());
        }

        for (ItemStack stack : containerItems) {
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof BaseGongFaItem gongFaItem) {
                GongFaCategory itemCategory = gongFaItem.getItemCategory(stack);
                result.computeIfAbsent(itemCategory, k -> new ArrayList<>()).add(stack.copy());
            }
        }

        return result;
    }
    public static void unlockSlot(Player player) {
        int currentCount = getUnlockedSlotCount(player);
        if (currentCount >= MAX_SLOTS) {
            return;
        }
        
        CompoundTag playerData = player.getPersistentData();
        CompoundTag slotData = playerData.getCompound("GongFuSlotData");
        
        slotData.putInt(CATEGORY_SLOTS_PREFIX, currentCount + 1);
        playerData.put("GongFuSlotData", slotData);
    }
    
    public static void setUnlockedSlotCount(Player player, int count) {
        int validCount = Math.max(INITIAL_SLOTS, Math.min(count, MAX_SLOTS));
        
        CompoundTag playerData = player.getPersistentData();
        CompoundTag slotData = playerData.getCompound("GongFuSlotData");
        
        slotData.putInt(CATEGORY_SLOTS_PREFIX, validCount);
        playerData.put("GongFuSlotData", slotData);
    }
    

    

        

    

    

}
