package xiaoshi2022.corpseorigin.entity;

/**
 * 可评定境界的实体 —— Jade（WAILA）准星联动显示"境界：XX"。
 * <p>
 * 返回值是境界表的绝对等级（1~20，见 {@code EvolutionTier}）：
 * 人1-4 = 1~4、地1-4 = 5~8、天 = 9、神/超神/神上 = 10~20。
 * NPC 与精英怪按各自设定实现本接口，数值同时是它们"抗性等级"的设计口径。
 */
public interface RealmRated {
    /** 境界表绝对等级，0 或负数表示未评定（Jade 不显示境界行） */
    int corpseRealmLevel();
}
