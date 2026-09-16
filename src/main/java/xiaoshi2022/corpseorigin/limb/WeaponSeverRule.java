package xiaoshi2022.corpseorigin.limb;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 内置的锋利武器规则（剑 / 斧 / 带 WEAPON 组件的物品）—— 也是默认唯一的截断来源。
 */
public final class WeaponSeverRule implements LimbSeverRule {

    public static final String ID = "corpseorigin:weapon";

    /** 基础截断概率 */
    public static final float BASE_CHANCE = 0.15F;
    /** 新生脆弱期内概率倍数 */
    public static final float VULNERABLE_CHANCE_MULT = 2.0F;
    /** 重击的概率倍数 */
    public static final float HEAVY_CHANCE_MULT = 2.0F;
    /**
     * 触发截断所需的最小伤害。
     * <p>
     * 判定用 {@code max(baseDamage, damageTaken)} —— baseDamage 是"这一刀多重"，
     * damageTaken 是穿甲后实际掉的血。只卡 damageTaken 的话，卫道士（基础攻击力仅 5）穿甲后
     * 永远到不了门槛；只看 baseDamage 又会让"打不动的人被断肢"，所以取两者较大值。
     */
    public static final float MIN_DAMAGE = 4.0F;
    /** 重击门槛 */
    public static final float HEAVY_DAMAGE = 9.0F;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean matches(ServerPlayer victim, DamageSource source, float hitPower, float damageTaken) {
        if (damageTaken <= 0.0F) {
            return false;   // 完全挡住 / 免疫，这一刀没打进去
        }
        if (hitPower < MIN_DAMAGE) {
            return false;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return false;
        }
        return isSharpWeapon(attacker.getMainHandItem());
    }

    /** 锋利武器判定：有 WEAPON 组件的物品，或原版剑 / 斧 */
    public static boolean isSharpWeapon(ItemStack stack) {
        return stack.has(DataComponents.WEAPON)
                || stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES);
    }

    @Override
    public float chance(ServerPlayer victim, LimbState state, int slot, float hitPower) {
        float chance = BASE_CHANCE;
        if (hitPower >= HEAVY_DAMAGE) {
            chance *= HEAVY_CHANCE_MULT;
        }
        if (state.cooldowns()[slot] > 0) {
            chance *= VULNERABLE_CHANCE_MULT;
        }
        return chance;
    }
}
