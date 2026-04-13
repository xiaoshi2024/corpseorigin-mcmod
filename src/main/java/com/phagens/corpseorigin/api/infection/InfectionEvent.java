package com.phagens.corpseorigin.api.infection;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

public class InfectionEvent extends Event {

    private final Level level;
    private final LivingEntity originalEntity;
    private final LivingEntity infectedEntity;
    private final float infectionProgress;

    public InfectionEvent(Level level, LivingEntity originalEntity, LivingEntity infectedEntity, float infectionProgress) {
        this.level = level;
        this.originalEntity = originalEntity;
        this.infectedEntity = infectedEntity;
        this.infectionProgress = infectionProgress;
    }

    public Level getLevel() {
        return level;
    }

    public LivingEntity getOriginalEntity() {
        return originalEntity;
    }

    public LivingEntity getInfectedEntity() {
        return infectedEntity;
    }

    public float getInfectionProgress() {
        return infectionProgress;
    }

    public static class InfectionStartEvent extends InfectionEvent {
        public InfectionStartEvent(Level level, LivingEntity originalEntity, LivingEntity infectedEntity) {
            super(level, originalEntity, infectedEntity, 0.0f);
        }
    }

    public static class InfectionProgressEvent extends InfectionEvent {
        public InfectionProgressEvent(Level level, LivingEntity originalEntity, LivingEntity infectedEntity, float progress) {
            super(level, originalEntity, infectedEntity, progress);
        }
    }

    public static class InfectionCompleteEvent extends InfectionEvent {
        public InfectionCompleteEvent(Level level, LivingEntity originalEntity, LivingEntity infectedEntity) {
            super(level, originalEntity, infectedEntity, 1.0f);
        }
    }

    public static class CorpseCreateEvent extends Event {
        private final LivingEntity originalEntity;
        private final Entity corpseEntity;

        public CorpseCreateEvent(LivingEntity originalEntity, Entity corpseEntity) {
            this.originalEntity = originalEntity;
            this.corpseEntity = corpseEntity;
        }

        public LivingEntity getOriginalEntity() {
            return originalEntity;
        }

        public Entity getCorpseEntity() {
            return corpseEntity;
        }
    }

    public static class InfectionCheckEvent extends Event {
        private final LivingEntity entity;
        private boolean canBeInfected;

        public InfectionCheckEvent(LivingEntity entity) {
            this.entity = entity;
            this.canBeInfected = false;
        }

        public LivingEntity getEntity() {
            return entity;
        }

        public boolean canBeInfected() {
            return canBeInfected;
        }

        public void setCanBeInfected(boolean canBeInfected) {
            this.canBeInfected = canBeInfected;
        }
    }
}
