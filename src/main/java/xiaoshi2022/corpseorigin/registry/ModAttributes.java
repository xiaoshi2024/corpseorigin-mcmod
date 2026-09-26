package xiaoshi2022.corpseorigin.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.PathfinderMob;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.*;

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
        FabricDefaultAttributeRegistry.register(ModEntities.ZBR_GOURD, xiaoshi2022.corpseorigin.entity.GourdOrganEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.HAM, HamEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.RED_FIRE_ANT, xiaoshi2022.corpseorigin.entity.CorpseAntEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.BULLET_ANT, xiaoshi2022.corpseorigin.entity.CorpseAntEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.VAMPIRE_BAT, xiaoshi2022.corpseorigin.entity.VampireBatEntity.createVampireAttributes());

        FabricDefaultAttributeRegistry.register(ModEntities.BLACK_GOLD_HEART, SkillConstructEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.VINE_BIND, SkillConstructEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.BLOOD_LOTUS_PETAL, SkillConstructEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.BEE_WHEEL, SkillConstructEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.SLAUGHTER_INCARNATION, SkillConstructEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.SEVERED_FOREARM, SkillConstructEntity.createMobAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.TIANGANG_HALO, SkillConstructEntity.createMobAttributes());

        // 注册低级尸兄的属性
        FabricDefaultAttributeRegistry.register(
                ModEntities.LOWER_LEVEL_ZB,
                LowerLevelZbEntity.createAttributes()
        );

        // 凹凸曼尸兄（低级尸兄的虚弱原皮版本）
        FabricDefaultAttributeRegistry.register(
                ModEntities.AOTUMAN_ZB,
                AotumanZbEntity.createAttributes()
        );

        FabricDefaultAttributeRegistry.register(ModEntities.CLONE_AVATAR, CloneAvatarEntity.createAttributes());

        // CoCo 企鹅 / 一阶段尸兄 / 二阶段合体尸兄
        FabricDefaultAttributeRegistry.register(ModEntities.COCO_PENGUIN, CocoPenguinEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.COCO_ZOMBIE, CocoZombieEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.COCO_ZOMBIE_X, CocoZombieXEntity.createAttributes());

        // 大叔 NPC / 尸兄虫
        FabricDefaultAttributeRegistry.register(ModEntities.UNCLE, UncleEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.ZB_WORM, ZbWormEntity.createAttributes());

        // 初音尸兄
        FabricDefaultAttributeRegistry.register(ModEntities.MIKU_ZB, MikuZbEntity.createAttributes());

        // 尸蛟龙（左护法"脱离"后放出来的宠物 BOSS）
        FabricDefaultAttributeRegistry.register(
                ModEntities.ZUO_FLOOD_LONG,
                ZuoFloodLongEntity.createAttributes()
        );

        // TODO: 添加其他实体的属性注册
        // 例如：
        // FabricDefaultAttributeRegistry.register(ModEntities.LONGYOU, LongyouEntity.createAttributes());
        // FabricDefaultAttributeRegistry.register(ModEntities.ZBR_FISH, ZbrFishEntity.createAttributes());

        CorpseOrigin.LOGGER.info("CorpseOrigin attributes registered");
    }
}
