package xiaoshi2022.corpseorigin.skill.baixiaofei;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.entity.FlyingGreatSwordEntity;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainGenerator;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager;

import java.util.List;

public class APSSubSkills {

    // ==================== 第一段 ====================
    public static void castZhaoCiBaiDi(ServerPlayer player, ServerLevel level) {
        BlockPos center = player.blockPosition();
        double[] dir = APSTerrainManager.getRiverDir(player);

        APSTerrainManager.removeMobsInRealm(player, level, center, dir[0], dir[1]);

        // 天光垂落 + 低空云气：整片区域采样收敛成 8 处气团（原来 300 次逐点撒粒子）
        for (int i = 0; i < 8; i++) {
            double along = (Math.random() - 0.5) * APSTerrainGenerator.LENGTH;
            double perp = (Math.random() - 0.5) * (APSTerrainGenerator.MOUNTAIN_RUN * 2);
            double x = center.getX() + dir[0] * along + (-dir[1]) * perp;
            double z = center.getZ() + dir[1] * along + dir[0] * perp;
            double y = center.getY() + 40 + Math.random() * 30;

            QiEffects.cloud(level, new Vec3(x, y, z), 0xdcefff, 3f, 14);
            QiEffects.cloud(level, new Vec3(x, y - 10, z), 0x9aa4b0, 3.5f, 14);
        }

        APSTerrainManager.toggleTransformation(player, player.getMainHandItem(), level);

        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, 0.3, look.z).normalize().scale(1.5);
        player.setDeltaMovement(dash);
        player.hurtMarked = true;

        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED, 100, 1, false, true, true));

        // 围绕自身升腾的云气：收敛成 6 处气团（原来 80 次逐点撒粒子）
        for (int i = 0; i < 6; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 5 + Math.random() * 20;
            double x = center.getX() + Math.cos(a) * r;
            double z = center.getZ() + Math.sin(a) * r;
            QiEffects.cloud(level, new Vec3(x, center.getY() + 2 + Math.random() * 6, z), 0x9aa4b0, 3f, 14);
            QiEffects.cloud(level, new Vec3(x, center.getY() + 3, z), 0xdcefff, 2.5f, 14);
        }

        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage1"));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.5F);
    }

    // ==================== 第二段 ====================
    public static void castQianLiJiangLing(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, 0.1, look.z).normalize().scale(3.5);
        player.setDeltaMovement(dash);
        player.hurtMarked = true;

        CompoundTag state = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
        state.putInt("aps_dash_ticks", 10);
        player.setAttached(ModDataAttachments.APS_STATE, state);

        double[] dir = APSTerrainManager.getRiverDir(player);
        Vec3 river = new Vec3(dir[0], 0, dir[1]).normalize();

        QiEffects.aura(player, "aps_release", 0xdcefff, 2, 12);
        for (int i = 0; i < 8; i++) {
            Vec3 p = player.position().add(river.scale(i * 1.2)).add(0, .5, 0);
            QiEffects.cloud(level, p, 0xdcefff, .85f, 10);
        }

        CorpseNetwork.broadcastInkPoem(player, 1);

        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage2"));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    // ==================== 第三段 ====================
    public static void castLiangAnYuanSheng(ServerPlayer player, ServerLevel level) {
        BlockPos center = player.blockPosition();
        double[] dir = APSTerrainManager.getRiverDir(player);
        double riverDirX = dir[0];
        double riverDirZ = dir[1];
        double perpDirX = -riverDirZ;
        double perpDirZ = riverDirX;

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
            for (int k = 0; k < 2; k++) {
                double px = x + fly.x * k * 3;
                double pz = z + fly.z * k * 3;
                QiEffects.cloud(level, new Vec3(px, y, pz), 0xdcefff, 1.5f, 14);
            }
        }

        // 沿岸的余韵：收敛成 8 处气团（原来 60 次逐点撒粒子）
        for (int i = 0; i < 8; i++) {
            double sign = Math.random() < 0.5 ? -1 : 1;
            double perp = sign * (APSTerrainGenerator.RIVER_HALF_WIDTH
                    + APSTerrainGenerator.BANK_WIDTH
                    + Math.random() * perpMax);
            double along = (Math.random() - 0.5) * APSTerrainGenerator.LENGTH;
            double x = center.getX() + riverDirX * along + perpDirX * perp;
            double z = center.getZ() + riverDirZ * along + perpDirZ * perp;
            QiEffects.cloud(level, new Vec3(x, center.getY() + 3, z), 0x39c5bb, 2.5f, 12);
        }

        CorpseNetwork.broadcastInkPoem(player, 2);

        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage3", 8));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8F, 1.5F);
    }

    // ==================== 第四段 ====================
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

        for (int i = 0; i < 24; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * fieldRadius;
            double x = center.getX() + Math.cos(a) * r;
            double z = center.getZ() + Math.sin(a) * r;
            QiEffects.cloud(level, new Vec3(x, center.getY() + 1 + Math.random() * 3, z),
                    0xdcefff, 1.4f, 16);
        }

        // ✅ 玩家挥刀 + 统一工厂发射大剑
        QiEffects.aura(player, "aps_release", 0xdcefff, 2.5f, 18);
        player.swing(InteractionHand.MAIN_HAND, true);

        FlyingGreatSwordEntity.spawnDirected(
                level, player, player.getMainHandItem(),
                3.5F,
                0f
        );

        CorpseNetwork.broadcastInkPoem(player, 3);

        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage4"));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}
