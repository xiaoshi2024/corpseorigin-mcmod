package com.phagens.corpseorigin.GongFU.JSskill.Factory;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.FaXiang.FaxiangEntity;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FaxiangFactory {
    @Nullable
    public static FaxiangEntity spawnFaxiang(LivingEntity shooter, Vec3 position,
                                             String modelPath, String texturePath,
                                             String animationPath, int lifespanTicks,
                                             double scale) {
        return spawnFaxiang(shooter, position, modelPath, texturePath, animationPath,
                lifespanTicks, scale, 50.0D, 10.0D, 5.0D, 1.5D, 32.0D, 8.0D, 20,
                ParticleTypes.ENCHANT, null);
    }

    @Nullable
    public static FaxiangEntity spawnFaxiang(LivingEntity shooter, Vec3 position,
                                             String modelPath, String texturePath,
                                             String animationPath, int lifespanTicks,
                                             double scale, double maxHealth,
                                             double attackDamage, double armor,
                                             double attackSpeed, double followRange) {
        return spawnFaxiang(shooter, position, modelPath, texturePath, animationPath,
                lifespanTicks, scale, maxHealth, attackDamage, armor, attackSpeed, followRange,
                8.0D, 20, ParticleTypes.ENCHANT, null);
    }

    @Nullable
    public static FaxiangEntity spawnFaxiang(LivingEntity shooter, Vec3 position,
                                             String modelPath, String texturePath,
                                             String animationPath, int lifespanTicks,
                                             double scale, double maxHealth,
                                             double attackDamage, double armor,
                                             double attackSpeed, double followRange,
                                             double auraRadius, int auraDamageInterval) {
        return spawnFaxiang(shooter, position, modelPath, texturePath, animationPath,
                lifespanTicks, scale, maxHealth, attackDamage, armor, attackSpeed, followRange,
                auraRadius, auraDamageInterval, ParticleTypes.ENCHANT, null);
    }

    /**
     * 召唤法相实体（完整版，支持粒子和回调）
     *
     * @param shooter 召唤者（玩家或其他生物实体）
     * @param position 法相生成位置（世界坐标）
     * @param modelPath 模型文件路径（GeoJSON格式，如 "corpseorigin:geo/entity/guigun.geo.json"）
     * @param texturePath 纹理文件路径（PNG格式，如 "corpseorigin:textures/entity/guigun.png"）
     * @param animationPath 动画文件路径（JSON格式，如 "corpseorigin:animations/entity/guigun.animation.json"）
     * @param lifespanTicks 生命周期（单位：tick，20tick=1秒，600tick=30秒）
     * @param scale 缩放比例（1.0为原始大小，3.0为3倍大小）
     * @param maxHealth 最大生命值（>0时生效，同时设置为当前生命值）
     * @param attackDamage 攻击力（用于近战攻击和光环伤害，>0时生效）
     * @param armor 护甲值（>=0时生效，影响受到的伤害减免）
     * @param attackSpeed 攻击速度（>0时生效，影响近战攻击频率）
     * @param followRange 索敌范围（>0时生效，单位：格，决定法相能发现多远的敌人）
     * @param auraRadius 光环半径（>0时生效，单位：格，光环对范围内敌人造成伤害）
     * @param auraDamageInterval 光环伤害间隔（>0时生效，单位：tick，20tick=1秒造成一次伤害）
     * @param auraParticleType 光环粒子特效类型（如 ParticleTypes.ENCHANT, FLAME, SOUL_FIRE_FLAME 等）
     * @param auraHitCallback 光环命中回调函数（可选，每次光环伤害命中敌人时调用）
     * @return 成功返回法相实体，失败返回null
     */
    @Nullable
    public static FaxiangEntity spawnFaxiang(LivingEntity shooter, Vec3 position,
                                             String modelPath, String texturePath,
                                             String animationPath, int lifespanTicks,
                                             double scale, double maxHealth,
                                             double attackDamage, double armor,
                                             double attackSpeed, double followRange,
                                             double auraRadius, int auraDamageInterval,
                                             ParticleOptions auraParticleType,
                                             Consumer<LivingEntity> auraHitCallback) {
        try {
            ServerLevel level = (ServerLevel) shooter.level();
            FaxiangEntity faxiang = new FaxiangEntity(EntityRegistry.FAXIANG.get(), level);

            faxiang.setPos(position.x, position.y, position.z);
            faxiang.setLifespan(lifespanTicks > 0 ? lifespanTicks : 600);
            faxiang.setScale(scale);
            faxiang.setOwner(shooter);
            faxiang.setResources(
                    ResourceLocation.parse(modelPath),
                    ResourceLocation.parse(texturePath),
                    ResourceLocation.parse(animationPath)
            );

            if (maxHealth > 0) {
                faxiang.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(maxHealth);
                faxiang.setHealth((float) maxHealth);
            }
            if (attackDamage > 0) {
                faxiang.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(attackDamage);
            }
            if (armor >= 0) {
                faxiang.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(armor);
            }
            if (attackSpeed > 0) {
                faxiang.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED).setBaseValue(attackSpeed);
            }
            if (followRange > 0) {
                faxiang.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(followRange);
            }

            faxiang.setAuraRadius(auraRadius > 0 ? auraRadius : 8.0D);
            faxiang.setAuraDamageInterval(auraDamageInterval > 0 ? auraDamageInterval : 20);
            faxiang.setAuraParticleType(auraParticleType != null ? auraParticleType : ParticleTypes.ENCHANT);
            faxiang.setAuraHitCallback(auraHitCallback);

            level.addFreshEntity(faxiang);

            CorpseOrigin.LOGGER.info("【法相召唤】已生成法相实体 召唤者：{} 位置：{} 生命：{} 攻击：{} 护甲：{} 光环半径：{} 光环间隔：{}tick 粒子：{}",
                    shooter.getName().getString(), position, maxHealth, attackDamage, armor, auraRadius, auraDamageInterval, auraParticleType);
            return faxiang;

        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("【法相召唤】生成失败：", e);
            return null;
        }
    }
}

