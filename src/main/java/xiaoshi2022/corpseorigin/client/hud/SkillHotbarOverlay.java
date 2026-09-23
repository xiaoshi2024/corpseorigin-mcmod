package xiaoshi2022.corpseorigin.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.ClientState;
import xiaoshi2022.corpseorigin.client.SkillHotbarState;
import xiaoshi2022.corpseorigin.skill.ISkill;

public final class SkillHotbarOverlay {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "skill_hotbar");
    private static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/gui/skill_slot.png");
    private static final Identifier SLOT_STATUS = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/gui/skill_slot_status.png");
    private static final Identifier SLOT_COOLDOWN = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/gui/skill_slot_cooldown.png");
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

            graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, x, y, 0, 0,
                    WIDTH, HEIGHT, WIDTH, HEIGHT);
            int statusPart = skill == null ? 0 : remaining > 0 ? 1 : 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_STATUS, x, y, 0, statusPart * HEIGHT,
                    3, HEIGHT, 3, HEIGHT * 3);
            if (skill != null && remaining > 0) {
                int shadeWidth = Math.min(WIDTH - 3, 3 + remaining * (WIDTH - 3)
                        / Math.max(1, skill.getCooldownTicks()));
                graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_COOLDOWN, x + 3, y, 0, 0,
                        shadeWidth - 3, HEIGHT, WIDTH - 3, HEIGHT);
            }
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
