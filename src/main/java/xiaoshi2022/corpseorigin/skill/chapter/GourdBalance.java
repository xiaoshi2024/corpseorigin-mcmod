package xiaoshi2022.corpseorigin.skill.chapter;

/** Bounded biological conversion and cast timings, shared with regression tests. */
public final class GourdBalance {
    private GourdBalance() {}

    public static final int PET_CAPACITY = 120, PET_COOLDOWN = 600;

    /**
     * 脆皮生物的血量上限：最大血量不超过这个值的目标，满血即可吞。
     * <p>
     * 覆盖鸡(4)、兔(3)、羊(8)、猪(10)、牛(10)、蜜蜂(10)、猫(10)、狐狸(10)、
     * 鹦鹉(6)、蝙蝠(6)、鱿鱼(10)、青蛙(10)、蝌蚪(6) 等；美西螈(14) 在内，
     * 如需覆盖可调到 14。
     */
    public static final float FRAGILE_HEALTH = 14f;

    /** 普通生物的可吞血线：低于最大血量的这个比例即可吞。 */
    private static final float EDIBLE_RATIO = 0.5f;

    public static int petFlesh(float maxHealth) {
        return Math.min(15, flesh(maxHealth) / 2);
    }

    public static int transfer(int stored, int playerBlood) {
        return Math.min(Math.max(0, stored), Math.max(0, 600 - playerBlood));
    }

    public static int duration(int form) {
        return switch (form) {
            case 1 -> 400;
            case 2 -> 90;
            case 3 -> 100;
            case 4 -> 100;
            case 5 -> 140;
            case 6 -> 240;
            default -> 20;
        };
    }

    /**
     * 可吞判定（无村民特例）。
     * <ul>
     *   <li>数值非法 / 已死 → 不可吞；</li>
     *   <li>最大血量 ≤ {@link #FRAGILE_HEALTH} → 满血可吞；</li>
     *   <li>其余目标 → 当前血量低于最大血量的 {@link #EDIBLE_RATIO} 才可吞。</li>
     * </ul>
     */
    public static boolean edible(float health, float maxHealth) {
        if (!Float.isFinite(health) || !Float.isFinite(maxHealth) || health <= 0 || maxHealth <= 0) {
            return false;
        }
        if (maxHealth <= FRAGILE_HEALTH) {
            return true;
        }
        return health < maxHealth * EDIBLE_RATIO;
    }

    /** 可吞判定（村民特例）：村民满血且最大血量不超过 20 时可直接吞，其余走通用规则。 */
    public static boolean edible(float health, float maxHealth, boolean villager) {
        if (villager && Float.isFinite(health) && Float.isFinite(maxHealth)
                && health > 0 && health <= maxHealth && maxHealth <= 20) {
            return true;
        }
        return edible(health, maxHealth);
    }

    public static int flesh(float maxHealth) {
        return Math.clamp((int) Math.ceil(maxHealth * 1.5), 8, 60);
    }

    public static float captureScale(int age) {
        return (float) (1 - .94 * Math.clamp((age - 8) / 30.0, 0, 1));
    }
}