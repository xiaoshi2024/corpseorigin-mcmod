package xiaoshi2022.corpseorigin.client.limb;

import com.geckolib.constant.dataticket.DataTicket;

import java.util.List;

/**
 * 挂在 {@code AvatarRenderState} 上的断肢渲染数据 / 动画信号。
 * <p>
 * GeckoLib 5.5.5 自带 {@code client.EntityRenderStateMixin}，把 {@code GeoRenderState} 混进了原版所有
 * EntityRenderState，所以 AvatarRenderState 本身就是 GeoRenderState，可以直接 addGeckolibData，
 * 不需要像 Woodwalkers 那样挂 Supplier、也不需要造假实体。
 * <p>
 * 这些是每帧重建的临时数据，**不能**当逻辑状态用；逻辑状态只信服务端的 PLAYER_CORPSE 附件。
 */
public final class LimbRenderData {

    /** 断肢位掩码；0 或缺省 = 正常渲染 */
    public static final DataTicket<Integer> LIMB_MASK =
            DataTicket.create("corpse_limb_mask", Integer.class);

    /** 是否在移动 —— 驱动 idle / walk 切换 */
    public static final DataTicket<Boolean> MOVING =
            DataTicket.create("corpse_limb_moving", Boolean.class);

    /** 是否在挥击 —— 驱动 attack */
    public static final DataTicket<Boolean> ATTACKING =
            DataTicket.create("corpse_limb_attacking", Boolean.class);

    // ==================== 再生进度（每个部位一个，单位 0→1） ====================
    // 取值语义见 ClientLimbCache：ClientLimbCache.PERMANENT = 断了不会自愈，ClientLimbCache.INTACT = 完好。
    // 拆成每部位一个信号，是为了让"再生动画"能按部位各播一条（见 corpse_player.animation.json 的 regrow_*）。

    public static final DataTicket<Float> REGROW_RIGHT_ARM =
            DataTicket.create("corpse_limb_regrow_right_arm", Float.class);
    public static final DataTicket<Float> REGROW_LEFT_ARM =
            DataTicket.create("corpse_limb_regrow_left_arm", Float.class);
    public static final DataTicket<Float> REGROW_RIGHT_LEG =
            DataTicket.create("corpse_limb_regrow_right_leg", Float.class);
    public static final DataTicket<Float> REGROW_LEFT_LEG =
            DataTicket.create("corpse_limb_regrow_left_leg", Float.class);

    /** 按下标取进度信号，顺序与 {@link xiaoshi2022.corpseorigin.limb.LimbSlots} 的部位下标一致 */
    public static final List<DataTicket<Float>> REGROW_BY_SLOT = List.of(
            REGROW_RIGHT_ARM, REGROW_LEFT_ARM, REGROW_RIGHT_LEG, REGROW_LEFT_LEG);

    private LimbRenderData() {
    }
}
