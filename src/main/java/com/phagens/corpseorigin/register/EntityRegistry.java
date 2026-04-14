package com.phagens.corpseorigin.register;

import com.phagens.corpseorigin.GongFU.FaXiang.FaxiangEntity;
import com.phagens.corpseorigin.entity.*;
import com.phagens.corpseorigin.entity.npc.KaiWeiNaiEntity;
import com.phagens.corpseorigin.entity.skills.LongyouEarthquakeEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.phagens.corpseorigin.CorpseOrigin.MODID;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ZbrFishEntity>> ZBR_FISH = ENTITIES.register("zbr_fish",
            () -> EntityType.Builder.<ZbrFishEntity>of(ZbrFishEntity::new, MobCategory.MONSTER)
                    .sized(0.5F, 0.5F)
                    .build("zbr_fish"));

    public static final DeferredHolder<EntityType<?>, EntityType<LowerLevelZbEntity>> LOWER_LEVEL_ZB = ENTITIES.register("lower_level_zb",
            () -> EntityType.Builder.<LowerLevelZbEntity>of(LowerLevelZbEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .build("lower_level_zb"));

    public static final DeferredHolder<EntityType<?>, EntityType<LongyouEntity>> LONGYOU = ENTITIES.register("longyou",
            () -> EntityType.Builder.<LongyouEntity>of(LongyouEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.5F)
                    .build("longyou"));

    // 鬼棍实体
    public static final DeferredHolder<EntityType<?>, EntityType<GuigunEntity>> GUIGUN = ENTITIES.register("guigun",
            () -> EntityType.Builder.<GuigunEntity>of(GuigunEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .build("guigun"));
    //法相测试
    public static final DeferredHolder<EntityType<?>, EntityType<FaxiangEntity>> FAXIANG = ENTITIES.register("faxiang",
            () -> EntityType.Builder.<FaxiangEntity>of(FaxiangEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("faxiang"));

    // 开胃奶NPC
    public static final DeferredHolder<EntityType<?>, EntityType<KaiWeiNaiEntity>> KAIWEINAI = ENTITIES.register("kaiweinai",
            () -> EntityType.Builder.<KaiWeiNaiEntity>of(KaiWeiNaiEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .build("kaiweinai"));
    
    // 地震实体
    public static final DeferredHolder<EntityType<?>, EntityType<LongyouEarthquakeEntity>> LONGYOU_EARTHQUAKE = ENTITIES.register("longyou_earthquake",
            () -> EntityType.Builder.<LongyouEarthquakeEntity>of(LongyouEarthquakeEntity::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F)
                    .build("longyou_earthquake"));

    // 尸体残肢实体 - 完全参照 Mob-Dismemberment 的 EntityGib
    // 合并尸体和残肢为一个实体类型
    public static final DeferredHolder<EntityType<?>, EntityType<CorpseGibEntity>> CORPSE_GIB = ENTITIES.register("corpse_gib",
            () -> EntityType.Builder.<CorpseGibEntity>of(CorpseGibEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F) // 基础尺寸，根据类型动态调整
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("corpse_gib"));

    // 异化孢子实体 - 尸兄死亡时喷射的孢子
    public static final DeferredHolder<EntityType<?>, EntityType<AlienatedSporeEntity>> ALIENATED_SPORE = ENTITIES.register("alienated_spore",
            () -> EntityType.Builder.<AlienatedSporeEntity>of(AlienatedSporeEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("alienated_spore"));
}
