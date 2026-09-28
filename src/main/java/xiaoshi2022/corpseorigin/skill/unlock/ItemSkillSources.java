package xiaoshi2022.corpseorigin.skill.unlock;

import net.minecraft.world.item.Item;
import xiaoshi2022.corpseorigin.registry.ModItems;

import java.util.List;

/** Only weapons explicitly required by the skill's activation checks belong here. */
public final class ItemSkillSources {
    private ItemSkillSources() {}
    public static List<SkillUnlockSource> forSkill(String path) {
        Item item = switch (path) {
            case "meteor_sword" -> ModItems.RED_METEOR_SWORD;
            case "dark_siphon", "blood_wing_blade" -> ModItems.BLOOD_WING_BLADE;
            case "tiger_claw_bee_wheel" -> ModItems.BEE_WHEEL;
            // 鬼棍的棍法绑定对应兵器：拿到就该学会（使用条件见 RoleChapterSkill 的 weapon 参数）
            case "guigun_sweep" -> ModItems.GUIGUN_WEAP;                        // 三节棍
            case "guigun_resonance", "guigun_crush" -> ModItems.GUIGUN_CLUB;    // 尸棍
            case "tian_gang_blood_lotus" -> ModItems.TIAN_GANG_KEY;
            default -> null;
        };
        if(item==null)return List.of();
        var source=SkillUnlockSource.item(path,item);
        String requirement=switch(path){
            case "dark_siphon","blood_wing_blade" -> "unlock.corpseorigin.vampire_blade";
            case "tian_gang_blood_lotus" -> "unlock.corpseorigin.tiangang_lineage";
            default -> null;
        };
        return requirement==null?List.of(source):List.of(SkillUnlockSource.custom(source.id(),
                net.minecraft.network.chat.Component.translatable(requirement),player->source.isSatisfiedBy(player)
                        &&xiaoshi2022.corpseorigin.growth.WeaponEligibility.skillReason(player,path)==null));
    }
}
