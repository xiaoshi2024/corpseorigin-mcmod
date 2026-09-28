package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinLoader;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.entity.evolution.ZbEvolution;
import xiaoshi2022.corpseorigin.entity.evolution.ZbOrganGrowth;
import xiaoshi2022.corpseorigin.registry.ModSounds;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

import java.util.Optional;

// 删除: import xiaoshi2022.corpseorigin.client.renderer.state.ZbEntityRenderState;

public class LowerLevelZbEntity extends PathfinderMob implements GeoEntity, ZombieKin {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ==================== 动画 ====================
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation GNAW_ANIM = RawAnimation.begin().thenLoop("gnaw");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlayAndHold("death_burst");
    private static final EntityDataAccessor<Boolean> DATA_CORPSE_EYE =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.BOOLEAN);
    public boolean hasCorpseEye(){return entityData.get(DATA_CORPSE_EYE);}
    public void setCorpseEye(boolean visible){entityData.set(DATA_CORPSE_EYE,visible);}
    private static final EntityDataAccessor<Boolean> DATA_CRACKED =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.BOOLEAN);
    public boolean isCracked(){return entityData.get(DATA_CRACKED);}
    public void setCracked(boolean cracked){entityData.set(DATA_CRACKED,cracked);}
    private static final EntityDataAccessor<Boolean> DATA_RIBS_VISIBLE =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.BOOLEAN);
    public boolean hasVisibleRibs(){return entityData.get(DATA_RIBS_VISIBLE);}

    private static final EntityDataAccessor<Integer> DATA_GRAPPLE_TARGET =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HORROR_ACTIVE =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.BOOLEAN);
    public boolean horrorWormsReleased;
    private int lastMovingAnimationTick = Integer.MIN_VALUE;

    public int getGrappleTarget() { return entityData.get(DATA_GRAPPLE_TARGET); }
    public void setGrappleTarget(int id) { entityData.set(DATA_GRAPPLE_TARGET, id); }
    public boolean isHorrorActive() { return entityData.get(DATA_HORROR_ACTIVE); }

    // ==================== 同步数据 ====================
    private static final EntityDataAccessor<String> DATA_PLAYER_NAME =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SKIN_STATE =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EVOLUTION_LEVEL =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_CUSTOM_ID =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.STRING);
    // ==================== 饥饿值 ====================
    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.INT);
    // ==================== 突变器官配置（客户端渲染要读，走同步） ====================
    private static final EntityDataAccessor<String> DATA_ORGAN_LOADOUT =
            SynchedEntityData.defineId(LowerLevelZbEntity.class, EntityDataSerializers.STRING);

    /** 饥饿阈值：低于这个值才攻击同类 */
    private static final int HUNGER_THRESHOLD = 30;
    /** 每次攻击同类消耗的饥饿值 */
    private static final int ATTACK_SAME_KIN_HUNGER_COST = 15;

    public int getHunger() {
        return this.entityData.get(DATA_HUNGER);
    }

    public void setHunger(int hunger) {
        this.entityData.set(DATA_HUNGER, Math.max(0, Math.min(100, hunger)));
    }

    public boolean isHungry() {
        return getHunger() < HUNGER_THRESHOLD;
    }

    public boolean isStarving() {
        return getHunger() <= 0;
    }

    // ==================== GeckoLib ====================
    private final AnimatableInstanceCache cache = new xiaoshi2022.corpseorigin.entity.animation.ZbLayerAnimationCache(this);

    // ==================== 皮肤系统 ====================
    @Environment(EnvType.CLIENT)
    private Identifier skinTexture;
    private boolean skinLoadStarted = false;

    // ==================== 构造方法 ====================

    public LowerLevelZbEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
        if(!level.isClientSide())setCorpseEye(this.random.nextFloat()<.2f);
        if(!level.isClientSide())setCracked(this.random.nextFloat()<.3f);
        this.setCanPickUpLoot(true);   // ✅ 开启捡东西
    }

    public LowerLevelZbEntity(EntityType<? extends PathfinderMob> entityType, Level level, Player player) {
        this(entityType, level);
        if (player != null) {
            this.entityData.set(DATA_PLAYER_NAME, player.getName().getString());
            LOGGER.debug("尸兄感染玩家: {}", player.getName().getString());
        }
    }

    // ==================== 属性 ====================

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    // ==================== GeoEntity 接口实现 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 5, this::movementController));
        controllers.add(new AnimationController<>("attack", 2, this::attackController));
        // 挂在身上的突变器官各用自己的渲染状态取 clip（本体管理器读不到 clip，CONTINUE 即什么都不播）
        controllers.add(new AnimationController<LowerLevelZbEntity>("custom_organs", 4, test -> {
            String clip = test.getData(xiaoshi2022.corpseorigin.entity.animation.ZbLayerAnimationCache.CLIP);
            return clip == null ? PlayState.STOP : test.setAndContinue(RawAnimation.begin().thenLoop(clip));
        }));
    }

    private PlayState movementController(AnimationTest<LowerLevelZbEntity> test) {
        // Accessory managers must not also animate the host's body clips.
        if (test.getData(xiaoshi2022.corpseorigin.entity.animation.ZbLayerAnimationCache.CLIP) != null) return PlayState.STOP;
        if (isDeadOrDying() && xiaoshi2022.corpseorigin.growth.CorpseHorror.applies(this)) return test.setAndContinue(DEATH_ANIM);
        if (getGrappleTarget() >= 0) return test.setAndContinue(GNAW_ANIM);
        // A short grace period avoids repeatedly restarting idle on tiny movement fluctuations.
        if (test.isMoving()) lastMovingAnimationTick = tickCount;
        if (lastMovingAnimationTick != Integer.MIN_VALUE && tickCount - lastMovingAnimationTick <= 3) {
            return test.setAndContinue(WALK_ANIM);
        }
        return test.setAndContinue(IDLE_ANIM);
    }

    private PlayState attackController(AnimationTest<LowerLevelZbEntity> test) {
        if (test.getData(xiaoshi2022.corpseorigin.entity.animation.ZbLayerAnimationCache.CLIP) != null
                || isDeadOrDying() || getGrappleTarget() >= 0) return PlayState.STOP;
        if (this.swinging) {
            return test.setAndContinue(ATTACK_ANIM);
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== 攻击 ====================

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        // ✅ 检查是否可以攻击
        if (!ZombieKin.canAttack(this, target)) {
            return false;
        }

        boolean result = super.doHurtTarget(level, target);

        if (result && !this.level().isClientSide()) {
            // ✅ 只要是活体就进食
            if (target instanceof LivingEntity) {
                int hungerGain;

                if (ZombieKin.isZombieKin(target)) {
                    // 同类相食：回复少一点
                    hungerGain = 10;
                    LOGGER.debug("尸兄 {} 吞噬同类，回复 {} 饥饿值", this.getId(), hungerGain);
                } else {
                    // 正常食物（人类/动物）
                    hungerGain = 20;
                }

                setHunger(getHunger() + hungerGain);

                // 吸血器官：命中回血（与玩家 vampire 口径一致，尸兄不消耗血能）
                if (xiaoshi2022.corpseorigin.entity.evolution.ZbOrganEffects.hasTrait(this, "vampire")) {
                    this.heal(2.0F);
                }
            }

            float pitch = 0.8F + this.random.nextFloat() * 0.4F;
            this.playSound(ModSounds.GROUND_CHI, 1.0F, pitch);

            if (level instanceof ServerLevel serverLevel) {
                QiEffects.burst(
                        serverLevel,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        0x8ce06a, 5, 0.3
                );
            }
        }

        return result;
    }

    // ==================== AI 目标 ====================
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new xiaoshi2022.corpseorigin.entity.ai.CorpseGrappleGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // ✅ 优先级0：被攻击后立刻反击（最高优先级）
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.npc.villager.AbstractVillager.class,10,true,false,
                (target, level)->ZombieKin.canAttack(this,target)&&!this.isAlliedTo(target)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this,
                UncleEntity.class,12,true,false,
                (target, level)->ZombieKin.canAttack(this,target)&&!this.isAlliedTo(target)));

        // ✅ 优先级1：攻击非尸族玩家（永远）
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(
                this,
                Player.class,
                10,
                true,
                false,
                (target, level) -> ZombieKin.isNotZombieKin(target)
        ));

        // ✅ 优先级2：饥饿时攻击尸族玩家（同类相食）
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(
                this,
                Player.class,
                10,
                true,
                false,
                (target, level) -> {
                    if (!this.isHungry()) return false;
                    if (!ZombieKin.isZombieKin(target)) return false;
                    return target != this;
                }
        ));

        // ✅ 优先级3：饥饿时攻击其他尸兄生物
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<LowerLevelZbEntity>(
                this,
                LowerLevelZbEntity.class,
                10,
                true,
                false,
                (target, level) -> {
                    if (!this.isHungry()) return false;

                    // ✅ 先判断类型，再调用 LowerLevelZbEntity 的方法
                    if (!(target instanceof LowerLevelZbEntity other)) return false;
                    if (other == this) return false;

                    return other.getEvolutionLevel() < this.getEvolutionLevel() || this.isStarving();
                }
        ));
    }

    // ==================== 数据同步 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CORPSE_EYE, false);
        builder.define(DATA_CRACKED, false);
        builder.define(DATA_RIBS_VISIBLE, false);
        builder.define(DATA_PLAYER_NAME, "");
        builder.define(DATA_CUSTOM_ID, "");
        builder.define(DATA_SKIN_STATE, ZbSkinState.NOT_LOADED.getCode());
        builder.define(DATA_EVOLUTION_LEVEL, 1);
        builder.define(DATA_HUNGER, 100);  // ✅ 默认满饥饿
        builder.define(DATA_ORGAN_LOADOUT, "[]");
        builder.define(DATA_GRAPPLE_TARGET, -1);
        builder.define(DATA_HORROR_ACTIVE, false);
    }

    // ==================== 血肉能量 / 临界突破（仅服务端，走 NBT） ====================

    private int fleshEnergy;
    private int breakthroughFailures;

    public int getFleshEnergy() {
        return this.fleshEnergy;
    }

    public void setFleshEnergy(int value) {
        this.fleshEnergy = Math.max(0, value);
    }

    public int getBreakthroughFailures() {
        return this.breakthroughFailures;
    }

    public void setBreakthroughFailures(int value) {
        this.breakthroughFailures = Math.max(0, value);
    }

    // ==================== 突变器官配置 ====================

    public String getOrganLoadout() {
        return this.entityData.get(DATA_ORGAN_LOADOUT);
    }

    public void setOrganLoadout(String json) {
        this.entityData.set(DATA_ORGAN_LOADOUT, json == null ? "[]" : json);
    }

    // ==================== 自定义 ID 系统 ====================

    public String getCustomId() {
        return this.entityData.get(DATA_CUSTOM_ID);
    }

    public void setCustomId(String customId) {
        this.entityData.set(DATA_CUSTOM_ID, customId != null ? customId : "");
    }

    // ==================== 进化系统 ====================

    public int getEvolutionLevel() {
        return this.entityData.get(DATA_EVOLUTION_LEVEL);
    }

    public void setEvolutionLevel(int level) {
        // 1-5 为普通进化；6-10 为吸食血肉超脱临界后的等级
        this.entityData.set(DATA_EVOLUTION_LEVEL, Math.max(1, Math.min(xiaoshi2022.corpseorigin.entity.evolution.ZbEvolution.MAX_LEVEL, level)));
        updateAttributesForEvolution();
    }

    protected void updateAttributesForEvolution() {
        int level = getEvolutionLevel();
        if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0 + level * 5.0);
        }
        if (this.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.0 + level * 1.5);
        }
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25 + level * 0.03);
        }
        if (this.getAttribute(Attributes.ARMOR) != null) {
            this.getAttribute(Attributes.ARMOR).setBaseValue(2.0 + level * 1.0);
        }
        this.setHealth(this.getMaxHealth());
    }

    // ==================== 玩家皮肤系统 ====================

    public String getPlayerSkinName() {
        return this.entityData.get(DATA_PLAYER_NAME);
    }

    public void setPlayerSkinName(String playerName) {
        this.entityData.set(DATA_PLAYER_NAME, playerName != null ? playerName : "");
    }

    /**
     * 这个尸兄是否"吃名字皮肤"—— 即要不要按 {@link #getPlayerSkinName()} 去查同名玩家的皮肤。
     * <p>
     * 凹凸曼是个例外（它用固定贴图，见 {@code AotumanZbRenderer}），所以会覆写成 {@code false}，
     * 免得白跑一遍皮肤查询。
     */
    protected boolean usesNamedSkin() {
        return true;
    }

    /**
     * 自然生成时补一个随机的"玩家 ID"。
     * <p>
     * 野外刷出来的尸兄本来是"无主"的（名字为空 → 一律默认皮肤），这里给它们随机组合一个像玩家的名字，
     * 客户端就能拿它去查同名玩家的皮肤，从而让每一只尸兄长得都不一样（查不到就退回默认皮肤）。
     * <p>
     * 只认 {@link EntitySpawnReason#NATURAL}：刷怪蛋、{@code /summon}、玩家被感染这些
     * 已经有明确来源的场景不去覆盖人家给的名字。
     */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData spawnGroupData) {
        // Keep every ordinary spawn path consistent with /summonzb. Explicitly assigned
        // skins are preserved, while spawn eggs and /summon receive a random skin too.
        if (usesNamedSkin() && getPlayerSkinName().isEmpty()) {
            setPlayerSkinName(ZbNameGenerator.random(this.getRandom()));
        }
        // 自然生成：按游戏日掷进化等级（越后期越强，见 ZbEvolution.rollSpawnLevel）
        if (reason == EntitySpawnReason.NATURAL && rollsSpawnEvolution()) {
            rollSpawnEvolution(level);
        }
        return super.finalizeSpawn(level, difficulty, reason, spawnGroupData);
    }

    /**
     * 自然生成时是否按"游戏日"掷进化等级（见 {@link #rollSpawnEvolution}）。
     * <p>
     * 固定强度的子类会覆写成 {@code false}：它们的属性不随等级走
     * （见 {@code AotumanZbEntity#updateAttributesForEvolution}），渲染器也不带器官层，
     * 掷出高阶只会得到"看不见的器官 + 不变的强度"。
     */
    protected boolean rollsSpawnEvolution() {
        return true;
    }

    /**
     * 自然生成的变异尸兄：等级按"越后期越强"掷（{@link ZbEvolution#rollSpawnLevel}），
     * 越过临界（地2 = 6 级）的顺带带上相应数量的突变器官 ——
     * 和靠吃血肉突破上来的尸兄同源，不是一个独立的强化表。
     */
    private void rollSpawnEvolution(ServerLevelAccessor level) {
        // Use the calendar day: sleeping advances dayTime, while gameTime only
        // advances by the few ticks spent sleeping.
        int evolutionLevel = ZbEvolution.rollSpawnLevel(this.getRandom(),
                xiaoshi2022.corpseorigin.util.WorldCalendar.dayTime(level.getLevel()));
        if (evolutionLevel <= 1) {
            return;
        }
        setEvolutionLevel(evolutionLevel);
        for (int i = 0; i < evolutionLevel - ZbEvolution.BREAKTHROUGH_LEVEL; i++) {
            if (!ZbOrganGrowth.tryMutateOrgan(this)) {
                break;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public Identifier getSkinTexture() {
        return this.skinTexture;
    }

    @Environment(EnvType.CLIENT)
    public void setSkinTexture(Identifier texture) {
        this.skinTexture = texture;
    }

    public ZbSkinState getSkinState() {
        return ZbSkinState.fromCode(this.entityData.get(DATA_SKIN_STATE));
    }

    @Environment(EnvType.CLIENT)
    public void setSkinState(ZbSkinState state) {
        this.entityData.set(DATA_SKIN_STATE, state.getCode());
    }

    public void setSkinTextureFromServer(Identifier texture) {
        // 服务端存储纹理路径 - 实际渲染在客户端
    }

    public void setSkinStateFromServer(ZbSkinState state) {
        this.entityData.set(DATA_SKIN_STATE, state.getCode());
    }

    // ==================== Tick ====================

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            entityData.set(DATA_HORROR_ACTIVE, xiaoshi2022.corpseorigin.growth.CorpseHorror.applies(this));
            entityData.set(DATA_RIBS_VISIBLE, xiaoshi2022.corpseorigin.growth.CorpseHorror.ribsVisible(this));
            xiaoshi2022.corpseorigin.growth.CorpseHorror.tick(this);
            // ✅ 每 200 tick（10秒）降低 1 点饥饿值
            if (this.tickCount % 200 == 0) {
                setHunger(getHunger() - 1);
            }

            // 突变器官效果（解剖属性 / 夜视 / 翅膀缓落）
            xiaoshi2022.corpseorigin.entity.evolution.ZbOrganEffects.tick(this);

            // ✅ 极度饥饿时显示粒子效果
            if (this.isStarving() && this.tickCount % 20 == 0) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    QiEffects.burst(
                            serverLevel,
                            this.getX(), this.getY() + 1.5, this.getZ(),
                            0xc0182a, 1, 0
                    );
                }
            }
        }

        if (this.level().isClientSide()) {
            if (this.tickCount % (90 + Math.floorMod(this.getId(),30)) == 0 && this.isAlive() && this.getTarget() == null)
                this.playSound(xiaoshi2022.corpseorigin.registry.ModSounds.CORPSE_BREATH,.28f,.72f+this.random.nextFloat()*.12f);
            tickClient();
        }
    }

    @Environment(EnvType.CLIENT)
    private void tickClient() {
        String name = this.entityData.get(DATA_PLAYER_NAME);
        int stateCode = this.entityData.get(DATA_SKIN_STATE);
        ZbSkinState currentState = ZbSkinState.fromCode(stateCode);

        if (name != null && !name.isEmpty()) {
            if (!skinLoadStarted && (currentState == ZbSkinState.NOT_LOADED || currentState == ZbSkinState.FAILED)) {
                startSkinLoad(name);
            } else if (currentState == ZbSkinState.LOADED && this.skinTexture == null) {
                startSkinLoad(name);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private void startSkinLoad(String playerName) {
        skinLoadStarted = true;
        this.entityData.set(DATA_SKIN_STATE, ZbSkinState.LOADING.getCode());
        ZbSkinLoader.loadSkinAsync(this, playerName);
    }

    // ==================== 器官：水下呼吸 / 摔落免疫 ====================

    @Override
    public boolean canBreatheUnderwater() {
        // 长了腮就能在水下正常呼吸，不溺水
        return super.canBreatheUnderwater()
                || xiaoshi2022.corpseorigin.entity.evolution.ZbOrganEffects.hasTrait(this, "gills");
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        // 长了翅膀：免疫摔落伤害（配合空中缓落）
        if (xiaoshi2022.corpseorigin.entity.evolution.ZbOrganEffects.hasTrait(this, "wings")) {
            return false;
        }
        return super.causeFallDamage(distance, multiplier, source);
    }

    // ==================== NBT ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("HasCorpseEye",hasCorpseEye());
        output.putBoolean("CrackedAppearance",isCracked());
        output.putString("PlayerSkinName", this.entityData.get(DATA_PLAYER_NAME));
        output.putString("CustomId", this.getCustomId());
        output.putInt("SkinState", this.entityData.get(DATA_SKIN_STATE));
        output.putInt("EvolutionLevel", this.getEvolutionLevel());
        output.putInt("Hunger", this.getHunger());
        output.putInt("FleshEnergy", this.fleshEnergy);
        output.putInt("BreakthroughFailures", this.breakthroughFailures);
        output.putString("OrganLoadout", this.getOrganLoadout());
        output.putBoolean("HorrorWormsReleased", horrorWormsReleased);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // Old saves get one stable UUID-based roll; new saves retain the explicit choice.
        setCorpseEye(input.getBooleanOr("HasCorpseEye",Math.floorMod(getUUID().hashCode(),5)==0));
        setCracked(input.getBooleanOr("CrackedAppearance",Math.floorMod(Long.hashCode(getUUID().getMostSignificantBits()),10)<3));

        Optional<String> skinNameOpt = input.getString("PlayerSkinName");
        skinNameOpt.ifPresent(name -> this.entityData.set(DATA_PLAYER_NAME, name));

        Optional<String> customIdOpt = input.getString("CustomId");
        customIdOpt.ifPresent(id -> this.setCustomId(id));

        Optional<Integer> skinStateOpt = input.getInt("SkinState");
        skinStateOpt.ifPresent(state -> this.entityData.set(DATA_SKIN_STATE, state));

        Optional<Integer> levelOpt = input.getInt("EvolutionLevel");
        levelOpt.ifPresent(this::setEvolutionLevel);

        this.fleshEnergy = input.getIntOr("FleshEnergy", 0);
        this.breakthroughFailures = input.getIntOr("BreakthroughFailures", 0);
        this.setOrganLoadout(input.getStringOr("OrganLoadout", "[]"));
        this.horrorWormsReleased = input.getBooleanOr("HorrorWormsReleased", false);
    }
}
