package xiaoshi2022.corpseorigin.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 「闭眼」黑场：意识转移过场期间盖住整个屏幕。
 * <p>
 * 过场一开始淡入到全黑（闭眼），服务端换完身体、镜头交还的那一刻淡出（睁眼）——
 * 这样"闭眼 → 再睁开已经在另一具身体里"是连贯的，也把镜头瞬移回玩家身上那一帧藏了起来。
 * <p>
 * 用 Screen 而不是 HUD 层来画：过场期间 HUD 是被 {@code PersistentCameraEntity#setup} 关掉的，
 * 挂在 HUD 上的东西根本不会渲染。所以这里把 {@link #isPauseScreen()} 设成 false
 * （不能暂停游戏，服务端还得照常换身）、{@link #shouldCloseOnEsc()} 设成 false（ESC 不能打断过场）。
 */
@Environment(EnvType.CLIENT)
public class CameraBlackoutScreen extends Screen {

    /** 淡入/淡出速度：每 tick 变化多少 alpha（约 12 tick = 0.6 秒走完） */
    private static final float FADE_PER_TICK = 1.0F / 12.0F;

    private static CameraBlackoutScreen instance;

    private float alpha;
    private float targetAlpha;

    private CameraBlackoutScreen() {
        super(Component.empty());
    }

    /** 过场开始：铺一层全黑的幕，然后淡到全黑 */
    public static void fadeIn(Minecraft client) {
        if (client.player == null) {
            return;
        }
        if (instance == null) {
            instance = new CameraBlackoutScreen();
        }
        instance.alpha = 0.0F;
        instance.targetAlpha = 1.0F;
        client.gui.setScreen(instance);
    }

    /** 过场结束：淡出（黑 → 亮），淡完自己摘掉这层幕 */
    public static void fadeOut() {
        if (instance != null) {
            instance.targetAlpha = 0.0F;
        }
    }

    /** 强行撤掉这层幕（换身失败、退出世界之类的情况） */
    public static void dismiss() {
        if (instance == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() == instance) {
            client.gui.setScreen(null);
        }
        instance = null;
    }

    @Override
    public void tick() {
        if (this.alpha < this.targetAlpha) {
            this.alpha = Math.min(this.targetAlpha, this.alpha + FADE_PER_TICK);
        } else if (this.alpha > this.targetAlpha) {
            this.alpha = Math.max(this.targetAlpha, this.alpha - FADE_PER_TICK);
        }
        // 淡出结束 → 撤幕
        if (this.targetAlpha <= 0.0F && this.alpha <= 0.0F) {
            dismiss();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int a = Math.round(Mth.clamp(this.alpha, 0.0F, 1.0F) * 255.0F);
        if (a <= 0) {
            return;
        }
        graphics.fill(0, 0, this.width, this.height, a << 24);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
