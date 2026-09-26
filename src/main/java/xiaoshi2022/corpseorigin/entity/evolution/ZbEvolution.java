package xiaoshi2022.corpseorigin.entity.evolution;

import net.minecraft.server.level.ServerLevel;
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

    /** 临界突破基础成功率。 */
    public static final float BREAKTHROUGH_CHANCE = 0.15F;
    /** 每次突破失败后叠加的成功率。 */
    public static final float BREAKTHROUGH_BONUS = 0.10F;

    private ZbEvolution() {}

    public static int energyValue(LivingEntity prey) {
        if (ZombieKin.isZombieKin(prey)) return 4;            // 同类相食
        if (prey instanceof Player) return 5;                // 人类玩家
        if (prey instanceof AbstractVillager) return 3;      // 村民
        if (prey instanceof Animal) return 1;                // 动物
        return 1;                                            // 其他活体
    }

    public static int thresholdForLevel(int level) {
        return Math.max(1, level) * 10;
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

        if (current < 5) {
            // 正常进化
            self.setFleshEnergy(self.getFleshEnergy() - thresholdForLevel(current));
            evolve(self, level, false);
            return true;
        }

        // 5 级临界：概率超脱
        float chance = BREAKTHROUGH_CHANCE + BREAKTHROUGH_BONUS * self.getBreakthroughFailures();
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
}
