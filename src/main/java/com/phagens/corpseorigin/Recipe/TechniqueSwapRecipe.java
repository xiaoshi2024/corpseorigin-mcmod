package com.phagens.corpseorigin.Recipe;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.GongFaZL.BaseGongFaItem;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import com.phagens.corpseorigin.GongFU.JsonLoader.GongFaJsonLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 功法兑换台配方数据类
 * 支持6个输入槽的材料匹配和多配方输出
 *  * 产出类型：
 *  * 1. 普通物品 (Items)
 *  * 2. 功法物品 (BaseGongFaItem + GongFaData)
 */
public class TechniqueSwapRecipe {

    private final ResourceLocation id;
    private final List<Ingredient> inputs;
    private final ItemStack output;
    private final OutputType outputType;
    private final String gongFaTypeId;
    private final int gongFaRarity;
    private final String gongFaCeng;

    /**
     * 输出类型枚举
     */
    public enum OutputType {
        NORMAL,      // 普通物品
        GONG_FA      // 功法物品
    }
    
    public TechniqueSwapRecipe(ResourceLocation id, List<Ingredient> inputs, ItemStack output) {
        this.id = id;
        this.inputs = inputs;
        this.output = output;
        this.outputType = OutputType.NORMAL;
        this.gongFaTypeId = null;
        this.gongFaRarity = 0;
        this.gongFaCeng = null;
    }

    public TechniqueSwapRecipe(ResourceLocation id, List<Ingredient> inputs,
                               String gongFaTypeId, int gongFaRarity, String gongFaCeng) {
        this.id = id;
        this.inputs = inputs;
        this.outputType = OutputType.GONG_FA;
        this.gongFaTypeId = gongFaTypeId;
        this.gongFaRarity = gongFaRarity;
        this.gongFaCeng = gongFaCeng;

        // 创建功法物品产出
        this.output = createGongFaOutput(gongFaTypeId, gongFaRarity, gongFaCeng);
    }

    /**
     * 创建功法物品产出
     */
    private ItemStack createGongFaOutput(String typeId, int rarity, String ceng) {
        try {
            // 从JSON加载器获取功法数据
            GongFaData data = GongFaJsonLoader.getGongFaData(typeId, rarity, ceng);

            if (data == null) {
                CorpseOrigin.LOGGER.error("无法创建功法物品：找不到功法数据 {}_{}_{}", typeId, rarity, ceng);
                return ItemStack.EMPTY;
            }

            // 获取功法模板物品
            BaseGongFaItem baseItem = (BaseGongFaItem) com.phagens.corpseorigin.register.Moditems.BASE_GONG_FA.get();

            // 创建物品堆栈
            ItemStack stack = new ItemStack(baseItem);

            // 将功法数据写入物品NBT
            baseItem.setDataToItem(stack, data);

            CorpseOrigin.LOGGER.info("创建功法物品：{} (稀有度:{}, 层数:{})",
                    data.getName(), rarity, ceng);

            return stack;
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("创建功法物品失败", e);
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * 检查材料是否匹配
     * @param inputItems 输入槽的6个物品
     * @return 是否匹配此配方
     */
    public boolean matches(ItemStack[] inputItems) {
        if (inputItems.length != 6) return false;
        // 创建输入槽的副本，用于消耗匹配
        ItemStack[] remaining = new ItemStack[6];
        for (int i = 0; i < 6; i++) {
            remaining[i] = inputItems[i].copy();
        }
        // 尝试匹配每个材料
        for (Ingredient ingredient : inputs) {
            boolean matched = false;
            for (int i = 0; i < 6; i++) {
                if (!remaining[i].isEmpty() && ingredient.test(remaining[i])) {
                    remaining[i].shrink(1); // 消耗一个材料
                    matched = true;
                    break;
                }
            }
            if (!matched) return false; // 有材料不匹配
        }
        
        return true;
    }
    
    /**
     * 消耗材料并生成产物
     * @param inputItems 输入槽物品（会被修改）
     * @return 产出物品
     */
    public ItemStack craft(ItemStack[] inputItems) {
        if (!matches(inputItems)) {
            return ItemStack.EMPTY;
        }
        
        // 消耗材料
        ItemStack[] remaining = new ItemStack[6];
        for (int i = 0; i < 6; i++) {
            remaining[i] = inputItems[i].copy();
        }
        
        for (Ingredient ingredient : inputs) {
            for (int i = 0; i < 6; i++) {
                if (!remaining[i].isEmpty() && ingredient.test(remaining[i])) {
                    inputItems[i].shrink(1); // 实际消耗
                    break;
                }
            }
        }
        
        return output.copy();
    }

    public ResourceLocation getId() { return id; }
    public List<Ingredient> getInputs() { return inputs; }
    public ItemStack getOutput() { return output; }
    public OutputType getOutputType() { return outputType; }
    public String getGongFaTypeId() { return gongFaTypeId; }
    public int getGongFaRarity() { return gongFaRarity; }
    public String getGongFaCeng() { return gongFaCeng; }
}
