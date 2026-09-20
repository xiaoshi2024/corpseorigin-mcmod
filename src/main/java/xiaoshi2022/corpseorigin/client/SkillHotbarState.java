package xiaoshi2022.corpseorigin.client;

import net.fabricmc.loader.api.FabricLoader;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.ISkill;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Client-side, per-character bindings for the three skill HUD slots. */
public final class SkillHotbarState {
    private static final int SLOT_COUNT = 3;
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("corpseorigin-skill-slots.properties");
    private static final Properties BINDINGS = new Properties();

    static {
        if (Files.isRegularFile(FILE)) {
            try (InputStream input = Files.newInputStream(FILE)) {
                BINDINGS.load(input);
            } catch (IOException exception) {
                CorpseOrigin.LOGGER.warn("Failed to load skill slot bindings", exception);
            }
        }
    }

    private SkillHotbarState() {
    }

    public static void bind(int slot, ISkill skill) {
        if (slot < 0 || slot >= SLOT_COUNT || skill == null) return;
        BINDINGS.setProperty(key(slot), skill.getId().getPath());
        save();
    }

    public static ISkill getSkill(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return null;
        String path = BINDINGS.getProperty(key(slot), "");
        for (ISkill skill : ClientCharacterCache.getActivatableSkills()) {
            if (skill.getId().getPath().equals(path)) return skill;
        }
        return null;
    }

    private static String key(int slot) {
        return ClientState.characterId + "." + slot;
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream output = Files.newOutputStream(FILE)) {
                BINDINGS.store(output, "Corpse Origin quick skill bindings");
            }
        } catch (IOException exception) {
            CorpseOrigin.LOGGER.warn("Failed to save skill slot bindings", exception);
        }
    }
}
