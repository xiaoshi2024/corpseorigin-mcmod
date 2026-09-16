package xiaoshi2022.corpseorigin.limb;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * 截断来源规则：决定"什么样的攻击能斩断四肢"。
 * <p>
 * 默认只注册 {@link WeaponSeverRule}（剑 / 斧 / 带 WEAPON 组件的武器），想加新来源就实现本接口后
 * {@link LimbSeverRules#register} 一条：
 * <ul>
 *   <li><b>自定义武器</b>：新开一条规则判定你的物品（比改 {@code #swords/#axes} 标签更可控）。</li>
 *   <li><b>炸弹 / 爆炸</b>：直接用现成的 {@link ExplosionSeverRule}。</li>
 *   <li><b>技能手刀 / 斩击</b>：这类是主动触发，不用写规则，直接调
 *       {@link DismembermentLogic#severByHit} 或 {@link DismembermentLogic#severRandom}。</li>
 * </ul>
 */
public interface LimbSeverRule {

    /** 规则标识，建议 "命名空间:名字" 形式；同 id 重复注册会覆盖，方便整合包替换 */
    String id();

    /** 越小越先匹配 */
    default int priority() {
        return 100;
    }

    /**
     * 这次攻击是否属于本规则的截断来源。
     *
     * @param hitPower    {@code max(baseDamage, damageTaken)}，即"这一刀多重"与实际打进去的较大值
     * @param damageTaken 实际造成的伤害（完全挡住 / 免疫时为 0）
     */
    boolean matches(ServerPlayer victim, DamageSource source, float hitPower, float damageTaken);

    /**
     * 截断概率。脆弱期、重击之类的加成都在这里算完。
     *
     * @param slot 已经选定的部位下标
     */
    float chance(ServerPlayer victim, LimbState state, int slot, float hitPower);

    /** 是否跳过概率直接断（爆炸 / 处决类来源） */
    default boolean guaranteed() {
        return false;
    }
}
