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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModItems;

/**
 * 尸兄虫 - 大叔体内钻出来的寄生虫。
 * <p>
 * 平时靠撕咬近身，被攻击时会先记仇再反击；有一定概率借「寄生」动画扑到攻击者脸上。
 * 玩家空手右键可以把它抓成物品。
 */
public class ZbWormEntity extends PathfinderMob implements GeoEntity, ZombieKin {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 动画 ====================
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack").thenLoop("idle");
    protected static final RawAnimation PARASITE_ANIM = RawAnimation.begin().thenPlay("parasite").thenLoop("idle");

    // ==================== 攻击冷却 ====================
    private static final int ATTACK_COOLDOWN_TICKS = 20;
    private int attackCooldown = 0;

    // ==================== 扑脸（寄生）====================
    private static final int JUMP_ATTACK_DURATION = 10;
    private static final double JUMP_ATTACK_HORIZONTAL = 1.2D;
    private static final double JUMP_ATTACK_VERTICAL = 0.8D;

    private boolean isJumpAttacking = false;
    private LivingEntity jumpTarget = null;
    private int jumpAttackTicks = 0;

    // ==================== 反击记忆 ====================
    private boolean isRetaliating = false;
    private LivingEntity attacker = null;

    public ZbWormEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // 只对「打过自己的人」反击，不做无差别主动仇恨（原著里是尸王的耳目，不主动猎玩家）
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class,
                0, true, false, (target, level) -> this.isRetaliating && target == this.attacker));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Villager>(this, Villager.class, true));
        // 非尸族生物照打；同类不打（原版的「饥饿时才同类相食」依赖尸兄饥饿系统，目标项目没有）
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<Mob>(this, Mob.class,
                0, true, false,
                (target, level) -> ZombieKin.isNotZombieKin(target) && !(target instanceof ZbWormEntity)));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<Animal>(this, Animal.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    // ==================== 动画控制器 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 5, this::movementController));
        // 攻击动画单独一条控制器，才能盖在移动动画之上
        controllers.add(new AnimationController<>("attackController", 0, this::attackController));
    }

    private PlayState movementController(AnimationTest<ZbWormEntity> test) {
        if (this.isJumpAttacking) {
            return test.setAndContinue(PARASITE_ANIM);
        }
        if (this.isSprinting() || this.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            return test.setAndContinue(WALK_ANIM);
        }
        return test.setAndContinue(IDLE_ANIM);
    }

    private PlayState attackController(AnimationTest<ZbWormEntity> test) {
        // 冷却刚被点上的那几 tick 视为「正在咬」，之后交还给移动控制器
        if (this.attackCooldown > ATTACK_COOLDOWN_TICKS - 5) {
            return test.setAndContinue(ATTACK_ANIM);
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== tick ====================

    @Override
    public void tick() {
        super.tick();

        if (this.isJumpAttacking && this.jumpTarget != null) {
            handleJumpAttack();
        } else if (this.isJumpAttacking) {
            // 目标没了，取消扑击
            this.isJumpAttacking = false;
            this.jumpTarget = null;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        // 每 2 秒清一次反击记忆，避免永久敌对
        if (this.isRetaliating && this.tickCount % 40 == 0 && this.getTarget() == null) {
            this.isRetaliating = false;
            this.attacker = null;
        }
    }

    private void handleJumpAttack() {
        if (this.jumpTarget == null || !this.jumpTarget.isAlive()) {
            this.isJumpAttacking = false;
            this.jumpTarget = null;
            return;
        }

        this.jumpAttackTicks++;

        if (this.jumpAttackTicks > JUMP_ATTACK_DURATION) {
            this.isJumpAttacking = false;
            this.jumpTarget = null;
            this.jumpAttackTicks = 0;
            return;
        }

        Vec3 direction = this.jumpTarget.position().subtract(this.position()).normalize();
        double progress = (double) this.jumpAttackTicks / JUMP_ATTACK_DURATION;

        // 抛物线：水平匀速，垂直方向先升后降
        double y = JUMP_ATTACK_VERTICAL * (1 - Math.abs(progress * 2 - 1));
        this.setDeltaMovement(direction.x * JUMP_ATTACK_HORIZONTAL, y, direction.z * JUMP_ATTACK_HORIZONTAL);

        // 贴到脸上就咬一口
        if (this.distanceTo(this.jumpTarget) < 1.5 && canAttack()
                && this.level() instanceof ServerLevel serverLevel) {
            this.doHurtTarget(serverLevel, this.jumpTarget);
            setAttackCooldown();
            this.isJumpAttacking = false;
            this.jumpTarget = null;
            this.jumpAttackTicks = 0;
        }
    }

    /** 50% 概率改扑击，否则继续走普通近战 */
    private void tryJumpAttack(LivingEntity target) {
        if (this.isJumpAttacking || !canAttack()) return;

        if (this.random.nextFloat() < 0.5F) {
            this.isJumpAttacking = true;
            this.jumpTarget = target;
            this.jumpAttackTicks = 0;
        }
    }

    public boolean canAttack() {
        return this.attackCooldown <= 0;
    }

    public void setAttackCooldown() {
        this.attackCooldown = ATTACK_COOLDOWN_TICKS;
    }

    // ==================== 受击 / 攻击 ====================

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity livingAttacker) {
            this.isRetaliating = true;
            this.attacker = livingAttacker;
            this.setTarget(livingAttacker);
            tryJumpAttack(livingAttacker);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!canAttack()) {
            return false;
        }
        boolean hurt = super.doHurtTarget(level, target);
        if (hurt) {
            setAttackCooldown();
        }
        return hurt;
    }

    // ==================== 交互 ====================

    /** 空手右键把虫子抓成物品 */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide() || !this.isAlive()) {
            return InteractionResult.PASS;
        }

        // 手上有东西时优先让物品自己处理交互
        if (!player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }

        ItemStack wormStack = new ItemStack(ModItems.ZB_WORM_ITEM);
        this.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 1.0F, 1.0F);

        if (!player.addItem(wormStack)) {
            player.drop(wormStack, false);
            player.sendOverlayMessage(Component.translatable("message.corpseorigin.zb_worm_entity.text_01"));
        } else {
            player.sendOverlayMessage(Component.translatable("message.corpseorigin.zb_worm_entity.text_02"));
        }

        this.discard();
        return InteractionResult.SUCCESS;
    }

    public boolean isJumpAttacking() {
        return this.isJumpAttacking;
    }

    public boolean isRetaliating() {
        return this.isRetaliating;
    }
}
