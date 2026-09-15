package xiaoshi2022.corpseorigin.skill.heixiaofei;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.List;

/**
 * 黑小飞·恶犬出笼 - 向前突进的位移技。
 * <p>
 * 沿视线方向冲刺，撞开并伤害路径上的敌人。
 */
public class HoundUnleashedSkill implements ISkill {

    public static final String PATH = "hound_unleashed";

    private static final double DASH_POWER = 2.0;
    private static final double DASH_DISTANCE = 6.0;
    private static final float DASH_DAMAGE = 6.0f;

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, PATH);
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin." + PATH);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin." + PATH + ".desc");
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.UTILITY;
    }

    @Override
    public int getCost() {
        return 1;
    }

    @Override
    public int getRequiredLevel() {
        return 1;
    }

    @Override
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 300;  // 15s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(1.0, 0.0, 0.0);
        }
        flat = flat.normalize();

        // 冲刺
        player.setDeltaMovement(new Vec3(flat.x * DASH_POWER, 0.25, flat.z * DASH_POWER));
        player.hurtMarked = true;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0, false, true, true));

        // 路径上的敌人被撞开
        Vec3 start = player.position();
        Vec3 end = start.add(flat.scale(DASH_DISTANCE));
        AABB path = new AABB(start, end).inflate(1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, path, e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), DASH_DAMAGE);

            Vec3 dir = target.position().subtract(player.position());
            if (dir.lengthSqr() < 1.0E-4) {
                dir = flat;
            }
            dir = dir.normalize().scale(1.2);
            target.push(dir.x, 0.4, dir.z);
            target.hurtMarked = true;
        }

        // 冲刺残影
        for (int i = 1; i <= 10; i++) {
            double t = i / 10.0;
            level.sendParticles(ParticleTypes.CLOUD,
                    start.x + flat.x * DASH_DISTANCE * t,
                    start.y + 0.2,
                    start.z + flat.z * DASH_DISTANCE * t,
                    3, 0.2, 0.1, 0.2, 0.02);
        }
        level.sendParticles(ParticleTypes.CRIT,
                start.x, start.y + 0.5, start.z, 10, 0.3, 0.3, 0.3, 0.1);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.4F);
    }
}
