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
import java.util.Map;

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
    private final List<GongFaInput> gongFaInputs;
    private final ItemStack output;
    private final OutputType outputType;
    private final GongFaOutput gongFaOutput;
    public static class GongFaOutput {
        private final String typeId;
        private final GongFaData outputData;

        public GongFaOutput(String typeId, GongFaData outputData) {
            this.typeId = typeId;
            this.outputData = outputData;
        }

        public ItemStack createOutputItem() {
            try {
                if (outputData == null) {
                    CorpseOrigin.LOGGER.error("无法创建功法物品：功法数据为空 {}", typeId);
                    return ItemStack.EMPTY;
                }

                BaseGongFaItem baseItem = (BaseGongFaItem) com.phagens.corpseorigin.register.Moditems.BASE_GONG_FA.get();
                ItemStack stack = new ItemStack(baseItem);
                baseItem.setDataToItem(stack, outputData);

                CorpseOrigin.LOGGER.info("创建功法物品：{} (稀有度:{}, 层数:{})",
                        outputData.getName(), outputData.getRarity(), outputData.getCeng());

                return stack;
            } catch (Exception e) {
                CorpseOrigin.LOGGER.error("创建功法物品失败", e);
                return ItemStack.EMPTY;
            }
        }

        public String getTypeId() { return typeId; }
        public GongFaData getOutputData() { return outputData; }
    }
    public static class GongFaInput {
        private final String typeId;
        private final GongFaData templateData;

        public GongFaInput(String typeId, GongFaData templateData) {
            this.typeId = typeId;
            this.templateData = templateData;
        }

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty() || !(stack.getItem() instanceof BaseGongFaItem)) {
                return false;
            }

            BaseGongFaItem gongFaItem = (BaseGongFaItem) stack.getItem();
            GongFaData data = gongFaItem.getDataFromItem(stack);

            if (data == null || templateData == null) {
                return false;
            }

            String itemTypeId = data.getTypeId();
            String templateTypeId = templateData.getTypeId();

            return itemTypeId != null && templateTypeId != null && itemTypeId.equalsIgnoreCase(templateTypeId);
        }

        public String getTypeId() { return typeId; }
        public GongFaData getTemplateData() { return templateData; }
    }
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
        this.gongFaInputs = new ArrayList<>();
        this.output = output;
        this.outputType = OutputType.NORMAL;
        this.gongFaOutput = null;
    }

    public TechniqueSwapRecipe(ResourceLocation id, List<Ingredient> inputs, List<GongFaInput> gongFaInputs,
                               GongFaOutput gongFaOutput) {
        this.id = id;
        this.inputs = inputs;
        this.gongFaInputs = gongFaInputs != null ? gongFaInputs : new ArrayList<>();
        this.outputType = OutputType.GONG_FA;
        this.gongFaOutput = gongFaOutput;
        this.output = this.gongFaOutput != null ? this.gongFaOutput.createOutputItem() : ItemStack.EMPTY;
    }

    
    /**
     * 检查材料是否匹配
     * @param inputItems 输入槽的6个物品
     * @return 是否匹配此配方
     */
    public boolean matches(ItemStack[] inputItems) {
        if (inputItems.length != 6) return false;

        ItemStack[] remaining = new ItemStack[6];
        for (int i = 0; i < 6; i++) {
            remaining[i] = inputItems[i].copy();
        }

        for (Ingredient ingredient : inputs) {
            if (ingredient == Ingredient.EMPTY) continue;

            boolean matched = false;
            for (int i = 0; i < 6; i++) {
                if (!remaining[i].isEmpty() && ingredient.test(remaining[i])) {
                    remaining[i].shrink(1);
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }

        for (GongFaInput gongFaInput : gongFaInputs) {
            boolean matched = false;
            for (int i = 0; i < 6; i++) {
                if (remaining[i].isEmpty()) continue;

                if (gongFaInput.matches(remaining[i])) {
                    remaining[i].shrink(1);
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
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

        for (GongFaInput gongFaInput : gongFaInputs) {
            for (int i = 0; i < 6; i++) {
                if (!remaining[i].isEmpty() && gongFaInput.matches(remaining[i])) {
                    inputItems[i].shrink(1);
                    break;
                }
            }
        }
        
        return output.copy();
    }

    public ResourceLocation getId() { return id; }
    public List<Ingredient> getInputs() { return inputs; }
    public List<GongFaInput> getGongFaInputs() { return gongFaInputs; }
    public ItemStack getOutput() { return output; }
    public GongFaOutput getGongFaOutput() { return gongFaOutput; }
    public OutputType getOutputType() { return outputType; }
}
