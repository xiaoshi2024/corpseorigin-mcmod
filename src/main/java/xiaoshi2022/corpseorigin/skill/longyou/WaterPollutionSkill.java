package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 龙右·尸水之源 —— <b>主动开关</b>技能。
 * <p>
 * 按下开启后，走过的水源会被污染成尸水；再按一次关闭。
 * 代价是<b>持续消耗饱食度</b>（{@link #HUNGER_EXHAUSTION} / 秒），
 * 所以想常驻污染一片水域就得一直吃 —— 饿到见底会自动中断。
 * <p>
 * 无冷却、不消耗内力：开关本身没有代价，代价只在"开着"的这段时间里。
 * <p>
 * 实装见 {@code LongYouEventHandler}：服务端每几 tick 扫一圈玩家脚边，
 * 把普通<b>水源方块</b>换成尸水源；之后尸水会像水一样自己往低处流，
 * 接触到的玩家/村民照常中毒、被感染（那部分逻辑在 {@code InfectedWaterFluid} 里）。
 */
public class WaterPollutionSkill extends AbstractSkill {

    public static final String PATH = "water_pollution";

    /** 扫描半径（格，水平方向） */
    public static final int RADIUS = 2;
    /** 脚下一圈的高度：往下 1 格到往上 1 格，游在水里时也能覆盖到身体周围 */
    public static final int VERTICAL_RADIUS = 1;
    /** 单次扫描最多污染几个源块（一头扎进水塘时别把整片水域一口气换掉） */
    public static final int MAX_PER_SCAN = 6;

    /** 开启中每多少 tick 扣一次饱食度（1 秒一次） */
    public static final int HUNGER_INTERVAL = 20;
    /** 每次扣掉的饥饿消耗量：约 8 秒掉 1 点饥饿 */
    public static final float HUNGER_EXHAUSTION = 0.5F;

    /** 已经开启尸水之源的玩家 */
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    public WaterPollutionSkill() {
        // 主动技能 + 冷却 0 + 不消耗内力：开关不该有冷却，代价是饱食度
        super(PATH, SkillType.UTILITY, 0);
    }

    /** 按一下切换开关状态 */
    @Override
    public void onActivate(ServerPlayer player) {
        boolean nowOn = !ENABLED.remove(player.getUUID());
        if (nowOn) {
            ENABLED.add(player.getUUID());
        }

        player.sendOverlayMessage(Component.translatable(msgKey(nowOn ? "on" : "off")));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                nowOn ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL,
                SoundSource.PLAYERS, 0.8F, nowOn ? 0.6F : 1.4F);
    }

    /** 这位玩家是不是开着尸水之源 */
    public static boolean isEnabled(UUID uuid) {
        return ENABLED.contains(uuid);
    }

    /** 关掉（饱食度耗尽时由事件处理器调用） */
    public static void disable(ServerPlayer player) {
        if (ENABLED.remove(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable(msgKey("starving")));
        }
    }

    /** 玩家断开连接时清掉开关状态 —— 免得重登之后不明不白地掉饱食度 */
    public static void clearOnDisconnect(UUID uuid) {
        ENABLED.remove(uuid);
    }

    private static String msgKey(String suffix) {
        return "skill.corpseorigin." + PATH + "." + suffix;
    }
}
