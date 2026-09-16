package xiaoshi2022.corpseorigin.skill.xiaoyanzi;

import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockSource;

import java.util.List;

/**
 * 小言子·哈姆召唤——召唤哈姆喷吐火球。
 * <p>
 * 主动技能，冷却 10 秒（200 ticks）。
 * <p>
 * 特效：小火焰粒子。
 * <p>
 * TODO 实装：召唤喷火蜥蜴实体/临时伙伴 + 火球攻击。
 * <p>
 * 解锁：除了技能树花点，<b>获得哈姆宠物</b>也会直接学会。判定暂时恒为 false
 * （哈姆实体尚未实装），但条件文案已经会显示在技能界面上，方便玩家知道该去做什么。
 */
public class HamSummonSkill extends AbstractSkill {

    public static final String PATH = "ham_summon";

    /** 解锁本技能需要的宠物 id */
    public static final String PET_ID = "ham";

    public HamSummonSkill() {
        super(PATH, SkillType.COMBAT, 200);
    }

    @Override
    public List<SkillUnlockSource> getUnlockSources() {
        return List.of(SkillUnlockSource.custom(
                "pet:" + PET_ID,
                Component.translatable("pet.corpseorigin." + PET_ID),
                // TODO 哈姆实体实装后，替换成「玩家是否拥有哈姆宠物」的判定
                player -> false));
    }
}
