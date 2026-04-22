package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.mca.McaZombieEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class McaInfectionHandler {

    @SubscribeEvent
    public static void onMcaVillagerDeath(LivingDeathEvent event) {
        if (!McaZombieEntity.isMcaAvailable()) {
            return;
        }

        Entity entity = event.getEntity();
        DamageSource source = event.getSource();

        try {
            Class<?> villagerEntityMcaClass = Class.forName("net.conczin.mca.entity.VillagerEntityMCA");
            
            if (villagerEntityMcaClass.isInstance(entity)) {
                if (shouldInfect(source)) {
                    event.setCanceled(true);
                    
                    McaZombieEntity.createFromMcaVillager(entity).ifPresent(mcaZombie -> {
                        entity.level().addFreshEntity(mcaZombie);
                        entity.discard();
                        
                        CorpseOrigin.LOGGER.info("MCA 村民已感染为尸兄: {}", entity.getName().getString());
                    });
                }
            }
        } catch (ClassNotFoundException e) {
            CorpseOrigin.LOGGER.debug("MCA 模组未找到，跳过感染逻辑");
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("处理 MCA 感染时出错", e);
        }
    }

    private static boolean shouldInfect(DamageSource source) {
        if (source == null) {
            return false;
        }

        Entity sourceEntity = source.getEntity();
        
        if (sourceEntity == null) {
            return false;
        }

        try {
            Class<?> lowerLevelZbEntityClass = Class.forName("com.phagens.corpseorigin.entity.LowerLevelZbEntity");
            Class<?> mcaZombieEntityClass = Class.forName("com.phagens.corpseorigin.entity.mca.McaZombieEntity");
            
            if (lowerLevelZbEntityClass.isInstance(sourceEntity) || mcaZombieEntityClass.isInstance(sourceEntity)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            CorpseOrigin.LOGGER.debug("尸兄实体类未找到");
        }

        return false;
    }
}
