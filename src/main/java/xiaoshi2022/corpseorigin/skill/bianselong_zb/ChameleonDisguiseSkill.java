package xiaoshi2022.corpseorigin.skill.bianselong_zb;

import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 变色龙尸兄·伪装——切换为小惠的外观模型潜入。
 * <p>
 * 主动技能，冷却 30 秒（600 ticks）。
 * <p>
 * 特效：外观模型切换。
 * <p>
 * 默认小惠；玩家皮肤由选择请求在服务端校验成功后替换。
 */
public class ChameleonDisguiseSkill extends AbstractSkill {

    public static final String PATH = "chameleon_disguise";

    public ChameleonDisguiseSkill() {
        super(PATH, SkillType.UTILITY, 600);
    }
    @Override public void onActivate(net.minecraft.server.level.ServerPlayer p) {
        p.removeAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.DISGUISE_PROFILE);
        p.setAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.DISGUISE,xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.XIAOHUI);
        p.setAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.DISGUISE_UNTIL,Long.MAX_VALUE);
    }
}
