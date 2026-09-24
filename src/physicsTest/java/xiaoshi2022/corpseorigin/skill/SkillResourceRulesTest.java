package xiaoshi2022.corpseorigin.skill;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.regex.Pattern;

public final class SkillResourceRulesTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        var poetry = SkillResourceRules.cost("ancient_poetry_sword", 0);
        check(!poetry.affordable(0, 100, 600), "No qi cannot cast poetry even with full blood");
        check(!poetry.affordable(100, 9, 600), "Blood cannot replace missing mana");
        check(poetry.affordable(100, 10, 0), "Exact mana pays without blood");
        var mixed = SkillResourceRules.cost("killing_incarnation", 0);
        check(!mixed.affordable(120, 39, 600), "Dual resource rejects missing mana");
        check(!mixed.affordable(120, 120, 39), "Dual resource rejects missing blood");
        check(mixed.affordable(120, 40, 40), "Dual resource exact threshold");
        check(SkillResourceRules.cost("blood_wing_blade", 10).affordable(0, 0, 15), "Vampire ability uses blood only");
        for (String path : new String[]{"hound_unleashed", "dog_eye_cannon", "peel_shell", "flesh_abandon", "detach_guardian"})
            check(SkillResourceRules.cost(path, 0).affordable(0, 0, 0), "Item/escape stays free: " + path);
        check(SkillResourceRules.poetryStage(1) + SkillResourceRules.poetryStage(2)
                + SkillResourceRules.poetryStage(3) + SkillResourceRules.poetryStage(4) == 70, "Poetry combo costs 70 plus startup");
        check(SkillResourceRules.cost("future_skill", 17).inner() == 17, "New skill preserves declared cost");
        String[] gourds={"gourd_arms","gourd_acid","gourd_fire","gourd_eyes","gourd_power"};
        int[] blood={15,20,30,10,20};
        for(int i=0;i<gourds.length;i++){
            var cost=SkillResourceRules.cost(gourds[i],0);
            check(cost.inner()==0 && cost.blood()==blood[i],"Gourd uses biological blood: "+gourds[i]);
            check(!cost.affordable(0,0,blood[i]-1),"Insufficient blood blocks gourd: "+gourds[i]);
            check(cost.affordable(0,0,blood[i]),"Gourd needs no human qi: "+gourds[i]);
        }
        check(SkillResourceRules.cost("gourd_link",0).affordable(0,0,0),"Recall remains free");
        // Fail if a new concrete skill is added without an explicit catalogue decision.
        Path root = Path.of("src/main/java/xiaoshi2022/corpseorigin/skill");
        String catalogue = Files.readString(root.resolve("SkillResourceRules.java"));
        var known = new HashSet<String>();
        var entries = Pattern.compile("\"([a-z_]+)\"").matcher(catalogue);
        while (entries.find()) known.add(entries.group(1));
        var id = Pattern.compile("(?:PATH\\s*=|super\\(|fromNamespaceAndPath\\(CorpseOrigin.MOD_ID,)\\s*\"([a-z_]+)\"");
        try (var files = Files.walk(root)) {
            for (var file : files.filter(p -> p.toString().endsWith("Skill.java")).toList()) {
                var match = id.matcher(Files.readString(file));
                if (match.find()) check(known.contains(match.group(1)), "Missing skill cost: " + file);
            }
        }
        for (String form : new String[]{"zhi","ji","li","yu","qi","hui","mie","wu","shen","nipo","pogang","tiangangpo"})
            check(known.contains("tiangang_" + form), "TianGang form audited: " + form);
        String manager = Files.readString(root.resolve("SkillManager.java"));
        check(manager.indexOf("SkillResources.pay(player, skill.getResourceCost())")
                < manager.indexOf("int ticks = skill.getCooldownTicks()"), "Payment gates cooldown");
        String resources = Files.readString(root.resolve("SkillResources.java"));
        check(resources.indexOf("cost.affordable") < resources.indexOf("InnerPowerManager.consume"), "Both balances checked before mana mutation");
        check(resources.indexOf("cost.affordable") < resources.indexOf("BloodReserve.spend"), "Both balances checked before blood mutation");
        String inner = Files.readString(root.resolve("../character/InnerPowerManager.java"));
        check(!inner.contains("BloodReserve") && !inner.contains("usesFlesh"), "Mana pool independent of blood");
        System.out.println("Skill resources: " + checks + " regression checks passed.");
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
