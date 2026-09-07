//package xiaoshi2022.corpseorigin.client;
//
//import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
//import net.minecraft.client.DeltaTracker;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiGraphicsExtractor;
//import net.minecraft.network.chat.Component;
//import net.minecraft.resources.Identifier;
//import xiaoshi2022.corpseorigin.CorpseOrigin;
//
///**
// * HUD 显示：感染度条 + 进化等级/点数
// */
//public final class InfectionHudOverlay {
//
//    private InfectionHudOverlay() {
//    }
//
//    public static void register() {
//        HudElementRegistry.attachElementAfter(VanillaHudElements.EXPERIENCE_LEVEL,
//                Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "infection_hud"),
//                (GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) -> render(graphics));
//    }
//
//    private static void render(GuiGraphicsExtractor graphics) {
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player == null || mc.player.isSpectator()) {
//            return;
//        }
//        if ("mortal".equals(ClientState.characterId) && ClientState.infection <= 0 && ClientState.earnedPoints <= 0) {
//            return; // 凡人且无进化数据时不显示
//        }
//
//        int width = graphics.guiWidth();
//        int height = graphics.guiHeight();
//        int x = width / 2 - 91;
//        int y = height - 39;
//
//        // ===== 感染度条（红紫色，在经验条下方） =====
//        if (ClientState.infection > 0) {
//            int barWidth = 91 * 2;
//            int infectionWidth = (int) (barWidth * ClientState.infection / 100.0);
//            graphics.fill(x, y, x + barWidth, y + 5, 0x88000000);
//            int color = ClientState.infection >= 60 ? 0xFFCC2244 : 0xFF8844AA;
//            graphics.fill(x, y, x + infectionWidth, y + 5, color);
//            graphics.text(mc.font,
//                    Component.translatable("hud.corpseorigin.infection", ClientState.infection),
//                    x, y + 6, 0xFFBB88CC, true);
//            y += 18;
//        }
//
//        // ===== 进化信息（左下角显示在状态栏上方） =====
//        if (!"mortal".equals(ClientState.characterId)) {
//            int level = xiaoshi2022.corpseorigin.skill.EvolutionManager.getLevel(ClientState.earnedPoints);
//            graphics.text(mc.font,
//                    Component.translatable("hud.corpseorigin.evolution", level, ClientState.availablePoints),
//                    x, y + 8, 0xFF55FF55, true);
//        }
//    }
//}
