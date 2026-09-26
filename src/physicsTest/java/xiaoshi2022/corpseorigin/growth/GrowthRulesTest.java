package xiaoshi2022.corpseorigin.growth;

import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.unlock.SkillLearningRules;

public final class GrowthRulesTest {
    private static int checks;
    public static void main(String[] args) {
        check(VampireRules.isVampire("k", false));
        check(VampireRules.isVampire("heixiaofei", false));
        check(VampireRules.isVampire("corpse_brother", true));
        check(VampireRules.isVampire("guigun_corpse", true));
        check(!VampireRules.isVampire("corpse_brother", false));
        check(!VampireRules.isVampire("mortal", false));
        // Only exposed, vulnerable K takes sunlight damage; every protection independently wins.
        check(VampireRules.sunlightHurts("k", true, true, true, true, true, false, false));
        for (String role : new String[]{"heixiaofei", "corpse_brother", "mortal"})
            check(!VampireRules.sunlightHurts(role, true, true, true, true, true, false, false));
        for (int disabled = 0; disabled < 7; disabled++) {
            boolean[] conditions = {true, true, true, true, true, false, false};
            conditions[disabled] = !conditions[disabled];
            check(!VampireRules.sunlightHurts("k", conditions[0], conditions[1], conditions[2],
                    conditions[3], conditions[4], conditions[5], conditions[6]));
        }
        check(GrowthRules.progress(0, 5) == 1);
        check(GrowthRules.progress(4, 5) == 5);
        check(GrowthRules.progress(Integer.MAX_VALUE, 5) == 5);
        check(GrowthRules.progress(-3, 0) == 1);
        check(!GrowthRules.inherit(0, 0));
        check(GrowthRules.inherit(.999, 1));
        check(!GrowthRules.inherit(.15, .15));
        check(GrowthRules.inherit(.149, .15));
        check(!GrowthRules.inherit(.1, Double.NaN));
        check(GrowthRules.reward(Integer.MAX_VALUE - 2, 100) == 2);
        check(GrowthRules.reward(10, -1) == 0);
        check(GrowthRules.reward(10, 25) == 25);
        check(SkillLearningRules.innate("longyou", "corpse_brother_rally"));
        check(SkillLearningRules.innate("longyou", "tian_gang_blood_lotus"));
        for (String path : SkillLearningRules.THUNDER) check(!SkillLearningRules.innate("longyou", path));
        check(!SkillLearningRules.innate("zhaoritian", "tian_gang_blood_lotus"));
        for(String path:new String[]{"gourd_link","gourd_arms","gourd_acid","gourd_fire","gourd_eyes","gourd_power"}){
            check(SkillLearningRules.innate("xiaojingang",path));
            check(!SkillLearningRules.innate("mortal",path));
        }
        check(!SkillLearningRules.innate("xiaojingang","guigun_sweep"));
        // 鬼棍两种形态天生招式不互串：人类只有棍法+金针，尸兄只有尸棍那两招
        check(SkillLearningRules.innate("guigun_human","guigun_sweep"));
        check(SkillLearningRules.innate("guigun_human","guigun_guard"));
        check(!SkillLearningRules.innate("guigun_human","guigun_resonance"));
        check(!SkillLearningRules.innate("guigun_human","guigun_crush"));
        check(SkillLearningRules.innate("guigun_corpse","guigun_resonance"));
        check(SkillLearningRules.innate("guigun_corpse","guigun_crush"));
        check(!SkillLearningRules.innate("guigun_corpse","guigun_sweep"));
        check(!SkillLearningRules.innate("guigun_corpse","guigun_guard"));
        check(SkillLearningRules.cost("thunder_power", SkillType.COMBAT, 0, true) == 8);
        check(SkillLearningRules.cost("corpse_king_thunder", SkillType.COMBAT, 200, true) == 15);
        check(SkillLearningRules.cost("natural_judgment", SkillType.ULTIMATE, 1200, true) == 30);
        check(SkillLearningRules.level("natural_judgment", SkillType.ULTIMATE, 1200, true) == 5);
        // 中心表梯度（保守）：普通 人2/4点 · 中坚 人3/6点 · 强力 人4/10点 · 终极 地1/18点 · 被动 人2/5点
        check(SkillLearningRules.level("pounce_combo", SkillType.COMBAT, 160, true) == 2);
        check(SkillLearningRules.cost("pounce_combo", SkillType.COMBAT, 160, true) == 4);
        check(SkillLearningRules.level("wood_bind", SkillType.COMBAT, 200, true) == 3);
        check(SkillLearningRules.cost("wood_bind", SkillType.COMBAT, 200, true) == 6);
        check(SkillLearningRules.level("meteor_sword", SkillType.COMBAT, 600, true) == 4);
        check(SkillLearningRules.cost("meteor_sword", SkillType.COMBAT, 600, true) == 10);
        check(SkillLearningRules.level("killing_incarnation", SkillType.ULTIMATE, 2400, true) == 5);
        check(SkillLearningRules.cost("killing_incarnation", SkillType.ULTIMATE, 2400, true) == 18);
        check(SkillLearningRules.level("iron_body", SkillType.DEFENSE, 0, false) == 2);
        check(SkillLearningRules.cost("iron_body", SkillType.DEFENSE, 0, false) == 5);
        check(SkillLearningRules.level("chameleon_disguise", SkillType.UTILITY, 600, true) == 2);
        check(SkillLearningRules.cost("chameleon_disguise", SkillType.UTILITY, 600, true) == 4);
        System.out.println("Growth and learning rules: " + checks + " regression checks passed.");
    }
    private static void check(boolean valid) {
        checks++;
        if (!valid) throw new AssertionError("Growth check " + checks + " failed");
    }
}
