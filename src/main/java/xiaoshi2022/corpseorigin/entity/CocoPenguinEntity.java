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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.registry.ModItems;

/**
 * CoCo 企鹅 - 未尸兄化的王企鹅。
 * <p>
 * 特色是独立于原版氧气条的「自持氧气 + 饥饿」系统：它不会自动上浮，
 * 氧气见底时会主动蹬一次水，靠吃鱼和吃虫子回饱食度。
 */
public class CocoPenguinEntity extends Animal implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 动画 ====================
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("swim");
    protected static final RawAnimation EAT_ANIM = RawAnimation.begin().thenPlay("eat");

    // ==================== 饥饿系统 ====================
    private static final int MAX_HUNGER = 720;
    private static final int HUNGER_DRAIN_INTERVAL = 1200;
    private int hunger = MAX_HUNGER;
    private int hungerDrainTimer = 0;

    private static final int EAT_COOLDOWN_TICKS = 80;
    private int eatCooldown = 0;

    // ==================== 攻击 ====================
    private static final int ATTACK_COOLDOWN_TICKS = 30;
    private int attackCooldown = 0;

    // ==================== 氧气 ====================
    private static final int MAX_AIR = 300;
    /** 剩余这么多氧气才开始上浮 */
    private static final int OXYGEN_WARNING_LEVEL = 30;
    private int airSupply = MAX_AIR;
    private int surfaceCooldown = 0;

    public CocoPenguinEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
    }

    @Override
    protected void registerGoals() {
        // 注意：不加 FloatGoal，否则企鹅会被强制浮在水面
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<AbstractFish>(this, AbstractFish.class,
                10, true, false, (target, level) -> target instanceof AbstractFish && !target.isBaby()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<ZbWormEntity>(this, ZbWormEntity.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    // ==================== 动画控制器 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 3, this::animationController));
    }

    private PlayState animationController(AnimationTest<CocoPenguinEntity> test) {
        // 进食动画只在陆地上播
        if (eatCooldown > 0 && !this.isInWater()) {
            return test.setAndContinue(EAT_ANIM);
        }
        if (this.isInWater()) {
            return test.setAndContinue(SWIM_ANIM);
        }
        if (test.isMoving()) {
            return test.setAndContinue(WALK_ANIM);
        }
        return test.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== tick ====================

    @Override
    public void tick() {
        super.tick();

        handleAirSupply();
        handleHungerDrain();

        if (eatCooldown > 0) eatCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (surfaceCooldown > 0) surfaceCooldown--;

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.distanceTo(target) < 2.0D && attackCooldown <= 0) {
            performAttack(target);
        }

        // 氧气不足时主动上浮，每 20 tick 检查一次
        if (!this.level().isClientSide() && this.tickCount % 20 == 0
                && airSupply <= OXYGEN_WARNING_LEVEL && this.isInWater() && this.isUnderWater()
                && surfaceCooldown == 0) {
            surfaceForAir();
        }

        // 水里缓慢下沉，模拟真实企鹅
        if (this.isInWater() && this.isUnderWater() && !isSurfacing() && airSupply > OXYGEN_WARNING_LEVEL) {
            Vec3 vel = this.getDeltaMovement();
            if (vel.y > -0.1) {
                this.setDeltaMovement(vel.x, vel.y - 0.005, vel.z);
            }
        }
    }

    private boolean isSurfacing() {
        return surfaceCooldown > 0 && this.getDeltaMovement().y > 0;
    }

    /** 只给一次向上的力，靠冷却防止反复触发 */
    private void surfaceForAir() {
        this.setDeltaMovement(this.getDeltaMovement().x, 0.4, this.getDeltaMovement().z);
        this.surfaceCooldown = 60;
    }

    private void handleAirSupply() {
        if (this.isInWater() && !this.isNoAi()) {
            if (this.isUnderWater()) {
                if (airSupply > 0) {
                    airSupply--;
                }
                if (airSupply <= 0) {
                    this.hurt(this.damageSources().drown(), 1.0F);
                }
            } else if (airSupply < MAX_AIR) {
                airSupply = Math.min(MAX_AIR, airSupply + 12);
            }
        } else if (airSupply < MAX_AIR) {
            airSupply = Math.min(MAX_AIR, airSupply + 8);
        }
    }

    private void handleHungerDrain() {
        if (this.level().isClientSide()) return;

        hungerDrainTimer++;
        if (hungerDrainTimer >= HUNGER_DRAIN_INTERVAL) {
            hungerDrainTimer = 0;
            hunger = Math.max(0, hunger - 1);
            if (hunger <= 0) {
                this.hurt(this.damageSources().starve(), 1.0F);
            }
        }
    }

    private void performAttack(LivingEntity target) {
        target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.0F);
        attackCooldown = ATTACK_COOLDOWN_TICKS;

        if (target.isAlive()) return;

        // 不同猎物回饱食度不同
        int hungerRestore = 20;
        if (target instanceof Salmon) hungerRestore = 30;
        else if (target instanceof Cod) hungerRestore = 25;
        else if (target instanceof TropicalFish) hungerRestore = 15;
        else if (target instanceof Pufferfish) hungerRestore = 10;
        else if (target instanceof ZbWormEntity) hungerRestore = 30;

        addHunger(hungerRestore);

        if (!this.isInWater()) {
            triggerEatAnimation();
        }

        // 吞下尸兄虫会被寄生，当场变异成 CoCo 尸兄
        if (target instanceof ZbWormEntity && !this.level().isClientSide()) {
            convertToZombie();
        }

        this.setTarget(null);
    }

    /**
     * 转化为 CoCo 尸兄。
     * <p>
     * 由「吞下 / 被喂尸兄虫」触发（{@link #isFood}、{@link #feedItem}、{@link #performAttack}）。
     */
    public void convertToZombie() {
        if (this.level().isClientSide() || !this.isAlive()) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        CocoZombieEntity zombie = new CocoZombieEntity(ModEntities.COCO_ZOMBIE, serverLevel);
        zombie.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        zombie.setYHeadRot(this.getYHeadRot());

        zombie.addRegularHunger(this.hunger);
        zombie.setHealth(zombie.getMaxHealth());

        this.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.0F, 1.0F);
        zombie.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.0F, 1.0F);

        serverLevel.sendParticles(ParticleTypes.SMOKE,
                this.getX(), this.getY() + 1.0, this.getZ(),
                30, 0.5, 0.5, 0.5, 0.05);

        serverLevel.addFreshEntity(zombie);
        this.discard();
    }

    // ==================== 喂养 ====================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (isFood(itemStack) && feedItem(itemStack)) {
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    public boolean feedFish(ItemStack stack) {
        if (!canEat()) return false;

        Item item = stack.getItem();
        int restoreAmount;
        if (item == Items.SALMON) restoreAmount = 40;
        else if (item == Items.COD) restoreAmount = 35;
        else if (item == Items.TROPICAL_FISH) restoreAmount = 25;
        else if (item == Items.PUFFERFISH) restoreAmount = 15;
        else return false;

        addHunger(restoreAmount);
        if (!this.isInWater()) {
            triggerEatAnimation();
        }
        if (!this.level().isClientSide()) {
            stack.shrink(1);
        }
        return true;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.COD || item == Items.SALMON
                || item == Items.TROPICAL_FISH || item == ModItems.ZB_WORM_ITEM;
    }

    public boolean feedItem(ItemStack stack) {
        if (!canEat()) return false;

        Item item = stack.getItem();
        int restoreAmount = 0;

        if (item == ModItems.ZB_WORM_ITEM) {
            restoreAmount = 30;
            // 吃了虫子，直接感染
            if (!this.level().isClientSide()) {
                convertToZombie();
            }
        } else if (item == Items.SALMON) restoreAmount = 40;
        else if (item == Items.COD) restoreAmount = 35;
        else if (item == Items.TROPICAL_FISH) restoreAmount = 25;
        else if (item == Items.PUFFERFISH) restoreAmount = 15;

        if (restoreAmount <= 0) return false;

        addHunger(restoreAmount);
        triggerEatAnimation();
        if (!this.level().isClientSide()) {
            stack.shrink(1);
        }
        return true;
    }

    public void addHunger(int amount) {
        hunger = Math.min(MAX_HUNGER, hunger + amount);
        if (this.getHealth() < this.getMaxHealth()) {
            this.heal(amount / 5.0F);
        }
    }

    public boolean canEat() {
        return eatCooldown <= 0;
    }

    public void triggerEatAnimation() {
        eatCooldown = EAT_COOLDOWN_TICKS;
        this.getNavigation().stop();
    }

    public boolean isHungry() {
        return hunger <= 100;
    }

    public int getHunger() {
        return hunger;
    }

    public int getMaxHunger() {
        return MAX_HUNGER;
    }

    public int getAirSupply() {
        return airSupply;
    }

    public int getMaxAir() {
        return MAX_AIR;
    }

    /** 氧气完全自管，屏蔽原版的补气逻辑 */
    @Override
    protected int increaseAirSupply(int currentAir) {
        return currentAir;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }

    // ==================== 持久化 ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Hunger", this.hunger);
        output.putInt("AirSupply", this.airSupply);
        output.putInt("EatCooldown", this.eatCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.hunger = input.getIntOr("Hunger", MAX_HUNGER);
        this.airSupply = input.getIntOr("AirSupply", MAX_AIR);
        this.eatCooldown = input.getIntOr("EatCooldown", 0);
    }

    // ==================== 水中移动 ====================

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(0.04F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.96D));
        } else {
            super.travel(travelVector);
        }
    }
}
