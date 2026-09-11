package xiaoshi2022.corpseorigin.skill.baixiaofei.aps;

import net.minecraft.core.BlockPos;

public class APSTerrainGenerator {
    private static final double[][] GRADIENTS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {0.7071, 0.7071}, {-0.7071, 0.7071},
            {0.7071, -0.7071}, {-0.7071, -0.7071}
    };

    public static int calculateHeight(int x, int z, BlockPos center, int radius, long seed) {
        double n = fractal(x, z, seed + 5000L);
        return center.getY() - 1 + (int) Math.round(n * 3.0);
    }

    public static int calculateFlatHeight(int x, int z, int baseY, long seed) {
        double n = fractal(x, z, seed + 5000L);
        return baseY - 1 + (int) Math.round(n * 3.0);
    }

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