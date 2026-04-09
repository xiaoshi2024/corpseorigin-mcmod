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
import org.jline.utils.InputStreamReader;

import javax.script.CompiledScript;
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

    public static boolean isDataLoaded() {return instance != null && !instance.gongFaDataMap.isEmpty();}

    public static int getDataCount() {return instance != null ? instance.gongFaDataMap.size() : 0;}

    public static GongFaJsonLoader getInstance() { return  instance;}



    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        CorpseOrigin.LOGGER.info("========== 开始加载功法数据 [环境: {}] ==========",
                net.neoforged.fml.loading.FMLEnvironment.dist);
        CorpseOrigin.LOGGER.info("找到 {} 个功法JSON文件", resourceLocationJsonElementMap.size());

        if (resourceLocationJsonElementMap.isEmpty()) {
            CorpseOrigin.LOGGER.error("⚠ 没有找到任何功法JSON文件！");
            CorpseOrigin.LOGGER.error("⚠ 请确保 JSON 文件位于: data/corpseorigin/gf_data/");
            CorpseOrigin.LOGGER.error("⚠ 文件名必须以 .json 结尾");
        }
        gongFaDataMap.clear();
        resourceLocationJsonElementMap.forEach((resourceLocation, jsonElement) -> {
            try {
                GongFaData data = parseGongFaData(jsonElement);
                if (data != null) {
                    gongFaDataMap.put(data.getTypeId() + "_" + data.getRarity() + "_" + data.getCeng(), data);
                    GongFaSkillManager.getInstance().registerGongFuSkill(data);
                    for (String skillName : data.getSkills()) {
                        com.phagens.corpseorigin.GongFU.JSskill.JSSkillEngine.getInstance()
                                .preloadScript(skillName);
                    }
                    CorpseOrigin.LOGGER.info("加载功法数据：{} (稀有度：{}, 层数：{})",
                            data.getTypeId(), data.getRarity(), data.getCeng());
                }

            }catch (Exception e){
                CorpseOrigin.LOGGER.error("解析功法数据失败：{}", resourceLocation, e);
            }
        });
        CorpseOrigin.LOGGER.info("========== 功法数据加载完成，共加载 {} 个功法 ==========", gongFaDataMap.size());
    }

    /**
     * ⭐手动从类路径加载JSON文件（用于客户端早期初始化）
     */
    public static void forceLoadFromResources() {
        if (instance != null && !instance.gongFaDataMap.isEmpty()) {
            CorpseOrigin.LOGGER.debug("【强制加载】功法数据已存在，跳过");
            return;
        }
        CorpseOrigin.LOGGER.info("【强制加载】开始从资源文件加载功法数据...");
        try {
            if (instance == null) {
                instance = new GongFaJsonLoader(GSON, FOLDER);
            }
            var classLoader = GongFaJsonLoader.class.getClassLoader();
            var resourcePath = "data/corpseorigin/gf_data/";
            // 尝试加载已知的JSON文件 - 请根据您的实际文件名修改这里
            String[] knownFiles = {
                    "ba_dao_shi.json",
                    "qi_jia_shu.json",
                    "shi_xian_jian.json",
                    "sha_lu_xue_mai.json",
                    "shi_chao_zhao_huan.json",
                    "shui_qiu_ren_yi_neng.json",
                    "xuan_kong_quan.json",
                    "you_sha_dao_fa.json"
            };
            int loadedCount = 0;
            for (String fileName : knownFiles) {
                try {
                    var stream = classLoader.getResourceAsStream(resourcePath + fileName);
                    if (stream != null) {
                        try (InputStreamReader reader = new InputStreamReader(stream)) {
                            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
                            GongFaData data = instance.parseGongFaData(jsonElement);
                            if (data != null) {
                                String key = data.getTypeId() + "_" + data.getRarity() + "_" + data.getCeng();
                                instance.gongFaDataMap.put(key, data);
                                GongFaSkillManager.getInstance().registerGongFuSkill(data);
                                for (String skillName : data.getSkills()) {
                                    com.phagens.corpseorigin.GongFU.JSskill.JSSkillEngine.getInstance()
                                            .preloadScript(skillName);
                                }
                                CorpseOrigin.LOGGER.info("【强制加载】✓ {}", data.getName());
                                loadedCount++;
                            }
                        }
                    } else {
                        CorpseOrigin.LOGGER.debug("【强制加载】未找到文件: {}", fileName);
                    }
                } catch (Exception e) {
                    CorpseOrigin.LOGGER.error("【强制加载】✗ 加载 {} 失败", fileName, e);
                }
            }

            CorpseOrigin.LOGGER.info("【强制加载】完成，共加载 {} 个功法", loadedCount);

        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("【强制加载】异常", e);
        }
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
