package com.phagens.corpseorigin.api.infection;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class InfectionAPI {

    private static final Map<EntityType<?>, IInfectionHandler> HANDLER_MAP = new HashMap<>();
    private static IInfectionHandler DEFAULT_HANDLER = null;

    private InfectionAPI() {
    }

    public static void registerInfectionHandler(EntityType<?> entityType, IInfectionHandler handler) {
        if (entityType == null || handler == null) {
            CorpseOrigin.LOGGER.warn("尝试注册空的感染处理器");
            return;
        }
        HANDLER_MAP.put(entityType, handler);
        CorpseOrigin.LOGGER.info("已注册感染处理器: {}", entityType);
    }

    public static void registerInfectionHandler(String modId, IInfectionHandler handler) {
        if (modId == null || handler == null) {
            CorpseOrigin.LOGGER.warn("尝试注册空的感染处理器");
            return;
        }
        CorpseOrigin.LOGGER.info("已注册模组感染处理器: {}", modId);
    }

    public static void setDefaultInfectionHandler(IInfectionHandler handler) {
        DEFAULT_HANDLER = handler;
        CorpseOrigin.LOGGER.info("已设置默认感染处理器");
    }

    public static IInfectionHandler getInfectionHandler(LivingEntity entity) {
        if (entity == null) return null;
        
        IInfectionHandler handler = HANDLER_MAP.get(entity.getType());
        if (handler != null) {
            return handler;
        }
        
        return DEFAULT_HANDLER;
    }

    public static boolean canBeInfected(LivingEntity entity) {
        IInfectionHandler handler = getInfectionHandler(entity);
        if (handler == null) return false;
        return handler.canBeInfected(entity);
    }

    public static boolean shouldCreateCorpse(LivingEntity entity) {
        IInfectionHandler handler = getInfectionHandler(entity);
        if (handler == null) return false;
        return handler.shouldCreateCorpse(entity);
    }

    public static LivingEntity createInfectedEntity(Level level, LivingEntity originalEntity) {
        IInfectionHandler handler = getInfectionHandler(originalEntity);
        if (handler == null) return null;
        return handler.createInfectedEntity(level, originalEntity);
    }

    public static float getInfectionSpeed(LivingEntity entity) {
        IInfectionHandler handler = getInfectionHandler(entity);
        if (handler == null) return 1.0f;
        return handler.getInfectionSpeed(entity);
    }

    public static void onInfectionComplete(LivingEntity originalEntity, LivingEntity infectedEntity) {
        IInfectionHandler handler = getInfectionHandler(originalEntity);
        if (handler == null) return;
        handler.onInfectionComplete(originalEntity, infectedEntity);
    }

    public static void onInfectionProgress(LivingEntity entity, float progress) {
        IInfectionHandler handler = getInfectionHandler(entity);
        if (handler == null) return;
        handler.onInfectionProgress(entity, progress);
    }

    public static void clearHandlers() {
        HANDLER_MAP.clear();
        DEFAULT_HANDLER = null;
    }

    public static boolean hasHandler(EntityType<?> entityType) {
        return HANDLER_MAP.containsKey(entityType);
    }

    public static boolean hasDefaultHandler() {
        return DEFAULT_HANDLER != null;
    }
}
