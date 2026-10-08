package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.compat.lostcities.LostCitiesCompat;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.effect.BYeffect;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 感染领域 tick 推进器。
 * <p>
 * 维护一份 {@code 施术者 UUID → Field} 的活跃实例表（参考 {@link InfrasoundFieldHandler} 的 ACTIVE Map 模式），
 * 由 {@code LongYouEventHandler} 注册到 {@code ServerTickEvents.END_SERVER_TICK}，
 * 每 tick 推进所有活跃领域，按 {@code scanIntervalTicks} 节流做感染扫描。
 * <p>
 * <b>领域形态</b>：
 * <ul>
 *   <li>{@link Mode#MOVING}：以施术者当前位置为中心，{@code caster.getBoundingBox().inflate(radius)}</li>
 *   <li>{@link Mode#ANCHORED}：以释放时记录的 {@code startPos} 为中心，{@code new AABB(startPos).inflate(radius)}</li>
 * </ul>
 * <b>Lost City 加成</b>：若 {@code enableLostCityIntegration=true} 且 Lost Cities 模组加载，
 * 用 {@link LostCitiesCompat#cityBoundsAround} 替代固定半径的 AABB（取城市边界并集）。
 * <p>
 * <b>感染扫描</b>：扫描领域内 {@link Monster} 子类实体，跳过已尸化（{@link ZombieKin#isZombieKin}）
 * 与已感染（带 {@link ModEffects#QIANS}），按 {@code infectionChance} 概率施加 {@link BYeffect#applyInfection}。
 */
public final class InfectionDomainHandler {

    private InfectionDomainHandler() {
    }

    /** 施术者 UUID → 正在持续的领域 */
    private static final Map<UUID, Field> ACTIVE = new ConcurrentHashMap<>();

    /**
     * 开/关切换。
     *
     * @param caster  施术者
     * @param anchored true = 固定锚点模式；false = 跟随移动模式
     * @return true = 领域已开启；false = 领域已关闭（或被新领域顶替）
     */
    public static boolean toggle(ServerPlayer caster, boolean anchored) {
        UUID uuid = caster.getUUID();
        if (ACTIVE.containsKey(uuid)) {
            // 已经开着：再按就关掉
            ACTIVE.remove(uuid);
            return false;
        }
        CorpseConfig.InfectionDomain cfg = CorpseConfig.get().infectionDomain;
        if (cfg == null || !cfg.enabled) {
            return false;
        }
        Field field = new Field(
                uuid,
                caster.position(),
                anchored ? Mode.ANCHORED : Mode.MOVING,
                cfg.radius,
                caster.tickCount
        );
        ACTIVE.put(uuid, field);
        CorpseOrigin.LOGGER.info("感染领域已展开：施术者={}，模式={}，半径={}",
                caster.getName().getString(), field.mode, field.radius);
        return true;
    }

    /** 主动释放：玩家下线/饱食度耗尽时调用，清除领域 */
    public static void release(UUID uuid) {
        if (uuid == null) return;
        ACTIVE.remove(uuid);
    }

    /** 这位玩家当前是不是开着感染领域 */
    public static boolean isActive(UUID uuid) {
        return uuid != null && ACTIVE.containsKey(uuid);
    }

    /** 服务端每 tick 推进所有活跃领域（由 LongYouEventHandler 注册到 ServerTickEvents） */
    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;

        CorpseConfig.InfectionDomain cfg = CorpseConfig.get().infectionDomain;
        if (cfg == null || !cfg.enabled) {
            // 配置总闸关掉：清掉所有活跃领域
            ACTIVE.clear();
            return;
        }

        Iterator<Map.Entry<UUID, Field>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Field> entry = iterator.next();
            Field field = entry.getValue();

            ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());
            if (caster == null || !caster.isAlive() || caster.hasDisconnected()) {
                iterator.remove();
                continue;
            }
            if (!(caster.level() instanceof ServerLevel level)) {
                continue;
            }

            // MOVING 模式：施术者下线/换维度时领域自然失效（ caster 在新维度会重新构造 AABB）
            // 节流：按 scanIntervalTicks 控制扫描频率
            if (caster.tickCount - field.lastScanTick < cfg.scanIntervalTicks) {
                continue;
            }
            field.lastScanTick = caster.tickCount;

            AABB area = computeArea(caster, field, cfg);
            if (area == null) continue;

            scanAndInfect(level, caster, field, area, cfg);
        }
    }

    /** 根据领域模式构造扫描 AABB；Lost City 加成时取城市边界 */
    private static AABB computeArea(ServerPlayer caster, Field field, CorpseConfig.InfectionDomain cfg) {
        Vec3 center;
        if (field.mode == Mode.ANCHORED) {
            center = field.startPos;
        } else {
            center = caster.position();
        }

        double radius = field.radius;
        if (cfg.enableLostCityIntegration && LostCitiesCompat.isLoaded()) {
            // Lost City 加成：按 cityCoverageMultiplier 放大半径，并优先取城市边界
            double cityRadius = radius * cfg.cityCoverageMultiplier;
            return LostCitiesCompat.cityBoundsAround((ServerLevel) caster.level(), center, cityRadius);
        }
        return new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
    }

    /** 扫描领域内的 Monster 子类，按感染概率施加 BYeffect.applyInfection */
    private static void scanAndInfect(ServerLevel level, ServerPlayer caster, Field field,
                                      AABB area, CorpseConfig.InfectionDomain cfg) {
        List<Monster> monsters;
        try {
            monsters = level.getEntitiesOfClass(Monster.class, area);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("感染领域扫描实体失败：{}", e.getMessage());
            return;
        }

        int infected = 0;
        for (Monster m : monsters) {
            if (!m.isAlive() || m.isRemoved()) continue;
            // 已是尸族（含半尸兄）：跳过
            if (ZombieKin.isZombieKin(m)) continue;
            // 已在感染中：跳过
            if (m.hasEffect(ModEffects.QIANS)) continue;

            // 概率感染
            if (caster.getRandom().nextFloat() >= cfg.infectionChance) continue;

            BYeffect.applyInfection(m, level, caster.getUUID());
            infected++;
            // 表现：感染者身上飘一蓬尸水绿雾
            infectFeedback(level, m);
        }

        if (infected > 0) {
            CorpseOrigin.LOGGER.debug("感染领域扫描：施术者={}，命中 {} 只",
                    caster.getName().getString(), infected);
        }
    }

    /** 感染成功时的表现：一蓬尸水绿雾 + 咕嘟声（参考 LongYouEventHandler.infectFeedback） */
    private static void infectFeedback(ServerLevel level, Mob target) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();
        QiEffects.burst(level, x, y, z, 0x3a8c3a, 6, 0.15);
        level.playSound(null, x, y, z,
                SoundEvents.WATER_AMBIENT, net.minecraft.sounds.SoundSource.HOSTILE, 0.6F, 0.5F);
    }

    // ==================== 数据结构 ====================

    /** 领域形态 */
    public enum Mode {
        /** 跟随施术者移动 */
        MOVING,
        /** 固定锚点（释放时位置钉死，施术者可离开） */
        ANCHORED
    }

    /** 一个领域实例的状态 */
    private static final class Field {
        final UUID casterUuid;
        /** 释放时的位置；MOVING 模式下每次 tick 用 caster.position() 重算，ANCHORED 模式下钉死 */
        final Vec3 startPos;
        final Mode mode;
        final double radius;
        /** 上一次扫描 tick，用于节流 */
        int lastScanTick;

        Field(UUID casterUuid, Vec3 startPos, Mode mode, double radius, int startTick) {
            this.casterUuid = casterUuid;
            this.startPos = startPos;
            this.mode = mode;
            this.radius = radius;
            this.lastScanTick = startTick;
        }
    }
}
