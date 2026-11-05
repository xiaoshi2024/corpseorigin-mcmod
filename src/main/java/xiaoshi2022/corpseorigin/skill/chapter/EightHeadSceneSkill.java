package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillResourceRules;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 八头长颈尸兄的「演出技能」—— 精神病院 BOSS（长颈尸兄的进化版：一个巨型长颈 + 顶上 8 个脑袋）。
 * <p>
 * 玩家扮演这个角色后渲染 eight_head_long_neck 模型，技能就是播动画：
 * <ul>
 *   <li>idle —— 收回演出，回到待机</li>
 *   <li>walk —— 正常行走（八个脑袋晃来晃去）</li>
 *   <li>attack —— 八颗脑袋齐咬</li>
 *   <li>stunned —— 被脑筋急转弯淦晕，八个脑袋各说各的</li>
 * </ul>
 * 和 LongNeckSceneSkill / MuDoctorSceneSkill 同款。
 */
public final class EightHeadSceneSkill extends AbstractSkill {
    private final String cue;

    public EightHeadSceneSkill(String cue, int cooldown) {
        super("eight_head_" + cue, SkillType.COMBAT, cooldown);
        this.cue = cue;
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin." + getId().getPath());
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin." + getId().getPath() + ".desc");
    }

    @Override
    public SkillResourceRules.Cost getResourceCost() {
        return new SkillResourceRules.Cost(0, 0);
    }

    @Override
    public Component checkUsable(ServerPlayer p) {
        if (!"eight_head_long_neck".equals(CharacterManager.getInstance().getPlayerCharacterId(p)))
            return Component.translatable("message.corpseorigin.new_chapter_skill.text_02");
        return null;
    }

    @Override
    public void onActivate(ServerPlayer p) {
        if ("idle".equals(cue)) {
            p.setAttached(ChapterScenes.ACTION, "");
            p.setAttached(ChapterScenes.UNTIL, 0L);
            return;
        }
        ChapterScenes.action(p, cue, switch (cue) {
            case "attack" -> 20;
            case "stunned" -> 120;
            default -> 40;
        });
    }
}
