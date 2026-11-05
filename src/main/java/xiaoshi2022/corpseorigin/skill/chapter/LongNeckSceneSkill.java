package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillResourceRules;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 长颈尸兄的「演出技能」—— 精神病院一楼那只长颈尸兄，穆博士用精神病病人改造的控制尸兄。
 * <p>
 * 玩家扮演这个角色后，颈部模型会被拉长（建模待补），配合穆博士的剧情镜头：
 * <ul>
 *   <li>idle —— 收回演出，回到待机</li>
 *   <li>walk —— 正常行走（脖子长所以头会摇）</li>
 *   <li>approach —— 伸长脖子凑近（"凑近点看清你"）</li>
 *   <li>lunge —— 脖子弹出去咬人</li>
 *   <li>growl —— 低沉咆哮（穆博士精神控制时的低吼）</li>
 * </ul>
 * 和 UncleSceneSkill / MuDoctorSceneSkill 同款，技能就是播动画。
 */
public final class LongNeckSceneSkill extends AbstractSkill {
    private final String cue;

    public LongNeckSceneSkill(String cue, int cooldown) {
        super("long_neck_" + cue, SkillType.COMBAT, cooldown);
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
        if (!"long_neck_zb".equals(CharacterManager.getInstance().getPlayerCharacterId(p)))
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
            case "approach" -> 40;
            case "lunge" -> 20;
            case "growl" -> 50;
            default -> 40;
        });
    }
}
