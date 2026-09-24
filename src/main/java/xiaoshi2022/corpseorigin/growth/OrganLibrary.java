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
    private static List<OrganDefinition> server = List.of();
    public static Component packStatus = Component.empty();
    private OrganLibrary() {}
    public static List<OrganDefinition> definitions() { return server; }
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
        try {
            Files.createDirectories(dir);
            var example = dir.resolve("examples.json");
            if (!Files.exists(example)) Files.writeString(example, JSON.toJson(examples()), StandardCharsets.UTF_8);
            try (var files = Files.list(dir)) {
                for (var file : files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    try {
                        if (Files.size(file) > 262144) throw new IllegalArgumentException("Catalog file too large");
                        var entries = JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), OrganDefinition[].class);
                        if (entries == null) throw new IllegalArgumentException("Expected an array");
                        for (var entry : entries) {
                            if (entry == null || !entry.valid()) throw new IllegalArgumentException("Invalid organ entry");
                            if (result.size() >= 128) throw new IllegalArgumentException("Maximum 128 organs");
                            var candidate = new LinkedHashMap<>(result);
                            candidate.put(entry.id(), entry);
                            if (JSON.toJson(candidate.values()).length() > 262144)
                                throw new IllegalArgumentException("Combined catalog too large to synchronize");
                            result.put(entry.id(), entry);
                        }
                    } catch (Exception e) { CorpseOrigin.LOGGER.warn("Cannot load organ catalog {}: {}", file, e.getMessage()); }
                }
            }
        } catch (Exception e) { CorpseOrigin.LOGGER.warn("Cannot load organ catalog", e); }
        packStatus=OrganPackCatalog.load(FabricLoader.getInstance().getConfigDir().resolve("corpseorigin/organ"),result);
        server = List.copyOf(result.values());
    }
    public static List<OrganDefinition> examples() {
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
