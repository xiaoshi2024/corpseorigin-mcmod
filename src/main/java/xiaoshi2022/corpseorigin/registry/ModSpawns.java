package xiaoshi2022.corpseorigin.registry;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.event.RoleplayMode;
import xiaoshi2022.corpseorigin.mixin.SpawnPlacementsInvoker;
import xiaoshi2022.corpseorigin.growth.LostCitiesCompat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 尸兄的自然生成。
 * <p>
 * 之前模组的生物<b>全都只能靠刷怪蛋 / {@code /summon}</b>：既没注册生成规则，也没进任何生物群系的生成表，
 * 于是生存模式下永远碰不到尸兄 —— "感染 → 变尸兄 → 尸兄虫 → 初音"整条线都启动不了。
 * <p>
 * 每个生物要补两件事（缺一不可）：
 * <ol>
 *   <li><b>生成规则</b>（哪种地形、什么光照、要不要"在尸水泉边上"）——
 *       怪物统一用 {@link #corpseorigin$zbSpawnRules}（= 原版僵尸那套暗处规则 + 湖边聚集判定）；
 *       动物类用不查光照的宽松规则；</li>
 *   <li><b>进生物群系生成表</b> —— 用 Fabric 的 {@code BiomeModifications}。
 *       ⚠️ 不注册生成规则的话，{@code getPlacementType} 会退回 {@code NO_RESTRICTIONS}、
 *       {@code checkSpawnRules} 直接返回 {@code true}，怪会白天也刷、还能刷在半空。</li>
 * </ol>
 * 权重都相对原版僵尸（100）来定，想更稀/更密只改下面这几个常量。
 * <p>
 * 尸兄虫<b>刻意不单独生成</b>：它的来源是大叔被木棍打时吐出来（见 {@code UncleEntity}）。
 */
public final class ModSpawns {

    // ==================== 权重 ====================
    // 权重与"尸水泉聚集"都在 config/corpseorigin.json 的 spawn 段里，不用改代码。
    // 参照原版僵尸 = 100。

    // ==================== 尸水泉聚集 ====================

    /** 以出生点所在区块中心为中心，找这么大范围内的尸水（格） */
    private static final int LAKE_SCAN_RADIUS = 12;
    /** 纵向也扫一段（池子有深度，出生点也可能在池边高处） */
    private static final int LAKE_SCAN_HEIGHT = 8;
    /** 同一个区块的扫描结果缓存多久（tick）—— 刷怪判定每 tick 都会问，不能每次都扫 */
    private static final long LAKE_CACHE_TTL = 400L;
    /** 缓存条目上限，超了就整体清空（防长时间运行无限涨） */
    private static final int LAKE_CACHE_LIMIT = 8192;

    /**
     * 不在湖边时额外再筛一道的几率（来源：配置 {@code spawn.nearBywaterChance}）。
     * <p>
     * 启动时读一次缓存下来 —— 这个值每 tick 的生成判定都要用，不适合每次都去读配置对象。
     */
    private static float farFromLakeChance = 0.2F;

    /** 区块 → 该区块中心附近有没有尸水 */
    private static final Map<Long, CachedLake> LAKE_CACHE = new ConcurrentHashMap<>();

    private record CachedLake(boolean nearLake, long expireTick) {
    }

    // ==================== 群系选择 ====================

    /** 主世界所有群系 */
    private static final Predicate<BiomeSelectionContext> OVERWORLD =
            context -> context.hasTag(BiomeTags.IS_OVERWORLD);
    /** 雪原/冰刺那一类群系（企鹅的家） */
    private static final Predicate<BiomeSelectionContext> SNOWY =
            context -> context.hasTag(BiomeTags.SPAWNS_SNOW_FOXES);
    /** 森林系群系（橡木/桦木/黑森林/繁花森林等，乌鸦的家） */
    private static final Predicate<BiomeSelectionContext> FOREST =
            context -> context.hasTag(BiomeTags.IS_FOREST);

    private ModSpawns() {
    }

    public static void register() {
        CorpseConfig.Spawn config = CorpseConfig.get().spawn;
        farFromLakeChance = config.nearBywaterChance;
        // GUI 保存（CorpseConfig.replace）时刷新启动快照缓存，做到能实时实时的全部实时
        CorpseConfig.onReplaceCallback = ModSpawns::refreshAfterReload;

        registerSpawnRules();
        registerBiomeSpawns(config);
        CorpseOrigin.LOGGER.info("CorpseOrigin natural spawns registered");
    }

    /**
     * 跑 {@code /corpseconfig reload} 时调用：把启动时缓存下来的"远湖通过几率"再刷一次，
     * 否则改完配置不重启服务器的话这个值一直是老的。
     * <p>
     * 自然生成权重（{@code registerBiomeSpawns}）和生成规则（{@code registerSpawnRules}）
     * 在服务器启动时就注册死了没法热更，要改这两条只能重启 ——
     * 但僵尸禁用开关走的是 {@link xiaoshi2022.corpseorigin.mixin.SpawnPlacementsMixin}，
     * 那条路每 tick 都直接读 {@link CorpseConfig#get()}，所以<b>实时</b>生效。
     */
    public static void refreshAfterReload() {
        farFromLakeChance = CorpseConfig.get().spawn.nearBywaterChance;
    }

    /** ① 生成规则 */
    private static void registerSpawnRules() {
        for (EntityType<?> type : new EntityType[]{
                ModEntities.LOWER_LEVEL_ZB, ModEntities.AOTUMAN_ZB,
                ModEntities.COCO_ZOMBIE, ModEntities.COCO_ZOMBIE_X, ModEntities.MIKU_ZB}) {
            corpseorigin$registerZbSpawnRules(type);
        }

        // 企鹅住在雪地上：不用 Animal 那套"脚下必须是草"的规则，否则雪原里一只都刷不出来
        SpawnPlacementsInvoker.corpseorigin$register(
                ModEntities.COCO_PENGUIN,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> !roleplayBlocksNaturalSpawn(level, reason)
                        && level.getDifficulty() != Difficulty.PEACEFUL);

        // 乌鸦尸兄（中立动物类）：落地生成即可，不查光照； roleplay / 和平照拦
        SpawnPlacementsInvoker.corpseorigin$register(
                ModEntities.RAVEN_ZBR,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> !roleplayBlocksNaturalSpawn(level, reason)
                        && level.getDifficulty() != Difficulty.PEACEFUL
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random));

        // 大叔是"可遇 NPC"：白天也会在草地上溜达
        SpawnPlacementsInvoker.corpseorigin$register(
                ModEntities.UNCLE,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> !roleplayBlocksNaturalSpawn(level, reason)
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random));

        // 哈姆是"村里的狗"：只在村庄旁边刷（判据抄原版猫，见 corpseorigin$hamSpawnRules）
        SpawnPlacementsInvoker.corpseorigin$register(
                ModEntities.HAM,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> corpseorigin$hamSpawnRules(type, level, reason, pos, random));

        SpawnPlacementsInvoker.corpseorigin$register(
                ModEntities.DAMO,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> corpseorigin$damoSpawnRules(type, level, reason, pos, random));
        SpawnPlacementsInvoker.corpseorigin$register(
                ModEntities.YU_DOCTOR, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> !roleplayBlocksNaturalSpawn(level, reason)
                        && level.getDifficulty() != Difficulty.PEACEFUL
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                        && (reason != EntitySpawnReason.NATURAL || !level.getEntitiesOfClass(
                        xiaoshi2022.corpseorigin.entity.DamoEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(48), Entity::isAlive).isEmpty()));
        SpawnPlacementsInvoker.corpseorigin$register(
                ModEntities.TIAN_DOCTOR, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> !roleplayBlocksNaturalSpawn(level, reason)
                        && level.getDifficulty() != Difficulty.PEACEFUL
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random));

        // 尸兄·尔多兽王 BOSS：注册生成规则（让刷怪蛋 + 指令 + 刷怪笼照常能用），
        // 但<b>刻意不进任何生物群系的生成表</b> —— 600 血 BOSS 野外自然刷=灾难。
        // 想真召唤 BOSS 走 /summoneldor 指令 或剧情事件，别用自然生成。
        // 规则用 corpseorigin$zbSpawnRules 那套尸兄专用规则（受 roleplay 拦截、和平模式拦），
        // 走 SpawnEggItem 召唤时不走 NATURAL 分支，所以"湖边聚集"判定不触发。
        SpawnPlacementsInvoker.corpseorigin$register(
                ModEntities.ELDOR_KING_ZBR,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModSpawns::corpseorigin$zbSpawnRules);
    }

    private static boolean corpseorigin$damoSpawnRules(EntityType<? extends Mob> type, ServerLevelAccessor level,
                                                        EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (roleplayBlocksNaturalSpawn(level, reason)) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) return false;
        if (reason != EntitySpawnReason.NATURAL) return true;
        if (!level.getLevel().isCloseToVillage(pos, 2)) return false;
        return level.getEntitiesOfClass(xiaoshi2022.corpseorigin.entity.DamoEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(128), entity -> entity.isAlive()).isEmpty();
    }

    /**
     * 哈姆的生成规则：和平模式拦掉 → 常规生物落地判定 → <b>必须在村庄旁边</b>。
     * <p>
     * 村庄判据完全照搬原版猫（{@code CatSpawner.spawnInVillage}）：2 个区段内有村庄，
     * 且 48 格内至少有 5 张被村民占用的床。
     * {@code isCloseToVillage} 对绝大多数位置直接返回 false，所以那个稍贵的 POI 计数
     * 只在村庄附近才真正执行，不会拖慢野外刷怪。
     */
    private static boolean corpseorigin$hamSpawnRules(EntityType<? extends Mob> type, ServerLevelAccessor level,
                                                      EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (roleplayBlocksNaturalSpawn(level, reason)) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        return level.getLevel().isCloseToVillage(pos, 2)
                && level.getLevel().getPoiManager().getCountInRange(
                        home -> home.is(PoiTypes.HOME), pos, 48, PoiManager.Occupancy.IS_OCCUPIED) > 4L;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void corpseorigin$registerZbSpawnRules(EntityType<?> type) {
        SpawnPlacementsInvoker.corpseorigin$register(
                (EntityType) type,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModSpawns::corpseorigin$zbSpawnRules);
    }

    /**
     * 尸兄专用生成规则：和平模式拦掉 → 原版怪物那套（站实心地上 + 暗处）→ 再看"是不是在尸水泉边上"。
     * <p>
     * ⚠️ <b>和平模式这一判必须自己写</b>：26.2 起 {@code Monster.checkMonsterSpawnRules} 里
     * <b>已经不含</b>和平模式判断了 —— 原版把这条挪到了 {@code EntityType#isAllowedInPeaceful()}
     * （由 {@code Mob#checkDespawn} 负责清除已存在的、{@code EntityType#canSummon} 负责挡召唤）。
     * 所以类型上的 {@code notInPeaceful()} 管"已经存在的会被清掉"，这里管"压根不刷"。
     * <p>
     * 权重是全局的、没法按位置变，所以"湖边更密"是在这里再做一道概率门实现的：
     * <b>湖边直接放行，别处只有 {@link #farFromLakeChance} 的几率通过</b>。
     * <p>
     * 非自然来源（刷怪蛋 / 刷怪笼 / 指令）不参与"湖边聚集"，免得你手动放一只还被筛掉。
     */
    private static boolean corpseorigin$zbSpawnRules(EntityType<? extends Mob> type, ServerLevelAccessor level,
                                                    EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (roleplayBlocksNaturalSpawn(level, reason)) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (!Monster.checkMonsterSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        if (reason != EntitySpawnReason.NATURAL || level.getLevel().dimension().equals(
                xiaoshi2022.corpseorigin.skill.longyou.CorpseNestDimension.KEY)) {
            return true;
        }
        Boolean city = LostCitiesCompat.isCity(level.getLevel(), pos);
        if (Boolean.TRUE.equals(city)) return true;
        if (Boolean.FALSE.equals(city))
            return random.nextFloat() < CorpseConfig.get().spawn.lostCitiesOutsideSpawnChance;
        return corpseorigin$nearBywater(level, pos) || random.nextFloat() < farFromLakeChance;
    }

    private static boolean roleplayBlocksNaturalSpawn(ServerLevelAccessor level, EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL && RoleplayMode.isEnabled(level.getLevel().getServer());
    }

    /** 这个出生点附近有没有尸水（按区块缓存，见 {@link #LAKE_CACHE_TTL}） */
    private static boolean corpseorigin$nearBywater(ServerLevelAccessor level, BlockPos pos) {
        long chunkKey = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        long now = level.getLevel().getGameTime();

        CachedLake cached = LAKE_CACHE.get(chunkKey);
        if (cached != null && cached.expireTick() > now) {
            return cached.nearLake();
        }

        boolean near = corpseorigin$scanForBywater(level, pos);
        if (LAKE_CACHE.size() > LAKE_CACHE_LIMIT) {
            LAKE_CACHE.clear();
        }
        LAKE_CACHE.put(chunkKey, new CachedLake(near, now + LAKE_CACHE_TTL));
        return near;
    }

    /**
     * 扫一圈找尸水。
     * <p>
     * 以<b>区块中心</b>（而不是出生点本身）为基准，同一个区块里所有出生点得到同一个结果，
     * 缓存才有意义；横竖都隔一格取样 —— 池子有好几格宽，不会漏。
     */
    private static boolean corpseorigin$scanForBywater(ServerLevelAccessor level, BlockPos pos) {
        int centerX = (pos.getX() >> 4 << 4) + 8;
        int centerZ = (pos.getZ() >> 4 << 4) + 8;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -LAKE_SCAN_RADIUS; dx <= LAKE_SCAN_RADIUS; dx += 2) {
            for (int dz = -LAKE_SCAN_RADIUS; dz <= LAKE_SCAN_RADIUS; dz += 2) {
                for (int dy = -LAKE_SCAN_HEIGHT; dy <= LAKE_SCAN_HEIGHT; dy += 2) {
                    cursor.set(centerX + dx, pos.getY() + dy, centerZ + dz);
                    if (level.getBlockState(cursor).is(ModFluids.INFECTED_WATER_BLOCK)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** ② 进生成表（权重来自配置）。注意：BiomeModifications 只在游戏启动时注册一次，运行时改权重需重启生效。 */
    private static void registerBiomeSpawns(CorpseConfig.Spawn spawn) {
        // 主世界夜晚：尸兄三兄弟 + 初音（合体形态权重极低）；权重 0 = 从生成表摘除
        if (spawn.lowerLevelZbWeight > 0) {
            BiomeModifications.addSpawn(OVERWORLD, MobCategory.MONSTER,
                    ModEntities.LOWER_LEVEL_ZB, spawn.lowerLevelZbWeight, 1, 2);
        }
        if (spawn.aotumanZbWeight > 0) {
            BiomeModifications.addSpawn(OVERWORLD, MobCategory.MONSTER,
                    ModEntities.AOTUMAN_ZB, spawn.aotumanZbWeight, 1, 1);
        }
        if (spawn.mikuZbWeight > 0) {
            BiomeModifications.addSpawn(OVERWORLD, MobCategory.MONSTER,
                    ModEntities.MIKU_ZB, spawn.mikuZbWeight, 1, 1);
        }
        if (spawn.cocoZombieWeight > 0) {
            BiomeModifications.addSpawn(OVERWORLD, MobCategory.MONSTER,
                    ModEntities.COCO_ZOMBIE, spawn.cocoZombieWeight, 1, 1);
        }
        // CoCo 尸兄·二阶段（合体形态）刻意不进生成表：它按设定是企鹅与大叔的合体产物，
        // 只应由合体流程产生（生成规则仍保留，刷怪蛋 / 指令 / 合体照常能用）。

        // 雪原：CoCo 企鹅（尸兄线的起点）
        BiomeModifications.addSpawn(SNOWY, MobCategory.CREATURE,
                ModEntities.COCO_PENGUIN, spawn.cocoPenguinWeight, 2, 3);

        // 森林系群系：乌鸦尸兄（中立，白天成小群出没；权重低密度，1~2 只一群）
        BiomeModifications.addSpawn(FOREST, MobCategory.CREATURE,
                ModEntities.RAVEN_ZBR, 8, 1, 2);

        // 主世界：大叔（打他会吐尸兄虫）
        BiomeModifications.addSpawn(OVERWORLD, MobCategory.CREATURE,
                ModEntities.UNCLE, spawn.uncleWeight, 1, 1);
        BiomeModifications.addSpawn(OVERWORLD, MobCategory.CREATURE, ModEntities.DAMO, 3, 1, 1);
        BiomeModifications.addSpawn(OVERWORLD, MobCategory.CREATURE, ModEntities.YU_DOCTOR, 1, 1, 1);
        BiomeModifications.addSpawn(OVERWORLD, MobCategory.CREATURE, ModEntities.TIAN_DOCTOR, 1, 1, 1);

        // 主世界：哈姆（有规则卡着"只在村庄附近"，权重再低也只在村里出）
        BiomeModifications.addSpawn(OVERWORLD, MobCategory.CREATURE,
                ModEntities.HAM, spawn.hamWeight, 1, 1);
    }
}
