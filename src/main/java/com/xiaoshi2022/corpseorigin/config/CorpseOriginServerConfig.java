package com.xiaoshi2022.corpseorigin.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 服务端配置（1.20.1 Forge）：尸水浸泡感染与尸兄血雨事件。
 * <p>
 * 文件位置：存档目录下的 {@code serverconfig/corpseorigin-server.toml}。
 * Forge 会监听配置文件变化，<b>运行中直接改文件保存即可生效</b>，无需重启；
 * 所有 getter 每次调用都读取当前值，因此血雨/浸泡逻辑会立即使用新数值。
 */
public final class CorpseOriginServerConfig {

    public static final ForgeConfigSpec SPEC;

    // ===== 尸水浸泡 =====
    private static final ForgeConfigSpec.BooleanValue FISH_IMMEDIATE;
    private static final ForgeConfigSpec.IntValue FISH_INCUBATION_TICKS;
    private static final ForgeConfigSpec.IntValue WATER_SUBMERGE_SECONDS;
    private static final ForgeConfigSpec.IntValue WATER_INCUBATION_TICKS;

    // ===== 尸兄血雨 =====
    private static final ForgeConfigSpec.BooleanValue BLOOD_RAIN_NATURAL_ENABLED;
    private static final ForgeConfigSpec.IntValue BLOOD_RAIN_NATURAL_CHECK_SECONDS;
    private static final ForgeConfigSpec.DoubleValue BLOOD_RAIN_NATURAL_CHANCE;
    private static final ForgeConfigSpec.IntValue BLOOD_RAIN_DEFAULT_DURATION_SECONDS;
    private static final ForgeConfigSpec.IntValue BLOOD_RAIN_SCAN_INTERVAL_TICKS;
    private static final ForgeConfigSpec.DoubleValue BLOOD_RAIN_MUTATE_CHANCE;
    private static final ForgeConfigSpec.IntValue BLOOD_RAIN_INCUBATION_TICKS;
    private static final ForgeConfigSpec.IntValue BLOOD_RAIN_RADIUS;
    private static final ForgeConfigSpec.BooleanValue BLOOD_RAIN_REQUIRE_SKY;
    private static final ForgeConfigSpec.IntValue BLOOD_RAIN_MAX_MUTATIONS_PER_SCAN;

    // ===== 尸兄进化 / 吞噬 =====
    private static final ForgeConfigSpec.BooleanValue EVOLUTION_ENABLED;
    private static final ForgeConfigSpec.IntValue EVOLUTION_ENERGY_PER_LEVEL;
    private static final ForgeConfigSpec.IntValue EVOLUTION_SAME_KIN_ENERGY;
    private static final ForgeConfigSpec.IntValue EVOLUTION_PLAYER_ENERGY;
    private static final ForgeConfigSpec.IntValue EVOLUTION_VILLAGER_ENERGY;
    private static final ForgeConfigSpec.IntValue EVOLUTION_ANIMAL_ENERGY;
    private static final ForgeConfigSpec.IntValue EVOLUTION_BREAKTHROUGH_LEVEL;
    private static final ForgeConfigSpec.DoubleValue EVOLUTION_BREAKTHROUGH_CHANCE;
    private static final ForgeConfigSpec.DoubleValue EVOLUTION_BREAKTHROUGH_BONUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("尸水浸泡感染").push("infectedWater");
        FISH_IMMEDIATE = builder
                .comment("鱼类（原版 AbstractFish：鳕鱼 / 鲑鱼 / 热带鱼 / 河豚）接触尸水是否立即感染。")
                .define("fishImmediateInfection", true);
        FISH_INCUBATION_TICKS = builder
                .comment("鱼类立即感染后的潜伏 tick（1 = 立刻尸变，20 tick = 1 秒）。")
                .defineInRange("fishIncubationTicks", 1, 1, 1200);
        WATER_SUBMERGE_SECONDS = builder
                .comment("非鱼类生物连续浸泡尸水多少秒后被感染。离开水面超过 0.5 秒会重新计时。")
                .defineInRange("submergeSeconds", 15, 1, 600);
        WATER_INCUBATION_TICKS = builder
                .comment("非鱼类生物浸泡感染后的潜伏 tick（20 tick = 1 秒）。")
                .defineInRange("incubationTicks", 200, 1, 12000);
        builder.pop();

        builder.comment("尸兄血雨事件").push("bloodRain");
        BLOOD_RAIN_NATURAL_ENABLED = builder
                .comment("是否允许自然触发：下雨时按概率自动降下血雨。")
                .define("naturalEnabled", true);
        BLOOD_RAIN_NATURAL_CHECK_SECONDS = builder
                .comment("自然触发判定间隔（秒）。")
                .defineInRange("naturalCheckSeconds", 300, 10, 86400);
        BLOOD_RAIN_NATURAL_CHANCE = builder
                .comment("每次判定的触发概率（0~1）。默认 0.15 = 下雨期间平均约 33 分钟一次。")
                .defineInRange("naturalChance", 0.15D, 0.0D, 1.0D);
        BLOOD_RAIN_DEFAULT_DURATION_SECONDS = builder
                .comment("自然触发的血雨持续时间（秒）。")
                .defineInRange("defaultDurationSeconds", 180, 5, 86400);
        BLOOD_RAIN_SCAN_INTERVAL_TICKS = builder
                .comment("血雨期间的变异判定间隔（tick）。20 tick = 1 秒。")
                .defineInRange("scanIntervalTicks", 20, 5, 200);
        BLOOD_RAIN_MUTATE_CHANCE = builder
                .comment("每次判定时，露天生物变异为尸兄的概率（0~1）。默认 0.35。")
                .defineInRange("mutateChance", 0.35D, 0.0D, 1.0D);
        BLOOD_RAIN_INCUBATION_TICKS = builder
                .comment("被血雨淋到后到尸变的潜伏 tick（20 tick = 1 秒）。")
                .defineInRange("incubationTicks", 100, 1, 12000);
        BLOOD_RAIN_RADIUS = builder
                .comment("以玩家为中心扫描生物的血雨范围（格）。")
                .defineInRange("radius", 32, 8, 128);
        BLOOD_RAIN_REQUIRE_SKY = builder
                .comment("是否只感染能被天空淋到的生物（有方块遮挡则安全）。")
                .define("requireSky", true);
        BLOOD_RAIN_MAX_MUTATIONS_PER_SCAN = builder
                .comment("单次判定最多让多少只生物开始变异，防止大规模尸潮瞬间卡服。")
                .defineInRange("maxMutationsPerScan", 96, 1, 1024);
        builder.pop();

        builder.comment("尸兄进化 / 互相吞噬").push("evolution");
        EVOLUTION_ENABLED = builder
                .comment("总开关。关闭后击杀不再获得血肉能量，已有等级与属性保留。")
                .define("enabled", true);
        EVOLUTION_ENERGY_PER_LEVEL = builder
                .comment("每一级进化所需的血肉能量 = 当前等级 × 此值。")
                .defineInRange("energyPerLevel", 10, 1, 1000);
        EVOLUTION_SAME_KIN_ENERGY = builder
                .comment("吞噬同类尸兄获得的能量（最高，鼓励互相吞噬）。")
                .defineInRange("sameKinEnergy", 4, 1, 100);
        EVOLUTION_PLAYER_ENERGY = builder
                .comment("击杀玩家获得的能量。")
                .defineInRange("playerEnergy", 5, 1, 100);
        EVOLUTION_VILLAGER_ENERGY = builder
                .comment("击杀村民获得的能量。")
                .defineInRange("villagerEnergy", 3, 1, 100);
        EVOLUTION_ANIMAL_ENERGY = builder
                .comment("击杀动物获得的能量。")
                .defineInRange("animalEnergy", 1, 1, 100);
        EVOLUTION_BREAKTHROUGH_LEVEL = builder
                .comment("正常进化的等级上限（1~9）。达到此等级后，再升级需要「超脱临界」概率突破。")
                .defineInRange("breakthroughLevel", 5, 1, 9);
        EVOLUTION_BREAKTHROUGH_CHANCE = builder
                .comment("临界突破基础成功率（0~1）。")
                .defineInRange("breakthroughChance", 0.15D, 0.0D, 1.0D);
        EVOLUTION_BREAKTHROUGH_BONUS = builder
                .comment("每次突破失败后叠加的成功率（0~1），成功后清零。")
                .defineInRange("breakthroughBonus", 0.10D, 0.0D, 0.5D);
        builder.pop();

        SPEC = builder.build();
    }

    private CorpseOriginServerConfig() {
    }

    /** 配置尚未加载时退回默认值，避免启动早期访问抛异常。 */
    private static <T> T value(ForgeConfigSpec.ConfigValue<T> configValue, T fallback) {
        try {
            return configValue.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public static boolean fishImmediateInfection() {
        return value(FISH_IMMEDIATE, true);
    }

    public static int fishIncubationTicks() {
        return value(FISH_INCUBATION_TICKS, 1);
    }

    public static int waterSubmergeSeconds() {
        return value(WATER_SUBMERGE_SECONDS, 15);
    }

    public static int waterIncubationTicks() {
        return value(WATER_INCUBATION_TICKS, 200);
    }

    public static boolean bloodRainNaturalEnabled() {
        return value(BLOOD_RAIN_NATURAL_ENABLED, true);
    }

    public static int bloodRainNaturalCheckSeconds() {
        return value(BLOOD_RAIN_NATURAL_CHECK_SECONDS, 300);
    }

    public static double bloodRainNaturalChance() {
        return value(BLOOD_RAIN_NATURAL_CHANCE, 0.15D);
    }

    public static int bloodRainDefaultDurationSeconds() {
        return value(BLOOD_RAIN_DEFAULT_DURATION_SECONDS, 180);
    }

    public static int bloodRainScanIntervalTicks() {
        return value(BLOOD_RAIN_SCAN_INTERVAL_TICKS, 20);
    }

    public static double bloodRainMutateChance() {
        return value(BLOOD_RAIN_MUTATE_CHANCE, 0.35D);
    }

    public static int bloodRainIncubationTicks() {
        return value(BLOOD_RAIN_INCUBATION_TICKS, 100);
    }

    public static int bloodRainRadius() {
        return value(BLOOD_RAIN_RADIUS, 32);
    }

    public static boolean bloodRainRequireSky() {
        return value(BLOOD_RAIN_REQUIRE_SKY, true);
    }

    public static int bloodRainMaxMutationsPerScan() {
        return value(BLOOD_RAIN_MAX_MUTATIONS_PER_SCAN, 96);
    }

    public static boolean evolutionEnabled() {
        return value(EVOLUTION_ENABLED, true);
    }

    public static int evolutionEnergyPerLevel() {
        return value(EVOLUTION_ENERGY_PER_LEVEL, 10);
    }

    public static int evolutionSameKinEnergy() {
        return value(EVOLUTION_SAME_KIN_ENERGY, 4);
    }

    public static int evolutionPlayerEnergy() {
        return value(EVOLUTION_PLAYER_ENERGY, 5);
    }

    public static int evolutionVillagerEnergy() {
        return value(EVOLUTION_VILLAGER_ENERGY, 3);
    }

    public static int evolutionAnimalEnergy() {
        return value(EVOLUTION_ANIMAL_ENERGY, 1);
    }

    public static int evolutionBreakthroughLevel() {
        return value(EVOLUTION_BREAKTHROUGH_LEVEL, 5);
    }

    public static double evolutionBreakthroughChance() {
        return value(EVOLUTION_BREAKTHROUGH_CHANCE, 0.15D);
    }

    public static double evolutionBreakthroughBonus() {
        return value(EVOLUTION_BREAKTHROUGH_BONUS, 0.10D);
    }
}
