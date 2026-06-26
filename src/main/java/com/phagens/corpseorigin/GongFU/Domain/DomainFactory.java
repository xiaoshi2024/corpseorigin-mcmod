package com.phagens.corpseorigin.GongFU.Domain;


import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.Domain.DomainEntity;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class DomainFactory {

    /**
     * 召唤领域实体（简化版 - 默认粒子配置）
     */
    @Nullable
    public static DomainEntity spawnDomain(LivingEntity shooter, Vec3 position,
                                           int lifespanTicks, double attackDamage,
                                           double domainRadius, int domainDamageInterval,
                                           ParticleOptions domainParticleType,
                                           Consumer<LivingEntity> domainHitCallback) {
        return spawnDomain(shooter, position, lifespanTicks, attackDamage,
                domainRadius, domainDamageInterval, domainParticleType,
                30, 0.0, 0.05, 0.0, 0.0, true, domainHitCallback);
    }

    /**
     * 召唤领域实体（完整版 - 自定义粒子配置）
     *
     * @param shooter 召唤者
     * @param position 领域生成位置
     * @param lifespanTicks 生命周期（tick）
     * @param attackDamage 领域伤害
     * @param domainRadius 领域半径
     * @param domainDamageInterval 伤害间隔（tick）
     * @param domainParticleType 领域粒子类型
     * @param particleCountPerTick 每tick粒子数量
     * @param particleSpreadX X轴扩散
     * @param particleSpreadY Y轴扩散
     * @param particleSpreadZ Z轴扩散
     * @param particleSpeed 粒子速度
     * @param useCircularPattern 是否使用环形排列（false=随机散布）
     * @param domainHitCallback 命中回调
     * @return 成功返回领域实体，失败返回null
     */
    @Nullable
    public static DomainEntity spawnDomain(LivingEntity shooter, Vec3 position,
                                           int lifespanTicks, double attackDamage,
                                           double domainRadius, int domainDamageInterval,
                                           ParticleOptions domainParticleType,
                                           int particleCountPerTick,
                                           double particleSpreadX, double particleSpreadY, double particleSpreadZ,
                                           double particleSpeed,
                                           boolean useCircularPattern,
                                           Consumer<LivingEntity> domainHitCallback) {
        try {
            ServerLevel level = (ServerLevel) shooter.level();
            DomainEntity domain = new DomainEntity(EntityRegistry.DOMAIN.get(), level);

            domain.setPos(position.x, position.y, position.z);
            domain.setLifespan(lifespanTicks > 0 ? lifespanTicks : 600);
            domain.setOwner(shooter);

            if (attackDamage > 0) {
                domain.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                        .setBaseValue(attackDamage);
            }

            domain.setDomainRadius(domainRadius > 0 ? domainRadius : 8.0D);
            domain.setDomainDamageInterval(domainDamageInterval > 0 ? domainDamageInterval : 20);
            domain.setDomainParticleType(domainParticleType != null ? domainParticleType : ParticleTypes.FLAME);
            domain.setDomainHitCallback(domainHitCallback);

            domain.setParticleCountPerTick(particleCountPerTick > 0 ? particleCountPerTick : 30);
            domain.setParticleSpread(particleSpreadX, particleSpreadY, particleSpreadZ);
            domain.setParticleSpeed(particleSpeed);
            domain.setUseCircularPattern(useCircularPattern);

            level.addFreshEntity(domain);

            CorpseOrigin.LOGGER.info("【领域召唤】已生成领域实体 召唤者：{} 伤害：{} 半径：{} 粒子数：{} 模式：{}",
                    shooter.getName().getString(), attackDamage, domainRadius,
                    particleCountPerTick, useCircularPattern ? "环形" : "随机");
            return domain;

        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("【领域召唤】生成失败：", e);
            return null;
        }
    }
}