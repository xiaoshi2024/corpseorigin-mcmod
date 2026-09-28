package xiaoshi2022.corpseorigin.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.ClientCharacterCache;
import xiaoshi2022.corpseorigin.client.ClientState;
import xiaoshi2022.corpseorigin.client.SkillHotbarState;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.skill.ISkill;

public final class SkillHotbarOverlay {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "skill_hotbar");
    private static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/gui/skill_slot.png");
    private static final Identifier SLOT_STATUS = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/gui/skill_slot_status.png");
    private static final Identifier SLOT_COOLDOWN = Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "textures/gui/skill_slot_cooldown.png");

    private static final int WIDTH = 104;
    private static final int HEIGHT = 28;
    private static final int SLOTS = 3;

    private SkillHotbarOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(HUD_ID,
                (GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientState.hudVisible || mc.player == null || mc.player.isSpectator()) return;
        if (ClientCharacterCache.getActivatableSkills().isEmpty()) return;

        CorpseConfig.SkillHud cfg = CorpseConfig.get().skillHud;
        float s = cfg.scale;

        int baseX = cfg.x >= 0 ? cfg.x : 8;
        int baseY = cfg.y >= 0
                ? cfg.y
                : Math.max(8, (graphics.guiHeight() - (int) (HEIGHT * SLOTS * s) - 8) / 2);

        // 缩放在本地坐标系里做：所有内部坐标都按未缩放的本地值算，再乘 scale + baseX/baseY
        // （26.x 的 Matrix3x2fStack 不支持 pushPose/translate/scale，所以手动乘）
        for (int slot = 0; slot < SLOTS; slot++) {
            int y = (int) (slot * (HEIGHT + cfg.spacing) * s);
            int xSlot = (int) (baseX);
            int ySlot = (int) (baseY + y);
            int wSlot = (int) (WIDTH * s);
            int hSlot = (int) (HEIGHT * s);

            ISkill skill = SkillHotbarState.getSkill(slot);
            int remaining = skill == null ? 0
                    : ClientState.getCooldownRemaining(skill.getId().getPath());

            graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, xSlot, ySlot, 0, 0,
                    wSlot, hSlot, WIDTH, HEIGHT);
            int statusPart = skill == null ? 0 : remaining > 0 ? 1 : 2;
            int statusW = (int) (3 * s);
            graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_STATUS, xSlot, ySlot, 0, statusPart * HEIGHT,
                    statusW, hSlot, 3, HEIGHT * 3);
            if (skill != null && remaining > 0) {
                int shadeWidthUnscaled = Math.min(WIDTH - 3, 3 + remaining * (WIDTH - 3)
                        / Math.max(1, ClientState.cooldownDurations.getOrDefault(skill.getId().getPath(), skill.getCooldownTicks())));
                int shadeW = (int) ((shadeWidthUnscaled - 3) * s);
                int shadeX = (int) (xSlot + 3 * s);
                graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_COOLDOWN, shadeX, ySlot, 0, 0,
                        shadeW, hSlot, WIDTH - 3, HEIGHT);
            }
            graphics.centeredText(mc.font, Integer.toString(slot + 1),
                    xSlot + (int) (15 * s), ySlot + (int) (10 * s),
                    0xFFFFFFFF);

            Component name = skill == null
                    ? Component.translatable("hud.corpseorigin.skill_slot.empty")
                    : skill.getName();
            String displayName = mc.font.plainSubstrByWidth(name.getString(), (int) ((WIDTH - 33) * s));
            graphics.text(mc.font, displayName,
                    xSlot + (int) (29 * s), ySlot + (int) (5 * s),
                    skill == null ? 0xFF888888 : 0xFFFFFFFF, false);
            if (skill != null) {
                Component status = remaining > 0
                        ? Component.translatable("hud.corpseorigin.skill_slot.cooldown", (remaining + 19) / 20)
                        : Component.translatable("gui.corpseorigin.skill_wheel.ready");
                graphics.text(mc.font, status,
                        xSlot + (int) (29 * s), ySlot + (int) (16 * s),
                        remaining > 0 ? 0xFFFF7777 : 0xFF66DD88, false);
            }
        }
    }
}