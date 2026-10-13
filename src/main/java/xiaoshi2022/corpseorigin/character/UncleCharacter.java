package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.chapter.UncleSceneSkill;

import java.util.List;

/**
 * 大叔（少女漫画家，{@code uncles}）—— 可扮演的人类剧情角色。
 * <p>
 * 玩家切到这个角色后渲染成 uncles 全身模型，技能全是「播动画」的演出技能
 * （walk / attack / flee / open / drop / idle），用于配合剧情镜头演出
 * （如企鹅尸兄袭击大叔、尸体被 CoCo 吞下合体进化等桥段）。
 * 注意：大叔是普通人类，不进 {@code reconcileCorpseState} 的尸兄名单。
 */
public final class UncleCharacter implements ICharacter {
    /** 与模型资源同名：geckolib/models/entity/uncles.geo.json */
    public static final String ID = "uncles";

    @Override public String getId() { return ID; }
    @Override public Component getName() { return Component.translatable("character.corpseorigin." + ID); }
    @Override public Component getDescription() { return Component.translatable("character.corpseorigin." + ID + ".desc"); }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("minecraft", "textures/item/book.png"); }
    @Override public boolean isPassive() { return false; }
    @Override public List<Component> getTraits() {
        return List.of(Component.translatable("character.corpseorigin.form.human"), getDescription());
    }
    @Override public List<ISkill> getSkills() {
        return List.of(new UncleSceneSkill("walk", 40),
                new UncleSceneSkill("attack", 40),
                new UncleSceneSkill("flee", 40),
                new UncleSceneSkill("open", 60),
                new UncleSceneSkill("drop", 60),
                new UncleSceneSkill("idle", 20));
    }
}
