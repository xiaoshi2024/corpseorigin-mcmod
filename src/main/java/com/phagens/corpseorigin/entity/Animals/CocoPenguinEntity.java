package com.phagens.corpseorigin.entity.Animals;

import com.phagens.corpseorigin.register.Moditems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CocoPenguinEntity extends Animal implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 动画
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("swim");
    protected static final RawAnimation EAT_ANIM = RawAnimation.begin().thenPlay("eat");

    // 饥饿系统
    private int hunger = 720;
    private static final int MAX_HUNGER = 720;
    private static final int HUNGER_DRAIN_INTERVAL = 1200;
    private int hungerDrainTimer = 0;

    public int eatCooldown = 0;
    private static final int EAT_COOLDOWN_TICKS = 80;

    // 攻击相关
    private int attackCooldown = 0;
    private static final int ATTACK_COOLDOWN_TICKS = 30;

    // 氧气
    private int airSupply = 300;
    private static final int MAX_AIR = 300;
    private static final int OXYGEN_WARNING_LEVEL = 30; // 剩余30才上浮
    private int surfaceCooldown = 0; // 上浮冷却，防止反复上浮

    public CocoPenguinEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
        // 降低水中浮力，不会自动上浮
        this.setNoGravity(false);
    }

    @Override
    protected void registerGoals() {
        // 注意：不要添加 FloatGoal！它会强制企鹅上浮
        // this.goalSelector.addGoal(0, new FloatGoal(this)); // 移除！不要使用

        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // 目标选择器
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, AbstractFish.class, 10, true, false, livingEntity ->
                livingEntity instanceof AbstractFish && !livingEntity.isBaby()
        ));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, ZbWormEntity.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CocoPenguinEntity>(this, "controller", 3, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationState<CocoPenguinEntity> animationState) {
        // 只有在陆地上且进食冷却期间才播放进食动画
        if (eatCooldown > 0 && !this.isInWater()) {
            animationState.setAnimation(EAT_ANIM);
            return PlayState.CONTINUE;
        }

        // 水里只播放游泳动画
        if (this.isInWater()) {
            animationState.setAnimation(SWIM_ANIM);
            return PlayState.CONTINUE;
        }

        // 陆地上移动时走路
        if (animationState.isMoving()) {
            animationState.setAnimation(WALK_ANIM);
            return PlayState.CONTINUE;
        }

        // 陆地待机
        animationState.setAnimation(IDLE_ANIM);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void tick() {
        super.tick();

        handleAirSupply();
        handleHungerDrain();

        if (eatCooldown > 0) eatCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (surfaceCooldown > 0) surfaceCooldown--;

        // 攻击逻辑
        if (this.getTarget() != null && this.getTarget().isAlive() && this.distanceTo(this.getTarget()) < 2.0D) {
            if (attackCooldown <= 0) {
                performAttack(this.getTarget());
            }
        }

        // 氧气不足时主动上浮（每20刻检测一次，避免频繁）
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            if (airSupply <= OXYGEN_WARNING_LEVEL && this.isInWater() && this.isUnderWater() && surfaceCooldown == 0) {
                surfaceForAir();
            }
        }

        // 让企鹅在水中缓慢下沉（模拟真实企鹅）
        if (this.isInWater() && this.isUnderWater() && !isSurfacing() && airSupply > OXYGEN_WARNING_LEVEL) {
            Vec3 vel = this.getDeltaMovement();
            // 轻微下沉，不会立即浮起
            if (vel.y > -0.1) {
                this.setDeltaMovement(vel.x, vel.y - 0.005, vel.z);
            }
        }
    }

    /**
     * 判断是否正在上浮
     */
    private boolean isSurfacing() {
        return surfaceCooldown > 0 && this.getDeltaMovement().y > 0;
    }

    /**
     * 主动上浮呼吸
     */
    private void surfaceForAir() {
        // 只给一次向上的力，不会持续上浮
        this.setDeltaMovement(this.getDeltaMovement().x, 0.4, this.getDeltaMovement().z);
        this.hasImpulse = true;
        surfaceCooldown = 60; // 3秒冷却，防止反复触发
    }

    private void handleAirSupply() {
        if (this.isInWater() && !this.isNoAi()) {
            if (this.isUnderWater()) {
                // 水下减少氧气
                if (airSupply > 0) {
                    airSupply--;
                }

                // 氧气耗尽后掉血
                if (airSupply <= 0) {
                    this.hurt(this.damageSources().drown(), 1.0F);
                }
            } else {
                // 水面呼吸恢复氧气（加快恢复）
                if (airSupply < MAX_AIR) {
                    airSupply = Math.min(MAX_AIR, airSupply + 12);
                }
            }
        } else {
            // 离开水中恢复氧气
            if (airSupply < MAX_AIR) {
                airSupply = Math.min(MAX_AIR, airSupply + 8);
            }
        }
    }

    private void handleHungerDrain() {
        if (this.level().isClientSide) return;

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
        if (target == null || attackCooldown > 0) return;

        target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.0F);
        attackCooldown = ATTACK_COOLDOWN_TICKS;

        // 判断目标是否死亡
        boolean isDead = !target.isAlive();

        if (isDead) {
            int hungerRestore = 20;

            // 根据目标类型决定恢复量
            if (target instanceof Salmon) hungerRestore = 30;
            else if (target instanceof Cod) hungerRestore = 25;
            else if (target instanceof TropicalFish) hungerRestore = 15;
            else if (target instanceof Pufferfish) hungerRestore = 10;
            else if (target instanceof ZbWormEntity) hungerRestore = 30;

            addHunger(hungerRestore);

            // 只在陆地上触发进食动画
            if (!this.isInWater()) {
                triggerEatAnimation();
            }

            // 如果是吃了虫子，尝试感染
            if (target instanceof ZbWormEntity && !this.level().isClientSide) {
                if (this.random.nextFloat() < 0.3f) {  // 30% 概率感染
                    convertToZombie();
                }
            }

            this.setTarget(null);
        }
    }

    /**
     * 将普通企鹅转化为尸兄企鹅
     */
    private void convertToZombie() {
        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        // 检查是否已经死亡或即将被移除
        if (!this.isAlive()) return;

        // 获取实体类型（需要确保已注册）
        EntityType<?> zombieType = com.phagens.corpseorigin.register.EntityRegistry.COCO_ZOMBIE.get();
        if (zombieType == null) {
            com.phagens.corpseorigin.CorpseOrigin.LOGGER.error("COCO_ZOMBIE 实体未注册，无法转化！");
            return;
        }

        // 创建尸兄企鹅实体
        CocoZombieEntity zombieEntity = new CocoZombieEntity(
                (EntityType<? extends CocoPenguinEntity>) zombieType,
                serverLevel
        );

        // 复制位置和旋转
        zombieEntity.setPos(this.getX(), this.getY(), this.getZ());
        zombieEntity.setYRot(this.getYRot());
        zombieEntity.setXRot(this.getXRot());
        zombieEntity.setYHeadRot(this.getYHeadRot());

        // 复制重要的NBT数据
        CompoundTag nbt = new CompoundTag();
        this.saveWithoutId(nbt);  // 保存当前实体数据

        // 清理不需要复制的数据
        nbt.remove("UUID");
        nbt.remove("Pos");
        nbt.remove("Motion");
        nbt.remove("Rotation");
        nbt.remove("FallDistance");
        nbt.remove("HurtTime");
        nbt.remove("HurtByTimestamp");
        nbt.remove("DeathTime");
        nbt.remove("Health");  // 尸兄企鹅有独立的生命值

        // 应用NBT到新实体
        zombieEntity.readAdditionalSaveData(nbt);

        // 设置尸兄企鹅的特有属性
        zombieEntity.addHunger(this.getHunger());  // 继承饥饿值
        zombieEntity.setHealth(zombieEntity.getMaxHealth());  // 满血转化

        // 播放转化音效
        this.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.0F, 1.0F);
        zombieEntity.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1.0F, 1.0F);

        // 添加转化粒子效果
        if (serverLevel.isClientSide) {
            for (int i = 0; i < 30; i++) {
                serverLevel.addParticle(
                        net.minecraft.core.particles.ParticleTypes.SMOKE,
                        this.getX(), this.getY() + 1, this.getZ(),
                        (this.random.nextDouble() - 0.5D) * 0.5D,
                        this.random.nextDouble() * 0.5D,
                        (this.random.nextDouble() - 0.5D) * 0.5D
                );
            }
        }

        // 将新实体添加到世界
        serverLevel.addFreshEntity(zombieEntity);

        // 移除旧实体
        this.discard();

        com.phagens.corpseorigin.CorpseOrigin.LOGGER.info(
                "企鹅 {} 在位置 ({}, {}, {}) 转化为尸兄企鹅",
                this.getName().getString(),
                this.getX(), this.getY(), this.getZ()
        );
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (isFood(itemStack)) {
            if (feedItem(itemStack)) {
                return InteractionResult.SUCCESS;
            }
        }

        return super.mobInteract(player, hand);
    }

    public boolean feedFish(ItemStack stack) {
        if (!canEat()) return false;

        Item item = stack.getItem();
        int restoreAmount;

        if (item == Items.SALMON) {
            restoreAmount = 40;
        } else if (item == Items.COD) {
            restoreAmount = 35;
        } else if (item == Items.TROPICAL_FISH) {
            restoreAmount = 25;
        } else if (item == Items.PUFFERFISH) {
            restoreAmount = 15;
        } else {
            return false;
        }

        addHunger(restoreAmount);

        // 只在陆地上触发进食动画
        if (!this.isInWater()) {
            triggerEatAnimation();
        }

        if (!this.level().isClientSide) {
            stack.shrink(1);
        }

        return true;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.COD || item == Items.SALMON ||
                item == Items.TROPICAL_FISH || item == Moditems.ZB_WORM_ITEM.get();
    }

    public boolean feedItem(ItemStack stack) {
        if (!canEat()) return false;

        Item item = stack.getItem();
        int restoreAmount = 0;

        if (item == Moditems.ZB_WORM_ITEM.get()) {
            restoreAmount = 30;
            // 可选：感染判定
            if (!this.level().isClientSide && this.random.nextFloat() < 0.3f) {
                convertToZombie();
            }
        } else if (item == Items.SALMON) {
            restoreAmount = 40;
        } else if (item == Items.COD) {
            restoreAmount = 35;
        } else if (item == Items.TROPICAL_FISH) {
            restoreAmount = 25;
        } else if (item == Items.PUFFERFISH) {
            restoreAmount = 15;
        }

        if (restoreAmount > 0) {
            addHunger(restoreAmount);
            triggerEatAnimation();
            if (!this.level().isClientSide) stack.shrink(1);
            return true;
        }
        return false;
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

    public int getHunger() { return hunger; }
    public int getMaxHunger() { return MAX_HUNGER; }
    public int getAirSupply() { return airSupply; }
    public int getMaxAir() { return MAX_AIR; }

    @Override
    protected int increaseAirSupply(int currentAir) {
        return currentAir;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Hunger", hunger);
        compound.putInt("AirSupply", airSupply);
        compound.putInt("EatCooldown", eatCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        hunger = compound.getInt("Hunger");
        airSupply = compound.getInt("AirSupply");
        eatCooldown = compound.getInt("EatCooldown");
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            // 正常水中移动
            this.moveRelative(0.04F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            // 水中阻力，让游泳更自然
            this.setDeltaMovement(this.getDeltaMovement().scale(0.96D));
        } else {
            super.travel(travelVector);
        }
    }
}