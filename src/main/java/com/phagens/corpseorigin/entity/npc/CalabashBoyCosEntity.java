package com.phagens.corpseorigin.entity.npc;

import com.phagens.corpseorigin.entity.LongyouEntity;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CalabashBoyCosEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");

    private int dialogueCooldown = 0;

    private static final String[] RANDOM_DIALOGUES = {
            "我这葫芦娃cos得怎么样？",
            "刚从漫展回来~",
            "这套衣服花了我不少钱呢。",
            "葫芦兄弟，合体！",
            "拍照可以，别发朋友圈。",
            "我这葫芦是真的道具！",
            "下次漫展我还穿这个。",
            "你看我这肌肉，练过的！"
    };

    public CalabashBoyCosEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
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

    private <E extends CalabashBoyCosEntity> software.bernie.geckolib.animation.PlayState controlAnimation(AnimationState<E> event) {
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
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
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
                player.sendSystemMessage(Component.literal("§e葫芦娃coser：§f" + dialogue));
            }
            dialogueCooldown = 400;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            String greeting = RANDOM_DIALOGUES[this.random.nextInt(RANDOM_DIALOGUES.length)];
            player.sendSystemMessage(Component.literal("§e葫芦娃coser：§f" + greeting));
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
}