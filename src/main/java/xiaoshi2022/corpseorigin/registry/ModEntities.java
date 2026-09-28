package xiaoshi2022.corpseorigin.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.*;

public final class ModEntities {
    public static final EntityType<ZishuRobotEntity> ZISHU_ROBOT=register("zishu_robot",EntityType.Builder.<ZishuRobotEntity>of(ZishuRobotEntity::new,MobCategory.CREATURE).sized(.75f,1.35f).clientTrackingRange(10));
    public static final EntityType<ZishuIonBallEntity> ZISHU_ION_BALL=register("zishu_ion_ball",EntityType.Builder.<ZishuIonBallEntity>of(ZishuIonBallEntity::new,MobCategory.MISC).sized(.3f,.3f).clientTrackingRange(10).updateInterval(1));
    public static final EntityType<CorpseMaggotEntity> CORPSE_MAGGOT = register("corpse_maggot",
            EntityType.Builder.<CorpseMaggotEntity>of(CorpseMaggotEntity::new, MobCategory.MONSTER)
                    .sized(.35f, .25f).clientTrackingRange(8).updateInterval(2).notInPeaceful());
    public static final EntityType<GourdOrganEntity> ZBR_GOURD=register("zbr_gourd",EntityType.Builder.<GourdOrganEntity>of(GourdOrganEntity::new,MobCategory.MISC).sized(.7f,1.2f).clientTrackingRange(12).updateInterval(1));
    private static EntityType<SkillConstructEntity> construct(String id){
        return register(id,EntityType.Builder.<SkillConstructEntity>of(SkillConstructEntity::new,MobCategory.MISC)
                .sized(.5f,.5f).clientTrackingRange(12).updateInterval(1));
    }
    public static final EntityType<SkillConstructEntity> BLACK_GOLD_HEART=construct("black_gold_heart");
    public static final EntityType<SkillConstructEntity> VINE_BIND=construct("vine_bind");
    public static final EntityType<SkillConstructEntity> BLOOD_LOTUS_PETAL=construct("blood_lotus_petal");
    public static final EntityType<SkillConstructEntity> BEE_WHEEL=register("bee_wheel",
            EntityType.Builder.<SkillConstructEntity>of(BeeWheelEntity::new,MobCategory.MISC)
                    .sized(.5f,.5f).clientTrackingRange(12).updateInterval(1));
    public static final EntityType<SkillConstructEntity> SLAUGHTER_INCARNATION=construct("slaughter_incarnation");
    public static final EntityType<SkillConstructEntity> SEVERED_FOREARM=construct("severed_forearm");
    public static final EntityType<SkillConstructEntity> TIANGANG_HALO=construct("tiangang_halo");
    public static final EntityType<CorpseAntEntity> RED_FIRE_ANT=register("red_fire_ant",EntityType.Builder.<CorpseAntEntity>of(CorpseAntEntity::new,MobCategory.MONSTER).sized(.7f,.65f).clientTrackingRange(8).notInPeaceful());
    public static final EntityType<CorpseAntEntity> BULLET_ANT=register("bullet_ant",EntityType.Builder.<CorpseAntEntity>of(CorpseAntEntity::new,MobCategory.MONSTER).sized(1.1f,.85f).clientTrackingRange(8).notInPeaceful());
    public static final EntityType<GreatTenguEntity> GREAT_TENGU = register("great_tengu",EntityType.Builder.<GreatTenguEntity>of(GreatTenguEntity::new,MobCategory.MISC)
            .sized(6,2).clientTrackingRange(12).updateInterval(3));
    public static final EntityType<xiaoshi2022.corpseorigin.entity.ChapterBombEntity> CHAPTER_BOMB = register("chapter_bomb",
            EntityType.Builder.<xiaoshi2022.corpseorigin.entity.ChapterBombEntity>of(xiaoshi2022.corpseorigin.entity.ChapterBombEntity::new,MobCategory.MISC)
                    .sized(.3f,.3f).clientTrackingRange(8).updateInterval(1));
    public static final EntityType<xiaoshi2022.corpseorigin.entity.VampireBatEntity> VAMPIRE_BAT = register("vampire_bat",
            EntityType.Builder.<xiaoshi2022.corpseorigin.entity.VampireBatEntity>of(xiaoshi2022.corpseorigin.entity.VampireBatEntity::new, MobCategory.MISC)
                    .sized(.5f,.9f).clientTrackingRange(8).updateInterval(2));
    public static final EntityType<CorpseFishEggEntity> CORPSE_FISH_EGG = register("corpse_fish_egg",
            EntityType.Builder.<CorpseFishEggEntity>of(CorpseFishEggEntity::new, MobCategory.MISC)
                    .sized(.24f,.24f).clientTrackingRange(8).updateInterval(1));

    public static final EntityType<HamEntity> HAM = register("ham",
            EntityType.Builder.<HamEntity>of(HamEntity::new, MobCategory.CREATURE)
                    .sized(0.65f, 1.1f).clientTrackingRange(8));

    public static final EntityType<LowerLevelZbEntity> LOWER_LEVEL_ZB = register(
            "lower_level_zb",
            EntityType.Builder.<LowerLevelZbEntity>of(LowerLevelZbEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .notInPeaceful()
    );

    /**
     * 凹凸曼尸兄 —— 低阶尸兄的"原皮"版本：贴图直接用 {@code textures/entity/aotuman.png}
     * （不叠尸化骨骼层，所以脸上没有尸眼），虚弱且不主动攻击。见 {@link AotumanZbEntity}。
     */
    public static final EntityType<AotumanZbEntity> AOTUMAN_ZB = register(
            "aotuman_zb",
            EntityType.Builder.<AotumanZbEntity>of(AotumanZbEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .notInPeaceful()
    );

    // ✅ 新增剑气实体
    public static final EntityType<BloodWingBeamEntity> BLOOD_WING_BEAM = register(
            "blood_wing_beam", EntityType.Builder.<BloodWingBeamEntity>of(BloodWingBeamEntity::new, MobCategory.MISC)
                    .sized(.5f, .5f).clientTrackingRange(32).updateInterval(1));

    public static final EntityType<JuQueBeamEntity> JUQUE_BEAM = register(
            "juque_beam",
            EntityType.Builder.<JuQueBeamEntity>of(JuQueBeamEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(32)
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
                    .notInPeaceful()
    );

    /** CoCo 尸兄二阶段 - 与大叔的合体形态 */
    public static final EntityType<CocoZombieXEntity> COCO_ZOMBIE_X = register(
            "coco_zombie_x",
            EntityType.Builder.<CocoZombieXEntity>of(CocoZombieXEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 1.4f)
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .notInPeaceful()
    );

    /** 尸兄虫 - 大叔体内钻出的寄生虫，也是 CoCo 企鹅的食物 */
    public static final EntityType<ZbWormEntity> ZB_WORM = register(
            "zb_worm",
            EntityType.Builder.<ZbWormEntity>of(ZbWormEntity::new, MobCategory.MONSTER)
                    .sized(0.2f, 0.2f)
                    .notInPeaceful()
    );

    /** 大叔（少女漫画家）- NPC，与 CoCo 尸兄合体线的起点 */
    public static final EntityType<UncleEntity> UNCLE = register(
            "uncle",
            EntityType.Builder.<UncleEntity>of(UncleEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
    );

    /** 初音尸兄 - 双马尾女性尸兄，投掷大葱、吸食血肉、腹部会膨胀 */
    public static final EntityType<MikuZbEntity> MIKU_ZB = register(
            "miku_zb",
            EntityType.Builder.<MikuZbEntity>of(MikuZbEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(8)
                    .notInPeaceful()
    );

    /** 投掷大葱 - 初音尸兄的远程投射物（原作：车顶投掷大葱击杀怪物控） */
    public static final EntityType<LeekProjectileEntity> LEEK_PROJECTILE = register(
            "leek_projectile",
            EntityType.Builder.<LeekProjectileEntity>of(LeekProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(2)
    );
    public static final EntityType<OsmiumIceSpearEntity> OSMIUM_ICE_SPEAR = register("osmium_ice_spear",
            EntityType.Builder.<OsmiumIceSpearEntity>of(OsmiumIceSpearEntity::new, MobCategory.MISC).sized(.35f, 1.2f).clientTrackingRange(8).updateInterval(1));

    /**
     * 左护法「蛟龙」的一节碰撞箱（隐形实体，伤害转给主人玩家）。
     * <p>
     * {@code noSummon}：不给刷怪蛋 / 指令召唤；{@code noSave}：不写进存档（主人没了就销毁）；
     * {@code updateInterval(1)}：每 tick 同步位置，客户端打架子才跟得上动画。
     */
    public static final EntityType<GuardianPartEntity> GUARDIAN_PART = register(
            "guardian_part",
            EntityType.Builder.<GuardianPartEntity>of(GuardianPartEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
    );

    /**
     * 尸蛟龙（{@code zuo_flood_long}）—— 左护法用「脱离」蜕下来的那条蛟龙，独立的宠物 BOSS。
     * <p>
     * {@code noSummon}：不给刷怪蛋（只能由技能放出来）。
     * <b>不加 {@code noSave}</b>：蛟龙是持久宠物，主人退出游戏 / 关服后要随区块存档，
     * 主人 UUID 存于实体 NBT（见 {@link ZuoFloodLongEntity}），再上线自动认主；
     * 只有它被打死、或主人用「合体」技能收回时才会消失。尺寸给的是"未缩放"的基准，实际大小按配置缩放（见实体里的 getDimensions）。
     */
    public static final EntityType<ZuoFloodLongEntity> ZUO_FLOOD_LONG = register(
            "zuo_flood_long",
            EntityType.Builder.<ZuoFloodLongEntity>of(ZuoFloodLongEntity::new, MobCategory.MISC)
                    .sized(3.0F, 5.0F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
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
