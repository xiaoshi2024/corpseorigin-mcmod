package com.phagens.corpseorigin.api.infection;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Map;

public class EntityInfectionRegistry {

    private static final Map<EntityType<?>, InfectionConfig> INFECTION_MAP = new java.util.HashMap<>();

    private EntityInfectionRegistry() {
    }

    public static void registerInfection(EntityType<?> originalType, EntityType<?> infectedType) {
        registerInfection(originalType, infectedType, 1.0f);
    }

    public static void registerInfection(EntityType<?> originalType, EntityType<?> infectedType, float infectionSpeed) {
        if (originalType == null || infectedType == null) {
            CorpseOrigin.LOGGER.warn("尝试注册空的实体类型");
            return;
        }
        
        InfectionConfig config = new InfectionConfig(infectedType, infectionSpeed);
        INFECTION_MAP.put(originalType, config);
        
        CorpseOrigin.LOGGER.info("已注册实体感染: {} -> {} (速度: {})", 
                originalType, infectedType, infectionSpeed);
    }

    public static void registerInfection(EntityType<?> originalType, EntityType<?> infectedType, 
                                        float infectionSpeed, boolean createCorpse) {
        if (originalType == null || infectedType == null) {
            CorpseOrigin.LOGGER.warn("尝试注册空的实体类型");
            return;
        }
        
        InfectionConfig config = new InfectionConfig(infectedType, infectionSpeed, createCorpse);
        INFECTION_MAP.put(originalType, config);
        
        CorpseOrigin.LOGGER.info("已注册实体感染: {} -> {} (速度: {}, 创建尸体: {})", 
                originalType, infectedType, infectionSpeed, createCorpse);
    }

    public static boolean canBeInfected(EntityType<?> entityType) {
        return INFECTION_MAP.containsKey(entityType);
    }

    public static boolean shouldCreateCorpse(EntityType<?> entityType) {
        InfectionConfig config = INFECTION_MAP.get(entityType);
        return config != null && config.createCorpse;
    }

    public static EntityType<?> getInfectedType(EntityType<?> originalType) {
        InfectionConfig config = INFECTION_MAP.get(originalType);
        return config != null ? config.infectedType : null;
    }

    public static float getInfectionSpeed(EntityType<?> entityType) {
        InfectionConfig config = INFECTION_MAP.get(entityType);
        return config != null ? config.infectionSpeed : 1.0f;
    }

    public static LivingEntity createInfectedEntity(Level level, LivingEntity originalEntity) {
        if (originalEntity == null) return null;
        
        EntityType<?> infectedType = getInfectedType(originalEntity.getType());
        if (infectedType == null) return null;
        
        try {
            LivingEntity infectedEntity = (LivingEntity) infectedType.create(level);
            if (infectedEntity != null) {
                infectedEntity.moveTo(originalEntity.getX(), originalEntity.getY(), originalEntity.getZ(), 
                        originalEntity.getYRot(), originalEntity.getXRot());
                return infectedEntity;
            }
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("创建感染实体失败: {} -> {}", 
                    originalEntity.getType(), infectedType, e);
        }
        
        return null;
    }

    public static void removeInfection(EntityType<?> entityType) {
        INFECTION_MAP.remove(entityType);
        CorpseOrigin.LOGGER.info("已移除实体感染: {}", entityType);
    }

    public static void clearAll() {
        INFECTION_MAP.clear();
        CorpseOrigin.LOGGER.info("已清空所有实体感染注册");
    }

    public static int getRegisteredCount() {
        return INFECTION_MAP.size();
    }

    private static class InfectionConfig {
        final EntityType<?> infectedType;
        final float infectionSpeed;
        final boolean createCorpse;

        InfectionConfig(EntityType<?> infectedType, float infectionSpeed) {
            this(infectedType, infectionSpeed, true);
        }

        InfectionConfig(EntityType<?> infectedType, float infectionSpeed, boolean createCorpse) {
            this.infectedType = infectedType;
            this.infectionSpeed = infectionSpeed;
            this.createCorpse = createCorpse;
        }
    }
}
