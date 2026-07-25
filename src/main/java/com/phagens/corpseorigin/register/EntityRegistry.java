package com.phagens.corpseorigin.register;

import com.phagens.corpseorigin.GongFU.Domain.DomainEntity;
import com.phagens.corpseorigin.GongFU.FaXiang.FaxiangEntity;
import com.phagens.corpseorigin.client.skin.ZbSkinCache;
import com.phagens.corpseorigin.entity.*;
import com.phagens.corpseorigin.entity.Animals.CocoPenguinEntity;
import com.phagens.corpseorigin.entity.Animals.CocoZombieEntity;
import com.phagens.corpseorigin.entity.Animals.ZbWormEntity;
import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.CentipedeHead;
import com.phagens.corpseorigin.entity.SegmentedEntity.Centipede.CentipedeJoint;
import com.phagens.corpseorigin.entity.npc.*;

import com.phagens.corpseorigin.entity.skills.LongyouEarthquakeEntity;
import com.phagens.corpseorigin.entity.zbrs.CocoZombieXEntity;
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

    public static final DeferredHolder<EntityType<?>, EntityType<DomainEntity>> DOMAIN = ENTITIES.register("domain",
            () -> EntityType.Builder.<DomainEntity>of(DomainEntity::new, MobCategory.CREATURE)
                    .sized(0.01F, 0.01F) // 极小尺寸（隐形）
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("domain"));

    // 葫芦娃人类cos
    public static final DeferredHolder<EntityType<?>, EntityType<CalabashBoyCosEntity>> CALABASH_BOY_COS = ENTITIES.register("calabash_boy_cos",
            () -> EntityType.Builder.<CalabashBoyCosEntity>of(CalabashBoyCosEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .build("calabash_boy_cos"));

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

    // CoCo企鹅实体 - 王企鹅，未尸兄化
    public static final DeferredHolder<EntityType<?>, EntityType<CocoPenguinEntity>> COCO_PENGUIN = ENTITIES.register("coco_penguin",
            () -> EntityType.Builder.<CocoPenguinEntity>of(CocoPenguinEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.2F)
                    .build("coco_penguin"));

    // CoCo尸兄化实体 - 尸兄企鹅（躺下姿态）
    public static final DeferredHolder<EntityType<?>, EntityType<CocoZombieEntity>> COCO_ZOMBIE = ENTITIES.register("coco_zombie",
            () -> EntityType.Builder.<CocoZombieEntity>of(CocoZombieEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 0.6F)
                    .build("coco_zombie"));

    // 尸兄虫子实体
    public static final DeferredHolder<EntityType<?>, EntityType<ZbWormEntity>> ZB_WORM = ENTITIES.register("zb_worm",
            () -> EntityType.Builder.<ZbWormEntity>of(ZbWormEntity::new, MobCategory.MONSTER)
                    .sized(0.2F, 0.2F)
                    .build("zb_worm"));

    // 大叔NPC（少女漫画家）
    public static final DeferredHolder<EntityType<?>, EntityType<UncleEntity>> UNCLE = ENTITIES.register("uncle",
            () -> EntityType.Builder.<UncleEntity>of(UncleEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .build("uncle"));

    // 合体尸兄 - CoCo企鹅 + 大叔合体（二阶段）
    public static final DeferredHolder<EntityType<?>, EntityType<CocoZombieXEntity>> COCO_ZOMBIE_X = ENTITIES.register("coco_zombie_x",
            () -> EntityType.Builder.<CocoZombieXEntity>of(CocoZombieXEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 1.4F)          // 合体后体型比一阶段更大
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("coco_zombie_x"));

    // 蜈蚣头部实体 - 根据模型重新调整碰撞箱
    public static final DeferredHolder<EntityType<?>, EntityType<CentipedeHead>> CENTIPEDE_HEAD = ENTITIES.register("centipede_head",
            () -> EntityType.Builder.<CentipedeHead>of(CentipedeHead::new, MobCategory.MONSTER)
                    .sized(2.8F, 2.8F)  // 头部宽度约2.8格，高度约3.2格
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("centipede_head"));

    // 蜈蚣躯干节段实体 - 根据模型重新调整碰撞箱
    public static final DeferredHolder<EntityType<?>, EntityType<CentipedeJoint>> CENTIPEDE_JOINT = ENTITIES.register("centipede_joint",
            () -> EntityType.Builder.<CentipedeJoint>of(CentipedeJoint::new, MobCategory.MONSTER)
                    .sized(1.5F, 1.2F)  // 躯干宽度约1.5格，高度约1.2格
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("centipede_joint"));

    // 丑牛实体
    public static final DeferredHolder<EntityType<?>, EntityType<ChouniuEntity>> CHOUNIU = ENTITIES.register("chouniu",
            () -> EntityType.Builder.<ChouniuEntity>of(ChouniuEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.0F)
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("chouniu"));

    // 子鼠实体
    public static final DeferredHolder<EntityType<?>, EntityType<ZishuEntity>> ZISHU = ENTITIES.register("zishu",
            () -> EntityType.Builder.<ZishuEntity>of(ZishuEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.4F)
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("zishu"));

    // 卯兔实体
    public static final DeferredHolder<EntityType<?>, EntityType<MaotuEntity>> MAOTU = ENTITIES.register("maotu",
            () -> EntityType.Builder.<MaotuEntity>of(MaotuEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.6F)
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("maotu"));


    // 追踪投掷物导弹
    public static final DeferredHolder<EntityType<?>, EntityType<MaotuProjectileEntity>> MAOTU_PROJECTILE = ENTITIES.register("maotu_projectile",
            () -> EntityType.Builder.<MaotuProjectileEntity>of(MaotuProjectileEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("maotu_projectile"));

    // 巨阙剑气实体
    public static final DeferredHolder<EntityType<?>, EntityType<JuQueBeamEntity>> JUQUE_BEAM = ENTITIES.register("juque_beam",
            () -> EntityType.Builder.<JuQueBeamEntity>of(JuQueBeamEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("juque_beam"));

}
