package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.block.MosquitoCoilBlock;
import xiaoshi2022.corpseorigin.registry.ModBlocks;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.registry.ModItems;

import java.util.List;
import java.util.UUID;

/**
 * 蚊子尸兄（{@code mosquito_zbr}）—— <b>蚊群核心</b> BOSS，"一个 Boss，多个判定"的载体。
 * <p>
 * <b>本体</b>：0.5 × 0.5 的小碰撞箱 + 0.34 的飞行速度（无重力 {@link #travel} 直推），
 * 围绕目标高速环绕游走，极难命中；独立血量 160，挂在屏幕上方的 BOSS 血条上。
 * <p>
 * <b>蚊群</b>：子实体 {@link MosquitoSwarmEntity} 的数量随血量线性减少——满血 16 只、
 * 濒死只剩 3 只；本体每被结算一次伤害，多消散 {@code 伤害/15} 只蚊子（"主实体受伤时，
 * 子实体同步消散"）。叮咬判定全在子实体上（低伤害、无视无敌帧、多只叠签持续掉血）。
 * <p>
 * <b>产卵</b>：战斗中（有目标）每 {@link #EGG_INTERVAL} tick 在附近地面产一枚
 * 蚊子尸兄卵（{@link xiaoshi2022.corpseorigin.block.MosquitoEggsBlock}），卵孵化出新蚊子
 * 补充蚊群——玩家只顾输出不清卵，场面会失控。
 * <p>
 * <b>蚊香克制</b>：进入烟雾范围移速大幅降低、持续消散蚊群，并刻意往远离蚊香的方向游走；
 * 蚊香由玩家放置（{@link xiaoshi2022.corpseorigin.block.MosquitoCoilBlock}），限时燃尽，
 * 蚊子还会去加速烧毁它，逼玩家不断换位。
 * <p>
 * 动画用 {@code mosquito_zbr} geo 资源（idle / fly / suck / look）：
 * 移动播 fly、悬停播 idle、贴近目标"吸血"播 suck。
 */
public class MosquitoZbrEntity extends PathfinderMob implements GeoEntity {

    // ==================== 数值 ====================

    /** 满血时环绕蚊子数量上限 */
    public static final int MAX_SWARM = 16;
    /** 濒死时保底的蚊子数量 */
    public static final int MIN_SWARM = 3;
    /** 补员节奏：每几 tick 至多补一只（防止一帧内刷出一团） */
    public static final int SWARM_REPLENISH_INTERVAL = 8;
    /** 产卵间隔（tick）：约 8 秒一枚 */
    public static final int EGG_INTERVAL = 160;
    /** 战场附近卵的数量上限（超过就不再产，防止把地图铺满） */
    public static final int MAX_EGGS_NEARBY = 6;

    // ==================== 同步数据 ====================

    private static final EntityDataAccessor<Byte> DATA_SWARM_COUNT =
            SynchedEntityData.defineId(MosquitoZbrEntity.class, EntityDataSerializers.BYTE);

    // ==================== 动画 ====================

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation SUCK_ANIM = RawAnimation.begin().thenLoop("suck");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ServerBossEvent bossEvent;

    public MosquitoZbrEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        this.setPersistenceRequired();
        this.setNoGravity(true);
    }

    // ==================== 属性 ====================

    /**
     * 160 血、0.34 飞行速度（人跑步约 0.28，追不上也难以瞄准）、视力 48 格。
     * 本体攻击力没意义——伤害全在蚊子叮咬上。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 160.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    // ==================== 同步数据 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SWARM_COUNT, (byte) 0);
    }

    public byte getSwarmCount() {
        return this.entityData.get(DATA_SWARM_COUNT);
    }

    private void setSwarmCount(byte count) {
        this.entityData.set(DATA_SWARM_COUNT, count);
    }

    // ==================== 受伤 → 蚊群同步消散 ====================

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            // 主实体受伤 → 子实体同步消散（伤害越高散得越多，配合数量公式再自然衰减）
            int extra = Math.min(getSwarmCount(), Math.round(amount / 15.0F));
            if (extra > 0) {
                List<MosquitoSwarmEntity> swarm = listSwarm(level);
                for (int i = 0; i < extra && i < swarm.size(); i++) {
                    MosquitoSwarmEntity m = swarm.get(i);
                    level.sendParticles(ParticleTypes.POOF,
                            m.getX(), m.getY() + 0.1D, m.getZ(), 3, 0.1D, 0.1D, 0.1D, 0.01D);
                    m.discard();
                }
            }
        }
        return hurt;
    }

    // ==================== AI 目标 ====================

    @Override
    protected void registerGoals() {
        // 目标选择照常走 Goal；移动与攻击在 tick 里手推（飞行、环绕、产卵）
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, false));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    // ==================== 服务端主循环 ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();

        // BOSS 血条
        if (this.bossEvent == null) {
            this.bossEvent = new ServerBossEvent(this.getUUID(),
                    Component.translatable("boss.corpseorigin.mosquito_zbr"),
                    BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
            this.bossEvent.setProgress(1.0F);
        }
        this.bossEvent.setProgress(Math.max(0F, Math.min(1F,
                this.getHealth() / this.getMaxHealth())));

        // 每 10 tick 做一次"重活"：蚊群清点/补员、烟雾判定、产卵
        if (this.tickCount % 10 == 0) {
            tickSwarmAndSmoke(level);
        }
        if (this.tickCount % EGG_INTERVAL == 0) {
            tryLayEggs(level);
        }

        // 移动手推：围绕目标环绕游走（本体很小很快，难以命中）
        steer(level);
    }

    /** 蚊群清点 + 数量公式 + 烟雾惩罚 + 补员 */
    private void tickSwarmAndSmoke(ServerLevel level) {
        List<MosquitoSwarmEntity> swarm = listSwarm(level);

        // 烟雾判定：核心在烟雾里 → 移速大减（steer 里读）、持续消散蚊群
        boolean inSmoke = MosquitoCoilBlock.isSmokeAround(level, this.blockPosition(), 4);
        if (inSmoke && !swarm.isEmpty()) {
            MosquitoSwarmEntity m = swarm.get(this.getRandom().nextInt(swarm.size()));
            level.sendParticles(ParticleTypes.POOF,
                    m.getX(), m.getY() + 0.1D, m.getZ(), 3, 0.1D, 0.1D, 0.1D, 0.01D);
            m.discard();
            swarm.remove(m);
        }

        // 数量公式：随血量线性下降（满血 16 → 濒死 3）
        float hpRatio = Math.max(0F, this.getHealth() / this.getMaxHealth());
        int target = Math.max(MIN_SWARM, (int) Math.ceil(MAX_SWARM * hpRatio));
        if (swarm.size() > target) {
            for (int i = swarm.size() - 1; i >= target; i--) {
                MosquitoSwarmEntity m = swarm.get(i);
                level.sendParticles(ParticleTypes.POOF,
                        m.getX(), m.getY() + 0.1D, m.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.01D);
                m.discard();
            }
            swarm.subList(target, swarm.size()).clear();
        } else if (swarm.size() < target && this.tickCount % (SWARM_REPLENISH_INTERVAL * 10) == 0) {
            spawnSwarmOne(level);
        }
        setSwarmCount((byte) swarm.size());
    }

    /** 补一只环绕蚊子（在核心身旁生成） */
    private void spawnSwarmOne(ServerLevel level) {
        MosquitoSwarmEntity mosquito = ModEntities.MOSQUITO_SWARM.create(level, EntitySpawnReason.EVENT);
        if (mosquito == null) return;
        double angle = this.getRandom().nextDouble() * Math.PI * 2;
        mosquito.setPos(this.getX() + Math.cos(angle), this.getY() + 0.3D,
                this.getZ() + Math.sin(angle));
        mosquito.setYRot(this.getYRot());
        mosquito.setSwarmOwner(this.getUUID());
        level.addFreshEntity(mosquito);
    }

    /** 收集归属自己的蚊子（32 格局部扫描，非全局） */
    private List<MosquitoSwarmEntity> listSwarm(ServerLevel level) {
        return level.getEntitiesOfClass(MosquitoSwarmEntity.class,
                this.getBoundingBox().inflate(32.0D),
                m -> m.isAlive() && this.getUUID().equals(m.getSwarmOwner()));
    }

    /** 战斗中产卵：往目标附近的地面放一枚卵（有数量上限） */
    private void tryLayEggs(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;
        if (countNearbyEggs(level) >= MAX_EGGS_NEARBY) return;

        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = this.getRandom().nextDouble() * Math.PI * 2;
            double dist = 2.0D + this.getRandom().nextDouble() * 5.0D;
            BlockPos pos = BlockPos.containing(
                    this.getX() + Math.cos(angle) * dist,
                    this.getY() - 0.5D,
                    this.getZ() + Math.sin(angle) * dist);
            // 往下找地面（最多 4 格），往上也容许一小段
            pos = findGround(level, pos);
            if (pos == null) continue;
            BlockState below = level.getBlockState(pos.below());
            if (!below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) continue;
            if (!level.getBlockState(pos).canBeReplaced()) continue;
            level.setBlock(pos, ModBlocks.MOSQUITO_ZBR_EGGS.defaultBlockState(), 3);
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 0.8F, 0.6F);
            return;
        }
    }

    /** 战场 12 格半径内的卵数（10s 一次、范围有限，无全局扫描） */
    private int countNearbyEggs(ServerLevel level) {
        BlockPos center = this.blockPosition();
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-12, -6, -12), center.offset(12, 6, 12))) {
            if (level.getBlockState(p).is(ModBlocks.MOSQUITO_ZBR_EGGS)) count++;
        }
        return count;
    }

    /** 从给定点附近找可以放卵的空气位（向下优先，最多 4 格） */
    private static BlockPos findGround(ServerLevel level, BlockPos pos) {
        BlockPos p = pos;
        for (int i = 0; i < 5; i++) {
            if (level.getBlockState(p).canBeReplaced()
                    && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) {
                return p;
            }
            p = p.below();
        }
        return null;
    }

    // ==================== 移动 ====================

    /** 环绕游走：目标身边 1.5~2.5 格快速转圈；烟雾里减速 70% 且往外飘 */
    private void steer(ServerLevel level) {
        LivingEntity target = this.getTarget();
        Vec3 desired;
        double speedScale = 1.0D;

        // 烟雾：移速大减 + 被推离蚊香
        var smoke = MosquitoCoilBlock.findSmoke(level, this.blockPosition(), 4);
        if (smoke != null) {
            speedScale = 0.3D;
            Vec3 away = this.position().subtract(Vec3.atCenterOf(smoke));
            if (away.lengthSqr() > 1.0E-4D) {
                desired = this.position().add(away.normalize().scale(4.0D));
                pushTowards(desired, 0.8D);
                return;
            }
        }

        if (target != null && target.isAlive()) {
            double angle = this.tickCount * 0.14D;
            double radius = 1.5D + Math.sin(this.tickCount * 0.05D) * 0.8D;
            desired = target.getEyePosition()
                    .add(Math.cos(angle) * radius, 0.6D, Math.sin(angle) * radius);
        } else {
            // 没目标：在目标玩家附近徘徊（48 格视线内最近玩家）
            Player near = level.getNearestPlayer(this, 24.0D);
            Vec3 center = near != null ? near.position() : this.position();
            double angle = this.tickCount * 0.03D;
            desired = center.add(Math.cos(angle) * 4.0D, 1.5D, Math.sin(angle) * 4.0D);
        }
        pushTowards(desired, speedScale);
    }

    /** 朝期望点推速度（保留惯性 → 高速环绕、急转弯有小弧线） */
    private void pushTowards(Vec3 desired, double speedScale) {
        Vec3 push = desired.subtract(this.position());
        if (push.lengthSqr() > 1.0E-4D) {
            push = push.normalize().scale(0.34D * speedScale);
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.72D).add(push));
    }

    /** 无重力飞行（同 {@link MosquitoSwarmEntity#travel}） */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.85D));
            this.calculateEntityAnimation(false);
        }
    }

    // ==================== BOSS 血条 ====================

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.bossEvent != null) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (this.bossEvent != null) {
            this.bossEvent.removePlayer(player);
        }
    }

    // ==================== 生命周期 ====================

    /** 死亡：血条清掉，蚊群陪葬 */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.bossEvent != null) {
            this.bossEvent.removeAllPlayers();
            this.bossEvent = null;
        }
        if (!this.level().isClientSide()) {
            for (MosquitoSwarmEntity m : listSwarm((ServerLevel) this.level())) {
                m.discard();
            }
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

    // ==================== 死亡掉落 ====================

    /** 必定（100%）掉落 2 个「蚊香」——击杀蚊群核心即拿到下一场的破局道具 */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(level, new ItemStack(ModItems.MOSQUITO_COIL, 2), 0.0F);
    }

    // ==================== GeckoLib 动画 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 4, this::movementController));
    }

    /** 移动：有速度播 fly、悬停播 idle、贴着目标"吸血"播 suck */
    private PlayState movementController(AnimationTest<MosquitoZbrEntity> test) {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.distanceToSqr(target) < 6.25D) {
            return test.setAndContinue(SUCK_ANIM);
        }
        if (this.getDeltaMovement().horizontalDistanceSqr() > 0.004D) {
            return test.setAndContinue(FLY_ANIM);
        }
        return test.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
