package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.dataticket.DataTicket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

import java.util.UUID;

/**
 * 开胃奶背挂（{@code niunaix}）的渲染数据和状态。
 * <p>
 * 形态本身是服务端状态（{@code PLAYER_CORPSE} 附件里的变种号），随 {@code PlayerCorpseSyncS2C}
 * 形态由服务端同步，并缓存在 {@link CorpseOriginClient#corpseDataCache} 中，
 * 因此自己和其他玩家看到的是同一套背挂，无需额外网络包。
 * <p>
 * 三个 ticket 都是每帧重建的临时数据，只在渲染管线中传递。
 * {@link #ACTIVE} 控制背挂绘制和动画门控；{@link #ATTACKING} 控制攻击动画，
 * {@link #PARRYING} 控制格挡动画。
 */
@Environment(EnvType.CLIENT)
public final class NiunaiXRenderData {

    /** 本帧是否渲染开胃奶背挂。 */
    public static final DataTicket<Boolean> ACTIVE =
            DataTicket.create("corpse_niunaix_active", Boolean.class);

    /** 玩家正在挥击，用于播放 {@code attack} 动画。 */
    public static final DataTicket<Boolean> ATTACKING =
            DataTicket.create("corpse_niunaix_attacking", Boolean.class);

    /** 玩家处于格挡窗口内，用于播放 {@code parry} 动画。 */
    public static final DataTicket<Boolean> PARRYING =
            DataTicket.create("corpse_niunaix_parrying", Boolean.class);

    /**
     * 当前时间基准，以实体年龄 tick 表示。
     * <p>
     * 动画控制器无法直接读取时钟，而 {@code attack} 动画比原版挥击动作更长。
     * 使用此值计算攻击开始后的时间，确保动画完整播放后再切换。详见
     * {@code ClientPlayerGeoAnimatableMixin#corpseorigin$niunai}
     */
    public static final DataTicket<Float> AGE_TICKS =
            DataTicket.create("corpse_niunaix_age_ticks", Float.class);

    private NiunaiXRenderData() {
    }

    /** 玩家当前是否为未伪装的开胃奶尸兄形态。 */
    public static boolean isNiunaiX(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        xiaoshi2022.corpseorigin.client.ClientCorpseData data = CorpseOriginClient.corpseDataCache.get(uuid);
        return data != null && data.isCorpse && !data.isDisguised()
                && data.getVariant() == PlayerCorpseComponent.VARIANT_NIUNAIX;
    }

    public static boolean isNiunaiX(AbstractClientPlayer player) {
        return player != null && isNiunaiX(player.getUUID());
    }
}
