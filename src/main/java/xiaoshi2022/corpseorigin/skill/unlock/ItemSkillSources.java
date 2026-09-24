package xiaoshi2022.corpseorigin.skill.unlock;

import java.util.List;
import net.minecraft.world.item.Item;
import xiaoshi2022.corpseorigin.registry.ModItems;

/** Only weapons explicitly required by the skill's activation checks belong here. */
public final class ItemSkillSources {
    private ItemSkillSources() {}
    public static List<SkillUnlockSource> forSkill(String path) {
        Item item = switch (path) {
            case "meteor_sword" -> ModItems.RED_METEOR_SWORD;
            case "dark_siphon", "blood_wing_blade" -> ModItems.BLOOD_WING_BLADE;
            case "tiger_claw_bee_wheel" -> ModItems.BEE_WHEEL;
            case "tian_gang_blood_lotus" -> ModItems.TIAN_GANG_KEY;
            default -> null;
        };
        if(item==null)return List.of();
        var source=SkillUnlockSource.item(path,item);
        String requirement=switch(path){
            case "dark_siphon","blood_wing_blade" -> "持有血翼黑刃 + 吸血鬼体质";
            case "tian_gang_blood_lotus" -> "持有天罡匙 + 天罡一脉 + 内力传承";
            default -> null;
        };
        return requirement==null?List.of(source):List.of(SkillUnlockSource.custom(source.id(),
                net.minecraft.network.chat.Component.literal(requirement),player->source.isSatisfiedBy(player)
                        &&xiaoshi2022.corpseorigin.growth.WeaponEligibility.skillReason(player,path)==null));
    }
}
