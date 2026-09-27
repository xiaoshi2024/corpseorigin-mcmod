package xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import xiaoshi2022.corpseorigin.character.MortalCharacter;
import xiaoshi2022.corpseorigin.skill.chapter.CloneCaster;

import java.util.EnumSet;

/**
 * 克隆分身的"角色技能 AI"：有角色的分身（或任何 BOSS 分身）盯着目标时，
 * 周期性按自己保存的角色释放对应招式，具体招式选择在 {@link CloneCaster}。
 * <p>
 * 频率故意放得比玩家手动慢：AI 没有走位成本，2 秒一个技能会把玩家碾压。
 */
public class CloneSkillGoal extends Goal {
    private final CloneAvatarEntity mob;
    /** 距下次尝试放技能还有多少 tick */
    private int cooldown = 40;

    public CloneSkillGoal(CloneAvatarEntity mob) {
        this.mob = mob;
        // 技能不占用移动标志，追击、近战和飞行期间仍然可以施放。
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    /** 有角色（非凡人）的苏醒分身，且手上有目标时才尝试 */
    @Override
    public boolean canUse() {
        return this.mob.isActive()
                && this.mob.getTarget() != null
                && this.mob.getTarget().isAlive()
                && hasRole();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    private boolean hasRole() {
        String role = this.mob.getBodyRole();
        return role != null;
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        // CloneCaster 内部还有每招独立的冷却，没招可放时很快返回
        if (CloneCaster.cast(this.mob)) {
            this.cooldown = 30;
        } else {
            this.cooldown = 10;
        }
    }

    @Override
    public void stop() {
        this.cooldown = 40;
    }
}
