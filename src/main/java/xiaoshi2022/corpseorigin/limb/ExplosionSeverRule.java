package xiaoshi2022.corpseorigin.limb;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/**
 * 爆炸 / 炸弹断肢规则。
 * <p>
 * <b>默认没有注册</b> —— 拍摄要用炸弹时，在 {@link LimbSeverRules#registerDefaults()}
 * 里解开那一行即可；想改概率直接调这里的常量，或者照着这个类写一条自己的规则。
 */
public final class ExplosionSeverRule implements LimbSeverRule {

    public static final String ID = "corpseorigin:explosion";
    /** 爆炸伤害门槛（比刀具高，炸弹本来就重） */
    public static final float MIN_DAMAGE = 8.0F;
    /** 爆炸截断概率 */
    public static final float CHANCE = 0.25F;

    @Override
    public String id() {
        return ID;
    }

    /** 排在武器规则之后：贴脸被砍应该按刀算 */
    @Override
    public int priority() {
        return 200;
    }

    @Override
    public boolean matches(ServerPlayer victim, DamageSource source, float hitPower, float damageTaken) {
        if (damageTaken <= 0.0F || hitPower < MIN_DAMAGE) {
            return false;
        }
        return source.is(DamageTypeTags.IS_EXPLOSION);
    }

    @Override
    public float chance(ServerPlayer victim, LimbState state, int slot, float hitPower) {
        return CHANCE;
    }
}
