package com.phagens.corpseorigin.entity.npc;

import com.phagens.corpseorigin.entity.Animals.CocoPenguinEntity;
import com.phagens.corpseorigin.entity.Animals.ZbWormEntity;
import com.phagens.corpseorigin.entity.LongyouEntity;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class UncleEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    protected static final RawAnimation FLEE_ANIM = RawAnimation.begin().thenPlay("flee");
    protected static final RawAnimation OPEN_ANIM = RawAnimation.begin().thenPlay("open");
    protected static final RawAnimation DROP_ANIM = RawAnimation.begin().thenPlay("drop");

    private static final EntityDataAccessor<Integer> DATA_ANIMATION_STATE = SynchedEntityData.defineId(UncleEntity.class, EntityDataSerializers.INT);

    private static final int OPEN_ANIM_DURATION = 58;
    private static final int FLEE_ANIM_DURATION = 20;
    private static final int DROP_ANIM_DURATION = 40;

    private int dialogueCooldown = 0;
    private int animationTimer = 0;
    private boolean isWormExposed = false;
    private boolean hasDroppedWorm = false;

    private static final String[] RANDOM_DIALOGUES = {
            "少女的内心，你懂吗？",
            "我的漫画可是一等奖！",
            "Coco别乱跑...",
            "这个月的稿费...",
            "平果4S就是我的命！",
            "为了艺术献身！",
            "你看过《鑫瓶梅》吗？"
    };

    public UncleEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANIMATION_STATE, 0);
    }

    public int getAnimationState() {
        return this.entityData.get(DATA_ANIMATION_STATE);
    }

    public void setAnimationState(int state) {
        this.entityData.set(DATA_ANIMATION_STATE, state);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));

        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LowerLevelZbEntity.class, 10.0F, 1.2D, 1.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LongyouEntity.class, 15.0F, 1.5D, 2.0D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 1.2D, 1.5D));

        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));

        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));

        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, 0, true, false,
                entity -> entity instanceof Monster && !(entity instanceof LowerLevelZbEntity) && !(entity instanceof LongyouEntity)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ATTACK_SPEED, 1.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::controlAnimation));
    }

    private <E extends UncleEntity> software.bernie.geckolib.animation.PlayState controlAnimation(AnimationState<E> event) {
        int state = getAnimationState();
        if (state == 1) {
            return event.setAndContinue(OPEN_ANIM);
        }
        if (state == 2) {
            return event.setAndContinue(FLEE_ANIM);
        }
        if (state == 3) {
            return event.setAndContinue(DROP_ANIM);
        }

        if (this.getAttackAnim(event.getPartialTick()) > 0) {
            return event.setAndContinue(ATTACK_ANIM);
        }

        if (event.isMoving()) {
            return event.setAndContinue(WALK_ANIM);
        }

        return event.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !hasDroppedWorm && source.getDirectEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();
            if (weapon.is(Items.STICK)) {
                triggerOpenAnimation();
            }
        }
        return super.hurt(source, amount);
    }

    public void triggerOpenAnimation() {
        if (this.level().isClientSide) return;
        if (getAnimationState() != 0) return;

        setAnimationState(1);
        this.animationTimer = OPEN_ANIM_DURATION;
        this.isWormExposed = true;

        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0D))) {
            player.sendSystemMessage(Component.literal("§e大叔：§f别...别看！"));
        }
    }

    private void spawnZbWorm() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        this.hasDroppedWorm = true;

        EntityType<ZbWormEntity> wormType = EntityRegistry.ZB_WORM.get();
        ZbWormEntity worm = new ZbWormEntity(wormType, serverLevel);
        worm.setPos(this.getX(), this.getY() + 0.5, this.getZ());
        serverLevel.addFreshEntity(worm);

        for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0D))) {
            player.sendSystemMessage(Component.literal("§e大叔：§f一只尸兄虫子从体内钻出！"));
        }

        var nearbyPenguins = serverLevel.getEntitiesOfClass(CocoPenguinEntity.class,
                this.getBoundingBox().inflate(20.0D));

        for (CocoPenguinEntity penguin : nearbyPenguins) {
            penguin.setTarget(worm);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (animationTimer > 0) {
                animationTimer--;
                if (animationTimer <= 0) {
                    if (getAnimationState() == 1) {
                        setAnimationState(2);
                        animationTimer = FLEE_ANIM_DURATION;
                    } else if (getAnimationState() == 2) {
                        setAnimationState(3);
                        animationTimer = DROP_ANIM_DURATION;
                        spawnZbWorm();
                    } else if (getAnimationState() == 3) {
                        setAnimationState(0);
                    }
                }
            }

            if (dialogueCooldown > 0) {
                dialogueCooldown--;
            } else if (this.tickCount % 200 == 0 && this.random.nextFloat() < 0.1F) {
                sayRandomDialogue();
            }
        }
    }

    private void sayRandomDialogue() {
        if (!this.level().isClientSide && RANDOM_DIALOGUES.length > 0) {
            String dialogue = RANDOM_DIALOGUES[this.random.nextInt(RANDOM_DIALOGUES.length)];
            for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0D))) {
                player.sendSystemMessage(Component.literal("§e大叔：§f" + dialogue));
            }
            dialogueCooldown = 400;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (itemStack.is(Items.STICK) && !hasDroppedWorm) {
            if (!this.level().isClientSide) {
                triggerOpenAnimation();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (!this.level().isClientSide) {
            String greeting = RANDOM_DIALOGUES[this.random.nextInt(RANDOM_DIALOGUES.length)];
            player.sendSystemMessage(Component.literal("§e大叔：§f" + greeting));
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public boolean isWormExposed() {
        return isWormExposed;
    }

    public boolean hasDroppedWorm() {
        return hasDroppedWorm;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.isWormExposed = tag.getBoolean("WormExposed");
        this.hasDroppedWorm = tag.getBoolean("HasDroppedWorm");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("WormExposed", this.isWormExposed);
        tag.putBoolean("HasDroppedWorm", this.hasDroppedWorm);
    }
}