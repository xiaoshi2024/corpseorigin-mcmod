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
 * 黑小飞·虎爪 / 飞蜂轮 - 双形态近战。
 * <p>
 * 前方有目标 → 虎爪：单体重击并施加虚弱；<br>
 * 前方无目标 → 飞蜂轮：以自身为中心旋转横扫，范围伤害并击退。
 */
public class TigerClawBeeWheelSkill implements ISkill {

    public static final String PATH = "tiger_claw_bee_wheel";

    private static final double CLAW_REACH = 4.0;
    private static final float CLAW_DAMAGE = 14.0f;

    private static final double WHEEL_RADIUS = 5.0;
    private static final float WHEEL_DAMAGE = 9.0f;
    private static final double WHEEL_KNOCKBACK = 1.4;

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
        return SkillType.COMBAT;
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
        return 400;  // 20s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity front = findFrontTarget(level, player);
        if (front != null) {
            castTigerClaw(level, player, front);
        } else {
            castBeeWheel(level, player);
        }
    }

    /** 虎爪形态：单体重击 + 虚弱 */
    private void castTigerClaw(ServerLevel level, ServerPlayer player, LivingEntity target) {
        target.hurt(level.damageSources().playerAttack(player), CLAW_DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true, true));

        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                14, 0.3, 0.3, 0.3, 0.2);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                2, 0.2, 0.2, 0.2, 0.0);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** 飞蜂轮形态：以自身为中心的旋转横扫 */
    private void castBeeWheel(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(WHEEL_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), WHEEL_DAMAGE);

            Vec3 dir = target.position().subtract(player.position());
            if (dir.lengthSqr() < 1.0E-4) {
                dir = new Vec3(1.0, 0.0, 0.0);
            }
            dir = dir.normalize().scale(WHEEL_KNOCKBACK);
            target.push(dir.x, 0.35, dir.z);
            target.hurtMarked = true;
        }

        // 环形横扫粒子
        for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2 * i / 32.0;
            double x = player.getX() + Math.cos(angle) * WHEEL_RADIUS * 0.75;
            double z = player.getZ() + Math.sin(angle) * WHEEL_RADIUS * 0.75;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    x, player.getY() + 0.8, z, 1, 0, 0, 0, 0.0);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.9F);
    }

    /** 正前方（扇形范围）最近的目标 */
    private LivingEntity findFrontTarget(ServerLevel level, ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(CLAW_REACH));
        AABB sweep = new AABB(eyePos, endPos).inflate(1.5);

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(
                LivingEntity.class, sweep, e -> e != player && e.isAlive())) {
            Vec3 toTarget = e.position().subtract(player.position()).normalize();
            if (look.dot(toTarget) < 0.5) {
                continue;   // 不在前方约 60° 视野内
            }
            double dist = player.distanceToSqr(e);
            if (dist < closestDist) {
                closestDist = dist;
                closest = e;
            }
        }
        return closest;
    }
}
