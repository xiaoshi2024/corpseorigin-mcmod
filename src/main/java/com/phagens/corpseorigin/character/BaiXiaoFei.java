package com.phagens.corpseorigin.character;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.skill.BaseSkill;
import com.phagens.corpseorigin.skill.ISkill;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

public class BaiXiaoFei implements ICharacter {

    public static final String ID = "baixiaofei";

    private final List<ISkill> skills = new ArrayList<>();

    public BaiXiaoFei() {
        skills.add(new BaiXiaoFeiSkills.HotBloodSkill());
        skills.add(new BaiXiaoFeiSkills.FightingTechniqueSkill());
        skills.add(new BaiXiaoFeiSkills.SurvivalInstinctSkill());
        skills.add(new BaiXiaoFeiSkills.FlyingKickSkill());
        skills.add(new BaiXiaoFeiSkills.SwordMasterySkill());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.baixiaofei");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.baixiaofei.desc");
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/character/baixiaofei.png");
    }

    @Override
    public List<ISkill> getSkills() {
        return new ArrayList<>(skills);
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public List<Component> getTraits() {
        List<Component> traits = new ArrayList<>();
        traits.add(Component.translatable("character.corpseorigin.baixiaofei.trait1"));
        traits.add(Component.translatable("character.corpseorigin.baixiaofei.trait2"));
        return traits;
    }

    public static class BaiXiaoFeiSkills {

        public static class HotBloodSkill extends BaseSkill {
            public HotBloodSkill() {
                super(new Builder(id("hot_blood"))
                        .name(Component.translatable("skill.corpseorigin.baixiaofei.hot_blood"))
                        .description(Component.translatable("skill.corpseorigin.baixiaofei.hot_blood.desc"))
                        .cost(0)
                        .skillType(ISkill.SkillType.BASIC_EVOLUTION)
                        .requiredLevel(1)
                        .passive(true)
                        .attributeModifier(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(id("hot_blood"), 2.0, AttributeModifier.Operation.ADD_VALUE)));
            }
        }

        public static class FightingTechniqueSkill extends BaseSkill {
            public FightingTechniqueSkill() {
                super(new Builder(id("fighting_technique"))
                        .name(Component.translatable("skill.corpseorigin.baixiaofei.fighting_technique"))
                        .description(Component.translatable("skill.corpseorigin.baixiaofei.fighting_technique.desc"))
                        .cost(0)
                        .skillType(ISkill.SkillType.BASIC_EVOLUTION)
                        .requiredLevel(2)
                        .passive(true));
            }
        }

        public static class SurvivalInstinctSkill extends BaseSkill {
            public SurvivalInstinctSkill() {
                super(new Builder(id("survival_instinct"))
                        .name(Component.translatable("skill.corpseorigin.baixiaofei.survival_instinct"))
                        .description(Component.translatable("skill.corpseorigin.baixiaofei.survival_instinct.desc"))
                        .cost(0)
                        .skillType(ISkill.SkillType.SPECIAL_MUTATION)
                        .requiredLevel(3)
                        .passive(true));
            }

            @Override
            public void onLearn(net.minecraft.world.entity.player.Player player) {
                super.onLearn(player);
            }
        }

        public static class FlyingKickSkill extends BaseSkill {
            public FlyingKickSkill() {
                super(new Builder(id("flying_kick"))
                        .name(Component.translatable("skill.corpseorigin.baixiaofei.flying_kick"))
                        .description(Component.translatable("skill.corpseorigin.baixiaofei.flying_kick.desc"))
                        .cost(0)
                        .skillType(ISkill.SkillType.POWER_MUTATION)
                        .requiredLevel(2)
                        .cooldown(100));
            }

            @Override
            public void onActivate(net.minecraft.world.entity.player.Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1));
            }
        }

        public static class SwordMasterySkill extends BaseSkill {
            public SwordMasterySkill() {
                super(new Builder(id("sword_mastery"))
                        .name(Component.translatable("skill.corpseorigin.baixiaofei.sword_mastery"))
                        .description(Component.translatable("skill.corpseorigin.baixiaofei.sword_mastery.desc"))
                        .cost(0)
                        .skillType(ISkill.SkillType.POWER_MUTATION)
                        .requiredLevel(4)
                        .passive(true));
            }
        }

        private static ResourceLocation id(String path) {
            return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "baixiaofei_" + path);
        }
    }
}
