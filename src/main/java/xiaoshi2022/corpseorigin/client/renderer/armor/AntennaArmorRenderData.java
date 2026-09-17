package xiaoshi2022.corpseorigin.client.renderer.armor;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;

/**
 * 天线宝宝尸兄盔甲自己的动画信号。
 * <p>
 * 挥击不在这里另开一套：直接沿用 GeckoLib 的 {@link DataTickets#SWINGING_ARM}
 * （实体渲染器也是拿 {@code LivingEntity.swinging} 填这个 ticket 的），
 * 由 {@link AntennaZBRitemRenderer} 在盔甲填充 render state 时写入。
 * <p>
 * ⚠️ 这些是每帧重建的临时数据，<b>不能</b>当逻辑状态用；"谁在吸食谁"这种逻辑状态
 * 只信服务端同步过来的数据（见 {@code CorpseOriginClient.antennaSucks}）。
 */
public final class AntennaArmorRenderData {

    /** 穿戴着是否在吸食 —— 驱动 absorb */
    public static final DataTicket<Boolean> ABSORBING =
            DataTicket.create("antenna_armor_absorbing", Boolean.class);

    /**
     * 吸食目标的水平偏角（度）—— 喂给动画里的 {@code query.target_y_rotation}。
     * <p>
     * 语义见 {@link AntennaZBRitemRenderer} 里的瞄准算法：0 = 正前方，正数 = 目标在右手边。
     */
    public static final DataTicket<Double> TARGET_Y_ROTATION =
            DataTicket.create("antenna_armor_target_y_rotation", Double.class);

    /**
     * 天线指向吸食目标所需的<b>从竖直向上起算的倾斜角</b>（度）—— 喂给动画里的
     * {@code query.target_x_rotation}。
     * <p>
     * 语义见 {@link AntennaZBRitemRenderer} 里的瞄准算法：0 = 目标在正上方，
     * 90 = 目标与天线根部同高（插脑门基本就是这个角度），&gt;90 = 目标在下方。
     * <p>
     * ⚠️ 这里<b>不是</b>原版那种"俯仰角"（0 = 水平）：天线在模型里是朝正上方长出去的，
     * 给俯仰角的话天线只会原地打转、不会朝目标伸出去。
     */
    public static final DataTicket<Double> TARGET_X_ROTATION =
            DataTicket.create("antenna_armor_target_x_rotation", Double.class);

    /**
     * 末尾"刺出去"那一段 {@code bone6} 该用的 Y 缩放 —— 按"根部到目标脑门的距离"反算出来的，
     * 让骨链伸直后的尖端正好落在脑门上（1.0 = 动画原本的基准长度）。
     */
    public static final DataTicket<Double> TARGET_STRETCH =
            DataTicket.create("antenna_armor_target_stretch", Double.class);

    private AntennaArmorRenderData() {
    }
}
