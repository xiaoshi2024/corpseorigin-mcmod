package com.phagens.corpseorigin.GongFU.JSskill;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.JSskill.Factory.ParticlePatternFactory;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import static net.minecraft.commands.arguments.ParticleArgument.getParticle;

public class SkillEffects {
    /**
     * 在玩家周围生成粒子效果
     *
     * @param player 施法玩家
     * @param particleId 粒子 ID（如 "minecraft:cloud"）
     * @param count 粒子数量
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @param offsetX X 扩散范围
     * @param offsetY Y 扩散范围
     * @param offsetZ Z 扩散范围
     * @param speed 粒子速度
     */
    public static void spawnParticles(ServerPlayer player, String particleId, int count,
                                      double x, double y, double z,
                                      double offsetX, double offsetY, double offsetZ, double speed) {
        try {
            // 根据 ID 获取粒子类型
            ParticleOptions particle = getParticle(particleId);
            if (particle == null) return;

            // 创建粒子数据包
            var packet = new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
                    particle, true,
                    (float) x, (float) y, (float) z,
                    (float) offsetX, (float) offsetY, (float) offsetZ,
                    (float) speed, count
            );

            player.connection.send(packet);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成粒子失败：{}", particleId, e);
        }
    }

    /**
     * 播放音效
     *
     * @param player 施法玩家
     * @param soundId 音效 ID（如 "minecraft:block.anvil.place"）
     * @param volume 音量 (0.0-1.0)
     * @param pitch 音调 (0.5-2.0)
     */
    public static void playSound(ServerPlayer player, String soundId, float volume, float pitch) {
        try {
            // 解析音效 ID
            ResourceLocation rl = ResourceLocation.parse(soundId);
            var soundEvent = BuiltInRegistries.SOUND_EVENT.get(rl);

            if (soundEvent != null) {  // ✅ 直接判断是否为 null
                player.serverLevel().playSound(
                        player,
                        player.getX(), player.getY(), player.getZ(),
                        soundEvent,   // ✅ 直接使用 soundEvent
                        SoundSource.PLAYERS,
                        volume, pitch
                );
            } else {
                CorpseOrigin.LOGGER.warn("未找到音效：{}", soundId);
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("播放音效失败：{}", soundId, e);
        }
    }

    /**
     * 给玩家添加状态效果
     *
     * @param player 目标玩家
     * @param effectId 效果 ID（如 "minecraft:resistance"）
     * @param duration 持续时间（tick，20tick=1 秒）
     * @param amplifier 效果等级（0=I 级，1=II 级）
     */
    public static void addEffect(ServerPlayer player, String effectId, int duration, int amplifier) {
        try {
            // 解析效果 ID
            ResourceLocation rl = ResourceLocation.parse(effectId);
            var effect = BuiltInRegistries.MOB_EFFECT.get(rl);

            if (effect != null) {
                var effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
                player.addEffect(new MobEffectInstance(
                        effectHolder,
                        duration,
                        amplifier,
                        false,
                        false
                ));
            } else {
                CorpseOrigin.LOGGER.warn("未找到效果：{}", effectId);
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("添加效果失败：{}", effectId, e);
        }
    }

    /**
     * 对生物造成伤害
     *
     * @param attacker 攻击者
     * @param target 目标生物
     * @param amount 伤害值
     */
    public static void damageTarget(ServerPlayer attacker, LivingEntity target, float amount) {
        try {
            // 创建玩家攻击伤害来源
            DamageSource damageSource = createPlayerDamageSource(attacker);
            if (damageSource != null) {
                target.hurt(damageSource, amount);
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("造成伤害失败", e);
        }
    }

    /**
     * 对目标造成魔法伤害
     *
     * @param attacker 攻击者
     * @param target 目标生物
     * @param amount 伤害值
     */
    public static void magicDamage(ServerPlayer attacker, LivingEntity target, float amount) {
        try {
            DamageSource damageSource = createMagicDamageSource(attacker);
            if (damageSource != null) {
                target.hurt(damageSource, amount);
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("魔法伤害失败", e);
        }
    }

    /**
     * 击退目标
     *
     * @param attacker 攻击者
     * @param target 目标生物
     * @param strength 击退强度 (0.0-2.0)
     */
    public static void knockback(ServerPlayer attacker, LivingEntity target, double strength) {
        try {
            double dx = target.getX() - attacker.getX();
            double dz = target.getZ() - attacker.getZ();

            // 归一化方向向量
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0) {
                dx /= distance;
                dz /= distance;
            }

            // 应用击退
            target.push(dx * strength, 0.5, dz * strength);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("击退失败", e);
        }
    }

    /**
     * 治疗目标
     *
     * @param target 目标生物
     * @param amount 治疗量
     */
    public static void heal(LivingEntity target, float amount) {
        try {
            target.heal(amount);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("治疗失败", e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取粒子类型
     */
    private static ParticleOptions getParticle(String id) {
        try {
            ResourceLocation rl = ResourceLocation.parse(id);
            // ✅ 直接从注册表获取已注册的粒子类型
            var particleType = BuiltInRegistries.PARTICLE_TYPE.get(rl);

            if (particleType != null) {
                return getParticleOptionsFromType(particleType);
            } else {
                CorpseOrigin.LOGGER.warn("粒子类型不存在：{}，使用默认", id);
                return ParticleTypes.CLOUD;
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("粒子类型不存在：{}，使用默认", id);
            return ParticleTypes.CLOUD;
        }
    }

    /**
     * 从 ParticleType 获取 ParticleOptions
     */
    private static ParticleOptions getParticleOptionsFromType(net.minecraft.core.particles.ParticleType<?> particleType) {
        // ✅ 其他类型尝试直接转换
        try {
            return (ParticleOptions) particleType;
        } catch (ClassCastException e) {
            return ParticleTypes.CLOUD;
        }
    }

    /**
     * 创建玩家攻击伤害来源
     */
    private static DamageSource createPlayerDamageSource(ServerPlayer player) {
        try {
            // ✅ 使用 DamageSources 获取标准的伤害类型
            return player.damageSources().playerAttack(player);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("创建玩家伤害来源失败", e);
        }
        return null;
    }

    /**
     * 创建魔法伤害来源
     */
    private static DamageSource createMagicDamageSource(ServerPlayer player) {
        try {
            // ✅ 使用 DamageSources 获取标准的魔法伤害类型
            return player.damageSources().magic();
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("创建魔法伤害来源失败", e);
        }
        return null;
    }

    /**
     * 生成龙卷风粒子图案
     */
    public static void spawnTornadoPattern(ServerPlayer player, double x, double y, double z,
                                           double height, double radius, String particleId,
                                           int count, double speed) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnTornado(player.serverLevel(), x, y, z, height, radius, particle, count, speed);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成龙卷风粒子失败", e);
        }
    }

    /**
     * 生成椭圆形粒子环
     */
    public static void spawnEllipsePattern(ServerPlayer player, double x, double y, double z,
                                           double radiusX, double radiusZ, String particleId,
                                           int count, double yOffset) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnEllipse(player.serverLevel(), x, y, z, radiusX, radiusZ, particle, count, yOffset);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成椭圆形粒子失败", e);
        }
    }

    /**
     * 生成圆弧形粒子
     */
    public static void spawnArcPattern(ServerPlayer player, double x, double y, double z,
                                       double radius, double startAngle, double endAngle,
                                       String particleId, int count, double yOffset) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnArc(player.serverLevel(), x, y, z, radius, startAngle, endAngle, particle, count, yOffset);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成圆弧形粒子失败", e);
        }
    }

    /**
     * 生成奥运五环图案
     */
    public static void spawnOlympicRingsPattern(ServerPlayer player, double x, double y, double z,
                                                double ringRadius, String particleId, int particlesPerRing) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnOlympicRings(player.serverLevel(), x, y, z, ringRadius, particle, particlesPerRing);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成奥运五环失败", e);
        }
    }

    /**
     * 生成地面图案
     */
    public static void spawnGroundPattern(ServerPlayer player, double x, double y, double z,
                                          String pattern, double size, String particleId, int density) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnGroundPattern(player.serverLevel(), x, y, z, pattern, size, particle, density);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成地面图案失败", e);
        }
    }

    /**
     * 生成球形粒子
     */
    public static void spawnSpherePattern(ServerPlayer player, double x, double y, double z,
                                          double radius, String particleId, int count) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnSphere(player.serverLevel(), x, y, z, radius, particle, count);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成球形粒子失败", e);
        }
    }

    /**
     * 生成柱状粒子
     */
    public static void spawnCylinderPattern(ServerPlayer player, double x, double y, double z,
                                            double radius, double height, String particleId, int count) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnCylinder(player.serverLevel(), x, y, z, radius, height, particle, count);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成柱状粒子失败", e);
        }
    }

    /**
     * 生成心形图案
     */
    public static void spawnHeartPattern(ServerPlayer player, double x, double y, double z,
                                         double size, String particleId, int count) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnHeart(player.serverLevel(), x, y, z, size, particle, count);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成心形图案失败", e);
        }
    }
    /**
     * 生成斩击特效
     */
    public static void spawnSlashingAttack(ServerPlayer player, double startX, double startY, double startZ,
                                           double dirX, double dirZ, double distance,
                                           double width, double height, String particleId,
                                           int segments, double speed) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnSlashingAttack(player.serverLevel(), startX, startY, startZ,
                    dirX, dirZ, distance, width, height, particle, segments, speed);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成斩击特效失败", e);
        }
    }

    /**
     * 生成旋转魔法阵
     */
    public static void spawnRotatingMagicCircle(ServerPlayer player, double x, double y, double z,
                                                double radius, int layers, double rotationAngle,
                                                String particleId, int particlesPerLayer,
                                                double rotationSpeed) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnRotatingMagicCircle(player.serverLevel(), x, y, z,
                    radius, layers, rotationAngle, particle, particlesPerLayer, rotationSpeed);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成旋转魔法阵失败", e);
        }
    }

    /**
     * 生成升腾漩涡
     */
    public static void spawnAscendingSwirl(ServerPlayer player, double x, double y, double z,
                                           double height, double radius, String particleId,
                                           int count, double swirlIntensity) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnAscendingSwirl(player.serverLevel(), x, y, z,
                    height, radius, particle, count, swirlIntensity);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成升腾漩涡失败", e);
        }
    }

    /**
     * 生成环绕轨道
     */
    public static void spawnOrbitRings(ServerPlayer player, double x, double y, double z,
                                       int orbits, double orbitRadius, double tiltAngle,
                                       String particleId, int particlesPerOrbit) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnOrbitRings(player.serverLevel(), x, y, z,
                    orbits, orbitRadius, tiltAngle, particle, particlesPerOrbit);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成环绕轨道失败", e);
        }
    }

    /**
     * 生成爆炸冲击波
     */
    public static void spawnShockwave(ServerPlayer player, double x, double y, double z,
                                      double maxRadius, String particleId, int density) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnShockwave(player.serverLevel(), x, y, z,
                    maxRadius, particle, density);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成冲击波失败", e);
        }
    }

    /**
     * 生成螺旋弹道
     */
    public static void spawnSpiralProjectile(ServerPlayer player, double startX, double startY, double startZ,
                                             double endX, double endY, double endZ,
                                             double rotations, double radius, String particleId,
                                             int count) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnSpiralProjectile(player.serverLevel(), startX, startY, startZ,
                    endX, endY, endZ, rotations, radius, particle, count);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成螺旋弹道失败", e);
        }
    }

    /**
     * 生成羽翼展开
     */
    public static void spawnWingExpansion(ServerPlayer player, double x, double y, double z,
                                          double wingspan, double featherLength, String particleId,
                                          int density) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnWingExpansion(player.serverLevel(), x, y, z,
                    wingspan, featherLength, particle, density);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成羽翼展开失败", e);
        }
    }

    /**
     * 生成锁链连接
     */
    public static void spawnChainLinks(ServerPlayer player, double x1, double y1, double z1,
                                       double x2, double y2, double z2,
                                       int chainCount, double sagAmount, String particleId) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnChainLinks(player.serverLevel(), x1, y1, z1,
                    x2, y2, z2, chainCount, sagAmount, particle);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成锁链失败", e);
        }
    }

    /**
     * 生成领域展开
     */
    public static void spawnDomainExpansion(ServerPlayer player, double x, double y, double z,
                                            double maxRadius, int shells, String particleId,
                                            int particlesPerShell) {
        try {
            ParticleOptions particle = getParticle(particleId);
            ParticlePatternFactory.spawnDomainExpansion(player.serverLevel(), x, y, z,
                    maxRadius, shells, particle, particlesPerShell);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("生成领域展开失败", e);
        }
    }
}
