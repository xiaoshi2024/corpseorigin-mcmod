package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.entity.AlienatedSporeEntity;
import com.phagens.corpseorigin.entity.ICorpseHunger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber
public class CorpseBrotherDeathHandler {
    
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        if (entity.level().isClientSide) {
            return;
        }
        
        int corpseHungerValue = getCorpseHungerValue(entity);
        
        if (corpseHungerValue > 0) {
            if (entity.level().random.nextFloat() < 0.5F) {
                ServerLevel serverLevel = (ServerLevel) entity.level();
                Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
                
                int sporeCount = 8 + entity.level().random.nextInt(8);
                AlienatedSporeEntity.spawnSporeBurst(serverLevel, center, sporeCount);
            }
        }
    }
    
    private static int getCorpseHungerValue(LivingEntity entity) {
        if (entity instanceof ICorpseHunger corpseHunger) {
            return corpseHunger.getCorpseHunger();
        }
        return 0;
    }
}
