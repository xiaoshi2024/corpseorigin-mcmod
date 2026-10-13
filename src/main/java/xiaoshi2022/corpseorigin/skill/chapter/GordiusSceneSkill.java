package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillResourceRules;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 寄生铁血虫尸兄的「演出技能」——这个角色没有战斗力，技能就是把对应动画播出来，
 * 配合剧情镜头演出（如「龙右破棺吸干水中尸兄」）。
 * <ul>
 *   <li>gordius_show：show —— 铁线虫从鱼腹撑出（出现）</li>
 *   <li>gordius_swing：swing —— 鱼鳍摆动的游泳姿态</li>
 *   <li>gordius_wizened：wizened —— 被吸干时的干瘪挣扎</li>
 *   <li>gordius_idle：收回演出，回到蜷缩待机姿态</li>
 * </ul>
 * 动画 clip 与 {@code ChapterScenes.ACTION} 同名，由 {@code CreaturePlayerRenderer}
 * 按 ROLE=gordius_zbrz 的特殊映射播放。
 */
public final class GordiusSceneSkill extends AbstractSkill {
    /** 各 cue 的默认演出时长（tick）：show 4.89s 播完整段，其余按镜头需要可再续 */
    private static final int SHOW_TICKS = 100, WIZENED_TICKS = 60, SWING_TICKS = 40;
    private final String cue;

    public GordiusSceneSkill(String cue, int cooldown) {
        super("gordius_" + cue, SkillType.COMBAT, cooldown);
        this.cue = cue;
    }

    @Override public Component getName() { return Component.translatable("skill.corpseorigin." + getId().getPath()); }
    @Override public Component getDescription() { return Component.translatable("skill.corpseorigin." + getId().getPath() + ".desc"); }
    @Override public SkillResourceRules.Cost getResourceCost() { return new SkillResourceRules.Cost(0, 0); }

    @Override public Component checkUsable(ServerPlayer p) {
        if (!"gordius_zbrz".equals(CharacterManager.getInstance().getPlayerCharacterId(p)))
            return Component.translatable("message.corpseorigin.new_chapter_skill.text_02");
        return null;
    }

    @Override public void onActivate(ServerPlayer p) {
        if ("idle".equals(cue)) {
            p.setAttached(ChapterScenes.ACTION, "");
            p.setAttached(ChapterScenes.UNTIL, 0L);
            return;
        }
        ChapterScenes.action(p, cue, switch (cue) {
            case "show" -> SHOW_TICKS;
            case "wizened" -> WIZENED_TICKS;
            default -> SWING_TICKS;
        });
    }
}
