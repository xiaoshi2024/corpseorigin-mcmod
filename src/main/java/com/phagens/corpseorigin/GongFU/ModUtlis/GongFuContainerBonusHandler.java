package com.phagens.corpseorigin.GongFU.ModUtlis;

import com.phagens.corpseorigin.CorpseOrigin;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.entity.ai.attributes.Attributes.*;

/**
 * 修行容器特殊效果事件分发器
 * <p>
 * 统一收集玩家容器中匹配的所有效果配置，按四类事件分发：
 * <ul>
 *   <li>PlayerTickEvent.Post       → EffPassivePotion</li>
 *   <li>LivingDeathEvent           → EffKillDrop / EffKillHeal / EffKillExpMultiplier</li>
 *   <li>LivingDamageEvent.Pre      → EffDamageReduction / EffDamageImmunity（玩家受伤）
 *                                    + EffAttackBonusDamage（玩家攻击）</li>
 *   <li>LivingDamageEvent.Post     → EffHurtRetaliate / EffHurtCurseAttacker（玩家受伤）
 *                                    + EffAttackLifeSteal / EffAttackCurseTarget（玩家攻击）</li>
 * </ul>
 */
@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class GongFuContainerBonusHandler {
    private static final String BONUS_MULT_PREFIX = "gongfu_bonus_mult_";
    /**
     * 属性名 → Holder<Attribute> 映射表
     * 新增属性只需在此加一行，multipliers Map 自动驱动
     */
    private static final Map<String, Holder<Attribute>> ATTR_MAP = Map.ofEntries(
            Map.entry("attack_damage", ATTACK_DAMAGE),
            Map.entry("movement_speed", MOVEMENT_SPEED),
            Map.entry("max_health", MAX_HEALTH),
            Map.entry("armor", ARMOR),
            Map.entry("knockback_resistance", KNOCKBACK_RESISTANCE),
            Map.entry("attack_speed", ATTACK_SPEED),
            Map.entry("luck", LUCK)
    );
    /** 收集玩家容器中所有匹配的配置 */
    private static List<GongFuContainerEffect> collectMatchingConfigs(Player player) {
        List<GongFuContainerEffect> configs = new ArrayList<>();
        NonNullList<ItemStack> items = GongFUDataUtlis.getGongFuItems(player);
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            for (GongFuContainerEffect config : GongFuContainerEffect.REGISTRY) {
                if (config.matches(stack)) {
                    configs.add(config);
                }
            }
        }
        return configs;
    }

    /** 收集所有激活的效果 */
    private static List<GongFuContainerEffect.ISpecialEffect> collectAllEffects(Player player) {
        List<GongFuContainerEffect.ISpecialEffect> all = new ArrayList<>();
        for (GongFuContainerEffect config : collectMatchingConfigs(player)) {
            all.addAll(config.getEffects());
        }
        return all;
    }

    // ==================== 每 tick：持续药水 ====================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        for (GongFuContainerEffect.ISpecialEffect e : collectAllEffects(player)) {
            if (e instanceof GongFuContainerEffect.EffPassivePotion potion) {
                player.addEffect(new MobEffectInstance(
                        potion.effect(), 40, potion.amplifier(), false, false));
            }
        }
    }

    // ==================== 击杀：掉落 / 治疗 / 经验 ====================

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        LivingEntity target = event.getEntity();

        for (GongFuContainerEffect.ISpecialEffect e : collectAllEffects(player)) {
            switch (e) {
                case GongFuContainerEffect.EffKillDrop drop -> {
                    if (drop.condition().test(target)
                            && target.getRandom().nextFloat() < drop.chance()) {
                        target.spawnAtLocation(drop.drop().get());
                    }
                }
                case GongFuContainerEffect.EffKillHeal heal -> player.heal(heal.amount());
                case GongFuContainerEffect.EffKillExpMultiplier expMult -> {
                    int baseExp = target.getExperienceReward(
                            (ServerLevel) target.level(), player);
                    int bonus = Math.round(baseExp * (expMult.multiplier() - 1.0f));
                    if (bonus > 0) {
                        ExperienceOrb.award(
                                (ServerLevel) target.level(), target.position(), bonus);
                    }
                }
                default -> {}
            }
        }
    }

    // ==================== 伤害前：防御减免 + 攻击附加 ====================

    @SubscribeEvent
    public static void onLivingHurtPre(LivingDamageEvent.Pre event) {
        // ---- 玩家是受伤方：减免 / 免疫 ----
        if (event.getEntity() instanceof Player player) {
            float totalReduction = 1.0f;
            boolean immune = false;

            for (GongFuContainerEffect.ISpecialEffect e : collectAllEffects(player)) {
                switch (e) {
                    case GongFuContainerEffect.EffDamageReduction red -> totalReduction *= (1.0f - red.ratio());
                    case GongFuContainerEffect.EffDamageImmunity imm -> {
                        if (imm.predicate().test(event.getSource())) immune = true;
                    }
                    default -> {}
                }
            }

            if (immune) {
                event.setNewDamage(0);
            } else if (totalReduction < 1.0f) {
                event.setNewDamage(event.getNewDamage() * totalReduction);
            }
            return;
        }

        // ---- 玩家是攻击方：额外伤害 ----
        if (event.getSource().getEntity() instanceof Player player) {
            float bonus = 0f;
            for (GongFuContainerEffect.ISpecialEffect e : collectAllEffects(player)) {
                if (e instanceof GongFuContainerEffect.EffAttackBonusDamage bd) {
                    bonus += bd.amount();
                }
            }
            if (bonus > 0f) {
                event.setNewDamage(event.getNewDamage() + bonus);
            }
        }
    }

    // ==================== 伤害后：受伤反击 + 攻击吸血 / 诅咒 ====================

    @SubscribeEvent
    public static void onLivingHurtPost(LivingDamageEvent.Post event) {
        float actualDamage = event.getNewDamage();

        // ---- 玩家是受伤方：反击 / 诅咒攻击者 ----
        if (event.getEntity() instanceof Player player) {
            LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity le
                    ? le : null;
            if (attacker == null || !attacker.isAlive()) return;

            for (GongFuContainerEffect.ISpecialEffect e : collectAllEffects(player)) {
                switch (e) {
                    case GongFuContainerEffect.EffHurtRetaliate r -> {
                        float retaliateDmg = actualDamage * r.multiplier();
                        if (retaliateDmg > 0f) {
                            attacker.hurt(player.damageSources().generic(), retaliateDmg);
                        }
                    }
                    case GongFuContainerEffect.EffHurtCurseAttacker curse -> attacker.addEffect(
                            new MobEffectInstance(curse.effect(), curse.durationTicks(),
                                    curse.amplifier(), false, true));
                    default -> {}
                }
            }
            return;
        }

        // ---- 玩家是攻击方：吸血 / 诅咒目标 ----
        if (event.getSource().getEntity() instanceof Player player) {
            LivingEntity target = event.getEntity();
            if (!target.isAlive()) return;

            for (GongFuContainerEffect.ISpecialEffect e : collectAllEffects(player)) {
                switch (e) {
                    case GongFuContainerEffect.EffAttackLifeSteal steal -> player.heal(actualDamage * steal.ratio());
                    case GongFuContainerEffect.EffAttackCurseTarget curse -> target.addEffect(
                            new MobEffectInstance(curse.effect(), curse.durationTicks(),
                                    curse.amplifier(), false, true));
                    default -> {}
                }
            }
        }
    }
}
