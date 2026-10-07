package xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.entity.LivingEntity;

/**
 * "生前执念"持有者：能被 {@code /zbfavorite} 设置喜欢的物品，
 * 并由 {@code ZbFavoriteItemGoal} 驱动追掉落物/盯手持玩家的演出。
 * <p>
 * 目前实现者：低阶尸兄（含奥图曼等子类）、多首蜈蚣尸兄。
 */
public interface FavoriteItemHolder {

    /** 执念物品 id 列表，逗号分隔（如 "corpseorigin:hair_dryer"）；空 = 无执念。 */
    String favoriteItems();

    /** @param ids 物品注册表 id（要带命名空间），逗号分隔；null/空 = 清除执念 */
    void setFavoriteItems(String ids);

    boolean hasFavoriteItems();

    /** 持有任意执念物品（通常为手持）的生物：会放下敌意凑过去盯着看。 */
    boolean isDistractedBy(LivingEntity entity);
}
