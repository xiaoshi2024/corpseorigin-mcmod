package com.phagens.corpseorigin.entity.npc;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.LongyouEntity;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

import java.util.UUID;

/**
 * 开胃奶NPC实体类（人类态）
 * 白小飞的远方表弟，H大学学生，少年志愿反扒队队长
 * 特征：红色头发，刘海向右撇，穿红色衣服，戴眼镜
 */
public class KaiWeiNaiEntity extends PathfinderMob implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // 动画定义
    protected static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    protected static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    protected static final RawAnimation SCARED_ANIM = RawAnimation.begin().thenLoop("scared");

    // 跟随系统
    private UUID followingPlayerUUID = null;
    private static final double FOLLOW_RANGE = 8.0D;
    private static final double TELEPORT_RANGE = 32.0D;
    private int followCooldown = 0;

    // 战斗/逃跑状态
    private boolean isScared = false;
    private int scaredCooldown = 0;

    // 对话系统
    private int dialogueCooldown = 0;
    private static final String[] RANDOM_DIALOGUES = {
        "表哥在哪里...",
        "这里好可怕...",
        "我饿了...",
        "有没有吃的？",
        "尸兄来了快跑！",
        "我是影帝！",
        "龙爪手！",
        "小飞表哥救我！"
    };

    // 交易相关
    private int tradeCooldown = 0;
    private boolean hasTradedToday = false;

    public KaiWeiNaiEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        // 优先级1：逃跑（如果害怕）
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D) {
            @Override
            public boolean canUse() {
                return isScared || super.canUse();
            }
        });

        // 优先级2：躲避尸兄
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LowerLevelZbEntity.class, 12.0F, 1.2D, 1.5D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LongyouEntity.class, 20.0F, 1.5D, 2.0D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 1.2D, 1.5D));

        // 优先级3：跟随玩家
        this.goalSelector.addGoal(2, new FollowPlayerGoal(this, 1.0D, 3.0F, 16.0F));

        // 优先级4：近战攻击（只有在不害怕且目标较弱时）
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                return !isScared && super.canUse();
            }
        });

        // 优先级5：随机移动
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));

        // 优先级6：看向玩家
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // 目标选择：攻击较弱的敌对生物
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, 0, true, false,
                entity -> entity instanceof Monster && !(entity instanceof LowerLevelZbEntity) && !(entity instanceof LongyouEntity)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)  // 比玩家少一点
                .add(Attributes.MOVEMENT_SPEED, 0.35D)  // 跑得比普通人快一点（逃跑用）
                .add(Attributes.ATTACK_DAMAGE, 2.0D)  // 攻击力较弱
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ATTACK_SPEED, 1.0D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, this::controlAnimation));
    }

    private <E extends KaiWeiNaiEntity> software.bernie.geckolib.animation.PlayState controlAnimation(AnimationState<E> event) {
        // 如果害怕，播放害怕动画
        if (isScared) {
            return event.setAndContinue(SCARED_ANIM);
        }

        // 如果在攻击
        if (this.getAttackAnim(event.getPartialTick()) > 0) {
            return event.setAndContinue(ATTACK_ANIM);
        }

        // 如果在逃跑（快速移动）
        if (isScared || this.getDeltaMovement().lengthSqr() > 0.08) {
            return event.setAndContinue(RUN_ANIM);
        }

        // 普通移动
        if (event.isMoving()) {
            return event.setAndContinue(WALK_ANIM);
        }

        // 默认待机动画
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
            // 更新跟随逻辑
            tickFollowPlayer();

            // 更新害怕状态
            if (scaredCooldown > 0) {
                scaredCooldown--;
                if (scaredCooldown <= 0) {
                    isScared = false;
                }
            }

            // 检查附近是否有尸兄，有的话进入害怕状态
            checkForZombies();

            // 随机对话
            if (dialogueCooldown > 0) {
                dialogueCooldown--;
            } else if (this.tickCount % 200 == 0 && this.random.nextFloat() < 0.1F) {
                sayRandomDialogue();
            }

            // 交易冷却
            if (tradeCooldown > 0) {
                tradeCooldown--;
            }
        }
    }

    /**
     * 检查附近是否有尸兄
     */
    private void checkForZombies() {
        if (this.level() instanceof ServerLevel serverLevel) {
            // 检查附近10格内是否有尸兄
            var nearbyZombies = serverLevel.getEntitiesOfClass(LowerLevelZbEntity.class,
                    this.getBoundingBox().inflate(10.0D));

            if (!nearbyZombies.isEmpty() && !isScared) {
                isScared = true;
                scaredCooldown = 200; // 害怕10秒

                // 对附近玩家喊话
                for (Player player : serverLevel.getEntitiesOfClass(Player.class,
                        this.getBoundingBox().inflate(16.0D))) {
                    player.sendSystemMessage(Component.literal("§e开胃奶：§f尸兄来了！快跑啊！"));
                }
            }
        }
    }

    /**
     * 跟随玩家的tick逻辑
     */
    private void tickFollowPlayer() {
        if (followingPlayerUUID == null) return;
        if (followCooldown > 0) {
            followCooldown--;
            return;
        }

        if (this.tickCount % 10 != 0) return;

        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Player player = serverLevel.getServer().getPlayerList().getPlayer(followingPlayerUUID);
        if (player == null || !player.isAlive()) {
            followingPlayerUUID = null;
            return;
        }

        double distanceToPlayer = this.distanceToSqr(player);

        // 太远就传送
        if (distanceToPlayer > TELEPORT_RANGE * TELEPORT_RANGE) {
            teleportToPlayer(player);
            return;
        }

        // 超出跟随范围就移动
        if (distanceToPlayer > FOLLOW_RANGE * FOLLOW_RANGE) {
            this.getNavigation().moveTo(player, 1.0D);
            followCooldown = 20;
        }
    }

    /**
     * 传送到玩家身边
     */
    private void teleportToPlayer(Player player) {
        for (int i = 0; i < 10; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double distance = 2 + this.random.nextDouble() * 2;
            double targetX = player.getX() + Math.cos(angle) * distance;
            double targetZ = player.getZ() + Math.sin(angle) * distance;
            double targetY = player.getY();

            if (this.level().noCollision(this.getBoundingBox().move(targetX - this.getX(), targetY - this.getY(), targetZ - this.getZ()))) {
                this.teleportTo(targetX, targetY, targetZ);
                this.getNavigation().stop();
                followCooldown = 40;
                break;
            }
        }
    }

    /**
     * 随机说话
     */
    private void sayRandomDialogue() {
        String dialogue = RANDOM_DIALOGUES[this.random.nextInt(RANDOM_DIALOGUES.length)];
        Component message = Component.literal("§e开胃奶：§f" + dialogue);

        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0D))) {
            player.sendSystemMessage(message);
        }

        dialogueCooldown = 400; // 20秒冷却
    }

    /**
     * 玩家交互
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        // 只在服务端处理交互逻辑，避免客户端和服务端都发送消息
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack itemStack = player.getItemInHand(hand);

        // Shift+右键：切换跟随/停止跟随
        if (player.isShiftKeyDown()) {
            if (followingPlayerUUID != null && followingPlayerUUID.equals(player.getUUID())) {
                followingPlayerUUID = null;
                player.sendSystemMessage(Component.literal("§e开胃奶：§f那我自己走了，表哥..."));
            } else {
                followingPlayerUUID = player.getUUID();
                player.sendSystemMessage(Component.literal("§e开胃奶：§f表哥！我跟着你！"));
            }

            // 播放音效
            if (this.level() instanceof ServerLevel level) {
                level.playSound(null, this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.VILLAGER_YES,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        // 普通右键：对话或交易
        if (itemStack.is(Items.EMERALD) || itemStack.is(Items.BREAD) || itemStack.is(Items.COOKED_BEEF)) {
            // 给食物，增加好感
            if (!player.isCreative()) {
                itemStack.shrink(1);
            }
            this.heal(4.0F);
            player.sendSystemMessage(Component.literal("§e开胃奶：§f谢谢表哥！我正好饿了！"));

            // 播放吃东西音效
            if (this.level() instanceof ServerLevel level) {
                level.playSound(null, this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.GENERIC_EAT,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        // 普通对话
        String[] greetings = {
            "表哥！你终于来了！",
            "这里好可怕，我们快走吧！",
            "你有吃的吗？我饿死了...",
            "我是影帝开胃奶！",
            "小心尸兄！它们无处不在！"
        };
        String greeting = greetings[this.random.nextInt(greetings.length)];
        player.sendSystemMessage(Component.literal("§e开胃奶：§f" + greeting));

        // 显示当前状态
        if (followingPlayerUUID != null && followingPlayerUUID.equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§7[当前状态: 跟随你] (Shift+右键取消跟随)"));
        } else {
            player.sendSystemMessage(Component.literal("§7[当前状态: 独立行动] (Shift+右键让他跟随)"));
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 受到伤害时的反应
     */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && !this.level().isClientSide) {
            // 进入害怕状态
            isScared = true;
            scaredCooldown = 100; // 害怕5秒

            // 对附近玩家求救
            for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0D))) {
                player.sendSystemMessage(Component.literal("§e开胃奶：§f救命啊！我被攻击了！"));
            }
        }

        return result;
    }

    @Override
    protected void actuallyHurt(net.minecraft.world.damagesource.DamageSource damageSrc, float damageAmount) {
        // 如果被尸兄攻击，有概率被感染（后续实现）
        if (damageSrc.getEntity() instanceof LowerLevelZbEntity) {
            CorpseOrigin.LOGGER.info("开胃奶被尸兄攻击，有感染风险！");
            // TODO: 后续实现感染系统
        }
        super.actuallyHurt(damageSrc, damageAmount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (followingPlayerUUID != null) {
            compound.putUUID("FollowingPlayer", followingPlayerUUID);
        }
        compound.putBoolean("IsScared", isScared);
        compound.putInt("ScaredCooldown", scaredCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("FollowingPlayer")) {
            followingPlayerUUID = compound.getUUID("FollowingPlayer");
        }
        if (compound.contains("IsScared")) {
            isScared = compound.getBoolean("IsScared");
        }
        if (compound.contains("ScaredCooldown")) {
            scaredCooldown = compound.getInt("ScaredCooldown");
        }
    }

    // ==================== 自定义跟随目标Goal ====================

    private static class FollowPlayerGoal extends Goal {
        private final KaiWeiNaiEntity entity;
        private final double speedModifier;
        private final float stopDistance;
        private final float startDistance;
        private Player player;

        public FollowPlayerGoal(KaiWeiNaiEntity entity, double speedModifier, float stopDistance, float startDistance) {
            this.entity = entity;
            this.speedModifier = speedModifier;
            this.stopDistance = stopDistance;
            this.startDistance = startDistance;
        }

        @Override
        public boolean canUse() {
            if (entity.followingPlayerUUID == null) return false;
            if (!(entity.level() instanceof ServerLevel serverLevel)) return false;

            this.player = serverLevel.getServer().getPlayerList().getPlayer(entity.followingPlayerUUID);
            if (player == null || !player.isAlive()) {
                entity.followingPlayerUUID = null;
                return false;
            }

            double distance = entity.distanceToSqr(player);
            return distance > stopDistance * stopDistance && distance < startDistance * startDistance;
        }

        @Override
        public boolean canContinueToUse() {
            if (entity.followingPlayerUUID == null) return false;
            if (!entity.getNavigation().isInProgress()) return false;
            if (player == null || !player.isAlive()) return false;

            return entity.distanceToSqr(player) > stopDistance * stopDistance;
        }

        @Override
        public void start() {
            if (player != null) {
                entity.getNavigation().moveTo(player, speedModifier);
            }
        }

        @Override
        public void stop() {
            entity.getNavigation().stop();
            player = null;
        }

        @Override
        public void tick() {
            if (player != null && entity.tickCount % 10 == 0) {
                entity.getNavigation().moveTo(player, speedModifier);
            }
        }
    }
}
