package com.phagens.corpseorigin.api.watercompany;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public interface IInfectionTrigger {

    void triggerInfection(LivingEntity target, ServerLevel level);

    void triggerInfection(LivingEntity target, ServerLevel level, int durationTicks);

    void triggerInfection(LivingEntity target, ServerLevel level, UUID sourceUUID);

    void triggerInfection(LivingEntity target, ServerLevel level, int durationTicks, UUID sourceUUID);

    boolean canInfect(LivingEntity target);

    boolean isInfected(LivingEntity target);

    void removeInfection(LivingEntity target);
}