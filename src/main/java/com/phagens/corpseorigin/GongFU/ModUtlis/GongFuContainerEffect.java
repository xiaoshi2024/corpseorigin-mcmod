package com.phagens.corpseorigin.GongFU.ModUtlis;

import com.phagens.corpseorigin.GongFU.GongFaZL.BaseGongFaItem;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 修行容器特殊物品效果配置 — 注册表驱动 + 多效果自由组合
 *
 * <h3>三种匹配方式</h3>
 * <pre>
 * // 1. 普通物品（按 Item 类型匹配）
 * new GongFuContainerEffect(() -> Moditems.ORDINARY_ZB_EYE.get())
 *
 * // 2. 功法物品（按 NBT 中的 typeId/rarity/ceng 匹配）
 * GongFuContainerEffect.forGongFa("qi_jia_shu", 3, "copy_5")
 *
 * // 3. 自定义 Predicate（任意复杂匹配逻辑）
 * new GongFuContainerEffect(stack -> {
 *     GongFaData d = BaseGongFaItem.getDataFromItem(stack);
 *     return d != null && d.getTypeId().equals("xxx") && d.getRarity() >= 5;
 * })
 * </pre>
 */
public class GongFuContainerEffect {

    /** 全局注册表—所有特殊物品效果在此注册，遍历容器物品时自动匹配 */
    public static final List<GongFuContainerEffect> REGISTRY = new ArrayList<>();

    /**
     * 工厂方法：匹配功法物品的 NBT 数据
     * @param typeId 功法类型标识（如 "qi_jia_shu"）
     * @param rarity 稀有度 1-9
     * @param ceng   层级（如 "copy_5"）
     */
    public static GongFuContainerEffect forGongFa(String typeId, int rarity, String ceng) {
        return new GongFuContainerEffect(stack -> {
            GongFaData data = BaseGongFaItem.getDataFromItem(stack);
            return data != null
                    && data.getTypeId().equals(typeId)
                    && data.getRarity() == rarity
                    && data.getCeng().equals(ceng);
        });
    }

    /**
     * 初始化默认配置。在 {@code CorpseOrigin} 构造器末尾调用。
     */
    public static void registerDefaults() {
        // 示例1：普通物品 — 攻击力×2 + 击杀怪物30%掉钻石 + 受伤反弹1.5倍
        new GongFuContainerEffect(() -> com.phagens.corpseorigin.register.Moditems.ORDINARY_ZB_EYE.get())
                .attrMultiplier("attack_damage", 2.0)
                .killDrop(t -> t instanceof net.minecraft.world.entity.monster.Monster,
                        () -> new ItemStack(net.minecraft.world.item.Items.DIAMOND), 0.3f)
                .hurtRetaliate(1.5f);

        // 示例2：气甲术(稀有度2, 一层) — 全属性×1.5 + 减伤30%
        forGongFa("QI_JIA_SHU", 2, "copy_1")
                .attrMultiplier("max_health", 1.5)
                .attrMultiplier("armor", 1.5)
                .attrMultiplier("knockback_resistance", 1.3)
                .damageReduction(0.3f)
                .killDrop(t -> true,
                        () -> new ItemStack(net.minecraft.world.item.Items.NETHER_STAR), 1.0f);


        // 示例3：霸刀势(稀有度4, 一层) — 攻击力×2 + 吸血20% + 击杀回血4
        forGongFa("BA_DAO_SHI", 4, "copy_1")
                .attrMultiplier("attack_damage", 2.0)
                .attackLifeSteal(0.2f)
                .killHeal(4.0f);
    }


    private final Predicate<ItemStack> matcher;
    private final List<ISpecialEffect> effects = new ArrayList<>();

    /** 按 Item 类型匹配（普通物品用） */
    public GongFuContainerEffect(Supplier<Item> itemSupplier) {
        this.matcher = stack -> {
            Item item = itemSupplier.get();
            return item != null && stack.is(item);
        };
        REGISTRY.add(this);
    }

    /** 自定义 Predicate 匹配（功法物品、复杂条件等） */
    public GongFuContainerEffect(Predicate<ItemStack> matcher) {
        this.matcher = matcher;
        REGISTRY.add(this);
    }


    // ==================== 建造者方法（链式调用） ====================

    /** 属性乘除：2.0=翻倍, 1.5=+50%, 0.5=减半。多物品同属性累乘 */
    public GongFuContainerEffect attrMultiplier(String attr, double multiplier) {
        effects.add(new EffAttributeMultiplier(attr, multiplier));
        return this;
    }

    /** 持续药水：容器中存在时每40tick自动刷新，不会因时间到期而消失 */
    public GongFuContainerEffect passivePotion(Holder<MobEffect> effect, int amplifier) {
        effects.add(new EffPassivePotion(effect, amplifier));
        return this;
    }

    /** 击杀怪物额外掉落：condition=条件, dropSupplier=掉落栈, chance=0~1概率 */
    public GongFuContainerEffect killDrop(Predicate<LivingEntity> condition,
                                          Supplier<ItemStack> dropSupplier,
                                          float chance) {
        effects.add(new EffKillDrop(condition, dropSupplier, chance));
        return this;
    }

    /** 击杀怪物时恢复生命值 */
    public GongFuContainerEffect killHeal(float amount) {
        effects.add(new EffKillHeal(amount));
        return this;
    }

    /** 击杀怪物经验倍率：1.5=多50%, 2.0=翻倍 */
    public GongFuContainerEffect killExpMultiplier(float multiplier) {
        effects.add(new EffKillExpMultiplier(multiplier));
        return this;
    }

    /** 受伤减免比例：0.3=减免30%，多个叠加为累乘 */
    public GongFuContainerEffect damageReduction(float ratio) {
        effects.add(new EffDamageReduction(ratio));
        return this;
    }

    /** 免疫特定伤害类型：传入 DamageSource 判断谓词 */
    public GongFuContainerEffect immuneTo(Predicate<DamageSource> predicate) {
        effects.add(new EffDamageImmunity(predicate));
        return this;
    }

    /** 受伤反击：将受伤值的N倍以真实伤害返还攻击者 */
    public GongFuContainerEffect hurtRetaliate(float multiplier) {
        effects.add(new EffHurtRetaliate(multiplier));
        return this;
    }

    /** 受伤诅咒攻击者：给攻击者施加药水效果 */
    public GongFuContainerEffect hurtCurseAttacker(Holder<MobEffect> effect,
                                                    int amplifier, int durationTicks) {
        effects.add(new EffHurtCurseAttacker(effect, amplifier, durationTicks));
        return this;
    }

    /** 攻击额外附加固定伤害 */
    public GongFuContainerEffect attackBonusDamage(float amount) {
        effects.add(new EffAttackBonusDamage(amount));
        return this;
    }

    /** 攻击吸血：吸取造成伤害的比例（0.3=回血30%伤害值） */
    public GongFuContainerEffect attackLifeSteal(float ratio) {
        effects.add(new EffAttackLifeSteal(ratio));
        return this;
    }

    /** 攻击诅咒目标：给被攻击目标施加药水效果 */
    public GongFuContainerEffect attackCurseTarget(Holder<MobEffect> effect,
                                                    int amplifier, int durationTicks) {
        effects.add(new EffAttackCurseTarget(effect, amplifier, durationTicks));
        return this;
    }

    // ==================== 查询方法 ====================

    /** 检查 ItemStack 是否匹配此配置 */
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && matcher.test(stack);
    }

    public List<ISpecialEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    /** 筛选所有属于 EffAttributeMultiplier 的效果，返回属性名→乘数映射 */
    public Map<String, Double> getAttributeMultipliers() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (ISpecialEffect e : effects) {
            if (e instanceof EffAttributeMultiplier am) {
                result.put(am.attr(), am.multiplier());
            }
        }
        return result;
    }

    // ==================== 效果类型定义 ====================

    public sealed interface ISpecialEffect permits
            EffAttributeMultiplier, EffPassivePotion, EffKillDrop,
            EffKillHeal, EffKillExpMultiplier, EffDamageReduction, EffDamageImmunity,
            EffHurtRetaliate, EffHurtCurseAttacker,
            EffAttackBonusDamage, EffAttackLifeSteal, EffAttackCurseTarget {}

    // ---- 属性 ----
    public record EffAttributeMultiplier(String attr, double multiplier) implements ISpecialEffect {}

    // ---- 持续 ----
    public record EffPassivePotion(Holder<MobEffect> effect, int amplifier) implements ISpecialEffect {}

    // ---- 击杀 ----
    public record EffKillDrop(Predicate<LivingEntity> condition,
                              Supplier<ItemStack> drop,
                              float chance) implements ISpecialEffect {}
    public record EffKillHeal(float amount) implements ISpecialEffect {}
    public record EffKillExpMultiplier(float multiplier) implements ISpecialEffect {}

    // ---- 防御 ----
    public record EffDamageReduction(float ratio) implements ISpecialEffect {}
    public record EffDamageImmunity(Predicate<DamageSource> predicate) implements ISpecialEffect {}

    // ---- 受伤反击 ----
    public record EffHurtRetaliate(float multiplier) implements ISpecialEffect {}
    public record EffHurtCurseAttacker(Holder<MobEffect> effect,
                                        int amplifier,
                                        int durationTicks) implements ISpecialEffect {}

    // ---- 攻击附加 ----
    public record EffAttackBonusDamage(float amount) implements ISpecialEffect {}
    public record EffAttackLifeSteal(float ratio) implements ISpecialEffect {}
    public record EffAttackCurseTarget(Holder<MobEffect> effect,
                                        int amplifier,
                                        int durationTicks) implements ISpecialEffect {}
}
