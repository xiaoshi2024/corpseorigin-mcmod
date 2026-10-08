package xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 半尸兄（混血尸兄）标签工具类。
 * <p>
 * 原版怪物被感染领域或龙右感染后，会转换为 {@link LowerLevelZbEntity} 基底，
 * 但通过 NBT 标签保留"原宿主类型"信息，形成"尸兄小黑/尸兄苦力怕"等混血概念。
 * <p> 
 * 标签三件套：
 * <ul>
 *   <li>{@link #ORIGINAL_SPECIES}：原宿主的实体类型 id（如 minecraft:enderman）</li>
 *   <li>{@link #HYBRID_GRADE}：混血等级（{@code half} / {@code full}）；{@code half} 表示由原版怪物感染而来</li>
 *   <li>{@link #INFECTION_GENERATION}：感染代数（龙右本体感染=1，半尸兄同族传播=2，以此类推）</li>
 * </ul>
 * 持久化走 {@link LowerLevelZbEntity#addAdditionalSaveData} / {@link LowerLevelZbEntity#readAdditionalSaveData}
 * 末尾的调用，区块重载不丢。
 */
public final class HybridZombie {

    private HybridZombie() {
    }

    /** 原宿主实体类型 id 的 NBT 键（如 minecraft:zombie） */
    public static final String ORIGINAL_SPECIES = "corpseorigin:original_species";
    /** 混血等级的 NBT 键：half / full */
    public static final String HYBRID_GRADE = "corpseorigin:hybrid_grade";
    /** 感染代数的 NBT 键（龙右本体=1，同族传播累加） */
    public static final String INFECTION_GENERATION = "corpseorigin:infection_generation";

    /** 半尸兄等级标记：原版怪物被龙右感染领域或近战感染后转换得到 */
    public static final String GRADE_HALF = "half";
    /** 全尸兄等级标记：当前未在自动转换链路使用，预留给后续纯血尸兄 */
    public static final String GRADE_FULL = "full";

    /**
     * 在 LowerLevelZbEntity 上写入半尸兄标签。
     * <p>
     * 通过 entityTags() / addTag() 写入：26.2 的 {@code Entity#addTag} 走的是 PersistentEntityTagSet，
     * 自动随实体 NBT 持久化（{@code Entity#save} 会写 "Tags" 列表）。
     * <p>
     * 同时也写一份到 ValueOutput/ValueInput（{@link #writeNbt} / {@link #readNbt}），
     * 一来与现有器官/执念等系统的 NBT 风格保持一致、便于 {@code /data get} 直接查看，
     * 二来兜底：实体从磁盘读回时 {@code entityTags()} 在 {@link net.minecraft.world.entity.Entity#readAdditionalSaveData}
     * 之前是空的，需要 {@link #readNbt} 主动重建。
     *
     * @param zb          被感染的 LowerLevelZbEntity
     * @param speciesKey  原宿主实体类型 id（{@link net.minecraft.world.entity.EntityType#getKey} 的 toString）
     * @param grade       {@link #GRADE_HALF} 或 {@link #GRADE_FULL}
     * @param generation  感染代数（龙右本体感染=1，同族传播=上一代+1）
     */
    public static void tagAsHybrid(LowerLevelZbEntity zb, String speciesKey, String grade, int generation) {
        if (zb == null || speciesKey == null) return;
        zb.addTag(ORIGINAL_SPECIES + ":" + speciesKey);
        zb.addTag(HYBRID_GRADE + ":" + grade);
        zb.addTag(INFECTION_GENERATION + ":" + Math.max(1, generation));
    }

    /** 这只 LowerLevelZbEntity 是不是半尸兄 */
    public static boolean isHybrid(Entity entity) {
        return entity != null && speciesKey(entity) != null;
    }

    /**
     * 取原宿主类型 id（如 minecraft:enderman）；不是半尸兄返回 null。
     * <p>
     * 标签格式：{@code corpseorigin:original_species:minecraft:enderman} —— 冒号分隔，
     * 因为 speciesKey 本身带冒号，所以用 startsWith + 子串截取。
     */
    public static String speciesKey(Entity entity) {
        if (entity == null) return null;
        String prefix = ORIGINAL_SPECIES + ":";
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(prefix)) {
                return tag.substring(prefix.length());
            }
        }
        return null;
    }

    /** 取混血等级（half / full）；不是半尸兄返回 null */
    public static String grade(Entity entity) {
        if (entity == null) return null;
        String prefix = HYBRID_GRADE + ":";
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(prefix)) {
                return tag.substring(prefix.length());
            }
        }
        return null;
    }

    /** 取感染代数；不是半尸兄返回 0 */
    public static int generation(Entity entity) {
        if (entity == null) return 0;
        String prefix = INFECTION_GENERATION + ":";
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(prefix)) {
                try {
                    return Integer.parseInt(tag.substring(prefix.length()));
                } catch (NumberFormatException ignored) {
                    return 1;
                }
            }
        }
        return 0;
    }

    // ==================== NBT 持久化（与 entityTags() 双写，互为兜底） ====================

    /** 在 {@link LowerLevelZbEntity#addAdditionalSaveData} 末尾调用 */
    public static void writeNbt(ValueOutput output, LowerLevelZbEntity zb) {
        if (output == null || zb == null) return;
        String species = speciesKey(zb);
        if (species != null) {
            output.putString("HybridSpecies", species);
            output.putString("HybridGrade", grade(zb) == null ? GRADE_HALF : grade(zb));
            output.putInt("HybridGeneration", generation(zb));
        }
    }

    /** 在 {@link LowerLevelZbEntity#readAdditionalSaveData} 末尾调用，把 NBT 数据回填到 entityTags */
    public static void readNbt(ValueInput input, LowerLevelZbEntity zb) {
        if (input == null || zb == null) return;
        // 老存档/未感染实体没有这一节；用 getString().isPresent() 判断
        java.util.Optional<String> speciesOpt = input.getString("HybridSpecies");
        if (speciesOpt.isEmpty()) return;

        String species = speciesOpt.get();
        if (species.isEmpty()) return;

        String grade = input.getString("HybridGrade").orElse(GRADE_HALF);
        int generation = input.getIntOr("HybridGeneration", 1);

        // 重建 entityTags（实体从磁盘读回时 entityTags() 在 readAdditionalSaveData 末尾仍是空的，
        // 需要主动重建；Entity#readAdditionalLoadData 之后会有一次 entityTags 的整体加载，重复 addTag 安全）
        zb.addTag(ORIGINAL_SPECIES + ":" + species);
        zb.addTag(HYBRID_GRADE + ":" + grade);
        zb.addTag(INFECTION_GENERATION + ":" + Math.max(1, generation));
    }
}
