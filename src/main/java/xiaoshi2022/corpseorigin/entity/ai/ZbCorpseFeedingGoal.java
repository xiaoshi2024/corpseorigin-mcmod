package xiaoshi2022.corpseorigin.entity.ai;

import java.util.Comparator;
import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;

import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.evolution.ZbEvolution;
import xiaoshi2022.corpseorigin.growth.CorpseHorror;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

/**
 * 高阶尸兄的"啃同类尸体"AI：饥饿且闲着（无活体目标）时，寻找附近留场的同类尸体，
 * 走过去慢慢啃——每口回饥饿，并按"同类相食"口径涨血肉能量
 * （复用 {@link ZbEvolution} 的进化/突破链路），啃空或吃撑为止。
 * 低阶尸兄不啃（进化等级 &lt; 临界线的不够格，见 {@link ZbEvolution#breakthroughLevel()}）。
 */
public final class ZbCorpseFeedingGoal extends Goal {
    /** 搜尸半径（格） */
    private static final double SEARCH_RANGE = 16.0;
    /** 每口耗时（tick），约 3 秒一口 */
    private static final int BITE_TICKS = 60;
    /** 每口回复的饥饿值（与吞食活人一致） */
    private static final int HUNGER_PER_BITE = 20;

    private final LowerLevelZbEntity zb;
    private LowerLevelZbEntity corpse;
    private int chew;
    private int nextScanTick;

    public ZbCorpseFeedingGoal(LowerLevelZbEntity zb) {
        this.zb = zb;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** 饿了、没有活体要打、且进化等级达到啃尸门槛（GUI"尸兄饥饿"页可调）时才会去啃尸体 */
    private boolean eligible() {
        if (zb.isDeadOrDying() || zb.isCorpse()) return false;
        int minLevel = xiaoshi2022.corpseorigin.config.CorpseConfig.get().spawn.zbHunger.feedingMinLevel;
        if (zb.getEvolutionLevel() < minLevel) return false;
        if (zb.getTarget() != null) return false;
        return zb.isHungry();
    }

    @Override
    public boolean canUse() {
        if (!eligible() || zb.tickCount < nextScanTick) return false;
        nextScanTick = zb.tickCount + 20; // 扫描节流：每秒一次
        corpse = zb.level().getEntitiesOfClass(LowerLevelZbEntity.class,
                        zb.getBoundingBox().inflate(SEARCH_RANGE),
                        e -> e != zb && e.isCorpse() && !e.isRemoved())
                .stream().min(Comparator.comparingDouble(zb::distanceToSqr)).orElse(null);
        return corpse != null;
    }

    @Override
    public boolean canContinueToUse() {
        return eligible() && corpse != null && corpse.isCorpse() && !corpse.isRemoved()
                && zb.distanceToSqr(corpse) < 24 * 24
                && zb.getHunger() < 100; // 吃撑为止
    }

    @Override
    public void start() {
        chew = 0;
        zb.getNavigation().moveTo(corpse, 1.0);
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        if (corpse == null) return;
        zb.getLookControl().setLookAt(corpse, 30, 30);
        if (zb.distanceToSqr(corpse) > 2.25) { // 1.5 格外：继续凑近
            if (zb.getNavigation().isDone()) zb.getNavigation().moveTo(corpse, 1.0);
            return;
        }
        zb.getNavigation().stop();
        chew++;
        if (chew % 15 == 0 && zb.level() instanceof ServerLevel level) {
            CorpseHorror.blood(level, corpse.position().add(0, .5, 0), 4); // 啃食血粒子
            zb.playSound(SoundEvents.GENERIC_EAT.value(), .8F, .6F);
        }
        if (chew >= BITE_TICKS) {
            chew = 0;
            if (!(zb.level() instanceof ServerLevel level)) return;
            if (corpse.consumeBite()) {
                zb.setHunger(Math.min(100, zb.getHunger() + HUNGER_PER_BITE));
                int before = zb.getEvolutionLevel();
                ZbEvolution.onKill(zb, corpse); // 同类相食口径 +4 血肉能量，复用进化/突破链路
                if (zb.getEvolutionLevel() > before) {
                    QiEffects.burst(level, zb.getX(), zb.getY() + 1.2, zb.getZ(), 0x8ce06a, 12, 0.3);
                    zb.playSound(xiaoshi2022.corpseorigin.registry.ModSounds.GROUND_CHI, 1.0F, .7F);
                }
            }
        }
    }

    @Override
    public void stop() {
        zb.getNavigation().stop();
        corpse = null;
    }
}
