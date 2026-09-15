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
 * 黑小飞·圆舞 - 受击脱身技。
 * <p>
 * 原地旋身震开周围敌人并使其减速，自身获得短暂抗性与加速脱离包围。
 */
public class RoundDanceSkill implements ISkill {

    public static final String PATH = "round_dance";

    private static final double RADIUS = 5.0;
    private static final float DAMAGE = 5.0f;
    private static final double KNOCKBACK = 1.8;
    private static final int SELF_BUFF_TICKS = 60;

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
        return 500;  // 25s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, false, true, true));

            Vec3 dir = target.position().subtract(player.position());
            if (dir.lengthSqr() < 1.0E-4) {
                dir = new Vec3(1.0, 0.0, 0.0);
            }
            dir = dir.normalize().scale(KNOCKBACK);
            target.push(dir.x, 0.45, dir.z);
            target.hurtMarked = true;
        }

        // 脱身：清火 + 短暂抗性/加速
        player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, SELF_BUFF_TICKS, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, SELF_BUFF_TICKS, 0, false, true, true));

        // 旋身气流
        for (int ring = 1; ring <= 3; ring++) {
            double radius = RADIUS * ring / 3.0;
            for (int i = 0; i < 20; i++) {
                double angle = Math.PI * 2 * i / 20.0;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY() + 0.9,
                        player.getZ() + Math.sin(angle) * radius,
                        1, 0, 0, 0, 0.0);
            }
        }
        level.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.5, player.getZ(), 16, 1.0, 0.3, 1.0, 0.05);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 1.2F);
    }
}
