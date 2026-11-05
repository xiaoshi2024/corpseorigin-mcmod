package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.chapter.LongNeckSceneSkill;

import java.util.List;

/**
 * 长颈尸兄（{@code long_neck_zb}）—— 玩家扮演的精神病院剧情角色。
 * <p>
 * 剧本桥段：穆博士用精神病病人做实验，把他们改造成脖子特别长的控制尸兄，
 * 守在精神病院一楼楼梯口。小噬拿两把消防斧把它干掉。
 * <p>
 * 与 {@link UncleCharacter} / {@link MuDoctorCharacter} 同款演出角色模式：
 * <ul>
 *   <li>模型资源待建模（long_neck_zb.geo.json）</li>
 *   <li>技能全是「播动画」的演出技能（walk/approach/lunge/growl）</li>
 *   <li>本体是尸兄 —— 进 reconcileCorpseState 的尸兄名单，会被自动标记 TYPE_NORMAL</li>
 *   <li>穆博士的"精神控制"效果由 ChapterActorState 的受控标志（模型层面）驱动</li>
 * </ul>
 */
public final class LongNeckCharacter implements ICharacter {
    public static final String ID = "long_neck_zb";

    @Override public String getId() { return ID; }

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

    @Override public boolean isPassive() { return false; }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.form.corpse"),
                getDescription()
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return List.of(
                new LongNeckSceneSkill("idle", 10),
                new LongNeckSceneSkill("walk", 40),
                new LongNeckSceneSkill("approach", 40),
                new LongNeckSceneSkill("lunge", 30),
                new LongNeckSceneSkill("growl", 60)
        );
    }
}
