package xiaoshi2022.corpseorigin.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

/**
 * 实体属性注册 - Fabric 26.2
 *
 * 在 Fabric 中，实体属性通过 FabricDefaultAttributeRegistry 在模组初始化时注册
 * 替代 NeoForge 的 @SubscribeEvent + EntityAttributeCreationEvent
 */
public final class ModAttributes {

    private ModAttributes() {
    }

    /**
     * 注册所有实体的属性
     * 在模组初始化时调用
     */
    public static void register() {
        // 注册低级尸兄的属性
        FabricDefaultAttributeRegistry.register(
                ModEntities.LOWER_LEVEL_ZB,
                LowerLevelZbEntity.createAttributes()
        );

        // TODO: 添加其他实体的属性注册
        // 例如：
        // FabricDefaultAttributeRegistry.register(ModEntities.LONGYOU, LongyouEntity.createAttributes());
        // FabricDefaultAttributeRegistry.register(ModEntities.ZBR_FISH, ZbrFishEntity.createAttributes());

        CorpseOrigin.LOGGER.info("CorpseOrigin attributes registered");
    }
}