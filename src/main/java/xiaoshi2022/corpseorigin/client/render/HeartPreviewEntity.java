package xiaoshi2022.corpseorigin.client.render;

import net.minecraft.world.level.Level;
import xiaoshi2022.corpseorigin.entity.SkillConstructEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/** Screen-owned entity with a continuous clock, including while the world is paused. */
public final class HeartPreviewEntity extends SkillConstructEntity {
    private final long startedAt = System.nanoTime();

    public HeartPreviewEntity(Level level) {
        super(ModEntities.BLACK_GOLD_HEART, level);
    }

    public float advancePreviewClock() {
        double ticks = (System.nanoTime() - startedAt) / 50_000_000.0;
        tickCount = (int) ticks;
        // GeckoLib treats exactly 1 as a frozen GUI frame.
        return Math.min(Math.nextDown(1.0F), (float) (ticks - tickCount));
    }
}
