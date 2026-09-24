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
                    ? Component.literal("尚未觉醒气感：请通过拜师或探索传承习得内力")
                    : Component.literal("资源不足：需要内力 " + cost.inner() + " / 气血 " + cost.blood()
                        + "，当前 " + inner + " / " + blood));
            return false;
        }
        if (cost.inner() > 0 && !InnerPowerManager.consume(player, cost.inner())) return false;
        if (cost.blood() > 0) BloodReserve.spend(player, cost.blood());
        return true;
    }
    public static Component description(ISkill skill) {
        var cost = skill.getResourceCost();
        String text = "消耗：内力 " + cost.inner() + " / 气血 " + cost.blood();
        if (cost.inner() > 0) text += "（需气感）";
        text += switch (skill.getId().getPath()) {
            case "ancient_poetry_sword" -> "；连招 10/15/20/25，领域 2 内力/秒；关闭免费";
            case "revive_guardian" -> "；复活另耗 100 气血";
            case "flesh_reshape" -> "；重塑按形态消耗气血或饱食度";
            case "water_pollution", "slaughter_awakening" -> "；另有饥饿代价";
            case "thunder_power" -> "；命中另耗 2 内力，关闭免费";
            case "tian_gang_blood_lotus" -> "；引导持续耗内力，取消免费";
            case "killing_incarnation" -> "；追加攻击另耗 15 内力 + 15 气血";
            case "xuanwu_body", "son_of_corpse_nest" -> "；解除免费";
            default -> "";
        };
        return Component.literal(text);
    }
}
