package com.phagens.corpseorigin.entity.skills;

import com.phagens.corpseorigin.entity.LongyouEntity;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class LongyouSkills {
    
    public static void useXuanwuBody(LongyouEntity entity) {
        if (!entity.level().isClientSide) {
            ServerLevel level = (ServerLevel) entity.level();
            
            // 播放玄武体激活动画
            entity.triggerAuraSkill();
            // 触发技能1动画
            entity.getEntityData().set(LongyouEntity.DATA_PLAYING_SKILL_1, true);
            entity.skill1AnimationTicks = 40; // 2秒动画
            
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
            
            // 检测附近的尸兄
            List<LowerLevelZbEntity> nearbyZombies = level.getEntitiesOfClass(
                    com.phagens.corpseorigin.entity.LowerLevelZbEntity.class,
                    entity.getBoundingBox().inflate(64.0) // 次声波范围
            );
            
            if (!nearbyZombies.isEmpty()) {
                // 召集附近的尸兄
                for (com.phagens.corpseorigin.entity.LowerLevelZbEntity zb : nearbyZombies) {
                    // 让尸兄朝向龙右并开始移动
                    zb.getNavigation().moveTo(entity, 1.2);
                    
                    // 生成粒子效果
                    for (int j = 0; j < 3; j++) {
                        double x = zb.getX() + (entity.getRandom().nextDouble() - 0.5) * 1.0;
                        double y = zb.getY() + entity.getRandom().nextDouble() * zb.getBbHeight();
                        double z = zb.getZ() + (entity.getRandom().nextDouble() - 0.5) * 1.0;
                        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
                    }
                }
                
                // 播放次声波音效
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
                        SoundEvents.WARDEN_SONIC_BOOM, 
                        net.minecraft.sounds.SoundSource.HOSTILE, 
                        3.0F, 0.8F);
            } else {
                // 如果没有尸兄，使附近的中立生物发怒
                AABB bounds = entity.getBoundingBox().inflate(32.0);
                List<LivingEntity> nearbyNeutralMobs = level.getEntitiesOfClass(
                        LivingEntity.class,
                        bounds,
                        (mob) -> {
                            // 过滤出中立生物
                            return mob instanceof IronGolem ||
                                    mob instanceof Spider ||
                                    mob instanceof PolarBear;
                        }
                );

                for (LivingEntity livingEntity : nearbyNeutralMobs) {
                    // 使中立生物发怒
                    if (livingEntity instanceof IronGolem ironGolem) {
                        ironGolem.setTarget(entity);
                    } else if (livingEntity instanceof Spider spider) {
                        spider.setTarget(entity);
                    } else if (livingEntity instanceof PolarBear polarBear) {
                        polarBear.setTarget(entity);
                    }

                    // 生成粒子效果
                    for (int j = 0; j < 3; j++) {
                        double x = livingEntity.getX() + (entity.getRandom().nextDouble() - 0.5) * 1.0;
                        double y = livingEntity.getY() + entity.getRandom().nextDouble() * livingEntity.getBbHeight();
                        double z = livingEntity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 1.0;
                        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);
                    }
                }

                // 播放次声波音效
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.WARDEN_SONIC_BOOM,
                        net.minecraft.sounds.SoundSource.HOSTILE,
                        3.0F, 0.6F);
            }
        }
    }
}
