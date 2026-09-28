package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.network.NestRadarPayload;

import java.util.List;

public final class NestRadarScreen extends Screen {
    private List<NestRadarPayload.Contact> contacts;
    private int selected = -1, ticks;
    private Button digest, kill;
    public NestRadarScreen(List<NestRadarPayload.Contact> contacts) {
        super(Component.translatable("skill.corpseorigin.nest_sense")); this.contacts = contacts;
    }
    public void update(List<NestRadarPayload.Contact> contacts) {
        this.contacts = contacts;
        if (contacts.stream().noneMatch(c -> c.id() == selected)) selected = -1;
    }
    private int radius() { return Math.max(24, Math.min(width / 2 - 16, (height - 112) / 2)); }
    private int cy() { return 30 + radius(); }
    @Override protected void init() {
        int w = Math.min(90, (width - 24) / 2);
        digest = addRenderableWidget(Button.builder(Component.translatable("gui.corpseorigin.nest.digest"),
                b -> act(1)).bounds(width / 2 - w - 4, height - 28, w, 20).build());
        kill = addRenderableWidget(Button.builder(Component.translatable("gui.corpseorigin.nest.kill"),
                b -> act(2)).bounds(width / 2 + 4, height - 28, w, 20).build());
    }
    private void act(int mode) {
        if (selected >= 0) ClientPlayNetworking.send(new NestRadarPayload.Action(selected, mode));
    }
    @Override public void tick() {
        if (++ticks % 20 == 0) ClientPlayNetworking.send(new NestRadarPayload.Action(-1, 0));
        digest.active = kill.active = selected >= 0;
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() {
        ClientPlayNetworking.send(new NestRadarPayload.Action(-1, -1));
        super.removed();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        int r = radius(), cx = width / 2, cy = cy();
        g.fill(cx-r, cy-r, cx+r, cy+r, 0xF0101918);
        for (int ring = 1; ring <= 3; ring++) for (int a = 0; a < 360; a += 3) {
            double rad = Math.toRadians(a);
            int x = cx + (int)(Math.cos(rad)*r*ring/3), y = cy + (int)(Math.sin(rad)*r*ring/3);
            g.fill(x,y,x+1,y+1,0xFF35574B);
        }
        g.fill(cx-r,cy,cx+r,cy+1,0xFF35574B); g.fill(cx,cy-r,cx+1,cy+r,0xFF35574B);
        g.fill(cx-2,cy-2,cx+3,cy+3,0xFFFFFFFF);
        g.centeredText(font, title, cx, 10, 0xFFFFFFFF);
        for (var c : contacts) {
            int x = cx + (int)(c.x()/128*r), y = cy + (int)(c.z()/128*r);
            int color = c.id() == selected ? 0xFFFFD66B : c.alarm() ? 0xFFFF5757 : 0xFF60D9A0;
            g.fill(x-3,y-3,x+4,y+4,color);
            if (Math.abs(mx-x)<6 && Math.abs(my-y)<6 || c.id() == selected) {
                String label = font.plainSubstrByWidth(c.name(), Math.max(40, width-32));
                g.centeredText(font, label, cx, height-64, color);
            }
        }
        g.centeredText(font, Component.translatable(contacts.isEmpty() ? "gui.corpseorigin.nest.empty"
                : "gui.corpseorigin.nest.contacts", contacts.size()), cx, height-46, 0xFFBCCBC3);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        if (e.button() == 0) for (var c : contacts) {
            int x = width/2 + (int)(c.x()/128*radius()), y = cy() + (int)(c.z()/128*radius());
            if (Math.abs(e.x()-x)<7 && Math.abs(e.y()-y)<7) { selected=c.id(); return true; }
        }
        return super.mouseClicked(e, doubled);
    }
}
