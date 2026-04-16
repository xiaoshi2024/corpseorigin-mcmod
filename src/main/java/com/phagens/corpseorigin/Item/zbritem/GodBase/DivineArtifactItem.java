package com.phagens.corpseorigin.Item.zbritem.GodBase;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 神器祖宗类 - 所有神器和超神器的基类
 * 提供：
 * 1. 品质系统（神器/超神器）
 * 2. 套装系统（检测玩家装备的套装部件）
 * 3. 独立效果系统（每个物品的独特能力）
 * 4. 统一的外观和描述格式
 */
public abstract class DivineArtifactItem extends Item {
    
    protected final ArtifactTier tier; //品质
    protected final String artifactName;  //物品名
    protected final String setName;   //套装名

    private static final Map<String, List<String>> SET_REGISTRY = new ConcurrentHashMap<>();  //全局套装注册
    
    public DivineArtifactItem(Properties properties, ArtifactTier tier, String artifactName, String setName) {
        super(properties.stacksTo(1));
        this.tier = tier;
        this.artifactName = artifactName;
        this.setName = setName;

        if (!setName.isEmpty()) {
            registerSetItem(setName, artifactName);
        }
    }

    private void registerSetItem(String setName, String itemName) {
        SET_REGISTRY.computeIfAbsent(setName, k -> new java.util.ArrayList<>()).add(itemName);
    }
    
    /**
     * 获取神器品质
     */
    public ArtifactTier getTier() {
        return tier;
    }
    
    /**
     * 获取神器名称
     */
    public String getArtifactName() {
        return artifactName;
    }
    
    /**
     * 获取所属套装名称
     */
    public String getSetName() {
        return setName;
    }
    
    /**
     * 检查是否属于某个套装
     */
    public boolean belongsToSet(String setName) {
        return this.setName.equals(setName);
    }

    /**
     * 获取玩家当前装备的同套装物品数量
     */
    protected int getEquippedSetCount(Player player) {
        if (setName.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() instanceof DivineArtifactItem item && item.belongsToSet(setName)) {
                count++;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof DivineArtifactItem item && item.belongsToSet(setName)) {
                count++;
            }
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof DivineArtifactItem item && item.belongsToSet(setName)) {
            count++;
        }

        return count;
    }

    /**
     * 检查玩家是否拥有指定名称的套装物品
     */
    protected boolean hasSetItem(Player player, String itemName) {
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() instanceof DivineArtifactItem item &&
                    item.belongsToSet(setName) &&
                    item.getArtifactName().equals(itemName)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof DivineArtifactItem item &&
                    item.belongsToSet(setName) &&
                    item.getArtifactName().equals(itemName)) {
                return true;
            }
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof DivineArtifactItem item &&
                item.belongsToSet(setName) &&
                item.getArtifactName().equals(itemName)) {
            return true;
        }

        return false;
    }
    
    /**
     * 应用套装效果（子类重写）
     * @param player 玩家
     * @param equippedCount 已装备的套装数量
     */
    protected void applySetEffect(Player player, int equippedCount) {
    }
    
    /**
     * 移除套装效果（子类重写）
     * @param player 玩家
     */
    protected void removeSetEffect(Player player) {
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            int setCount = getEquippedSetCount(player);
            applySetEffect(player, setCount);
            onInventoryTick(stack, level, player, slotId, isSelected, setCount);
        }
    }

    /**
     * 自定义tick逻辑（子类重写）
     */
    protected void onInventoryTick(ItemStack stack, Level level, Player player, int slotId, boolean isSelected, int setCount) {
    }

    /**
     * 添加神器专属描述（子类重写）
     */
    protected void addArtifactDescription(List<Component> tooltip) {
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        ChatFormatting tierColor = tier == ArtifactTier.SUPER_ARTIFACT ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD;
        tooltipComponents.add(Component.literal("【" + tier.getDisplayName() + "】").withStyle(tierColor));

        if (!setName.isEmpty()) {
            tooltipComponents.add(Component.literal("套装: " + setName).withStyle(ChatFormatting.GREEN));

            List<String> setItems = SET_REGISTRY.get(setName);
            if (setItems != null && !setItems.isEmpty()) {
                tooltipComponents.add(Component.literal("套装部件:").withStyle(ChatFormatting.GRAY));
                for (String itemName : setItems) {
                    tooltipComponents.add(Component.literal("  • " + itemName).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }

        addArtifactDescription(tooltipComponents);
    }

    /**
     * 神器品质枚举
     */
    public enum ArtifactTier {
        ARTIFACT("神器", ChatFormatting.GOLD),
        SUPER_ARTIFACT("超神器", ChatFormatting.LIGHT_PURPLE);
        
        private final String displayName;
        private final ChatFormatting color;
        
        ArtifactTier(String displayName, ChatFormatting color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public ChatFormatting getColor() {
            return color;
        }
    }
}
