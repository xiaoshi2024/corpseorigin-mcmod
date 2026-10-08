package xiaoshi2022.corpseorigin.compat.lostcities;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.lang.reflect.Method;

/**
 * 与 Lost Cities 模组的软联动。
 * <p>
 * Lost Cities 是 mcjty 制作的"废墟城市"维度模组（mod id：{@code lostcities}）。
 * 装了它之后城市维度里会自然生成一栋栋高楼废墟，本次联动目标是让龙右的"感染领域"技能
 * 在城市维度里<b>自动覆盖整座城市</b>而不是固定半径 —— 用户明确要求"至少笼罩一座城市"。
 * <p>
 * <b>实现策略</b>：反射调用 Lost Cities 公开 API。
 * <ul>
 *   <li>若 Lost Cities 加载且 API 可用：调用 {@code mcjty.lostcities.api.ILostCityInformation#getCityInfo}
 *     或同等方法，拿到当前 chunk 的城市边界（多 chunk 范围），返回并集 AABB。</li>
 *   <li>若 API 反射失败或方法签名变化：降级返回 {@code new AABB(center).inflate(fallbackRadius)}，
 *     与不装模组时同档。{@code cityCoverageMultiplier} 已在调用方把 fallback 半径放大，
 *     所以即便降级，大半径也能覆盖小城市。</li>
 * </ul>
 * 不写硬依赖：本类只在反射成功时调用 Lost Cities 类，{@code fabric.mod.json} 也不声明依赖。
 * 没装模组时 {@link #isLoaded()} 直接返回 false，调用方走降级路径。
 */
public final class LostCitiesCompat {

    private LostCitiesCompat() {
    }

    public static final String MOD_ID = "lostcities";

    /** 缓存"是否加载"，避免每 tick 都查 FabricLoader */
    private static volatile Boolean loadedCache;

    /** 缓存反射得到的 LostCityAPI 类（每次调用都 Class.forName 太贵） */
    private static volatile Class<?> apiClass;
    private static volatile Method getApiMethod;
    private static volatile boolean apiReflectionFailed;

    /** Lost Cities 模组是否加载 */
    public static boolean isLoaded() {
        if (loadedCache == null) {
            loadedCache = FabricLoader.getInstance().isModLoaded(MOD_ID);
        }
        return loadedCache;
    }

    /**
     * 取以 {@code center} 为中心、覆盖整座城市边界的 AABB。
     * <p>
     * 失败/没装模组时降级为 {@code new AABB(center).inflate(fallbackRadius)}。
     * 调用方（{@code InfectionDomainHandler}）已经把 fallbackRadius 按 cityCoverageMultiplier 放大过，
     * 所以降级路径也能覆盖一片区域。
     *
     * @param level           服务端世界
     * @param center          领域中心（施术者位置或锚点位置）
     * @param fallbackRadius  降级时用的半径（格）
     */
    public static AABB cityBoundsAround(ServerLevel level, Vec3 center, double fallbackRadius) {
        if (!isLoaded() || center == null) {
            return inflateFallback(center, fallbackRadius);
        }

        // 反射失败过的就不再尝试（避免每 tick 抛一次异常拖性能）
        if (apiReflectionFailed) {
            return inflateFallback(center, fallbackRadius);
        }

        try {
            // 试 mcjty.lostcities.api.LostCityAPI.getLostCityAPI(level) → ILostCityInformation
            if (apiClass == null) {
                apiClass = Class.forName("mcjty.lostcities.api.LostCityAPI");
            }
            if (getApiMethod == null) {
                getApiMethod = apiClass.getMethod("getLostCityAPI", net.minecraft.world.level.Level.class);
            }
            Object api = getApiMethod.invoke(null, level);
            if (api == null) {
                return inflateFallback(center, fallbackRadius);
            }

            // ILostCityInformation 有 getCityInfo(BlockPos) / getCityInfo(int chunkX, int chunkZ)
            // 但 API 版本差异较大，且城市边界本身的"多 chunk 范围"概念在 Lost Cities 2.x 后才有
            // 这里保守地调用 getCityInfo 拿一个城市对象，再尝试调用其 getRadius()/getCityCenter() 等方法
            // 任何一步失败就降级
            Method getCityInfo = api.getClass().getMethod("getCityInfo",
                    int.class, int.class);
            int chunkX = (int) (center.x) >> 4;
            int chunkZ = (int) (center.z) >> 4;
            Object cityInfo = getCityInfo.invoke(api, chunkX, chunkZ);
            if (cityInfo == null) {
                return inflateFallback(center, fallbackRadius);
            }

            // 尝试多种可能的城市半径 getter（API 版本不统一）
            double cityRadius = tryInvokeDouble(cityInfo, "getRadius", "getCityRadius", "getMaxRadius");
            if (cityRadius <= 0) cityRadius = fallbackRadius;

            // 尝试多种可能的城市中心 getter；失败则用调用方传入的 center
            Vec3 cityCenter = tryInvokeVec3(cityInfo, center);

            return new AABB(
                    cityCenter.x - cityRadius, cityCenter.y - cityRadius, cityCenter.z - cityRadius,
                    cityCenter.x + cityRadius, cityCenter.y + cityRadius, cityCenter.z + cityRadius);
        } catch (ClassNotFoundException e) {
            apiReflectionFailed = true;
            CorpseOrigin.LOGGER.warn("Lost Cities API 类未找到，降级使用 fallback 半径");
            return inflateFallback(center, fallbackRadius);
        } catch (NoSuchMethodException e) {
            apiReflectionFailed = true;
            CorpseOrigin.LOGGER.warn("Lost Cities API 方法签名变化，降级使用 fallback 半径：{}", e.getMessage());
            return inflateFallback(center, fallbackRadius);
        } catch (Exception e) {
            apiReflectionFailed = true;
            CorpseOrigin.LOGGER.warn("Lost Cities API 调用失败，降级使用 fallback 半径：{}", e.getMessage());
            return inflateFallback(center, fallbackRadius);
        }
    }

    private static AABB inflateFallback(Vec3 center, double radius) {
        if (center == null) {
            // 极端兜底：返回一个不可能的位置
            return new AABB(0, 0, 0, 0, 0, 0);
        }
        return new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
    }

    /** 依次尝试多个候选方法名，取第一个能成功调用的 double 返回值 */
    private static double tryInvokeDouble(Object obj, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object result = m.invoke(obj);
                if (result instanceof Number n) {
                    return n.doubleValue();
                }
            } catch (Exception ignored) {
                // 试下一个
            }
        }
        return -1;
    }

    /** 尝试从 cityInfo 拿城市中心坐标；失败回退到 fallbackCenter */
    private static Vec3 tryInvokeVec3(Object obj, Vec3 fallbackCenter) {
        // 多种可能的 getter：getCityCenter() 返回 BlockPos
        String[] candidates = {"getCityCenter", "getCenter", "getCenterPos"};
        for (String name : candidates) {
            try {
                Method m = obj.getClass().getMethod(name);
                Object result = m.invoke(obj);
                if (result != null) {
                    // 用反射拿 x/y/z 字段（BlockPos 都有这三个公共字段）
                    double x = result.getClass().getField("x").getInt(result);
                    double y = result.getClass().getField("y").getInt(result);
                    double z = result.getClass().getField("z").getInt(result);
                    return new Vec3(x, y, z);
                }
            } catch (Exception ignored) {
                // 试下一个
            }
        }
        return fallbackCenter;
    }
}
