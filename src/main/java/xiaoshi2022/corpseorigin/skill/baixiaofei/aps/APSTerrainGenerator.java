package xiaoshi2022.corpseorigin.skill.baixiaofei.aps;

import net.minecraft.core.BlockPos;

public class APSTerrainGenerator {
    private static final double[][] GRADIENTS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {0.7071, 0.7071}, {-0.7071, 0.7071},
            {0.7071, -0.7071}, {-0.7071, -0.7071}
    };

    /** 整条山河的长度（沿河道方向） */
    public static final int LENGTH = 200;
    /** 河道半宽 */
    public static final int RIVER_HALF_WIDTH = 5;
    /** 河岸到山脚的缓冲 */
    public static final int BANK_WIDTH = 4;
    /** 山体从山脚升到山顶的水平距离 */
    public static final int MOUNTAIN_RUN = 50;
    /** 山最高升高多少格 */
    public static final int MOUNTAIN_HEIGHT = 64;
    /** 平台底座往下厚度 */
    public static final int PLATFORM_DEPTH = 6;
    /** 清空/保存的上界（相对 baseY） */
    public static final int COLUMN_TOP_OFFSET = 100;

    // ==================== 一剑山河 ====================

    public static double distanceToRiverCenter(int x, int z, BlockPos center,
                                               double dirX, double dirZ) {
        double dx = x - center.getX();
        double dz = z - center.getZ();
        return dx * (-dirZ) + dz * dirX;
    }

    public static double alongRiver(int x, int z, BlockPos center,
                                    double dirX, double dirZ) {
        double dx = x - center.getX();
        double dz = z - center.getZ();
        return dx * dirX + dz * dirZ;
    }

    /**
     * 区域：
     * -1 = 山河外
     *  0 = 河道
     *  1 = 河岸
     *  2 = 山体
     */
    public static int getZone(int x, int z, BlockPos center,
                              double dirX, double dirZ) {
        double along = alongRiver(x, z, center, dirX, dirZ);
        if (Math.abs(along) > LENGTH * 0.5) return -1;

        double perp = Math.abs(distanceToRiverCenter(x, z, center, dirX, dirZ));
        if (perp <= RIVER_HALF_WIDTH) return 0;
        if (perp <= RIVER_HALF_WIDTH + BANK_WIDTH) return 1;
        if (perp <= RIVER_HALF_WIDTH + BANK_WIDTH + MOUNTAIN_RUN) return 2;
        return -1;
    }

    /**
     * 地面高度：
     * - 河道：baseY - 3
     * - 河岸：baseY
     * - 山体：从山脚向上升，叠加山谷/山脊起伏
     */
    public static int calculateSwordLandHeight(int x, int z, BlockPos center,
                                               long seed,
                                               double dirX, double dirZ) {
        int baseY = center.getY();
        int zone = getZone(x, z, center, dirX, dirZ);
        if (zone < 0) return baseY;

        double along = alongRiver(x, z, center, dirX, dirZ);
        double perpAbs = Math.abs(distanceToRiverCenter(x, z, center, dirX, dirZ));

        switch (zone) {
            case 0: {
                double n = fractal(x, z, seed + 7000L);
                return baseY - 3 + (int) Math.round(n * 1.0);
            }
            case 1: {
                double n = fractal(x, z, seed + 8000L);
                return baseY + (int) Math.round(n * 1.0);
            }
            default: {
                double t = (perpAbs - RIVER_HALF_WIDTH - BANK_WIDTH) / (double) MOUNTAIN_RUN;
                t = Math.min(1.0, Math.max(0.0, t));

                double rise = Math.sqrt(t) * MOUNTAIN_HEIGHT;
                double valley = fractal((int) (along * 0.5), 0, seed + 21000L) * 14.0;
                double ridge = fractal((int) (perpAbs * 1.5), (int) (along * 1.5), seed + 9000L) * 8.0;
                double local = fractal(x, z, seed + 11000L) * 4.0;

                double height = rise + valley * t + ridge * t + local * t;
                return baseY + (int) Math.round(height);
            }
        }
    }

    // ==================== 噪声底层 ====================

    private static double fractal(int x, int z, long seed) {
        double noise = 0, amp = 1, freq = 0.02, max = 0;
        for (int i = 0; i < 4; i++) {
            noise += smooth(x * freq, z * freq, seed + i * 1000) * amp;
            max += amp;
            amp *= 0.5;
            freq *= 2;
        }
        return noise / max;
    }

    private static double smooth(double x, double z, long seed) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        int x1 = x0 + 1, z1 = z0 + 1;
        double sx = x - x0, sz = z - z0;
        sx = sx * sx * sx * (sx * (sx * 6 - 15) + 10);
        sz = sz * sz * sz * (sz * (sz * 6 - 15) + 10);
        double n00 = dot(x0, z0, x - x0, z - z0, seed);
        double n10 = dot(x1, z0, x - x1, z - z0, seed);
        double n01 = dot(x0, z1, x - x0, z - z1, seed);
        double n11 = dot(x1, z1, x - x1, z - z1, seed);
        return lerp(lerp(n00, n10, sx), lerp(n01, n11, sx), sz);
    }

    private static double dot(int gx, int gz, double dx, double dz, long seed) {
        long h = gx * 374761393L + gz * 668265263L + seed;
        h = (h ^ h >> 13) * 1274126177L;
        h ^= h >> 16;
        int i = (int) (h & 7);
        return GRADIENTS[i][0] * dx + GRADIENTS[i][1] * dz;
    }

    private static double lerp(double a, double b, double t) { return a + t * (b - a); }

    public static boolean isInCircle(int x, int z, BlockPos center, int radius) {
        double dx = x - center.getX(), dz = center.getZ() - z;
        return dx * dx + dz * dz <= radius * radius;
    }
}