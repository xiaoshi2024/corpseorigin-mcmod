package xiaoshi2022.corpseorigin.skill.heixiaofei;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 黑小飞·黑金心脏（被动）
 * <p>
 * 锁血：受到致命伤害时保留 {@value #LOCK_HEALTH} 点生命，并震退周围敌人。
 * 被动逻辑本体在 {@link xiaoshi2022.corpseorigin.event.HeiXiaoFeiEventHandler}，
 * 本类只负责技能树中的注册信息与数值常量。
 */
public class BlackGoldHeartSkill implements ISkill {

    public static final String PATH = "black_gold_heart";

    /** 锁血后保留的生命值 */
    public static final float LOCK_HEALTH = 1.0f;
    /** 锁血冷却（60 秒） */
    public static final int LOCK_COOLDOWN_TICKS = 1200;
    /** 锁血时震退敌人的半径 */
    public static final double KNOCKBACK_RADIUS = 6.0;

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, PATH);
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin." + PATH);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin." + PATH + ".desc");
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.DEFENSE;
    }

    @Override
    public int getCost() {
        return 2;
    }

    @Override
    public int getRequiredLevel() {
        return 1;
    }

    /** 被动技能，不进技能轮盘 */
    @Override
    public boolean isActivatable() {
        return false;
    }

    @Override
    public int getCooldownTicks() {
        return LOCK_COOLDOWN_TICKS;
    }
}
