package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinLoader;
import xiaoshi2022.corpseorigin.client.skin.ZbSkinState;
import xiaoshi2022.corpseorigin.item.weapon.BloodLotusLamp;

import java.util.Optional;

// 删除: import xiaoshi2022.corpseorigin.client.renderer.state.ZbEntityRenderState;

public class LowerLevelZbEntity extends PathfinderMob implements GeoEntity, ZombieKin {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ==================== 动画 ====================
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");

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
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 皮肤系统 ====================
    @Environment(EnvType.CLIENT)
    private Identifier skinTexture;
    private boolean skinLoadStarted = false;

    // ==================== 构造方法 ====================

    public LowerLevelZbEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
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
    }

    private PlayState movementController(AnimationTest<LowerLevelZbEntity> test) {
        if (test.isMoving()) {
            return test.setAndContinue(WALK_ANIM);
        }
        return test.setAndContinue(IDLE_ANIM);
    }

    private PlayState attackController(AnimationTest<LowerLevelZbEntity> test) {
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
            }

            float pitch = 0.8F + this.random.nextFloat() * 0.4F;
            this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, pitch);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.ITEM_SLIME,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        5, 0.3, 0.3, 0.3, 0.1
                );
            }
        }

        return result;
    }

    // ==================== AI 目标 ====================
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // ✅ 优先级0：被攻击后立刻反击（最高优先级）
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));

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
        builder.define(DATA_PLAYER_NAME, "");
        builder.define(DATA_CUSTOM_ID, "");
        builder.define(DATA_SKIN_STATE, ZbSkinState.NOT_LOADED.getCode());
        builder.define(DATA_EVOLUTION_LEVEL, 1);
        builder.define(DATA_HUNGER, 100);  // ✅ 默认满饥饿
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
        this.entityData.set(DATA_EVOLUTION_LEVEL, Math.max(1, Math.min(5, level)));
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
            // ✅ 每 200 tick（10秒）降低 1 点饥饿值
            if (this.tickCount % 200 == 0) {
                setHunger(getHunger() - 1);
            }

            // ✅ 极度饥饿时显示粒子效果
            if (this.isStarving() && this.tickCount % 20 == 0) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.ANGRY_VILLAGER,
                            this.getX(), this.getY() + 1.5, this.getZ(),
                            1, 0, 0, 0, 0
                    );
                }
            }
        }

        if (this.level().isClientSide()) {
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

    // ==================== NBT ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("PlayerSkinName", this.entityData.get(DATA_PLAYER_NAME));
        output.putString("CustomId", this.getCustomId());
        output.putInt("SkinState", this.entityData.get(DATA_SKIN_STATE));
        output.putInt("EvolutionLevel", this.getEvolutionLevel());
        output.putInt("Hunger", this.getHunger());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        Optional<String> skinNameOpt = input.getString("PlayerSkinName");
        skinNameOpt.ifPresent(name -> this.entityData.set(DATA_PLAYER_NAME, name));

        Optional<String> customIdOpt = input.getString("CustomId");
        customIdOpt.ifPresent(id -> this.setCustomId(id));

        Optional<Integer> skinStateOpt = input.getInt("SkinState");
        skinStateOpt.ifPresent(state -> this.entityData.set(DATA_SKIN_STATE, state));

        Optional<Integer> levelOpt = input.getInt("EvolutionLevel");
        levelOpt.ifPresent(this::setEvolutionLevel);
    }
}