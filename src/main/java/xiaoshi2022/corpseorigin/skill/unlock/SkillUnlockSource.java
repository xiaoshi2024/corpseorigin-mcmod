package xiaoshi2022.corpseorigin.skill.unlock;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import xiaoshi2022.corpseorigin.component.PlayerRelicComponent;

import java.util.function.Predicate;

/**
 * 技能的「获取式」解锁来源。
 * <p>
 * 声明了这个来源的技能，玩家<b>满足条件就免费直接学会</b>，不花进化点、不检查前置与等级
 * —— 和技能树那条「花点点亮」的路并存（双通道）。所以：
 * <ul>
 *   <li>{@code getUnlockSources()} 为空 → 纯技能树技能，行为和以前完全一致；</li>
 *   <li>非空 → 除了技能树，玩家拿到对应获取物时也会自动学会。</li>
 * </ul>
 * 典型用法：
 * <pre>{@code
 * // 黑金心脏技能：拿到「黑金心脏」这个器官就解锁
 * public List<SkillUnlockSource> getUnlockSources() {
 *     return List.of(SkillUnlockSource.relic(BlackGoldHeartSkill.RELIC_ID));
 * }
 *
 * // 哈姆大炮：获得哈姆宠物后解锁（宠物尚未实装，用 custom 先挂上条件）
 * return List.of(SkillUnlockSource.custom("ham_pet",
 *         Component.translatable("pet.corpseorigin.ham"),
 *         player -> HamPetManager.hasPet(player)));
 * }</pre>
 *
 * <p><b>注意</b>：{@link #describe()} 会在客户端（技能界面）被调用，所以实现里不要碰服务端状态。
 */
public interface SkillUnlockSource {

    /** 唯一 id，用于调试输出与去重，形如 {@code relic:black_gold_heart} */
    String id();

    /** 给玩家看的「需要什么」 */
    Component describe();

    /** 玩家当前是否满足 */
    boolean isSatisfiedBy(ServerPlayer player);

    // ==================== 现成实现 ====================

    /** Remember an encounter independently of the items needed to activate the skill. */
    static <T extends net.minecraft.world.entity.LivingEntity> SkillUnlockSource encounter(
            String encounterId, Class<T> entityClass, double radius, Component description) {
        if (!Double.isFinite(radius) || radius <= 0 || radius > 64)
            throw new IllegalArgumentException("Encounter radius must be in (0, 64]");
        return custom("encounter:" + encounterId, description, player -> {
            if (!player.isAlive() || player.isSpectator()) return false;
            var journal = xiaoshi2022.corpseorigin.growth.SurvivalGrowth.JOURNAL;
            String key = "encounter:" + encounterId;
            if (player.getAttachedOrCreate(journal).getBooleanOr(key, false)) return true;
            boolean seen = !player.level().getEntitiesOfClass(entityClass,
                    player.getBoundingBox().inflate(radius), entity -> entity.isAlive()
                            && player.distanceToSqr(entity) <= radius * radius
                            && player.hasLineOfSight(entity)).isEmpty();
            if (seen) {
                var data = player.getAttachedOrCreate(journal).copy();
                data.putBoolean(key, true);
                player.setAttached(journal, data);
            }
            return seen;
        });
    }

    /**
     * 需要持有某个器官 / 收藏品（{@link PlayerRelicComponent} 里的记录，不占背包）。
     */
    static SkillUnlockSource relic(String relicId) {
        return new SkillUnlockSource() {
            @Override
            public String id() {
                return "relic:" + relicId;
            }

            @Override
            public Component describe() {
                return Component.translatable("relic.corpseorigin." + relicId);
            }

            @Override
            public boolean isSatisfiedBy(ServerPlayer player) {
                return PlayerRelicComponent.has(player, relicId);
            }
        };
    }

    /**
     * 需要背包（含装备栏）里有某个物品。
     * <p>
     * 和 {@link #relic} 的区别：这条路要求物品<b>实际在玩家身上</b>，丢掉就失去解锁资格
     * （技能已经学会的不会退回，但没学会时不再满足）。适合「拿着法宝才开窍」这类设定。
     */
    static SkillUnlockSource item(String itemId, Item item) {
        return new SkillUnlockSource() {
            @Override
            public String id() {
                return "item:" + itemId;
            }

            @Override
            public Component describe() {
                return item.getDefaultInstance().getHoverName();
            }

            @Override
            public boolean isSatisfiedBy(ServerPlayer player) {
                return player.getInventory().contains(stack -> stack.is(item))
                        || player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
            }
        };
    }

    /**
     * 自定义条件 —— 宠物、剧情进度、击杀数等任何需要现算的东西都走这个。
     *
     * @param unlockId 唯一 id，例如 {@code ham_pet}
     * @param describe 给玩家看的「需要什么」
     * @param test     判定，必须能在服务端任意时刻安全求值
     */
    static SkillUnlockSource custom(String unlockId, Component describe, Predicate<ServerPlayer> test) {
        return new SkillUnlockSource() {
            @Override
            public String id() {
                return unlockId;
            }

            @Override
            public Component describe() {
                return describe;
            }

            @Override
            public boolean isSatisfiedBy(ServerPlayer player) {
                return test.test(player);
            }
        };
    }
}
