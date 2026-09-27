package xiaoshi2022.corpseorigin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;

/** Uses the existing flying qi collision/trail, with owner-safe lifesteal hits. */
public final class BloodWingBeamEntity extends JuQueBeamEntity {

    public BloodWingBeamEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Override
    public BloodWingBeamEntity setDamage(float damage) {
        super.setDamage(damage);
        return this;
    }

    /**
     * 主人可以是玩家，也可以是拿着血翼黑刃的克隆分身；命中规则两边统一走
     * {@link ChapterCombat#canHit(LivingEntity, LivingEntity)}（本体 / 创造 / 同盟 / 尸族互斥）。
     */
    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity)
                && getOwner() instanceof LivingEntity owner
                && entity instanceof LivingEntity living
                && ChapterCombat.canHit(owner, living);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level() instanceof ServerLevel level
                && getOwner() instanceof LivingEntity owner
                && hit.getEntity() instanceof LivingEntity target
                && ChapterCombat.canHit(owner, target)) {
            float before = target.getHealth();
            if (target.hurtServer(level, damageSources().mobProjectile(this, owner), damageFor(owner,target))) {
                // 命中吸血对分身同样生效（生物 heal 是通用能力）
                owner.heal(Math.min(4, Math.max(0, before - target.getHealth()) * .4f));
            }
            discard();
        }
    }
}
