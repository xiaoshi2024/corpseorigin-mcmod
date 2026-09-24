package xiaoshi2022.corpseorigin.skill;

/**
 * 尸兄体系进化等级对照表
 * <p>
 * 完整体系：
 * <pre>
 * 人 (1,2,3,4)  → 地 (5,6,7,8)  → 天 (9)  → 神 (10,11,12)  → 超神 (13=SSS / 14=初期)  → 神上 (15+=EX)
 * </pre>
 * <p>
 * 枚举顺序即自然等级顺序，便于比较。
 */
public enum EvolutionTier {

    /** 人 1-4 级 */
    REN("人", 0xFFCCCCCC, "人1", "人2", "人3", "人4"),

    /** 地 5-8 级 */
    DI("地", 0xFF4CAF50, "地1", "地2", "地3", "地4"),

    /** 天 9 级 */
    TIAN("天", 0xFF2196F3, "天"),

    /** 神 10-12 级（前期/中期/后期） */
    SHEN("神", 0xFF9C27B0, "神·前期", "神·中期", "神·后期"),

    /** 超神 13 级（SSS） */
    CHAO_SHEN_SSS("超神", 0xFFFFD700, "超神SSS"),

    /** 超神 14 级（后期） */
    CHAO_SHEN_LATE("超神", 0xFFFFECB3, "超神后期"),

    /** 神上 15+ 级（EX） */
    SHEN_SHANG("神上", 0xFFE53935, "神上EX");

    /** 阶层名称前缀（中文） */
    private final String displayName;
    /** 显示颜色（ARGB） */
    private final int color;
    /** 每个子等级的显示名 */
    private final String[] subNames;

    EvolutionTier(String displayName, int color, String... subNames) {
        this.displayName = displayName;
        this.color = color;
        this.subNames = subNames;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    public String[] getSubNames() {
        return subNames;
    }

    /**
     * 根据 level 查找对应子等级的完整显示名（如 "人2"、"神·中期"、"超神SSS"）。
     */
    public String getFullName(int levelInTier) {
        if (subNames.length == 0) return displayName;
        int idx = Math.max(0, Math.min(subNames.length - 1, levelInTier - 1));
        return subNames[idx];
    }

    /**
     * 根据绝对进化等级得到所属阶层。
     *
     * @param absoluteLevel 绝对进化等级 (1..N+)
     * @return 所属的 EvolutionTier
     */
    public static EvolutionTier fromAbsoluteLevel(int absoluteLevel) {
        if (absoluteLevel <= 4) return REN;
        if (absoluteLevel <= 8) return DI;
        if (absoluteLevel == 9) return TIAN;
        if (absoluteLevel <= 12) return SHEN;
        if (absoluteLevel == 13) return CHAO_SHEN_SSS;
        if (absoluteLevel == 14) return CHAO_SHEN_LATE;
        return SHEN_SHANG;
    }

    /**
     * 返回该阶层在当前绝对等级下的"第几子级"（从 1 开始），超出 subNames 长度时钳制到末尾。
     */
    public static int levelInTier(int absoluteLevel) {
        if (absoluteLevel <= 4) return absoluteLevel;                          // 1..4
        if (absoluteLevel <= 8) return absoluteLevel - 4;                      // 1..4
        if (absoluteLevel == 9) return 1;                                     // 1
        if (absoluteLevel <= 12) return absoluteLevel - 9;                     // 1..3
        if (absoluteLevel == 13) return 1;                                     // 1
        if (absoluteLevel == 14) return 1;                                     // 1
        return 1;                                                             // EX 统一
    }

    /**
     * 把绝对等级格式化成完整显示字符串，如 "人2" / "神·中期" / "超神SSS" / "神上EX"。
     */
    public static String formatFullName(int absoluteLevel) {
        EvolutionTier tier = fromAbsoluteLevel(absoluteLevel);
        return tier.getFullName(levelInTier(absoluteLevel));
    }

    /**
     * 仅返回阶层前缀，如 "人" / "地" / "天" / "神" / "超神" / "神上"。
     */
    public static String formatShortName(int absoluteLevel) {
        return fromAbsoluteLevel(absoluteLevel).getDisplayName();
    }

    /**
     * 获取该绝对等级应该使用的显示颜色。
     */
    public static int colorOf(int absoluteLevel) {
        return fromAbsoluteLevel(absoluteLevel).getColor();
    }
}
