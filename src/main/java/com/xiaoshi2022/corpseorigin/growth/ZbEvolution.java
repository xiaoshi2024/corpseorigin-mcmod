package com.xiaoshi2022.corpseorigin.growth;

import com.xiaoshi2022.corpseorigin.config.CorpseOriginServerConfig;
import com.xiaoshi2022.corpseorigin.entity.ZombieKin;
import com.xiaoshi2022.corpseorigin.network.CorpseOriginNetwork;
import com.xiaoshi2022.corpseorigin.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 尸兄吞噬进化系统（移植自 26.2 Fabric 版）。
 * <p>
 * 规则：
 * <ul>
 *   <li>尸兄击杀活体获得血肉能量：同类 4、玩家 5、村民 3、动物 1、其它 1；</li>
 *   <li>能量攒满「当前等级 × energyPerLevel」触发进化，多余能量顺延；</li>
 *   <li>1~5 级正常进化；5 级之后进入「超脱临界」：基础 15% 成功率，
 *       每次失败 +10%，成功后清零 —— 每一次突破都是境界跃迁；</li>
 *   <li>吞噬同类时额外回复生命，体现"吃掉对方的血肉"。</li>
 * </ul>
 * 进化等级 1~10 对应境界 人1~人4 → 地1~地4 → 天 → 神·前期，
 * 映射见 {@link EvolutionTier}。数值全部在服务端配置 {@code evolution} 段。
 */
public final class ZbEvolution {

    public static final int MAX_LEVEL = 10;

    private static final UUID HEALTH_ID = UUID.fromString("9f1b7c2e-4a3d-4e5f-8a6b-1c2d3e4f5a60");
    private static final UUID ATTACK_ID = UUID.fromString("9f1b7c2e-4a3d-4e5f-8a6b-1c2d3e4f5a61");
    private static final UUID ARMOR_ID = UUID.fromString("9f1b7c2e-4a3d-4e5f-8a6b-1c2d3e4f5a62");
    private static final UUID SPEED_ID = UUID.fromString("9f1b7c2e-4a3d-4e5f-8a6b-1c2d3e4f5a63");

    /** 已经应用过属性加成的等级；读档后缓存为空会自动重算。 */
    private static final Map<Mob, Integer> APPLIED_LEVEL = new WeakHashMap<>();

    private ZbEvolution() {
    }

    /** 击杀猎物能获得的血肉能量。 */
    public static int energyValue(LivingEntity prey) {
        if (ZombieKin.isZombieKin(prey)) {
            return Math.max(1, CorpseOriginServerConfig.evolutionSameKinEnergy());
        }
        if (prey instanceof Player) {
            return Math.max(1, CorpseOriginServerConfig.evolutionPlayerEnergy());
        }
        if (prey instanceof AbstractVillager) {
            return Math.max(1, CorpseOriginServerConfig.evolutionVillagerEnergy());
        }
        if (prey instanceof Animal) {
            return Math.max(1, CorpseOriginServerConfig.evolutionAnimalEnergy());
        }
        return Math.max(1, CorpseOriginServerConfig.evolutionAnimalEnergy());
    }

    /** 当前等级升到下一级需要的血肉能量。 */
    public static int thresholdForLevel(int level) {
        return Math.max(1, level) * Math.max(1, CorpseOriginServerConfig.evolutionEnergyPerLevel());
    }

    /** 正常进化的等级上限（默认 5）；达到后需要概率突破。 */
    public static int breakthroughLevel() {
        return Math.max(1, Math.min(MAX_LEVEL - 1, CorpseOriginServerConfig.evolutionBreakthroughLevel()));
    }

    /** 尸兄当前的绝对境界等级（1~10，与进化等级一致）。 */
    public static int realmLevel(Mob mob) {
        return NativeCorpseState.evolutionLevel(mob);
    }

    /**
     * 击杀回调：由 {@code ModEvents#onLivingDeath} 调用。
     * 只有已转化的尸兄（{@link NativeCorpseState#infected}）才能吞噬成长。
     */
    public static void onKill(Mob killer, LivingEntity prey) {
        if (!(killer.level() instanceof ServerLevel level)) return;
        if (!NativeCorpseState.infected(killer)) return;
        if (!CorpseOriginServerConfig.evolutionEnabled()) return;

        // 同类相食：吞掉对方的血肉，立刻回复生命
        if (ZombieKin.isZombieKin(prey)) {
            killer.heal(Math.max(2.0F, killer.getMaxHealth() * 0.2F));
            level.playSound(null, killer.blockPosition(), SoundEvents.GENERIC_EAT,
                    SoundSource.HOSTILE, 0.8F, 0.6F);
        }

        NativeCorpseState.setFleshEnergy(killer,
                NativeCorpseState.fleshEnergy(killer) + energyValue(prey));

        int guard = 0;
        while (guard++ <= MAX_LEVEL
                && NativeCorpseState.evolutionLevel(killer) < MAX_LEVEL
                && NativeCorpseState.fleshEnergy(killer)
                        >= thresholdForLevel(NativeCorpseState.evolutionLevel(killer))) {
            if (!tryEvolve(killer, level)) break;
        }
    }

    /** @return 本次是否真的完成了一次进化 */
    private static boolean tryEvolve(Mob mob, ServerLevel level) {
        int current = NativeCorpseState.evolutionLevel(mob);
        if (current >= MAX_LEVEL) {
            NativeCorpseState.setFleshEnergy(mob, 0);
            return false;
        }

        int threshold = thresholdForLevel(current);
        if (current < breakthroughLevel()) {
            // 正常进化
            NativeCorpseState.setFleshEnergy(mob, NativeCorpseState.fleshEnergy(mob) - threshold);
            evolve(mob, level, false);
            return true;
        }

        // 临界线之上：概率超脱
        double chance = Math.max(0.0D, CorpseOriginServerConfig.evolutionBreakthroughChance())
                + Math.max(0.0D, CorpseOriginServerConfig.evolutionBreakthroughBonus())
                * NativeCorpseState.breakthroughFailures(mob);
        if (mob.getRandom().nextDouble() < chance) {
            NativeCorpseState.setFleshEnergy(mob, NativeCorpseState.fleshEnergy(mob) - threshold);
            NativeCorpseState.setBreakthroughFailures(mob, 0);
            evolve(mob, level, true);
            return true;
        }

        // 突破失败：能量清空，累计下次成功率
        NativeCorpseState.setFleshEnergy(mob, 0);
        NativeCorpseState.setBreakthroughFailures(mob, NativeCorpseState.breakthroughFailures(mob) + 1);
        return false;
    }

    private static void evolve(Mob mob, ServerLevel level, boolean critical) {
        NativeCorpseState.setEvolutionLevel(mob, NativeCorpseState.evolutionLevel(mob) + 1);
        applyAttributes(mob, true);

        // 境界变化同步给客户端（名称前缀显示用）
        CorpseOriginNetwork.sync(mob);

        level.playSound(null, mob.blockPosition(), ModSounds.GROUND_CHI.get(), SoundSource.HOSTILE,
                1.2F, critical ? 0.7F : 1.0F);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.6D, mob.getZ(),
                critical ? 18 : 10, 0.35D, 0.5D, 0.35D, 0.05D);
    }

    /** 读档 / 转化后补挂境界属性；已经应用过的等级直接返回。 */
    public static void ensureAttributes(Mob mob) {
        if (!NativeCorpseState.infected(mob)) return;
        int level = NativeCorpseState.evolutionLevel(mob);
        Integer applied = APPLIED_LEVEL.get(mob);
        if (applied != null && applied == level) return;
        applyAttributes(mob, false);
        APPLIED_LEVEL.put(mob, level);
    }

    /**
     * 按进化等级重算属性：
     * 每级 +15% 生命、+10% 攻击（乘基础值），+0.5 护甲、+0.01 移速（加值）。
     * 用固定 UUID 的永久修饰符，重复调用不会叠加。
     */
    private static void applyAttributes(Mob mob, boolean heal) {
        int steps = NativeCorpseState.evolutionLevel(mob) - 1;
        apply(mob, Attributes.MAX_HEALTH, HEALTH_ID, "corpse_evo_health",
                steps * 0.15D, AttributeModifier.Operation.MULTIPLY_BASE);
        apply(mob, Attributes.ATTACK_DAMAGE, ATTACK_ID, "corpse_evo_attack",
                steps * 0.10D, AttributeModifier.Operation.MULTIPLY_BASE);
        apply(mob, Attributes.ARMOR, ARMOR_ID, "corpse_evo_armor",
                steps * 0.5D, AttributeModifier.Operation.ADDITION);
        apply(mob, Attributes.MOVEMENT_SPEED, SPEED_ID, "corpse_evo_speed",
                steps * 0.01D, AttributeModifier.Operation.ADDITION);

        if (mob.getHealth() > mob.getMaxHealth()) {
            mob.setHealth(mob.getMaxHealth());
        }
        if (heal && mob.isAlive()) {
            mob.setHealth(mob.getMaxHealth());
        }
        APPLIED_LEVEL.put(mob, NativeCorpseState.evolutionLevel(mob));
    }

    private static void apply(Mob mob, Attribute attribute, UUID id, String name,
                              double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) return;
        if (Math.abs(amount) < 1.0E-6D) {
            instance.removeModifier(id);
            return;
        }
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, name, amount, operation));
    }
}
