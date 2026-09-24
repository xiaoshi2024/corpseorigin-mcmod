package xiaoshi2022.corpseorigin.character;

/**
 * 角色阵营 —— 用于「角色选择书」界面按阵营分页展示。
 * <p>
 * 所有角色的阵营归属集中在 {@link CharacterManager#registerDefaults()} 里的
 * {@code FACTION_MAP.put(角色ID, CharacterFaction.xxx)} 一处维护，后期调整只改那里就行，
 * 不用改角色类本身。
 * <p>
 * 阵营名通过 {@code faction.corpseorigin.xxx} 语言键本地化。
 */
public enum CharacterFaction {

    /** 人类阵营（炎黄特能队、幸存人类、拥有人性的角色） */
    HUMAN("faction.corpseorigin.human", 0xFF4CAF50),

    /** 尸王阵营（龙右及其手下尸兄） */
    CORPSE_KING("faction.corpseorigin.corpse_king", 0xFFE53935),

    /** 东瀛势力（风魔灰太郎等） */
    TOYO("faction.corpseorigin.toyo", 0xFF2196F3),

    /** 米国欧盟（黑暗议会等西方势力） */
    WESTERN("faction.corpseorigin.western", 0xFFFF9800),

    /** 其他（血莲教唯欣、未归类角色、可后期补充） */
    OTHER("faction.corpseorigin.other", 0xFF9E9E9E);

    /** 语言键 */
    private final String translationKey;
    /** 界面显示颜色（ARGB） */
    private final int color;

    CharacterFaction(String translationKey, int color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public int getColor() {
        return color;
    }
}
