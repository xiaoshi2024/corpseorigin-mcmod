package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.lanhai.SoulProjectionSkill;

import java.util.List;

/**
 * 蓝海 —— 炎黄特能队第四小队成员，与尔多并称「海·尔兄弟」。
 * <p>
 * 特异功能是<b>睡觉时灵魂出窍</b>：灵魂能离开身体四处游荡，甚至穿墙飞行；
 * 本体则像昏迷一样瘫在原地。实装技能见 {@link SoulProjectionSkill}。
 * <p>
 * 没有专属 GEO 模型，外观直接走玩家皮肤（和威信等人类角色一样）；
 * 也不进 {@code reconcileCorpseState} 的尸兄名单 —— 他是人类。
 */
public final class LanHai implements ICharacter {

    public static final String ID = "lanhai";

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
        return ICharacter.iconId(ID);
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(Component.translatable("character.corpseorigin.form.human"), getDescription());
    }

    @Override
    public List<ISkill> getSkills() {
        return List.of(new SoulProjectionSkill());
    }
}
