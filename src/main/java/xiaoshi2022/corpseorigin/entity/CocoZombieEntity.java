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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

import java.util.UUID;

/**
 * CoCo 尸兄（一阶段）- 企鹅外观的尸兄。
 * <p>
 * 保留企鹅的游泳/潜水/吃鱼特性，另外有独立的「普通饱食度」与「尸兄饱食度」两套数值，
 * 攻击会触发 eat 动画，靠击杀进化。
 */
public class CocoZombieEntity extends PathfinderMob implements GeoEntity, ZombieKin {

    // ==================== 动画 ====================
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    /** 模型没有 swim 动画，用 walk 代替 */
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation EAT_ANIM = RawAnimation.begin().thenPlay("eat");

    // ==================== 同步数据 ====================
    private static final EntityDataAccessor<Boolean> DATA_PLAYING_EAT =
            SynchedEntityData.defineId(CocoZombieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_EAT_TICKS =
            SynchedEntityData.defineId(CocoZombieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EAT_COOLDOWN =
            SynchedEntityData.defineId(CocoZombieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(CocoZombieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AIR_SUPPLY =
            SynchedEntityData.defineId(CocoZombieEntity.class, EntityDataSerializers.INT);

    // ==================== 字段 ====================
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int kills = 0;
    private int attackCooldown = 0;
    private static final int ATTACK_COOLDOWN_TICKS = 20;

    private static final int MAX_AIR = 300;
    private static final int OXYGEN_WARNING_LEVEL = 30;
    private int surfaceCooldown = 0;

    /** 主人跟随范围 */
    private static final double FOLLOW_RANGE = 16.0D;
    private static final double TELEPORT_RANGE = 32.0D;
    private int followCooldown = 0;

    // 普通饱食度（消化用），与「尸兄饱食度」相互独立
    private static final int MAX_REGULAR_HUNGER = 720;
    private static final int HUNGER_DRAIN_INTERVAL = 1200;
    private int hungerDrainTimer = 0;

    // 尸兄饱食度（吃尸体获得）
    private static final int MAX_CORPSE_HUNGER = 3;
    private int corpseHunger = 0;

    private int evolutionLevel = 1;

    /** 反击记忆：被攻击后的一段时间内会反击尸族 */
    private static final int HURT_MEMORY_DURATION = 200;
    private int lastHurtTick = -1000;

    private UUID masterUUID;

    /** 正在/已经和大叔合体，用于防止重复触发 */
    private boolean isFusing = false;
    private boolean hasFused = false;

    private final WaterBoundPathNavigation waterNavigation;
    private final GroundPathNavigation groundNavigation;

    // ==================== 构造函数 ====================

    public CocoZombieEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);

        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);

        // 水陆双导航，按是否潜水切换
        this.waterNavigation = new WaterBoundPathNavigation(this, level);
        this.groundNavigation = new GroundPathNavigation(this, level);
        this.navigation = this.groundNavigation;
    }

    public CocoZombieEntity(EntityType<? extends PathfinderMob> entityType, Level level, Player player) {
        this(entityType, level);
        if (player != null) {
            this.masterUUID = player.getUUID();
        }
    }

    // ==================== 属性 ====================

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 35.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLAYING_EAT, false);
        builder.define(DATA_EAT_TICKS, 0);
        builder.define(DATA_EAT_COOLDOWN, 0);
        builder.define(DATA_HUNGER, 720);
        builder.define(DATA_AIR_SUPPLY, MAX_AIR);
    }

    // ==================== AI ====================

    @Override
    protected void registerGoals() {
        // 最高优先级：盯上大叔（击杀后才有合体进化），不要求大叔已经吐出虫子
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<UncleEntity>(this, UncleEntity.class,
                10, true, false, (target, level) -> target.isAlive()));

        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // 主动攻击非尸族玩家
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class,
                10, true, false, (target, level) -> this.shouldAttackNonCorpsePlayer(target)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Villager>(this, Villager.class, true));
        // 攻击动物（企鹅自己除外）
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<Animal>(this, Animal.class,
                10, true, false, (target, level) ->
                        !(target instanceof CocoZombieEntity) && !(target instanceof CocoPenguinEntity)));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<AbstractFish>(this, AbstractFish.class,
                10, true, false, (target, level) -> target instanceof AbstractFish));

        // 只有被攻击过才反击尸族
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<Player>(this, Player.class,
                10, true, false, (target, level) -> this.shouldAttackCorpsePlayer(target)));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<CocoZombieEntity>(this, CocoZombieEntity.class,
                10, true, false, (target, level) -> this.shouldAttackOtherCorpseEntity(target)));
    }

    private boolean shouldAttackNonCorpsePlayer(LivingEntity entity) {
        if (!(entity instanceof Player)) return false;
        if (isMaster(entity)) return false;
        return ZombieKin.isNotZombieKin(entity);
    }

    private boolean shouldAttackCorpsePlayer(LivingEntity entity) {
        if (!(entity instanceof Player)) return false;
        if (isMaster(entity)) return false;
        if (!ZombieKin.isZombieKin(entity)) return false;
        return wasRecentlyHurt();
    }

    private boolean shouldAttackOtherCorpseEntity(LivingEntity entity) {
        if (entity == this) return false;
        if (entity instanceof CocoPenguinEntity) return false;
        if (!ZombieKin.isZombieKin(entity)) return false;
        return wasRecentlyHurt();
    }

    private boolean wasRecentlyHurt() {
        return (this.tickCount - this.lastHurtTick) < HURT_MEMORY_DURATION;
    }

    private boolean isMaster(LivingEntity entity) {
        return this.masterUUID != null && entity.getUUID().equals(this.masterUUID);
    }

    // ==================== 攻击 ====================

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        triggerEatAnimation();

        boolean result = super.doHurtTarget(level, target);

        if (result) {
            this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
            QiEffects.burst(level,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    0xc0182a, 1, 0.1);
            handleKill(target);
        }

        return result;
    }

    private void triggerEatAnimation() {
        if (this.level().isClientSide()) return;

        this.entityData.set(DATA_PLAYING_EAT, true);
        this.entityData.set(DATA_EAT_TICKS, 45);
        this.entityData.set(DATA_EAT_COOLDOWN, 20);
        this.getNavigation().stop();
    }

    private void handleKill(Entity target) {
        this.kills++;

        if (target instanceof LivingEntity living) {
            this.heal(living.getMaxHealth() * 0.2F);
        }

        addCorpseHunger(1);
        addRegularHunger(20);

        if (target instanceof Salmon) addRegularHunger(30);
        else if (target instanceof Cod) addRegularHunger(25);
        else if (target instanceof TropicalFish) addRegularHunger(15);
        else if (target instanceof Pufferfish) addRegularHunger(10);

        checkEvolution();

        // 击杀大叔 → 合体进化为二阶段
        if (target instanceof UncleEntity uncle && !this.hasFused) {
            tryFuseWithUncle(uncle);
        }
    }

    /**
     * 与大叔尸体合体，进化为 {@link CocoZombieXEntity}。
     * <p>
     * 原著剧情：CoCo 用长舌贯穿大叔头部后，与大叔尸体合体。
     */
    private void tryFuseWithUncle(UncleEntity uncle) {
        if (this.level().isClientSide()) return;
        if (this.isFusing || this.hasFused) return;
        this.isFusing = true;

        CorpseOrigin.LOGGER.info("企鹅尸兄击杀了大叔！开始合体进化...");

        // 长舌贯穿 + 血雾
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 tongueVec = uncle.position().add(0, uncle.getBbHeight() / 2, 0);
            // 原先在同一个点上重复撒 30 次粒子，整团塌缩成一朵气。
            QiEffects.burst(serverLevel,
                    tongueVec.x, tongueVec.y, tongueVec.z,
                    0xc0182a, 30, 0.1);
            QiEffects.burst(serverLevel,
                    uncle.getX(), uncle.getY() + uncle.getBbHeight() / 2, uncle.getZ(),
                    0xa855f7, 20, 0.3);
        }

        this.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.5F, 0.6F);
        this.playSound(SoundEvents.GENERIC_EAT.value(), 1.5F, 0.5F);

        // 合体实体自己会读两只原实体身上的数据
        CocoZombieXEntity fusedEntity = new CocoZombieXEntity(this.level(), this, uncle);

        this.discard();
        uncle.discard();

        fusedEntity.snapTo(this.getX(), this.getY(), this.getZ());
        this.level().addFreshEntity(fusedEntity);

        if (this.level() instanceof ServerLevel serverLevel) {
            for (Player player : serverLevel.players()) {
                if (player.distanceTo(fusedEntity) < 50) {
                    player.sendOverlayMessage(
                            Component.translatable("message.corpseorigin.coco_zombie_entity.text_01"));
                }
            }
        }

        this.hasFused = true;
        CorpseOrigin.LOGGER.info("企鹅尸兄与大叔合体完成！生成 CocoZombieXEntity");
    }

    // ==================== 进化 ====================

    private void checkEvolution() {
        if (this.kills >= this.evolutionLevel * 8 && this.evolutionLevel < 5) {
            evolve();
        }
    }

    private void evolve() {
        this.evolutionLevel++;
        this.kills = 0;

        double healthBonus = this.evolutionLevel * 5.0;
        double damageBonus = this.evolutionLevel * 1.0;
        double speedBonus = this.evolutionLevel * 0.03;

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(35.0 + healthBonus);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0 + damageBonus);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28 + speedBonus);
        this.getAttribute(Attributes.ARMOR).setBaseValue(2.0 + this.evolutionLevel);

        this.setHealth(this.getMaxHealth());

        this.level().broadcastEntityEvent(this, (byte) 20);
        this.playSound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.5F, 0.8F);
    }

    public int getEvolutionLevel() {
        return this.evolutionLevel;
    }

    public void setEvolutionLevel(int level) {
        this.evolutionLevel = Math.max(1, level);
    }

    // ==================== 喂养 ====================

    public boolean feedFish(ItemStack stack) {
        if (!canEat()) return false;

        Item item = stack.getItem();
        int restoreAmount;
        if (item == Items.SALMON) restoreAmount = 40;
        else if (item == Items.COD) restoreAmount = 35;
        else if (item == Items.TROPICAL_FISH) restoreAmount = 25;
        else if (item == Items.PUFFERFISH) restoreAmount = 15;
        else return false;

        addRegularHunger(restoreAmount);

        if (!this.isInWater()) {
            triggerEatAnimation();
        }
        if (!this.level().isClientSide()) {
            stack.shrink(1);
        }
        return true;
    }

    private boolean canEat() {
        return this.entityData.get(DATA_EAT_COOLDOWN) <= 0;
    }

    public void addRegularHunger(int amount) {
        this.entityData.set(DATA_HUNGER, Math.min(MAX_REGULAR_HUNGER, getRegularHunger() + amount));

        if (this.getHealth() < this.getMaxHealth()) {
            this.heal(amount / 5.0F);
        }
    }

    public int getRegularHunger() {
        return this.entityData.get(DATA_HUNGER);
    }

    private void addCorpseHunger(int amount) {
        this.corpseHunger = Math.max(0, Math.min(MAX_CORPSE_HUNGER, this.corpseHunger + amount));
    }

    public int getCorpseHunger() {
        return this.corpseHunger;
    }

    public void setCorpseHunger(int hunger) {
        this.corpseHunger = Math.max(0, Math.min(MAX_CORPSE_HUNGER, hunger));
    }

    // ==================== 氧气 ====================

    private void handleAirSupply() {
        if (this.isInWater()) {
            if (this.isUnderWater()) {
                int air = this.entityData.get(DATA_AIR_SUPPLY);
                if (air > 0) {
                    this.entityData.set(DATA_AIR_SUPPLY, air - 1);
                }
                if (air <= 0) {
                    this.hurt(this.damageSources().drown(), 1.0F);
                }
            } else {
                int air = this.entityData.get(DATA_AIR_SUPPLY);
                if (air < MAX_AIR) {
                    this.entityData.set(DATA_AIR_SUPPLY, Math.min(MAX_AIR, air + 12));
                }
            }
        } else {
            int air = this.entityData.get(DATA_AIR_SUPPLY);
            if (air < MAX_AIR) {
                this.entityData.set(DATA_AIR_SUPPLY, Math.min(MAX_AIR, air + 8));
            }
        }
    }

    private void handleSurfaceForAir() {
        if (this.surfaceCooldown > 0) {
            this.surfaceCooldown--;
            return;
        }

        if (this.entityData.get(DATA_AIR_SUPPLY) <= OXYGEN_WARNING_LEVEL && this.isInWater() && this.isUnderWater()) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0.4, this.getDeltaMovement().z);
            this.surfaceCooldown = 60;
        }
    }

    // ==================== 普通饱食度 ====================

    private void handleRegularHungerDrain() {
        if (this.level().isClientSide()) return;

        hungerDrainTimer++;
        if (hungerDrainTimer >= HUNGER_DRAIN_INTERVAL) {
            hungerDrainTimer = 0;
            int newHunger = Math.max(0, getRegularHunger() - 1);
            this.entityData.set(DATA_HUNGER, newHunger);

            if (newHunger <= 0) {
                this.hurt(this.damageSources().starve(), 1.0F);
            }
        }
    }

    // ==================== 动画控制器 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("controller", 5, this::animationController));
    }

    private PlayState animationController(AnimationTest<CocoZombieEntity> test) {
        if (this.entityData.get(DATA_PLAYING_EAT)) {
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

    private void tickEatAnimation() {
        if (this.level().isClientSide()) return;

        if (this.entityData.get(DATA_PLAYING_EAT)) {
            int eatTicks = this.entityData.get(DATA_EAT_TICKS) - 1;
            this.entityData.set(DATA_EAT_TICKS, eatTicks);
            if (eatTicks <= 0) {
                this.entityData.set(DATA_PLAYING_EAT, false);
            }
        }

        int eatCooldown = this.entityData.get(DATA_EAT_COOLDOWN);
        if (eatCooldown > 0) {
            this.entityData.set(DATA_EAT_COOLDOWN, eatCooldown - 1);
        }
    }

    // ==================== 导航切换 ====================

    private void updateNavigation() {
        this.navigation = this.isInWater() ? this.waterNavigation : this.groundNavigation;
    }

    // ==================== 主人跟随 ====================

    private void tickFollowMaster() {
        if (this.masterUUID == null) return;

        if (this.followCooldown > 0) {
            this.followCooldown--;
            return;
        }
        if (this.tickCount % 10 != 0) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Player master = serverLevel.getServer().getPlayerList().getPlayer(this.masterUUID);
        if (master == null || !master.isAlive()) return;

        double distanceToMaster = this.distanceToSqr(master);
        if (distanceToMaster > TELEPORT_RANGE * TELEPORT_RANGE) {
            teleportToMaster(master);
        } else if (distanceToMaster > FOLLOW_RANGE * FOLLOW_RANGE) {
            this.getNavigation().moveTo(master, 1.2D);
            this.followCooldown = 20;
        }
    }

    private void teleportToMaster(Player master) {
        for (int i = 0; i < 10; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double distance = 2 + this.random.nextDouble() * 2;
            double targetX = master.getX() + Math.cos(angle) * distance;
            double targetZ = master.getZ() + Math.sin(angle) * distance;
            double targetY = master.getY();

            if (this.level().noCollision(this.getBoundingBox()
                    .move(targetX - this.getX(), targetY - this.getY(), targetZ - this.getZ()))) {
                this.teleportTo(targetX, targetY, targetZ);
                this.getNavigation().stop();
                this.followCooldown = 40;
                break;
            }
        }
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

    // ==================== 状态效果 / 受击 ====================

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect().value() == MobEffects.POISON.value()
                || effect.getEffect().value() == MobEffects.HUNGER.value()) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // 记录被攻击（用于反击），主人打自己不算
        if (source.getEntity() instanceof LivingEntity attacker && !isMaster(attacker)) {
            this.lastHurtTick = this.tickCount;
        }
        return super.hurtServer(level, source, amount);
    }

    // ==================== tick ====================

    @Override
    public void tick() {
        this.updateNavigation();

        super.tick();

        if (this.attackCooldown > 0) this.attackCooldown--;

        if (this.level() instanceof ServerLevel serverLevel) {
            tickEatAnimation();
            handleRegularHungerDrain();
            handleAirSupply();
            handleSurfaceForAir();
            updateVisionRange();
            tickFollowMaster();

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && this.distanceTo(target) < 2.0D && this.attackCooldown <= 0) {
                doHurtTarget(serverLevel, target);
                this.attackCooldown = ATTACK_COOLDOWN_TICKS;
            }
        }
    }

    private void updateVisionRange() {
        if (this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16.0D + this.evolutionLevel * 2.0D);
        }
    }

    // ==================== 持久化 ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Kills", this.kills);
        output.putInt("RegularHunger", getRegularHunger());
        output.putInt("AirSupply", this.entityData.get(DATA_AIR_SUPPLY));
        output.putInt("CorpseHunger", this.corpseHunger);
        output.putInt("EvolutionLevel", this.evolutionLevel);
        if (this.masterUUID != null) {
            output.putString("MasterUUID", this.masterUUID.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.kills = input.getIntOr("Kills", this.kills);
        this.entityData.set(DATA_HUNGER, input.getIntOr("RegularHunger", getRegularHunger()));
        this.entityData.set(DATA_AIR_SUPPLY, input.getIntOr("AirSupply", MAX_AIR));
        this.corpseHunger = input.getIntOr("CorpseHunger", this.corpseHunger);
        this.evolutionLevel = Math.max(1, input.getIntOr("EvolutionLevel", this.evolutionLevel));
        input.getString("MasterUUID").ifPresent(uuid -> this.masterUUID = UUID.fromString(uuid));
    }

    // ==================== 主人系统 ====================

    public void setMaster(UUID masterUUID) {
        this.masterUUID = masterUUID;
    }

    public UUID getMasterUUID() {
        return this.masterUUID;
    }

    public boolean hasMaster() {
        return this.masterUUID != null;
    }

    // ==================== 交互 ====================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (feedFish(player.getItemInHand(hand))) {
            return InteractionResult.SUCCESS;
        }

        if (isMaster(player)) {
            if (!this.level().isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.corpseorigin.coco_zombie_entity.text_02", getEvolutionLevel(), this.kills, getCorpseHunger(), getRegularHunger()));
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    // ==================== 死亡掉落 ====================

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);

        if (this.random.nextFloat() < 0.4f) {
            this.spawnAtLocation(level, new ItemStack(Items.COD), 0.0f);
        }
    }

    // ==================== Getter ====================

    public int getKills() {
        return this.kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public boolean isPlayingEat() {
        return this.entityData.get(DATA_PLAYING_EAT);
    }

    public boolean isFusing() {
        return this.isFusing;
    }

    public boolean hasFused() {
        return this.hasFused;
    }

    public int getAirSupply() {
        return this.entityData.get(DATA_AIR_SUPPLY);
    }

    public int getMaxAir() {
        return MAX_AIR;
    }
}
