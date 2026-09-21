package xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import xiaoshi2022.corpseorigin.item.MagicianRabbitItem;
import xiaoshi2022.corpseorigin.registry.ModItems;
import java.util.Comparator;
import java.util.EnumSet;

/** Walk up to a dropped plush and chew one toy at a time. */
public class HamBiteToyGoal extends Goal {
    private final HamEntity ham;
    private ItemEntity toy;
    private int nextBite;
    private int timeout;
    public HamBiteToyGoal(HamEntity ham) {
        this.ham = ham;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() {
        if (ham.isOrderedToSit() || ham.getTarget() != null || ham.tickCount < nextBite) return false;
        toy = ham.level().getEntitiesOfClass(ItemEntity.class, ham.getBoundingBox().inflate(8),
                item -> item.isAlive() && item.getItem().is(ModItems.MAGICIAN_RABBIT) && ham.hasLineOfSight(item))
                .stream().min(Comparator.comparingDouble(ham::distanceToSqr)).orElse(null);
        return toy != null;
    }
    @Override public void start() { timeout = 100; }
    @Override public boolean canContinueToUse() {
        return timeout > 0 && !ham.isOrderedToSit() && ham.getTarget() == null
                && toy != null && toy.isAlive() && toy.getItem().is(ModItems.MAGICIAN_RABBIT);
    }
    @Override public void tick() {
        timeout--;
        ham.getLookControl().setLookAt(toy, 30, 30);
        ham.getNavigation().moveTo(toy, 1.1);
        if (ham.distanceToSqr(toy) < 1.5 && ham.hasLineOfSight(toy)) {
            var remaining = toy.getItem().copy();
            if (MagicianRabbitItem.bite(ham, remaining)) {
                if (remaining.isEmpty()) toy.discard();
                else toy.setItem(remaining);
            }
            timeout = 0;
            nextBite = ham.tickCount + 30;
        }
    }
    @Override public void stop() {
        ham.getNavigation().stop();
        toy = null;
        nextBite = Math.max(nextBite, ham.tickCount + 20);
    }
}
