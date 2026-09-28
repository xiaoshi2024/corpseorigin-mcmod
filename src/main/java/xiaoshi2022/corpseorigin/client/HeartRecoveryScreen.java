package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.network.HeartWakePayload;
import xiaoshi2022.corpseorigin.skill.heixiaofei.HeartImplant;

public final class HeartRecoveryScreen extends Screen {
    private static boolean shown;
    public HeartRecoveryScreen() { super(Component.translatable("gui.corpseorigin.heart.title")); }
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean active = client.player != null && HeartImplant.active(client.player);
            if (!active) {
                shown = false;
                if (client.gui.screen() instanceof HeartRecoveryScreen) client.gui.setScreen(null);
            } else if (!shown && client.gui.screen() == null) {
                shown = true;
                client.gui.setScreen(new HeartRecoveryScreen());
            }
        });
    }
    @Override protected void init() {
        int x = width / 2 - 100, y = height / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.corpseorigin.heart.wake"), b -> {
            ClientPlayNetworking.send(new HeartWakePayload()); b.active = false;
        }).bounds(x,y,200,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.corpseorigin.heart.stay"), b -> onClose())
                .bounds(x,y+26,200,20).build());
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        super.extractRenderState(g,mx,my,delta);
        g.centeredText(font,title,width/2,height/2-36,0xFFFFD879);
        g.centeredText(font,Component.translatable("gui.corpseorigin.heart.hint"),width/2,height/2-19,0xFFFFFFFF);
    }
}
