package xiaoshi2022.corpseorigin.skill.shichaozhizi;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 尸巢之子·千眼万目 —— 第二形态专属的凝视技。
 * <p>
 * 按下后整具身体播模型的 {@code special} 动画（3.2 秒），并在 {@link #DURATION} tick 内持续扫视：
 * <b>正看着你的生物与玩家会被麻木定住</b>（走不动、原地站桩、也放不出招式）。
 * 定身是"刷新式"的 —— 只要一直盯着你就会被一直定住；转头看别处，约半秒后自行解除。
 * <p>
 * 分工：
 * <ul>
 *   <li>动画表现：{@link CorpseNetwork#broadcastShiChaoSpecial} 发一条覆盖整段动画的窗口，
 *       客户端据此让尸巢之子身体改播 {@code special}；</li>
 *   <li>定身判定：{@link ThousandEyesHandler} 服务端每 tick 扫一遍（视线锥 + 视线不被挡）。</li>
 * </ul>
 */
public final class ThousandEyesSkill extends AbstractSkill {

    public static final String PATH = "thousand_eyes";

    /** 凝视窗口 = {@code special} 动画长度 3.2 秒（64 tick） */
    public static final int DURATION = 64;
    /** 生效半径（格），以施术者的眼睛为球心 */
    public static final double RANGE = 16.0;
    /** 视线判定阈值：对方视线方向 · "望向施术者"的方向；0.85 ≈ 32° 的锥 */
    public static final double GAZE_DOT = 0.85;
    /**
     * 定身的刷新间隔（tick）。
     * <p>
     * 每 tick 都重新施加会疯狂刷包（效果同步是逐次比较的），所以按这个间隔续一次缓慢；
     * 给的时长比间隔多几 tick，保证窗口内接得上、窗口结束又能很快失效。
     */
    public static final int STUN_REFRESH = 10;

    /** 冷却 30 秒 */
    private static final int COOLDOWN = 600;

    public ThousandEyesSkill() {
        super(PATH, SkillType.COMBAT, COOLDOWN);
    }

    /** 只有处于第二形态（巨大化）时才放得出来 —— 魔瞳长在那具身体上 */
    @Override
    public Component checkUsable(ServerPlayer player) {
        if (PlayerCorpseComponent.get(player).getVariant()
                != PlayerCorpseComponent.VARIANT_SHICHAOZHIZI) {
            return Component.translatable("skill.corpseorigin." + PATH + ".need_second_form");
        }
        return null;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        ThousandEyesHandler.start(player);
        CorpseNetwork.broadcastShiChaoSpecial(player, DURATION);
        player.sendOverlayMessage(Component.translatable("skill.corpseorigin." + PATH + ".on"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 1.0F, 0.5F);
    }
}
