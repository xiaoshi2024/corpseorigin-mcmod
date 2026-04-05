package com.phagens.corpseorigin.GongFU.JsonLoader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaSkillManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GongFaJsonLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();
    private static final String FOLDER = "gf_data";

    private Map<String, GongFaData> gongFaDataMap = new HashMap<>();

    public GongFaJsonLoader(Gson gson, String directory) {
        super(gson, directory);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        gongFaDataMap.clear();
        resourceLocationJsonElementMap.forEach((resourceLocation, jsonElement) -> {
            try {
                GongFaData data = parseGongFaData(jsonElement);
                if (data != null) {
                    gongFaDataMap.put(data.getTypeId() + "_" + data.getRarity() + "_" + data.getCeng(), data);
                    GongFaSkillManager.getInstance().registerGongFuSkill(data);
                    CorpseOrigin.LOGGER.info("加载功法数据：{} (稀有度：{}, 层数：{})",
                            data.getTypeId(), data.getRarity(), data.getCeng());
                }

            }catch (Exception e){
                CorpseOrigin.LOGGER.error("解析功法数据失败：{}", resourceLocation, e);
            }
        });
    }
    /// 解析为GongFaData
    private GongFaData parseGongFaData(JsonElement jsonElement) {
        JsonObject json = jsonElement.getAsJsonObject();
        String typeId = json.has("type_id") ? json.get("type_id").getAsString() : "UNKNOWN";
        String type = json.has("type") ? json.get("type").getAsString() : "?";
        String name = json.has("name") ? json.get("name").getAsString() : "Name";
        int rarity = json.has("rarity") ? json.get("rarity").getAsInt() : 1;
        String ceng = json.has("ceng") ? json.get("ceng").getAsString() : "copy_1";
        int cooldown =json.has("cooldown")?json.get("cooldown").getAsInt():300;
        String icon = json.has("icon") ? json.get("icon").getAsString() : null;
        // 解析属性 Map
        Map<String, Double> attributes = new HashMap<>();
        if (json.has("attributes")) {
            JsonObject attrObj = json.getAsJsonObject("attributes");
            attrObj.entrySet().forEach(entry -> {
                attributes.put(entry.getKey(), entry.getValue().getAsDouble());
            });
        }
        // 解析技能列表
        List<String> skills = new ArrayList<>();
        if (json.has("skills")) {
            json.getAsJsonArray("skills").forEach(element -> {
                skills.add(element.getAsString());
            });
        }

        return new GongFaData(typeId,type,name,attributes, skills, rarity, ceng,cooldown, icon);
    }
    /**
     * 获取特定的功法数据
     */
    public static GongFaData getGongFaData(String typeId, int rarity, String ceng) {
        String key = typeId + "_" + rarity + "_" + ceng;
        return instance != null ? instance.gongFaDataMap.get(key) : null;
    }

    /**
     * 获取所有功法数据（返回副本）
     */
    public static Map<String, GongFaData> getAllGongFaData() {
        return instance != null ? new HashMap<>(instance.gongFaDataMap) : new HashMap<>();
    }
    // 单例实例
    private static GongFaJsonLoader instance;

    /**
     * 注册加载器（在 AddReloadListenerEvent 中调用）
     */
    public static void register(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        instance = new GongFaJsonLoader(GSON, FOLDER);
        event.addListener(instance);
    }

}
