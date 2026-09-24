package xiaoshi2022.corpseorigin.client.skin;

/**
 * 尸兄皮肤加载状态枚举
 */
public enum ZbSkinState {
    NOT_LOADED(0, "gui.corpseorigin.skin_state.0"),
    LOADING(1, "gui.corpseorigin.skin_state.1"),
    LOADED(2, "gui.corpseorigin.skin_state.2"),
    FAILED(3, "gui.corpseorigin.skin_state.3");

    private final int code;
    private final String description;

    ZbSkinState(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return net.minecraft.client.resources.language.I18n.get(description);
    }

    public static ZbSkinState fromCode(int code) {
        for (ZbSkinState state : values()) {
            if (state.code == code) return state;
        }
        return NOT_LOADED;
    }
}