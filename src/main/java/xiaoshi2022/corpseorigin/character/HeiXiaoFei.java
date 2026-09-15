package xiaoshi2022.corpseorigin.character;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.heixiaofei.BlackGoldHeartSkill;
import xiaoshi2022.corpseorigin.skill.heixiaofei.DarkSiphonSkill;
import xiaoshi2022.corpseorigin.skill.heixiaofei.HoundUnleashedSkill;
import xiaoshi2022.corpseorigin.skill.heixiaofei.KillingIncarnationSkill;
import xiaoshi2022.corpseorigin.skill.heixiaofei.RoundDanceSkill;
import xiaoshi2022.corpseorigin.skill.heixiaofei.SeveredArmStrikeSkill;
import xiaoshi2022.corpseorigin.skill.heixiaofei.SlaughterMomentumSkill;
import xiaoshi2022.corpseorigin.skill.heixiaofei.TigerClawBeeWheelSkill;

import java.util.List;

/**
 * 黑小飞 - 白小飞的克隆体，《尸巢之战篇》本篇主角。
 * <p>
 * 8 项核心技能：杀势、黑暗虹吸、黑金心脏、断臂攻击、虎爪/飞蜂轮、
 * 恶犬出笼、圆舞、杀戮化形·身外化身。
 */
public class HeiXiaoFei implements ICharacter {

    public static final String ID = "heixiaofei";

    private static final List<ISkill> SKILLS = List.of(
            new SlaughterMomentumSkill(),
            new DarkSiphonSkill(),
            new BlackGoldHeartSkill(),
            new SeveredArmStrikeSkill(),
            new TigerClawBeeWheelSkill(),
            new HoundUnleashedSkill(),
            new RoundDanceSkill(),
            new KillingIncarnationSkill()
    );

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.heixiaofei");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.heixiaofei.desc");
    }

    @Override
    public Identifier getIcon() {
        return ICharacter.iconId("heixiaofei");
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        return List.of(
                Component.translatable("character.corpseorigin.heixiaofei.trait1"),
                Component.translatable("character.corpseorigin.heixiaofei.trait2"),
                Component.translatable("character.corpseorigin.heixiaofei.trait3")
        );
    }

    @Override
    public List<ISkill> getSkills() {
        return SKILLS;
    }

    @Override
    public float getInfectionMultiplier() {
        return 0.4f;
    }
}
