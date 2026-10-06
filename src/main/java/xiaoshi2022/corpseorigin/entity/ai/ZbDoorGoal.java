package xiaoshi2022.corpseorigin.entity.ai;

import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRules;

import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.evolution.ZbEvolution;

/**
 * 尸兄的门交互 AI（挂在原版 DoorInteractGoal 体系上，只认木门，铁门两档都无解）：
 * <ul>
 *   <li>低阶（进化等级 &lt; 临界线 {@link ZbEvolution#breakthroughLevel()}）：像原版僵尸一样"敲门"——
 *       贴着门咣咣砸（挥手 + 砸门声，每秒一记），带裂纹进度砸满 240t（≈12 秒）后破门而入；
 *       破门受 mobGriefing 游戏规则约束（原版 BreakDoorGoal 同款检查），不设难度卡——尸兄围家是常态演出。</li>
 *   <li>高阶（进化等级 ≥ 临界线，超脱临界后）：学聪明了，会"开门"——贴近直接拉开门走进去，门保持敞开。</li>
 * </ul>
 * 进化等级运行时可变（吃血肉突破升级），模式在 start() 时按当时的等级冻结，砸到一半升阶不会中途换招。
 */
public class ZbDoorGoal extends DoorInteractGoal {
    /** 破门耗时（tick），与原版僵尸一致：240t ≈ 12 秒，给屋主留反应时间 */
    private static final int DOOR_BREAK_TIME = 240;

    private final LowerLevelZbEntity zb;
    /** start() 时冻结的模式：true=开门（高阶），false=敲门破门（低阶） */
    private boolean openMode;
    private int breakTime;
    private int lastBreakProgress = -1;

    public ZbDoorGoal(LowerLevelZbEntity zb) {
        super(zb);
        this.zb = zb;
    }

    private boolean isHighTier() {
        return zb.getEvolutionLevel() >= ZbEvolution.breakthroughLevel();
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) return false;
        if (isHighTier()) return true;
        // 低阶：破门受 mobGriefing 约束（原版 BreakDoorGoal 同款），门已敞开则无事可做
        return getServerLevel(zb).getGameRules().get(GameRules.MOB_GRIEFING) && !isOpen();
    }

    @Override
    public boolean canContinueToUse() {
        if (openMode) {
            return super.canContinueToUse(); // 走过门板为止
        }
        return breakTime <= DOOR_BREAK_TIME
                && !isOpen()
                && doorPos.closerToCenterThan(zb.position(), 2.0);
    }

    @Override
    public void start() {
        super.start();
        openMode = isHighTier();
        breakTime = 0;
        lastBreakProgress = -1;
        zb.setDoorInteracting(true); // 交互期间暂停爬墙，否则贴门撞门判定会被爬墙系统顶飞
        if (openMode) setOpen(true);
    }

    @Override
    public void stop() {
        if (!openMode) {
            zb.level().destroyBlockProgress(zb.getId(), doorPos, -1); // 清掉裂纹进度
        }
        zb.setDoorInteracting(false);
    }

    @Override
    public void tick() {
        super.tick(); // DoorInteractGoal：走过门板即结束
        if (openMode) return;

        // 敲门演出：每 20t 一记"僵尸砸木门"声 + 挥手
        if (zb.getRandom().nextInt(20) == 0) {
            zb.level().levelEvent(1019, doorPos, 0);
            if (!zb.swinging) {
                zb.swing(zb.getUsedItemHand());
            }
        }

        breakTime++;
        int progress = (int) ((float) breakTime / DOOR_BREAK_TIME * 10.0F);
        if (progress != lastBreakProgress) {
            zb.level().destroyBlockProgress(zb.getId(), doorPos, progress);
            lastBreakProgress = progress;
        }

        if (breakTime >= DOOR_BREAK_TIME) {
            zb.level().removeBlock(doorPos, false);
            zb.level().levelEvent(1021, doorPos, 0); // 原版"僵尸破门"声
            zb.level().levelEvent(2001, doorPos, Block.getId(zb.level().getBlockState(doorPos))); // 破坏粒子/声
        }
    }
}
