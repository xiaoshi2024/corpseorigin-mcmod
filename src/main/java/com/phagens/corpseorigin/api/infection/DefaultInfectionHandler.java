package com.phagens.corpseorigin.api.infection;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class DefaultInfectionHandler implements IInfectionHandler {

    @Override
    public boolean canBeInfected(LivingEntity entity) {
        String entityType = entity.getType().toString().toLowerCase();
        return entityType.contains("zombie") || entityType.contains("villager");
    }

    @Override
    public LivingEntity createInfectedEntity(Level level, LivingEntity originalEntity) {
        return null;
    }

    @Override
    public float getInfectionSpeed(LivingEntity entity) {
        return 1.0f;
    }

    @Override
    public void onInfectionComplete(LivingEntity originalEntity, LivingEntity infectedEntity) {
        CorpseOrigin.LOGGER.info("默认感染完成: {} -> {}", 
                originalEntity.getType(), infectedEntity.getType());
    }
}
