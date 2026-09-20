package xiaoshi2022.corpseorigin.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.ClientState;
import xiaoshi2022.corpseorigin.client.SkillHotbarState;
import xiaoshi2022.corpseorigin.skill.ISkill;

public final class SkillHotbarOverlay {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "skill_hotbar");
    private static final int WIDTH = 104;
    private static final int HEIGHT = 28;

    private SkillHotbarOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(HUD_ID,
                (GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientState.hudVisible || mc.player == null || mc.player.isSpectator()) return;
        if (xiaoshi2022.corpseorigin.client.ClientCharacterCache.getActivatableSkills().isEmpty()) return;

        int x = 8;
        int startY = Math.max(8, (graphics.guiHeight() - HEIGHT * 3 - 8) / 2);
        for (int slot = 0; slot < 3; slot++) {
            int y = startY + slot * (HEIGHT + 4);
            ISkill skill = SkillHotbarState.getSkill(slot);
            int remaining = skill == null ? 0
                    : ClientState.getCooldownRemaining(skill.getId().getPath());

            graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xCC101518);
            graphics.fill(x, y, x + 3, y + HEIGHT,
                    skill == null ? 0xFF555555 : remaining > 0 ? 0xFF9A4450 : 0xFF55B878);
            if (skill != null && remaining > 0) {
                int shadeWidth = Math.min(WIDTH - 3, 3 + remaining * (WIDTH - 3)
                        / Math.max(1, skill.getCooldownTicks()));
                graphics.fill(x + 3, y, x + shadeWidth, y + HEIGHT, 0x66000000);
            }
            graphics.fill(x + 6, y + 5, x + 24, y + 23, 0xFF252D31);
            graphics.centeredText(mc.font, Integer.toString(slot + 1), x + 15, y + 10,
                    0xFFFFFFFF);

            Component name = skill == null
                    ? Component.translatable("hud.corpseorigin.skill_slot.empty")
                    : skill.getName();
            String displayName = mc.font.plainSubstrByWidth(name.getString(), WIDTH - 33);
            graphics.text(mc.font, displayName, x + 29, y + 5,
                    skill == null ? 0xFF888888 : 0xFFFFFFFF, false);
            if (skill != null) {
                Component status = remaining > 0
                        ? Component.translatable("hud.corpseorigin.skill_slot.cooldown", (remaining + 19) / 20)
                        : Component.translatable("gui.corpseorigin.skill_wheel.ready");
                graphics.text(mc.font, status, x + 29, y + 16,
                        remaining > 0 ? 0xFFFF7777 : 0xFF66DD88, false);
            }
        }
    }
}
