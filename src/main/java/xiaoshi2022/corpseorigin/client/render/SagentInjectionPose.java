package xiaoshi2022.corpseorigin.client.render;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.item.SagentItem;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes;

/** Shared two-second timeline for the injecting hand and needle. */
public final class SagentInjectionPose {
    private SagentInjectionPose() {}

    public static boolean active(Entity actor, HumanoidArm arm, ItemStack stack) {
        if (!(stack.getItem() instanceof SagentItem item) || SagentItem.EMPTY.equals(item.variant())) return false;
        String action = actor.getAttachedOrCreate(ChapterScenes.ACTION);
        return action.equals(arm == HumanoidArm.RIGHT ? "s_agent_press_right" : "s_agent_press_left")
                && actor.getAttachedOrCreate(ChapterScenes.UNTIL) > actor.level().getGameTime();
    }

    public static float approach(Entity actor, float partialTick) {
        float elapsed = 40 - (actor.getAttachedOrCreate(ChapterScenes.UNTIL) - actor.level().getGameTime()) + partialTick;
        float t = Math.clamp(elapsed / 10f, 0f, 1f);
        return t * t * (3 - 2 * t);
    }
}
