package xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.character.LongYou;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

public interface ZombieKin {

    // ==================== 默认实现 ====================

    default boolean isTrueZombieKin() {
        return true;
    }

    default int getHunger() {
        return 100;
    }

    default boolean isHungry() {
        return getHunger() < 30;
    }

    // ==================== 静态工具方法 ====================

    static boolean isZombieKin(Entity entity) {
        if (entity == null) return false;

        if (entity instanceof ZombieKin kin) {
            return kin.isTrueZombieKin();
        }

        if (entity instanceof Player player) {
            if (!PlayerCorpseComponent.isCorpse(player)) return false;
            return !PlayerCorpseComponent.get(player).isDisguised();
        }

        return false;
    }

    static boolean isSameKin(Entity a, Entity b) {
        return isZombieKin(a) && isZombieKin(b);
    }

    static boolean isNotZombieKin(Entity entity) {
        return !isZombieKin(entity);
    }

    /**
     * 这位是不是"尸王"（龙右）。
     * <p>
     * 认的是身体不是账号：玩家把意识转移进龙右克隆身体、或者龙右分身实体，都算尸王。
     * 尸族对尸王只有敬畏 —— 不主动索敌、被打也不还手（AI 层见 {@code MobZombieKingRespectMixin}，
     * 伤害层见 {@link #canAttack}）。
     */
    static boolean isZombieKing(Entity entity) {
        return entity instanceof LivingEntity living && LongYou.isLongYouBody(living);
    }

    // ==================== 攻击判定 ====================

    /**
     * 判断攻击者是否可以攻击目标
     * <p>
     * 规则：
     * - 目标不是尸族 → 允许
     * - 目标是自己 → 禁止
     * - 攻击者是玩家 → 永远允许（玩家有自主意识）
     * - 攻击者是尸兄生物：
     *   - 反击（被攻击过）→ 允许
     *   - 主动攻击 + 饥饿 → 允许
     *   - 主动攻击 + 不饥饿 → 禁止
     */
    static boolean canAttack(LivingEntity attacker, Entity target) {
        if (attacker == target) return false;

        // ✅ 尸王：尸族不敢对他不敬 —— 主动攻击、被打了还手，一律不允许
        if (isZombieKing(target)) return false;

        // 目标不是尸族 → 可以攻击
        if (!isZombieKin(target)) {
            return true;
        }

        // ==================== 目标是尸族 ====================

        // ✅ 1. 玩家主动攻击 → 永远允许
        if (attacker instanceof Player player) {
            return true;
        }

        // ✅ 2. 尸兄生物攻击
        if (attacker instanceof ZombieKin kin) {
            if (isRetaliating(attacker, target)) return true;
            return kin.isHungry();
        }

        // ✅ 3. 其他攻击者（原版怪物、铁傀儡、其他模组生物）
        //      → 默认允许，不参与尸族互伤规则
        return true;
    }

    /**
     * ✅ 判断攻击者是否在反击目标
     * <p>
     * 检查攻击者的 lastHurtByMob 是否为 target
     */
    static boolean isRetaliating(LivingEntity attacker, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return false;
        }
        LivingEntity lastAttacker = attacker.getLastHurtByMob();
        return lastAttacker == livingTarget;
    }
}