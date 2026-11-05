package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.chapter.MuDoctorSceneSkill;

import java.util.List;

/**
 * 穆博士（{@code mu_doctor}）—— 可扮演的人类反派剧情角色。
 * <p>
 * 玩家切到这个角色后渲染成 mu_doctor 全身模型（与 BOSS 实体共用 geo 资源），
 * 技能全是「播动画」的演出技能，用于配合精神病院剧情（抓捕晓佳/小惠、
 * 叫嚣建立新国度、扎针进化等桥段）。
 * <p>
 * 注意：穆博士是人类反派，本体不是尸兄，所以不进
 * {@code ChapterActorState.reconcileCorpseState} 的尸兄名单。
 */
public final class MuDoctorCharacter implements ICharacter {
    /** 与模型资源同名：geckolib/models/entity/mu_doctor.geo.json */
    public static final String ID = "mu_doctor";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin." + ID);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin." + ID + ".desc");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/item/book.png");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.form.human"),
                getDescription()
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return List.of(
                new MuDoctorSceneSkill("idle", 10),
                new MuDoctorSceneSkill("walk", 40),
                new MuDoctorSceneSkill("run", 40),
                new MuDoctorSceneSkill("think", 30),
                new MuDoctorSceneSkill("press", 60),
                new MuDoctorSceneSkill("speech", 80),
                new MuDoctorSceneSkill("grab", 30)
        );
    }
}
