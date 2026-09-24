package xiaoshi2022.corpseorigin.growth;

/** Deterministic bounds shared by server gates and regression tests. */
public final class OpportunityRules {
    private OpportunityRules() {}
    public static boolean lessonDue(int count,int interval){return count>0 && count%Math.clamp(interval,1,10000)==0;}
    public static boolean roll(double random,double chance){
        return random>=0 && random<1 && random<(Double.isFinite(chance)?Math.clamp(chance,0,1):.5);
    }
    public static boolean ready(long now,long last,int cooldown){
        return last==Long.MIN_VALUE || now<last || now-last>=Math.clamp(cooldown,0,1728000);
    }
}
