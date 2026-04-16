package com.phagens.corpseorigin.entity.Animals;

import com.phagens.corpseorigin.entity.CorpseHungerSystem;
import com.phagens.corpseorigin.entity.ICorpseBrother;
import com.phagens.corpseorigin.entity.ICorpseHunger;
import com.phagens.corpseorigin.register.Moditems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Random;

import static software.bernie.geckolib.animation.AnimationController.State.RUNNING;

public class ZbWormEntity extends PathfinderMob implements GeoEntity, ICorpseBrother, ICorpseHunger {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 动画定义
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack").thenLoop("idle");
    protected static final RawAnimation PARASITE_ANIM = RawAnimation.begin().thenPlay("parasite").thenLoop("idle");
    protected static final RawAnimation WALL_CLIMB_ANIM = RawAnimation.begin().thenLoop("wall_climb");

    private int attackCooldown = 0;
    private static final int ATTACK_COOLDOWN_TICKS = 20;

    // 跳跃攻击相关
    private boolean isJumpAttacking = false;
    private LivingEntity jumpTarget = null;
    private int jumpAttackTicks = 0;
    private static final int JUMP_ATTACK_DURATION = 10;
    private static final double JUMP_ATTACK_HORIZONTAL = 1.2D;
    private static final double JUMP_ATTACK_VERTICAL = 0.8D;

    // 反击标记
    private boolean isRetaliating = false;
    private LivingEntity attacker = null;

    private final CorpseHungerSystem hungerSystem = new CorpseHungerSystem(this);
    private final Random random = new Random();

    public ZbWormEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new com.phagens.corpseorigin.entity.EntityAI.JLAI.CorpseBrotherGatherGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new com.phagens.corpseorigin.entity.EntityAI.JLAI.CorpseBrotherHiveMindGoal(this));
        // 修改为只在被攻击时反击
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, this::shouldRetaliate));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.npc.Villager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 0, true, false, this::shouldAttackNonCorpseMob));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Animal.class, true));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, ZbWormEntity.class, 0, true, false, this::shouldAttackOtherCorpseEntity));
    }

    // 反击条件：只有被攻击时才反击
    private boolean shouldRetaliate(LivingEntity entity) {
        return isRetaliating && attacker == entity && hungerSystem.shouldAttackNonCorpsePlayer(entity);
    }

    private boolean shouldAttackNonCorpseMob(LivingEntity entity) {
        return hungerSystem.shouldAttackNonCorpseMob(entity);
    }

    private boolean shouldAttackOtherCorpseEntity(LivingEntity entity) {
        return hungerSystem.shouldAttackOtherCorpseEntity(entity);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 主控制器 - 根据状态切换动画
        controllers.add(new AnimationController<>(this, "controller", 5, this::predicate));

        // 攻击控制器 - 处理攻击动画（更高优先级）
        controllers.add(new AnimationController<>(this, "attackController", 0, this::attackPredicate));
    }

    private <T extends ZbWormEntity> PlayState predicate(AnimationState<T> animationState) {
        // 如果正在播放攻击动画，不干扰
        if (animationState.getController().getAnimationState() == RUNNING) {
            return PlayState.CONTINUE;
        }

        if (isJumpAttacking) {
            animationState.getController().setAnimation(PARASITE_ANIM);
        } else if (this.isSprinting() || this.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            animationState.getController().setAnimation(WALK_ANIM);
        } else {
            animationState.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }

    private <T extends ZbWormEntity> PlayState attackPredicate(AnimationState<T> animationState) {
        // 只在攻击或跳跃攻击时触发攻击动画
        if (attackCooldown > 0 && attackCooldown > ATTACK_COOLDOWN_TICKS - 5) {
            animationState.getController().setAnimation(ATTACK_ANIM);
            return PlayState.CONTINUE;
        }

        if (isJumpAttacking) {
            animationState.getController().setAnimation(PARASITE_ANIM);
            return PlayState.CONTINUE;
        }

        animationState.getController().stop();
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void tick() {
        super.tick();

        // 处理跳跃攻击
        if (isJumpAttacking && jumpTarget != null) {
            handleJumpAttack();
        } else if (isJumpAttacking) {
            // 目标消失，取消跳跃攻击
            isJumpAttacking = false;
            jumpTarget = null;
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // 重置反击标记（每2秒重置，避免永久敌对）
        if (isRetaliating && tickCount % 40 == 0 && this.getTarget() == null) {
            isRetaliating = false;
            attacker = null;
        }

        hungerSystem.tick();
    }

    private void handleJumpAttack() {
        if (jumpTarget == null || !jumpTarget.isAlive()) {
            isJumpAttacking = false;
            jumpTarget = null;
            return;
        }

        jumpAttackTicks++;

        if (jumpAttackTicks <= JUMP_ATTACK_DURATION) {
            // 计算跳跃方向
            Vec3 targetPos = jumpTarget.position();
            Vec3 currentPos = this.position();
            Vec3 direction = targetPos.subtract(currentPos).normalize();

            // 跳跃运动
            double progress = (double) jumpAttackTicks / JUMP_ATTACK_DURATION;
            double horizontalSpeed = JUMP_ATTACK_HORIZONTAL;
            double verticalSpeed = JUMP_ATTACK_VERTICAL;

            // 抛物线运动
            double x = direction.x * horizontalSpeed;
            double z = direction.z * horizontalSpeed;
            double y = verticalSpeed * (1 - Math.abs(progress * 2 - 1));

            this.setDeltaMovement(x, y, z);
            this.hasImpulse = true;

            // 到达目标时造成伤害
            if (this.distanceTo(jumpTarget) < 1.5 && canAttack()) {
                this.doHurtTarget(jumpTarget);
                setAttackCooldown();
                isJumpAttacking = false;
                jumpTarget = null;
                jumpAttackTicks = 0;
            }
        } else {
            // 跳跃攻击结束
            isJumpAttacking = false;
            jumpTarget = null;
            jumpAttackTicks = 0;
        }
    }

    // 尝试跳跃攻击
    private boolean tryJumpAttack(LivingEntity target) {
        if (isJumpAttacking) return false;
        if (!canAttack()) return false;

        // 50%几率跳跃攻击
        if (random.nextFloat() < 0.5f) {
            isJumpAttacking = true;
            jumpTarget = target;
            jumpAttackTicks = 0;
            return true;
        }
        return false;
    }

    public boolean canAttack() {
        return attackCooldown <= 0;
    }

    public void setAttackCooldown() {
        attackCooldown = ATTACK_COOLDOWN_TICKS;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // 记录被攻击的时间（用于反击逻辑）
        if (source.getEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) source.getEntity();
            hungerSystem.recordHurt();

            // 设置反击状态
            isRetaliating = true;
            this.attacker = attacker;

            // 设置攻击目标
            this.setTarget(attacker);

            // 50%几率尝试跳跃攻击到攻击者脸上
            tryJumpAttack(attacker);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (canAttack()) {
            boolean hurt = super.doHurtTarget(target);
            if (hurt) {
                setAttackCooldown();
            }
            return hurt;
        }
        return false;
    }

    /**
     * 玩家右键交互 - 抓取虫子
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            // 检查虫子是否还活着
            if (this.isAlive()) {
                // 检查玩家是否手持物品（如果有物品在手，优先处理物品交互）
                ItemStack handItem = player.getItemInHand(hand);
                if (!handItem.isEmpty()) {
                    return InteractionResult.PASS;
                }

                // 创建虫子物品
                ItemStack wormStack = new ItemStack(Moditems.ZB_WORM_ITEM.get());

                // 播放声音
                this.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 1.0F, 1.0F);

                // 给玩家物品
                if (!player.addItem(wormStack)) {
                    // 如果背包满了，掉落到地上
                    player.drop(wormStack, false);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你的背包满了，虫子掉到了地上！"));
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a你抓住了一只尸兄虫子！"));
                }

                // 移除实体
                this.discard();

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    // ICorpseBrother 接口实现
    @Override
    public boolean isCorpseBrotherOf(net.minecraft.world.entity.Mob entity) {
        return entity instanceof ICorpseBrother;
    }

    @Override
    public void setHiveMindTarget(LivingEntity target) {
        hungerSystem.setHiveMindTarget(target);
    }

    @Override
    public LivingEntity getHiveMindTarget() {
        return hungerSystem.getHiveMindTarget();
    }

    @Override
    public boolean hasAttackTarget() {
        return this.getTarget() != null;
    }

    @Override
    public int getEvolutionLevel() {
        return hungerSystem.getEvolutionLevel();
    }

    // ICorpseHunger 接口实现
    @Override
    public int getCorpseHunger() {
        return hungerSystem.getCorpseHunger();
    }

    @Override
    public int getTicksExisted() {
        return this.tickCount;
    }

    @Override
    public Level getLevel() {
        return this.level();
    }

    @Override
    public net.minecraft.core.BlockPos blockPosition() {
        return super.blockPosition();
    }

    @Override
    public boolean isAlive() {
        return super.isAlive();
    }

    @Override
    public void setCorpseHunger(int hunger) {
        hungerSystem.setCorpseHunger(hunger);
    }

    @Override
    public void setEvolutionLevel(int level) {
        hungerSystem.setEvolutionLevel(level);
    }

    // Getter/Setter
    public boolean isJumpAttacking() {
        return isJumpAttacking;
    }

    public boolean isRetaliating() {
        return isRetaliating;
    }
}