package xiaoshi2022.corpseorigin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;

/** Uses the existing flying qi collision/trail, with owner-safe lifesteal hits. */
public final class BloodWingBeamEntity extends JuQueBeamEntity {
    private float hitDamage = 4;

    public BloodWingBeamEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Override
    public BloodWingBeamEntity setDamage(float damage) {
        super.setDamage(damage);
        hitDamage = damage;
        return this;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && getOwner() instanceof ServerPlayer player
                && entity instanceof LivingEntity living && ChapterCombat.canHit(player, living);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level() instanceof ServerLevel level && getOwner() instanceof ServerPlayer player
                && hit.getEntity() instanceof LivingEntity target && ChapterCombat.canHit(player, target)) {
            float before = target.getHealth();
            if (target.hurtServer(level, damageSources().indirectMagic(this, player), hitDamage)) {
                player.heal(Math.min(4, Math.max(0, before - target.getHealth()) * .4f));
            }
            discard();
        }
    }
}
