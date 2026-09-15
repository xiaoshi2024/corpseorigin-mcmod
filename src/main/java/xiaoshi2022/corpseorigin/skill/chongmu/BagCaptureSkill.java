package xiaoshi2022.corpseorigin.skill.chongmu;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 虫母·袋中捕获
 * <p>
 * 设定效果：以布袋捕获目标，禁锢并施加毒伤（剧情禁锢毒伤）。
 * 冷却：20 秒。特效：禁铜布袋道具。
 * <p>
 * TODO 实装：近身捕获目标 → 禁锢状态 + 持续毒伤 + 布袋道具表现。
 */
public class BagCaptureSkill extends AbstractSkill {

    public static final String PATH = "bag_capture";

    public BagCaptureSkill() {
        super(PATH, SkillType.COMBAT, 400);   // 20s
    }
}
