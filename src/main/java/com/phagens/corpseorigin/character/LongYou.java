package com.phagens.corpseorigin.character;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.skill.BaseSkill;
import com.phagens.corpseorigin.skill.ISkill;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

public class LongYou implements ICharacter {

    public static final String ID = "longyou";

    private final List<ISkill> skills = new ArrayList<>();

    public LongYou() {
        skills.add(new LongYouSkills.CorpseKingAuthoritySkill());
        skills.add(new LongYouSkills.DarkEnergySkill());
        skills.add(new LongYouSkills.BloodLineSkill());
        skills.add(new LongYouSkills.UndeadImmortalitySkill());
        skills.add(new LongYouSkills.HauntSkill());
        skills.add(new LongYouSkills.FearDominanceSkill());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getName() {
        return Component.translatable("character.corpseorigin.longyou");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("character.corpseorigin.longyou.desc");
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "textures/character/longyou.png");
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
        traits.add(Component.translatable("character.corpseorigin.longyou.trait1"));
        traits.add(Component.translatable("character.corpseorigin.longyou.trait2"));
        return traits;
    }

    public static class LongYouSkills {

        public static class CorpseKingAuthoritySkill extends BaseSkill {
            public CorpseKingAuthoritySkill() {
                super(new Builder(id("corpse_king_authority"))
                        .name(Component.translatable("skill.corpseorigin.longyou.corpse_king_authority"))
                        .description(Component.translatable("skill.corpseorigin.longyou.corpse_king_authority.desc"))
                        .cost(0)
                        .skillType(ISkill.SkillType.SUPREME_ABILITY)
                        .requiredLevel(1)
                        .passive(true)
                        .attributeModifier(Attributes.MAX_HEALTH,
                                new AttributeModifier(id("corpse_king_authority"), 40.0, AttributeModifier.Operation.ADD_VALUE))
                        .attributeModifier(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(id("corpse_king_authority_damage"), 10.0, AttributeModifier.Operation.ADD_VALUE)));
            }
        }

        public static class DarkEnergySkill extends BaseSkill {
            public DarkEnergySkill() {
                super(new Builder(id("dark_energy"))
                        .name(Component.translatable("skill.corpseorigin.longyou.dark_energy"))
                        .description(Component.translatable("skill.corpseorigin.longyou.dark_energy.desc"))
                        .cost(0)
                        .skillType(ISkill.SkillType.SUPREME_ABILITY)
                        .requiredLevel(2)
                        .cooldown(400));
            }

            @Override
            public void onActivate(net.minecraft.world.entity.player.Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 2));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0));
            }
        }

        public static class BloodLineSkill extends BaseSkill {
            public BloodLineSkill() {
                super(new Builder(id("blood_line"))
                        .name(Component.translatable("skill.corpseorigin.longyou.blood_line"))
                        .description(Component.translatable("skill.corpseorigin.longyou.blood_line.desc"))
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

        public static class UndeadImmortalitySkill extends BaseSkill {
            public UndeadImmortalitySkill() {
                super(new Builder(id("undead_immortality"))
                        .name(Component.translatable("skill.corpseorigin.longyou.undead_immortality"))
                        .description(Component.translatable("skill.corpseorigin.longyou.undead_immortality.desc"))
                        .cost(0)
                        .skillType(ISkill.SkillType.DIVINE_ABILITY)
                        .requiredLevel(4)
                        .passive(true)
                        .attributeModifier(Attributes.ARMOR,
                                new AttributeModifier(id("undead_immortality"), 10.0, AttributeModifier.Operation.ADD_VALUE)));
            }
        }

        public static class HauntSkill extends BaseSkill {
            public HauntSkill() {
                super(new Builder(id("haunt"))
                        .name(Component.translatable("skill.corpseorigin.longyou.haunt"))
                        .description(Component.translatable("skill.corpseorigin.longyou.haunt.desc"))
                        .cost(0)
                        .skillType(ISkill.SkillType.SPECIAL_MUTATION)
                        .requiredLevel(3)
                        .cooldown(600));
            }

            @Override
            public void onActivate(net.minecraft.world.entity.player.Player player) {
                player.level().getEntities(player, player.getBoundingBox().inflate(8.0),
                                entity -> entity instanceof LivingEntity && !(entity instanceof net.minecraft.world.entity.player.Player))
                        .forEach(entity -> {
                            if (entity instanceof LivingEntity living) {
                                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
                                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
                            }
                        });
            }
        }

        public static class FearDominanceSkill extends BaseSkill {
            public FearDominanceSkill() {
                super(new Builder(id("fear_dominance"))
                        .name(Component.translatable("skill.corpseorigin.longyou.fear_dominance"))
                        .description(Component.translatable("skill.corpseorigin.longyou.fear_dominance.desc"))
                        .cost(0)
                        .skillType(ISkill.SkillType.SUPREME_ABILITY)
                        .requiredLevel(5)
                        .cooldown(800));
            }

            @Override
            public void onActivate(net.minecraft.world.entity.player.Player player) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 150, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 150, 2));

                player.level().getEntities(player, player.getBoundingBox().inflate(10.0),
                                entity -> entity instanceof LivingEntity)
                        .forEach(entity -> {
                            if (entity instanceof LivingEntity living && !(entity instanceof net.minecraft.world.entity.player.Player)) {
                                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 150, 3));
                            }
                        });
            }
        }

        private static ResourceLocation id(String path) {
            return ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "longyou_" + path);
        }
    }
}
