package com.xiaoshi2022.corpseorigin.growth;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import java.util.Map;
import java.util.Set;

/** Portable subset of the 26.2 manifest: GEO, texture and motion clips. */
public record OrganDefinition(String id, String name, String trait, String model, String texture,
                              String animation, Map<String, String> clips) {
    public Component displayName() { return name.startsWith("organ.") || name.startsWith("organ_editor.") ? Component.translatable(name) : Component.literal(name); }
    public static String legacyPath(String path) {
        return path == null ? null : path.replace(":geckolib/models/", ":geo/").replace(":geckolib/animations/", ":animations/");
    }
    public OrganDefinition normalized() { return new OrganDefinition(id, name, trait, legacyPath(model), texture, legacyPath(animation), clips); }
    public boolean valid() {
        return id != null && !id.isBlank() && id.length() <= 96 && name != null && !name.isBlank() && name.length() <= 80
                && Set.of("cosmetic", "wings", "gills", "vampire").contains(trait == null ? "" : trait)
                && resource(legacyPath(model), "geo/", ".geo.json") && resource(texture, "textures/", ".png")
                && resource(legacyPath(animation), "animations/", ".animation.json")
                && clips != null && clips.size() <= 10 && clips.containsKey("idle")
                && clips.keySet().stream().allMatch(k -> k != null && Set.of("idle", "walk", "attack", "crouch", "fly", "glide", "swim").contains(k))
                && clips.values().stream().allMatch(v -> v != null && !v.isBlank() && v.length() <= 96);
    }
    private static boolean resource(String value, String prefix, String suffix) {
        if (value == null || value.length() > 192 || value.contains("..")) return false;
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id != null && id.getPath().startsWith(prefix) && id.getPath().endsWith(suffix);
    }
}
