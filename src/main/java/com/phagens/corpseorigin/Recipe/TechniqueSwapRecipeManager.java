package com.phagens.corpseorigin.Recipe;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 功法兑换台配方管理器
 * 负责加载、查询和匹配配方
 */
public class TechniqueSwapRecipeManager {
    
    private static final TechniqueSwapRecipeManager INSTANCE = new TechniqueSwapRecipeManager();
    private final Map<ResourceLocation, TechniqueSwapRecipe> recipes = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    
    public static TechniqueSwapRecipeManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 加载配方文件
     * @param resourceLocation JSON文件路径
     * @param inputStream JSON输入流
     */
    public void loadRecipe(ResourceLocation resourceLocation, InputStream inputStream) {
        try {
            JsonObject json = gson.fromJson(new InputStreamReader(inputStream), JsonObject.class);
            TechniqueSwapRecipe recipe = parseRecipe(resourceLocation, json);
            if (recipe != null) {
                recipes.put(resourceLocation, recipe);
                CorpseOrigin.LOGGER.info("加载功法兑换台配方：{}", resourceLocation);
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("加载配方失败：{}", resourceLocation, e);
        }
    }
    
    /**
     * 解析JSON配方
     */
    private TechniqueSwapRecipe parseRecipe(ResourceLocation id, JsonObject json) {
        try {
            // 解析输入材料
            JsonArray inputArray = json.getAsJsonArray("inputs");
            List<Ingredient> inputs = new ArrayList<>();

            for (int i = 0; i < 6; i++) {
                if (i < inputArray.size()) {
                    JsonObject ingredientJson = inputArray.get(i).getAsJsonObject();
                    String itemStr = ingredientJson.get("item").getAsString();

                    // 空字符串表示空槽位
                    if (itemStr.isEmpty()) {
                        inputs.add(Ingredient.EMPTY);
                    } else {
                        // 通过物品注册表获取物品实例
                        ResourceLocation itemLocation = ResourceLocation.parse(itemStr);
                        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(itemLocation);

                        int count = ingredientJson.has("count") ? ingredientJson.get("count").getAsInt() : 1;
                        inputs.add(Ingredient.of(new ItemStack(item, count)));
                    }
                } else {
                    // 不足6个材料，剩余槽位为空
                    inputs.add(Ingredient.EMPTY);
                }
            }

            // 判断配方类型
            String type = json.has("type") ? json.get("type").getAsString() : "normal";

            if ("gong_fa".equals(type)) {
                // 功法物品配方
                JsonObject outputJson = json.getAsJsonObject("output");
                String gongFaTypeId = outputJson.get("gong_fa_type_id").getAsString();
                int rarity = outputJson.get("rarity").getAsInt();
                String ceng = outputJson.get("ceng").getAsString();

                return new TechniqueSwapRecipe(id, inputs, gongFaTypeId, rarity, ceng);
            } else {
                // 普通物品配方
                JsonObject outputJson = json.getAsJsonObject("output");
                String outputItemStr = outputJson.get("item").getAsString();
                int outputCount = outputJson.has("count") ? outputJson.get("count").getAsInt() : 1;

                ResourceLocation outputLocation = ResourceLocation.parse(outputItemStr);
                net.minecraft.world.item.Item outputItem = BuiltInRegistries.ITEM.get(outputLocation);
                ItemStack output = new ItemStack(outputItem, outputCount);

                return new TechniqueSwapRecipe(id, inputs, output);
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("解析配方失败：{}", id, e);
            return null;
        }
    }
    
    /**
     * 根据输入材料匹配所有可用配方
     * @param inputItems 6个输入槽的物品
     * @return 匹配的配方列表
     */
    public List<TechniqueSwapRecipe> findMatchingRecipes(ItemStack[] inputItems) {
        List<TechniqueSwapRecipe> matching = new ArrayList<>();
        
        for (TechniqueSwapRecipe recipe : recipes.values()) {
            if (recipe.matches(inputItems)) {
                matching.add(recipe);
            }
        }
        
        return matching;
    }
    
    /**
     * 清空所有配方（资源重载时调用）
     */
    public void clear() {
        recipes.clear();
    }
    
    public int getRecipeCount() {
        return recipes.size();
    }
}
