package xiaoshi2022.corpseorigin.skill.kaiweinai;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.KaiWeiNai;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockSource;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开胃奶·菊花盾·开 —— 展开正面无敌盾，持续期间不能攻击。
 * <p>
 * 主动技能，冷却 20 秒（400 ticks），持续 5 秒。
 * <p>
 * 表现：背后那套 {@code niunaix} 背挂（触角 / 捆仙索 / 菊花盾）的花瓣张开成盾 ——
 * 服务端广播一条覆盖整个持续时间的窗口（{@link CorpseNetwork#broadcastNiunaiParry}），
 * 客户端据此播 {@code parry} 动画。
 * <p>
 * 判定：窗口内<b>正面</b>来的伤害整下挡掉，箭矢 / 弩矢 / 三叉戟会被原样弹回去 ——
 * 全在服务端 {@link xiaoshi2022.corpseorigin.event.KaiWeiNaiEventHandler}。
 * <p>
 * 解锁：拿到「开胃奶」这个角色就免费学会（走获取式解锁那条通道，见 {@link #getUnlockSources}）。
 */
public class ChrysanthemumShieldSkill extends AbstractSkill {

    public static final String PATH = "chrysanthemum_shield";

    /** 盾的持续时间（tick）：5 秒 */
    public static final int SHIELD_DURATION = 100;
    /** 冷却（20 秒） */
    private static final int SHIELD_COOLDOWN = 400;

    /**
     * 施术者 uuid → 盾的结束时刻。
     * <p>
     * 同天线格挡：存的是<b>世界的 gameTime</b>而不是 {@code player.tickCount} ——
     * 玩家重生后 tickCount 会归零，用 tickCount 会留下一段时间的"假盾"。
     */
    private static final Map<UUID, Long> SHIELD_UNTIL = new ConcurrentHashMap<>();

    public ChrysanthemumShieldSkill() {
        super(PATH, SkillType.DEFENSE, SHIELD_COOLDOWN);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        SHIELD_UNTIL.put(player.getUUID(), player.level().getGameTime() + SHIELD_DURATION);

        player.sendOverlayMessage(Component.translatable(msgKey("activate"), SHIELD_DURATION / 20));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.7F);

        // 表现：背后背挂张开成盾，窗口覆盖整个持续时间
        CorpseNetwork.broadcastNiunaiParry(player, SHIELD_DURATION);
    }

    /**
     * 现在是否处于菊花盾窗口内（顺带清掉过期项，免得表越攒越大）。
     */
    public static boolean isShielding(ServerPlayer player) {
        Long until = SHIELD_UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() >= until) {
            SHIELD_UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }

    /**
     * 开胃奶角色的专属技能 —— 拿到角色就免费学会。
     * <p>
     * 和技能树那条路并存：这里只是让"选中开胃奶"这个动作本身就把盾交到玩家手里，
     * 否则骨架阶段没有别的学习入口（角色切换并不会自动授予技能，见 {@code CharacterManager}）。
     */
    @Override
    public List<SkillUnlockSource> getUnlockSources() {
        return List.of(SkillUnlockSource.custom(
                "character:" + KaiWeiNai.ID,
                Component.translatable("character.corpseorigin." + KaiWeiNai.ID),
                player -> KaiWeiNai.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))));
    }

    private static String msgKey(String suffix) {
        return "skill.corpseorigin." + PATH + "." + suffix;
    }
}
