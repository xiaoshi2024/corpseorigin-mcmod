package xiaoshi2022.corpseorigin.client.aps;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class APSInkSceneManager {

    public static boolean ACTIVE = false;
    public static UUID CASTER_ID = null;

    public static double CENTER_X, CENTER_Y, CENTER_Z;

    /** 河道方向（单位向量） */
    public static double RIVER_DIR_X = 1.0;
    public static double RIVER_DIR_Z = 0.0;

    public static long START_TICK = 0L;
    public static long CLOSE_TICK = -1L;

    // ==================== 时间轴 ====================

    /** 天空淡入时长 */
    public static final int SKY_FADE_TICKS = 20;
    /** 远山淡入时长 */
    public static final int SCENE_FADE_TICKS = 25;
    /** 关闭淡出时长 */
    public static final int CLOSE_FADE_TICKS = 30;

    // ==================== 诗牌（逐句落下） ====================

    /** 诗牌总数 */
    public static final int POEM_LINES = 4;
    /** 每句诗的下落开始 tick，-1 表示还没落 */
    public static final long[] POEM_DROP_TICK = new long[]{-1L, -1L, -1L, -1L};
    /** 单句从"天上"落到"目标位置"所需 tick */
    public static final int POEM_FALL_TICKS = 35;

    private APSInkSceneManager() {}

    // ==================== 开/关 ====================

    public static void open(UUID casterId, double x, double y, double z,
                            double riverDirX, double riverDirZ) {
        ACTIVE = true;
        CASTER_ID = casterId;
        CENTER_X = x;
        CENTER_Y = y;
        CENTER_Z = z;
        RIVER_DIR_X = riverDirX;
        RIVER_DIR_Z = riverDirZ;
        START_TICK = nowTick();
        CLOSE_TICK = -1L;
        for (int i = 0; i < POEM_LINES; i++) POEM_DROP_TICK[i] = -1L;
    }

    public static void close() {
        if (!ACTIVE) return;
        CLOSE_TICK = nowTick();
    }

    /** 服务端通知：第 lineIndex 句开始下落 */
    public static void dropPoem(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= POEM_LINES) return;
        if (POEM_DROP_TICK[lineIndex] < 0) {
            POEM_DROP_TICK[lineIndex] = nowTick();
        }
    }

    /** 第 i 句的进度：0=还没出现，1=已落定；返回 -1 表示还没落 */
    public static float poemProgress(int i) {
        long drop = POEM_DROP_TICK[i];
        if (drop < 0) return -1f;
        float t = (float)(nowTick() - drop) / POEM_FALL_TICKS;
        return clamp01(t);
    }

    // ==================== 时间轴 ====================

    public static float elapsedTicks() {
        if (!ACTIVE) return 0f;
        return (float)(nowTick() - START_TICK);
    }

    public static float closingTicks() {
        if (CLOSE_TICK < 0) return -1f;
        return (float)(nowTick() - CLOSE_TICK);
    }

    /** 天空 alpha */
    public static float getGroundAlpha() {
        float t = elapsedTicks();
        float fade = smooth(clamp01(t / SKY_FADE_TICKS));
        return fade * getExitAlpha();
    }

    /** 远山 alpha */
    public static float getSceneAlpha() {
        float t = elapsedTicks() - SKY_FADE_TICKS;
        float fade = smooth(clamp01(t / SCENE_FADE_TICKS));
        return fade * getExitAlpha();
    }

    public static float getExitAlpha() {
        float c = closingTicks();
        if (c < 0) return 1f;
        return 1f - smooth(clamp01(c / CLOSE_FADE_TICKS));
    }

    public static boolean shouldStop() {
        if (!ACTIVE) return true;
        float c = closingTicks();
        if (c < 0) return false;
        if (c >= CLOSE_FADE_TICKS) {
            ACTIVE = false;
            return true;
        }
        return false;
    }

    public static Vec3 center() {
        return new Vec3(CENTER_X, CENTER_Y, CENTER_Z);
    }

    public static boolean shouldRender() {
        return ACTIVE && (getGroundAlpha() > 0.01f || getSceneAlpha() > 0.01f);
    }

    // ==================== 工具 ====================

    private static long nowTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0L;
        return mc.level.getGameTime();
    }

    static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    static float smooth(float v) {
        v = clamp01(v);
        return v * v * (3f - 2f * v);
    }

    static float smoother(float v) {
        v = clamp01(v);
        return v * v * v * (v * (v * 6f - 15f) + 10f);
    }
}