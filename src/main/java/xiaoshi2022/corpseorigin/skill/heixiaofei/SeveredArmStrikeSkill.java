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
import java.util.Optional;

/**
 * 黑小飞·断臂攻击 - 用断臂抓取正前方目标并砸击。
 * <p>
 * 命中后造成伤害、减速，并把目标拽向自己（抓取）。
 * 断臂形态模型为美术资源，未实装前沿用玩家本体模型。
 */
public class SeveredArmStrikeSkill implements ISkill {

    public static final String PATH = "severed_arm_strike";

    private static final double REACH = 5.0;
    private static final float DAMAGE = 12.0f;
    private static final double PULL_POWER = 1.2;

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
        return 200;  // 10s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity target = findTarget(level, player);
        if (target == null) {
            // 抓空：只有挥臂表现
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 0.8F);
            return;
        }

        target.hurt(level.damageSources().playerAttack(player), DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, true, true));

        // 抓取：把目标拽向玩家
        Vec3 pull = player.position().subtract(target.position());
        if (pull.lengthSqr() > 1.0E-4) {
            pull = pull.normalize().scale(PULL_POWER);
            target.push(pull.x, 0.25, pull.z);
            target.hurtMarked = true;
        }

        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                8, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    /** 视线射线优先，射线未命中时退化为正前方最近目标 */
    private LivingEntity findTarget(ServerLevel level, ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 endPos = eyePos.add(player.getLookAngle().scale(REACH));
        AABB sweep = new AABB(eyePos, endPos).inflate(1.0);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, sweep, e -> e != player && e.isAlive());

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity e : candidates) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(eyePos, endPos);
            if (hit.isPresent()) {
                double dist = eyePos.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = e;
                }
            }
        }
        return closest;
    }
}
