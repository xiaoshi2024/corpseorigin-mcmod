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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.registry.ModEntities;

import java.util.List;

/**
 * 青蛙奇葩尸兄（{@code frog_zbr_mc}）——精英怪。
 * <p>
 * 原著设定：巨大的青蛙尸兄，头顶上骑着一只"大鬼头"小尸兄（模型里 {@code rider → Waist →
 * Headx → brains} 一串骨骼），由它控制青蛙行动。
 * <ul>
 *   <li><b>舌头肘飞</b>：中距离（2.5~10 格）弹射舌头 {@code tongue_attack}，把目标连人带甲
 *       抽上天（高击退 + 竖直上抛）。</li>
 *   <li><b>腐蚀吐息</b>：骑手控制的远程招——鼓起 {@code throat} 吐出墨绿色腐蚀液
 *       （{@code cast} 动画，原著"喷出墨绿色的液体，腐蚀性冲击力很强"），中毒 + 直伤，
 *       附带一条从嘴到目标的绿色粘液粒子轨迹。</li>
 *   <li><b>捕食蚊子</b>：附近有蚊子尸兄（核心或环绕蚊）时主动弹舌吃掉，回复自身生命——
 *       原著里青蛙吃蚊子天经地义，玩家放蚊香围场时它会来抢食。</li>
 *   <li><b>射中脑袋</b>：被远程武器（投射物）命中时，骑手的大脑被当场射出
 *       （{@code was_shot_brains} 动画：大脑飞出落地），随后进入"无脑"状态
 *       （{@code not_brains} 隐藏大脑）：移速 +30%、近战伤害 +50%，但失去骑手的
 *       精确控制——不再吐毒液，只剩疯狂的撕咬与抽打。</li>
 * </ul>
 * 动画全部用上：idle / walk / attack（近战撕咬）/ tongue_attack / cast（吐液）/
 * hurt / death / was_shot_brains（大脑射出）/ not_brains（无脑状态）。
 */
public class FrogZbrMcEntity extends PathfinderMob implements GeoEntity {

    // ==================== 数值 ====================

    /** 舌头攻击最远距离（格） */
    public static final double TONGUE_RANGE = 10.0D;
    /** 舌头攻击最小距离（更近直接撕咬） */
    public static final double TONGUE_MIN_RANGE = 2.5D;
    /** 舌头攻击冷却（tick） */
    public static final int TONGUE_COOLDOWN = 60;
    /** 腐蚀吐息最远距离（格） */
    public static final double SPIT_RANGE = 16.0D;
    /** 腐蚀吐息冷却（tick） */
    public static final int SPIT_COOLDOWN = 200;
    /** 无脑狂暴：移速加成 */
    private static final double BRAINLESS_SPEED_BONUS = 0.30D;
    /** 无脑狂暴：近战伤害倍率 */
    private static final float BRAINLESS_DAMAGE_MULT = 1.5F;
    /** 吃一只蚊子回复的生命 */
    public static final float EAT_HEAL = 25.0F;
    /** 吃蚊子核心一口的伤害（160 血核心两口吃完） */
    public static final float EAT_CORE_BITE = 80.0F;

    // ==================== 同步数据 ====================

    /** 骑手大脑是否已被射掉（客户端据此播放 not_brains 并保持） */
    private static final EntityDataAccessor<Boolean> DATA_BRAINLESS =
            SynchedEntityData.defineId(FrogZbrMcEntity.class, EntityDataSerializers.BOOLEAN);

    // ==================== 动画 ====================

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation HURT_ANIM = RawAnimation.begin().thenPlayAndHold("hurt");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation ANIM_TONGUE = RawAnimation.begin().thenPlay("tongue_attack");
    private static final RawAnimation ANIM_CAST = RawAnimation.begin().thenPlay("cast");
    private static final RawAnimation ANIM_WAS_SHOT = RawAnimation.begin()
            .thenPlay("was_shot_brains").thenPlayAndHold("not_brains");
    private static final RawAnimation ANIM_NOT_BRAINS = RawAnimation.begin().thenPlayAndHold("not_brains");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 服务端计时 ====================

    private int tongueCooldown = 0;
    private int spitCooldown = 0;
    private int eatCooldown = 0;
    /** 大脑刚被射出：was_shot_brains 播完（2s）后才进入无脑狂暴态（动画衔接由触发链自理） */
    private int brainShotDelay = -1;
    /** 吐液预约：cast 动画抬头上扬到 1.08s（约 22 tick）后液体才出手 */
    private int pendingSpitTicks = -1;
    private LivingEntity pendingSpitTarget;

    public FrogZbrMcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 25;
        this.setPersistenceRequired();
    }

    // ==================== 属性 ====================

    /** 140 血的重甲大青蛙：皮糙肉厚、顶抗击退，怕的是被放风筝和火焰。 */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 140.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // ==================== 同步数据 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BRAINLESS, false);
    }

    public boolean isBrainless() {
        return this.entityData.get(DATA_BRAINLESS);
    }

    private void setBrainless(boolean value) {
        this.entityData.set(DATA_BRAINLESS, value);
    }

    // ==================== AI 目标 ====================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, false));
    }

    // ==================== 音效 ====================

    @Override
    public net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.FROG_AMBIENT;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FROG_HURT;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.FROG_DEATH;
    }

    @Override
    public float getSoundVolume() {
        return 1.0F;
    }

    // ==================== 存档 ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Brainless", isBrainless());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        if (in.getBooleanOr("Brainless", false)) {
            applyBrainlessModifiers();
        }
    }

    // ==================== 受伤：射中脑袋 → 大脑飞出 ====================

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // 原著：射骑手的大脑才有用——任何投射物命中（弓弩/枪械/三叉戟）一击爆脑
        if (!isBrainless() && brainShotDelay < 0
                && source.getDirectEntity() instanceof Projectile) {
            brainShotDelay = 50; // was_shot_brains 动画 2.0s，播完再进入无脑态
            triggerAnim("brainsController", "was_shot_brains");
            level.playSound(null, this.blockPosition(),
                    SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 1.6F);
            level.sendParticles(ParticleTypes.CRIT,
                    this.getX(), this.getEyeY(), this.getZ(), 12, 0.2D, 0.2D, 0.2D, 0.1D);
        }
        return super.hurtServer(level, source, amount);
    }

    /** 无脑狂暴：移速 +30%（切换与读档都要调用，带去重检查） */
    private void applyBrainlessModifiers() {
        setBrainless(true);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        net.minecraft.resources.Identifier id = CorpseOrigin.id("frog_brainless");
        if (speed != null && speed.getModifier(id) == null) {
            speed.addOrReplacePermanentModifier(new AttributeModifier(
                    id, BRAINLESS_SPEED_BONUS, AttributeModifier.Operation.ADD_VALUE));
        }
        // 骑手死了，愤怒地嚎一嗓子
        this.playSound(SoundEvents.RAVAGER_ROAR, 1.4F, 0.7F);
    }

    // ==================== 服务端主循环 ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        if (this.tongueCooldown > 0) this.tongueCooldown--;
        if (this.spitCooldown > 0) this.spitCooldown--;
        if (this.eatCooldown > 0) this.eatCooldown--;

        // 大脑射出动画播完 → 正式进入无脑狂暴态
        if (this.brainShotDelay > 0) {
            this.brainShotDelay--;
            if (this.brainShotDelay == 0) {
                this.brainShotDelay = -1;
                applyBrainlessModifiers();
            }
        }

        // 腐蚀吐息出手（cast 动画抬头到最高点时）
        if (this.pendingSpitTicks > 0) {
            this.pendingSpitTicks--;
            if (this.pendingSpitTicks == 0) {
                executeSpit(this.pendingSpitTarget);
                this.pendingSpitTarget = null;
                this.pendingSpitTicks = -1;
            }
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            double distance = this.distanceTo(target);
            if (!isBrainless()) {
                // 无脑化之后只剩撕咬：舌头与吐息都是骑手的操作
                if (distance > TONGUE_MIN_RANGE && distance <= TONGUE_RANGE
                        && this.tongueCooldown <= 0 && this.hasLineOfSight(target)) {
                    performTongueAttack(target, 5.0F);
                } else if (distance > TONGUE_RANGE && distance <= SPIT_RANGE
                        && this.spitCooldown <= 0 && this.hasLineOfSight(target)) {
                    performSpit(target);
                }
            }
            if (distance <= 2.6D && this.tongueCooldown <= 0 && this.hurtTime == 0) {
                performMeleeBite(target);
            }
        } else {
            // 没有战斗目标：找蚊子加餐（16 格内最近的蚊子尸兄 / 环绕蚊）
            tryEatMosquito();
        }
    }

    // ==================== 近战撕咬 ====================

    private void performMeleeBite(LivingEntity target) {
        triggerAnim("action", "attack");
        this.tongueCooldown = 20; // 撕咬与舌头共用冷却轴，防止连招无缝

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (isBrainless() ? BRAINLESS_DAMAGE_MULT : 1.0F);
        if (target.hurtServer((ServerLevel) this.level(), this.damageSources().mobAttack(this), damage)) {
            this.playSound(SoundEvents.FROG_EAT, 1.0F, 0.7F);
        }
    }

    // ==================== 舌头肘飞 ====================

    /**
     * 弹舌抽击：{@code tongue_attack} 动画（0.04s 出舌），命中把目标"肘飞"——
     * 高水平击退 + 强竖直上抛，脱离战斗的代价就是飞出去摔一跤。
     */
    private void performTongueAttack(LivingEntity target, float damage) {
        triggerAnim("action", "tongue_attack");
        this.tongueCooldown = TONGUE_COOLDOWN;
        // 舌头在动画 0.04s（≈2 tick）后才弹出，伤害与击退预约 3 tick
        scheduleTongueHit(target, damage);
    }

    private void scheduleTongueHit(LivingEntity target, float damage) {
        // 用一个小状态机在 3 tick 后结算（舌头弹出瞬间）
        this.pendingSpitTicks = -1;
        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.POOF,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                4, 0.2D, 0.2D, 0.2D, 0.01D);
        // 直接结算（舌头动画与命中的 1~2 tick 偏差肉眼不可见）
        Vec3 knock = target.position().subtract(this.position());
        knock = new Vec3(knock.x, 0, knock.z);
        if (knock.lengthSqr() < 1.0E-4D) {
            knock = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(1.0D);
        }
        knock = knock.normalize().scale(1.9D);
        if (target.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
            target.setDeltaMovement(target.getDeltaMovement()
                    .add(knock.x, 0.85D, knock.z));
            target.hurtMarked = true; // 立即同步击退给客户端
            this.playSound(SoundEvents.FROG_TONGUE, 1.2F, 0.6F);
        }
    }

    // ==================== 腐蚀吐息（骑手的远程招） ====================

    private void performSpit(LivingEntity target) {
        triggerAnim("action", "cast");
        this.spitCooldown = SPIT_COOLDOWN;
        this.pendingSpitTicks = 22; // cast 动画 1.083s（26 tick）抬头完成 → 出手
        this.pendingSpitTarget = target;
    }

    /** 墨绿色腐蚀液：一条粘液粒子轨迹 + 直伤 + 中毒 */
    private void executeSpit(LivingEntity target) {
        if (target == null || !target.isAlive()) return;
        ServerLevel level = (ServerLevel) this.level();

        Vec3 from = this.getEyePosition().add(0.0D, -0.4D, 0.0D);
        Vec3 to = target.getEyePosition();
        for (int i = 0; i <= 14; i++) {
            Vec3 p = from.lerp(to, i / 14.0D);
            level.sendParticles(ParticleTypes.ITEM_SLIME, p.x, p.y, p.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 1.0F, 0.6F);

        if (target.hurtServer(level, this.damageSources().mobAttack(this), 6.0F)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
        }
    }

    // ==================== 捕食蚊子尸兄 ====================

    /** 原著：青蛙吃蚊子。附近有蚊子尸兄（核心或环绕蚊）就弹舌吃掉回血。 */
    private void tryEatMosquito() {
        if (this.eatCooldown > 0 || this.tongueCooldown > 0) return;
        ServerLevel level = (ServerLevel) this.level();

        // 先找核心（大餐），再找环绕蚊（小食）
        List<MosquitoZbrEntity> cores = level.getEntitiesOfClass(MosquitoZbrEntity.class,
                this.getBoundingBox().inflate(12.0D), LivingEntity::isAlive);
        if (!cores.isEmpty()) {
            MosquitoZbrEntity core = cores.get(0);
            this.getLookControl().setLookAt(core);
            triggerAnim("action", "tongue_attack");
            this.eatCooldown = 40;
            this.tongueCooldown = 30;
            if (core.hurtServer(level, this.damageSources().mobAttack(this), EAT_CORE_BITE)) {
                heal(EAT_HEAL);
                this.playSound(SoundEvents.FROG_EAT, 1.2F, 0.8F);
                level.sendParticles(ParticleTypes.HEART,
                        this.getX(), this.getEyeY(), this.getZ(), 2, 0.4D, 0.4D, 0.4D, 0.0D);
            }
            return;
        }

        List<MosquitoSwarmEntity> swarm = level.getEntitiesOfClass(MosquitoSwarmEntity.class,
                this.getBoundingBox().inflate(12.0D), LivingEntity::isAlive);
        if (!swarm.isEmpty()) {
            MosquitoSwarmEntity snack = swarm.get(0);
            this.getLookControl().setLookAt(snack);
            triggerAnim("action", "tongue_attack");
            this.eatCooldown = 30;
            this.tongueCooldown = 20;
            snack.discard();
            heal(EAT_HEAL * 0.5F);
            this.playSound(SoundEvents.FROG_EAT, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.POOF,
                    snack.getX(), snack.getY(), snack.getZ(), 6, 0.1D, 0.1D, 0.1D, 0.01D);
            level.sendParticles(ParticleTypes.HEART,
                    this.getX(), this.getEyeY(), this.getZ(), 1, 0.4D, 0.4D, 0.4D, 0.0D);
        }
    }

    // ==================== 死亡掉落 ====================

    /** 必定掉落：尸兄肉块 ×6 + 腐肉 ×2 */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(
                xiaoshi2022.corpseorigin.registry.ModItems.ZBR_FLESH, 6), 0.0F);
        this.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.ROTTEN_FLESH, 2), 0.0F);
    }

    // ==================== GeckoLib 动画 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主控制器：death > hurt > 行走 > idle
        controllers.add(new AnimationController<>("movement", 4, test -> {
            if (this.isDeadOrDying()) {
                return test.setAndContinue(DEATH_ANIM);
            }
            if (this.hurtTime > 0) {
                return test.setAndContinue(HURT_ANIM);
            }
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.004D) {
                return test.setAndContinue(WALK_ANIM);
            }
            return test.setAndContinue(IDLE_ANIM);
        }));
        // 动作控制器：attack / tongue_attack / cast（triggerAnim 触发）
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.CONTINUE)
                .triggerableAnim("attack", ANIM_ATTACK)
                .triggerableAnim("tongue_attack", ANIM_TONGUE)
                .triggerableAnim("cast", ANIM_CAST));
        // 骑手大脑控制器：was_shot_brains 触发链自带 not_brains 结尾（播完无缝续播）；
        // default 只认 DATA_BRAINLESS（读档直接无脑态 / 触发链播完后的兜底保持）
        controllers.add(new AnimationController<>("brainsController", 0, test -> {
            if (isBrainless()) {
                return test.setAndContinue(ANIM_NOT_BRAINS);
            }
            return PlayState.CONTINUE;
        }).triggerableAnim("was_shot_brains", ANIM_WAS_SHOT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
