package xiaoshi2022.corpseorigin.growth;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.util.LocalizedException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;
import xiaoshi2022.corpseorigin.CorpseOrigin;

public final class OrganLibrary {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String BODY_KEY = "organ_loadout";
    public static final int MAX_SLOTS = 8;
    /** 自动生成的示例目录文件名：只作格式参照，不算玩家自定义内容。 */
    public static final String EXAMPLE_FILE = "examples.json";
    /** F8「打开器官资源包目录」释放到 organ 根目录的示例包名。 */
    public static final String EXAMPLE_PACK_ZIP = "CorpseOrigin-Organs.zip";

    /** 器官来源。示例内容只给玩家预览，尸兄突变只吃内置与玩家自定义。 */
    public enum Origin { BUILTIN, CATALOG, PACK, EXAMPLE }

    private static List<OrganDefinition> server = List.of();
    private static Map<String, Origin> origins = Map.of();
    public static Component packStatus = Component.empty();
    private OrganLibrary() {}
    public static List<OrganDefinition> definitions() { return server; }

    /** 该器官是否来自随模组分发的示例内容。 */
    public static boolean isExampleOrgan(String id) {
        return origins.get(id) == Origin.EXAMPLE;
    }
    public static List<OrganSlot> parseSlots(String json) {
        if (json == null || json.length() > 8192) throw new LocalizedException("message.corpseorigin.organ.validation.0");
        OrganSlot[] slots;
        try { slots = JSON.fromJson(json, OrganSlot[].class); }
        catch (com.google.gson.JsonParseException e) { throw new LocalizedException("message.corpseorigin.organ.validation.1"); }
        if (slots == null) throw new LocalizedException("message.corpseorigin.organ.validation.2");
        if (slots.length > MAX_SLOTS) throw new LocalizedException("message.corpseorigin.organ.validation.3");
        if (Arrays.stream(slots).anyMatch(s -> s == null || !s.valid()))
            throw new LocalizedException("message.corpseorigin.organ.validation.4");
        if (Arrays.stream(slots).filter(OrganSlot::replacesBody).count() > 1)
            throw new LocalizedException("message.corpseorigin.organ.validation.5");
        return List.of(slots);
    }
    public static void load() {
        var dir = FabricLoader.getInstance().getConfigDir().resolve("corpseorigin/organs");
        var result = new LinkedHashMap<String,OrganDefinition>();
        var originMap = new LinkedHashMap<String,Origin>();
        // ① 硬编内置（翅膀 / 尾巴）：不依赖任何外部文件，永远可用
        for (var def : builtin()) { result.put(def.id(), def); originMap.put(def.id(), Origin.BUILTIN); }
        try {
            Files.createDirectories(dir);
            var example = dir.resolve(EXAMPLE_FILE);
            if (!Files.exists(example)) Files.writeString(example, JSON.toJson(builtin()), StandardCharsets.UTF_8);
            try (var files = Files.list(dir)) {
                for (var file : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    boolean exampleFile = file.getFileName().toString().equals(EXAMPLE_FILE);
                    try {
                        if (Files.size(file) > 262144) throw new IllegalArgumentException("Catalog file too large");
                        var entries = JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), OrganDefinition[].class);
                        if (entries == null) throw new IllegalArgumentException("Expected an array");
                        for (var entry : entries) {
                            if (entry == null || !entry.valid()) throw new IllegalArgumentException("Invalid organ entry");
                            if (result.size() >= 128) throw new IllegalArgumentException("Maximum 128 organs");
                            // 内置定义优先：示例文件不覆盖硬编的翅膀 / 尾巴
                            if (exampleFile && originMap.get(entry.id()) == Origin.BUILTIN) continue;
                            var candidate = new LinkedHashMap<>(result);
                            candidate.put(entry.id(), entry);
                            if (JSON.toJson(candidate.values()).length() > 262144)
                                throw new IllegalArgumentException("Combined catalog too large to synchronize");
                            result.put(entry.id(), entry);
                            originMap.put(entry.id(), exampleFile ? Origin.EXAMPLE : Origin.CATALOG);
                        }
                    } catch (Exception e) { CorpseOrigin.LOGGER.warn("Cannot load organ catalog {}: {}", file, e.getMessage()); }
                }
            }
        } catch (Exception e) { CorpseOrigin.LOGGER.warn("Cannot load organ catalog", e); }
        packStatus=OrganPackCatalog.load(FabricLoader.getInstance().getConfigDir().resolve("corpseorigin/organ"),result,originMap);
        server = List.copyOf(result.values());
        origins = Map.copyOf(originMap);
    }
    public static List<OrganDefinition> builtin() {
        var out = new ArrayList<OrganDefinition>();
        var clips = Map.of("idle","evolution.idle","crouch","evolution.crouch","fly","evolution.fly",
                "glide","evolution.glide","swim","evolution.swim");
        for (String wing : List.of("bat", "feather")) for (String color : EvolutionAppearance.COLORS)
            out.add(new OrganDefinition(wing+"_"+color, "organ.corpseorigin."+wing+"_"+color,
                    "wings", "corpseorigin:geckolib/models/entity/organ_"+wing+".geo.json",
                    "corpseorigin:textures/entity/evolution/"+color+".png",
                    "corpseorigin:geckolib/animations/entity/organ_parts.animation.json", clips));
        out.add(new OrganDefinition("aquatic_tail", "organ.corpseorigin.aquatic_tail", "gills",
                "corpseorigin:geckolib/models/entity/organ_tail.geo.json", "corpseorigin:textures/entity/evolution/cyan.png",
                "corpseorigin:geckolib/animations/entity/organ_parts.animation.json", clips));
        return out;
    }
}
