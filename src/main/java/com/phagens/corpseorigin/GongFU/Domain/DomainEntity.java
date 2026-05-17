package com.phagens.corpseorigin.GongFU.Domain;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 领域实体 - 用于JS技能召唤的持续光环效果实体
 * 【功能说明】
 * 1. 完全隐形（无模型、无渲染）
 * 2. 每tick瞬移跟随召唤者位置
 * 3. 无碰撞箱，无法被攻击
 * 4. 光环效果对范围内敌人造成周期性伤害
 * 5. 支持自定义粒子特效和命中回调
 * 6. 生命周期结束后自动消失
 */
public class DomainEntity extends LivingEntity {

    // 生命周期管理
    private int lifespan = 600;
    private int age = 0;

    // 召唤者信息
    private UUID ownerUUID = null;
    private LivingEntity cachedOwner = null;

    // 领域配置
    private double domainRadius = 8.0D;
    private int domainDamageInterval = 20;
    private int domainTimer = 0;

    // ⭐ 粒子配置（支持自定义）
    private ParticleOptions domainParticleType = ParticleTypes.FLAME;
    private int particleCountPerTick = 30;
    private double particleSpreadX = 0.0D;
    private double particleSpreadY = 0.05D;
    private double particleSpreadZ = 0.0D;
    private double particleSpeed = 0.0D;
    private boolean useCircularPattern = true; // 是否使用环形排列

    private Consumer<LivingEntity> domainHitCallback = null;

    public DomainEntity(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.noCulling = true;
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 999999.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 10.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 0.0D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            age++;

            if (age >= lifespan) {
                this.discard();
                return;
            }

            updateOwner();

            LivingEntity owner = getOwner();
            if (owner == null || !owner.isAlive()) {
                this.discard();
                return;
            }

            this.setPos(owner.getX(), owner.getY() + 1.0, owner.getZ());
            updateDomainEffect();
        } else {
            renderDomainParticles();
        }
    }

    /**
     * 渲染领域粒子特效（客户端）- 支持多种模式
     */
    private void renderDomainParticles() {
        if (domainParticleType == null) {
            return;
        }

        double radius = domainRadius;

        if (useCircularPattern) {
            // ⭐ 环形排列模式
            for (int i = 0; i < particleCountPerTick; i++) {
                double angle = 2 * Math.PI * i / particleCountPerTick + (age % 100) * 0.05;
                double xOffset = Math.cos(angle) * radius;
                double zOffset = Math.sin(angle) * radius;

                double particleX = this.getX() + xOffset;
                double particleY = this.getY() + this.random.nextDouble() * 2.5;
                double particleZ = this.getZ() + zOffset;

                // ✅ 7参数版本：(particle, x, y, z, xOffset, yOffset, zOffset)
                this.level().addParticle(domainParticleType,
                        particleX, particleY, particleZ,
                        particleSpreadX,      // xOffset: X轴扩散
                        particleSpreadY,      // yOffset: Y轴扩散
                        particleSpreadZ       // zOffset: Z轴扩散
                );
            }
        } else {
            // ⭐ 随机散布模式
            for (int i = 0; i < particleCountPerTick; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double distance = this.random.nextDouble() * radius;

                double xOffset = Math.cos(angle) * distance;
                double zOffset = Math.sin(angle) * distance;

                double particleX = this.getX() + xOffset;
                double particleY = this.getY() + this.random.nextDouble() * 3.0;
                double particleZ = this.getZ() + zOffset;

                this.level().addParticle(domainParticleType,
                        particleX, particleY, particleZ,
                        particleSpreadX,
                        particleSpreadY,
                        particleSpreadZ
                );
            }
        }
    }


    /**
     * 更新领域效果
     */
    private void updateDomainEffect() {
        domainTimer++;
        if (domainTimer < domainDamageInterval) {
            return;
        }
        domainTimer = 0;

        float attackDamage = (float) this.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attackDamage <= 0) {
            return;
        }

        AABB domainBox = this.getBoundingBox().inflate(domainRadius);
        List<LivingEntity> entitiesInRange = this.level().getEntitiesOfClass(
                LivingEntity.class,
                domainBox,
                entity -> isValidTarget(entity) && this.distanceToSqr(entity) <= domainRadius * domainRadius
        );

        for (LivingEntity entity : entitiesInRange) {
            DamageSource damageSource = this.damageSources().mobAttack(this);
            entity.hurt(damageSource, attackDamage);

            if (domainHitCallback != null) {
                domainHitCallback.accept(entity);
            }
        }
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        LivingEntity owner = getOwner();
        if (owner != null) {
            if (target == owner) {
                return false;
            }

            if (target instanceof net.minecraft.world.entity.player.Player player &&
                    owner instanceof net.minecraft.world.entity.player.Player ownerPlayer) {
                if (player.getUUID().equals(ownerPlayer.getUUID())) {
                    return false;
                }

                if (player.getTeam() != null && ownerPlayer.getTeam() != null) {
                    if (player.getTeam().isAlliedTo(ownerPlayer.getTeam())) {
                        return false;
                    }
                }
            }
        }

        if (target instanceof DomainEntity) {
            return false;
        }

        return true;
    }

    private void updateOwner() {
        if (ownerUUID != null && (cachedOwner == null || !cachedOwner.isAlive())) {
            if (level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                cachedOwner = (LivingEntity) serverLevel.getEntity(ownerUUID);
            }
        }
    }

    public LivingEntity getOwner() {
        return cachedOwner;
    }

    public void setOwner(LivingEntity owner) {
        this.cachedOwner = owner;
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
    }

    public void setLifespan(int ticks) {
        this.lifespan = ticks;
    }

    public void setDomainRadius(double radius) {
        this.domainRadius = radius;
    }

    public void setDomainDamageInterval(int interval) {
        this.domainDamageInterval = interval;
    }

    public void setDomainParticleType(ParticleOptions particleType) {
        this.domainParticleType = particleType;
    }

    public void setDomainHitCallback(Consumer<LivingEntity> callback) {
        this.domainHitCallback = callback;
    }

    // ⭐ 新增：自定义粒子参数的setter方法

    public void setParticleCountPerTick(int count) {
        this.particleCountPerTick = count;
    }

    public void setParticleSpread(double x, double y, double z) {
        this.particleSpreadX = x;
        this.particleSpreadY = y;
        this.particleSpreadZ = z;
    }

    public void setParticleSpeed(double speed) {
        this.particleSpeed = speed;
    }

    public void setUseCircularPattern(boolean useCircular) {
        this.useCircularPattern = useCircular;
    }

    @Override
    public Iterable<net.minecraft.world.item.ItemStack> getArmorSlots() {
        return java.util.Collections.emptyList();
    }

    @Override
    public net.minecraft.world.item.ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot equipmentSlot) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(net.minecraft.world.entity.EquipmentSlot equipmentSlot, net.minecraft.world.item.ItemStack itemStack) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public HumanoidArm getMainArm() {
        return net.minecraft.world.entity.HumanoidArm.RIGHT;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean shouldRender(double p_148843_, double p_148844_, double p_148845_) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double p_148843_) {
        return false;
    }
}
