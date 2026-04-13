package com.phagens.corpseorigin.api.infection;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class InfectionAPIExample {

    public static void registerExampleInfections() {
        CorpseOrigin.LOGGER.info("=== 尸兄模组感染API使用示例 ===");

        // 示例1：注册简单的实体感染（使用实体注册系统）
        // 当暮色森林的实体死亡时，可以被感染为尸兄
        // EntityType<?> twilightForestEntity = ...; // 获取暮色森林的实体类型
        // EntityInfectionRegistry.registerInfection(twilightForestEntity, 
        //         EntityRegistry.LOWER_LEVEL_ZB.get(), 1.5f);

        // 示例2：使用高级API注册自定义感染处理器
        // InfectionAPI.registerInfectionHandler(twilightForestEntity, new CustomInfectionHandler());

        // 示例3：设置默认感染处理器（所有未注册的实体）
        // InfectionAPI.setDefaultInfectionHandler(new DefaultInfectionHandler());

        CorpseOrigin.LOGGER.info("=== 示例注册完成 ===");
    }

    /**
     * 自定义感染处理器示例
     */
    public static class CustomInfectionHandler implements IInfectionHandler {

        @Override
        public boolean canBeInfected(LivingEntity entity) {
            // 自定义逻辑：判断实体是否可以被感染
            // 例如：只有特定生物群系中的实体才能被感染
            // String biome = entity.level().getBiome(entity.blockPosition()).unwrapKey().location().toString();
            // return biome.contains("twilight_forest");
            return true;
        }

        @Override
        public LivingEntity createInfectedEntity(Level level, LivingEntity originalEntity) {
            // 自定义逻辑：创建感染后的实体
            // 可以根据原实体的属性创建不同类型的尸兄
            // 例如：大型生物感染后变成高级尸兄
            // if (originalEntity.getMaxHealth() > 50) {
            //     return EntityRegistry.LONGYOU.get().create(level);
            // }
            return EntityRegistry.LOWER_LEVEL_ZB.get().create(level);
        }

        @Override
        public float getInfectionSpeed(LivingEntity entity) {
            // 自定义逻辑：根据实体属性返回感染速度
            // 例如：血量越高的生物感染越慢
            // float healthFactor = Math.min(2.0f, entity.getMaxHealth() / 20.0f);
            // return 1.0f / healthFactor;
            return 1.0f;
        }

        @Override
        public void onInfectionComplete(LivingEntity originalEntity, LivingEntity infectedEntity) {
            // 自定义逻辑：感染完成时的回调
            CorpseOrigin.LOGGER.info("自定义感染完成: {} -> {} (血量: {})",
                    originalEntity.getType(),
                    infectedEntity.getType(),
                    originalEntity.getMaxHealth());

            // 可以在这里添加额外的效果，例如：
            // - 播放特殊音效
            // - 生成粒子效果
            // - 给予感染实体特殊buff
            // - 触发成就
        }

        @Override
        public void onInfectionProgress(LivingEntity entity, float progress) {
            // 自定义逻辑：感染进度更新时的回调
            // 可以在这里添加进度相关的效果
            if (progress > 0.5f && progress < 0.51f) {
                CorpseOrigin.LOGGER.debug("感染进度过半: {}", entity.getType());
            }
        }

        @Override
        public boolean shouldCreateCorpse(LivingEntity entity) {
            // 自定义逻辑：判断死亡时是否应该创建尸体
            // 例如：只有被特定伤害源杀死才创建尸体
            // return entity.getLastDamageSource() != null && 
            //        entity.getLastDamageSource().is(net.minecraft.tags.DamageTypeTags.IS_PHYSICAL);
            return true;
        }
    }

    /**
     * 暮色森林联动示例
     * 展示如何为暮色森林模组的实体注册感染
     */
    public static class TwilightForestIntegration {

        public static void registerTwilightForestInfections() {
            CorpseOrigin.LOGGER.info("开始注册暮色森林实体感染...");

            // 注意：这些是示例代码，实际使用时需要：
            // 1. 确保暮色森林模组已加载
            // 2. 获取正确的实体类型引用
            // 3. 在合适的时机调用（如FMLCommonSetupEvent）

            // 示例：为暮色森林的各类怪物注册感染
            // EntityType<?> naga = getTwilightEntityType("twilightforest:naga");
            // if (naga != null) {
            //     EntityInfectionRegistry.registerInfection(naga, 
            //             EntityRegistry.LOWER_LEVEL_ZB.get(), 0.8f);
            // }

            // EntityType<?> hydra = getTwilightEntityType("twilightforest:hydra");
            // if (hydra != null) {
            //     // 大型怪物感染后变成龙右
            //     EntityInfectionRegistry.registerInfection(hydra, 
            //             EntityRegistry.LONGYOU.get(), 0.5f);
            // }

            // EntityType<?> urGhast = getTwilightEntityType("twilightforest:ur_ghast");
            // if (urGhast != null) {
            //     // 使用自定义处理器
            //     InfectionAPI.registerInfectionHandler(urGhast, new CustomInfectionHandler());
            // }

            CorpseOrigin.LOGGER.info("暮色森林实体感染注册完成");
        }

        private static EntityType<?> getTwilightEntityType(String entityId) {
            // 通过反射或NeoForge注册表获取实体类型
            // 这里只是示例，实际实现需要根据NeoForge的API
            try {
                return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                        .get(net.minecraft.resources.ResourceLocation.parse(entityId));
            } catch (Exception e) {
                CorpseOrigin.LOGGER.warn("无法获取实体类型: {}", entityId);
                return null;
            }
        }
    }
}
