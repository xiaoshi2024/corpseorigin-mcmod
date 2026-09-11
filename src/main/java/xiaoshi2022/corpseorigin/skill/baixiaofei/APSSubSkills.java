package xiaoshi2022.corpseorigin.skill.baixiaofei;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainGenerator;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager;

import java.util.List;

/**
 * 古仙剑·四段子技能实现 —— 一剑山河
 */
public class APSSubSkills {

    // ==================== 第一段：朝辞白帝彩云间 ====================
    public static void castZhaoCiBaiDi(ServerPlayer player, ServerLevel level) {
        APSTerrainManager.toggleTransformation(player, player.getMainHandItem(), level);

        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, 0.3, look.z).normalize().scale(1.5);
        player.setDeltaMovement(dash);
        player.hurtMarked = true;

        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED, 100, 1, false, true, true));

        BlockPos c = player.blockPosition();
        for (int i = 0; i < 80; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 5 + Math.random() * 20;
            double x = c.getX() + Math.cos(a) * r;
            double z = c.getZ() + Math.sin(a) * r;
            level.sendParticles(ParticleTypes.CLOUD,
                    x, c.getY() + 2 + Math.random() * 6, z,
                    1, 0.2, 0.2, 0.2, 0.02);
            level.sendParticles(ParticleTypes.END_ROD,
                    x, c.getY() + 3, z, 1, 0.1, 0.1, 0.1, 0.01);
        }

        player.sendSystemMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage1"));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.5F);
    }

    // ==================== 第二段：千里江陵一日还 ====================
    public static void castQianLiJiangLing(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, 0.1, look.z).normalize().scale(3.5);
        player.setDeltaMovement(dash);
        player.hurtMarked = true;

        CompoundTag state = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
        state.putInt("aps_dash_ticks", 10);
        player.setAttached(ModDataAttachments.APS_STATE, state);

        for (int i = 0; i < 24; i++) {
            Vec3 p = player.position().add(look.scale(i * 0.4));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    p.x, p.y + 0.5, p.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CRIT,
                    p.x, p.y + 0.5, p.z, 2, 0.2, 0.2, 0.2, 0.05);
        }

        player.sendSystemMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage2"));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    // ==================== 第三段：两岸猿声啼不住 ====================
    // ✅ 把生物搬到两岸山上，不击杀；还原时搬回去
    public static void castLiangAnYuanSheng(ServerPlayer player, ServerLevel level) {
        BlockPos center = player.blockPosition();
        double[] dir = APSTerrainManager.getRiverDir(player);
        double riverDirX = dir[0];
        double riverDirZ = dir[1];
        double perpDirX = -riverDirZ;
        double perpDirZ = riverDirX;

        double searchRadius = APSTerrainGenerator.RIVER_HALF_WIDTH
                + APSTerrainGenerator.BANK_WIDTH
                + APSTerrainGenerator.MOUNTAIN_RUN + 20;
        AABB box = player.getBoundingBox().inflate(searchRadius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive()
                        && !(e instanceof ServerPlayer)   // 不搬其他玩家
                        && e.onGround()
                        && !e.isInWater());

        // ✅ 最多搬 8 个
        int maxMove = 8;
        if (targets.size() > maxMove) {
            targets = targets.subList(0, maxMove);
        }

        double targetPerpBase = APSTerrainGenerator.RIVER_HALF_WIDTH
                + APSTerrainGenerator.BANK_WIDTH + 4;

        int hit = 0;
        for (LivingEntity t : targets) {
            // ✅ 记录原位置
            APSTerrainManager.recordMobOrigin(player, t);

            double dx = t.getX() - center.getX();
            double dz = t.getZ() - center.getZ();
            double perp = dx * perpDirX + dz * perpDirZ;
            double along = dx * riverDirX + dz * riverDirZ;

            double sign;
            if (Math.abs(perp) < 0.5) {
                sign = Math.random() < 0.5 ? -1 : 1;
            } else {
                sign = perp >= 0 ? 1 : -1;
            }

            double targetPerp = sign * (targetPerpBase + Math.random() * 6);
            double tx = center.getX() + riverDirX * along + perpDirX * targetPerp;
            double tz = center.getZ() + riverDirZ * along + perpDirZ * targetPerp;

            int ty = APSTerrainGenerator.calculateSwordLandHeight(
                    (int) tx, (int) tz, center, level.getGameTime(),
                    riverDirX, riverDirZ) + 1;

            // ✅ 直接 teleport 到两岸山上，不伤害
            t.teleportTo(tx, ty, tz);
            t.setDeltaMovement(Vec3.ZERO);
            t.hurtMarked = true;

            // 轻微减速（可选）
            t.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 100, 0, false, false, false));
            hit++;
        }

        // 多股剑气：从两岸山体朝河道中心飞
        int perpMax = APSTerrainGenerator.RIVER_HALF_WIDTH
                + APSTerrainGenerator.BANK_WIDTH
                + APSTerrainGenerator.MOUNTAIN_RUN;
        for (int i = 0; i < 40; i++) {
            double sign = Math.random() < 0.5 ? -1 : 1;
            double perp = sign * (APSTerrainGenerator.RIVER_HALF_WIDTH
                    + APSTerrainGenerator.BANK_WIDTH
                    + Math.random() * perpMax);
            double along = (Math.random() - 0.5) * APSTerrainGenerator.LENGTH;

            double x = center.getX() + riverDirX * along + perpDirX * perp;
            double z = center.getZ() + riverDirZ * along + perpDirZ * perp;
            int y = APSTerrainGenerator.calculateSwordLandHeight(
                    (int) x, (int) z, center, level.getGameTime(),
                    riverDirX, riverDirZ) + 3;

            Vec3 fly = new Vec3(-sign * perpDirX, 0, -sign * perpDirZ);
            for (int k = 0; k < 4; k++) {
                double px = x + fly.x * k * 1.5;
                double pz = z + fly.z * k * 1.5;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        px, y, pz, 1, 0, 0, 0, 0);
            }
        }

        // 猿声：音符粒子
        for (int i = 0; i < 60; i++) {
            double sign = Math.random() < 0.5 ? -1 : 1;
            double perp = sign * (APSTerrainGenerator.RIVER_HALF_WIDTH
                    + APSTerrainGenerator.BANK_WIDTH
                    + Math.random() * perpMax);
            double along = (Math.random() - 0.5) * APSTerrainGenerator.LENGTH;
            double x = center.getX() + riverDirX * along + perpDirX * perp;
            double z = center.getZ() + riverDirZ * along + perpDirZ * perp;
            level.sendParticles(ParticleTypes.NOTE,
                    x, center.getY() + 3, z, 1, 0, 0, 0, 0.1);
        }

        player.sendSystemMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage3", hit));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8F, 1.5F);
    }

    // ==================== 第四段：轻舟已过万重山 ====================
    public static void castQingZhouYiGuo(ServerPlayer player, ServerLevel level) {
        BlockPos center = player.blockPosition();
        double fieldRadius = 24.0;

        AABB field = player.getBoundingBox().inflate(fieldRadius);
        List<LivingEntity> inField = level.getEntitiesOfClass(LivingEntity.class, field,
                e -> e != player && e.isAlive());
        for (LivingEntity t : inField) {
            t.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 120, 0, false, true, true));
            t.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 120, 0, false, true, true));
        }

        for (int i = 0; i < 120; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * fieldRadius;
            double x = center.getX() + Math.cos(a) * r;
            double z = center.getZ() + Math.sin(a) * r;
            level.sendParticles(ParticleTypes.ENCHANT,
                    x, center.getY() + 1 + Math.random() * 3, z,
                    1, 0, 0, 0, 0.05);
        }

        spawnGreatSword(player, level, center);

        player.sendSystemMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage4"));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    // ==================== 大剑飞行 ====================
    private static void spawnGreatSword(ServerPlayer player, ServerLevel level, BlockPos center) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.position().add(0, player.getEyeHeight() - 0.2, 0)
                .add(look.scale(1.5));

        double maxDist = 40.0;
        double step = 0.6;
        LivingEntity hitTarget = null;

        for (double d = 0; d < maxDist; d += step) {
            Vec3 p = start.add(look.scale(d));

            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    p.x, p.y, p.z, 3, 0.3, 0.3, 0.3, 0.0);
            level.sendParticles(ParticleTypes.CRIT,
                    p.x, p.y, p.z, 6, 0.2, 0.2, 0.2, 0.1);
            level.sendParticles(ParticleTypes.END_ROD,
                    p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.02);

            AABB hitBox = new AABB(p.x - 1.5, p.y - 1.5, p.z - 1.5,
                    p.x + 1.5, p.y + 1.5, p.z + 1.5);
            List<LivingEntity> hitList = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != player && e.isAlive());
            if (!hitList.isEmpty()) {
                hitTarget = hitList.get(0);
                hitTarget.hurt(player.damageSources().playerAttack(player), 666.0F);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        hitTarget.getX(), hitTarget.getY() + 1, hitTarget.getZ(),
                        1, 0, 0, 0, 0);
                level.playSound(null, hitTarget.getX(), hitTarget.getY(), hitTarget.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.8F);
                break;
            }
        }

        Vec3 slashCenter = (hitTarget != null)
                ? hitTarget.position()
                : start.add(look.scale(maxDist));
        double slashRadius = 12.0;
        AABB slashBox = new AABB(
                slashCenter.x - slashRadius, slashCenter.y - slashRadius, slashCenter.z - slashRadius,
                slashCenter.x + slashRadius, slashCenter.y + slashRadius, slashCenter.z + slashRadius);
        List<LivingEntity> slashTargets = level.getEntitiesOfClass(LivingEntity.class, slashBox,
                e -> e != player && e.isAlive());
        for (LivingEntity t : slashTargets) {
            t.hurt(player.damageSources().playerAttack(player), 20.0F);
        }

        for (int i = 0; i < 180; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * slashRadius;
            double x = slashCenter.x + Math.cos(a) * r;
            double z = slashCenter.z + Math.sin(a) * r;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    x, slashCenter.y + Math.random() * 2, z,
                    1, 0, 0, 0, 0);
        }
        level.playSound(null, slashCenter.x, slashCenter.y, slashCenter.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5F, 0.7F);
    }
}