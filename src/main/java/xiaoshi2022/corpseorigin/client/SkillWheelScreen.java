//package xiaoshi2022.corpseorigin.client;
//
//import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiGraphicsExtractor;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.client.input.KeyEvent;
//import net.minecraft.client.input.MouseButtonEvent;
//import net.minecraft.network.chat.Component;
//import xiaoshi2022.corpseorigin.network.CorpsePayloads;
//
//import java.util.List;
//
///**
// * 技能轮盘 GUI - 已学习的主动技能环形排布，点击释放
// */
//public class SkillWheelScreen extends Screen {
//
//    private record WheelSlot(ISkill skill, double x, double y) {
//    }
//
//    private final List<WheelSlot> slots = new java.util.ArrayList<>();
//    private int centerX;
//    private int centerY;
//    private double radius;
//
//    public SkillWheelScreen() {
//        super(Component.translatable("gui.corpseorigin.skill_wheel"));
//    }
//
//    @Override
//    protected void init() {
//        centerX = this.width / 2;
//        centerY = this.height / 2;
//        radius = Math.min(width, height) * 0.28;
//
//        slots.clear();
//        List<ISkill> skills = ClientCharacterCache.getActivatableSkills();
//        int n = skills.size();
//        if (n == 0) {
//            return;
//        }
//        // 从正上方开始，顺时针均分
//        for (int i = 0; i < n; i++) {
//            double angle = -Math.PI / 2 + (Math.PI * 2 * i / n);
//            slots.add(new WheelSlot(skills.get(i),
//                    centerX + radius * Math.cos(angle),
//                    centerY + radius * Math.sin(angle)));
//        }
//    }
//
//    @Override
//    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
//        // 半透明背景
//        graphics.fill(0, 0, this.width, this.height, 0x66000000);
//
//        // 中心提示
//        graphics.centeredText(font, title, centerX, centerY - font.lineHeight / 2, 0xFFFFFFAA);
//        graphics.centeredText(font,
//                Component.translatable("gui.corpseorigin.skill_wheel.points", ClientState.availablePoints),
//                centerX, centerY + 10, 0xFF55FF55);
//
//        for (WheelSlot slot : slots) {
//            ISkill skill = slot.skill();
//            boolean onCooldown = ClientState.getCooldownRemaining(skill.getId().getPath()) > 0;
//            boolean hovered = isHovered(slot, mouseX, mouseY);
//
//            int slotRadius = 22;
//            int baseColor = onCooldown ? 0xFF333344 : 0xEE224433;
//            if (hovered) {
//                baseColor = 0xEE44AA44;
//            }
//            drawCircle(graphics, slot.x(), slot.y(), slotRadius, baseColor);
//            drawCircleOutline(graphics, slot.x(), slot.y(), slotRadius, 0xFF88FF88);
//
//            // 名称（截断）
//            String name = skill.getName().getString();
//            if (name.length() > 5) {
//                name = name.substring(0, 5);
//            }
//            graphics.centeredText(font, name,
//                    (int) slot.x(), (int) slot.y() - 4,
//                    onCooldown ? 0xFF888888 : 0xFFFFFFFF);
//
//            int remaining = ClientState.getCooldownRemaining(skill.getId().getPath());
//            if (remaining > 0) {
//                graphics.centeredText(font, (remaining / 20 + 1) + "s",
//                        (int) slot.x(), (int) slot.y() + 6, 0xFFFF5555);
//            }
//        }
//
//        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
//    }
//
//    private boolean isHovered(WheelSlot slot, int mouseX, int mouseY) {
//        double dx = mouseX - slot.x();
//        double dy = mouseY - slot.y();
//        return dx * dx + dy * dy <= 22 * 22;
//    }
//
//    @Override
//    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
//        if (event.button() == 0) {
//            int mouseX = (int) event.x();
//            int mouseY = (int) event.y();
//            for (WheelSlot slot : slots) {
//                if (isHovered(slot, mouseX, mouseY)) {
//                    ClientPlayNetworking.send(new CorpsePayloads.ActivateSkillC2S(
//                            slot.skill().getId().getPath()));
//                    Minecraft.getInstance().setScreenAndShow(null);
//                    return true;
//                }
//            }
//        }
//        return super.mouseClicked(event, doubled);
//    }
//
//    @Override
//    public boolean keyPressed(KeyEvent event) {
//        // 按 ESC 或再次按轮盘键关闭
//        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
//                || CorpseKeyBindings.openSkillWheel.matches(event)) {
//            onClose();
//            return true;
//        }
//        return super.keyPressed(event);
//    }
//
//    @Override
//    public boolean isPauseScreen() {
//        return false;
//    }
//
//    // ==================== 简易圆形绘制 ====================
//
//    private void drawCircle(GuiGraphicsExtractor graphics, double cx, double cy, int r, int color) {
//        int r2 = r * r;
//        for (int dy = -r; dy <= r; dy++) {
//            int halfWidth = (int) Math.sqrt(r2 - dy * dy);
//            graphics.fill((int) cx - halfWidth, (int) cy + dy,
//                    (int) cx + halfWidth, (int) cy + dy + 1, color);
//        }
//    }
//
//    private void drawCircleOutline(GuiGraphicsExtractor graphics, double cx, double cy, int r, int color) {
//        for (int angle = 0; angle < 360; angle += 3) {
//            double rad = Math.toRadians(angle);
//            int px = (int) (cx + r * Math.cos(rad));
//            int py = (int) (cy + r * Math.sin(rad));
//            graphics.fill(px, py, px + 1, py + 1, color);
//        }
//    }
//}
