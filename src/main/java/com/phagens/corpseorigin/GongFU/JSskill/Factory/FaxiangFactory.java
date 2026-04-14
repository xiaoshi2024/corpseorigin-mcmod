package com.phagens.corpseorigin.GongFU.JSskill.Factory;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.FaXiang.FaxiangEntity;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class FaxiangFactory {
    @Nullable
    public static FaxiangEntity spawnFaxiang(LivingEntity shooter, Vec3 position,
                                             String modelPath, String texturePath,
                                             String animationPath, int lifespanTicks,
                                             double scale) {
        return spawnFaxiang(shooter, position, modelPath, texturePath, animationPath,
                lifespanTicks, scale, 50.0D, 10.0D, 5.0D, 1.5D, 32.0D);
    }

    @Nullable
    public static FaxiangEntity spawnFaxiang(LivingEntity shooter, Vec3 position,
                                             String modelPath, String texturePath,
                                             String animationPath, int lifespanTicks,
                                             double scale, double maxHealth,
                                             double attackDamage, double armor,
                                             double attackSpeed, double followRange) {
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

            level.addFreshEntity(faxiang);

            CorpseOrigin.LOGGER.info("【法相召唤】已生成法相实体 召唤者：{} 位置：{} 生命：{} 攻击：{} 护甲：{}",
                    shooter.getName().getString(), position, maxHealth, attackDamage, armor);
            return faxiang;

        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("【法相召唤】生成失败：", e);
            return null;
        }
    }
}
