package xiaoshi2022.corpseorigin.skill;

/**
 * 进化管理器 - 进化点数与等级换算
 * <p>
 * 完整等级体系参见 {@link EvolutionTier}：
 * <pre>
 * 人 (1-4) → 地 (5-8) → 天 (9) → 神 (10-12) → 超神 (13 SSS / 14 初期) → 神上 (15+ EX)
 * </pre>
 */
public final class EvolutionManager {

    /** 体系最大支持等级（超过视为神上 EX） */
    public static final int MAX_LEVEL = 20;

    private EvolutionManager() {
    }

    private static double difficulty = 1;
    private static boolean preserveLegacyProgress = true;
    public static void preserveLegacyProgress(boolean enabled) { preserveLegacyProgress = enabled; }
    public static boolean preservesLegacyProgress() { return preserveLegacyProgress; }
    public static void configure(double multiplier) {
        difficulty = Double.isFinite(multiplier) ? Math.clamp(multiplier, 1, 1000) : 3;
    }

    /**
     * 难度倍率 → 进化点数阈值的缩放系数。
     * <p>
     * ≤10 倍：线性（历史上限体验不变）；&gt;10 倍：指数爬升 ——
     * 40 倍（GUI 滑条上限）时系数 ≈435，封顶"神上"（基础 2300 点）≈ <b>100 万点</b>。
     * 上限钳制 50 万倍防 int 溢出（难度 1000 时也安全）。
     */
    private static double difficultyFactor() {
        if (difficulty <= 10) return difficulty;
        return Math.min(10 * Math.pow(434.78 / 10, (difficulty - 10) / 30.0), 500_000);
    }
    /** Preserve a legacy character's fractional tier progress without awarding spendable currency. */
    public static int migrateLegacyPoints(int points) {
        return (int)Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, points) * difficultyFactor()));
    }

    /**
     * 每级所需的累计进化点数阈值。
     * <p>
     * 索引 = 目标等级，值 = 累计所需点数。
     * level 0 为占位。
     */
    private static final int[] THRESHOLDS = {
            0,     // 0  占位
            0,     // 1  人1
            5,     // 2  人2
            12,    // 3  人3
            25,    // 4  人4
            45,    // 5  地1  (原 level 5)
            70,    // 6  地2
            100,   // 7  地3
            140,   // 8  地4
            200,   // 9  天
            280,   // 10 神·前期
            380,   // 11 神·中期
            500,   // 12 神·后期
            650,   // 13 超神 SSS
            780,   // 14 超神初期
            950,   // 15 神上 EX
            1150,  // 16 神上 EX+
            1380,  // 17
            1650,  // 18
            1960,  // 19
            2300   // 20 封顶
    };

    /**
     * 根据累计进化点数返回进化等级 (1..MAX_LEVEL)。
     */
    public static int getLevel(int earnedPoints) {
        if (earnedPoints <= 0) {
            return 1;
        }
        for (int level = MAX_LEVEL; level >= 1; level--) {
            if (earnedPoints >= getThreshold(level)) {
                return level;
            }
        }
        return 1;
    }

    /** 升到指定等级所需的累计进化点数 */
    public static int getThreshold(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            return Integer.MAX_VALUE;
        }
        return (int)Math.ceil(THRESHOLDS[level] * difficultyFactor());
    }

    /**
     * 返回升到下一级还需要多少点数。
     * 已经是 MAX_LEVEL 时返回 0。
     */
    public static int pointsToNextLevel(int earnedPoints) {
        int currentLevel = getLevel(earnedPoints);
        if (currentLevel >= MAX_LEVEL) return 0;
        return getThreshold(currentLevel + 1) - earnedPoints;
    }
}
