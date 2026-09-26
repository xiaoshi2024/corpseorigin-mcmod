package xiaoshi2022.corpseorigin.entity.animation;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.instance.InstancedAnimatableInstanceCache;
import com.geckolib.constant.dataticket.DataTicket;

import java.util.HashMap;
import java.util.Map;

/**
 * 低阶尸兄的动画缓存：本体两套控制器照旧，另外为挂在身上的器官各保留一个独立
 * {@link AnimatableManager}，让每个器官播自己的动画。
 * <p>
 * 与玩家的 {@code PlayerLayerAnimationCache} 同样的思路：器官渲染器用
 * {@link #organId(int)} 作为实例 id 取管理器；id 落在器官区间内时，
 * 从单独的 instanced cache 拿（管理器仍由尸兄实体自己的 registerControllers 构建）。
 */
public final class ZbLayerAnimationCache extends InstancedAnimatableInstanceCache {

    /** 器官渲染状态里"当前要播哪条 clip"的票据。 */
    public static final DataTicket<String> CLIP = DataTicket.create("corpseorigin.organ_clip", String.class);

    private static final long ORGAN_BASE = Long.MIN_VALUE + 64;
    private static final int ORGAN_SLOTS = 16;

    private final Map<Long, InstancedAnimatableInstanceCache> organs = new HashMap<>();

    public ZbLayerAnimationCache(GeoAnimatable host) {
        super(host);
    }

    public static long organId(int slot) {
        return ORGAN_BASE + Math.max(0, Math.min(ORGAN_SLOTS - 1, slot));
    }

    @Override
    public AnimatableManager<?> getManagerForId(long id) {
        if (id >= ORGAN_BASE && id < ORGAN_BASE + ORGAN_SLOTS) {
            return organs.computeIfAbsent(id,
                    k -> new InstancedAnimatableInstanceCache(this.animatable)).getManagerForId(id);
        }
        return super.getManagerForId(id);
    }
}
