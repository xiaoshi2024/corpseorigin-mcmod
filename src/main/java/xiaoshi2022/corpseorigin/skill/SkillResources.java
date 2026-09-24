package xiaoshi2022.corpseorigin.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.InnerPowerManager;
import xiaoshi2022.corpseorigin.skill.longyou.BloodReserve;

/** Server-thread payment: validate both balances before mutating either. */
public final class SkillResources {
    private SkillResources() {}
    public static boolean pay(ServerPlayer player, SkillResourceRules.Cost cost) {
        int max = InnerPowerManager.getMaxInnerPower(player);
        int inner = InnerPowerManager.getInnerPower(player);
        int blood = BloodReserve.get(player);
        if (!cost.affordable(max, inner, blood)) {
            player.sendOverlayMessage(cost.inner() > 0 && max <= 0
                    ? Component.translatable("message.corpseorigin.skill_resources.text_01")
                    : Component.translatable("message.corpseorigin.skill_resources.text_02", cost.inner(), cost.blood(), inner, blood));
            return false;
        }
        if (cost.inner() > 0 && !InnerPowerManager.consume(player, cost.inner())) return false;
        if (cost.blood() > 0) BloodReserve.spend(player, cost.blood());
        return true;
    }
    public static Component description(ISkill skill) {
        var cost = skill.getResourceCost();
        var text = Component.translatable("skill.corpseorigin.resource_cost", cost.inner(), cost.blood());
        if(cost.inner()>0) text.append(Component.translatable("skill.corpseorigin.requires_qi"));
        String extra=switch(skill.getId().getPath()) {
            case "ancient_poetry_sword" -> "poetry";
            case "revive_guardian" -> "revive";
            case "flesh_reshape" -> "reshape";
            case "water_pollution", "slaughter_awakening" -> "hunger";
            case "thunder_power" -> "thunder";
            case "tian_gang_blood_lotus" -> "lotus";
            case "killing_incarnation" -> "incarnation";
            case "xuanwu_body", "son_of_corpse_nest" -> "cancel";
            default -> "";
        };
        if(!extra.isEmpty())text.append(Component.translatable("skill.corpseorigin.extra_cost."+extra));
        return text;
    }
}
