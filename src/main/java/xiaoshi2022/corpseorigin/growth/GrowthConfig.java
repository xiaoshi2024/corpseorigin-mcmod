package xiaoshi2022.corpseorigin.growth;

import java.util.ArrayList;
import java.util.List;

/** Server settings under growth in config/corpseorigin.json. */
public final class GrowthConfig {
    public boolean enabled = true;
    public int preyRequired = 5;
    public int flightBloodPerSecond = 6;
    public int aquaticBloodPerSecond = 2;
    public float juqueExecuteThreshold = .20f;
    public float juqueExecuteDamageCap = 20;
    public float sustainedAreaDamageMultiplier = .35f;
    public int combatBloodPerSecond = 4;
    public int fleshPerOpportunity = 20;
    /** Apply even to existing configs: fewer discoveries from plentiful flesh. */
    public int fleshOpportunityMultiplier = 3;
    public int fleshOpportunityCooldownTicks = 12000;
    public int fleshPerGrowthPoint = 5;
    public int villageTrainingPoints = 3;
    public boolean villageOpportunitiesEnabled = true;
    /** One skill discovery per three successful daily lessons; ordinary training rewards remain. */
    public int villageLessonsPerOpportunity = 3;
    public boolean explorationOpportunitiesEnabled = true;
    public double explorationOpportunityChance = .5;
    public boolean teachingEnabled = true;
    public int teachingCooldownTicks = 12000;
    public boolean fleshOpportunitiesEnabled = true;
    public double inheritanceChance = .15;
    public List<Exploration> exploration = new ArrayList<>(List.of(
            new Exploration("corpseorigin:h_city_ruins", 12),
            new Exploration("corpseorigin:h_city_library", 25)));
    public List<Teaching> teachings = new ArrayList<>();

    public static final class Exploration {
        public String structure;
        public int points;
        public Exploration(String structure, int points) { this.structure = structure; this.points = points; }
    }
    public static final class Teaching {
        /** Stable unique event ID and administrator-assigned entity scoreboard tag. */
        public String id, npcTag, role, skill;
        public int points = 10;
    }
}
