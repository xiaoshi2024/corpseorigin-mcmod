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
import xiaoshi2022.corpseorigin.entity.FlyingGreatSwordEntity;
import xiaoshi2022.corpseorigin.entity.JuQueBeamEntity;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

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