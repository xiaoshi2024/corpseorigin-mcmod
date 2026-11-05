package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillResourceRules;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 穆博士的「演出技能」——与大叔、铁血虫同款：这个角色的模型资源
 * （mu_doctor.geo.json）同时被 BOSS 实体和扮演玩家共用，技能就是
 * 把对应动画 clip 播出来配合剧情镜头。
 * <p>
 * cue 与动画 clip 同名：
 * <ul>
 *   <li>idle —— 收回演出，回到待机</li>
 *   <li>walk —— 正常行走</li>
 *   <li>run —— 逃跑/紧张时的奔跑</li>
 *   <li>think —— 抬手思考/说话的姿态</li>
 *   <li>press —— 给自己扎针（二阶段进化）</li>
 *   <li>speech —— 对着空气叫嚣/演讲</li>
 *   <li>grab —— 抓取（二阶段抛投玩家）</li>
 * </ul>
 */
public final class MuDoctorSceneSkill extends AbstractSkill {
    private final String cue;

    public MuDoctorSceneSkill(String cue, int cooldown) {
        super("mu_doctor_" + cue, SkillType.COMBAT, cooldown);
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
        if (!"mu_doctor".equals(CharacterManager.getInstance().getPlayerCharacterId(p)))
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
        // 时长按剧情镜头需要定：think/speech/press 较长，walk/run/grab 较短
        ChapterScenes.action(p, cue, switch (cue) {
            case "think" -> 40;
            case "press" -> 45;   // 和 MuDoctorEntity.TRANSITION_TICKS 对齐
            case "speech" -> 80;  // 叫嚣/演讲需要更长
            case "grab" -> 20;
            case "run" -> 30;
            default -> 40;       // walk
        });
    }
}
