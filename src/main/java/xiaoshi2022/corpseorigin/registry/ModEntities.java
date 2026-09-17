package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.entity.CocoPenguinEntity;
import xiaoshi2022.corpseorigin.entity.CocoZombieEntity;
import xiaoshi2022.corpseorigin.entity.CocoZombieXEntity;
import xiaoshi2022.corpseorigin.entity.FlyingGreatSwordEntity;
import xiaoshi2022.corpseorigin.entity.JuQueBeamEntity;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.UncleEntity;
import xiaoshi2022.corpseorigin.entity.ZbWormEntity;

public final class ModEntities {

    public static final EntityType<LowerLevelZbEntity> LOWER_LEVEL_ZB = register(
            "lower_level_zb",
            EntityType.Builder.<LowerLevelZbEntity>of(LowerLevelZbEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
    );

    // ✅ 新增剑气实体
    public static final EntityType<JuQueBeamEntity> JUQUE_BEAM = register(
            "juque_beam",
            EntityType.Builder.<JuQueBeamEntity>of(JuQueBeamEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(1)
    );

    public static final EntityType<FlyingGreatSwordEntity> FLYING_GREAT_SWORD = register(
            "flying_great_sword",
            EntityType.Builder.<FlyingGreatSwordEntity>of(FlyingGreatSwordEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
    );

    public static final EntityType<CloneAvatarEntity> CLONE_AVATAR = register(
            "clone_avatar",
            EntityType.Builder.<CloneAvatarEntity>of(CloneAvatarEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
    );

    /** CoCo 企鹅 - 未尸兄化的王企鹅 */
    public static final EntityType<CocoPenguinEntity> COCO_PENGUIN = register(
            "coco_penguin",
            EntityType.Builder.<CocoPenguinEntity>of(CocoPenguinEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.2f)
    );

    /** CoCo 尸兄 - 一阶段，躺下姿态 */
    public static final EntityType<CocoZombieEntity> COCO_ZOMBIE = register(
            "coco_zombie",
            EntityType.Builder.<CocoZombieEntity>of(CocoZombieEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 0.6f)
    );

    /** CoCo 尸兄二阶段 - 与大叔的合体形态 */
    public static final EntityType<CocoZombieXEntity> COCO_ZOMBIE_X = register(
            "coco_zombie_x",
            EntityType.Builder.<CocoZombieXEntity>of(CocoZombieXEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 1.4f)
                    .clientTrackingRange(64)
                    .updateInterval(2)
    );

    /** 尸兄虫 - 大叔体内钻出的寄生虫，也是 CoCo 企鹅的食物 */
    public static final EntityType<ZbWormEntity> ZB_WORM = register(
            "zb_worm",
            EntityType.Builder.<ZbWormEntity>of(ZbWormEntity::new, MobCategory.MONSTER)
                    .sized(0.2f, 0.2f)
    );

    /** 大叔（少女漫画家）- NPC，与 CoCo 尸兄合体线的起点 */
    public static final EntityType<UncleEntity> UNCLE = register(
            "uncle",
            EntityType.Builder.<UncleEntity>of(UncleEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
    );

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = CorpseOrigin.id(name);
        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                id,
                builder.build(ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), id))
        );
    }

    public static void init() {
    }
}