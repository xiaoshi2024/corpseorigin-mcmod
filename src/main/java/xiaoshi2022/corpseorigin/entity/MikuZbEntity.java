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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.registry.ModSounds;

/**
 * 初音尸兄 - 《尸兄》同人原创女性尸兄（致敬初音未来）。
 * <p>
 * 原作设定：
 * <ul>
 *   <li>初登场于车顶，投掷大葱击杀怪物控（动画版为漫展尸潮成员）</li>
 *   <li>腹部膨胀（消化不良），通过口中管状器官吸食血肉</li>
 *   <li>后被男尸兄吸干吞噬，助其进化为大块头尸兄</li>
 * </ul>
 * <p>
 * 本实体特性：
 * <ul>
 *   <li>远程「投掷大葱」：4~14 格内有视线时掷出大葱（{@link LeekProjectileEntity}）</li>
 *   <li>近战命中触发「吸食」动画并吸血回自身</li>
 *   <li>腹部饱食度（DATA_BELLY）：击杀/进食积累，过饱触发「消化不良」（减速+缓慢回血）</li>
 *   <li>青绿色双马尾配色（贴图），空闲时偶尔冒音符粒子（唱歌梗）</li>
 * </ul>
 */
public class MikuZbEntity extends PathfinderMob implements GeoEntity, ZombieKin {

    // ==================== 动画（复用 lower_level_zb 的人形骨架动画） ====================
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");

    // ==================== 同步数据 ====================
    /** 正在播放吸食/投掷动画 */
    private static final EntityDataAccessor<Boolean> DATA_ATTACKING =
            SynchedEntityData.defineId(MikuZbEntity.class, EntityDataSerializers.BOOLEAN);
    /** 动画剩余 tick */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TICKS =
            SynchedEntityData.defineId(MikuZbEntity.class, EntityDataSerializers.INT);
    /** 腹部饱食度 0~100（原作：肚子被血肉撑大） */
    private static final EntityDataAccessor<Integer> DATA_BELLY =
            SynchedEntityData.defineId(MikuZbEntity.class, EntityDataSerializers.INT);

    // ==================== 数值 ====================
    private static final int ATTACK_ANIM_TICKS = 15;
    private static final int THROW_COOLDOWN_TICKS = 100;
    private static final int THROW_WINDUP_TICKS = 15;
    private static final float THROW_MIN_DIST = 5.0F;
    private static final float THROW_MAX_DIST = 16.0F;
    /** 饱食度超过该值 → 消化不良 */
    private static final int OVERFULL_THRESHOLD = 80;
    /** 消化速度：每 100 tick 消化 1 点 */
    private static final int DIGEST_INTERVAL = 100;
    /** 唱歌音符粒子间隔 */
    private static final int NOTE_PARTICLE_INTERVAL = 160;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int throwCooldown = 0;
    private int digestTimer = 0;

    // ==================== 构造 ====================

    public MikuZbEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 8;
    }

    // ==================== 属性 ====================

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACKING, false);
        builder.define(DATA_ATTACK_TICKS, 0);
        builder.define(DATA_BELLY, 40);
    }

    // ==================== AI ====================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // 被攻击立刻反击
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));

        // 主动攻击非尸族玩家
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class,
                10, true, false, (target, level) -> ZombieKin.isNotZombieKin(target)));

        // 村民 / 动物
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Villager>(this, Villager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<Animal>(this, Animal.class,
                10, true, false, (target, level) -> !(target instanceof MikuZbEntity)));

        // 饥饿时同类相食（遵循尸族规则）
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<Monster>(this, Monster.class,
                10, true, false, (target, level) ->
                ZombieKin.isZombieKin(target) && this.isHungry() && target != this));
    }

    // ==================== 近战（吸食） ====================

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        // 挥手 + 吸食动画
        this.swing(InteractionHand.MAIN_HAND, true);
        triggerAttackAnimation();

        boolean result = super.doHurtTarget(level, target);

        if (result) {
            // 管状器官吸食：命中回血
            this.heal(2.0F);
            addBelly(5);

            this.playSound(ModSounds.GROUND_CHI, 1.0F, 0.7F + this.random.nextFloat() * 0.3F);
            level.sendParticles(ParticleTypes.ITEM_SLIME,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    4, 0.2, 0.2, 0.2, 0.05);

            if (target instanceof LivingEntity living && !living.isAlive()) {
                onKill(living);
            }
        }

        return result;
    }

    private void onKill(LivingEntity target) {
        addBelly(15);
        this.heal(target.getMaxHealth() * 0.2F);
        this.playSound(SoundEvents.PLAYER_BURP, 1.0F, 0.8F);
    }

    private void triggerAttackAnimation() {
        if (this.level().isClientSide()) return;
        this.entityData.set(DATA_ATTACKING, true);
        this.entityData.set(DATA_ATTACK_TICKS, ATTACK_ANIM_TICKS);
    }

    // ==================== 投掷大葱 ====================

    private int throwWindup = 0;

    private void tickLeekThrow() {
        if (this.throwCooldown > 0) this.throwCooldown--;

        // 投掷前摇：先停顿瞄准再出手，避免边跑边扔
        if (this.throwWindup > 0) {
            this.throwWindup--;
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1.0, 0.2));
            if (this.throwWindup == 0) {
                doThrowLeek();
            }
            return;
        }

        if (this.tickCount % 5 != 0 || this.throwCooldown > 0) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        float dist = this.distanceTo(target);
        if (dist < THROW_MIN_DIST || dist > THROW_MAX_DIST) return;
        if (!this.hasLineOfSight(target)) return;

        // 进入前摇：面向目标、短暂停顿，再掷出
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.throwWindup = THROW_WINDUP_TICKS;
        this.throwCooldown = THROW_COOLDOWN_TICKS;
    }

    private void doThrowLeek() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;
        if (!this.hasLineOfSight(target)) return;

        if (this.level() instanceof ServerLevel serverLevel) {
            LeekProjectileEntity.throwLeek(serverLevel, this, target);
            triggerAttackAnimation();
        }
    }

    // ==================== 腹部饱食度 ====================

    public int getBelly() {
        return this.entityData.get(DATA_BELLY);
    }

    public void setBelly(int belly) {
        this.entityData.set(DATA_BELLY, Math.max(0, Math.min(100, belly)));
    }

    public void addBelly(int amount) {
        setBelly(getBelly() + amount);
    }

    public boolean isOverfull() {
        return getBelly() >= OVERFULL_THRESHOLD;
    }

    @Override
    public int getHunger() {
        return getBelly();
    }

    @Override
    public boolean isHungry() {
        return getBelly() < 30;
    }

    /**
     * 消化：饱食随时间下降；过饱时「消化不良」——减速但缓慢回血（原作梗：血肉堆积肠胃）。
     */
    private void tickDigest() {
        this.digestTimer++;
        if (this.digestTimer < DIGEST_INTERVAL) return;
        this.digestTimer = 0;

        int belly = getBelly();
        if (belly > 0) {
            setBelly(belly - 1);
            if (belly > 0) this.heal(0.5F);
        }

        if (isOverfull()) {
            this.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DIGEST_INTERVAL, 1, false, false));
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DIGEST_INTERVAL, 0, false, false));
        }
    }

    // ==================== 动画控制器 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 5, this::movementController));
        controllers.add(new AnimationController<>("attack", 2, this::attackController));
    }

    private PlayState movementController(AnimationTest<MikuZbEntity> test) {
        if (test.isMoving()) {
            return test.setAndContinue(WALK_ANIM);
        }
        return test.setAndContinue(IDLE_ANIM);
    }

    private PlayState attackController(AnimationTest<MikuZbEntity> test) {
        if (this.entityData.get(DATA_ATTACKING)) {
            return test.setAndContinue(ATTACK_ANIM);
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private void tickAttackAnimation() {
        if (this.level().isClientSide()) return;

        if (this.entityData.get(DATA_ATTACKING)) {
            int ticks = this.entityData.get(DATA_ATTACK_TICKS) - 1;
            this.entityData.set(DATA_ATTACK_TICKS, ticks);
            if (ticks <= 0) {
                this.entityData.set(DATA_ATTACKING, false);
            }
        }
    }

    // ==================== Tick ====================

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            tickAttackAnimation();
            tickLeekThrow();
            tickDigest();

            // 唱歌梗：空闲时冒音符
            if (this.tickCount % NOTE_PARTICLE_INTERVAL == 0 && this.getTarget() == null
                    && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.NOTE,
                        this.getX(), this.getY() + 2.0, this.getZ(),
                        1, 0.2, 0.2, 0.2, 1.0);
            }
        }
    }

    // ==================== 死亡掉落 ====================

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);

        // 大葱：掉落 0~2 根
        int count = this.random.nextInt(3);
        if (count > 0) {
            this.spawnAtLocation(level, new ItemStack(ModItems.LEEK, count), 0.0F);
        }
    }

    // ==================== 持久化 ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Belly", getBelly());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_BELLY, input.getIntOr("Belly", 40));
    }
}
