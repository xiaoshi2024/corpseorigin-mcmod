package xiaoshi2022.corpseorigin.skill;

/**
 * 技能类型 - 用于技能树分类与颜色标记
 */
public enum SkillType {

    COMBAT(0xFFAA2222, "combat"),
    UTILITY(0xFF22AA22, "utility"),
    DEFENSE(0xFF2222AA, "defense"),
    ULTIMATE(0xFFAA22AA, "ultimate");

    private final int color;
    private final String name;

    SkillType(int color, String name) {
        this.color = color;
        this.name = name;
    }

    /** 渲染颜色（不含 alpha 通道，调用方自行 | 0xFF000000） */
    public int getColor() {
        return color;
    }

    /** 类型名（用于 i18n key: skilltype.corpseorigin.<name>） */
    public String getName() {
        return name;
    }
}
