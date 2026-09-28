package xiaoshi2022.corpseorigin.skill;

import net.minecraft.world.damagesource.DamageSource;

/** Explicit skill marker: ordinary melee never inherits an active ultimate's qi bonus. */
public final class QiSkillDamageSource extends DamageSource {
    private QiSkillDamageSource(DamageSource source) {
        super(source.typeHolder(), source.getDirectEntity(), source.getEntity());
    }
    public static DamageSource wrap(DamageSource source) {
        return source instanceof QiSkillDamageSource ? source : new QiSkillDamageSource(source);
    }
}
