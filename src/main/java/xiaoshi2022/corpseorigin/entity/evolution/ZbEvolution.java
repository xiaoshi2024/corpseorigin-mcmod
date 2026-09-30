package xiaoshi2022.corpseorigin.entity.evolution;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.registry.ModSounds;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

/**
 * 低阶尸兄吸食血肉进化规则：
 * <ul>
 *   <li>击杀活体按猎物类型获得血肉能量：动物 1、村民 3、同类 4、玩家 5；</li>
 *   <li>能量攒满"当前等级 × 10"触发进化，多余能量顺延到下一级；</li>
 *   <li>1-5 级为正常进化；5 级之后是"超脱临界"——每次 15% 成功率，
 *       每失败一次下次 +10%，直到成功为止；</li>
 *   <li>每次临界突破成功都会随机突变一个器官（见 {@link ZbOrganGrowth}）。</li>
 * </ul>
 */
public final class ZbEvolution {

    public static final int MAX_LEVEL = 10;

    /**
     * 正常进化的等级上限：过了它就得靠"超脱临界"（突破）才能继续涨。
     * <p>
     * 也是"有没有突变器官"的分界线 —— 突破一次长一个，所以 6 级 = 1 个器官。
     */
    public static final int BREAKTHROUGH_LEVEL = 5;

    /** 临界突破基础成功率。 */
    public static final float BREAKTHROUGH_CHANCE = 0.15F;
    /** 每次突破失败后叠加的成功率。 */
    public static final float BREAKTHROUGH_BONUS = 0.10F;

    /** 一个游戏日的游戏刻数。 */
    private static final long TICKS_PER_DAY = 24000L;

    private ZbEvolution() {}

    public static int energyValue(LivingEntity prey) {
        if (ZombieKin.isZombieKin(prey)) return 4;            // 同类相食
        if (prey instanceof Player) return 5;                // 人类玩家
        if (prey instanceof AbstractVillager) return 3;      // 村民
        if (prey instanceof Animal) return 1;                // 动物
        return 1;                                            // 其他活体
    }

    public static int thresholdForLevel(int level) {
        int perLevel = xiaoshi2022.corpseorigin.config.CorpseConfig.get().spawn.zbEvolutionEnergyPerLevel;
        return Math.max(1, level) * Math.max(1, perLevel);
    }

    /**
     * 正常进化的等级上限（可配置，默认 5）：过了它就得靠"超脱临界"概率突破。
     * <p>
     * {@link #BREAKTHROUGH_LEVEL} 常量保留作默认值；GUI / 配置文件里改
     * {@code spawn.zbEvolutionBreakthroughLevel} 生效。下界钳到 1（低于 1 没意义）、
     * 上界钳到 {@code MAX_LEVEL - 1}（顶满后没有突破的意义）。
     */
    public static int breakthroughLevel() {
        int configured = xiaoshi2022.corpseorigin.config.CorpseConfig.get().spawn.zbEvolutionBreakthroughLevel;
        return Math.max(1, Math.min(MAX_LEVEL - 1, configured));
    }

    /** 击杀回调：加能量，满足条件就连续进化（一次大餐理论上可能连升多级）。 */
    public static void onKill(LowerLevelZbEntity self, LivingEntity prey) {
        if (!(self.level() instanceof ServerLevel level)) return;
        self.setFleshEnergy(self.getFleshEnergy() + energyValue(prey));
        int guard = 0;
        while (guard++ < MAX_LEVEL
                && self.getEvolutionLevel() < MAX_LEVEL
                && self.getFleshEnergy() >= thresholdForLevel(self.getEvolutionLevel())) {
            if (!tryEvolve(self, level)) break;
        }
    }

    /** @return 这一趟是否真的完成了一次进化 */
    private static boolean tryEvolve(LowerLevelZbEntity self, ServerLevel level) {
        int current = self.getEvolutionLevel();
        if (current >= MAX_LEVEL) {
            self.setFleshEnergy(0);
            return false;
        }

        var config = xiaoshi2022.corpseorigin.config.CorpseConfig.get().spawn;
        if (current < breakthroughLevel()) {
            // 正常进化
            self.setFleshEnergy(self.getFleshEnergy() - thresholdForLevel(current));
            evolve(self, level, false);
            return true;
        }

        // 临界线之上：概率超脱（基础率 + 每次失败叠加，均可在配置里改）
        float chance = Math.max(0F, config.zbEvolutionBreakthroughChance)
                + Math.max(0F, config.zbEvolutionBreakthroughBonus) * self.getBreakthroughFailures();
        if (self.getRandom().nextFloat() < chance) {
            self.setFleshEnergy(self.getFleshEnergy() - thresholdForLevel(current));
            self.setBreakthroughFailures(0);
            evolve(self, level, true);
            return true;
        }

        // 突破失败：能量清空，累积下次成功率
        self.setFleshEnergy(0);
        self.setBreakthroughFailures(self.getBreakthroughFailures() + 1);
        return false;
    }

    private static void evolve(LowerLevelZbEntity self, ServerLevel level, boolean critical) {
        self.setEvolutionLevel(self.getEvolutionLevel() + 1);
        if (critical) {
            ZbOrganGrowth.tryMutateOrgan(self);
        }
        // 进化反馈：音效 + 气血粒子
        self.playSound(ModSounds.GROUND_CHI, 1.2F, critical ? 0.7F : 1.0F);
        QiEffects.burst(level, self.getX(), self.getY() + 1.0, self.getZ(),
                critical ? 0xc0182a : 0x8ce06a, critical ? 18 : 10, 0.5);
    }

    // ==================== 自然生成的等级 ====================

    /**
     * 自然生成的尸兄该是多少级：<b>越后期越强</b>。
     * <p>
     * 先算出当天的等级上限：{@code 上限 = 1 + 游戏日 / spawn.daysPerEvolutionLevel}，
     * 封顶 {@code spawn.maxEvolutionLevel}；再在上限之内按权重抽一个等级 ——
     * 权重<b>以当前上限为峰</b>往下衰减，也就是世界的平均强度随天数整体抬升，
     * 同时总有一小撮更弱的，不会清一色。
     * <p>
     * 所以第 0 天全是人1（和原来一样），之后每一档台阶都往上移一层；
     * {@code spawn.levelDecay} 控制分布有多集中在上限附近（越大越集中）。
     * <p>
     * 把 {@code spawn.evolutionLevelRamp} 关掉就一律人1（回到"高阶只能靠吃血肉突破"）。
     * 数值全在 {@code config/corpseorigin.json} 的 {@code spawn} 段，不用改代码。
     *
     * @param dayTime 主世界日历刻数（睡觉跨天也会前进）
     * @return 1 ~ {@code spawn.maxEvolutionLevel}
     */
    public static int rollSpawnLevel(RandomSource random, long dayTime) {
        var config = xiaoshi2022.corpseorigin.config.CorpseConfig.get().spawn;
        if (!config.evolutionLevelRamp) {
            return 1;
        }
        long days = Math.max(0L, dayTime) / TICKS_PER_DAY;
        int cap = (int) Math.min(config.maxEvolutionLevel,
                1L + days / Math.max(1, config.daysPerEvolutionLevel));
        cap = Math.max(1, Math.min(MAX_LEVEL, cap));

        double decay = Math.max(1.1, config.levelDecay);
        double[] weights = new double[cap];
        double total = 0.0;
        for (int i = 0; i < cap; i++) {
            // 离上限越远越稀有：i = cap-1（正好是上限）权重 1，往下每级除以 decay
            weights[i] = 1.0 / Math.pow(decay, cap - 1 - i);
            total += weights[i];
        }

        double roll = random.nextDouble() * total;
        for (int i = 0; i < cap; i++) {
            roll -= weights[i];
            if (roll <= 0.0) {
                return i + 1;
            }
        }
        return cap;
    }
}
