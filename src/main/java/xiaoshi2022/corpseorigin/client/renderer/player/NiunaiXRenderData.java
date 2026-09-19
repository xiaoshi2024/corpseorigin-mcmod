package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.dataticket.DataTicket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

import java.util.UUID;

/**
 * 开胃奶「背挂」（{@code niunaix}）的渲染数据 / 判定。
 * <p>
 * 形态本身是服务端状态（{@code PLAYER_CORPSE} 附件里的变种号），随 {@code PlayerCorpseSyncS2C}
 * 广播到所有人，客户端从 {@link CorpseOriginClient#corpseDataCache} 读 —— 所以"自己"和
 * "别人看你"看到的是同一套背挂，不需要额外网络包。
 * <p>
 * 三个 ticket 都是每帧重建的临时数据，只在渲染管线里传：
 * {@link #ACTIVE} 同时充当"这一帧要不要画背挂"的开关与动画控制器的门控（不是开胃奶就整条停掉，
 * 免得 {@code idle}/{@code attack} 这些重名动画泄漏到别的模型上刷日志）；
 * {@link #ATTACKING} 驱动尖刺，{@link #PARRYING} 驱动菊花盾格挡。
 */
@Environment(EnvType.CLIENT)
public final class NiunaiXRenderData {

    /** 这一帧渲染的是开胃奶背挂 —— 决定画不画，也决定动画控制器归不归它管 */
    public static final DataTicket<Boolean> ACTIVE =
            DataTicket.create("corpse_niunaix_active", Boolean.class);

    /** 玩家正在挥击 —— 驱动 {@code attack}（尖刺） */
    public static final DataTicket<Boolean> ATTACKING =
            DataTicket.create("corpse_niunaix_attacking", Boolean.class);

    /** 菊花盾格挡窗口内 —— 驱动 {@code parry}（花瓣张开成盾） */
    public static final DataTicket<Boolean> PARRYING =
            DataTicket.create("corpse_niunaix_parrying", Boolean.class);

    /**
     * 当前时间基准（实体年龄，tick）。
     * <p>
     * 控制器本身拿不到时钟，而 {@code attack} 那条 clip（1 秒）比原版一下挥击（约 6 tick）长得多，
     * 需要"播满再切" —— 靠它算出"离这次挥击开始过了多久"。见
     * {@code ClientPlayerGeoAnimatableMixin#corpseorigin$niunai}。
     */
    public static final DataTicket<Float> AGE_TICKS =
            DataTicket.create("corpse_niunaix_age_ticks", Float.class);

    private NiunaiXRenderData() {
    }

    /** 这位玩家现在是不是开胃奶背挂形态（尸兄 + 非伪装 + 变种 4） */
    public static boolean isNiunaiX(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        CorpseOriginClient.ClientCorpseData data = CorpseOriginClient.corpseDataCache.get(uuid);
        return data != null && data.isCorpse && !data.isDisguised()
                && data.getVariant() == PlayerCorpseComponent.VARIANT_NIUNAIX;
    }

    public static boolean isNiunaiX(AbstractClientPlayer player) {
        return player != null && isNiunaiX(player.getUUID());
    }
}
