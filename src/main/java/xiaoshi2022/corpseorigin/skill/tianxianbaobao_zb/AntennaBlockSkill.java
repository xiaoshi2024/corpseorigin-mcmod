package xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天线宝宝尸兄·天线格挡 —— <b>被动 + 主动并存</b>。
 * <ul>
 *   <li><b>被动（常驻）</b>：挨到斧头 / 箭矢·投掷物时，有
 *       {@value #PASSIVE_BLOCK_CHANCE_PERCENT}% 概率<b>把这一下整个挡掉</b>（是格挡，不是减伤）——
 *       学会就生效，不看冷却、不用按键；</li>
 *   <li><b>主动（手动触发）</b>：按下即生效，{@link #GUARD_DURATION} tick 内这两类伤害<b>必定</b>全部挡下，
 *       冷却 {@link #GUARD_COOLDOWN} tick。</li>
 * </ul>
 * 判定与伤害结算在 {@code xiaoshi2022.corpseorigin.event.TianXianBaoBaoEventHandler}；
 * 动画同步也由那边发起（{@link CorpseNetwork#broadcastAntennaBlock}）：
 * 动画文件里没有专门的格挡 clip，所以借用 {@code special_attack} 那条。
 */
public class AntennaBlockSkill extends AbstractSkill {

    public static final String PATH = "antenna_block";

    /**
     * 被动：挡下这一击的概率。
     * <p>
     * ⚠️ 注意挡的是<b>整次伤害</b>（不是减伤），等于凭空多一条命。原来 30% 太超模，
     * 砍到 15% —— 还是能明显感觉到"偶尔免伤"，但不再是对砍时的常态。
     */
    public static final double PASSIVE_BLOCK_CHANCE = 0.15;
    /** 上面概率的百分数写法（只用于文案） */
    public static final int PASSIVE_BLOCK_CHANCE_PERCENT = 15;

    /** 主动：按下后"必定格挡"的持续时间（2 秒）—— 原来是 3 秒，挡得又久又稳 */
    public static final int GUARD_DURATION = 40;
    /** 主动：冷却（20 秒）—— 原来是 12 秒，配合被动几乎半程无敌 */
    private static final int GUARD_COOLDOWN = 400;

    /**
     * 被动挡下时同步给客户端的动画时长。
     * <p>
     * 必须 ≥ {@code special_attack} 自己的 1.25 秒（25 tick），否则那条 clip 会被信号掐断在半路；
     * 给 30 tick 让它完整播完。
     */
    public static final int PASSIVE_ANIM_TICKS = 30;

    /**
     * 施术者 uuid → 主动格挡的结束时刻。
     * <p>
     * 存的是<b>世界的 gameTime</b>而不是 {@code player.tickCount}：玩家重生后 tickCount 会归零，
     * 用 tickCount 会留下一段时间的"假格挡"。
     */
    private static final Map<UUID, Long> GUARD_UNTIL = new ConcurrentHashMap<>();

    public AntennaBlockSkill() {
        // 用的是"主动技能"构造器（带冷却）→ 会进技能轮盘、可手动触发；
        // 被动那一半不依赖 isActivatable，所以两者互不干扰。
        super(PATH, SkillType.DEFENSE, GUARD_COOLDOWN);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        GUARD_UNTIL.put(player.getUUID(), player.level().getGameTime() + GUARD_DURATION);

        player.sendOverlayMessage(Component.translatable(msgKey("activate"), GUARD_DURATION / 20));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.7F);

        // 主动是"立刻生效"：动画也立刻开始同步，持续整个格挡窗口
        CorpseNetwork.broadcastAntennaBlock(player, GUARD_DURATION);
    }

    /**
     * 现在是否处于"主动格挡"窗口内（顺带清掉过期项，免得表越攒越大）。
     * <p>
     * 被动那一层不需要看它：被动是每次挨打自己掷骰子。
     */
    public static boolean isGuarding(ServerPlayer player) {
        Long until = GUARD_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() >= until) {
            GUARD_UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }

    private static String msgKey(String suffix) {
        return "skill.corpseorigin." + PATH + "." + suffix;
    }
}
