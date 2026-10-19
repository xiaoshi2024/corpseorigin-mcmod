package com.xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * 原版感染生物的物种/代数兼容工具。原版宿主保持原实体，
 * 通过 entityTags 持久化混血标记（与 26.2 Fabric 版本一致）。
 */
public final class HybridZombie {
    private HybridZombie() {}

    public static final String ORIGINAL_SPECIES = "corpseorigin:original_species";
    public static final String HYBRID_GRADE = "corpseorigin:hybrid_grade";
    public static final String INFECTION_GENERATION = "corpseorigin:infection_generation";

    public static final String GRADE_HALF = "half";
    public static final String GRADE_FULL = "full";

    public static void tagAsHybrid(Mob zb, String speciesKey, String grade, int generation) {
        if (zb == null || speciesKey == null) return;
        zb.addTag(ORIGINAL_SPECIES + ":" + speciesKey);
        zb.addTag(HYBRID_GRADE + ":" + grade);
        zb.addTag(INFECTION_GENERATION + ":" + Math.max(1, generation));
    }

    public static boolean isHybrid(Entity entity) {
        return entity != null && (com.xiaoshi2022.corpseorigin.growth.NativeCorpseState.infected(entity) || speciesKey(entity) != null);
    }

    public static String speciesKey(Entity entity) {
        if (entity == null) return null;
        if (com.xiaoshi2022.corpseorigin.growth.NativeCorpseState.infected(entity))
            return net.minecraft.world.entity.EntityType.getKey(entity.getType()).toString();
        String prefix = ORIGINAL_SPECIES + ":";
        for (String tag : entity.getTags()) {
            if (tag.startsWith(prefix)) return tag.substring(prefix.length());
        }
        return null;
    }

    public static String grade(Entity entity) {
        if (entity == null) return null;
        if (com.xiaoshi2022.corpseorigin.growth.NativeCorpseState.infected(entity)) return GRADE_HALF;
        String prefix = HYBRID_GRADE + ":";
        for (String tag : entity.getTags()) {
            if (tag.startsWith(prefix)) return tag.substring(prefix.length());
        }
        return null;
    }

    public static int generation(Entity entity) {
        if (entity == null) return 0;
        int nativeGen = com.xiaoshi2022.corpseorigin.growth.NativeCorpseState.generation(entity);
        if (nativeGen > 0) return nativeGen;
        String prefix = INFECTION_GENERATION + ":";
        for (String tag : entity.getTags()) {
            if (tag.startsWith(prefix)) {
                try { return Integer.parseInt(tag.substring(prefix.length())); }
                catch (NumberFormatException ignored) { return 1; }
            }
        }
        return 0;
    }
}
