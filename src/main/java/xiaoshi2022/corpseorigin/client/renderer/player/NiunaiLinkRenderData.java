package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.dataticket.DataTicket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import xiaoshi2022.corpseorigin.character.KaiWeiNai;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;

import java.util.UUID;

/**
 * 开胃奶「拦腰斩断」（{@code niunai_link_player}）的渲染数据 / 判定。
 * <p>
 * 窗口是服务端状态（受到致命一击时由 {@code KaiWeiNaiEventHandler} 开启），随
 * {@code NiunaiLinkSyncS2C} 广播到所有人，客户端从 {@link CorpseOriginClient#niunaiLinks} 读
 * —— 所以"自己"和"别人看你"看到的是同一套腰斩效果，不需要额外网络包。
 * <p>
 * 三个 ticket 都是每帧重建的临时数据，只在渲染管线里传：
 * {@link #ACTIVE} 是"这一帧玩家在不在腰斩窗口里"（决定画不画、也决定动画控制器归不归它管），
 * {@link #LINKING} 决定控制器播断开的 {@code broken_off} 还是接回的 {@code link}。
 * <p>
 * {@link #BODY_PASS} 是给 GeckoLib 架构擦屁股用的：<b>两套模型共用玩家这一份控制器</b>，
 * 而一帧内只有<b>先求值</b>的那个模型能决定动画名怎么解析（见
 * {@code AnimationController#checkControllerState} —— 动画没变时它直接推进旧时间轴，
 * 不会为新模型重新解析），所以必须把两次求值标成"背挂那趟"和"身体那趟"，
 * 让每条控制器只在属于自己模型的那趟里解析动画名。
 */
@Environment(EnvType.CLIENT)
public final class NiunaiLinkRenderData {

    /** 这一帧渲染的是「拦腰斩断」形态 —— 决定画不画，也决定动画控制器归不归它管 */
    public static final DataTicket<Boolean> ACTIVE =
            DataTicket.create("corpse_niunai_link_active", Boolean.class);

    /** 已经进入接回阶段（断开 + 保持都走完了）—— 驱动 {@code link} 而不是 {@code broken_off} */
    public static final DataTicket<Boolean> LINKING =
            DataTicket.create("corpse_niunai_link_linking", Boolean.class);

    /**
     * 这趟求值是不是"腰斩身体"那趟（模型 {@code niunai_link_player}）。
     * <p>
     * {@code false} = 正在为背后的菊花盾（模型 {@code niunaix}）求值，{@code broken_off} / {@code link}
     * 在这趟里不该解析；{@code true} = 为身体求值，背挂那套动画名在这趟里不该解析。
     */
    public static final DataTicket<Boolean> BODY_PASS =
            DataTicket.create("corpse_niunai_link_body_pass", Boolean.class);

    private NiunaiLinkRenderData() {
    }

    /** 这位玩家现在是不是处于拦腰斩断窗口内 */
    public static boolean isNiunaiLink(UUID uuid) {
        return uuid != null && CorpseOriginClient.isNiunaiLink(uuid);
    }

    public static boolean isNiunaiLink(AbstractClientPlayer player) {
        return player != null && isNiunaiLink(player.getUUID());
    }

    /** 这一帧该播接回动画了吗（只剩最后 {@code link} 那一段的时长） */
    public static boolean isLinking(UUID uuid) {
        int remaining = uuid == null ? -1 : CorpseOriginClient.niunaiLinkRemaining(uuid);
        return remaining >= 0 && remaining <= KaiWeiNai.NIUNAI_LINK_RESTORE_TICKS;
    }
}
