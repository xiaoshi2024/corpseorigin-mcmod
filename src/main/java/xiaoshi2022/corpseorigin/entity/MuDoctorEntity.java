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
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.chapter.MuDoctorCombat;

/**
 * 穆博士（{@code mu_doctor}）—— 使用 {@code mu_doctor} geo 资源的双阶段 BOSS。
 * <p>
 * <b>一阶段（正常人形态）</b>：完全不近战。AI 只做三件事 —— 保持距离（风筝）→ 投针 → 换位。
 * 距离 &lt;8 格立刻后撤并补一针；8~20 格缓慢横向移动持续投针；&gt;20 格主动靠近到 15 格左右再投针。
 * 三根针（毒针 / 虚弱针 / 迟缓针）由 {@link MuDoctorCombat} 随机轮换，并按"是否已被减速 / 血量是否过半"
 * 做隐藏优先 —— 让玩家感觉"他在针对我"。
 * <p>
 * <b>二阶段（金属进化形态）</b>：一阶段血量降到 50% 时自己扎针（播放 {@code press} 动画，约 2 秒无敌），
 * 长出第三只眼（{@code haseye} 动画）、皮肤金属化 —— <b>刀枪不入</b>，但每次把玩家抛出去之后金属会
 * 软化 3 秒（唯一输出窗口，见 {@link #isDamageImmune()}）。不再投针，改为主动靠近到 4 格内抛投玩家
 * （像铁傀儡把人扔出去），抛完软化 → 玩家趁机输出 → 恢复 → 再追。
 * <p>
 * 服务端行为全部由 {@link MuDoctorCombat} 推（每个实体自己 tick 自己的状态机，零全局扫描）；
 * 本类只负责属性 / 同步数据 / AI 目标 / 阶段切换 / 无敌判定 / 动画控制器读同步数据切动画。
 */
public class MuDoctorEntity extends PathfinderMob implements GeoEntity {

    // ==================== 阶段 ====================

    public static final byte PHASE_NORMAL = 0;      // 一阶段：正常人形态
    public static final byte PHASE_TRANSITION = 1;  // 扎针中（无敌，不做任何行为）
    public static final byte PHASE_METAL = 2;       // 二阶段：金属进化形态

    /** 扎针动画（press）时长：约 2.2 秒，稍微留一点余量 */
    public static final int TRANSITION_TICKS = 45;

    // ==================== 动作（驱动 attack 控制器切动画） ====================

    public static final byte ACTION_IDLE = 0;
    public static final byte ACTION_NEEDLE = 1;   // 投针（think 起手）
    public static final byte ACTION_INJECT = 2;   // 扎自己（press）
    public static final byte ACTION_GRAB = 3;     // 抓取抛投（think 起手）

    // ==================== 同步数据 ====================

    private static final EntityDataAccessor<Byte> DATA_PHASE =
            SynchedEntityData.defineId(MuDoctorEntity.class, EntityDataSerializers.BYTE);
    /** 二阶段金属是否处于"软化"窗口（抛投后 3 秒内可被正常伤害） */
    private static final EntityDataAccessor<Boolean> DATA_METAL_SOFT =
            SynchedEntityData.defineId(MuDoctorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_ACTION =
            SynchedEntityData.defineId(MuDoctorEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_ACTION_TICKS =
            SynchedEntityData.defineId(MuDoctorEntity.class, EntityDataSerializers.INT);

    // ==================== 动画 ====================

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    /** 二阶段待机：第三只眼转动（haseye） */
    private static final RawAnimation EYE_ANIM = RawAnimation.begin().thenLoop("haseye");
    /** 投针 / 抓取起手（think 抬臂） */
    private static final RawAnimation GESTURE_ANIM = RawAnimation.begin().thenPlay("think");
    /** 扎自己（press，保持末帧） */
    private static final RawAnimation INJECT_ANIM = RawAnimation.begin().thenPlayAndHold("press");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 二阶段 modifier ID ====================

    private static final Identifier METAL_ARMOR_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "mu_doctor_metal_armor");
    private static final Identifier METAL_KB_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "mu_doctor_metal_kb");
    private static final Identifier METAL_SPEED_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "mu_doctor_metal_speed");

    private ServerBossEvent bossEvent;
    private int transitionTicks;

    public MuDoctorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        this.setPersistenceRequired();
    }

    // ==================== 属性 ====================

    /**
     * 一阶段面板：300 血、针伤基线 6、速度偏快（风筝流）、视力极好（48 格跟随）。
     * 二阶段的"刀枪不入 / 大力 / 变重"由 {@link #enterMetalPhase()} 动态挂 modifier 实现。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    // ==================== 同步数据 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, PHASE_NORMAL);
        builder.define(DATA_METAL_SOFT, false);
        builder.define(DATA_ACTION, ACTION_IDLE);
        builder.define(DATA_ACTION_TICKS, 0);
    }

    public byte getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public boolean isMetalSoft() {
        return this.entityData.get(DATA_METAL_SOFT);
    }

    public byte getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    public int getActionTicks() {
        return this.entityData.get(DATA_ACTION_TICKS);
    }

    public void setAction(byte action) {
        this.entityData.set(DATA_ACTION, action);
        this.entityData.set(DATA_ACTION_TICKS, 0);
    }

    public void incrementActionTicks() {
        this.entityData.set(DATA_ACTION_TICKS, this.entityData.get(DATA_ACTION_TICKS) + 1);
    }

    public void setMetalSoft(boolean soft) {
        this.entityData.set(DATA_METAL_SOFT, soft);
    }

    private void setPhase(byte phase) {
        this.entityData.set(DATA_PHASE, phase);
    }

    // ==================== 无敌判定 ====================

    /**
     * 是否免疫伤害：扎针过程中完全无敌；二阶段金属化期间刀枪不入，
     * <b>只有</b>抛投后软化（{@link #isMetalSoft()}，3 秒）这个窗口能被正常伤害。
     */
    public boolean isDamageImmune() {
        byte phase = getPhase();
        if (phase == PHASE_TRANSITION) return true;
        return phase == PHASE_METAL && !isMetalSoft();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isDamageImmune()) {
            // 金属碰撞反馈：让玩家明确知道"这一下没打动"
            this.playSound(SoundEvents.IRON_GOLEM_HURT, 0.8F, 1.4F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX(), this.getY(0.8D), this.getZ(), 8, 0.4D, 0.6D, 0.4D, 0.05D);
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    // ==================== GeckoLib 动画 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 5, this::movementController));
        controllers.add(new AnimationController<>("action", 0, this::actionController));
    }

    /** 移动动画：有动作时让位给 action 控制器；移动播 run；二阶段静止播 haseye；否则 idle */
    private PlayState movementController(AnimationTest<MuDoctorEntity> test) {
        if (this.getAction() != ACTION_IDLE) {
            return PlayState.CONTINUE;
        }
        if (this.getDeltaMovement().horizontalDistanceSqr() > 0.002D) {
            return test.setAndContinue(RUN_ANIM);
        }
        return test.setAndContinue(this.getPhase() == PHASE_METAL ? EYE_ANIM : IDLE_ANIM);
    }

    /** 动作动画：投针 / 抓取复用 think 起手，扎针用 press；动作结束 STOP 交回 movement */
    private PlayState actionController(AnimationTest<MuDoctorEntity> test) {
        return switch (this.getAction()) {
            case ACTION_NEEDLE, ACTION_GRAB -> test.setAndContinue(GESTURE_ANIM);
            case ACTION_INJECT -> test.setAndContinue(INJECT_ANIM);
            default -> PlayState.STOP;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== AI 目标 ====================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, false));
    }

    // ==================== 生命周期 ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();

        // BOSS 血条：lazy-init + 每 tick 同步进度
        if (this.bossEvent == null) {
            ServerBossEvent event = new ServerBossEvent(this.getUUID(),
                    Component.translatable("boss.corpseorigin.mu_doctor"),
                    BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            event.setProgress(1.0F);
            this.bossEvent = event;
        }
        this.bossEvent.setProgress(Math.max(0F, Math.min(1F,
                this.getHealth() / this.getMaxHealth())));

        // 扎针中：不做任何行为，倒计时结束进入金属形态
        if (this.getPhase() == PHASE_TRANSITION) {
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            if (++this.transitionTicks >= TRANSITION_TICKS) {
                enterMetalPhase(serverLevel);
            }
            return;
        }

        // 一阶段：血量降到 50% → 停手扎针进化
        if (this.getPhase() == PHASE_NORMAL && this.getHealth() > 0
                && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            beginTransition();
            return;
        }

        // 战斗状态机（自己 tick 自己，零全局扫描）
        MuDoctorCombat.tickBoss(serverLevel, this);
    }

    /** 开始扎针进化：清空移动、播放 press、进入无敌 */
    public void beginTransition() {
        setPhase(PHASE_TRANSITION);
        this.transitionTicks = 0;
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        setAction(ACTION_INJECT);
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2F, 0.9F);
    }

    /** 进入二阶段金属形态：挂上"刀枪不入 / 大力 / 变重"的 modifier + 金属化表现 */
    private void enterMetalPhase(ServerLevel level) {
        setPhase(PHASE_METAL);
        setAction(ACTION_IDLE);

        AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
        if (armor != null && armor.getModifier(METAL_ARMOR_ID) == null) {
            armor.addTransientModifier(new AttributeModifier(METAL_ARMOR_ID, 14.0D,
                    AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance kb = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null && kb.getModifier(METAL_KB_ID) == null) {
            kb.addTransientModifier(new AttributeModifier(METAL_KB_ID, 1.0D,
                    AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(METAL_SPEED_ID) == null) {
            // 金属太重：0.32 → 0.22，比一阶段慢
            speed.addTransientModifier(new AttributeModifier(METAL_SPEED_ID, -0.10D,
                    AttributeModifier.Operation.ADD_VALUE));
        }

        // 金属化表现：铁傀儡修复音效 + 电弧火花 + 一圈冲击
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                this.getX(), this.getY(1.0D), this.getZ(), 40, 0.6D, 1.0D, 0.6D, 0.15D);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                this.getX(), this.getY(2.0D), this.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.1D);
        this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.5F, 0.7F);
        this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.2F, 0.6F);
        CorpseOrigin.LOGGER.debug("MuDoctor entered metal phase at HP {}/{}",
                this.getHealth(), this.getMaxHealth());
    }

    // ==================== BOSS 血条 ====================

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.bossEvent != null) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (this.bossEvent != null) {
            this.bossEvent.removePlayer(player);
        }
    }

    // ==================== 死亡掉落 ====================

    /** 必定（100%）掉落「穆博士眼睛」{@link xiaoshi2022.corpseorigin.registry.ModItems#DR_MU_EYE} */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(level, new ItemStack(ModItems.DR_MU_EYE), 0.0F);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
            this.bossEvent = null;
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
            this.bossEvent = null;
        }
        if (!this.level().isClientSide()) {
            MuDoctorCombat.forget(this.getUUID());
        }
    }

    /** 让 combat 播放低沉音效这类表现方便（保持同一处音源设置） */
    public void feedbackSound(net.minecraft.sounds.SoundEvent event, float volume, float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), event, SoundSource.HOSTILE, volume, pitch);
    }
}