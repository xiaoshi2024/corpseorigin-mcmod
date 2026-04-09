package com.phagens.corpseorigin.GongFU.JSskill.Factory;

import com.phagens.corpseorigin.GongFU.FaXiang.FaxiangEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 通用飞行道具管理器
 * 【功能说明】
 * 1. 统一管理所有飞行投射物（剑气、火球、冰锥等）
 * 2. 每 tick 自动更新位置和检测碰撞
 * 3. 支持自定义粒子、音效、命中回调
 * 【使用方式】
 * - 粒子模式：createProjectile() - 轻量级，适合剑气、魔法弹
 * - 实体模式：spawnEntityProjectile() - 真实实体，适合箭矢、火球
 * - 自定义实体：spawnCustomProjectile() - 完全自定义
 */
public class ProjectileManager {
    private static final ProjectileManager INSTANCE = new ProjectileManager();
    private final List<GenericProjectile> projectiles = new ArrayList<>();
    public static ProjectileManager getInstance() {
        return INSTANCE;
    }

    /**
     * 创建通用飞行投射物（支持任意实体发射）
     * @param shooter      射击者（可以是玩家、怪物、NPC等），用于排除自身和计算伤害来源
     * @param startPos     起始位置（Vec3），投射物生成的世界坐标
     * @param direction    飞行方向（Vec3），会自动归一化为单位向量
     * @param damage       伤害值，命中时造成的魔法/物理伤害
     * @param maxRange     最大射程，超过此距离后投射物自动消失
     * @param speed        飞行速度，每 tick 移动的格数（建议 0.5-2.0）
     * @param particleType 粒子类型字符串，如 "enchanted_hit"、"flame"、"cloud"
     * @param hitboxSize   碰撞箱大小，以投射物为中心的立方体边长的一半
     * @param hitSound     命中音效（可选），如 "minecraft:entity.generic.explode"，传 null 则不播放
     * @param onHit        命中回调（可选），命中目标后执行的自定义逻辑，传 null 则只造成伤害
     * @param particleCount   每tick粒子数量
     * @param particleOffsetX X轴扩散范围
     * @param particleOffsetY Y轴扩散范围
     * @param particleOffsetZ Z轴扩散范围
     * @param particleSpeed   粒子扩散速度
     */
    public void createProjectile(LivingEntity shooter, Vec3 startPos, Vec3 direction,
                                 double damage, double maxRange, double speed,
                                 String particleType, double hitboxSize,
                                 @Nullable String hitSound, @Nullable Consumer<LivingEntity> onHit,
                                 int particleCount,
                                 double particleOffsetX, double particleOffsetY, double particleOffsetZ,
                                 double particleSpeed) {
        GenericProjectile projectile = new GenericProjectile(
                shooter, startPos, direction, damage, maxRange, speed,
                particleType, hitboxSize, hitSound, onHit
        );

        projectile.particleCount = particleCount;
        projectile.particleOffsetX = particleOffsetX;
        projectile.particleOffsetY = particleOffsetY;
        projectile.particleOffsetZ = particleOffsetZ;
        projectile.particleSpeed = particleSpeed;
        projectiles.add(projectile);

    }


    /**
     * 创建追踪飞行投射物（完整粒子参数）
     * @param shooter         射击者
     * @param startPos        起始位置
     * @param direction       初始飞行方向
     * @param damage          伤害值
     * @param maxRange        最大射程
     * @param speed           飞行速度
     * @param particleType    粒子类型
     * @param hitboxSize      碰撞箱大小
     * @param hitSound        命中音效
     * @param onHit           命中回调
     * @param turnRate        转向灵敏度（0.0-1.0）
     * @param particleCount   每tick粒子数量
     * @param particleOffsetX X轴扩散范围
     * @param particleOffsetY Y轴扩散范围
     * @param particleOffsetZ Z轴扩散范围
     * @param particleSpeed   粒子扩散速度
     */
    public void createHomingProjectile(LivingEntity shooter, Vec3 startPos, Vec3 direction,
                                       double damage, double maxRange, double speed,
                                       String particleType, double hitboxSize,
                                       @Nullable String hitSound, @Nullable Consumer<LivingEntity> onHit,
                                       double turnRate, int particleCount,
                                       double particleOffsetX, double particleOffsetY, double particleOffsetZ,
                                       double particleSpeed) {
        GenericProjectile projectile = new GenericProjectile(
                shooter, startPos, direction, damage, maxRange, speed,
                particleType, hitboxSize, hitSound, onHit
        );
        projectile.turnRate = turnRate;
        projectile.particleCount = particleCount;
        projectile.particleOffsetX = particleOffsetX;
        projectile.particleOffsetY = particleOffsetY;
        projectile.particleOffsetZ = particleOffsetZ;
        projectile.particleSpeed = particleSpeed;
        projectiles.add(projectile);
    }

    /**
     * 创建追踪飞行投射物（简化版，默认粒子参数）
     */
    public void createHomingProjectile(LivingEntity shooter, Vec3 startPos, Vec3 direction,
                                       double damage, double maxRange, double speed,
                                       String particleType, double hitboxSize,
                                       @Nullable String hitSound, @Nullable Consumer<LivingEntity> onHit,
                                       double turnRate) {
        createHomingProjectile(shooter, startPos, direction, damage, maxRange, speed,
                particleType, hitboxSize, hitSound, onHit, turnRate, 1, 0.1, 0.1, 0.1, 0);
    }

    /**
     * 生成真实的 Minecraft 实体投射物
     *
     * @param shooter    射击者（玩家、怪物等）
     * @param entityType 实体类型（如 EntityType.ARROW、EntityType.FIREBALL）
     * @param startPos   起始位置
     * @param direction  飞行方向
     * @param power      威力/速度系数
     * @param configure  配置回调（可选），用于自定义实体属性
     * @return 生成的实体，失败返回 null
     */
    @Nullable
    public Entity spawnEntityProjectile(LivingEntity shooter, EntityType<?> entityType,
                                        Vec3 startPos, Vec3 direction, double power,
                                        @Nullable Consumer<Entity> configure) {
        try {
            ServerLevel level = (ServerLevel) shooter.level();

            Entity entity = entityType.create(level);
            if (entity == null) {
                return null;
            }

            entity.setPos(startPos.x, startPos.y, startPos.z);

            if (entity instanceof AbstractArrow arrow) {
                arrow.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0, (float)power, 1.0F);
                if (shooter instanceof net.minecraft.server.level.ServerPlayer player) {
                    arrow.setOwner(player);
                }
            } else if (entity instanceof Fireball fireball) {
                Vec3 motion = direction.normalize().scale(power);
                fireball.setDeltaMovement(motion);
                fireball.setOwner(shooter);
            } else if (entity instanceof Snowball snowball) {
                snowball.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0, (float)power, 1.0F);
                snowball.setOwner(shooter);
            } else {
                Vec3 motion = direction.normalize().scale(power);
                entity.setDeltaMovement(motion);
            }

            if (configure != null) {
                configure.accept(entity);
            }

            level.addFreshEntity(entity);
            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成自定义实体投射物（通过工厂函数）
     *
     * @param shooter   射击者
     * @param factory   实体工厂函数
     * @param startPos  起始位置
     * @param direction 飞行方向
     * @param speed     速度
     * @param configure 配置回调
     * @return 生成的实体，失败返回 null
     */
    @Nullable
    public Entity spawnCustomProjectile(LivingEntity shooter,
                                        Function<ServerLevel, Entity> factory,
                                        Vec3 startPos, Vec3 direction, double speed,
                                        @Nullable Consumer<Entity> configure) {
        try {
            ServerLevel level = (ServerLevel) shooter.level();
            Entity entity = factory.apply(level);

            if (entity == null) {
                return null;
            }

            entity.setPos(startPos.x, startPos.y, startPos.z);

            Vec3 motion = direction.normalize().scale(speed);
            entity.setDeltaMovement(motion);

            if (configure != null) {
                configure.accept(entity);
            }

            level.addFreshEntity(entity);
            return entity;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



    /**
     * 更新所有活跃投射物（应在 ServerTickEvent.Post 中调用）
     * 【工作流程】
     * 1. 遍历所有投射物
     * 2. 调用每个投射物的 tick() 方法
     * 3. 移除已死亡（命中或超距）的投射物
     */
    public void tick() {
        Iterator<GenericProjectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            GenericProjectile projectile = iterator.next();
            projectile.tick();

            if (projectile.isDead()) {
                iterator.remove();
            }
        }
    }
    /**
     * 单个飞行投射物的实现
     * 【生命周期】
     * 创建 → 每tick更新位置 → 检测碰撞 → 命中或超距 → 销毁
     */
    private static class GenericProjectile {
        private final LivingEntity shooter;        // 射击者（支持任意实体）
        private final ServerLevel level;           // 所在世界
        private Vec3 position;                     // 当前位置
        private Vec3 direction;              // 飞行方向
        private final double damage;               // 伤害值
        private final double maxRange;             // 最大射程
        private final double speed;                // 飞行速度
        private final String particleType;         // 粒子类型
        private final double hitboxSize;           // 碰撞箱大小
        private final String hitSound;             // 命中音效
        private final Consumer<LivingEntity> onHitCallback; // 命中回调
        private double traveledDistance;           // 已飞行距离
        private boolean hasHit;                    // 是否已命中
        private LivingEntity trackingTarget; // 追踪目标实体
        private double turnRate;             // 转向灵敏度（0-1）
        private int targetSearchCooldown;    // 目标搜索冷却计时器
        private int particleCount;
        private double particleOffsetX;
        private double particleOffsetY;
        private double particleOffsetZ;
        private double particleSpeed;

        /**
         * 构造函数
         * @param shooter 射击者（玩家、怪物、NPC等）
         * @param startPos 起始位置
         * @param direction 飞行方向
         * @param damage 伤害值
         * @param maxRange 最大射程
         * @param speed 飞行速度
         * @param particleType 粒子类型
         * @param hitboxSize 碰撞箱大小
         * @param hitSound 命中音效
         * @param onHit 命中回调
         */
        public GenericProjectile(LivingEntity shooter, Vec3 startPos, Vec3 direction,
                                 double damage, double maxRange, double speed,
                                 String particleType, double hitboxSize,
                                 String hitSound, Consumer<LivingEntity> onHit) {
            this.shooter = shooter;
            this.level = (ServerLevel) shooter.level();  // 从实体获取世界
            this.position = startPos;
            this.direction = direction.normalize();  // 归一化方向向量
            this.damage = damage;
            this.maxRange = maxRange;
            this.speed = speed;
            this.particleType = particleType;
            this.hitboxSize = hitboxSize;
            this.hitSound = hitSound;
            this.onHitCallback = onHit;
            this.traveledDistance = 0;
            this.hasHit = false;
            this.trackingTarget = null;      // 新增：初始无追踪目标
            this.turnRate = 0.0;             // 新增：默认不追踪
            this.targetSearchCooldown = 0;   // 新增：初始无冷却
            this.particleCount = 1;
            this.particleOffsetX = 0.1;
            this.particleOffsetY = 0.1;
            this.particleOffsetZ = 0.1;
            this.particleSpeed = 0;
        }

        /**
         * 每 tick 执行一次更新
         * 【执行顺序】
         * 1. 检查是否应停止（已命中或超距）
         * 2. 更新位置
         * 3. 生成粒子效果
         * 4. 检测碰撞
         */
        public void tick() {
            if (hasHit || traveledDistance >= maxRange) {
                return;
            }
            if (trackingTarget != null) {
                if (!trackingTarget.isAlive()) {
                    trackingTarget = null;
                } else {
                    updateTrackingDirection();
                }
            }
            if (trackingTarget == null && targetSearchCooldown <= 0) {
                findNewTarget();
                targetSearchCooldown = 10;
            }

            if (targetSearchCooldown > 0) {
                targetSearchCooldown--;
            }
            position = position.add(direction.scale(speed));
            traveledDistance += speed;
            spawnParticles();
            checkCollision();
        }
        /**
         * 在当前位置生成粒子
         */
        private void spawnParticles() {
            try {
                ParticleOptions particle = getParticle(particleType);
                level.sendParticles(particle, position.x, position.y, position.z,
                        particleCount, particleOffsetX, particleOffsetY, particleOffsetZ, particleSpeed);
            } catch (Exception e) {
                //失败
            }
        }

        private void updateTrackingDirection() {
            Vec3 toTarget = trackingTarget.getPosition(1.0F).subtract(position);
            Vec3 desiredDirection = toTarget.normalize();     // 期望方向：指向目标
            Vec3 currentDirection = direction.normalize();    // 当前方向

            // 线性插值实现平滑转向
            Vec3 newDirection = currentDirection.lerp(desiredDirection, turnRate).normalize();
            this.direction = newDirection;
        }

        private void findNewTarget() {
            double searchRadius = 25.0;
            List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(position.x - searchRadius, position.y - searchRadius, position.z - searchRadius,
                            position.x + searchRadius, position.y + searchRadius, position.z + searchRadius),
                    entity -> entity != shooter && entity.isAlive() && !entity.isAlliedTo(shooter)
            );
            if (!nearby.isEmpty()) {
                double closestDist = Double.MAX_VALUE;
                LivingEntity closest = null;
                for (LivingEntity entity : nearby) {
                    double dist = position.distanceTo(entity.position());
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = entity;
                    }
                }
                if (closest != null) {
                    this.trackingTarget = closest;
                }
            }
        }



        /**
         * 根据字符串获取粒子类型
         * @param type 粒子类型字符串
         * @return 对应的 ParticleOptions
         */
        private ParticleOptions getParticle(String type) {
            return switch (type.toLowerCase()) {
                case "enchanted_hit" -> ParticleTypes.ENCHANTED_HIT;  // 附魔光点
                case "crit" -> ParticleTypes.CRIT;                    // 暴击星星
                case "flame" -> ParticleTypes.FLAME;                  // 火焰
                case "cloud" -> ParticleTypes.CLOUD;                  // 烟雾
                case "heart" -> ParticleTypes.HEART;                  // 爱
                default -> ParticleTypes.ENCHANTED_HIT;               // 默认粒子
            };
        }

        /**
         * 检测碰撞并处理命中逻辑
         * 【检测流程】
         * 1. 创建碰撞箱（AABB）
         * 2. 获取箱内所有生物
         * 3. 排除自己和盟友
         * 4. 根据射击者类型选择伤害来源
         * 5. 对第一个命中的目标造成伤害
         * 6. 执行自定义回调（如果有）
         * 7. 播放命中音效（如果有）
         */
        private void checkCollision() {
            //创建碰撞箱
            AABB hitBox = new AABB(
                    position.x - hitboxSize, position.y - hitboxSize, position.z - hitboxSize,
                    position.x + hitboxSize, position.y + hitboxSize, position.z + hitboxSize
            );

            //获取范围内的生物
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, hitBox);

            //遍历检测
            for (LivingEntity target : targets) {
                // 排除射击者自己和盟友
                if (target != shooter && !target.isAlliedTo(shooter)) {
                    if (target instanceof FaxiangEntity) {
                        continue;
                    }
                    //根据射击者类型选择伤害来源
                    if (shooter instanceof net.minecraft.server.level.ServerPlayer player) {
                        // 玩家发射 → 玩家攻击伤害
                        target.hurt(level.damageSources().playerAttack(player), (float) damage);
                    } else {
                        // 怪物/NPC 发射 → 生物攻击伤害
                        target.hurt(level.damageSources().mobAttack(shooter), (float) damage);
                    }

                    // 执行自定义回调
                    if (onHitCallback != null) {
                        onHitCallback.accept(target);
                    }

                    // 播放音效
                    if (hitSound != null) {
                        playHitSound();
                    }

                    hasHit = true;  // 标记为已命中
                    break;          // 只命中第一个目标
                }
            }
        }

        /**
         * 播放命中音效
         */
        private void playHitSound() {
            try {
                var sound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                        .get(net.minecraft.resources.ResourceLocation.parse(hitSound));
                if (sound != null) {
                    level.playSound(null, position.x, position.y, position.z,
                            sound, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            } catch (Exception e) {
                // 音效播放失败，忽略
            }
        }

        /**
         * 检查投射物是否应销毁
         *
         * @return true 如果已命中目标或飞行距离超过最大射程
         */
        public boolean isDead() {
            return hasHit || traveledDistance >= maxRange;
        }
    }
}