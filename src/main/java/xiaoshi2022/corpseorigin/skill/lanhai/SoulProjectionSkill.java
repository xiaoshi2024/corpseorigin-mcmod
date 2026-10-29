package xiaoshi2022.corpseorigin.skill.lanhai;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 蓝海·灵魂出窍 —— <b>主动开关</b>技能。
 * <p>
 * 按一次：灵魂从胸口飘出，可以穿墙飞行；本体像睡着一样瘫在原地（躺姿 + 无法移动）。
 * 再按一次：灵魂钻回体内，视角交还。
 * <p>
 * 真正的状态推进在 {@link SoulProjectionHandler}（服务端），客户端表现
 * （半透明蓝色灵魂模型 / 出窍回窍动画 / 灵魂粒子）由摄像机与渲染器负责。
 * 冷却 0、不消耗内力：代价只落在"出窍期间本体不能动"上。
 */
public final class SoulProjectionSkill extends AbstractSkill {

    public static final String PATH = "lanhai_soul_projection";

    public SoulProjectionSkill() {
        super(PATH, SkillType.UTILITY, 0);
    }

    /** 按一下切换：出窍 / 回窍 */
    @Override
    public void onActivate(ServerPlayer player) {
        SoulProjectionHandler.toggle(player);
    }
}
