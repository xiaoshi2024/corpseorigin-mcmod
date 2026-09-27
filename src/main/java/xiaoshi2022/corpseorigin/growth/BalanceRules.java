package xiaoshi2022.corpseorigin.growth;

/** Bounded calculations shared by combat and regression checks. */
public final class BalanceRules {
    private BalanceRules() {}
    public static float bounded(float value,float fallback,float min,float max){
        return Float.isFinite(value)?Math.clamp(value,min,max):fallback;
    }
    public static float executeDamage(float health,float maxHealth,float attack,float threshold,float cap){
        if(!Float.isFinite(health)||!Float.isFinite(maxHealth)||maxHealth<=0||health<=0
                ||health/maxHealth>bounded(threshold,.2f,.01f,.5f))return 0;
        return Math.min(bounded(cap,20,1,100),bounded(attack,1,0,100)*1.5f);
    }
    public static int discoveryLevel(int required,boolean ultimate){return Math.max(required,ultimate?9:1);}
    /** Sword qi remains lethal to wounded bodies, but equipment spikes cannot erase a full-health peer. */
    public static float swordQiDamage(float raw,float maxHealth,int attackerLevel,int defenderLevel){
        if(!Float.isFinite(raw)||raw<=0||!Float.isFinite(maxHealth)||maxHealth<=0)return 0;
        // Zero denotes a creature outside the player/clone evolution system.
        if(defenderLevel<=0)return raw;
        int gap=Math.clamp(attackerLevel,1,20)-Math.clamp(defenderLevel,1,20);
        float resistance=(float)Math.pow(.85,Math.max(0,-gap));
        float fraction=Math.clamp(.25f+gap*.03f,.10f,.60f);
        return Math.min(raw*resistance,maxHealth*fraction);
    }
    public static final class Window {
        private long start=Long.MIN_VALUE;
        private int spent;
        public int take(long tick,int requested,int limit){
            if(start==Long.MIN_VALUE||tick<start||tick-start>=20){start=tick;spent=0;}
            int granted=Math.max(0,Math.min(requested,Math.max(0,limit-spent)));spent+=granted;return granted;
        }
    }
}
