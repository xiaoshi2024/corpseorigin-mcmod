package xiaoshi2022.corpseorigin.growth;
public final class CorpseHorrorConfig {
    public boolean enabled=true, exposedRibs=true, maggotsOnDeath=true, bloodEffects=true;
    public double livingRibsChance=.1, deathMaggotChance=.25, grappleEscapeChance=.18;
    public double villagerInfectionChance=.3, fishWaterInfectionChance=.35;
    /** 尸族直接攻击命中玩家时的感染概率（上 QIANS 效果，60~300 秒后尸化；牛奶可清）。 */
    public double playerBiteInfectionChance=.15;
    public int fishInfectionTicks=200;
    public int packRadius=20,packAllies=8,maxGrapplers=3,grappleTicks=60,grappleCooldownTicks=60;
    public int maggotsPerCorpse=3,localMaggotCap=24,worldMaggotCap=128,maggotLifetimeTicks=1200;
    public boolean maggotParasitism=true;
    public int maggotsPerHost=2,maggotBurrowTicks=30,maggotInfectionTicks=200;
    public void sanitize(){
        villagerInfectionChance=Double.isFinite(villagerInfectionChance)?Math.clamp(villagerInfectionChance,0,1):.3;
        playerBiteInfectionChance=Double.isFinite(playerBiteInfectionChance)?Math.clamp(playerBiteInfectionChance,0,1):.15;
        fishWaterInfectionChance=Double.isFinite(fishWaterInfectionChance)?Math.clamp(fishWaterInfectionChance,0,1):.35;
        fishInfectionTicks=Math.clamp(fishInfectionTicks,20,12000);
        livingRibsChance=Double.isFinite(livingRibsChance)?Math.clamp(livingRibsChance,0,1):.1;
        deathMaggotChance=Double.isFinite(deathMaggotChance)?Math.clamp(deathMaggotChance,0,1):.25;
        grappleEscapeChance=Double.isFinite(grappleEscapeChance)?Math.clamp(grappleEscapeChance,0,1):.18;
        packRadius=Math.clamp(packRadius,4,40);packAllies=Math.clamp(packAllies,1,16);
        maxGrapplers=Math.clamp(maxGrapplers,1,4);grappleTicks=Math.clamp(grappleTicks,20,100);grappleCooldownTicks=Math.clamp(grappleCooldownTicks,20,400);
        maggotsPerCorpse=Math.clamp(maggotsPerCorpse,0,6);localMaggotCap=Math.clamp(localMaggotCap,1,48);worldMaggotCap=Math.clamp(worldMaggotCap,8,256);
        maggotLifetimeTicks=Math.clamp(maggotLifetimeTicks,200,6000);
        maggotsPerHost=Math.clamp(maggotsPerHost,1,4);
        maggotBurrowTicks=Math.clamp(maggotBurrowTicks,10,100);
        maggotInfectionTicks=Math.clamp(maggotInfectionTicks,100,1200);
    }
}
