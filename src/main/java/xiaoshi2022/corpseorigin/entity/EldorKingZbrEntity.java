package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.chapter.EldorKingZbrCombat;

import java.util.EnumSet;

/**
 * 尸兄·尔多兽王（{@code eldor_king_zbr}）—— 多尔兽王尸化后的 BOSS 形态。
 * <p>
 * 原著设定：尔多兽王「天崩地裂 / 兽王重拳」的二阶段被尸兄化放大为
 * 「震地波 / 獠牙撕咬 / 无喘息连击」。失去理智后只剩杀意，二阶段（红毛遍布头颈）
 * 攻速翻倍、伤害 +30%，按 Sans 式压迫感设计 —— 玩家不能靠回合制节奏苟，
 * 必须靠位移躲震地波、靠断食抗压。
 * <p>
 * 服务端：{@link EldorKingZbrCombat} 推攻击状态机；本类只负责
 * 属性 / 同步数据 / AI 目标 / 二阶段触发 / 动画控制器读同步数据切动画。
 */
public class EldorKingZbrEntity extends PathfinderMob implements GeoEntity, ZombieKin {

    // ==================== 同步数据 ====================

    /** 二阶段（狂暴）：客户端按它切红毛贴图，服务端按它加倍攻速恢复期 */
    private static final EntityDataAccessor<Boolean> DATA_BERSERK =
            SynchedEntityData.defineId(EldorKingZbrEntity.class, EntityDataSerializers.BOOLEAN);
    /** 当前攻击状态：0=IDLE, 1=BITE, 2=SLAM（与 EldorKingZbrCombat.AttackState 对齐） */
    private static final EntityDataAccessor<Byte> DATA_ATTACK_STATE =
            SynchedEntityData.defineId(EldorKingZbrEntity.class, EntityDataSerializers.BYTE);
    /** 攻击已进行 tick 数（客户端按它定位 hit frame 同步表现） */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TICKS =
            SynchedEntityData.defineId(EldorKingZbrEntity.class, EntityDataSerializers.INT);

    // ==================== 动画 ====================

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    /** 獠牙撕咬：命中帧由 {@link EldorKingZbrCombat#BITE_HIT_TICK} 调度 */
    private static final RawAnimation BITE_ANIM = RawAnimation.begin().thenPlay("attack");
    /** 震地波：落地帧由 {@link EldorKingZbrCombat#SLAM_HIT_TICK} 调度 */
    private static final RawAnimation SLAM_ANIM = RawAnimation.begin().thenPlay("hammer");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 二阶段 modifier ID（用 Identifier 而非 UUID：26.2 AttributeModifier 是 record(Identifier, double, Operation)） ====================

    /** 二阶段速度加成 modifier（稳定 Identifier 便于清除 / 重挂） */
    private static final Identifier BERSERK_SPEED_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "eldor_zbr_berserk_speed");
    /** 二阶段伤害加成 modifier */
    private static final Identifier BERSERK_DAMAGE_ID =
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "eldor_zbr_berserk_damage");

    /** BOSS 血条（参考 {@link xiaoshi2022.corpseorigin.entity.CloneAvatarEntity} 的写法） */
    private ServerBossEvent bossEvent;

    public EldorKingZbrEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 35;
        this.setPersistenceRequired();
    }

    // ==================== 属性 ====================

    /** BOSS 面板：600 血（Asterion minotaur 720 调降）、24 伤基线、高抗、大跟随范围 */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D)
                .add(Attributes.SCALE, 1.15D);
    }

    // ==================== 同步数据 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // ⚠️ super 必须有：LivingEntity / Mob 各自声明同步字段（血量、AI 标志），漏掉实体造不出来
        super.defineSynchedData(builder);
        builder.define(DATA_BERSERK, false);
        builder.define(DATA_ATTACK_STATE, (byte) 0);
        builder.define(DATA_ATTACK_TICKS, 0);
    }

    public boolean isBerserk() {
        return this.entityData.get(DATA_BERSERK);
    }

    /** 让 {@link EldorKingZbrCombat} 推状态机时调用：设置当前攻击态 + 重置 tick 计数 */
    public void setAttackState(int state) {
        this.entityData.set(DATA_ATTACK_STATE, (byte) state);
        this.entityData.set(DATA_ATTACK_TICKS, 0);
    }

    /** 每服务端 tick 增量同步攻击 tick 数（客户端按它播命中表现） */
    public void incrementAttackTicks() {
        int t = this.entityData.get(DATA_ATTACK_TICKS);
        this.entityData.set(DATA_ATTACK_TICKS, t + 1);
    }

    public int getAttackState() {
        return this.entityData.get(DATA_ATTACK_STATE);
    }

    public int getAttackTicks() {
        return this.entityData.get(DATA_ATTACK_TICKS);
    }

    // ==================== GeckoLib ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 5, this::movementController));
        controllers.add(new AnimationController<>("attack", 0, this::attackController));
    }

    /** 移动动画：跑动播 run，静止播 idle（这套模型没有 walk） */
    private PlayState movementController(AnimationTest<EldorKingZbrEntity> test) {
        if (this.getAttackState() != 0) {
            // 攻击中不切移动动画，避免与 attack 控制器抢骨骼
            return PlayState.CONTINUE;
        }
        return test.setAndContinue(this.getDeltaMovement().horizontalDistanceSqr() > 0.002D ? RUN_ANIM : IDLE_ANIM);
    }

    /** 攻击动画：读同步数据切 bite / slam，攻击结束自动 STOP 让 movement 接管 */
    private PlayState attackController(AnimationTest<EldorKingZbrEntity> test) {
        int state = this.getAttackState();
        if (state == 1) {
            return test.setAndContinue(BITE_ANIM);
        }
        if (state == 2) {
            return test.setAndContinue(SLAM_ANIM);
        }
        // IDLE：返回 STOP，让 movement 控制器接管（攻击态切回时 lambda 会重新切动画）
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== AI 目标 ====================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 攻击调度由 EldorKingZbrCombat 推（基于 entity 同步数据切动画 + 命中帧伤害），
        // 这里挂一个轻量 Goal 把"目标在范围内"作为可攻击判定
        this.goalSelector.addGoal(1, new BossApproachGoal());
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, false));
    }

    /** 接近目标直到进入攻击范围（攻击动作由 {@link EldorKingZbrCombat} 调度） */
    private class BossApproachGoal extends Goal {
        private int recalcCooldown;

        BossApproachGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() != null && getTarget().isAlive()
                    && distanceToSqr(getTarget()) > 2.5D * 2.5D;
        }

        @Override
        public void tick() {
            if (getTarget() == null) return;
            if (--this.recalcCooldown <= 0) {
                this.recalcCooldown = 10;
                getNavigation().moveTo(getTarget(), isBerserk() ? 1.5D : 1.15D);
            }
        }
    }

    // ==================== ZombieKin ====================

    @Override
    public boolean isTrueZombieKin() {
        return true;
    }

    @Override
    public boolean isHungry() {
        return true;   // 尸兄 BOSS 永远饥饿
    }

    // ==================== 生命周期：二阶段触发 + BOSS 血条 ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        // 二阶段触发：< 40% 血量时翻 BERSERK 并挂 modifier
        if (!this.isBerserk() && this.getHealth() > 0
                && this.getHealth() < this.getMaxHealth() * 0.4D) {
            enterBerserk();
        }
        // BOSS 自己推进自己的攻击状态机 —— 不用全局扫描！
        // 每个实体 tick 只处理自己的战斗逻辑，零 spatial hash 开销
        EldorKingZbrCombat.tickBoss((ServerLevel) this.level(), this);
        // BOSS 血条：lazy-init + 进度同步（每 tick 都推一次，确保跟实际 HP 一致）
        if (this.bossEvent == null) {
            ServerBossEvent event = new ServerBossEvent(this.getUUID(),
                    Component.translatable("boss.corpseorigin.eldor_king_zbr"),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
            event.setProgress(Math.max(0F, Math.min(1F,
                    this.getHealth() / this.getMaxHealth())));
            this.bossEvent = event;
        }
        this.bossEvent.setProgress(Math.max(0F, Math.min(1F,
                this.getHealth() / this.getMaxHealth())));
    }

    /** 玩家进入视野 → 加入 BOSS 血条 */
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.bossEvent != null) {
            this.bossEvent.addPlayer(player);
        }
    }

    /** 玩家离开视野 / 切维度 → 移除 BOSS 血条 */
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (this.bossEvent != null) {
            this.bossEvent.removePlayer(player);
        }
    }

    /** BOSS 死亡时清掉血条 */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
            this.bossEvent = null;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
            this.bossEvent = null;
        }
    }

    /** 进入二阶段：挂 STR + 速度 ×1.4 + 伤害 ×1.3 modifier（一次性，不会重复挂） */
    private void enterBerserk() {
        this.entityData.set(DATA_BERSERK, true);
        this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, Integer.MAX_VALUE, 1, false, true, true));
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            // 基础 0.30 → 0.42（+40%）：用 ADD_VALUE 0.12 实现，避免与 multiplier 语义混淆
            speed.addTransientModifier(new AttributeModifier(BERSERK_SPEED_ID,
                    0.12D, AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance dmg = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            // 基础 18 → 23.4（+30%）：用 ADD_VALUE 5.4
            dmg.addTransientModifier(new AttributeModifier(BERSERK_DAMAGE_ID,
                    5.4D, AttributeModifier.Operation.ADD_VALUE));
        }
        // 二阶段登场音效 + 粒子（让玩家明确感知到形态切换）
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                    this.getX(), this.getY(0.6D), this.getZ(), 30,
                    0.6D, 0.8D, 0.6D, 0.4D);
            this.playSound(net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, 1.5F, 0.7F);
        }
        CorpseOrigin.LOGGER.debug("EldorKingZbr entered berserk phase at HP {}/{}",
                this.getHealth(), this.getMaxHealth());
    }
}
