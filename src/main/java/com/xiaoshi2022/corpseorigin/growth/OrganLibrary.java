
package com.xiaoshi2022.corpseorigin.growth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

/** Server-approved definitions; local resources never register a remote server organ. */
public final class OrganLibrary {
    public static final Gson JSON = new Gson();
    private static final Type SLOTS = new TypeToken<List<OrganSlot>>() {}.getType();
    private static volatile List<OrganDefinition> definitions = List.of();
    private static volatile Set<String> custom = Set.of();
    public static net.minecraft.network.chat.Component packStatus = net.minecraft.network.chat.Component.empty();
    private OrganLibrary() {}
    public static Path directory() { return FMLPaths.CONFIGDIR.get().resolve("corpseorigin/organ"); }
    public static List<OrganDefinition> definitions() { return definitions; }
    public static OrganDefinition find(String id) { return definitions.stream().filter(d -> d.id().equals(id)).findFirst().orElse(null); }
    public static boolean isCustomOrgan(String id) { return custom.contains(id); }
    public static List<OrganSlot> parseSlots(String json) {
        if (json == null || json.isBlank()) return List.of();
        if (json.length() > 8192) throw OrganText.error("too_long");
        try {
            List<OrganSlot> slots = JSON.fromJson(json, SLOTS);
            if (slots == null || slots.size() > 8 || slots.stream().anyMatch(s -> s == null || !s.valid()))
                throw OrganText.error("invalid_slots");
            return List.copyOf(slots);
        } catch (com.google.gson.JsonParseException | IllegalStateException e) {
            throw OrganText.error("invalid_json");
        }
    }
    public static String toJson(List<OrganSlot> slots) { return JSON.toJson(slots == null ? List.of() : slots); }
    public static void load() {
        Map<String, OrganDefinition> out = new LinkedHashMap<>();
        for (String wing : List.of("bat", "feather", "tail")) {
            var clips = Map.of("idle", "evolution.idle", "crouch", "evolution.crouch", "fly", "evolution.fly", "glide", "evolution.glide", "swim", "evolution.swim");
            out.put(wing, new OrganDefinition(wing, "organ_editor.corpseorigin." + wing, wing.equals("tail") ? "gills" : "wings",
                    "corpseorigin:geo/entity/organ_" + wing + ".geo.json", "corpseorigin:textures/entity/evolution/cyan.png",
                    "corpseorigin:animations/entity/organ_parts.animation.json", clips));
        }
        Set<String> ids = new HashSet<>(); List<net.minecraft.network.chat.Component> errors = new ArrayList<>();
        Path legacy = directory().resolveSibling("organs");
        try {
            Files.createDirectories(legacy);
            try (var files = Files.list(legacy)) {
                for (Path p : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                    try { OrganPacks.readDefinitions(Files.newInputStream(p), out, ids); }
                    catch (Exception e) { errors.add(OrganText.text("pack_error",p.getFileName().toString(),OrganText.failure(e))); }
                }
            }
            for (Path p : OrganPacks.sources(directory())) {
                try { OrganPacks.readManifest(p, out, ids); }
                catch (Exception e) { errors.add(OrganText.text("pack_error",p.getFileName().toString(),OrganText.failure(e))); }
            }
        } catch (Exception e) { errors.add(OrganText.failure(e)); }
        definitions = List.copyOf(out.values()); custom = Set.copyOf(ids);
        packStatus = errors.isEmpty() ? OrganText.text("loaded",out.size()) : OrganText.text("pack_errors",net.minecraft.network.chat.ComponentUtils.formatList(errors, net.minecraft.network.chat.Component.literal("; ")));
        if (!errors.isEmpty()) com.xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("Organ catalogs: {}", packStatus);
    }
}
