package xiaoshi2022.corpseorigin.skill;

/** Shared server/client catalogue: blood never substitutes for inner power. */
public final class SkillResourceRules {
    private SkillResourceRules() {}
    public record Cost(int inner, int blood) {
        public Cost { if (inner < 0 || blood < 0) throw new IllegalArgumentException("Negative skill cost"); }
        public boolean affordable(int maxInner, int innerAvailable, int bloodAvailable) {
            return (inner == 0 || maxInner > 0 && innerAvailable >= inner) && bloodAvailable >= blood;
        }
    }
    public static Cost cost(String path, int declaredInner) {
        return switch (path) {
            case "gourd_arms" -> new Cost(0,15);
            case "gourd_acid", "gourd_power" -> new Cost(0,20);
            case "gourd_fire" -> new Cost(0,30);
            case "gourd_eyes" -> new Cost(0,10);
            // 三节棍的棍术横扫是纯物理武艺，不吃内力 —— 尸兄鬼棍的内力上限是 0，
            // 收内力的话它反而永远挥不动这把兵器（见 RoleChapterSkill#castWithWeapon）。
            case "gourd_link", "gourd_devour", "guigun_sweep", "guigun_resonance", "guigun_crush", "wuchou_blade", "wuchou_step", "wusheng_twin", "wusheng_cross" -> new Cost(0,0);
            case "guigun_guard" -> new Cost(20,0);
            case "ancient_poetry_sword", "water_orb", "tian_gang_blood_lotus" -> new Cost(10, 0);
            case "spatial_blink", "sword_flower", "round_dance", "power_strike", "osmium_gold", "osmium_ice_spike" -> new Cost(15, 0);
            case "meteor_sword", "corpse_king_thunder" -> new Cost(25, 0);
            case "tengu_divine_array", "corpse_king_infrasound" -> new Cost(40, 0);
            case "wood_bind", "flame_strike" -> new Cost(12, 0);
            case "five_elements_formation", "reverse_formation_fireball", "qi_lock" -> new Cost(20, 0);
            case "swallow_nest" -> new Cost(8, 0);
            case "natural_judgment" -> new Cost(80, 0);
            case "thunder_power" -> new Cost(5, 0);
            case "swarm_bite", "water_bite", "pounce_combo", "bear_charge" -> new Cost(0, 8);
            case "heart_grab_ambush", "chameleon_disguise", "bag_capture", "severed_arm_strike", "bat_cloak", "blood_wing_blade", "chrysanthemum_shield", "killing_gas", "ground_burrow", "mouth_snake" -> new Cost(0, 15);
            case "summon_swarm", "corpse_fish_eggs", "muscle_rage", "thousand_eyes", "xuanwu_body" -> new Cost(0, 25);
            case "antenna_block", "jingang_infant_convergence", "merge_guardian" -> new Cost(0, 10);
            case "son_of_corpse_nest", "golden_cicada_shell" -> new Cost(0, 40);
            case "corpse_brother_rally" -> new Cost(0, 20);
            case "blood_lotus_armor", "blood_cloud", "tyrant_strike" -> new Cost(10, 15);
            case "crimson_blood_spear" -> new Cost(30, 15);
            case "blood_lotus" -> new Cost(25, 30);
            case "killing_incarnation" -> new Cost(40, 40);
            // Physical/item actions, resource acquisition, passive abilities and escape controls.
            // Revive/reshape and pollution retain their existing internal blood/hunger payments.
            case "dark_siphon", "life_drain_suck", "dog_eye_cannon", "hound_unleashed", "ham_summon", "tiger_claw_bee_wheel", "keeper_melee", "special_forces_combat", "defense_stance", "parcel_bomb", "black_friday_eight", "revive_guardian", "flesh_reshape", "flesh_abandon", "peel_shell", "detach_guardian", "nest_sense", "water_pollution", "slaughter_awakening", "black_gold_heart", "slaughter_momentum", "iron_body", "undying_chest", "escape_passive" -> new Cost(0, 0);
            case "tiangang_zhi", "tiangang_qi" -> new Cost(0, 0);
            case "tiangang_ji", "tiangang_nipo" -> new Cost(15, 0);
            case "tiangang_li", "tiangang_yu", "tiangang_pogang" -> new Cost(20, 0);
            case "tiangang_hui" -> new Cost(30, 0);
            case "tiangang_mie", "tiangang_wu" -> new Cost(25, 0);
            case "tiangang_shen" -> new Cost(50, 0);
            case "tiangang_tiangangpo" -> new Cost(40, 0);
            default -> new Cost(Math.max(0, declaredInner), 0);
        };
    }
    public static int poetryStage(int stage) {
        return switch (stage) { case 1 -> 10; case 2 -> 15; case 3 -> 20; case 4 -> 25;
            default -> throw new IllegalArgumentException("Invalid poetry stage"); };
    }
}
