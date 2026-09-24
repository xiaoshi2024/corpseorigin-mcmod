package xiaoshi2022.corpseorigin.growth;

import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.unlock.SkillLearningRules;

public final class GrowthRulesTest {
    private static int checks;
    public static void main(String[] args) {
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
        check(SkillLearningRules.cost("thunder_power", SkillType.COMBAT, 0, true) == 8);
        check(SkillLearningRules.cost("corpse_king_thunder", SkillType.COMBAT, 200, true) == 15);
        check(SkillLearningRules.cost("natural_judgment", SkillType.ULTIMATE, 1200, true) == 30);
        check(SkillLearningRules.level("natural_judgment", SkillType.ULTIMATE) == 5);
        System.out.println("Growth and learning rules: " + checks + " regression checks passed.");
    }
    private static void check(boolean valid) {
        checks++;
        if (!valid) throw new AssertionError("Growth check " + checks + " failed");
    }
}
