package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.chapter.GordiusSceneSkill;

import java.util.List;

/**
 * 寄生铁血虫尸兄（{@code gordius_zbrz}）—— 肚子里藏着铁线虫的尸兄鱼。
 * <p>
 * 扮演角色：切到这个角色后玩家渲染成 gordius_zbrz 全身模型（鱼体 + 藏在鱼腹里的铁线虫），
 * 技能全是「播动画」的演出技能，用于配合剧情镜头
 * （MC市水源河道下游古棺剧情：虚弱的龙右破棺而出，抓住水中尸兄吸干气血）。
 * 被尸王（龙右）吸干的表现走 wizened 演出技能 / 实体侧 {@code beginDrain()}。
 */
public final class GordiusZbrzCharacter implements ICharacter {
    public static final String ID = "gordius_zbrz";

    @Override public String getId() { return ID; }
    @Override public Component getName() { return Component.translatable("character.corpseorigin." + ID); }
    @Override public Component getDescription() { return Component.translatable("character.corpseorigin." + ID + ".desc"); }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("minecraft", "textures/item/book.png"); }
    @Override public boolean isPassive() { return false; }
    @Override public List<Component> getTraits() {
        return List.of(Component.translatable("character.corpseorigin.form.corpse"), getDescription());
    }
    @Override public List<ISkill> getSkills() {
        return List.of(new GordiusSceneSkill("show", 60),
                new GordiusSceneSkill("swing", 60),
                new GordiusSceneSkill("wizened", 60),
                new GordiusSceneSkill("idle", 20));
    }
}
