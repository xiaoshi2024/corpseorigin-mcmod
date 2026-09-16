package xiaoshi2022.corpseorigin.limb;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.HeiXiaoFei;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.skill.heixiaofei.DarkSiphonSkill;

/**
 * 黑小飞·吸血鬼血脉：吸收气血供能，所以再生<b>不消化饱食度</b>、而且更快。
 * <p>
 * 开关是"已经在技能树里学会黑暗虹吸（{@link DarkSiphonSkill}）"——
 * 技能不会随角色自动授予，所以这条策略在解锁之前不会生效，属于后期内容。
 * <p>
 * 想改成"必须刚吸过血才加速"（更贴合"吸收气血"），覆写 {@link #appliesTo} 加上你自己的
 * 时间窗判断即可；想改倍率改 {@link #SPEED}。
 */
public final class VampireRegenProfile implements LimbRegenProfile {

    public static final String ID = "corpseorigin:vampire";

    /** 再生速度倍率：精英 60s → 20s */
    public static final float SPEED = 3.0F;
    /** 再快也不低于 1 秒，免得动画来不及播 */
    public static final int MIN_TICKS = 20;

    @Override
    public String id() {
        return ID;
    }

    /** 优先于普通尸兄策略 */
    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean appliesTo(ServerPlayer player) {
        if (!HeiXiaoFei.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            return false;
        }
        return PlayerCharacterData.get(player)
                .hasLearned(player.getUUID(), DarkSiphonSkill.PATH);
    }

    @Override
    public int regrowTicks(ServerPlayer player, PlayerCorpseComponent comp, int slot) {
        int base = CorpseRegenProfile.baseTicks(comp);
        if (base <= 0) {
            return base;   // 本来就不能自愈的照旧
        }
        return Math.max(MIN_TICKS, (int) (base / SPEED));
    }

    /** 靠气血供能，不吃饱食度 */
    @Override
    public boolean consumesHunger(ServerPlayer player) {
        return false;
    }

    /** 不消耗饱食度，自然也不受饥饿限制 */
    @Override
    public boolean hungerGate(ServerPlayer player, PlayerCorpseComponent comp) {
        return true;
    }
}
