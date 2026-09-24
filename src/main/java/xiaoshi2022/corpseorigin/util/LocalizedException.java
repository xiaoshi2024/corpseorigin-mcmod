package xiaoshi2022.corpseorigin.util;

import net.minecraft.network.chat.Component;

/** Keep validation feedback translatable until it reaches the player's client. */
public final class LocalizedException extends IllegalArgumentException {
    private final Component message;
    public LocalizedException(String key, Object... args) {
        super(key);
        this.message = Component.translatable(key, args);
    }
    public Component component() { return message; }
    public static Component describe(Exception error) {
        return error instanceof LocalizedException localized ? localized.component()
                : Component.translatable("message.corpseorigin.organ.invalid_data");
    }
}
