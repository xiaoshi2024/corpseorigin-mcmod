package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.registry.CorpseKeyBindings;
import xiaoshi2022.corpseorigin.skill.ISkill;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能轮盘 GUI - 环形扇区排布（26.2 GuiGraphicsExtractor API）
 * 用"旋转矩形"拼出扇区：每段一个小矩形，绕中心旋转
 *
 * 打开/关闭由 CorpseOriginClient 的 tick 统一管理（按住显示，松开关闭），
 * 本 Screen 内部只处理 ESC 关闭，以及点击扇区释放技能（不关闭自己）。
 */
public class SkillWheelScreen extends Screen {

    private static final int SECTOR_SEGMENTS = 20;   // 每个扇区细分段数
    private static final float GAP_DEG = 2.0f;       // 扇区之间的间隙（角度）

    private record WheelSlot(ISkill skill, float midAngleDeg) {
    }

    private final List<WheelSlot> slots = new ArrayList<>();
    private int centerX;
    private int centerY;
    private float innerRadius;
    private float outerRadius;
    private float stepDeg;       // 每个扇区占的角度（度）
    private int hoveredSlot = -1;
    private static final int SKILLS_PER_PAGE = 10;
    private int page;
    private List<ISkill> allSkills = List.of();
    private net.minecraft.client.gui.components.Button previousPage;
    private net.minecraft.client.gui.components.Button nextPage;

    public SkillWheelScreen() {
        super(Component.translatable("gui.corpseorigin.skill_wheel"));
    }

    @Override
    protected void init() {
        centerX = this.width / 2;
        centerY = this.height / 2;
        outerRadius = Math.min(width, height) * 0.32f;
        innerRadius = outerRadius * 0.55f;

        clearWidgets();
        allSkills = List.copyOf(ClientCharacterCache.getActivatableSkills());
        int buttonY = Math.min(height - 24, (int)(centerY + outerRadius) + 20);
        previousPage = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("gui.corpseorigin.skill_wheel.previous"), b -> changePage(-1))
                .bounds(centerX - 90, buttonY, 80, 20).build());
        nextPage = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("gui.corpseorigin.skill_wheel.next"), b -> changePage(1))
                .bounds(centerX + 10, buttonY, 80, 20).build());
        rebuildPage();
    }

    private int pageCount() { return Math.max(1, (allSkills.size() + SKILLS_PER_PAGE - 1) / SKILLS_PER_PAGE); }

    private void rebuildPage() {
        page = Math.clamp(page, 0, pageCount() - 1);
        hoveredSlot = -1;
        slots.clear();
        int start = page * SKILLS_PER_PAGE;
        List<ISkill> skills = allSkills.subList(start, Math.min(start + SKILLS_PER_PAGE, allSkills.size()));
        previousPage.visible = nextPage.visible = pageCount() > 1;
        previousPage.active = page > 0;
        nextPage.active = page < pageCount() - 1;
        int n = skills.size();
        if (n == 0) {
            stepDeg = 0;
            return;
        }
        stepDeg = 360f / n;
        // 从正上方开始，顺时针均分
        for (int i = 0; i < n; i++) {
            float mid = -90f + stepDeg * i;
            slots.add(new WheelSlot(skills.get(i), mid));
        }
    }

    private void refreshSkills() {
        var current = ClientCharacterCache.getActivatableSkills();
        if (!allSkills.stream().map(ISkill::getId).toList().equals(current.stream().map(ISkill::getId).toList())) {
            allSkills = List.copyOf(current);
            rebuildPage();
        }
    }

    private void changePage(int direction) {
        refreshSkills();
        page = Math.clamp(page + direction, 0, pageCount() - 1);
        rebuildPage();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && pageCount() > 1) {
            changePage(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        refreshSkills();
        // 半透明背景
        graphics.fill(0, 0, this.width, this.height, 0x66000000);

        if (slots.isEmpty()) {
            graphics.centeredText(font, title, centerX, centerY - 4, 0xFFFFFFAA);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int hovered = sectorAt(mouseX, mouseY);
        hoveredSlot = hovered;

        // ✅ 画扇区：每个扇区用 SECTOR_SEGMENTS 个旋转矩形拼
        for (int i = 0; i < slots.size(); i++) {
            WheelSlot slot = slots.get(i);
            ISkill skill = slot.skill();
            boolean onCooldown = ClientState.getCooldownRemaining(skill.getId().getPath()) > 0;
            boolean isHover = i == hovered;

            int baseColor = onCooldown ? 0xCC333344 : 0xCC224433;
            if (isHover) {
                baseColor = onCooldown ? 0xCC444466 : 0xCC44AA44;
            }

            drawSector(graphics, slot.midAngleDeg(), baseColor);
        }

        // ✅ 画扇区边界线（外缘弧线）
        for (int i = 0; i < slots.size(); i++) {
            WheelSlot slot = slots.get(i);
            ISkill skill = slot.skill();
            boolean onCooldown = ClientState.getCooldownRemaining(skill.getId().getPath()) > 0;
            boolean isHover = i == hovered;
            int lineColor = isHover ? 0xFFAAFFAA : (onCooldown ? 0xFF666677 : 0xFF88FF88);
            drawArcLine(graphics, slot.midAngleDeg(), outerRadius, lineColor);
        }

        // ✅ 画名称 + 冷却
        for (WheelSlot slot : slots) {
            ISkill skill = slot.skill();
            boolean onCooldown = ClientState.getCooldownRemaining(skill.getId().getPath()) > 0;

            float midRad = (float) Math.toRadians(slot.midAngleDeg());
            float labelR = (innerRadius + outerRadius) * 0.5f;
            int lx = (int) (centerX + Math.cos(midRad) * labelR);
            int ly = (int) (centerY + Math.sin(midRad) * labelR);

            String name = skill.getName().getString();
            if (name.length() > 5) {
                name = name.substring(0, 5);
            }
            graphics.centeredText(font, name, lx, ly - 4,
                    onCooldown ? 0xFF999999 : 0xFFFFFFFF);

            int remaining = ClientState.getCooldownRemaining(skill.getId().getPath());
            if (remaining > 0) {
                graphics.centeredText(font, (remaining / 20 + 1) + "s",
                        lx, ly + 6, 0xFFFF5555);
            }
        }

        // ✅ 中心信息
        if (hovered >= 0 && hovered < slots.size()) {
            ISkill skill = slots.get(hovered).skill();
            graphics.centeredText(font, skill.getName(), centerX, centerY - 8, 0xFFFFFFAA);
            var resourceCost = skill.getResourceCost();
            graphics.centeredText(font, Component.translatable("gui.corpseorigin.skill_wheel.cost", resourceCost.inner(), resourceCost.blood()),
                    centerX, centerY + 28, 0xFF99CCFF);
            int cd = ClientState.getCooldownRemaining(skill.getId().getPath());
            if (cd > 0) {
                graphics.centeredText(font, cd / 20 + 1 + "s", centerX, centerY + 4, 0xFFFF5555);
            } else {
                graphics.centeredText(font, "\u5c31\u7eea", centerX, centerY + 4, 0xFF55FF55);
            }
            graphics.centeredText(font,
                    Component.translatable("hud.corpseorigin.skill_slot.bind_hint"),
                    centerX, centerY + 16, 0xFFBBBBBB);
        } else {
            graphics.centeredText(font, title, centerX, centerY - 8, 0xFFFFFFAA);
            graphics.centeredText(font,
                    Component.translatable("gui.corpseorigin.skill_wheel.points", ClientState.availablePoints),
                    centerX, centerY + 4, 0xFF55FF55);
        }

        graphics.centeredText(font, Component.translatable("gui.corpseorigin.skill_wheel.page",
                page + 1, pageCount(), allSkills.size()), centerX, Math.max(4, (int)(centerY - outerRadius) - 26), 0xFFFFFFFF);
        if (pageCount() > 1) graphics.centeredText(font,
                Component.translatable("gui.corpseorigin.skill_wheel.paging_hint"),
                centerX, Math.max(15, (int)(centerY - outerRadius) - 14), 0xFFBBBBBB);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    // ==================== 扇区绘制 ====================

    /** 把一个扇区近似成 N 个"旋转的细矩形" */
    private void drawSector(GuiGraphicsExtractor g, float midAngleDeg, int color) {
        float halfSpan = (stepDeg - GAP_DEG * 2) * 0.5f;
        float segAngle = (halfSpan * 2) / SECTOR_SEGMENTS;

        float midR = (innerRadius + outerRadius) * 0.5f;
        float w = outerRadius - innerRadius;
        float h = (float) (2 * Math.PI * midR * segAngle / 360.0);

        for (int i = 0; i < SECTOR_SEGMENTS; i++) {
            float a = midAngleDeg - halfSpan + segAngle * i;

            g.pose().pushMatrix();
            g.pose().translate(centerX, centerY);
            g.pose().rotate((float) Math.toRadians(a));
            int x0 = (int) (midR - w / 2);
            int y0 = (int) (-h / 2);
            int x1 = (int) (midR + w / 2);
            int y1 = (int) (h / 2);
            g.fill(x0, y0, x1, y1, color);
            g.pose().popMatrix();
        }
    }

    /** 在扇区外缘画一段弧（用短线段拼） */
    private void drawArcLine(GuiGraphicsExtractor g, float midAngleDeg, float radius, int color) {
        float halfSpan = (stepDeg - GAP_DEG * 2) * 0.5f;
        int segs = 6;
        for (int i = 0; i < segs; i++) {
            float a = midAngleDeg - halfSpan + (halfSpan * 2) * i / segs;
            float b = midAngleDeg - halfSpan + (halfSpan * 2) * (i + 1) / segs;
            float ar = (float) Math.toRadians(a);
            float br = (float) Math.toRadians(b);
            int x0 = (int) (centerX + Math.cos(ar) * radius);
            int y0 = (int) (centerY + Math.sin(ar) * radius);
            int x1 = (int) (centerX + Math.cos(br) * radius);
            int y1 = (int) (centerY + Math.sin(br) * radius);
            drawLine(g, x0, y0, x1, y1, color);
        }
    }

    /** 用 Bresenham 画一条线（用 fill 逐像素） */
    private void drawLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    // ==================== 扇区几何 ====================

    private int sectorAt(int mouseX, int mouseY) {
        if (slots.isEmpty() || stepDeg == 0) return -1;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.hypot(dx, dy);
        if (dist < innerRadius || dist > outerRadius) return -1;

        double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
        double rel = angleDeg + 90;
        rel = (rel % 360 + 360) % 360;
        rel += stepDeg / 2;
        rel = rel % 360;
        return (int) (rel / stepDeg);
    }

    // ==================== 输入 ====================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        refreshSkills();
        if (event.button() == 0) {
            int mouseX = (int) event.x();
            int mouseY = (int) event.y();
            int index = sectorAt(mouseX, mouseY);
            if (index >= 0 && index < slots.size()) {
                ISkill skill = slots.get(index).skill();
                ChameleonDisguiseScreen.activate(skill.getId().getPath());
                // ❌ 不 onClose：让外层 tick 的"松开关闭"统一处理
                return true;
            }
        }
        // ❌ 右键不 onClose
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        refreshSkills();
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) { changePage(-1); return true; }
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) { changePage(1); return true; }
        if (event.key() >= org.lwjgl.glfw.GLFW.GLFW_KEY_1
                && event.key() <= org.lwjgl.glfw.GLFW.GLFW_KEY_3
                && hoveredSlot >= 0 && hoveredSlot < slots.size()) {
            int quickSlot = event.key() - org.lwjgl.glfw.GLFW.GLFW_KEY_1;
            ISkill skill = slots.get(hoveredSlot).skill();
            SkillHotbarState.bind(quickSlot, skill);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendOverlayMessage(Component.translatable(
                        "hud.corpseorigin.skill_slot.bound", quickSlot + 1, skill.getName()));
            }
            return true;
        }
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        // ✅ 松开轮盘键 → 关闭轮盘
        if (CorpseKeyBindings.openSkillWheel.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
