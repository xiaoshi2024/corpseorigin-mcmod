package xiaoshi2022.corpseorigin.skill.zuohufa;

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
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

import java.util.List;
import java.util.Optional;

/**
 * 左护法·暴龙一击 —— 原著里在武神阁学会的武功，一击把陌轩打飞，威力大到能贯穿大楼。
 * <p>
 * 表现：对正前方单体打出一记贯注重击 —— 高伤害 + 大幅击退 + 短暂减速，
 * 命中处带血雾与冲击粒子。打的是"看得见的那个"，所以走视线射线，射线没命中就不打。
 * <p>
 * 冷却 15 秒，消耗内力 8。
 */
public class TyrantStrikeSkill implements ISkill {

    public static final String PATH = "tyrant_strike";

    /** 打击距离（格） */
    private static final double REACH = 6.0;
    private static final float DAMAGE = 16.0F;
    /** 击退力度：原著是"击飞"，所以给得比一般技能大 */
    private static final double KNOCKBACK = 2.4;
    private static final int SLOW_TICKS = 60;
    private static final int INNER_POWER_COST = 8;

    /** 打击色（原 CRIT / DAMAGE_INDICATOR / SWEEP_ATTACK 统一取血红） */
    private static final int STRIKE_COLOR = 0xc0182a;

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
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 300;   // 15s
    }

    @Override
    public int getInnerPowerCost() {
        return INNER_POWER_COST;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // 起手：血光在身前拉出一道（整条线塌缩成一朵气团，取中点、半径≈线长一半）
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        QiEffects.cloud(level, eye.add(look.scale(1.75)), STRIKE_COLOR, 1.3f, 10);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);

        LivingEntity target = findTarget(level, player, eye, look);
        if (target == null) {
            return;
        }

        target.hurt(level.damageSources().playerAttack(player), DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, 1, false, true, true));

        // 击飞：沿玩家视线方向推出去，再补一点上抬
        Vec3 push = new Vec3(look.x, 0.0, look.z);
        if (push.lengthSqr() < 1.0E-4) {
            push = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
        }
        if (push.lengthSqr() > 1.0E-4) {
            push = push.normalize().scale(KNOCKBACK);
            target.push(push.x, 0.55, push.z);
            target.hurtMarked = true;
        }

        Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        QiEffects.burst(level, hit.x, hit.y, hit.z, STRIKE_COLOR, 14, 0.4);
        QiEffects.burst(level, hit.x, hit.y, hit.z, STRIKE_COLOR, 4, 0.3);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.4F, 0.6F);
    }

    /** 视线射线上的最近目标（射线没打中就不打，避免"隔空命中"） */
    private LivingEntity findTarget(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 look) {
        Vec3 end = eye.add(look.scale(REACH));
        AABB sweep = new AABB(eye, end).inflate(1.0);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, sweep, e -> e != player && e.isAlive());

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isEmpty()) {
                continue;
            }
            double dist = eye.distanceToSqr(hit.get());
            if (dist < closestDist) {
                closestDist = dist;
                closest = candidate;
            }
        }
        return closest;
    }
}
