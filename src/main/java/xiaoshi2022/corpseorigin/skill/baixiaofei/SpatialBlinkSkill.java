package xiaoshi2022.corpseorigin.skill.baixiaofei;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 白小飞·空间异能（短距瞬移）
 * <p>
 * 设定效果：闪现 8–16 格，落点产生空间涟漪。
 * 冷却：15 秒。特效：紫/黑色粒子，瞬移音效。
 * <p>
 * TODO 实装：视线落点检测 → 距离上限 8–16 格 → 瞬移 + 落点空间涟漪粒子与音效。
 */
public class SpatialBlinkSkill extends AbstractSkill {

    public static final String PATH = "spatial_blink";

    public SpatialBlinkSkill() {
        super(PATH, SkillType.UTILITY, 300);   // 15s
    }
}
