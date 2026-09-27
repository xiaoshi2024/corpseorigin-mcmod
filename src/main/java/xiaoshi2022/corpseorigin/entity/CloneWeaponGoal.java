package xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * 克隆分身的"剑气武器 AI"：手持巨阙 / 血翼黑刃时和目标拉开距离游走，
 * 有视线就按玩家同款冷却射剑气；贴脸时停下来让位给近战 goal。
 * <p>
 * 走位逻辑参考原版骷髅的远程 AI，但更克制 —— 分身拿的是近战剑气而不是弓，
 * 交战距离故意压在 12~15 格，逼玩家有反打窗口。
 */
public class CloneWeaponGoal extends Goal {

    private final CloneAvatarEntity mob;
    /** 后撤/走位节奏 */
    private int strafeDir = 1;
    private int strafeTimer;

    public CloneWeaponGoal(CloneAvatarEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private ItemStack weapon() {
        ItemStack stack = this.mob.getMainHandItem();
        return CloneWeaponArts.isRangedWeapon(stack) ? stack : ItemStack.EMPTY;
    }

    @Override
    public boolean canUse() {
        return this.mob.isActive() && this.mob.isHostile()
                && this.mob.getTarget() != null
                && this.mob.getTarget().isAlive()
                && !weapon().isEmpty() && !this.mob.isOrganFlying()
                && this.mob.distanceToSqr(this.mob.getTarget()) > CloneWeaponArts.MELEE_LIMIT * CloneWeaponArts.MELEE_LIMIT;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void stop() {
        this.strafeTimer = 0;
        this.mob.getMoveControl().strafe(0F, 0F);
    }

    @Override
    public void tick() {
        var target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        ItemStack stack = weapon();
        if (stack.isEmpty()) {
            return;
        }

        double distSqr = this.mob.distanceToSqr(target);
        int range = CloneWeaponArts.preferredRange(stack);
        boolean lineOfSight = this.mob.hasLineOfSight(target);

        this.mob.getLookControl().setLookAt(target, 30F, 30F);

        // 贴脸时不抢近战 goal 的移动，转身挥砍交给近战
        boolean inMelee = distSqr <= CloneWeaponArts.MELEE_LIMIT * CloneWeaponArts.MELEE_LIMIT;

        if (!inMelee) {
            if (distSqr > (range - 2) * (range - 2) || !lineOfSight) {
                // 太远或被挡住：追
                this.mob.getNavigation().moveTo(target, 1.15);
            } else if (distSqr < 16) {
                // 太近：后退半步
                double awayX = this.mob.getX() + (this.mob.getX() - target.getX()) * 0.6;
                double awayZ = this.mob.getZ() + (this.mob.getZ() - target.getZ()) * 0.6;
                this.mob.getNavigation().moveTo(awayX, this.mob.getY(), awayZ, 1.0);
            } else {
                // 中距离横向绕步，不站桩当靶子
                if (--this.strafeTimer <= 0) {
                    this.strafeTimer = 20 + this.mob.getRandom().nextInt(20);
                    this.strafeDir = this.mob.getRandom().nextBoolean() ? 1 : -1;
                }
                this.mob.getNavigation().stop();
                this.mob.getMoveControl().strafe(0F, this.strafeDir * 0.35F);
            }
        }

        // Firing is ticked independently so melee, flight and pathfinding cannot starve weapon skills.
    }
}
