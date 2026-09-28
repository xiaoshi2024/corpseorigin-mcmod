package xiaoshi2022.corpseorigin.skill;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.InnerPowerManager;
import xiaoshi2022.corpseorigin.growth.RealmProgression;
import xiaoshi2022.corpseorigin.growth.RealmRules;

/** Server-side prices and cooldowns, evaluated once when an action starts. */
public final class SkillBalance {
    private SkillBalance() {}

    public static int ordinaryCooldown(ServerPlayer player, int base) {
        var c = RealmProgression.config();
        return RealmRules.cooldown(base, RealmProgression.level(player), false, c);
    }

    public static int cooldown(ServerPlayer player, ISkill skill) {
        var c = RealmProgression.config();
        if (skill.getId().getPath().equals("ancient_poetry_sword")) return c.poetryCooldownTicks;
        return RealmRules.cooldown(skill.getCooldownTicks(), RealmProgression.level(player),
                skill.hasFixedCooldown(), c);
    }

    public static SkillResourceRules.Cost cost(ServerPlayer player, ISkill skill) {
        var base = skill.getResourceCost();
        var c = RealmProgression.config();
        double fraction = skill.getId().getPath().equals("ancient_poetry_sword")
                ? c.poetryActivationFraction : c.ultimateQiFraction;
        // Blood-only and purely physical abilities must not gain a new qi prerequisite.
        int inner = skill.hasFixedCooldown() && base.inner() > 0
                ? RealmRules.resourceCost(base.inner(), InnerPowerManager.getMaxInnerPower(player), fraction)
                : base.inner();
        return new SkillResourceRules.Cost(inner, base.blood());
    }

    public static SkillResourceRules.Cost poetryCost(ServerPlayer player, int stage) {
        var c = RealmProgression.config();
        int base;
        double fraction;
        if (stage == 0) { base = 2; fraction = c.poetryUpkeepFraction; }
        else if (stage == 5) { base = 15; fraction = c.poetrySwordFraction; }
        else {
            base = SkillResourceRules.poetryStage(stage);
            fraction = c.poetryStageFraction * (stage + 1);
        }
        return new SkillResourceRules.Cost(RealmRules.resourceCost(base,
                InnerPowerManager.getMaxInnerPower(player), fraction), 0);
    }
}
