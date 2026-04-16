package com.phagens.corpseorigin.Item.zbritem.GodBase;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 超神器基类 - 继承自 DivineArtifactItem
 * 特点：淡紫色品质、超强效果、额外特效
 */
public abstract class SuperArtifactItem extends DivineArtifactItem {
    
    public SuperArtifactItem(Properties properties, String artifactName, String setName) {
        super(properties, ArtifactTier.SUPER_ARTIFACT, artifactName, setName);
    }
    
    /**
     * 超神器专属tick逻辑（子类可选重写）
     */
    protected void onSuperArtifactTick(ItemStack stack, Level level, Player player, int slotId, boolean isSelected, int setCount) {
    }
    
    @Override
    protected final void onInventoryTick(ItemStack stack, Level level, Player player, int slotId, boolean isSelected, int setCount) {
        onSuperArtifactTick(stack, level, player, slotId, isSelected, setCount);
    }
    
    /**
     * 超神器专属描述前缀（子类可选重写）
     */
    protected void addSuperArtifactPrefix(List<Component> tooltip) {
    }
    
    @Override
    protected final void addArtifactDescription(List<Component> tooltip) {
        addSuperArtifactPrefix(tooltip);
    }
}
