package xiaoshi2022.corpseorigin.clone;

import net.minecraft.util.Mth;

/**
 * 布尔值平滑动画器（移植自 Sync 的 BooleanAnimator）。
 * 每 tick 朝目标值推进 stepDelta，渲染时用 partialTick 在两帧之间插值，
 * 让克隆仓的舱门像铁活板门一样平滑开合，而不是瞬间切换。
 */
public class DoorAnimator {
    private boolean value;
    private float progress;
    private float lastProgress;
    private final float stepDelta;

    public DoorAnimator(boolean value) {
        this(value, 0.1F);
    }

    public DoorAnimator(boolean value, float stepDelta) {
        this.value = value;
        // 初始化为目标值，避免刚加载方块时门播放一遍开合动画
        this.progress = value ? 1.0F : 0.0F;
        this.lastProgress = this.progress;
        this.stepDelta = stepDelta;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public float getProgress(float partialTick) {
        return Mth.lerp(partialTick, this.lastProgress, this.progress);
    }

    public void step() {
        this.lastProgress = this.progress;
        if (!this.value && this.progress > 0.0F) {
            this.progress = Math.max(this.progress - this.stepDelta, 0.0F);
        } else if (this.value && this.progress < 1.0F) {
            this.progress = Math.min(this.progress + this.stepDelta, 1.0F);
        }
    }
}
