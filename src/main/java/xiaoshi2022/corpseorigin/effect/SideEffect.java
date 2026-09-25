package xiaoshi2022.corpseorigin.effect;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.registry.ModEffects;

/**
 * 强化剂副作用 —— 黄色强化剂的代价。
 * <p>
 * 等级随每次注射叠加：等级越高，身体越撑不住，达到阈值后可能直接暴毙；
 * 蓝色中和剂可以下调等级（每次 -3 级），降到 0 即完全清除。
 * <ul>
 *   <li>1 级起：反胃</li>
 *   <li>3 级起：缓慢 + 虚弱</li>
 *   <li>5 级起：中毒，并开始按等级概率暴毙</li>
 * </ul>
 */
public class SideEffect extends MobEffect {

    /** 超过此等级开始有暴毙风险 */
    public static final int DEATH_THRESHOLD = 5;
    /** 暴毙基础概率（每秒判定一次，等级越高越高） */
    public static final double DEATH_CHANCE = 0.02;
    /** 副作用持续时间（tick）：10 分钟 */
    public static final int EFFECT_DURATION = 12000;
    /** 中和剂一次下调的等级 */
    public static final int NEUTRALIZE_STEPS = 3;

    public SideEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 每 20 tick（1 秒）结算一次
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        applySideEffects(entity, amplifier);
        checkDeathRisk(level, entity, amplifier);
        return true;
    }

    /** 按等级施加对应的负面效果。 */
    private void applySideEffects(LivingEntity entity, int level) {
        if (level >= 1) {
            entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, false, true, true));
        }
        if (level >= 3) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, Math.min(level - 3, 1), false, true, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, Math.min(level - 3, 1), false, true, true));
        }
        if (level >= 5) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, Math.min(level - 5, 2), false, true, true));
        }
    }

    /** 等级越过阈值后，身体有概率直接崩溃。 */
    private void checkDeathRisk(ServerLevel level, LivingEntity entity, int sideLevel) {
        if (sideLevel < DEATH_THRESHOLD) return;
        double deathChance = DEATH_CHANCE * (sideLevel - DEATH_THRESHOLD + 1);
        if (entity.getRandom().nextDouble() >= deathChance) return;
        if (entity instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("message.corpseorigin.side_effect.death"));
            CorpseOrigin.LOGGER.info("玩家 {} 因强化剂副作用暴毙", player.getName().getString());
        }
        entity.hurtServer(level, entity.damageSources().magic(), Float.MAX_VALUE);
    }

    /** 注射黄色强化剂：叠加副作用等级并提示玩家。 */
    public static void applySideEffect(Player player, int level) {
        if (!(player instanceof ServerPlayer server)) return;
        MobEffectInstance existing = server.getEffect(ModEffects.SIDE_EFFECT);
        int newLevel = level;
        int newDuration = EFFECT_DURATION;
        if (existing != null) {
            newLevel = existing.getAmplifier() + level;
            newDuration = Math.max(existing.getDuration(), EFFECT_DURATION);
        }
        newLevel = Math.min(newLevel, 10);
        server.addEffect(new MobEffectInstance(ModEffects.SIDE_EFFECT, newDuration, newLevel, false, true, true));
        sendWarningMessage(server, newLevel);
        CorpseOrigin.LOGGER.info("玩家 {} 获得副作用效果，当前等级: {}", server.getName().getString(), newLevel);
    }

    /** 按等级把警告顶到玩家聊天栏。 */
    private static void sendWarningMessage(ServerPlayer player, int level) {
        String key = level <= 1 ? "message.corpseorigin.side_effect.text_01"
                : level <= 2 ? "message.corpseorigin.side_effect.text_02"
                : level <= 4 ? "message.corpseorigin.side_effect.text_03"
                : "message.corpseorigin.side_effect.text_04";
        player.sendSystemMessage(Component.translatable(key));
    }

    /** 注射蓝色中和剂：下调副作用等级，降到 0 即完全清除。 */
    public static void clearSideEffect(Player player) {
        if (!(player instanceof ServerPlayer server)) return;
        MobEffectInstance existing = server.getEffect(ModEffects.SIDE_EFFECT);
        if (existing == null) return;
        int currentLevel = existing.getAmplifier();
        int newLevel = Math.max(0, currentLevel - NEUTRALIZE_STEPS);
        if (newLevel > 0) {
            server.addEffect(new MobEffectInstance(ModEffects.SIDE_EFFECT, existing.getDuration(),
                    newLevel, false, true, true));
            server.sendSystemMessage(Component.translatable("message.corpseorigin.side_effect.text_05"));
        } else {
            server.removeEffect(ModEffects.SIDE_EFFECT);
            server.sendSystemMessage(Component.translatable("message.corpseorigin.side_effect.text_06"));
        }
        CorpseOrigin.LOGGER.info("玩家 {} 使用中和剂，副作用从 {} 级降至 {} 级",
                server.getName().getString(), currentLevel, newLevel);
    }
}
