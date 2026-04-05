package com.phagens.corpseorigin.entity.skills;

import com.phagens.corpseorigin.entity.LongyouEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class LongyouTianGangQi {
    
    // 天罡气二重·疾
    public static void useTianGangQiJi(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放技能动画
            entity.triggerAuraSkill();
            
            // 标记属性已修改（30秒后自动重置）
            entity.markAttributesModified();
            
            // 大幅度提高速度
            entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue() * 2.0D
            );
            
            // 添加速度效果
            entity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, 
                    600, // 30秒
                    3    // 等级4
            ));
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.BREEZE_SHOOT,
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    2.0F, 0.8F);
            
            // 生成粒子效果
            for (int i = 0; i < 20; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                double y = entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight();
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.2, 0.2, 0.2, 0.5);
            }
        }
    }
    
    // 天罡气三重·力
    public static void useTianGangQiLi(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放技能动画
            entity.triggerAuraSkill();
            
            // 标记属性已修改（30秒后自动重置）
            entity.markAttributesModified();
            
            // 身体变小 - 使用实体尺寸属性
            entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(0.75D);
            
            // 大幅度提高力量
            entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() * 2.5D
            );
            
            // 添加力量效果
            entity.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST, 
                    600, // 30秒
                    3    // 等级4
            ));
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.IRON_GOLEM_ATTACK, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    2.0F, 0.8F);
            
            // 生成粒子效果
            for (int i = 0; i < 15; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 1.5;
                double y = entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight();
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 1.5;
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
    
    // 天罡气六重·毁
    public static void useTianGangQiHui(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放技能动画
            entity.triggerAuraSkill();
            
            // 从高空猛击地面
            Vec3 originalPos = entity.position();
            Vec3 jumpPos = originalPos.add(0, 10, 0);
            entity.teleportTo(jumpPos.x, jumpPos.y, jumpPos.z);
            
            // 生成下落粒子
            for (int i = 0; i < 30; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                double y = entity.getY() - i * 0.3;
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.1, 0.1, 0.1, 0.1);
            }
            
            // 猛击地面
            entity.teleportTo(originalPos.x, originalPos.y, originalPos.z);
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.GENERIC_EXPLODE, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    3.0F, 0.7F);
            
            // 生成爆炸粒子
            for (int i = 0; i < 50; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 8.0;
                double y = entity.getY() + entity.getRandom().nextDouble() * 3.0;
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 8.0;
                level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
            }
            
            // 伤害范围内的实体
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, 
                    entity.getBoundingBox().inflate(8.0))) {
                if (living != entity && !(living instanceof com.phagens.corpseorigin.entity.LowerLevelZbEntity)) {
                    living.hurt(level.damageSources().mobAttack(entity), 15.0F);
                    // 震飞效果
                    Vec3 pushDir = living.position().subtract(entity.position()).normalize().scale(1.0);
                    living.setDeltaMovement(pushDir.x, 1.0, pushDir.z);
                }
            }
            
            // 方块抬起效果
            LongyouEarthquakeEntity.create(level, entity.position(), 8.0, 40);
        }
    }
    
    // 天罡气七重·灭
    public static void useTianGangQiMie(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放技能动画
            entity.triggerAuraSkill();
            
            // 获取目标方向
            LivingEntity target = entity.getTarget();
            Vec3 direction = target != null ? 
                    target.position().subtract(entity.position()).normalize() : 
                    entity.getForward();
            
            // 右拳发出红色冲击波
            for (int i = 0; i < 10; i++) {
                double distance = i * 1.5;
                Vec3 pos = entity.position().add(0, entity.getBbHeight() / 2, 0).add(
                        direction.x * distance,
                        0,
                        direction.z * distance
                );
                
                // 生成红色粒子
                for (int j = 0; j < 5; j++) {
                    DustParticleOptions dustOptions =
                            new DustParticleOptions(
                                    new org.joml.Vector3f(1.0F, 0.0F, 0.0F),  // RGB 红色
                                    1.0F  // 大小
                            );
                    level.sendParticles(dustOptions,
                            pos.x + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                            pos.y + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                            pos.z + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                            1, 0.1, 0.1, 0.1, 0.2);
                }
                
                // 伤害范围内的实体
                for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, 
                        entity.getBoundingBox().inflate(2.0).move(direction.x * distance, 0, direction.z * distance))) {
                    if (living != entity && !(living instanceof com.phagens.corpseorigin.entity.LowerLevelZbEntity)) {
                        living.hurt(level.damageSources().mobAttack(entity), 12.0F);
                    }
                }
            }
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.BLAZE_SHOOT, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    2.0F, 0.8F);
        }
    }
    
    // 天罡气八重·无
    public static void useTianGangQiWu(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放技能动画
            entity.triggerAuraSkill();
            
            // 左拳挥出，可破除“虚无之镜”类技能
            Vec3 direction = entity.getForward();
            
            // 生成白色粒子
            for (int i = 0; i < 15; i++) {
                double distance = i * 1.0;
                Vec3 pos = entity.position().add(0, entity.getBbHeight() / 2, 0).add(
                        direction.x * distance,
                        0,
                        direction.z * distance
                );
                
                for (int j = 0; j < 3; j++) {
                    level.sendParticles(ParticleTypes.WHITE_ASH, 
                            pos.x + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                            pos.y + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                            pos.z + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                            1, 0.1, 0.1, 0.1, 0.2);
                }
            }
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.TRIDENT_THROW, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    2.0F, 0.8F);
            
            // 这里可以添加破除虚无之镜的逻辑
        }
    }
    
    // 天罡气九重·神
    public static void useTianGangQiShen(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放技能动画
            entity.triggerAuraSkill();
            
            // 标记属性已修改（30秒后自动重置）
            entity.markAttributesModified();
            
            // 八重合一，增强所有属性
            entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getBaseValue() * 1.5D
            );
            entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue() * 1.5D
            );
            entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).getBaseValue() + 5.0D
            );
            
            // 添加各种效果
            entity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, 
                    600, // 30秒
                    2    // 等级3
            ));
            entity.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST, 
                    600, // 30秒
                    2    // 等级3
            ));
            entity.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE, 
                    600, // 30秒
                    2    // 等级3
            ));
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.ENDER_DRAGON_GROWL, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    3.0F, 0.8F);
            
            // 生成粒子效果
            for (int i = 0; i < 50; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 3.0;
                double y = entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight() * 2;
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 3.0;
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
    
    // 逆破拳
    public static void useNiPoQuan(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            // 播放技能动画
            entity.triggerAuraSkill();
            
            // 启动逆破拳状态（由tick方法处理连续攻击）
            entity.startNiPoQuan();
            
            // 播放音效
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.PLAYER_ATTACK_STRONG, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    2.0F, 0.8F);
        }
    }
    
    // 破罡
    public static void usePoGang(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放技能动画
            entity.triggerAuraSkill();
            
            // 可击破内力防御类招式
            LivingEntity target = entity.getTarget();
            if (target != null) {
                // 生成破除防御的粒子
                for (int i = 0; i < 20; i++) {
                    double x = target.getX() + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                    double y = target.getY() + target.getBbHeight() * 0.5 + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                    double z = target.getZ() + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                    level.sendParticles(ParticleTypes.CRIT, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
                }
                
                // 造成伤害并忽略防御
                target.hurt(level.damageSources().magic(), 15.0F);
            }
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.SHIELD_BLOCK, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    2.0F, 0.8F);
        }
    }
    
    // 天罡破
    public static void useTianGangPo(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放技能动画
            entity.triggerAuraSkill();
            
            // 从空中向下进攻，发出极强冲击波
            Vec3 originalPos = entity.position();
            Vec3 jumpPos = originalPos.add(0, 15, 0);
            entity.teleportTo(jumpPos.x, jumpPos.y, jumpPos.z);
            
            // 生成下落粒子
            for (int i = 0; i < 50; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                double y = entity.getY() - i * 0.3;
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.1, 0.1, 0.1, 0.1);
            }
            
            // 猛击地面
            entity.teleportTo(originalPos.x, originalPos.y, originalPos.z);
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.GENERIC_EXPLODE, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    4.0F, 0.6F);
            
            // 生成爆炸粒子
            for (int i = 0; i < 100; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 12.0;
                double y = entity.getY() + entity.getRandom().nextDouble() * 5.0;
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 12.0;
                level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
            }
            
            // 伤害范围内的实体
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, 
                    entity.getBoundingBox().inflate(12.0))) {
                if (living != entity && !(living instanceof com.phagens.corpseorigin.entity.LowerLevelZbEntity)) {
                    living.hurt(level.damageSources().mobAttack(entity), 20.0F);
                    // 震飞效果
                    Vec3 pushDir = living.position().subtract(entity.position()).normalize().scale(1.5);
                    living.setDeltaMovement(pushDir.x, 1.5, pushDir.z);
                }
            }

            // 方块抬起效果 - 更大的范围
            LongyouEarthquakeEntity.create(level, entity.position(), 12.0, 50);
        }
    }
    

}
