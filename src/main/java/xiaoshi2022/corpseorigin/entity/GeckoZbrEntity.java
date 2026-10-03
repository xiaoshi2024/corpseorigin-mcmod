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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/**
 * 壁虎奇葩尸兄（{@code gecko_zbr}）——精英怪，原型是原著"金色壁虎"。
 * <p>
 * 原著设定：一个半人大的金色壁虎，脑袋上还顶着<b>两颗带人脸的扭曲脑袋</b>
 * （模型 {@code zbr_head → L / R} 两颗小人头，idle/crawl/attack 里都在晃）；
 * "射他的两个脑袋"时尾巴会横过来挡子弹；断尾 + 舌头都能再生。
 * <ul>
 *   <li><b>伤害大头走短尾</b>：尾巴完好时，近战伤害的 70%（远程投射物 90%）
 *       由尾巴血池（自身最大生命的 50%）吸收——尾巴就是它的盾。原著里子弹把尾巴
 *       打成筛子，壁虎本体毫发无损。</li>
 *   <li><b>断尾</b>：尾池耗尽 → {@code no_tail}（尾巴崩落，掉落史莱姆球+腐肉）→
 *       {@code not_tail} 隐藏尾巴。断尾后减负提速 +20%，但本体开始全额承伤。</li>
 *   <li><b>再生</b>：断尾 30 秒后 {@code grow} 动画重新长出尾巴，尾池回满。</li>
 *   <li><b>怕火</b>：火焰伤害 1.75 倍且<b>无视尾巴分摊</b>直接烧本体——原著里它
 *       最终也是被丢进嘴里的手雷（爆炸/火）炸死的。</li>
 *   <li><b>三头轮咬</b>：近战每第 3 口是"双头合咬"（{@code zbr_head} 的 L/R 两颗
 *       人头一起下口）：伤害 ×1.5 并把人扑飞。</li>
 *   <li><b>舌头拖拽</b>：中距离（2.5~9 格）鞭击舌头 {@code tongue_attack}，
 *       把目标拽到自己嘴边。</li>
 * </ul>
 * 动画全部用上：idle / crawl / attack / tongue_attack / no_tail / not_tail / grow。
 */
public class GeckoZbrEntity extends PathfinderMob implements GeoEntity {

    // ==================== 数值 ====================

    /** 尾巴血池 = 最大生命 × 此系数（伤害大头由短尾承担） */
    public static final float TAIL_POOL_RATIO = 0.5F;
    /** 近战伤害走尾巴的比例（原著：尾巴替两颗脑袋挡子弹） */
    public static final float TAIL_ABSORB_MELEE = 0.7F;
    /** 投射物伤害走尾巴的比例（点名"射脑袋"被尾巴挡） */
    public static final float TAIL_ABSORB_PROJECTILE = 0.9F;
    /** 火焰弱点倍率（且无视尾巴分摊） */
    public static final float FIRE_WEAKNESS_MULT = 1.75F;
    /** 断尾后再生等待（tick，30 秒） */
    public static final int TAIL_REGROW_DELAY = 600;
    /** 断尾提速 */
    private static final double TAILLESS_SPEED_BONUS = 0.20D;
    /** 舌头拖拽最远距离 */
    public static final double TONGUE_RANGE = 9.0D;
    public static final double TONGUE_MIN_RANGE = 2.5D;
    private static final int TONGUE_COOLDOWN = 70;
    /** 双头合咬的间隔：每第 3 口 */
    private static final int TWIN_BITE_EVERY = 3;

    // ==================== 同步数据 ====================

    /** 尾巴是否还在（客户端据此播放 not_tail / grow） */
    private static final EntityDataAccessor<Boolean> DATA_HAS_TAIL =
            SynchedEntityData.defineId(GeckoZbrEntity.class, EntityDataSerializers.BOOLEAN);
    /** 断尾动画窗口：no_tail 播放期间为 true（客户端据此不提前切 not_tail，避免顶掉动画） */
    private static final EntityDataAccessor<Boolean> DATA_TAIL_SEVERING =
            SynchedEntityData.defineId(GeckoZbrEntity.class, EntityDataSerializers.BOOLEAN);

    // ==================== 动画 ====================

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CRAWL_ANIM = RawAnimation.begin().thenLoop("crawl");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation ANIM_TONGUE = RawAnimation.begin().thenPlay("tongue_attack");
    private static final RawAnimation ANIM_NO_TAIL = RawAnimation.begin()
            .thenPlay("no_tail").thenPlayAndHold("not_tail");
    private static final RawAnimation ANIM_NOT_TAIL = RawAnimation.begin().thenPlayAndHold("not_tail");
    private static final RawAnimation ANIM_GROW = RawAnimation.begin().thenPlay("grow");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 服务端状态 ====================

    private float tailHealth;
    /** no_tail 动画（1.5s）播完前不切到 not_tail，避免动画被顶掉 */
    private int severAnimDelay = -1;
    private int tailRegrowTimer = 0;
    private int tongueCooldown = 0;
    private int meleeCounter = 0;
    private int meleeCooldown = 0;

    public GeckoZbrEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 25;
        this.setPersistenceRequired();
        this.tailHealth = this.getMaxHealth() * TAIL_POOL_RATIO;
    }

    // ==================== 属性 ====================

    /** 180 血的金色大壁虎：皮糙肉厚跑得快，弱点只有火。 */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // ==================== 同步数据 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HAS_TAIL, true);
        builder.define(DATA_TAIL_SEVERING, false);
    }

    public boolean hasTail() {
        return this.entityData.get(DATA_HAS_TAIL);
    }

    private void setHasTail(boolean value) {
        this.entityData.set(DATA_HAS_TAIL, value);
    }

    /** 断尾动画（no_tail）是否正在播放窗口内 */
    private boolean isTailSevering() {
        return this.entityData.get(DATA_TAIL_SEVERING);
    }

    private void setTailSevering(boolean value) {
        this.entityData.set(DATA_TAIL_SEVERING, value);
    }

    public float getTailHealthRatio() {
        return Math.clamp(this.tailHealth / (this.getMaxHealth() * TAIL_POOL_RATIO), 0.0F, 1.0F);
    }

    // ==================== AI 目标 ====================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, true, false));
    }

    // ==================== 音效 ====================

    @Override
    public net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    public float getSoundVolume() {
        return 0.9F;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F; // 尸兄腔调，低沉
    }

    // ==================== 存档 ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putFloat("TailHealth", this.tailHealth);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.tailHealth = in.getFloatOr("TailHealth", this.getMaxHealth() * TAIL_POOL_RATIO);
        if (this.tailHealth <= 0F) {
            // 读档时已断尾：直接进入无尾待再生状态
            this.severAnimDelay = -1;
            this.tailRegrowTimer = 1;
            setHasTail(false);
            applyTaillessSpeed();
        }
    }

    // ==================== 受伤：伤害分摊 / 火焰弱点 / 断尾 ====================

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float toBody = amount;

        if (source.is(DamageTypeTags.IS_FIRE)) {
            // 怕火：1.75 倍且无视尾巴分摊，直接烧本体
            toBody = amount * FIRE_WEAKNESS_MULT;
            level.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + this.getBbHeight() * 0.6D, this.getZ(),
                    8, this.getBbWidth() * 0.4D, 0.3D, this.getBbWidth() * 0.4D, 0.02D);
            level.playSound(null, this.blockPosition(), SoundEvents.GUARDIAN_HURT,
                    SoundSource.HOSTILE, 0.8F, 0.6F);
        } else if (this.tailHealth > 0F) {
            // 尾巴挡刀：近战 70% / 投射物 90% 的伤害由尾池吸收
            float ratio = source.getDirectEntity() instanceof Projectile
                    ? TAIL_ABSORB_PROJECTILE : TAIL_ABSORB_MELEE;
            float toTail = Math.min(amount * ratio, this.tailHealth);
            this.tailHealth -= toTail;
            toBody = Math.max(amount - toTail, 1.0F);
            if (this.tailHealth <= 0F) {
                severTail(level);
            }
        }

        return super.hurtServer(level, source, toBody);
    }

    // ==================== 断尾与再生 ====================

    /** 断尾：播 no_tail（尾巴崩落动画）→ not_tail 隐藏，掉落尾段，开始计时再生 */
    private void severTail(ServerLevel level) {
        this.tailHealth = 0F;
        setHasTail(false);
        this.severAnimDelay = 30; // no_tail 动画 1.5s
        this.tailRegrowTimer = TAIL_REGROW_DELAY;
        setTailSevering(true); // 同步给客户端：窗口内不切 not_tail（触发链播完自动续上）
        triggerAnim("tailController", "no_tail");
        applyTaillessSpeed();

        // 尾巴掉在地上：史莱姆球（黏滑的尾巴）+ 腐肉
        this.spawnAtLocation(level, new ItemStack(Items.SLIME_BALL, 2), 0.3F);
        this.spawnAtLocation(level, new ItemStack(Items.ROTTEN_FLESH, 2), 0.0F);
        level.playSound(null, this.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK,
                SoundSource.HOSTILE, 1.2F, 0.6F);
        level.sendParticles(ParticleTypes.SQUID_INK,
                this.getX(), this.getY() + 0.5D, this.getZ() + 1.0D, 10, 0.4D, 0.3D, 0.4D, 0.02D);
    }

    /** 断尾减负提速 +20%（断尾与读档都要调用，带去重检查） */
    private void applyTaillessSpeed() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        Identifier id = CorpseOrigin.id("gecko_tailless");
        if (speed != null && speed.getModifier(id) == null) {
            speed.addOrReplacePermanentModifier(new AttributeModifier(
                    id, TAILLESS_SPEED_BONUS, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private void removeTaillessSpeed() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(CorpseOrigin.id("gecko_tailless"));
    }

    /** 再生：grow 动画 2.5s 长出新尾巴，尾池回满 */
    private void regrowTail(ServerLevel level) {
        this.tailHealth = this.getMaxHealth() * TAIL_POOL_RATIO;
        setHasTail(true);
        removeTaillessSpeed();
        triggerAnim("tailController", "grow");
        level.playSound(null, this.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE,
                SoundSource.HOSTILE, 0.8F, 1.4F);
    }

    // ==================== 服务端主循环 ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        if (this.tongueCooldown > 0) this.tongueCooldown--;
        if (this.meleeCooldown > 0) this.meleeCooldown--;

        // no_tail 播完 → 关闭断尾窗口（触发链已自动续上 not_tail），handler 恢复兜底
        if (this.severAnimDelay > 0) {
            this.severAnimDelay--;
            if (this.severAnimDelay == 0) {
                this.severAnimDelay = -1;
                setTailSevering(false);
            }
        }

        // 断尾再生倒计时
        if (!hasTail() && this.tailRegrowTimer > 0) {
            this.tailRegrowTimer--;
            if (this.tailRegrowTimer == 0) {
                regrowTail((ServerLevel) this.level());
            }
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            double distance = this.distanceTo(target);
            if (distance > TONGUE_MIN_RANGE && distance <= TONGUE_RANGE
                    && this.tongueCooldown <= 0 && this.hasLineOfSight(target)) {
                performTonguePull(target);
            } else if (distance <= 2.6D && this.meleeCooldown <= 0 && this.hurtTime == 0) {
                performBite(target);
            }
        }
    }

    // ==================== 近战：三头轮咬 ====================

    /** 主颌撕咬；每第 3 口换成"双头合咬"——两颗人脸脑袋一起下口，扑飞目标 */
    private void performBite(LivingEntity target) {
        triggerAnim("action", "attack");
        this.meleeCooldown = 16;
        this.meleeCounter++;

        boolean twinBite = this.meleeCounter % TWIN_BITE_EVERY == 0;
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (twinBite ? 1.5F : 1.0F);
        ServerLevel level = (ServerLevel) this.level();
        if (target.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
            this.playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.6F, 1.3F);
            if (twinBite) {
                Vec3 knock = target.position().subtract(this.position());
                knock = new Vec3(knock.x, 0, knock.z);
                if (knock.lengthSqr() < 1.0E-4D) {
                    knock = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(1.0D);
                }
                knock = knock.normalize().scale(0.9D);
                target.setDeltaMovement(target.getDeltaMovement().add(knock.x, 0.55D, knock.z));
                target.hurtMarked = true;
                level.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getEyeY(), target.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.1D);
            }
        }
    }

    // ==================== 舌头拖拽 ====================

    /** 鞭击舌头：原著"鞭子似的长舌"，命中把目标拽到嘴边 */
    private void performTonguePull(LivingEntity target) {
        triggerAnim("action", "tongue_attack");
        this.tongueCooldown = TONGUE_COOLDOWN;

        ServerLevel level = (ServerLevel) this.level();
        level.sendParticles(ParticleTypes.POOF,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                4, 0.2D, 0.2D, 0.2D, 0.01D);
        if (target.hurtServer(level, this.damageSources().mobAttack(this), 6.0F)) {
            Vec3 pull = this.position().subtract(target.position());
            pull = new Vec3(pull.x, 0, pull.z);
            if (pull.lengthSqr() > 1.0E-4D) {
                pull = pull.normalize().scale(1.2D);
                target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.25D, pull.z));
                target.hurtMarked = true;
            }
            this.playSound(SoundEvents.FROG_TONGUE, 1.2F, 0.5F);
        }
    }

    // ==================== 死亡掉落 ====================

    /** 必定掉落：尸兄肉块（带尾 ×8 / 断尾死 ×5）+ 尾巴的史莱姆球 + 腐肉 */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(level, new ItemStack(
                xiaoshi2022.corpseorigin.registry.ModItems.ZBR_FLESH, hasTail() ? 8 : 5), 0.0F);
        if (hasTail()) {
            this.spawnAtLocation(level, new ItemStack(Items.SLIME_BALL, 4), 0.0F);
        }
        this.spawnAtLocation(level, new ItemStack(Items.ROTTEN_FLESH, 3), 0.0F);
    }

    // ==================== GeckoLib 动画 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主控制器：爬行 / 待机（原著壁虎没有专属受击/死亡动画，走原版红闪）
        controllers.add(new AnimationController<>("movement", 4, test -> {
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.004D) {
                return test.setAndContinue(CRAWL_ANIM);
            }
            return test.setAndContinue(IDLE_ANIM);
        }));
        // 动作控制器：attack / tongue_attack
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.CONTINUE)
                .triggerableAnim("attack", ANIM_ATTACK)
                .triggerableAnim("tongue_attack", ANIM_TONGUE));
        // 尾巴控制器：no_tail 触发链自带 not_tail 结尾（播完无缝续播）；
        // 断尾窗口内返回 CONTINUE 让链播完，之后兜底保持 not_tail（读档无尾也走这里）
        controllers.add(new AnimationController<>("tailController", 0, test -> {
            if (!isTailSevering() && !hasTail()) {
                return test.setAndContinue(ANIM_NOT_TAIL);
            }
            return PlayState.CONTINUE;
        }).triggerableAnim("no_tail", ANIM_NO_TAIL)
          .triggerableAnim("grow", ANIM_GROW));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
