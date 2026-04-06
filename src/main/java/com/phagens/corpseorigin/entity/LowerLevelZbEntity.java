package com.phagens.corpseorigin.entity;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.EntityAI.Vibrationsys.ModVibrationUser;
import com.phagens.corpseorigin.entity.EntityAI.JLAI.PounceAttackGoal;
import com.phagens.corpseorigin.client.skin.ZbSkinLoader;
import com.phagens.corpseorigin.client.skin.ZbSkinState;
import com.phagens.corpseorigin.entity.skills.InnateSkillManager;
import com.phagens.corpseorigin.network.ZbSkinUpdatePacket;
import com.phagens.corpseorigin.register.Moditems;
import com.phagens.corpseorigin.register.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 尸兄实体类
 * 实现了Vampirism模组的IBiteableEntity接口（通过IEntity扩展）
 * 允许吸血鬼玩家吸食尸兄的血液
 */
public class LowerLevelZbEntity extends PathfinderMob implements GeoEntity, VibrationSystem {
    // 变种类型枚举
    public enum Variant {
        NORMAL(0),
        CRACKED(1), // 裂口尸兄
        WINGS(2), // 翅膀尸兄
        WINGS_CRACKED(3); // 裂口翅膀尸兄

        private final int code;

        Variant(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static Variant fromCode(int code) {
            for (Variant variant : values()) {
                if (variant.code == code) {
                    return variant;
                }
            }
            return NORMAL;
        }
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    protected static final RawAnimation SHIEYE_ANIM = RawAnimation.begin().thenPlay("shieye");
    protected static final RawAnimation SPECIAL_ANIM = RawAnimation.begin().thenPlay("special");
    protected static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop("fly");

    // 同步数据
    private static final EntityDataAccessor<String> DATA_PLAYER_NAME =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_CUSTOM_ID =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SKIN_STATE =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_SHIEYE =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_SPECIAL = 
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SPECIAL_TICKS = 
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COMMAND_STATE = 
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_WING = 
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_TAIL = 
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_FLYING = 
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID playerSkinId;
    private String playerSkinName;

    // 皮肤相关（仅客户端使用）- 直接在声明时初始化
    @OnlyIn(Dist.CLIENT)
    private ResourceLocation skinTexture;

    // 皮肤加载状态标记 - 需要在两端都存在
    private boolean skinLoadStarted = false;
    
    // 动画状态跟踪
    private int shieyeCooldown = 0;
    private int shieyeAnimationTicks = 0;
    
    // 进化相关
    private int evolutionLevel = 1;
    private int kills = 0;
    
    // 饥饿度系统 (0-100, 100为饱腹, 0为极度饥饿)
    private int hunger = 100;
    private static final int HUNGER_THRESHOLD_FOR_CANNIBALISM = 20; // 饥饿度低于20才允许吞噬
    
    // 尸体饱腹值系统 (0-3, 吞噬尸体获得)
    private int corpseHunger = 0;
    private static final int MAX_CORPSE_HUNGER = 3;
    
    // 飞扑攻击系统
    private int pounceCooldown = 0;
    private static final int POUNCE_COOLDOWN_TICKS = 100; // 5秒冷却
    private boolean isPouncing = false;
    
    // 酸液喷射系统
    private int acidSprayCooldown = 0;
    private static final int ACID_SPRAY_COOLDOWN_TICKS = 60; // 3秒冷却
    
    // 神志系统
    private boolean hasSentient = false; // 是否保留生前神志
    private int speechCooldown = 0; // 说话冷却时间
    
    // 贪婪系统
    private boolean isGreedy = false; // 是否贪婪
    private int greedCooldown = 0; // 贪婪行为冷却时间
    
    // 器官系统
    private boolean hasWing = false; // 是否有翅膀
    private boolean hasTail = false; // 是否有鱼尾
    
    // 被攻击记忆系统（用于反击）
    private int lastHurtTick = -1000; // 上次被攻击的游戏刻
    private static final int HURT_MEMORY_DURATION = 200; // 被攻击记忆持续时间（10秒）

    // 主人系统（被尸王收服后）
    private UUID masterUUID = null; // 主人的UUID
    private static final double FOLLOW_RANGE = 16.0D; // 跟随范围
    private static final double TELEPORT_RANGE = 32.0D; // 传送范围
    private int followCooldown = 0; // 跟随冷却

    // 尸兄命令状态
    public enum CommandState {
        FOLLOW(0),      // 跟随主人（默认）
        ATTACK(1),      // 主动攻击
        DEFEND(2),      // 防御模式（不主动攻击）
        STAY(3);        // 停留原地

        private final int code;

        CommandState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static CommandState fromCode(int code) {
            for (CommandState state : values()) {
                if (state.code == code) {
                    return state;
                }
            }
            return FOLLOW;
        }
    }

    private CommandState currentCommand = CommandState.FOLLOW; // 当前命令状态

    // 定身系统：记录被此尸兄定身的实体及其定身结束时间
    private final Map<UUID, Long> immobilizedEntities = new HashMap<>();

    // 振动系统
    private final DynamicGameEventListener<Listener> dynamicGameEventListener;
    private final VibrationSystem.User vibrationUser;
    private VibrationSystem.Data vibrationData;

    public LowerLevelZbEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.vibrationUser = new ModVibrationUser(this);
        this.vibrationData = new VibrationSystem.Data();
        this.dynamicGameEventListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));

        // 初始化神志和贪婪系统
        this.hasSentient = this.random.nextFloat() < 0.3; // 30%概率保留生前神志
        this.isGreedy = this.random.nextFloat() < 0.5; // 50%概率贪婪

        // 客户端字段已经在声明时初始化，不需要在这里处理
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        // 如果有翅膀，使用飞行导航
        if (this.entityData.get(DATA_HAS_WING)) {
            FlyingPathNavigation flyingNavigation = new FlyingPathNavigation(this, level);
            flyingNavigation.setCanOpenDoors(true);
            flyingNavigation.setCanFloat(true);
            return flyingNavigation;
        } else {
            net.minecraft.world.entity.ai.navigation.GroundPathNavigation navigation = new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level);
            navigation.setCanOpenDoors(true);
            return navigation;
        }
    }

    /**
     * 更新导航类型（当翅膀状态改变时调用）
     */
    public void updateNavigation() {
        this.navigation = createNavigation(this.level());
    }

    public LowerLevelZbEntity(EntityType<? extends PathfinderMob> entityType, Level level, Player player) {
        this(entityType, level);
        if (player != null) {
            this.playerSkinId = player.getUUID();
            this.playerSkinName = player.getName().getString();
            this.entityData.set(DATA_PLAYER_NAME, this.playerSkinName);
            CorpseOrigin.LOGGER.info("尸兄感染玩家: {}, 将使用其皮肤", this.playerSkinName);
        }
    }

    /**
     * 设置离线玩家的皮肤名称（用于指令召唤）
     */
    public void setPlayerSkinName(String playerName) {
        this.playerSkinName = playerName;
        this.entityData.set(DATA_PLAYER_NAME, playerName);
        CorpseOrigin.LOGGER.info("尸兄设置皮肤玩家: {}", playerName);

        if (!this.level().isClientSide) {
            updateCustomName();
        }
    }

    /**
     * 设置自定义 ID（用于指令召唤）
     */
    // 在设置自定义ID时调用
    public void setCustomId(String customId) {
        this.entityData.set(DATA_CUSTOM_ID, customId);
        CorpseOrigin.LOGGER.info("尸兄设置自定义ID: {}", customId);

        if (!this.level().isClientSide) {
            updateCustomName();
        }
    }

    /**
     * 获取自定义 ID
     */
    public String getCustomId() {
        return this.entityData.get(DATA_CUSTOM_ID);
    }

    /**
     * 获取变种类型
     */
    public Variant getVariant() {
        return Variant.fromCode(this.entityData.get(DATA_VARIANT));
    }

    /**
     * 设置变种类型
     */
    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.getCode());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLAYER_NAME, "");
        builder.define(DATA_CUSTOM_ID, "");
        builder.define(DATA_SKIN_STATE, ZbSkinState.NOT_LOADED.getCode());
        builder.define(DATA_PLAYING_SHIEYE, false);
        builder.define(DATA_VARIANT, Variant.NORMAL.getCode());
        builder.define(DATA_PLAYING_SPECIAL, false);
        builder.define(DATA_SPECIAL_TICKS, 0);
        builder.define(DATA_COMMAND_STATE, CommandState.FOLLOW.getCode());
        builder.define(DATA_HAS_WING, false);
        builder.define(DATA_HAS_TAIL, false);
        builder.define(DATA_IS_FLYING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // 飞扑攻击 - 高优先级
        this.goalSelector.addGoal(2, new PounceAttackGoal(this));
        // 寻找并吞噬尸体 - 尸兄被尸体吸引
        this.goalSelector.addGoal(3, new com.phagens.corpseorigin.entity.EntityAI.JLAI.SeekCorpseGibGoal(this));
        // 近战攻击行为 - 当找到目标时会执行攻击
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        // 高阶尸兄可以开门
        this.goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // 第一优先级：攻击非尸兄玩家（正常活人）
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, this::shouldAttackNormalPlayer));

        // 第二优先级：攻击非尸兄的其他怪物（Mob类，排除尸兄和龙右）
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 0, true, false, this::shouldAttackNonCorpseMob));

        // 第三优先级：攻击动物
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Animal.class, true));

        // 第四优先级：攻击尸兄玩家（同类）- 只有在极度饥饿时才会攻击
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, this::shouldAttackCorpsePlayer));

        // 第五优先级：攻击其他尸兄实体（同类相食）- 只有在极度饥饿时才会攻击
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LowerLevelZbEntity.class, 0, true, false, this::shouldAttackOtherCorpseEntity));
    }

    /**
     * 判断是否应该攻击非尸兄玩家（正常活人）
     * 这是第一优先级目标
     */
    private boolean shouldAttackNormalPlayer(net.minecraft.world.entity.LivingEntity entity) {
        // 不攻击龙右（真王）
        if (entity instanceof LongyouEntity) {
            return false;
        }

        // 只攻击玩家
        if (!(entity instanceof Player player)) {
            return false;
        }

        // 如果有主人，不攻击主人
        if (this.masterUUID != null && player.getUUID().equals(this.masterUUID)) {
            return false;
        }

        // 只攻击非尸兄玩家（正常活人）
        return !com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player);
    }

    /**
     * 判断是否应该攻击非尸兄的其他怪物（Mob类）
     * 这是第二优先级目标
     * 排除：龙右、尸兄实体、玩家
     */
    private boolean shouldAttackNonCorpseMob(net.minecraft.world.entity.LivingEntity entity) {
        // 不攻击龙右（真王）
        if (entity instanceof LongyouEntity) {
            return false;
        }

        // 不攻击尸兄实体（同类）
        if (entity instanceof LowerLevelZbEntity) {
            return false;
        }

        // 不攻击玩家（玩家由单独的目标处理）
        if (entity instanceof Player) {
            return false;
        }

        // 攻击其他所有怪物
        return true;
    }

    /**
     * 判断是否应该攻击尸兄玩家（已成为尸兄的玩家）
     * 这是第四优先级目标，只有在极度饥饿时才会攻击
     */
    private boolean shouldAttackCorpsePlayer(net.minecraft.world.entity.LivingEntity entity) {
        // 不攻击龙右（真王）
        if (entity instanceof LongyouEntity) {
            return false;
        }

        // 只攻击玩家
        if (!(entity instanceof Player player)) {
            return false;
        }

        // 如果有主人，不攻击主人
        if (this.masterUUID != null && player.getUUID().equals(this.masterUUID)) {
            return false;
        }

        // 只攻击已成为尸兄的玩家
        if (!com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
            return false;
        }

        // 被攻击时允许反击
        boolean wasRecentlyHurt = (this.tickCount - lastHurtTick) < HURT_MEMORY_DURATION;
        if (wasRecentlyHurt) {
            return true;
        }

        // 极度饥饿且没有尸王领导时才攻击同类
        boolean isHungry = this.hunger <= HUNGER_THRESHOLD_FOR_CANNIBALISM;
        boolean notUnderKing = !isUnderZombieKingLeadership();

        return isHungry && notUnderKing;
    }

    /**
     * 判断是否应该攻击其他尸兄实体（同类相食）
     * 这是第五优先级目标，只有在极度饥饿时才会攻击
     */
    private boolean shouldAttackOtherCorpseEntity(net.minecraft.world.entity.LivingEntity entity) {
        // 只攻击尸兄实体
        if (!(entity instanceof LowerLevelZbEntity otherZb)) {
            return false;
        }

        // 被攻击时允许反击
        boolean wasRecentlyHurt = (this.tickCount - lastHurtTick) < HURT_MEMORY_DURATION;
        if (wasRecentlyHurt) {
            return true;
        }

        // 检查是否有非同类目标存在
        if (hasNonZombieTargets()) {
            return false; // 有非同类目标时不攻击同类
        }

        // 同类目标，只有在极度饥饿时才攻击
        boolean isHungry = this.hunger <= HUNGER_THRESHOLD_FOR_CANNIBALISM;
        boolean notUnderKing = !isUnderZombieKingLeadership();

        return isHungry && notUnderKing;
    }
    
    /**
     * 检查周围是否存在非同类目标（非尸兄的目标）
     * 非尸兄玩家、非尸兄怪物、动物都是优先攻击目标
     */
    private boolean hasNonZombieTargets() {
        if (!(this.level() instanceof ServerLevel level)) return false;

        // 检查周围16格内是否有非尸兄玩家（第一优先级目标）
        var normalPlayers = level.getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(16.0D),
                entity -> shouldAttackNormalPlayer(entity)
        );
        if (!normalPlayers.isEmpty()) {
            return true;
        }

        // 检查周围16格内是否有非尸兄的其他怪物（第二优先级目标）
        var nonCorpseMobs = level.getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class,
                this.getBoundingBox().inflate(16.0D),
                entity -> shouldAttackNonCorpseMob(entity)
        );
        if (!nonCorpseMobs.isEmpty()) {
            return true;
        }

        // 检查周围16格内是否有可攻击的动物（第三优先级目标）
        var animals = level.getEntitiesOfClass(
                net.minecraft.world.entity.animal.Animal.class,
                this.getBoundingBox().inflate(16.0D),
                entity -> entity.isAlive()
        );
        if (!animals.isEmpty()) {
            return true;
        }

        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    // 添加服务端设置皮肤的方法（由网络包调用）
    public void setSkinTextureFromServer(ResourceLocation texture) {
        // 服务端只存储但不使用
    }

    public void setSkinStateFromServer(ZbSkinState state) {
        this.entityData.set(DATA_SKIN_STATE, state.getCode());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::controlAnimation));
    }

    private <E extends LowerLevelZbEntity> software.bernie.geckolib.animation.PlayState controlAnimation(AnimationState<E> event) {
        // 如果正在播放 special 动画，继续播放
        if (this.entityData.get(DATA_PLAYING_SPECIAL)) {
            return event.setAndContinue(SPECIAL_ANIM);
        }

        // 如果正在播放 shieye 动画，继续播放
        if (this.entityData.get(DATA_PLAYING_SHIEYE)) {
            return event.setAndContinue(SHIEYE_ANIM);
        }

        // 如果正在攻击，播放攻击动画
        if (this.getAttackAnim(event.getPartialTick()) > 0) {
            // 攻击时有30%几率触发 shieye 动画
            if (!this.level().isClientSide && shieyeCooldown <= 0 && this.random.nextFloat() < 0.3F) {
                triggerShieyeAnimation();
                return event.setAndContinue(SHIEYE_ANIM);
            }
            return event.setAndContinue(ATTACK_ANIM);
        }

        // 如果正在飞行，播放飞行动画
        if (this.entityData.get(DATA_IS_FLYING)) {
            return event.setAndContinue(FLY_ANIM);
        }

        // 移动时播放行走动画
        if (event.isMoving()) {
            return event.setAndContinue(WALK_ANIM);
        }

        // 默认播放待机动画
        return event.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void aiStep() {
        super.aiStep();
    }
    
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // 飞行时免疫摔落伤害
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) && this.entityData.get(DATA_IS_FLYING)) {
            return false;
        }

        // 有翅膀的尸兄在落地前如果还在飞行状态，免疫摔落伤害
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) && this.entityData.get(DATA_HAS_WING)) {
            // 检查是否刚从飞行状态落下（给一点缓冲时间）
            if (this.fallDistance < 5.0F) {
                return false;
            }
        }

        // 检查攻击者是否是主人，如果是则不记录（不反击）
        if (source.getEntity() instanceof Player player) {
            if (this.masterUUID != null && player.getUUID().equals(this.masterUUID)) {
                // 被主人攻击，不记录，不反击
                return super.hurt(source, amount);
            }
        }

        // 所有变种在special动画期间防御远程攻击（类似盾牌）
        if (this.entityData.get(DATA_PLAYING_SPECIAL)) {
            if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                // 播放防御音效
                if (this.level() instanceof ServerLevel level) {
                    level.playSound(null, this.blockPosition(),
                            net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
                    // 播放盾牌防御粒子
                    level.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.CRIT,
                            this.getX(), this.getY() + 1, this.getZ(),
                            10, 0.5, 0.5, 0.5, 0.1
                    );
                }
                return false; // 完全防御远程攻击
            }
        }

        // 防御模式下不记录被攻击（不反击）
        if (getCommand() != CommandState.DEFEND) {
            // 记录被攻击的时间（用于反击逻辑）
            if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity) {
                lastHurtTick = this.tickCount;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        // 飞行时完全免疫摔落伤害
        if (this.entityData.get(DATA_IS_FLYING)) {
            return false;
        }
        // 有翅膀的尸兄免疫小额摔落伤害
        if (this.entityData.get(DATA_HAS_WING) && fallDistance < 5.0F) {
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        if (playerSkinId != null) {
            compound.putUUID("PlayerSkinId", playerSkinId);
        }
        if (playerSkinName != null) {
            compound.putString("PlayerSkinName", playerSkinName);
        }
        compound.putString("CustomId", this.entityData.get(DATA_CUSTOM_ID));
        compound.putInt("SkinState", this.entityData.get(DATA_SKIN_STATE));
        compound.putBoolean("SkinLoadStarted", this.skinLoadStarted);
        compound.putBoolean("PlayingShieye", this.entityData.get(DATA_PLAYING_SHIEYE));
        compound.putInt("ShieyeCooldown", this.shieyeCooldown);
        compound.putInt("EvolutionLevel", this.evolutionLevel);
        compound.putInt("Kills", this.kills);
        compound.putInt("Hunger", this.hunger);
        compound.putInt("CorpseHunger", this.corpseHunger);
        compound.putInt("PounceCooldown", this.pounceCooldown);
        compound.putInt("AcidSprayCooldown", this.acidSprayCooldown);
        compound.putInt("Variant", this.entityData.get(DATA_VARIANT));
        
        // 保存神志和贪婪系统数据
        compound.putBoolean("HasSentient", this.hasSentient);
        compound.putInt("SpeechCooldown", this.speechCooldown);
        compound.putBoolean("IsGreedy", this.isGreedy);
        compound.putInt("GreedCooldown", this.greedCooldown);
        
        // 保存器官系统数据
        compound.putBoolean("HasWing", this.entityData.get(DATA_HAS_WING));
        compound.putBoolean("HasTail", this.entityData.get(DATA_HAS_TAIL));
        compound.putBoolean("IsFlying", this.entityData.get(DATA_IS_FLYING));

        // 保存被攻击记忆
        compound.putInt("LastHurtTick", this.lastHurtTick);

        // 保存主人信息
        if (this.masterUUID != null) {
            compound.putUUID("MasterUUID", this.masterUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        if (compound.hasUUID("PlayerSkinId")) {
            this.playerSkinId = compound.getUUID("PlayerSkinId");
        }
        if (compound.contains("PlayerSkinName")) {
            this.playerSkinName = compound.getString("PlayerSkinName");
            this.entityData.set(DATA_PLAYER_NAME, this.playerSkinName);
        }
        if (compound.contains("CustomId")) {
            this.entityData.set(DATA_CUSTOM_ID, compound.getString("CustomId"));
        }
        if (compound.contains("SkinState")) {
            this.entityData.set(DATA_SKIN_STATE, compound.getInt("SkinState"));
        }
        if (compound.contains("SkinLoadStarted")) {
            this.skinLoadStarted = compound.getBoolean("SkinLoadStarted");
        }
        if (compound.contains("PlayingShieye")) {
            this.entityData.set(DATA_PLAYING_SHIEYE, compound.getBoolean("PlayingShieye"));
        }
        if (compound.contains("ShieyeCooldown")) {
            this.shieyeCooldown = compound.getInt("ShieyeCooldown");
        }
        if (compound.contains("EvolutionLevel")) {
            this.evolutionLevel = compound.getInt("EvolutionLevel");
        }
        if (compound.contains("Kills")) {
            this.kills = compound.getInt("Kills");
        }
        if (compound.contains("Hunger")) {
            this.hunger = compound.getInt("Hunger");
        }
        if (compound.contains("CorpseHunger")) {
            this.corpseHunger = compound.getInt("CorpseHunger");
        }
        if (compound.contains("PounceCooldown")) {
            this.pounceCooldown = compound.getInt("PounceCooldown");
        }
        if (compound.contains("AcidSprayCooldown")) {
            this.acidSprayCooldown = compound.getInt("AcidSprayCooldown");
        }
        if (compound.contains("Variant")) {
            this.entityData.set(DATA_VARIANT, compound.getInt("Variant"));
        }
        
        // 读取神志和贪婪系统数据
        if (compound.contains("HasSentient")) {
            this.hasSentient = compound.getBoolean("HasSentient");
        }
        if (compound.contains("SpeechCooldown")) {
            this.speechCooldown = compound.getInt("SpeechCooldown");
        }
        if (compound.contains("IsGreedy")) {
            this.isGreedy = compound.getBoolean("IsGreedy");
        }
        if (compound.contains("GreedCooldown")) {
            this.greedCooldown = compound.getInt("GreedCooldown");
        }
        
        // 读取器官系统数据
        if (compound.contains("HasWing")) {
            this.entityData.set(DATA_HAS_WING, compound.getBoolean("HasWing"));
        }
        if (compound.contains("HasTail")) {
            this.entityData.set(DATA_HAS_TAIL, compound.getBoolean("HasTail"));
        }
        if (compound.contains("IsFlying")) {
            this.entityData.set(DATA_IS_FLYING, compound.getBoolean("IsFlying"));
        }
        
        // 读取被攻击记忆
        if (compound.contains("LastHurtTick")) {
            this.lastHurtTick = compound.getInt("LastHurtTick");
        }

        // 读取主人信息
        if (compound.contains("MasterUUID")) {
            this.masterUUID = compound.getUUID("MasterUUID");
        }

        // 读取完成后更新自定义名称
        if (!this.level().isClientSide) {
            updateCustomName();
        }
    }

    @Override
    public void tick() {
        super.tick();

        // 处理 shieye 动画状态
        if (shieyeCooldown > 0) {
            shieyeCooldown--;
        }

        // 处理 shieye 动画计时器
        if (!this.level().isClientSide && this.entityData.get(DATA_PLAYING_SHIEYE)) {
            shieyeAnimationTicks--;
            if (shieyeAnimationTicks <= 0) {
                this.entityData.set(DATA_PLAYING_SHIEYE, false);
            }
        }

        // 处理 special 动画计时器
        if (!this.level().isClientSide && this.entityData.get(DATA_PLAYING_SPECIAL)) {
            int specialTicks = this.entityData.get(DATA_SPECIAL_TICKS);
            specialTicks--;
            this.entityData.set(DATA_SPECIAL_TICKS, specialTicks);
            if (specialTicks <= 0) {
                this.entityData.set(DATA_PLAYING_SPECIAL, false);
                onSpecialAnimationEnd();
            }
        }

        // 处理飞行状态
        if (this.entityData.get(DATA_HAS_WING)) {
            handleFlight();
        }

        // 确保飞行状态和重力状态同步
        if (!this.level().isClientSide) {
            if (this.entityData.get(DATA_IS_FLYING) && !this.isNoGravity()) {
                // 飞行状态但重力未取消，修复
                this.setNoGravity(true);
            } else if (!this.entityData.get(DATA_IS_FLYING) && this.isNoGravity() && !this.isPassenger()) {
                // 非飞行状态但有重力取消，修复（排除乘坐载具的情况）
                this.setNoGravity(false);
            }
        }

        // 服务端：饥饿度系统
        if (!this.level().isClientSide) {
            // 每100 tick（5秒）减少1点饥饿度
            if (this.tickCount % 100 == 0 && hunger > 0) {
                hunger--;
            }
            
            // 飞行时消耗更多饥饿度
            if (this.entityData.get(DATA_IS_FLYING) && this.tickCount % 20 == 0 && hunger > 0) {
                hunger--;
            }
            
            // 飞扑攻击冷却
            if (pounceCooldown > 0) {
                pounceCooldown--;
            }
            
            // 酸液喷射冷却
            if (acidSprayCooldown > 0) {
                acidSprayCooldown--;
            }
            
            // 尝试吞噬附近的尸体
            if (this.tickCount % 40 == 0) {
                tryEatNearbyCorpseGib();
            }
            
            // 饱腹值≥1时，尝试向目标喷射酸液
            if (this.corpseHunger >= 1 && this.tickCount % 60 == 0) {
                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive()) {
                    double dist = this.distanceTo(target);
                    if (dist > 2.0D && dist <= 8.0D) {
                        sprayAcidAtTarget(target);
                    }
                }
            }
            
            // 服务端：神志系统
            if (hasSentient) {
                if (speechCooldown > 0) {
                    speechCooldown--;
                } else if (this.tickCount % 200 == 0) { // 每10秒尝试说话
                    attemptSpeech();
                }
            }
            
            // 服务端：贪婪系统
            if (isGreedy) {
                if (greedCooldown > 0) {
                    greedCooldown--;
                } else if (this.tickCount % 150 == 0) { // 每7.5秒尝试贪婪行为
                    attemptGreedyBehavior();
                }
            }
            
            // 服务端：视力随等级变化
            updateVisionRange();

            // 服务端：随机触发special技能
            tickSpecialSkill();

            InnateSkillManager.tick(this);
        }

        // 客户端：加载皮肤
        if (this.level().isClientSide) {
            tickClient();
        }

        // 服务端：振动系统
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            VibrationSystem.Ticker.tick(serverLevel, this.vibrationData, this.vibrationUser);
        }

        // 服务端：跟随主人
        if (!this.level().isClientSide) {
            tickFollowMaster();
        }

        // 服务端：更新命令行为
        if (!this.level().isClientSide) {
            updateCommandBehavior();
        }

        // 服务端：更新定身实体
        if (!this.level().isClientSide) {
            tickImmobilizedEntities();
        }

        // 服务端：高阶尸兄自动开门
        if (!this.level().isClientSide) {
            tickDoorInteraction();
        }
    }

    /**
     * 处理飞行逻辑
     */
    private void handleFlight() {
        // 如果有主人且主人在空中，由 handleFlyingFollow 处理飞行逻辑
        if (shouldFollowMasterFlying()) {
            return;
        }

        // 检查是否在地面上
        boolean onGround = this.onGround();

        // 如果在地面上且饥饿度足够（>10），有概率起飞（随机起飞）
        if (onGround && hunger > 10 && this.random.nextFloat() < 0.05F) {
            startFlying();
        }

        // 如果在飞行中但饥饿度不足（<=5），强制降落
        if (this.entityData.get(DATA_IS_FLYING) && hunger <= 5) {
            stopFlying();
            CorpseOrigin.LOGGER.info("尸兄 {} 饥饿度不足，被迫降落", this.getId());
            return;
        }

        // 飞行时的移动逻辑（仅在没有主人或主人不在空中时执行）
        if (this.entityData.get(DATA_IS_FLYING)) {
            // 自由飞行模式 - 像创造模式玩家一样
            handleFreeFlight();
        }

        // 有翅膀但未飞行时（如下落中），添加缓降效果
        if (!this.entityData.get(DATA_IS_FLYING) && this.entityData.get(DATA_HAS_WING) && !this.onGround()) {
            applySlowFalling();
        }
    }

    /**
     * 应用缓降效果 - 有翅膀的尸兄下落时会缓慢降落
     */
    private void applySlowFalling() {
        // 获取当前速度
        double dy = this.getDeltaMovement().y;

        // 如果正在下落（y速度为负）
        if (dy < 0) {
            // 限制下落速度，实现缓降
            double slowFallSpeed = -0.15; // 最大下落速度
            if (dy < slowFallSpeed) {
                this.setDeltaMovement(
                        this.getDeltaMovement().x * 0.9,
                        slowFallSpeed,
                        this.getDeltaMovement().z * 0.9
                );
            }

            // 添加粒子效果（可选）
            if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 5 == 0) {
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.CLOUD,
                        this.getX(), this.getY() + 0.5, this.getZ(),
                        1, 0.2, 0.1, 0.2, 0.01
                );
            }
        }
    }

    /**
     * 开始飞行
     */
    private void startFlying() {
        setFlying(true);
        this.setNoGravity(true);
    }

    /**
     * 停止飞行
     */
    private void stopFlying() {
        setFlying(false);
        this.setNoGravity(false);
    }

    /**
     * 自由飞行处理 - 像创造模式玩家一样自由移动
     */
    private void handleFreeFlight() {
        // 确保不受重力影响
        this.setNoGravity(true);

        // 基础飞行速度
        double speed = 0.15;

        // 如果有目标，向目标飞行
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            flyTowardsTarget(this.getTarget(), speed);
        } else {
            // 没有目标时悬停或缓慢移动
            hoverOrWander(speed);
        }
    }

    /**
     * 向目标飞行
     */
    private void flyTowardsTarget(net.minecraft.world.entity.LivingEntity target, double speed) {
        double dx = target.getX() - this.getX();
        double dy = target.getY() + target.getEyeHeight() - this.getY() - this.getEyeHeight();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > 2.0) {
            // 归一化并设置速度
            dx /= distance;
            dy /= distance;
            dz /= distance;

            this.setDeltaMovement(
                    dx * speed,
                    dy * speed,
                    dz * speed
            );

            // 面向目标
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        } else {
            // 距离足够近，悬停
            this.setDeltaMovement(0, 0, 0);
        }
    }

    /**
     * 悬停或随机漫游
     */
    private void hoverOrWander(double speed) {
        // 简单的悬停逻辑 - 保持当前位置
        if (this.getDeltaMovement().lengthSqr() > 0.001) {
            // 减速到停止
            this.setDeltaMovement(
                    this.getDeltaMovement().x * 0.8,
                    this.getDeltaMovement().y * 0.8,
                    this.getDeltaMovement().z * 0.8
            );
        } else {
            // 完全悬停
            this.setDeltaMovement(0, 0, 0);
        }

        // 偶尔随机移动（模拟自由飞行）
        if (this.random.nextFloat() < 0.02F) {
            double rx = (this.random.nextDouble() - 0.5) * speed;
            double ry = (this.random.nextDouble() - 0.5) * speed * 0.5;
            double rz = (this.random.nextDouble() - 0.5) * speed;
            this.setDeltaMovement(rx, ry, rz);
        }
    }

    /**
     * 检查是否应该跟随主人飞行
     * 当主人在空中且距离较远时，有翅膀的尸兄应该起飞跟随
     */
    private boolean shouldFollowMasterFlying() {
        if (this.masterUUID == null) return false;
        if (!this.entityData.get(DATA_HAS_WING)) return false;

        if (!(this.level() instanceof ServerLevel serverLevel)) return false;
        Player master = serverLevel.getServer().getPlayerList().getPlayer(this.masterUUID);
        if (master == null || !master.isAlive()) return false;

        // 如果主人在空中（飞行或鞘翅滑翔）
        boolean masterInAir = !master.onGround() || master.isFallFlying();
        if (!masterInAir) return false;

        // 计算与主人的距离
        double verticalDistance = master.getY() - this.getY();
        double horizontalDistance = Math.sqrt(
                Math.pow(this.getX() - master.getX(), 2) +
                        Math.pow(this.getZ() - master.getZ(), 2)
        );

        // 如果主人在上方超过1格，或者水平距离超过3格，应该起飞跟随
        return verticalDistance > 1.0 || horizontalDistance > 3.0;
    }

    /**
     * 检查尸兄是否可以开门
     * 只有进化等级≥3的高阶尸兄才能打开铁门
     */
    public boolean canOpenDoors() {
        return this.evolutionLevel >= 3;
    }

    /**
     * 检查尸兄是否可以破坏门
     * 只有进化等级≥4的尸兄才能破坏门
     */
    public boolean canBreakDoors() {
        return this.evolutionLevel >= 4;
    }

    /**
     * special技能随机触发逻辑
     * 根据命令状态决定触发哪种效果：
     * - 防御模式：大概率发动防御效果（NORMAL变种效果）
     * - 进攻模式：大概率发动定身效果（CRACKED变种效果）
     * - 其他模式：正常随机触发
     */
    private void tickSpecialSkill() {
        // 如果已经在播放special动画，不触发
        if (this.entityData.get(DATA_PLAYING_SPECIAL)) return;

        CommandState command = getCommand();
        float triggerChance = 0.001F; // 基础几率 0.1%

        // 根据命令状态调整触发几率和效果
        switch (command) {
            case DEFEND:
                // 防御模式：大概率发动防御效果（5%几率）
                if (this.random.nextFloat() < 0.05F) {
                    triggerDefenseSpecial();
                }
                break;

            case ATTACK:
                // 进攻模式：大概率发动定身效果（5%几率）
                if (this.random.nextFloat() < 0.05F) {
                    triggerAttackSpecial();
                }
                break;

            case FOLLOW:
            case STAY:
            default:
                // 其他模式：正常随机触发（0.1%几率）
                if (this.random.nextFloat() < triggerChance) {
                    triggerSpecialAnimation();
                }
                break;
        }
    }

    /**
     * 防御模式下的special技能 - 防御效果
     */
    private void triggerDefenseSpecial() {
        if (this.level().isClientSide) return;

        this.entityData.set(DATA_PLAYING_SPECIAL, true);
        this.entityData.set(DATA_SPECIAL_TICKS, 100); // 5秒

        // 防御模式总是发动防御效果（类似NORMAL变种）
        applyNormalSpecialEffect();

        CorpseOrigin.LOGGER.info("尸兄 {} 在防御模式下发动防御技能", this.getId());
    }

    /**
     * 进攻模式下的special技能 - 定身效果
     */
    private void triggerAttackSpecial() {
        if (this.level().isClientSide) return;

        this.entityData.set(DATA_PLAYING_SPECIAL, true);
        this.entityData.set(DATA_SPECIAL_TICKS, 100); // 5秒

        // 进攻模式总是发动定身效果（类似CRACKED变种）
        applyCrackedSpecialEffect();

        CorpseOrigin.LOGGER.info("尸兄 {} 在进攻模式下发动定身技能", this.getId());
    }

    /**
     * 跟随主人的tick逻辑
     */
    private void tickFollowMaster() {
        if (this.masterUUID == null) return;

        // 减少跟随冷却
        if (followCooldown > 0) {
            followCooldown--;
            return;
        }

        // 每10tick检查一次
        if (this.tickCount % 10 != 0) return;

        // 获取主人
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Player master = serverLevel.getServer().getPlayerList().getPlayer(this.masterUUID);
        if (master == null || !master.isAlive()) return;

        double distanceToMaster = this.distanceToSqr(master);

        // 如果距离太远，传送到主人身边
        if (distanceToMaster > TELEPORT_RANGE * TELEPORT_RANGE) {
            teleportToMaster(master);
            return;
        }

        // 处理有翅膀尸兄的飞行跟随
        if (this.entityData.get(DATA_HAS_WING)) {
            handleFlyingFollow(master);
        }

        // 如果距离超出跟随范围，向主人移动（只在不飞行时使用地面导航）
        if (distanceToMaster > FOLLOW_RANGE * FOLLOW_RANGE && !this.entityData.get(DATA_IS_FLYING)) {
            this.getNavigation().moveTo(master, 1.2D);
            followCooldown = 20; // 1秒冷却
        }
    }

    /**
     * 处理飞行跟随逻辑
     * 有主人的尸兄会保持与主人相同的高度飞行
     */
    private void handleFlyingFollow(Player master) {
        // 检查主人在空中
        boolean masterInAir = !master.onGround() || master.isFallFlying();

        // 计算与主人的距离
        double verticalDistance = master.getY() - this.getY();
        double horizontalDistance = Math.sqrt(
                Math.pow(this.getX() - master.getX(), 2) +
                        Math.pow(this.getZ() - master.getZ(), 2)
        );

        // 如果主人在地面且尸兄在飞行，降落
        if (!masterInAir) {
            if (this.entityData.get(DATA_IS_FLYING)) {
                stopFlying();
            }
            return;
        }

        // 检查饥饿度 - 如果饥饿度不足，不能起飞或必须降落
        if (hunger <= 5) {
            // 饥饿度不足，如果在飞行中则降落
            if (this.entityData.get(DATA_IS_FLYING)) {
                stopFlying();
                CorpseOrigin.LOGGER.info("尸兄 {} 饥饿度不足({})，停止跟随降落", this.getId(), hunger);
            }
            return;
        }

        // 主人在空中，检查是否应该起飞跟随
        // 条件：主人在上方超过1格，或者水平距离超过3格
        boolean shouldFly = verticalDistance > 1.0 || horizontalDistance > 3.0;

        // 饥饿度中等（6-15）时，只在必要距离才起飞
        // 饥饿度高（>15）时，更容易起飞
        int hungerThreshold = hunger > 15 ? 3 : 5;
        if (hunger <= 15 && !shouldFly) {
            // 饥饿度中等且距离不够，不起飞
            if (this.entityData.get(DATA_IS_FLYING)) {
                stopFlying();
            }
            return;
        }

        if (shouldFly && hunger > 5) {
            // 起飞
            if (!this.entityData.get(DATA_IS_FLYING)) {
                startFlying();
                CorpseOrigin.LOGGER.info("尸兄 {} 起飞跟随主人（饥饿度：{}）", this.getId(), hunger);
            }

            // 如果在飞行中，向主人移动（保持相同高度）
            if (this.entityData.get(DATA_IS_FLYING)) {
                flyAtSameHeightAsMaster(master, horizontalDistance);
            }
        } else if (!shouldFly && this.entityData.get(DATA_IS_FLYING)) {
            // 距离很近且主人在附近，降落
            stopFlying();
        }
    }

    /**
     * 保持与主人相同高度飞行
     */
    private void flyAtSameHeightAsMaster(Player master, double horizontalDistance) {
        // 水平方向朝向主人移动
        double dx = master.getX() - this.getX();
        double dz = master.getZ() - this.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // 基础飞行速度
        double speed = 0.15;

        // 水平方向移动
        double moveX = 0;
        double moveZ = 0;
        if (horizontalDist > 2.0) {
            moveX = (dx / horizontalDist) * speed;
            moveZ = (dz / horizontalDist) * speed;
        }

        // 垂直方向：保持与主人相同高度
        double targetY = master.getY();
        double dy = targetY - this.getY();
        double moveY;

        // 根据高度差调整垂直速度
        if (Math.abs(dy) < 0.5) {
            // 高度接近，保持悬停
            moveY = 0;
        } else if (dy > 0) {
            // 主人在上方，向上飞
            moveY = Math.min(dy * 0.1, speed);
        } else {
            // 主人在下方，向下飞
            moveY = Math.max(dy * 0.1, -speed);
        }

        // 设置移动速度
        this.setDeltaMovement(moveX, moveY, moveZ);

        // 面向主人
        this.getLookControl().setLookAt(master, 30.0F, 30.0F);
    }

    /**
     * 传送到主人身边
     */
    private void teleportToMaster(Player master) {
        // 寻找主人附近的安全位置
        for (int i = 0; i < 10; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double distance = 2 + this.random.nextDouble() * 2;
            double targetX = master.getX() + Math.cos(angle) * distance;
            double targetZ = master.getZ() + Math.sin(angle) * distance;
            double targetY = master.getY();

            // 检查位置是否安全
            if (this.level().noCollision(this.getBoundingBox().move(targetX - this.getX(), targetY - this.getY(), targetZ - this.getZ()))) {
                this.teleportTo(targetX, targetY, targetZ);
                this.getNavigation().stop();
                followCooldown = 40; // 2秒冷却
                break;
            }
        }
    }

    /**
     * 设置主人
     */
    public void setMaster(UUID masterUUID) {
        this.masterUUID = masterUUID;
    }

    /**
     * 获取主人UUID
     */
    public UUID getMasterUUID() {
        return this.masterUUID;
    }

    /**
     * 检查是否有主人
     */
    public boolean hasMaster() {
        return this.masterUUID != null;
    }
    
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity entity) {
        // 如果目标是其他尸兄，检查是否应该攻击（饥饿且可以吞噬）
        if (entity instanceof LowerLevelZbEntity otherZb) {
            // 只有满足吞噬条件时才攻击其他尸兄
            if (!shouldAttackOtherZb(otherZb)) {
                return false; // 不攻击，避免无意义的互相攻击
            }
        }
        
        // 如果目标是已成为尸兄的玩家，检查是否应该攻击
        if (entity instanceof Player player) {
            if (com.phagens.corpseorigin.player.PlayerCorpseData.isCorpse(player)) {
                // 如果被攻击了，允许反击
                boolean wasRecentlyHurt = (this.tickCount - lastHurtTick) < HURT_MEMORY_DURATION;
                if (wasRecentlyHurt) {
                    CorpseOrigin.LOGGER.info("尸兄 {} 反击同类玩家 {}", this.getId(), player.getName().getString());
                    // 继续执行攻击逻辑
                } else {
                    // 同类尸兄玩家，只有在极度饥饿时才攻击
                    boolean isHungry = this.hunger <= HUNGER_THRESHOLD_FOR_CANNIBALISM;
                    boolean notUnderKing = !isUnderZombieKingLeadership();
                    
                    // 只有在满足吞噬条件时才攻击同类玩家
                    if (!isHungry || !notUnderKing) {
                        return false; // 不饥饿或有尸王领导时不攻击同类
                    }
                    
                    // 满足条件，可以攻击同类玩家
                    CorpseOrigin.LOGGER.info("尸兄 {} 因极度饥饿攻击同类玩家 {}", this.getId(), player.getName().getString());
                }
            }
        }
        
        boolean result = super.doHurtTarget(entity);
        
        if (result && !this.level().isClientSide) {
            // 攻击时播放吃的音效
            float pitch = 0.8F + this.random.nextFloat() * 0.4F;
            this.playSound(ModSounds.GROUND_CHI.get(), 1.0F, pitch);
            
            // 随机感染村民 - 大多数作为食物（85%），极少数感染（15%）
            if (entity instanceof net.minecraft.world.entity.npc.Villager villager) {
                if (this.random.nextFloat() < 0.15) { // 15% 概率感染，85%作为食物
                    infectVillager(villager);
                }
            }
            
            // 击杀目标后处理进化逻辑
            handleKill(entity);
        }
        
        return result;
    }
    
    /**
     * 感染村民为尸兄
     */
    private void infectVillager(net.minecraft.world.entity.npc.Villager villager) {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        
        // 使用 BYeffect 来感染村民（3-15秒随机延迟后变异）
        com.phagens.corpseorigin.effect.BYeffect.applyInfection(villager, serverLevel);
    }
    
    /**
     * 判断是否应该攻击另一个尸兄
     * 只有在极度饥饿且满足吞噬条件时才攻击
     */
    private boolean shouldAttackOtherZb(LowerLevelZbEntity otherZb) {
        // 检查吞噬条件
        boolean isHungry = this.hunger <= HUNGER_THRESHOLD_FOR_CANNIBALISM;
        boolean notUnderKing = !isUnderZombieKingLeadership();
        
        // 只有在满足吞噬条件时才攻击
        return isHungry && notUnderKing;
    }
    
    private void handleKill(net.minecraft.world.entity.Entity target) {
        // 如果目标是其他尸兄，触发吞噬机制
        if (target instanceof LowerLevelZbEntity otherZb) {
            // 吞噬条件已经在 shouldAttackOtherZb 中检查过了
            // 30%概率触发吞噬
            if (this.random.nextFloat() < 0.3F) {
                CorpseOrigin.LOGGER.info("尸兄 {} 吞噬了尸兄 {}！饥饿度: {}", this.getId(), otherZb.getId(), this.hunger);
                performCannibalism(otherZb);
            }
            return; // 不增加击杀计数，吞噬单独处理
        }
        
        // 增加击杀计数
        this.kills++;
        
        // 播放吞噬效果
        this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
        
        // 根据被击杀生物的生命值恢复对应血量
        if (target instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            float targetHealth = livingEntity.getMaxHealth();
            // 恢复被击杀生物最大生命值的20%
            this.heal(targetHealth * 0.2F);
        }
        
        // 检查是否击杀鸟类/鸡，有概率长出羽翼
        if (isBirdOrChicken(target) && !hasWing()) {
            if (this.random.nextFloat() < 0.3f) { // 30%概率
                setHasWing(true);
                // 根据当前变种类型切换到对应的翅膀变种
                Variant currentVariant = getVariant();
                Variant newVariant = switch (currentVariant) {
                    case CRACKED -> Variant.WINGS_CRACKED;
                    default -> Variant.WINGS;
                };
                setVariant(newVariant);
                CorpseOrigin.LOGGER.info("尸兄 {} 击杀鸟类后长出了羽翼，变种变为: {}", this.getId(), newVariant.name());
            }
        }
        
        // 检查是否击杀鱼类，有概率长出鱼尾
        if (isFish(target) && !hasTail()) {
            if (this.random.nextFloat() < 0.3f) { // 30%概率
                setHasTail(true);
                CorpseOrigin.LOGGER.info("尸兄 {} 击杀鱼类后长出了鱼尾", this.getId());
            }
        }
        
        // 检查是否可以进化
        checkEvolution();
    }
    
    /**
     * 检查是否被尸王领导
     * 尸王是进化等级5的尸兄，在32格范围内会领导其他尸兄
     */
    private boolean isUnderZombieKingLeadership() {
        if (this.level().isClientSide) return false;
        
        // 如果自己就是尸王（等级5），不需要检查
        if (this.evolutionLevel >= 5) return false;
        
        // 检查32格范围内是否有进化等级5的尸王
        return this.level().getEntitiesOfClass(LowerLevelZbEntity.class, 
            this.getBoundingBox().inflate(32.0D))
            .stream()
            .anyMatch(zb -> zb.evolutionLevel >= 5 && zb != this);
    }
    
    /**
     * 执行尸兄互相吞噬
     * 只有一个尸兄能存活并获得进化
     */
    private void performCannibalism(LowerLevelZbEntity otherZb) {
        if (this.level().isClientSide) return;
        
        boolean thisSurvives = shouldSurvive(otherZb);
        
        if (thisSurvives) {
            CorpseOrigin.LOGGER.info("尸兄 {} 吞噬了尸兄 {}！", this.getId(), otherZb.getId());
            
            this.level().broadcastEntityEvent(this, (byte) 35);
            this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 2.0F, 0.8F);
            
            int evolutionGain = Math.max(1, otherZb.getEvolutionLevel());
            this.kills += evolutionGain * 2;
            this.heal(this.getMaxHealth() * 0.5F);
            this.hunger = 100;
            
            this.addCorpseHunger(otherZb.getCorpseHunger());
            
            CorpseOrigin.LOGGER.info("尸兄 {} 继承了 {} 点饱腹值，当前饱腹值: {}", this.getId(), otherZb.getCorpseHunger(), this.corpseHunger);
            
            checkEvolution();
            
            otherZb.discard();
        } else {
            CorpseOrigin.LOGGER.info("尸兄 {} 被尸兄 {} 吞噬！", this.getId(), otherZb.getId());
            
            this.level().broadcastEntityEvent(otherZb, (byte) 35);
            otherZb.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 2.0F, 0.8F);
            
            int evolutionGain = Math.max(1, this.evolutionLevel);
            otherZb.setKills(otherZb.getKills() + evolutionGain * 2);
            otherZb.heal(otherZb.getMaxHealth() * 0.5F);
            otherZb.hunger = 100;
            
            otherZb.addCorpseHunger(this.corpseHunger);
            
            CorpseOrigin.LOGGER.info("尸兄 {} 继承了 {} 点饱腹值，当前饱腹值: {}", otherZb.getId(), this.corpseHunger, otherZb.corpseHunger);
            
            otherZb.checkEvolution();
            
            this.discard();
        }
    }
    
    /**
     * 判断这个尸兄是否应该在与另一个尸兄的吞噬中存活
     */
    private boolean shouldSurvive(LowerLevelZbEntity other) {
        // 优先比较进化等级
        if (this.evolutionLevel != other.getEvolutionLevel()) {
            return this.evolutionLevel > other.getEvolutionLevel();
        }
        
        // 等级相同，比较当前生命值
        if (this.getHealth() != other.getHealth()) {
            return this.getHealth() > other.getHealth();
        }
        
        // 生命值也相同，随机决定
        return this.random.nextBoolean();
    }
    
    private void checkEvolution() {
        int requiredKills = this.evolutionLevel * 6; // 每个等级需要6次击杀，需要多吃些生物才会进阶
        
        if (this.kills >= requiredKills && this.evolutionLevel < 5) {
            evolve();
        }
    }
    
    private void evolve() {
        this.evolutionLevel++;
        this.kills = 0;
        
        // 增加属性
        double healthBonus = this.evolutionLevel * 5.0;
        double damageBonus = this.evolutionLevel * 1.0;
        double speedBonus = this.evolutionLevel * 0.05;
        
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0 + healthBonus);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.0 + damageBonus);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3 + speedBonus);
        
        this.setHealth(this.getMaxHealth());
        
        // 播放进化效果
        this.level().broadcastEntityEvent(this, (byte) 20);
        this.playSound(net.minecraft.sounds.SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.5F, 0.8F);
        
        CorpseOrigin.LOGGER.info("尸兄进化到 {} 级！", this.evolutionLevel);
    }

    @OnlyIn(Dist.CLIENT)
    private void tickClient() {
        String name = this.entityData.get(DATA_PLAYER_NAME);
        int stateCode = this.entityData.get(DATA_SKIN_STATE);
        ZbSkinState currentState = ZbSkinState.fromCode(stateCode);

        // 如果有玩家名
        if (name != null && !name.isEmpty()) {
            // 情况1：未开始加载，且当前状态允许加载
            if (!skinLoadStarted && (currentState == ZbSkinState.NOT_LOADED || currentState == ZbSkinState.FAILED)) {
                startSkinLoad(name);
            }
            // 情况2：皮肤状态是LOADED但皮肤纹理为null（重启后会出现）
            else if (currentState == ZbSkinState.LOADED && this.skinTexture == null) {
                // 重新加载皮肤
                startSkinLoad(name);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void startSkinLoad(String playerName) {
        // 即使已经开始加载，也要重新加载（用于重启后皮肤为null的情况）
        skinLoadStarted = true;

        // 设置状态为加载中
        this.entityData.set(DATA_SKIN_STATE, ZbSkinState.LOADING.getCode());

        // 异步加载皮肤
        ZbSkinLoader.loadSkinAsync(this, playerName);
    }

    /**
     * 设置皮肤纹理（客户端调用）
     */
    @OnlyIn(Dist.CLIENT)
    public void setSkinTexture(ResourceLocation texture) {
        this.skinTexture = texture;
    }

    /**
     * 获取皮肤纹理（渲染器调用）
     */
    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getSkinTexture() {
        return this.skinTexture;
    }

    /**
     * 设置皮肤状态（客户端调用后通知服务端）
     */
    @OnlyIn(Dist.CLIENT)
    public void setSkinState(ZbSkinState state) {
        this.entityData.set(DATA_SKIN_STATE, state.getCode());

        // 发送网络包通知服务端
        if (!(this.level() instanceof ServerLevel)) {
            try {
                PacketDistributor.sendToServer(new ZbSkinUpdatePacket(
                        this.getId(),
                        this.skinTexture,
                        state.getCode()
                ));
            } catch (Exception e) {
                CorpseOrigin.LOGGER.error("发送皮肤更新包失败", e);
            }
        }
    }

    /**
     * 获取皮肤状态（服务端/客户端均可）
     */
    public ZbSkinState getSkinState() {
        return ZbSkinState.fromCode(this.entityData.get(DATA_SKIN_STATE));
    }

    public UUID getPlayerSkinId() {
        return playerSkinId;
    }

    public String getPlayerSkinName() {
        return this.entityData.get(DATA_PLAYER_NAME);
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this.vibrationUser;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence();
    }
    
    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        // 尸族免疫普通毒素
        if (effect.getEffect().value() == net.minecraft.world.effect.MobEffects.POISON.value() ||
            effect.getEffect().value() == net.minecraft.world.effect.MobEffects.HUNGER.value() ||
            effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    /**
     * 更新实体的自定义名称（用于玉模组和头顶标签）
     */
    private void updateCustomName() {
        String customId = this.getCustomId();
        String playerName = this.getPlayerSkinName();

        net.minecraft.network.chat.Component displayName;

        if (!customId.isEmpty()) {
            displayName = net.minecraft.network.chat.Component.translatable(
                    "entity.corpseorigin.lower_level_zb.named",
                    customId
            );
        } else if (playerName != null && !playerName.isEmpty()) {
            displayName = net.minecraft.network.chat.Component.translatable(
                    "entity.corpseorigin.lower_level_zb.named",
                    playerName
            );
        } else {
            displayName = net.minecraft.network.chat.Component.translatable(
                    "entity.corpseorigin.lower_level_zb.default"
            );
        }

        // 设置自定义名称
        this.setCustomName(displayName);
        // 确保始终显示（可选）
        this.setCustomNameVisible(false);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        String customId = this.getCustomId();
        String playerName = this.getPlayerSkinName();

        if (!customId.isEmpty()) {
            // 有自定义ID时：使用 entity.corpseorigin.lower_level_zb.named
            return net.minecraft.network.chat.Component.translatable(
                    "entity.corpseorigin.lower_level_zb.named",
                    customId
            );
        } else if (playerName != null && !playerName.isEmpty()) {
            // 有玩家皮肤名称时：也使用 entity.corpseorigin.lower_level_zb.named
            return net.minecraft.network.chat.Component.translatable(
                    "entity.corpseorigin.lower_level_zb.named",
                    playerName
            );
        } else {
            // 默认显示：使用 entity.corpseorigin.lower_level_zb.default
            return net.minecraft.network.chat.Component.translatable(
                    "entity.corpseorigin.lower_level_zb.default"
            );
        }
    }
    
    public int getEvolutionLevel() {
        return this.evolutionLevel;
    }
    
    public int getKills() {
        return this.kills;
    }
    
    public void setEvolutionLevel(int level) {
        this.evolutionLevel = Math.max(1, Math.min(5, level));
    }
    
    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }
    
    /**
     * 器官系统方法
     */
    public boolean hasWing() {
        return this.entityData.get(DATA_HAS_WING);
    }
    
    public boolean hasTail() {
        return this.entityData.get(DATA_HAS_TAIL);
    }
    
    public void setHasWing(boolean hasWing) {
        boolean wasWing = this.entityData.get(DATA_HAS_WING);
        this.entityData.set(DATA_HAS_WING, hasWing);
        // 如果翅膀状态改变，更新导航
        if (wasWing != hasWing) {
            updateNavigation();
        }
    }
    
    public void setHasTail(boolean hasTail) {
        this.entityData.set(DATA_HAS_TAIL, hasTail);
    }
    
    /**
     * 飞扑攻击相关方法
     */
    public boolean isPouncing() {
        return this.isPouncing;
    }
    
    public void setPouncing(boolean pouncing) {
        this.isPouncing = pouncing;
    }
    
    public int getPounceCooldown() {
        return this.pounceCooldown;
    }
    
    public void resetPounceCooldown() {
        this.pounceCooldown = POUNCE_COOLDOWN_TICKS;
    }
    
    /**
     * 尸体饱腹值相关方法
     */
    public int getCorpseHunger() {
        return this.corpseHunger;
    }
    
    public void setCorpseHunger(int value) {
        this.corpseHunger = Math.min(MAX_CORPSE_HUNGER, Math.max(0, value));
        
        if (this.corpseHunger >= MAX_CORPSE_HUNGER) {
            onCorpseHungerFull();
        }
    }
    
    public void addCorpseHunger(int amount) {
        setCorpseHunger(this.corpseHunger + amount);
    }
    
    /**
     * 当饱腹值达到3时触发等级提升
     */
    private void onCorpseHungerFull() {
        if (this.evolutionLevel < 5) {
            evolve();
            CorpseOrigin.LOGGER.info("尸兄 {} 因吞噬尸体饱腹值满而进化到 {} 级！", this.getId(), this.evolutionLevel);
        }
        this.corpseHunger = 0;
    }
    
    /**
     * 吞噬尸体实体
     */
    public void eatCorpseGib(CorpseGibEntity gib) {
        if (this.level().isClientSide) return;
        
        gib.discard();
        addCorpseHunger(1);
        
        this.playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                this.getX(), this.getY() + 0.5, this.getZ(),
                5, 0.3, 0.3, 0.3, 0.1);
        }
        
        CorpseOrigin.LOGGER.info("尸兄 {} 吞噬了尸体，饱腹值: {}", this.getId(), this.corpseHunger);
    }
    
    /**
     * 尝试吞噬附近的尸体实体
     */
    private void tryEatNearbyCorpseGib() {
        if (this.level().isClientSide) return;
        if (this.corpseHunger >= MAX_CORPSE_HUNGER) return;
        
        AABB searchBox = this.getBoundingBox().inflate(2.0D);
        List<CorpseGibEntity> nearbyGibs = this.level().getEntitiesOfClass(CorpseGibEntity.class, searchBox);
        
        if (!nearbyGibs.isEmpty()) {
            CorpseGibEntity nearestGib = null;
            double nearestDist = Double.MAX_VALUE;
            
            for (CorpseGibEntity gib : nearbyGibs) {
                double dist = this.distanceToSqr(gib);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearestGib = gib;
                }
            }
            
            if (nearestGib != null) {
                eatCorpseGib(nearestGib);
            }
        }
    }
    
    /**
     * 喷射酸液攻击
     * 饱腹值≥1时向敌人喷射酸液，赋予5秒中毒效果
     */
    public void sprayAcidAtTarget(LivingEntity target) {
        if (this.level().isClientSide) return;
        if (this.corpseHunger < 1) return;
        if (this.acidSprayCooldown > 0) return;
        
        double distance = this.distanceTo(target);
        if (distance > 8.0D) return;
        
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true));
        
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 start = this.position().add(0, 1.0, 0);
            Vec3 end = target.position().add(0, target.getBbHeight() / 2, 0);
            Vec3 direction = end.subtract(start).normalize();
            
            for (int i = 0; i < 10; i++) {
                double t = i / 10.0;
                double x = start.x + direction.x * distance * t;
                double y = start.y + direction.y * distance * t;
                double z = start.z + direction.z * distance * t;
                
                serverLevel.sendParticles(ParticleTypes.DRIPPING_HONEY,
                    x, y, z, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }
        
        this.acidSprayCooldown = ACID_SPRAY_COOLDOWN_TICKS;
        
        CorpseOrigin.LOGGER.info("尸兄 {} 向 {} 喷射了酸液", this.getId(), target.getName().getString());
    }
    
    /**
     * 飞行状态方法
     */
    public boolean isFlying() {
        return this.entityData.get(DATA_IS_FLYING);
    }
    
    public void setFlying(boolean flying) {
        boolean wasFlying = this.entityData.get(DATA_IS_FLYING);
        this.entityData.set(DATA_IS_FLYING, flying);

        // 同步更新重力状态
        if (flying && !wasFlying) {
            this.setNoGravity(true);
        } else if (!flying && wasFlying) {
            this.setNoGravity(false);
        }
    }
    
    /**
     * 触发 shieye 动画
     */
    private void triggerShieyeAnimation() {
        this.entityData.set(DATA_PLAYING_SHIEYE, true);
        this.shieyeCooldown = 200; // 10秒冷却时间
        this.shieyeAnimationTicks = 60; // 3秒 = 60 ticks，动画持续时间
    }

    /**
     * 触发 special 动画
     * CRACKED变种：定身效果
     * NORMAL变种：防御效果
     */
    public void triggerSpecialAnimation() {
        if (this.level().isClientSide) return;
        if (this.entityData.get(DATA_PLAYING_SPECIAL)) return; // 已经在播放

        this.entityData.set(DATA_PLAYING_SPECIAL, true);
        this.entityData.set(DATA_SPECIAL_TICKS, 100); // 5秒 = 100 ticks

        Variant variant = getVariant();
        if (variant == Variant.CRACKED) {
            // CRACKED变种：定身效果 - 冻结附近敌人
            applyCrackedSpecialEffect();
        } else {
            // NORMAL变种：防御效果 - 减少受到的伤害
            applyNormalSpecialEffect();
        }
    }

    /**
     * CRACKED变种的特殊效果：定身附近4格内的敌人
     * 真正的定身：禁止移动、禁止跳跃、锁定位置
     */
    private void applyCrackedSpecialEffect() {
        if (!(this.level() instanceof ServerLevel level)) return;

        // 获取附近的目标（4格范围内）
        double range = 4.0D;
        var nearbyEntities = level.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                this.getBoundingBox().inflate(range),
                entity -> entity != this && !(entity instanceof LowerLevelZbEntity)
        );

        long currentTime = level.getGameTime();

        // 对敌人施加定身效果
        for (var entity : nearbyEntities) {
            if (entity instanceof Player player) {
                // 如果是玩家主人，不施加效果
                if (this.masterUUID != null && player.getUUID().equals(this.masterUUID)) {
                    continue;
                }
            }

            // 施加缓慢效果（禁止移动）
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 10, false, true
            ));

            // 记录定身状态：锁定5秒（100 tick），与特殊动画持续时间匹配
            immobilizedEntities.put(entity.getUUID(), currentTime + 100);

            // 播放粒子效果
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SOUL,
                    entity.getX(), entity.getY() + 1, entity.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1
            );
        }

        // 播放音效
        level.playSound(null, this.blockPosition(),
                net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.5F);
    }

    /**
     * 更新定身实体：强制锁定位置
     */
    private void tickImmobilizedEntities() {
        if (!(this.level() instanceof ServerLevel level)) return;

        long currentTime = level.getGameTime();

        // 遍历所有被定身的实体
        immobilizedEntities.entrySet().removeIf(entry -> {
            UUID entityId = entry.getKey();
            long unlockTime = entry.getValue();

            // 如果定身时间已过，移除
            if (currentTime >= unlockTime) {
                return true;
            }

            // 获取实体并强制锁定位置
            net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
            if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                // 强制停止移动
                livingEntity.setDeltaMovement(0, 0, 0);
                livingEntity.hurtMarked = true; // 标记需要同步

                // 如果是玩家，额外设置位置（防止客户端预测移动）
                if (livingEntity instanceof Player player) {
                    // 强制玩家在原地
                    player.setDeltaMovement(0, 0, 0);
                }
            } else {
                // 实体不存在或已死亡，移除记录
                return true;
            }

            return false;
        });
    }

    /**
     * NORMAL变种的特殊效果：防御增强
     */
    private void applyNormalSpecialEffect() {
        if (!(this.level() instanceof ServerLevel level)) return;

        // 添加防御效果
        this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 100, 1, false, true
        ));
        this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.ABSORPTION, 100, 0, false, true
        ));

        // 播放粒子效果
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                this.getX(), this.getY() + 1, this.getZ(),
                30, 0.5, 1, 0.5, 0.1
        );

        // 播放音效
        level.playSound(null, this.blockPosition(),
                net.minecraft.sounds.SoundEvents.IRON_GOLEM_REPAIR, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    /**
     * special动画结束时的回调
     */
    private void onSpecialAnimationEnd() {
        // 动画结束，可以在这里添加后续效果
    }

    /**
     * 检查是否正在播放special动画
     */
    public boolean isPlayingSpecial() {
        return this.entityData.get(DATA_PLAYING_SPECIAL);
    }
    
    /**
     * 更新视力范围
     * 基础尸兄在晚上和人类一样的视力
     * 随着等级提升，视力范围增加
     */
    private void updateVisionRange() {
        double baseVision = 16.0D; // 人类基础视力
        double levelBonus = this.evolutionLevel * 2.0D; // 每级增加2格视力
        double totalVision = baseVision + levelBonus;
        
        // 更新视力属性
        if (this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(totalVision);
        }
    }
    
    /**
     * 尝试说话
     * 保留生前神志的尸兄会随机口吐人言
     */
    private void attemptSpeech() {
        if (this.random.nextFloat() < 0.3) { // 30%概率说话
            String[] phrases = {
                "救...救我...",
                "好饿...",
                "我...我怎么了...",
                "不要...不要杀我...",
                "肉...肉...",
                "谁来...救救我...",
                "好疼...好疼...",
                "我不想死..."
            };
            
            String phrase = phrases[this.random.nextInt(phrases.length)];
            this.level().broadcastEntityEvent(this, (byte) 46); // 播放说话效果
            
            // 发送聊天消息（只有附近的玩家能看到）
            net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.literal(phrase);
            for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(32.0D))) {
                player.sendSystemMessage(message);
            }
            
            speechCooldown = 400; // 20秒冷却
        }
    }
    
    /**
     * 尝试贪婪行为
     * 贪婪的尸兄会被黄金和绿宝石吸引
     */
    private void attemptGreedyBehavior() {
        // 搜索附近的黄金和绿宝石物品
        java.util.List<net.minecraft.world.entity.item.ItemEntity> items = this.level().getEntitiesOfClass(
            net.minecraft.world.entity.item.ItemEntity.class,
            this.getBoundingBox().inflate(16.0D)
        );
        
        net.minecraft.world.entity.item.ItemEntity targetItem = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (net.minecraft.world.entity.item.ItemEntity item : items) {
            net.minecraft.world.item.ItemStack stack = item.getItem();
            if (isValuableItem(stack)) {
                double distance = this.distanceToSqr(item);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    targetItem = item;
                }
            }
        }
        
        // 如果找到有价值的物品，靠近它
        if (targetItem != null) {
            this.getNavigation().moveTo(targetItem, 1.2D);
            greedCooldown = 200; // 10秒冷却
        }
    }
    
    /**
     * 判断物品是否有价值（黄金或绿宝石）
     */
    private boolean isValuableItem(net.minecraft.world.item.ItemStack stack) {
        net.minecraft.world.item.Item item = stack.getItem();
        return item == net.minecraft.world.item.Items.GOLD_INGOT ||
               item == net.minecraft.world.item.Items.GOLD_NUGGET ||
               item == net.minecraft.world.item.Items.GOLD_BLOCK ||
               item == net.minecraft.world.item.Items.EMERALD ||
               item == net.minecraft.world.item.Items.EMERALD_BLOCK ||
               item == net.minecraft.world.item.Items.GOLDEN_APPLE ||
               item == net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE;
    }

    // ==================== 尸兄命令系统 ====================

    /**
     * 设置命令状态
     * @param command 命令状态
     */
    public void setCommand(CommandState command) {
        this.currentCommand = command;
        this.entityData.set(DATA_COMMAND_STATE, command.getCode());

        // 根据命令执行相应行为
        switch (command) {
            case STAY:
                // 停止移动
                this.getNavigation().stop();
                break;
            case ATTACK:
                // 主动攻击模式 - 增加攻击欲望
                break;
            case DEFEND:
                // 防御模式 - 停止攻击，只防御
                this.setTarget(null);
                break;
            case FOLLOW:
            default:
                // 跟随模式 - 默认行为
                break;
        }

        CorpseOrigin.LOGGER.info("尸兄 {} 命令状态变更为: {}", this.getId(), command.name());
    }

    /**
     * 获取当前命令状态
     */
    public CommandState getCommand() {
        return CommandState.fromCode(this.entityData.get(DATA_COMMAND_STATE));
    }

    /**
     * 主人对尸兄执行命令
     * @param master 主人玩家
     * @param command 命令
     * @return 是否成功执行
     */
    public boolean executeCommand(Player master, CommandState command) {
        // 检查是否是主人
        if (this.masterUUID == null || !this.masterUUID.equals(master.getUUID())) {
            return false;
        }

        // 设置命令
        setCommand(command);

        // 播放命令确认音效
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
        }

        // 发送消息给主人
        String message = switch (command) {
            case FOLLOW -> "§a尸兄开始跟随你！";
            case ATTACK -> "§c尸兄进入攻击模式！";
            case DEFEND -> "§b尸兄进入防御模式！";
            case STAY -> "§7尸兄停留在原地！";
        };
        master.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));

        return true;
    }

    /**
     * 检查尸兄是否接受命令（有主人且主人在附近）
     */
    public boolean canAcceptCommand(Player player) {
        if (this.masterUUID == null) return false;
        if (!this.masterUUID.equals(player.getUUID())) return false;

        // 检查主人是否在32格范围内
        return this.distanceToSqr(player) <= 32.0D * 32.0D;
    }

    /**
     * 根据当前命令状态更新行为
     * 在tick中调用
     */
    private void updateCommandBehavior() {
        CommandState command = getCommand();

        switch (command) {
            case STAY:
                // 停留模式：停止导航，不主动移动，不主动攻击
                if (this.getNavigation().isInProgress()) {
                    this.getNavigation().stop();
                }
                // 清除当前目标，停止攻击
                if (this.getTarget() != null) {
                    this.setTarget(null);
                }
                break;

            case DEFEND:
                // 防御模式：完全被动，不主动攻击，被攻击也不反击
                // 立即清除当前目标
                if (this.getTarget() != null) {
                    this.setTarget(null);
                }
                // 停止导航
                if (this.getNavigation().isInProgress()) {
                    this.getNavigation().stop();
                }
                break;

            case ATTACK:
                // 攻击模式：主动寻找并攻击附近敌人
                if (this.tickCount % 20 == 0 && this.getTarget() == null) {
                    // 每1秒寻找一次目标
                    findAndAttackTarget();
                }
                break;

            case FOLLOW:
            default:
                // 跟随模式：默认行为，由tickFollowMaster处理
                break;
        }
    }

    /**
     * 攻击模式下寻找并攻击目标
     */
    private void findAndAttackTarget() {
        if (!(this.level() instanceof ServerLevel level)) return;

        // 按优先级寻找目标
        // 第一优先级：非尸兄玩家
        var normalPlayers = level.getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(16.0D),
                entity -> shouldAttackNormalPlayer(entity)
        );
        if (!normalPlayers.isEmpty()) {
            // 选择最近的非尸兄玩家
            Player closest = null;
            double minDist = Double.MAX_VALUE;
            for (var player : normalPlayers) {
                double dist = this.distanceToSqr(player);
                if (dist < minDist) {
                    minDist = dist;
                    closest = player;
                }
            }
            if (closest != null) {
                this.setTarget(closest);
                return;
            }
        }

        // 第二优先级：非尸兄的其他怪物
        var nonCorpseMobs = level.getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class,
                this.getBoundingBox().inflate(16.0D),
                entity -> shouldAttackNonCorpseMob(entity)
        );
        if (!nonCorpseMobs.isEmpty()) {
            net.minecraft.world.entity.Mob closest = null;
            double minDist = Double.MAX_VALUE;
            for (var mob : nonCorpseMobs) {
                double dist = this.distanceToSqr(mob);
                if (dist < minDist) {
                    minDist = dist;
                    closest = mob;
                }
            }
            if (closest != null) {
                this.setTarget(closest);
                return;
            }
        }

        // 第三优先级：动物
        var animals = level.getEntitiesOfClass(
                net.minecraft.world.entity.animal.Animal.class,
                this.getBoundingBox().inflate(16.0D),
                entity -> entity.isAlive()
        );
        if (!animals.isEmpty()) {
            net.minecraft.world.entity.animal.Animal closest = null;
            double minDist = Double.MAX_VALUE;
            for (var animal : animals) {
                double dist = this.distanceToSqr(animal);
                if (dist < minDist) {
                    minDist = dist;
                    closest = animal;
                }
            }
            if (closest != null) {
                this.setTarget(closest);
                return;
            }
        }

        // 第四优先级：尸兄玩家（极度饥饿时）
        var corpsePlayers = level.getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(16.0D),
                entity -> shouldAttackCorpsePlayer(entity)
        );
        if (!corpsePlayers.isEmpty()) {
            Player closest = null;
            double minDist = Double.MAX_VALUE;
            for (var player : corpsePlayers) {
                double dist = this.distanceToSqr(player);
                if (dist < minDist) {
                    minDist = dist;
                    closest = player;
                }
            }
            if (closest != null) {
                this.setTarget(closest);
                return;
            }
        }

        // 第五优先级：其他尸兄实体（极度饥饿时）
        var otherCorpses = level.getEntitiesOfClass(
                LowerLevelZbEntity.class,
                this.getBoundingBox().inflate(16.0D),
                entity -> shouldAttackOtherCorpseEntity(entity)
        );
        if (!otherCorpses.isEmpty()) {
            LowerLevelZbEntity closest = null;
            double minDist = Double.MAX_VALUE;
            for (var corpse : otherCorpses) {
                double dist = this.distanceToSqr(corpse);
                if (dist < minDist) {
                    minDist = dist;
                    closest = corpse;
                }
            }
            if (closest != null) {
                this.setTarget(closest);
            }
        }
    }

    /**
     * 玩家与尸兄互动（右键点击）
     * Shift+右键：切换命令状态
     */
    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        // 只处理主手交互，避免触发两次
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return net.minecraft.world.InteractionResult.PASS;
        }

        // 检查是否是主人
        if (this.masterUUID == null || !this.masterUUID.equals(player.getUUID())) {
            return super.mobInteract(player, hand);
        }

        // 获取当前命令状态（使用getCommand确保同步）
        CommandState currentCmd = getCommand();

        // Shift+右键切换命令
        if (player.isShiftKeyDown()) {
            // 切换到下一个命令状态
            CommandState nextCommand = switch (currentCmd) {
                case FOLLOW -> CommandState.ATTACK;
                case ATTACK -> CommandState.DEFEND;
                case DEFEND -> CommandState.STAY;
                case STAY -> CommandState.FOLLOW;
            };

            // 执行命令
            executeCommand(player, nextCommand);

            // 播放互动音效
            if (this.level() instanceof ServerLevel level) {
                level.playSound(null, this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.5F);
            }

            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // 普通右键显示当前状态
        String state = switch (currentCmd) {
            case FOLLOW -> "§a跟随";
            case ATTACK -> "§c攻击";
            case DEFEND -> "§b防御（被动）";
            case STAY -> "§7停留";
        };
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§e尸兄状态: " + state + " §7(Shift+右键切换)"
        ));

        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    // ==================== 开门系统（高阶尸兄更聪明） ====================

    /**
     * 处理开门逻辑
     * 当尸兄路径中包含门时调用
     */
    public void setDoorToOpen(BlockState state, BlockPos pos, boolean open) {
        if (!this.canOpenDoors()) {
            return;
        }

        if (state.getBlock() instanceof DoorBlock doorBlock) {
            // 检查门是否已经是目标状态
            boolean isCurrentlyOpen = state.getValue(DoorBlock.OPEN);
            if (isCurrentlyOpen != open) {
                // 切换门的状态
                this.level().setBlock(pos, state.cycle(DoorBlock.OPEN), 10);
                
                // 播放开门/关门音效
                this.level().levelEvent(null, open ? 1005 : 1011, pos, 0);
                
                CorpseOrigin.LOGGER.debug("高阶尸兄（等级{}）{}了门 at {}", 
                    this.evolutionLevel, open ? "打开" : "关闭", pos);
            }
        }
    }

    /**
     * 在tick中检查并处理路径上的门
     * 高阶尸兄会自动打开路径上的门
     */
    private void tickDoorInteraction() {
        if (!this.canOpenDoors()) {
            return;
        }

        // 获取当前路径
        var path = this.getNavigation().getPath();
        if (path == null || path.isDone()) {
            return;
        }

        // 获取下一个路径节点
        var nextNode = path.getNextNode();
        if (nextNode == null) {
            return;
        }

        BlockPos pos = nextNode.asBlockPos();
        BlockState state = this.level().getBlockState(pos);

        // 检查是否是门
        if (state.getBlock() instanceof DoorBlock) {
            // 检查门是否关闭
            if (!state.getValue(DoorBlock.OPEN)) {
                // 计算距离
                double distance = this.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                
                // 距离门2格以内时开门
                if (distance < 4.0D) {
                    setDoorToOpen(state, pos, true);
                }
            }
        }
    }

    /**
     * 判断是否是鸟类或鸡
     */
    private static boolean isBirdOrChicken(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.animal.Chicken ||
                entity instanceof net.minecraft.world.entity.animal.Parrot;
    }

    /**
     * 判断是否是鱼类
     */
    private static boolean isFish(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.world.entity.animal.AbstractFish;
    }

    // ==================== 死亡掉落系统 ====================

    /**
     * 死亡时掉落器官
     * 尸兄死亡时有概率掉落各种器官
     */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        
        // 基础掉落概率 30%
        float baseDropChance = 0.30f;
        
        // 根据进化等级增加掉落概率
        float levelBonus = this.evolutionLevel * 0.05f;
        
        // 最终掉落概率
        float finalDropChance = baseDropChance + levelBonus;
        
        // 检查是否掉落
        if (this.random.nextFloat() < finalDropChance) {
            dropOrgan(level);
        }
    }
    
    /**
     * 掉落器官
     */
    private void dropOrgan(net.minecraft.server.level.ServerLevel level) {
        // 根据变种类型决定掉落什么器官
        Variant variant = getVariant();
        
        // 所有变种都有概率掉落普通尸眼
        if (this.random.nextFloat() < 0.6f) {
            // 掉落普通尸眼
            spawnAtLocation(new net.minecraft.world.item.ItemStack(Moditems.ORDINARY_ZB_EYE.get()), 0.0f);
            CorpseOrigin.LOGGER.debug("尸兄 {} 掉落了普通尸眼", this.getId());
        }
        
        // 根据变种额外掉落
        switch (variant) {
            case CRACKED -> {
                // 裂口变种有更高概率掉落眼睛
                if (this.random.nextFloat() < 0.3f) {
                    spawnAtLocation(new net.minecraft.world.item.ItemStack(Moditems.ORDINARY_ZB_EYE.get()), 0.0f);
                }
            }
            case WINGS -> {
                // 翅膀变种有概率掉落翅膀（未来实现）
                // spawnAtLocation(new net.minecraft.world.item.ItemStack(Moditems.ZB_WING.get()), 0.0f);
            }
            case WINGS_CRACKED -> {
                // 翅膀裂口变种既有裂口特性又有翅膀特性
                if (this.random.nextFloat() < 0.3f) {
                    spawnAtLocation(new net.minecraft.world.item.ItemStack(Moditems.ORDINARY_ZB_EYE.get()), 0.0f);
                }
                // spawnAtLocation(new net.minecraft.world.item.ItemStack(Moditems.ZB_WING.get()), 0.0f);
            }
            default -> {
                // 普通变种无额外掉落
            }
        }
        
        // 高等级尸兄有概率掉落多个器官
        if (this.evolutionLevel >= 3 && this.random.nextFloat() < 0.2f) {
            spawnAtLocation(new net.minecraft.world.item.ItemStack(Moditems.ORDINARY_ZB_EYE.get()), 0.0f);
        }
    }
}