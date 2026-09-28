package xiaoshi2022.corpseorigin.growth;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;

public final class RealmRulesTest {
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);}
    public static void main(String[] args){
        RealmConfig cooldown = new RealmConfig();
        for(int tier=2;tier<=20;tier++){
            check(xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules.height(tier)>xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules.height(tier-1),"sword size grows each level");
            check(xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules.range(tier)>xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules.range(tier-1),"sword range grows each level");
        }
        check(xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules.riftLength(9)==0,"no mortal terrain rift");
        check(xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules.height(12)==224,"late god mountain blade");
        check(xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules.riftLength(15)==512,"EX canyon scale");
        check(RealmRules.qiSkillMultiplier(1000000,15,cooldown)==1,"baseline qi does not multiply realm twice");
        check(RealmRules.qiSkillMultiplier(2000000,15,cooldown)==2,"double capacity doubles qi skill damage");
        check(RealmRules.qiSkillMultiplier(3000000,15,cooldown)==3,"triple capacity scales linearly");
        check(RealmRules.qiSkillMultiplier(0,15,cooldown)==1,"no reduction below baseline");
        check(RealmRules.damage(20,1e6,RealmRules.qiSkillMultiplier(2000000,15,cooldown))==2e6f,"damage applies one capacity multiplier");
        check(RealmRules.damage(2e6f,1e6,2)==4e6f,"attribute based hits not multiplied by realm again");
        check(RealmRules.damage(1e12f,1e12,1e9)<=RealmRules.ATTRIBUTE_CAP,"qi bonus respects damage safety cap");
        cooldown.qiSkillDamageScaling=0;
        check(RealmRules.qiSkillMultiplier(2000000,15,cooldown)==1,"qi multiplier configurable off");
        cooldown.qiSkillDamageScaling=1;
        int previous = 200;
        for (int level=1;level<=20;level++) {
            int ticks=RealmRules.cooldown(200,level,false,cooldown);
            check(ticks<=previous && ticks>=50,"ordinary cooldown decreases to cap");
            check(RealmRules.cooldown(1200,level,true,cooldown)==1200,"ultimate cooldown fixed");
            previous=ticks;
        }
        check(RealmRules.cooldown(200,1,false,cooldown)==200,"level one unchanged");
        check(RealmRules.cooldown(200,15,false,cooldown)==88,"EX ordinary 56 percent reduction");
        check(RealmRules.cooldown(200,20,false,cooldown)==50,"75 percent cap");
        check(RealmRules.cooldown(0,20,false,cooldown)==0,"zero cooldown remains zero");
        check(RealmRules.cooldown(2,20,false,cooldown)==2,"floor never lengthens short skills");
        cooldown.enabled=false;
        check(RealmRules.qiSkillMultiplier(2000000,15,cooldown)==1,"realm off disables qi damage");
        check(RealmRules.cooldown(200,20,false,cooldown)==200,"disabled reduction");
        check(RealmRules.resourceCost(10,1000000,.08)==80000,"EX activation percentage");
        check(RealmRules.resourceCost(10,50,.08)==10,"legacy minimum price");
        check(RealmRules.resourceCost(2,1000000,.01)==10000,"domain upkeep scales");
        cooldown.maxCooldownReduction=Double.NaN;cooldown.poetryStageFraction=10;cooldown.poetryCooldownTicks=-1;cooldown.sanitize();
        check(cooldown.maxCooldownReduction==.75 && cooldown.poetryStageFraction==.2 && cooldown.poetryCooldownTicks==20,"cooldown and cost config bounds");
        check(RealmRules.cost(0,10,10,2)==190,"batch price");
        long sum=0; for(int i=0;i<1000;i++)sum+=RealmRules.cost(100000+i,1,10,2);
        check(sum==RealmRules.cost(100000,1000,10,2),"large batch avoids int overflow");
        check(RealmRules.cost(1,-1,10,2)==Long.MAX_VALUE,"negative count rejected");
        check(RealmRules.damage(20,1e6,1)==1e6f,"fixed skill tier scaling");
        check(RealmRules.damage(2e6f,1e6,1)==2e6f,"attribute hit not amplified twice");
        for(int i=2;i<=20;i++){
            check(RealmRules.health(i)>RealmRules.health(i-1),"health progression");
            check(RealmRules.attack(i)>RealmRules.attack(i-1),"damage progression");
            check(RealmRules.protection(i,100000)<1,"no absolute invulnerability");
        }
        check(RealmRules.health(15)>=1e8,"EX health tier");
        for(int rank:new int[]{0,100,1000,10000,99999}) {
            check(RealmRules.trained(1e8,rank+1)>RealmRules.trained(1e8,rank),"late vitality still useful");
            check(RealmRules.speed(15,rank+1)>RealmRules.speed(15,rank),"late agility still useful");
            check(RealmRules.protection(15,rank+1)>RealmRules.protection(15,rank),"late guard still useful");
            check(RealmRules.regeneration(15,rank+1)>RealmRules.regeneration(15,rank),"late recovery still useful");
        }
        check(RealmRules.trained(1e12,1000000)<=RealmRules.ATTRIBUTE_CAP,"finite upper bound");
        RealmConfig c=new RealmConfig();c.difficultyMultiplier=Double.NaN;c.burstRadius=Double.POSITIVE_INFINITY;c.trainingCostStep=-1;c.sanitize();
        check(c.difficultyMultiplier==3 && c.burstRadius==64 && c.trainingCostStep==1,"invalid config repaired");
        EvolutionManager.configure(3);
        for(int i=2;i<=20;i++){
            int threshold=EvolutionManager.getThreshold(i);
            check(EvolutionManager.getLevel(threshold)==i,"configured boundary");
            check(EvolutionManager.getLevel(threshold-1)==i-1,"below boundary");
        }
        check(EvolutionManager.getLevel(EvolutionManager.migrateLegacyPoints(950))==15,"legacy rank migration");
        check(EvolutionManager.pointsToNextLevel(Integer.MAX_VALUE)==0,"maximum rank");
        EvolutionManager.configure(1);
        System.out.println("RealmRulesTest passed");
    }
}
