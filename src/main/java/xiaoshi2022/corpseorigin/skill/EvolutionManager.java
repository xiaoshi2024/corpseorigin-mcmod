package xiaoshi2022.corpseorigin.skill;

/**
 * 进化管理器 - 进化点数与等级换算
 * <p>
 * 与 {@link xiaoshi2022.corpseorigin.component.PlayerCorpseComponent#setEvolutionLevel(int)}
 * 的 1-5 clamp 范围保持一致。
 */
public final class EvolutionManager {

    private EvolutionManager() {
    }

    /** 每级所需进化点数 */
    private static final int[] THRESHOLDS = {0, 0, 5, 12, 25, 45};

    /**
     * 根据累计进化点数返回进化等级（1-5）。
     */
    public static int getLevel(int earnedPoints) {
        if (earnedPoints <= 0) {
            return 1;
        }
        for (int level = 5; level >= 1; level--) {
            if (earnedPoints >= THRESHOLDS[level]) {
                return level;
            }
        }
        return 1;
    }

    /** 升到指定等级所需的累计进化点数 */
    public static int getThreshold(int level) {
        if (level < 1 || level > 5) {
            return Integer.MAX_VALUE;
        }
        return THRESHOLDS[level];
    }
}
