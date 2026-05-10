package com.phagens.corpseorigin.Recipe;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
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
public class TechniqueSwapRecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson gson = new Gson();
    private static final String FOLDER = "technique_swap_recipes";
    private static final TechniqueSwapRecipeManager INSTANCE = new TechniqueSwapRecipeManager();
    private final Map<ResourceLocation, TechniqueSwapRecipe> recipes = new ConcurrentHashMap<>();
    public static TechniqueSwapRecipeManager getInstance() {
        return INSTANCE;
    }
    public TechniqueSwapRecipeManager() {
        super(gson, FOLDER);
    }

    
    /**
     * 解析JSON配方
     */
    private TechniqueSwapRecipe parseRecipe(ResourceLocation id, JsonObject json) {
        try {
            List<Ingredient> inputs = new ArrayList<>();
            List<TechniqueSwapRecipe.GongFaInput> gongFaInputs = new ArrayList<>();

            JsonArray inputArray = json.getAsJsonArray("inputs");

            for (int i = 0; i < 6; i++) {
                if (i < inputArray.size()) {
                    JsonElement element = inputArray.get(i);

                    if (element.isJsonObject()) {
                        JsonObject ingredientJson = element.getAsJsonObject();

                        if (ingredientJson.has("gong_fa_data")) {
                            String typeId = ingredientJson.get("gong_fa_data").getAsString();
                            GongFaData templateData = findGongFaDataByTypeId(typeId);

                            if (templateData == null) {
                                CorpseOrigin.LOGGER.warn("配方 {} 中找不到功法数据: {}", id, typeId);
                                return null;
                            }

                            gongFaInputs.add(new TechniqueSwapRecipe.GongFaInput(typeId, templateData));
                            inputs.add(Ingredient.EMPTY);
                        } else {
                            String itemStr = ingredientJson.get("item").getAsString();

                            if (itemStr.isEmpty()) {
                                inputs.add(Ingredient.EMPTY);
                            } else {
                                ResourceLocation itemLocation = ResourceLocation.parse(itemStr);
                                net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(itemLocation);

                                int count = ingredientJson.has("count") ? ingredientJson.get("count").getAsInt() : 1;
                                inputs.add(Ingredient.of(new ItemStack(item, count)));
                            }
                        }
                    } else {
                        inputs.add(Ingredient.EMPTY);
                    }
                } else {
                    inputs.add(Ingredient.EMPTY);
                }
            }

            String type = json.has("type") ? json.get("type").getAsString() : "normal";

            if ("gong_fa".equals(type)) {
                JsonObject outputJson = json.getAsJsonObject("output");
                String typeId = outputJson.get("gong_fa_data").getAsString();
                GongFaData outputData = findGongFaDataByTypeId(typeId);

                if (outputData == null) {
                    CorpseOrigin.LOGGER.warn("配方 {} 中找不到输出功法数据: {}", id, typeId);
                    return null;
                }

                TechniqueSwapRecipe.GongFaOutput gongFaOutput = new TechniqueSwapRecipe.GongFaOutput(typeId, outputData);

                return new TechniqueSwapRecipe(id, inputs, gongFaInputs, gongFaOutput);
            } else {
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
    private GongFaData findGongFaDataByTypeId(String typeId) {
        Map<String, GongFaData> allData = com.phagens.corpseorigin.GongFU.JsonLoader.GongFaJsonLoader.getAllGongFaData();

        for (Map.Entry<String, GongFaData> entry : allData.entrySet()) {
            GongFaData data = entry.getValue();
            if (data.getTypeId().equalsIgnoreCase(typeId)) {
                return data;
            }
        }

        return null;
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

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        CorpseOrigin.LOGGER.info("========== 开始加载功法兑换台配方 ==========");
        CorpseOrigin.LOGGER.info("找到 {} 个配方JSON文件", resourceLocationJsonElementMap.size());

        if (resourceLocationJsonElementMap.isEmpty()) {
            CorpseOrigin.LOGGER.warn("没有找到任何功法兑换台配方JSON文件！");
            CorpseOrigin.LOGGER.warn("请确保 JSON 文件位于: data/corpseorigin/technique_swap_recipes/");
        }

        recipes.clear();

        resourceLocationJsonElementMap.forEach((resourceLocation, jsonElement) -> {
            try {
                JsonObject json = jsonElement.getAsJsonObject();
                TechniqueSwapRecipe recipe = parseRecipe(resourceLocation, json);
                if (recipe != null) {
                    recipes.put(resourceLocation, recipe);
                    CorpseOrigin.LOGGER.info("加载配方：{} ({})", resourceLocation, recipe.getOutputType());
                }
            } catch (Exception e) {
                CorpseOrigin.LOGGER.error("加载配方失败：{}", resourceLocation, e);
            }
        });

        CorpseOrigin.LOGGER.info("========== 配方加载完成，共加载 {} 个配方 ==========", recipes.size());

    }
}
