package xiaoshi2022.corpseorigin.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 「尸兄撕咬」流血效果 —— 被尔多兽王尸兄的獠牙咬中后挂上。
 * <p>
 * 仿 Undertale Sans 的 KR：玩家在流血期间<b>无法进食回血</b>
 * （由 {@link xiaoshi2022.corpseorigin.mixin.BleedBlockFoodMixin} 在 {@code Consumable.canConsume} 拦下），
 * 持续被扣血直到效果自然结束 —— 让"被咬一口"变成持续威胁，而不是一次性掉血。
 * <p>
 * 每 40 tick（2 秒）造成 2 点（1 颗心）魔法伤害，并发 DAMAGE_INDICATOR 粒子提示玩家正在流血。
 */
public class BleedEffect extends MobEffect {

    /** 每 40 tick（2 秒）结算一次流血伤害 */
    private static final int TICK_INTERVAL = 40;
    /** 每次结算的伤害（1 颗心） */
    private static final float DAMAGE_PER_TICK = 2.0F;

    public BleedEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 每 TICK_INTERVAL tick 结算一次；最后一 tick 也触发，确保流血不至于"超期未结算"
        return duration % TICK_INTERVAL == 0 || duration <= 1;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // 用魔法伤害结算：绕过护甲，但能被常规抗性减免；与 Sans KR 的"无视防御"思路一致
        if (entity.hurtServer(level, entity.damageSources().magic(), DAMAGE_PER_TICK + amplifier)) {
            // 红色伤害指示器飘起来，玩家就知道自己在流血
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    entity.getX(), entity.getY(0.5D), entity.getZ(),
                    6, 0.4D, 0.3D, 0.4D, 0.2D);
            return true;
        }
        return false;
    }
}
