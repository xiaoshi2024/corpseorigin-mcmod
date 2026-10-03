package xiaoshi2022.corpseorigin.entity;

import java.util.Map;

import net.minecraft.world.entity.EntityType;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/**
 * 尸兄系实体的基础境界评级表（Jade/WAILA 联动显示用，参照尸兄原著实力定位）。
 * <p>
 * 已实现 {@link RealmRated} 的实体（达摩/天博士/青蛙/壁虎/蚊群核心）不走本表 ——
 * 它们在自己的类里声明设定境界。这里只收录"没写实体类专用评级"的普通尸兄：
 * <ul>
 *   <li>低阶尸兄 → 人3（刚尸化的低级杂兵）</li>
 *   <li>凹凸曼尸兄 → 人1（原皮虚弱态，不主动攻击）</li>
 *   <li>尸虫 / 尸蛆 → 人1（寄生虫）</li>
 *   <li>红火蚁 → 人3、子弹蚁 → 人4（兵蚁等级差）</li>
 *   <li>CoCo 尸兄 → 人3（一阶段躺尸），合体二阶段 → 地1（大叔合体强化）</li>
 *   <li>多头尸虫 → 地2（精英级虫巢首领）</li>
 *   <li>吸血蝙蝠 → 人2</li>
 * </ul>
 * 世界威胁等级（{@code WorldThreatManager}）只改属性数值，不改显示评级 —— 看到的是血统出身。
 */
public final class ZbRatings {

    private ZbRatings() {}

    private static final Map<EntityType<?>, Integer> BASE_REALM = Map.ofEntries(
            Map.entry(ModEntities.LOWER_LEVEL_ZB, 3),
            Map.entry(ModEntities.AOTUMAN_ZB, 1),
            Map.entry(ModEntities.ZB_WORM, 1),
            Map.entry(ModEntities.CORPSE_MAGGOT, 1),
            Map.entry(ModEntities.RED_FIRE_ANT, 3),
            Map.entry(ModEntities.BULLET_ANT, 4),
            Map.entry(ModEntities.COCO_ZOMBIE, 3),
            Map.entry(ModEntities.COCO_ZOMBIE_X, 6),
            Map.entry(ModEntities.MULTI_HEAD_CORPSE_WORM, 7),
            Map.entry(ModEntities.VAMPIRE_BAT, 2));

    /** 基础境界等级（人1=1 … 地2=7 … 神上=20）；未评级实体返回 -1（不显示境界行） */
    public static int of(EntityType<?> type) {
        return BASE_REALM.getOrDefault(type, -1);
    }
}
