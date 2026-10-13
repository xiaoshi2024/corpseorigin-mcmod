package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillResourceRules;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 大叔（少女漫画家）的「演出技能」——与寄生铁血虫尸兄同款：角色没有战斗力，
 * 技能就是把 {@code uncles} 模型的对应动画播出来配合剧情演出。
 * <p>
 * cue 与动画 clip 同名（walk / attack / flee / open / drop），
 * idle 用于收回演出回到待机；时长取自 {@code UncleEntity} 各动画的帧长。
 */
public final class UncleSceneSkill extends AbstractSkill {
    private final String cue;

    public UncleSceneSkill(String cue, int cooldown) {
        super("uncle_" + cue, SkillType.COMBAT, cooldown);
        this.cue = cue;
    }

    @Override public Component getName() { return Component.translatable("skill.corpseorigin." + getId().getPath()); }
    @Override public Component getDescription() { return Component.translatable("skill.corpseorigin." + getId().getPath() + ".desc"); }
    @Override public SkillResourceRules.Cost getResourceCost() { return new SkillResourceRules.Cost(0, 0); }

    @Override public Component checkUsable(ServerPlayer p) {
        if (!"uncles".equals(CharacterManager.getInstance().getPlayerCharacterId(p)))
            return Component.translatable("message.corpseorigin.new_chapter_skill.text_02");
        return null;
    }

    @Override public void onActivate(ServerPlayer p) {
        if ("idle".equals(cue)) {
            p.setAttached(ChapterScenes.ACTION, "");
            p.setAttached(ChapterScenes.UNTIL, 0L);
            return;
        }
        // 时长与 UncleEntity 的动画时长对齐：open 58 / flee 20 / drop 40 / walk 40 / attack 15
        ChapterScenes.action(p, cue, switch (cue) {
            case "open" -> 58;
            case "flee" -> 20;
            case "drop" -> 40;
            case "walk" -> 40;
            default -> 15;
        });
    }
}
