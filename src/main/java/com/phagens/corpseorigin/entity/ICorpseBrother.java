package com.phagens.corpseorigin.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public interface ICorpseBrother extends ICorpseHunger {

    boolean isCorpseBrotherOf(Mob entity);

    void setHiveMindTarget(LivingEntity target);

    LivingEntity getHiveMindTarget();

    boolean hasAttackTarget();

    int getEvolutionLevel();
}
