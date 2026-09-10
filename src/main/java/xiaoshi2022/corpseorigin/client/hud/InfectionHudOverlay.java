package xiaoshi2022.corpseorigin.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.ClientState;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;

/**
 * HUD 显示：感染度条 + 进化等级/点数（右上角）
 */
public final class InfectionHudOverlay {

    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "infection_hud");

    private InfectionHudOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(
                HUD_ID,
                (GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) -> render(graphics));
        CorpseOrigin.LOGGER.debug("[HUD] 注册成功（addLast）");
    }

    private static void render(GuiGraphicsExtractor graphics) {
        if (!ClientState.hudVisible) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) return;

        int barWidth = 182;
        int x = graphics.guiWidth() - barWidth - 10;
        int y = 10;

        // ✅ 判断是不是尸兄玩家
        boolean isCorpse = false;
        var selfData = CorpseOriginClient.corpseDataCache.get(mc.player.getUUID());
        if (selfData != null && selfData.isCorpse) {
            isCorpse = true;
        }

        // ===== 感染度条 =====
        // 尸兄玩家直接 100%
        int infection;
        if (isCorpse) {
            infection = 100;
        } else {
            infection = ClientState.infection;
            boolean hasInfectionBuff = mc.player.hasEffect(ModEffects.QIANS);
            if (infection <= 0 && hasInfectionBuff) {
                infection = 1;
            }
        }

        int infectionWidth = (int) (barWidth * infection / 100.0);
        graphics.fill(x, y, x + barWidth, y + 5, 0x88000000);
        int color = infection >= 60 ? 0xFFCC2244 : 0xFF8844AA;
        graphics.fill(x, y, x + infectionWidth, y + 5, color);
        graphics.text(mc.font,
                Component.translatable("hud.corpseorigin.infection", infection),
                x, y + 6, 0xFFBB88CC, true);
        y += 18;

        // ===== 进化信息 =====
        int level = EvolutionManager.getLevel(ClientState.earnedPoints);
        graphics.text(mc.font,
                Component.translatable("hud.corpseorigin.evolution",
                        level, ClientState.availablePoints),
                x, y + 8, 0xFF55FF55, true);
    }
}