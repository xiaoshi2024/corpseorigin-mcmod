package xiaoshi2022.corpseorigin.skill.baixiaofei;

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
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager;

import java.util.List;

/**
 * 古仙剑·四段子技能实现
 */
public class APSSubSkills {

    // ==================== 第一段：朝辞白帝彩云间 ====================
    public static void castZhaoCiBaiDi(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, 0.3, look.z).normalize().scale(1.5);
        player.setDeltaMovement(dash);
        player.hurtMarked = true;

        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED, 100, 1, false, true, true));

        level.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 1, player.getZ(),
                30, 0.5, 0.5, 0.5, 0.05);
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1, player.getZ(),
                15, 0.3, 0.3, 0.3, 0.02);

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

        for (int i = 0; i < 20; i++) {
            Vec3 p = player.position().add(look.scale(i * 0.3));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    p.x, p.y + 0.5, p.z, 1, 0, 0, 0, 0);
        }

        player.sendSystemMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage2"));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    // ==================== 第三段：两岸猿声啼不住 ====================
    public static void castLiangAnYuanSheng(ServerPlayer player, ServerLevel level) {
        double radius = 8.0;
        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        int hit = 0;
        for (LivingEntity t : targets) {
            t.hurt(player.damageSources().playerAttack(player), 6.0F);
            t.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, 100, 2, false, true, true));
            t.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 100, 1, false, true, true));
            hit++;
        }

        for (int i = 0; i < 60; i++) {
            double angle = Math.random() * Math.PI * 2;
            double r = Math.random() * radius;
            double x = player.getX() + Math.cos(angle) * r;
            double z = player.getZ() + Math.sin(angle) * r;
            level.sendParticles(ParticleTypes.NOTE,
                    x, player.getY() + 1, z, 1, 0, 0, 0, 0.1);
        }

        player.sendSystemMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage3", hit));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8F, 1.5F);
    }

    // ==================== 第四段：轻舟已过万重山 ====================
    public static void castQingZhouYiGuo(ServerPlayer player, ServerLevel level) {
        // 调用 APS 地形改造：劈山落瀑布
        APSTerrainManager.toggleTransformation(player, player.getMainHandItem(), level);

        player.sendSystemMessage(Component.translatable(
                "skill.corpseorigin.ancient_poetry_sword.stage4"));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}