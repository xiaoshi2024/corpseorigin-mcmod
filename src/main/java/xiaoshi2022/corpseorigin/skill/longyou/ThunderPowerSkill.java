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
 * 龙右·雷电之力（基础）—— <b>主动开关</b>能力。
 * <p>
 * 能力来源：吸收「电鳗」尸兄 + H市发电厂的全部雷电之力。
 * <p>
 * 开启后徒手/近战命中附带额外雷伤与短暂麻痹（实装在 {@code LongYouEventHandler} 的
 * AFTER_DAMAGE 里，调用 {@link ThunderStrikeHandler#meleeZap}）；再按一次关闭。
 * 开关本身没有冷却、不消耗内力 —— 和「尸水之源」一样，代价只落在"开着的时候"那点强度上。
 * <p>
 * 它同时也是 {@code corpse_king_thunder}（雷鳗）与 {@code natural_judgment}（球状闪电）的前置：
 * 没学会「雷电之力」，那两个技能点不亮（学会即可，不要求开关处于开启状态）。
 */
public class ThunderPowerSkill extends AbstractSkill {

    public static final String PATH = "thunder_power";

    /** 已经开启雷电之力的玩家 */
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();

    public ThunderPowerSkill() {
        // 主动开关：冷却 0 + 不消耗内力
        super(PATH, SkillType.COMBAT, 0);
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
                nowOn ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.8F, nowOn ? 1.4F : 1.0F);
    }

    /** 这位玩家是不是开着雷电之力 */
    public static boolean isEnabled(UUID uuid) {
        return ENABLED.contains(uuid);
    }

    /** 玩家断开连接时清掉开关状态 */
    public static void clearOnDisconnect(UUID uuid) {
        ENABLED.remove(uuid);
    }

    private static String msgKey(String suffix) {
        return "skill.corpseorigin." + PATH + "." + suffix;
    }
}
