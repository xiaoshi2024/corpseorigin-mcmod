package com.xiaoshi2022.corpseorigin.event;

import com.xiaoshi2022.corpseorigin.config.CorpseOriginServerConfig;
import com.xiaoshi2022.corpseorigin.effect.BYeffect;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import com.xiaoshi2022.corpseorigin.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * 尸兄血雨事件（1.20.1 Forge 版本）。
 * <p>
 * 事件期间，玩家周围露天生物每秒进行一次判定，命中后进入尸变潜伏期，
 * 最终由 {@link BYeffect} / {@link NativeCorpseState} 完成转化。
 * <p>
 * 触发方式：
 * <ul>
 *   <li>自然触发：下雨时按 {@code bloodRain.naturalChance} 概率自动开始；</li>
 *   <li>手动触发：{@code /bloodrain start [秒数] [概率]}，不受天气限制。</li>
 * </ul>
 * 所有数值来自 {@link CorpseOriginServerConfig}，运行中改配置文件即时生效；
 * {@code /bloodrain chance <0~1>} 可临时覆盖本次事件的变异概率。
 */
public final class BloodRainManager {

    private static boolean active;
    /** true = 指令手动开启，不受"雨停"影响。 */
    private static boolean manual;
    /** 结束时间（游戏 tick），-1 = 无限持续。 */
    private static long endsAt = -1L;
    /** 本次事件的变异概率覆盖，-1 = 使用配置值。 */
    private static float chanceOverride = -1.0F;
    private static long nextNaturalCheck;

    private BloodRainManager() {
    }

    /** 服务端每 tick 调用（ModEvents#onServerTick）。 */
    public static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (level == null) return;
        long now = level.getGameTime();

        if (!active) {
            if (!CorpseOriginServerConfig.bloodRainNaturalEnabled()) return;
            if (now < nextNaturalCheck) return;
            nextNaturalCheck = now + CorpseOriginServerConfig.bloodRainNaturalCheckSeconds() * 20L;
            if (!level.isRaining()) return;
            if (level.getRandom().nextDouble() >= CorpseOriginServerConfig.bloodRainNaturalChance()) return;
            start(server, CorpseOriginServerConfig.bloodRainDefaultDurationSeconds(), -1.0F, false);
            return;
        }

        // 自然触发：雨停即结束
        if (!manual && !level.isRaining()) {
            stop(server, true);
            return;
        }
        if (endsAt > 0 && now >= endsAt) {
            stop(server, true);
            return;
        }

        if (now % 5L == 0L) spawnParticles(level);

        int interval = CorpseOriginServerConfig.bloodRainScanIntervalTicks();
        if (interval > 0 && now % interval == 0L) mutateExposedMobs(level);
    }

    /**
     * 开启血雨。
     *
     * @param seconds     持续时间（秒），<= 0 表示无限
     * @param chance      本次变异概率覆盖（0~1），< 0 表示使用配置值
     * @param manualStart true = 指令触发（不受天气限制）
     */
    public static void start(MinecraftServer server, int seconds, float chance, boolean manualStart) {
        ServerLevel level = server.overworld();
        long now = level == null ? 0L : level.getGameTime();
        active = true;
        manual = manualStart;
        endsAt = seconds > 0 ? now + seconds * 20L : -1L;
        chanceOverride = chance >= 0.0F ? Mth.clamp(chance, 0.0F, 1.0F) : -1.0F;
        broadcast(server, "message.corpseorigin.blood_rain.start", SoundEvents.WEATHER_RAIN_ABOVE);
    }

    /** 关闭血雨；返回此前是否处于开启状态。 */
    public static boolean stop(MinecraftServer server, boolean notify) {
        boolean wasActive = active;
        active = false;
        manual = false;
        endsAt = -1L;
        chanceOverride = -1.0F;
        if (wasActive && notify) {
            broadcast(server, "message.corpseorigin.blood_rain.end", null);
        }
        return wasActive;
    }

    /** 临时调整本次事件的变异概率。 */
    public static boolean setChance(float chance) {
        if (!active) return false;
        chanceOverride = Mth.clamp(chance, 0.0F, 1.0F);
        return true;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isManual() {
        return active && manual;
    }

    /** 剩余秒数；-1 = 无限。 */
    public static long remainingSeconds(MinecraftServer server) {
        if (!active) return 0L;
        if (endsAt < 0) return -1L;
        ServerLevel level = server.overworld();
        if (level == null) return -1L;
        return Math.max(0L, (endsAt - level.getGameTime()) / 20L);
    }

    /** 当前实际使用的变异概率（0~1）。 */
    public static float currentChance() {
        if (chanceOverride >= 0.0F) return chanceOverride;
        return (float) CorpseOriginServerConfig.bloodRainMutateChance();
    }

    /** 让范围内露天生物按概率进入尸变潜伏期。 */
    private static void mutateExposedMobs(ServerLevel level) {
        int radius = CorpseOriginServerConfig.bloodRainRadius();
        boolean requireSky = CorpseOriginServerConfig.bloodRainRequireSky();
        float chance = currentChance();
        int maxMutations = CorpseOriginServerConfig.bloodRainMaxMutationsPerScan();
        int incubation = CorpseOriginServerConfig.bloodRainIncubationTicks();
        RandomSource random = level.getRandom();

        Set<Mob> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        int mutations = 0;

        for (ServerPlayer player : level.players()) {
            if (mutations >= maxMutations) break;
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius))) {
                if (mutations >= maxMutations) break;
                if (!seen.add(mob)) continue;
                if (!BYeffect.canInfect(mob)) continue;
                if (mob.hasEffect(ModEffects.QIANS.get()) || NativeCorpseState.incubating(mob)) continue;
                // 有方块遮挡 = 没被血雨淋到
                if (requireSky && !level.canSeeSky(mob.blockPosition().above())) continue;
                if (random.nextFloat() >= chance) continue;

                BYeffect.applyInfection(mob, level, incubation, null);
                mutations++;
            }
        }
    }

    /** 玩家周围的血雨粒子（服务端下发，无需客户端代码）。 */
    private static void spawnParticles(ServerLevel level) {
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < 6; i++) {
                double x = player.getX() + (random.nextDouble() - 0.5D) * 20.0D;
                double z = player.getZ() + (random.nextDouble() - 0.5D) * 20.0D;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
                level.sendParticles(ParticleTypes.FALLING_LAVA,
                        x, y + 7.0D + random.nextDouble() * 5.0D, z,
                        1, 0.12D, 0.0D, 0.12D, 0.02D);
            }
        }
    }

    private static void broadcast(MinecraftServer server, String translationKey, SoundEvent sound) {
        Component message = Component.translatable(translationKey);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
            if (sound != null && player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(), sound, SoundSource.WEATHER, 1.0F, 0.7F);
            }
        }
    }
}
