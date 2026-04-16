package com.phagens.corpseorigin.Item.zbritem.GodBase;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 神器基类 - 继承自 DivineArtifactItem
 * 特点：金色品质、中等强度效果
 */
public abstract class ArtifactItem extends DivineArtifactItem {
    
    public ArtifactItem(Properties properties, String artifactName, String setName) {
        super(properties, ArtifactTier.ARTIFACT, artifactName, setName);
    }
    
    /**
     * 神器专属tick逻辑（子类可选重写）
     */
    protected void onArtifactTick(ItemStack stack, Level level, Player player, int slotId, boolean isSelected, int setCount) {
    }
    
    @Override
    protected final void onInventoryTick(ItemStack stack, Level level, Player player, int slotId, boolean isSelected, int setCount) {
        onArtifactTick(stack, level, player, slotId, isSelected, setCount);
    }
    
    /**
     * 神器专属描述前缀（子类可选重写）
     */
    protected void addArtifactPrefix(List<Component> tooltip) {
    }
    
    @Override
    protected final void addArtifactDescription(List<Component> tooltip) {
        addArtifactPrefix(tooltip);
    }
}
