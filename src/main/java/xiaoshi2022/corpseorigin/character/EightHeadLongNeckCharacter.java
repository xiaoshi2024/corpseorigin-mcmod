package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.chapter.EightHeadSceneSkill;

import java.util.List;

/**
 * 八头长颈尸兄（{@code eight_head_long_neck}）—— 玩家扮演的精神病院 BOSS 角色。
 * <p>
 * 剧本桥段：精神病院第 2 关 BOSS，{@link LongNeckCharacter} 的进化版 ——
 * 一个巨型长颈 + 顶上 8 个脑袋，每颗脑袋都能独立说话。穆博士通过广播器操控它，
 * 主角用脑筋急转弯让 8 个脑袋答出 8 个不同答案，陷入混乱眩晕后集火。
 * <p>
 * 与 {@link LongNeckCharacter} 同款演出角色模式：
 * <ul>
 *   <li>模型资源已就绪（eight_head_long_neck.geo.json / animation / png）</li>
 *   <li>技能全是「播动画」的演出技能（walk/attack/stunned）</li>
 *   <li>本体是尸兄 —— 进 reconcileCorpseState 的尸兄名单，会被自动标记 TYPE_NORMAL</li>
 * </ul>
 */
public final class EightHeadLongNeckCharacter implements ICharacter {
    public static final String ID = "eight_head_long_neck";

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
                new EightHeadSceneSkill("idle", 10),
                new EightHeadSceneSkill("walk", 40),
                new EightHeadSceneSkill("attack", 30),
                new EightHeadSceneSkill("stunned", 60)
        );
    }
}
