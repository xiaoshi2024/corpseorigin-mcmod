package xiaoshi2022.corpseorigin.entity.evolution;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.growth.OrganDefinition;

import java.util.List;

/**
 * 尸兄身上器官的实际效果（与玩家器官口径一致，但尸兄不消耗血能）：
 * <ul>
 *   <li>解剖（anatomy）：额外手臂加攻击、额外腿加速度、夜视；</li>
 *   <li>翅膀（wings）：免疫摔落，空中下落时缓落；</li>
 *   <li>腮（gills）：水下呼吸（见实体 canBreatheUnderwater）；</li>
 *   <li>吸血（vampire）：攻击回血（见实体 doHurtTarget）。</li>
 * </ul>
 */
public final class ZbOrganEffects {

    private ZbOrganEffects() {}

    public static boolean hasTrait(LowerLevelZbEntity self, String trait) {
        return ZbOrganGrowth.activeDefinitions(self).stream()
                .anyMatch(d -> trait.equals(d.trait()));
    }

    /** 每个服务端 tick 调用。 */
    public static void tick(LowerLevelZbEntity self) {
        List<OrganDefinition> organs = ZbOrganGrowth.activeDefinitions(self);

        // ---- 解剖：额外肢体 ----
        var anatomyParts = organs.stream().filter(d -> d.anatomy() != null).map(OrganDefinition::anatomy).toList();
        double extraDamage = Math.min(8, anatomyParts.stream().mapToDouble(a -> a.extraArms() * a.damagePerArm()).sum());
        double extraSpeed = Math.min(0.20, anatomyParts.stream().mapToDouble(a -> a.extraLegs() * a.speedPerLeg()).sum());
        applyModifier(self, Attributes.ATTACK_DAMAGE, "zb_organ_arms", extraDamage, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(self, Attributes.MOVEMENT_SPEED, "zb_organ_legs", extraSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        if (self.tickCount % 60 == 0
                && anatomyParts.stream().anyMatch(OrganDefinition.Anatomy::nightVision)) {
            self.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 120, 0, true, false));
        }

        // ---- 翅膀：空中下落时缓落 ----
        if (hasTrait(self, "wings")
                && !self.onGround()
                && self.getDeltaMovement().y < 0
                && self.tickCount % 5 == 0) {
            self.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 12, 0, true, false));
        }
    }

    private static void applyModifier(LowerLevelZbEntity self, Holder<Attribute> type,
                                      String key, double value, AttributeModifier.Operation operation) {
        var instance = self.getAttribute(type);
        if (instance == null) return;
        var id = CorpseOrigin.id(key);
        var old = instance.getModifier(id);
        if (value == 0) {
            if (old != null) instance.removeModifier(id);
        } else if (old == null || old.amount() != value) {
            instance.addOrReplacePermanentModifier(new AttributeModifier(id, value, operation));
        }
    }
}
