package xiaoshi2022.corpseorigin.entity.evolution;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.entity.FavoriteItemHolder;

import java.util.EnumSet;

/**
 * 生前执念：尸兄会追着生前喜欢的物品跑——
 * 掉落物（如掉在地上的吹风机）或手持该物品的玩家，凑近后痴痴盯着看。
 * 原著名场面：邻居女尸兄生前爱吹头发，变异后追着吹风机不放，被白小飞利用。
 * <p>
 * 注意力转移机制：手持执念物品的玩家已在 {@code registerGoals} 的目标选择器里豁免敌意，
 * 所以这个 Goal 一旦激活，尸兄既不攻击也不游荡，一路尾随——直到物品消失或玩家收手。
 * <p>
 * 泛型：任何 {@code PathfinderMob} 实现 {@link FavoriteItemHolder} 都能挂这个 Goal
 * （低阶尸兄、多首蜈蚣尸兄等）。
 */
public final class ZbFavoriteItemGoal<T extends PathfinderMob & FavoriteItemHolder> extends Goal {
    private static final double SEARCH_RANGE = 16.0;

    private final T zb;
    private LivingEntity targetHolder;   // 手持执念物品的玩家
    private ItemEntity targetDrop;       // 执念物品掉落物
    private int stareTicks;              // 凑近后发呆/发声的计时

    public ZbFavoriteItemGoal(T zb) {
        this.zb = zb;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!zb.hasFavoriteItems()) return false;
        // 正在战斗就不犯花痴（被打会经 HurtByTarget 反击，那时放下执念）
        if (zb.getTarget() != null) return false;
        return findTarget();
    }

    @Override
    public boolean canContinueToUse() {
        if (!zb.hasFavoriteItems() || zb.getTarget() != null) return false;
        if (targetHolder != null)
            return targetHolder.isAlive() && !targetHolder.isSpectator()
                    && zb.distanceToSqr(targetHolder) < SEARCH_RANGE * SEARCH_RANGE * 4
                    && zb.isDistractedBy(targetHolder);
        if (targetDrop != null)
            return targetDrop.isAlive() && !targetDrop.hasPickUpDelay()
                    && zb.distanceToSqr(targetDrop) < SEARCH_RANGE * SEARCH_RANGE * 4
                    && matchesFavorite(targetDrop.getItem().getItem());
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        zb.getNavigation().stop();
        stareTicks = 0;
    }

    @Override
    public void stop() {
        targetHolder = null;
        targetDrop = null;
        zb.getNavigation().stop();
    }

    @Override
    public void tick() {
        Vec3 target = targetHolder != null ? targetHolder.position()
                : targetDrop != null ? targetDrop.position() : null;
        if (target == null) return;
        double distSqr = zb.distanceToSqr(target);
        if (distSqr > 2.25) { // 1.5 格外：一路小跑凑过去
            zb.getNavigation().moveTo(target.x, target.y, target.z, 1.1);
            stareTicks = 0;
        } else {              // 凑近了：驻足凝视，偶尔发出呜咽声
            zb.getNavigation().stop();
            if (targetHolder != null) zb.getLookControl().setLookAt(targetHolder);
            else zb.getLookControl().setLookAt(target.x, target.y + 0.5, target.z);
            if (++stareTicks % 60 == 0) zb.playAmbientSound();
        }
    }

    /** 找最近的执念目标：手持玩家优先（戏剧性更强），其次掉落物。 */
    private boolean findTarget() {
        targetHolder = null;
        targetDrop = null;
        double best = SEARCH_RANGE * SEARCH_RANGE;
        for (var p : zb.level().players()) {
            if (!p.isAlive() || p.isSpectator() || !zb.isDistractedBy(p)) continue;
            double d = zb.distanceToSqr(p);
            if (d < best) { best = d; targetHolder = p; }
        }
        if (targetHolder != null) return true;
        var drops = zb.level().getEntitiesOfClass(ItemEntity.class,
                zb.getBoundingBox().inflate(SEARCH_RANGE),
                item -> matchesFavorite(item.getItem().getItem()));
        for (var item : drops) {
            double d = zb.distanceToSqr(item);
            if (d < best) { best = d; targetDrop = item; }
        }
        return targetDrop != null;
    }

    private boolean matchesFavorite(net.minecraft.world.item.Item item) {
        if (!zb.hasFavoriteItems()) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
        for (String fav : zb.favoriteItems().split(",")) {
            if (id.equals(fav.trim())) return true;
        }
        return false;
    }
}
