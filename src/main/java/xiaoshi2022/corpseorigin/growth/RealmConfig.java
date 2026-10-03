package xiaoshi2022.corpseorigin.growth;

/** Server-authoritative settings in config/corpseorigin.json -> realm. */
public final class RealmConfig {
    public boolean enabled = true;
    public boolean swordTerrainDestruction = true;
    public int swordRiftBlocksPerTick = 1024, swordRiftScansPerTick = 8192, swordRiftMillisPerTick = 4;
    public int swordRiftMaxBlocks = 4000000, swordRiftMaxLength = 1024, swordRiftConcurrent = 2, swordRiftCooldownTicks = 100;
    public double difficultyMultiplier = 3;
    public boolean preserveExistingProgress = true;
    public double statMultiplier = 1;
    public double qiSkillDamageScaling = 1;
    public double cooldownReductionPerLevel = .04, maxCooldownReduction = .75;
    public int minimumSkillCooldownTicks = 5, poetryCooldownTicks = 1200;
    public double ultimateQiFraction = .05, poetryActivationFraction = .08;
    public double poetryStageFraction = .02, poetryUpkeepFraction = .01, poetrySwordFraction = .03;
    /** 强化消耗：单次 = base + step×已强化次数（买 n 次为等差数列求和）。
     *  默认 4/1：面板 ×100 按钮 0 强化时共 5350 点（旧 10/2 为 10900，约半价） */
    public int trainingCostBase = 4, trainingCostStep = 1, maxTrainingRank = 100000;
    public int rechargePointCost = 50;
    public int practiceXpPerRank = 100, combatXpPerSecond = 4, fleshPracticeXp = 8;
    public int meditationTicks = 200, meditationXp = 4;
    public int burstQiCost = 20000, burstCooldownTicks = 1200;
    public double burstRadius = 64;
    public boolean burstBreaksTerrain = false;
    public int terrainBlocksPerTick = 128, terrainTotalBlocks = 4096;
    /** 身法速度加值上限（MOVEMENT_SPEED 0.1 基础上的加值；0.35 = 最高 4.5 倍地面速度） */
    public double speedBonusCap = .35;
    /** 飞行速度灵敏度：flySpeed = 0.05 * (1 + 速度加值/该值)；越小飞行对身法越敏感 */
    public double flightSensitivity = 5;

    public void sanitize() {
        swordRiftBlocksPerTick=Math.clamp(swordRiftBlocksPerTick,16,8192);
        swordRiftScansPerTick=Math.clamp(swordRiftScansPerTick,64,65536);
        swordRiftMillisPerTick=Math.clamp(swordRiftMillisPerTick,1,15);
        swordRiftMaxBlocks=Math.clamp(swordRiftMaxBlocks,128,8000000);
        swordRiftMaxLength=Math.clamp(swordRiftMaxLength,8,2048);
        swordRiftConcurrent=Math.clamp(swordRiftConcurrent,1,4);
        swordRiftCooldownTicks=Math.clamp(swordRiftCooldownTicks,20,72000);
        difficultyMultiplier = finite(difficultyMultiplier, 1, 1000, 3);
        cooldownReductionPerLevel = finite(cooldownReductionPerLevel, 0, .2, .04);
        maxCooldownReduction = finite(maxCooldownReduction, 0, .95, .75);
        minimumSkillCooldownTicks = Math.clamp(minimumSkillCooldownTicks, 1, 1200);
        poetryCooldownTicks = Math.clamp(poetryCooldownTicks, 20, 72000);
        ultimateQiFraction = finite(ultimateQiFraction, 0, 1, .05);
        poetryActivationFraction = finite(poetryActivationFraction, 0, 1, .08);
        poetryStageFraction = finite(poetryStageFraction, 0, .2, .02);
        poetryUpkeepFraction = finite(poetryUpkeepFraction, 0, 1, .01);
        poetrySwordFraction = finite(poetrySwordFraction, 0, 1, .03);
        statMultiplier = finite(statMultiplier, .01, 10, 1);
        qiSkillDamageScaling = finite(qiSkillDamageScaling, 0, 10, 1);
        trainingCostBase = Math.clamp(trainingCostBase, 1, 1000000);
        trainingCostStep = Math.clamp(trainingCostStep, 1, 1000000);
        maxTrainingRank = Math.clamp(maxTrainingRank, 1, 1000000);
        rechargePointCost = Math.clamp(rechargePointCost,1,1000000);
        practiceXpPerRank = Math.clamp(practiceXpPerRank, 1, 1000000);
        combatXpPerSecond = Math.clamp(combatXpPerSecond, 0, 100);
        fleshPracticeXp = Math.clamp(fleshPracticeXp, 0, 1000);
        meditationTicks = Math.clamp(meditationTicks, 40, 72000);
        meditationXp = Math.clamp(meditationXp, 1, 1000);
        burstQiCost = Math.clamp(burstQiCost, 1, 10000000);
        burstCooldownTicks = Math.clamp(burstCooldownTicks, 20, 72000);
        burstRadius = finite(burstRadius, 8, 128, 64);
        terrainBlocksPerTick = Math.clamp(terrainBlocksPerTick, 1, 512);
        terrainTotalBlocks = Math.clamp(terrainTotalBlocks, 0, 32768);
        speedBonusCap = finite(speedBonusCap, .05, 2, .35);
        flightSensitivity = finite(flightSensitivity, 1, 50, 5);
    }
    private static double finite(double v, double min, double max, double fallback) {
        return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback;
    }
}
