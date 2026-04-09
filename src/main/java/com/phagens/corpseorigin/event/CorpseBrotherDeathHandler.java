//package com.phagens.corpseorigin.event;
//
//import com.phagens.corpseorigin.entity.AlienatedSporeEntity;
//import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
//import com.phagens.corpseorigin.entity.LongyouEntity;
//import com.phagens.corpseorigin.entity.ZbrFishEntity;
//import com.phagens.corpseorigin.entity.GuigunEntity;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.phys.Vec3;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
//
//import java.lang.reflect.Method;
//
//@EventBusSubscriber
//public class CorpseBrotherDeathHandler {
//
//    @SubscribeEvent
//    public static void onLivingDeath(LivingDeathEvent event) {
//        LivingEntity entity = event.getEntity();
//
//        if (entity.level().isClientSide) {
//            return;
//        }
//
//        int corpseHungerValue = getCorpseHungerValue(entity);
//
//        if (corpseHungerValue > 0) {
//            if (entity.level().random.nextFloat() < 0.5F) {
//                ServerLevel serverLevel = (ServerLevel) entity.level();
//                Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
//
//                int sporeCount = 8 + entity.level().random.nextInt(8);
//                AlienatedSporeEntity.spawnSporeBurst(serverLevel, center, sporeCount);
//            }
//        }
//    }
//
//    private static int getCorpseHungerValue(LivingEntity entity) {
//        if (entity instanceof LowerLevelZbEntity lowerLevelZb) {
//            return lowerLevelZb.getCorpseHunger();
//        } else if (entity instanceof ZbrFishEntity zbrFish) {
//            return zbrFish.getCorpseHunger();
//        } else if (entity instanceof LongyouEntity longyou) {
//            try {
//                Method getHungerMethod = LongyouEntity.class.getDeclaredMethod("getHunger");
//                getHungerMethod.setAccessible(true);
//                return (int) getHungerMethod.invoke(longyou) > 0 ? 1 : 0;
//            } catch (Exception e) {
//                try {
//                    java.lang.reflect.Field hungerField = LongyouEntity.class.getDeclaredField("hunger");
//                    hungerField.setAccessible(true);
//                    int hunger = (int) hungerField.get(longyou);
//                    return hunger > 0 ? 1 : 0;
//                } catch (Exception ex) {
//                    return 0;
//                }
//            }
//        }
//        return 0;
//    }
//}
