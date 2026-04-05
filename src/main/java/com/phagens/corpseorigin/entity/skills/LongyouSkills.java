package com.phagens.corpseorigin.entity.skills;

import com.phagens.corpseorigin.entity.LongyouEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class LongyouSkills {
    
    public static void useXuanwuBody(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放玄武体激活动画
            entity.triggerAuraSkill();
            
            // 标记属性已修改（30秒后自动重置）
            entity.markAttributesModified();
            
            // 增加护甲和抗性
            entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).getBaseValue() + 10.0D
            );
            
            // 添加抗性效果
            entity.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE, 
                    600, // 30秒
                    2    // 等级3
            ));
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.IRON_GOLEM_HURT, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    2.0F, 0.8F);
            
            // 生成粒子效果
            for (int i = 0; i < 20; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                double y = entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight();
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 2.0;
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
    
    public static void useGeckoTechnique(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 恢复生命值
            float healAmount = entity.getMaxHealth() * 0.3F;
            entity.heal(healAmount);
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.ZOMBIE_VILLAGER_CURE, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    1.5F, 0.9F);
            
            // 生成粒子效果
            for (int i = 0; i < 15; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 1.5;
                double y = entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight();
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 1.5;
                level.sendParticles(ParticleTypes.HEART, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
    
    public static void useTianGangQi(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放天罡气技能动画
            entity.triggerAuraSkill();
            
            // 获取目标方向
            LivingEntity target = entity.getTarget();
            Vec3 direction = target != null ? 
                    target.position().subtract(entity.position()).normalize() : 
                    entity.getForward();
            
            // 释放能量波
            for (int i = 0; i < 5; i++) {
                double distance = i * 2.0;
                Vec3 pos = entity.position().add(0, entity.getBbHeight() / 2, 0).add(
                        direction.x * distance,
                        0,
                        direction.z * distance
                );
                
                // 生成粒子效果
                for (int j = 0; j < 3; j++) {
                    level.sendParticles(ParticleTypes.FLAME, 
                            pos.x + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                            pos.y + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                            pos.z + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                            1, 0.1, 0.1, 0.1, 0.2);
                }
                
                // 伤害范围内的实体
                for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, 
                        entity.getBoundingBox().inflate(1.0).move(direction.x * distance, 0, direction.z * distance))) {
                    if (living != entity && !(living instanceof com.phagens.corpseorigin.entity.LowerLevelZbEntity)) {
                        living.hurt(level.damageSources().mobAttack(entity), 10.0F);
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

    // 修复 useEarthquake 方法
    public static void useEarthquake(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();

            // 播放地震动画
            entity.triggerAuraSkill();

            // 创建地震效果实体，带方块翻动
            LongyouEarthquakeEntity.create(level, entity.position(), 8.0, 40);

            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.HOSTILE,
                    3.0F, 0.7F);
        }
    }
    
    public static void useSummonMinions(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放召唤动画
            entity.triggerAuraSkill();
            
            // 召唤尸兄手下
            for (int i = 0; i < 3; i++) {
                double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 4.0;
                double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 4.0;
                double y = entity.getY(); // 简化为实体当前高度
                
                // 生成粒子效果
                for (int j = 0; j < 5; j++) {
                    level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
                }
                
                // 召唤尸兄
                com.phagens.corpseorigin.entity.LowerLevelZbEntity zb = 
                        new com.phagens.corpseorigin.entity.LowerLevelZbEntity(
                                com.phagens.corpseorigin.register.EntityRegistry.LOWER_LEVEL_ZB.get(),
                                level
                        );
                zb.setPos(x, y, z);
                level.addFreshEntity(zb);
            }
            
            // 播放音效
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                    SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 
                    net.minecraft.sounds.SoundSource.HOSTILE, 
                    2.0F, 0.8F);
        }
    }
}
