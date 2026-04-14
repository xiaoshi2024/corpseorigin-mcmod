package com.phagens.corpseorigin.api.infection;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public interface IInfectionHandler {

    boolean canBeInfected(LivingEntity entity);

    LivingEntity createInfectedEntity(Level level, LivingEntity originalEntity);

    float getInfectionSpeed(LivingEntity entity);

    void onInfectionComplete(LivingEntity originalEntity, LivingEntity infectedEntity);

    default void onInfectionProgress(LivingEntity entity, float progress) {
    }

    default boolean shouldCreateCorpse(LivingEntity entity) {
        return canBeInfected(entity);
    }
}
