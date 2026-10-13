package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

/**
 * 寄生铁血虫尸兄（{@code gordius_zbrz}）—— 肚子里藏着铁线虫的尸兄鱼。
 * <p>
 * 模型是一条鱼（bigfish），体内盘着一条铁线虫（Gordius 骨骼组），4 种动画：
 * <ul>
 *   <li><b>idle</b>：铁线虫蜷缩躲进鱼肚子的姿态（静止时的默认姿态）</li>
 *   <li><b>show</b>：出现时的动画——鱼鳃收拢、铁线虫从鱼腹里撑出来（出现时播一遍）</li>
 *   <li><b>wizened</b>：被龙右吸干时动画——虫体干瘪收缩（吸食期间循环保持）</li>
 *   <li><b>swing</b>：游泳动画——鱼鳍鱼尾摆动，根据 idle / show 的状态配合判断触发：
 *       仅在普通状态（非 show / wizened）且在水中游动时播放</li>
 * </ul>
 * 龙右（尸王形态玩家）主手右键可以把它吸干：虫体干瘪、血量被抽走，玩家回饥饿回血。
 */
public class GordiusZbrzEntity extends AbstractFish implements GeoEntity, ZombieKin {

    // ==================== 状态同步 ====================

    /** 普通状态：idle / swing 按游动切换 */
    public static final byte STATE_NORMAL = 0;
    /** 出现中：show 动画独占播放 */
    public static final byte STATE_SHOW = 1;
    /** 被吸干中：wizened 动画 */
    public static final byte STATE_WIZENED = 2;

    private static final EntityDataAccessor<Byte> DATA_STATE =
            SynchedEntityData.defineId(GordiusZbrzEntity.class, EntityDataSerializers.BYTE);

    // ==================== 动画 ====================

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SHOW = RawAnimation.begin().thenPlay("show");
    private static final RawAnimation SWING = RawAnimation.begin().thenLoop("swing");
    private static final RawAnimation WIZENED = RawAnimation.begin().thenPlayAndHold("wizened");

    /** show 动画时长（4.89s，取整用 tick 计） */
    private static final int SHOW_TICKS = 98;
    /** 被吸干时长（wizened 动画 3s） */
    public static final int DRAIN_TICKS = 60;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int showTicks;
    private int drainTicks;

    public GordiusZbrzEntity(EntityType<? extends AbstractFish> type, Level level) {
        super(type, level);
        moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, .1f, .02f, false);
    }

    // ==================== 属性 ====================

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractFish.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8)
                .add(Attributes.MOVEMENT_SPEED, .5)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, STATE_NORMAL);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(3, new RandomSwimmingGoal(this, 1, 40));
    }

    // ==================== 状态机 ====================

    public byte getState() { return entityData.get(DATA_STATE); }

    /** 播「出现」动画（服务端调用，自动同步） */
    public void playShow() {
        if (getState() != STATE_WIZENED) {
            entityData.set(DATA_STATE, STATE_SHOW);
            showTicks = SHOW_TICKS;
        }
    }

    /** 被龙右吸干：进入 wizened 状态，DRAIN_TICKS 内血量被抽干后死亡 */
    public void beginDrain() {
        if (!isAlive() || getState() == STATE_WIZENED) return;
        entityData.set(DATA_STATE, STATE_WIZENED);
        drainTicks = DRAIN_TICKS;
        getNavigation().stop();
        setDeltaMovement(0, 0, 0);
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            // 出现时播 show（/summon、刷怪蛋、自然生成统一走这里）
            if (tickCount == 1) playShow();
            if (getState() == STATE_SHOW && --showTicks <= 0) {
                entityData.set(DATA_STATE, STATE_NORMAL);
            }
            if (getState() == STATE_WIZENED) {
                getNavigation().stop();
                setDeltaMovement(0, getDeltaMovement().y * .5, 0);
                // 血量匀速被抽干，动画播完正好死透
                setHealth(Math.max(0f, getHealth() - getMaxHealth() / (float) DRAIN_TICKS));
                if (--drainTicks <= 0 || getHealth() <= 0) {
                    die(damageSources().generic());
                }
            }
        }
    }

    // ==================== 龙右吸食 ====================

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (PlayerCorpseComponent.isCorpse(player)
                && PlayerCorpseComponent.get(player).getCorpseType() == PlayerCorpseComponent.TYPE_KING
                && hand == InteractionHand.MAIN_HAND) {
            if (!level().isClientSide()) {
                beginDrain();
                player.getFoodData().eat(4, 0.5F);
                player.heal(3.0F);
                playSound(SoundEvents.GENERIC_DRINK.value(), .6f, .8f);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                            getX(), getY() + getBbHeight() * .5, getZ(), 8, .3, .3, .3, .02);
                }
            }
            return level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    // ==================== 水生基础（同 ZbrFish：不被空桶删掉） ====================

    @Override protected SoundEvent getFlopSound() { return SoundEvents.COD_FLOP; }
    @Override public ItemStack getBucketItemStack() { return ItemStack.EMPTY; }

    // ==================== 动画控制器 ====================

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("fish", 4, this::fish));
        controllers.add(new AnimationController<>("worm", 4, this::worm));
    }

    /** 鱼体层：游泳动画只在普通状态且水中游动时播（show / wizened 期间鱼体保持静止） */
    private PlayState fish(AnimationTest<GordiusZbrzEntity> t) {
        if (getState() != STATE_NORMAL) return PlayState.STOP;
        return isInWater() && t.isMoving() ? t.setAndContinue(SWING) : PlayState.STOP;
    }

    /** 虫体层：wizened > show > idle（蜷缩在鱼肚子里） */
    private PlayState worm(AnimationTest<GordiusZbrzEntity> t) {
        if (getState() == STATE_WIZENED) return t.setAndContinue(WIZENED);
        if (getState() == STATE_SHOW) return t.setAndContinue(SHOW);
        return t.setAndContinue(IDLE);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ==================== 存档 ====================

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        byte state = getState();
        out.putBoolean("Wizened", state == STATE_WIZENED);
        out.putInt("WizenedTicks", drainTicks);
    }

    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        if (in.getBooleanOr("Wizened", false)) {
            entityData.set(DATA_STATE, STATE_WIZENED);
            drainTicks = Math.max(1, in.getIntOr("WizenedTicks", DRAIN_TICKS));
        }
    }
}
