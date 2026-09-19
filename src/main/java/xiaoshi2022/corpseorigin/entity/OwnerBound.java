package xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.entity.Entity;

/**
 * 「属于某个主人的身体」—— 蛟龙的节碰撞箱（{@link GuardianPartEntity}）和脱离出来的
 * 尸蛟龙（{@link ZuoFloodLongEntity}）都算这一类。
 * <p>
 * 存在的意义只有一个：主人自己的攻击（近战射线 / 射出去的箭）要能<b>穿过/跳过</b>它，
 * 否则"身体在身前挡着"就永远砍不到敌人（见 {@code ProjectileOwnBodyMixin}）。
 */
public interface OwnerBound {

    /** 这个实体是不是这一份身体的主人（UUID 比对，客户端也能用 —— 那边没有主人实体引用） */
    boolean isOwnedBy(Entity entity);
}
