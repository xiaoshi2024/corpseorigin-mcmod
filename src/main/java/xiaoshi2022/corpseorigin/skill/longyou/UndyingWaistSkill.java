package xiaoshi2022.corpseorigin.skill.longyou;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 龙右·不死腰体（再生）
 * <p>
 * 设定效果：每秒回血，空血后进入「爆头复活」阶段一次。
 * 被动，无冷却。特效：断口血肉/黑气重组粒子。
 * <p>
 * TODO 实装：每秒再生 buff → 空血时触发一次爆头复活 → 复活演出与黑气粒子。
 */
public class UndyingWaistSkill extends AbstractSkill {

    public static final String PATH = "undying_waist";

    public UndyingWaistSkill() {
        super(PATH, SkillType.DEFENSE);   // 被动
    }
}
