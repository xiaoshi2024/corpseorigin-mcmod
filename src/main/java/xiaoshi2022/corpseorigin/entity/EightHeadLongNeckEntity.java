package xiaoshi2022.corpseorigin.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 八头长颈尸兄（{@code eight_head_long_neck}）—— 精神病院 BOSS。
 * <p>
 * 剧本桥段：主角团闯入精神病院第 2 关，这只 BOSS 被穆博士通过广播器操控，
 * 对主角团喊话（"你是不是在找一位红色头发的姑娘？"）。主角用脑筋急转弯让它
 * 陷入混乱（八个脑袋各说各的，互相矛盾），趁它被淦晕时集火。
 * <p>
 * 与 {@link MultiHeadCorpseWormEntity}（蜈蚣型多头）不同，这只 BOSS 是
 * {@link LongNeckZbEntity} 的进化版：一个巨型长颈 + 顶上 8 个脑袋（建模待补）。
 * 每个脑袋都能独立说话/回答，脑筋急转弯机制就是让 8 个脑袋答出 8 个不同答案，
 * 造成混乱眩晕。
 * <p>
 * 特殊机制：
 * <ul>
 *   <li>8 个脑袋：每颗脑袋有独立的台词/反应</li>
 *   <li>脑筋急转弯提问系统（服务端逻辑）：被玩家用正确的问句击中后
 *       进入 {@code STUNNED} 状态 6 秒，期间不能移动/攻击、受到双倍伤害</li>
 *   <li>穆博士通过广播器喊话（事件触发器，不在实体自身 tick 里）</li>
 * </ul>
 */
public class EightHeadLongNeckEntity extends PathfinderMob implements com.geckolib.animatable.GeoEntity, ZombieKin {

    // ==================== 阶段 ====================

    public static final byte PHASE_NORMAL = 0;      // 正常战斗状态
    public static final byte PHASE_STUNNED = 1;     // 被脑筋急转弯淦晕（8 个脑袋打架）

    // ==================== 同步数据 ====================

    private static final EntityDataAccessor<Byte> DATA_PHASE =
            SynchedEntityData.defineId(EightHeadLongNeckEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_STUN_TICKS =
            SynchedEntityData.defineId(EightHeadLongNeckEntity.class, EntityDataSerializers.INT);

    // ==================== 动画骨架（待建模后补 clip） ====================

    private static final com.geckolib.animation.RawAnimation IDLE =
            com.geckolib.animation.RawAnimation.begin().thenLoop("idle");
    private static final com.geckolib.animation.RawAnimation WALK =
            com.geckolib.animation.RawAnimation.begin().thenLoop("walk");
    private static final com.geckolib.animation.RawAnimation ATTACK =
            com.geckolib.animation.RawAnimation.begin().thenPlay("attack");
    private static final com.geckolib.animation.RawAnimation STUNNED =
            com.geckolib.animation.RawAnimation.begin().thenLoop("stunned");

    private final com.geckolib.animatable.instance.AnimatableInstanceCache cache =
            com.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    private ServerBossEvent bossEvent;

    /** 脑筋急转弯题库 —— 可以让 8 个脑袋混乱的问题 */
    private static final String[][] RIDDLES = {
            // { 脑筋急转弯（简短触发词）, 触发词1, 触发词2 }
            { "为什么葫芦娃的脑袋不一样大？", "葫芦娃", "脑袋", "大" },
            { "一只羊有几个角？", "羊", "角" },
            { "长颈鹿的脖子为什么那么长？", "长颈鹿", "脖子" },
    };

    public EightHeadLongNeckEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 60;
        this.setPersistenceRequired();
    }

    // ==================== 属性 ====================

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 240.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    // ==================== 同步数据 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, PHASE_NORMAL);
        builder.define(DATA_STUN_TICKS, 0);
    }

    public byte getPhase() {
        return entityData.get(DATA_PHASE);
    }

    public boolean isStunned() {
        return getPhase() == PHASE_STUNNED;
    }

    public int getStunTicks() {
        return entityData.get(DATA_STUN_TICKS);
    }

    // ==================== AI 目标 ====================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, true, false));
    }

    // ==================== 动画 ====================

    @Override
    public void registerControllers(com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new com.geckolib.animation.AnimationController<>("movement", 5, test -> {
            if (isStunned()) return test.setAndContinue(STUNNED);
            if (test.isMoving()) return test.setAndContinue(WALK);
            return test.setAndContinue(IDLE);
        }));
        controllers.add(new com.geckolib.animation.AnimationController<>("attack", 2, test -> {
            if (isStunned()) return com.geckolib.animation.object.PlayState.STOP;
            if (this.swinging) return test.setAndContinue(ATTACK);
            return com.geckolib.animation.object.PlayState.STOP;
        }));
    }

    @Override
    public com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ==================== Tick ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) this.level();

        // BOSS 血条
        if (this.bossEvent == null) {
            ServerBossEvent event = new ServerBossEvent(this.getUUID(),
                    Component.translatable("boss.corpseorigin.eight_head_long_neck"),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
            event.setProgress(1.0F);
            this.bossEvent = event;
        }
        this.bossEvent.setProgress(Math.max(0F, Math.min(1F,
                this.getHealth() / this.getMaxHealth())));

        // 被淦晕期间：不能动、不能攻击、减速归零
        if (isStunned()) {
            int ticks = entityData.get(DATA_STUN_TICKS);
            if (--ticks <= 0) {
                recoverFromStun(serverLevel);
            } else {
                entityData.set(DATA_STUN_TICKS, ticks);
                this.getNavigation().stop();
                this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            }
        }
    }

    // ==================== 脑筋急转弯机制 ====================

    /**
     * 检查玩家的聊天消息是否命中脑筋急转弯题库。
     * 命中则触发淦晕效果 —— 八个脑袋答出八个不同答案，互相矛盾。
     * <p>
     * 这是服务端逻辑，通常由聊天事件处理器触发（见 {@code ServerEvents} 或自定义事件）。
     *
     * @param player 提问的玩家
     * @param message 聊天消息内容
     * @return 是否命中（外部可据此发反馈）
     */
    public boolean tryAnswerRiddle(ServerPlayer player, String message) {
        if (isStunned()) return false;

        for (String[] riddle : RIDDLES) {
            for (int i = 1; i < riddle.length; i++) {
                if (message.contains(riddle[i])) {
                    stunByRiddle(player, riddle[0]);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 被脑筋急转弯淦晕：8 秒内不能动、受到双倍伤害。
     */
    private void stunByRiddle(ServerPlayer player, String riddle) {
        entityData.set(DATA_PHASE, PHASE_STUNNED);
        entityData.set(DATA_STUN_TICKS, 160); // 8 秒

        // 每个脑袋答一个不同的答案 —— 混乱效果
        String[] chaoticAnswers = {
                "八个脑袋同时爆炸！", "我不知道！你才不知道！", "等一下...等一下...",
                "葫芦？什么葫芦？", "我看到了...我看到了...", "别问我！别问我！",
                "我拒绝回答！", "救命啊——"
        };
        for (int i = 0; i < chaoticAnswers.length; i++) {
            // 逐个脑袋回答（客户端需要对应 8 个脑袋的话泡渲染，先留骨架）
            this.level().playSound(null, this.getX(), this.getY(1.5 + i * 0.15), this.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.HOSTILE, 0.6F, 1.0F + random.nextFloat() * 0.3F);
        }

        // 给提问玩家发反馈
        player.sendSystemMessage(Component.translatable(
                "message.corpseorigin.eight_head.stunned", riddle));
    }

    /**
     * 从混乱中恢复。
     */
    private void recoverFromStun(ServerLevel level) {
        entityData.set(DATA_PHASE, PHASE_NORMAL);
        entityData.set(DATA_STUN_TICKS, 0);
        this.heal(this.getMaxHealth() * 0.05F); // 恢复一点点血，给玩家"打醒了还挺硬"的感觉
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.VILLAGER_YES, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    // ==================== 伤害加倍（被淦晕时） ====================

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isStunned()) {
            amount *= 2.0F; // 淦晕期间双倍伤害
        }
        return super.hurtServer(level, source, amount);
    }

    // ==================== ZombieKin ====================

    @Override
    public boolean isHungry() {
        return false; // BOSS 不需要进食
    }

    // ==================== BOSS 血条 ====================

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.bossEvent != null) this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (this.bossEvent != null) this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
            this.bossEvent = null;
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
            this.bossEvent = null;
        }
    }
}
