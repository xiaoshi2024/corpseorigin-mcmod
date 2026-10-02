package xiaoshi2022.corpseorigin.growth;

/** Numeric progression rules; no Minecraft initialization needed for regression tests. */
public final class RealmRules {
    private RealmRules() {}
    public static final double ATTRIBUTE_CAP = 1.0e12;
    private static final double[] HEALTH = {0,0,10,20,40,100,200,400,800,4000,20000,60000,200000,1000000,10000000,100000000,300000000,1e9,3e9,1e10,3e10};
    private static final double[] ATTACK = {0,0,2,4,8,20,40,80,160,800,4000,12000,40000,200000,500000,1000000,3000000,1e7,3e7,1e8,3e8};
    private static final int[] QI = {0,0,20,40,80,200,400,800,1600,5000,20000,40000,80000,160000,320000,1000000,1500000,2000000,3000000,4000000,5000000};
    public static int cooldown(int base, int level, boolean fixed, RealmConfig c) {
        if (base <= 0) return 0;
        if (fixed || !c.enabled) return base;
        double reduction = Math.min(c.maxCooldownReduction,
                (Math.clamp(level, 1, 20) - 1) * c.cooldownReductionPerLevel);
        return Math.min(base, Math.max(c.minimumSkillCooldownTicks,
                (int)Math.ceil(base * (1 - reduction) - 1e-9)));
    }
    public static int resourceCost(int base, int capacity, double fraction) {
        return Math.max(Math.max(0, base), (int)Math.min(Integer.MAX_VALUE,
                Math.ceil(Math.max(0, capacity) * fraction)));
    }
    public static double health(int level) { return HEALTH[Math.clamp(level,1,20)]; }
    /** Capacity, not current reserves: paying for a technique must not weaken its own hit. */
    public static double qiSkillMultiplier(int capacity, int level, RealmConfig config) {
        if (!config.enabled) return 1;
        double baseline = Math.max(100, qi(level) * config.statMultiplier);
        return 1 + Math.max(0, capacity / baseline - 1) * config.qiSkillDamageScaling;
    }
    public static double attack(int level) { return ATTACK[Math.clamp(level,1,20)]; }
    public static int qi(int level) { return QI[Math.clamp(level,1,20)]; }
    public static double trained(double base, int rank) {
        // Logarithmic long-term growth remains useful at EX without hitting numeric caps early.
        return Math.min(ATTRIBUTE_CAP, Math.max(0,base) * (1 + Math.log1p(Math.clamp(rank,0,1000000) * .005)));
    }
    public static long cost(int rank, int count, int base, int step) {
        if (rank < 0 || count <= 0 || count > 1000 || base <= 0 || step <= 0) return Long.MAX_VALUE;
        return (long)count * base + (long)step * count * (2L * rank + count - 1) / 2;
    }
    /** Never amplifies an attribute-based melee hit a second time. Fixed skill hits get a tier floor. */
    public static float damage(float amount, double attack, double multiplier) {
        if (!Float.isFinite(amount) || amount <= 0) return amount;
        double extra = Math.max(0,attack) * Math.min(1, amount / 20.0);
        return (float)Math.min(ATTRIBUTE_CAP, Math.max(amount, extra) * Math.max(1, multiplier));
    }
    public static double protection(int level, int rank) {
        double base = level >= 15 ? .90 : level >= 13 ? .80 : level >= 10 ? .65 : level >= 9 ? .45 : level >= 5 ? .20 : 0;
        return .99 - (.99-base) / (1 + Math.clamp(rank,0,1000000) * .005);
    }
    /** @param cap 速度加值上限（GUI 可调 speedBonusCap） */
    public static double speed(int level,int rank,double cap) {
        // 放大版：等级基础每级 +1%（上限 +30%），rank 收敛至上限 cap（MOVEMENT_SPEED 0.1 基础）
        double base=Math.min(.30,Math.max(0,level-1)*.010);
        return cap-(cap-base)/(1+Math.clamp(rank,0,1000000)*.005);
    }
    public static double regeneration(int level,int rank) {
        if(level<5)return 0;
        double base=.0005*(Math.clamp(level,5,20)-4);
        return .02-(.02-base)/(1+Math.clamp(rank,0,1000000)*.005);
    }
}
