package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

import java.util.EnumSet;
import java.util.UUID;

/**
 * 尸蛟龙（{@code zuo_flood_long}）—— 左护法用「脱离」把身上那条蛟龙蜕下来之后，
 * 它就成了一只独立的宠物 BOSS，听从本体号令。
 * <p>
 * 原著设定：左护法与一条青龙合体成尸兄，作为「青龙」是尸王四大神宠之一；
 * 这里把它拆成"人形 + 蛟龙"两份，人形负责打，蛟龙放出来当打手。
 * <p>
 * 行为（服务端）：
 * <ul>
 *   <li><b>听从本体</b>：跟着主人跑，<b>主人砍谁它就咬谁</b>（哪怕是同类尸兄、甚至尸王），
 *       其次才咬刚打主人的家伙；护主时会自己冲上去；</li>
 *   <li><b>只认主人</b>：唯一不能碰的是主人本人（主人打它也不掉血）；</li>
 *   <li><b>永不自行消散</b>：只跟在本维度里，主人离太远（>64 格）会自己盘回身边；
 *       主人掉线 / 退出游戏 / 死亡 / 换角色 / 跑去别的维度<b>都不会消失</b> ——
 *       实体随区块存盘，主人回到同维度再上线会自动认主。它只有两条正常消失途径：
 *       自己被打死，或主人用「合体」技能把它收回身上（{@code MergeGuardianSkill} 主动 discard）；</li>
 *   <li>主人自己打它不掉血（射线也跳过它），免得误伤自己的宠物。</li>
 * </ul>
 * 模型直接用它的专属资源 {@code zuo_flood_long}（纯龙、没有骑手骨），
 * 和玩家蛟龙形态那套 {@code zuo_guardian}（龙 + 骑手）分开，各用各的。
 */
public class ZuoFloodLongEntity extends PathfinderMob implements GeoEntity, ZombieKin, OwnerBound {

    /** 主人 UUID（同步给客户端：本人的客户端要跳过自己宠物的箱子；归属判定也靠它） */
    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(ZuoFloodLongEntity.class, EntityDataSerializers.STRING);

    /** 超过这个距离就盘回主人身边 */
    private static final double RECALL_DISTANCE = 64.0;
    /** 到这个距离以内就停下 */
    private static final double NEAR_DISTANCE = 12.0;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    /** 这套模型没有 walk —— 移动借 reptile（蛇在爬） */
    private static final RawAnimation MOVE_ANIM = RawAnimation.begin().thenLoop("reptile");
    /** 泡在水里换成游动（原著里它本来就是与青龙合体的蛟龙，属水） */
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("swim");
    //这里动画应该是eat！
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("eat");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 主人实体引用；只在服务端有值，主人掉线后失效，再上线时按 {@link #ownerUuid} 重新认主 */
    @Nullable
    private Player owner;

    /** 主人 UUID：随实体一起存进 NBT —— 退游戏再上来蛟龙还在，就靠它把主人找回来 */
    @Nullable
    private UUID ownerUuid;

    public ZuoFloodLongEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
        this.setPersistenceRequired();

        // ★ 水生物（原著里它就是与青龙合体的蛟龙），但<b>不换移动控制</b>：
        //   SmoothSwimmingMoveControl 那套是给纯水生生物（海豚）用的 —— 陆地分支只有
        //   "当前速度 × outsideWaterSpeedModifier"（海豚给 0.1，上岸只剩一成速度），
        //   而且没有跳台阶的逻辑，宠物上岸就成了原地挪、跟不上主人。
        //   原版的水陆两栖范例是海龟：默认移动控制 + 两栖寻路 + 水下移动效率拉满 —— 照它来。
        // 水里/水边对寻路不扣分（默认对陆生生物是 8.0，会绕着水走）
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);

        // 体型走 SCALE 属性：碰撞箱（LivingEntity 按属性算）+ 渲染（GeckoLib 会乘上 render state 的 scale）
        // 一起缩，不会出现"看着缩小了、还是按巨物判定"的错位
        AttributeInstance scale = this.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(CorpseConfig.get().mutantBody.scale);
            this.refreshDimensions();
        }
    }

    /** 水陆两栖寻路：水里能追、上岸也能追（默认的陆生寻路一进水就"没路了"） */
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    /** 蛟龙属水，泡多久都不掉氧 */
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    // ==================== 属性 ====================

    /** 神宠级别的面板：比普通尸兄厚得多，但比尸王本体弱一线；水下移动效率拉满（水生物） */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0D);
    }

    // ==================== 同步数据 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // ⚠️ 这一句不能省：LivingEntity / Mob 各自声明了同步字段（血量、AI 标志等），
        //    它们全靠这条 super 链去填。漏掉的话 Builder.build() 一上来就抛
        //    "Entity class ... has not defined synched data value N"（实体造不出来，技能一放就崩）。
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, "");
    }

    public void setOwner(Player owner) {
        this.owner = owner;
        if (owner != null) {
            this.ownerUuid = owner.getUUID();
            this.entityData.set(DATA_OWNER_UUID, owner.getUUID().toString());
        }
    }

    /** 主人（服务端用；客户端没有实体引用，只有同步下来的 UUID） */
    @Nullable
    public Player ownerPlayer() {
        if (this.owner != null && !this.owner.isRemoved() && this.owner.isAlive()) {
            return this.owner;
        }
        return null;
    }

    @Override
    public boolean isOwnedBy(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (this.owner != null && this.owner == entity) {
            return true;
        }
        String uuid = this.entityData.get(DATA_OWNER_UUID);
        return !uuid.isEmpty() && uuid.equals(entity.getUUID().toString());
    }

    /** 主人身边（半径内）的那条蛟龙；没有就返回 {@code null}（"脱离 / 合体"技能用） */
    @Nullable
    public static ZuoFloodLongEntity findOwned(Player owner, double radius) {
        java.util.List<ZuoFloodLongEntity> dragons = owner.level().getEntitiesOfClass(
                ZuoFloodLongEntity.class,
                owner.getBoundingBox().inflate(radius),
                dragon -> dragon.isOwnedBy(owner));
        return dragons.isEmpty() ? null : dragons.get(0);
    }

    // ==================== GeckoLib ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 5, this::movementController));
        controllers.add(new AnimationController<>("attack", 2, this::attackController));
    }

    /** 动画：泡在水里播 swim，陆地移动播 reptile（这套模型没有 walk），静止播 idle */
    private PlayState movementController(AnimationTest<ZuoFloodLongEntity> test) {
        if (this.isInWater()) {
            return test.setAndContinue(SWIM_ANIM);
        }
        return test.setAndContinue(test.isMoving() ? MOVE_ANIM : IDLE_ANIM);
    }

    private PlayState attackController(AnimationTest<ZuoFloodLongEntity> test) {
        return this.swinging ? test.setAndContinue(ATTACK_ANIM) : PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== AI ====================

    @Override
    protected void registerGoals() {
        // 注意：<b>不挂 FloatGoal</b> —— 那个 goal 只要泡进水里就会一直往上浮，
        // 水生物（原版海豚同理）挂上它就没法潜下去追人了。它本来就不掉氧，不需要浮头换气。
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal());
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new AssistOwnerGoal());
    }

    /** 跟随主人：离得远就走过去（再远由 {@link #tick} 直接盘回） */
    private class FollowOwnerGoal extends Goal {

        private int recalcCooldown;

        FollowOwnerGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Player owner = ownerPlayer();
            return owner != null && distanceToSqr(owner) > NEAR_DISTANCE * NEAR_DISTANCE;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = ownerPlayer();
            return owner != null && distanceToSqr(owner) > 6.0 * 6.0 && !getNavigation().isDone();
        }

        @Override
        public void start() {
            this.recalcCooldown = 0;
        }

        @Override
        public void tick() {
            Player owner = ownerPlayer();
            if (owner == null) {
                return;
            }
            if (--this.recalcCooldown <= 0) {
                this.recalcCooldown = 10;
                getNavigation().moveTo(owner, 1.25D);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /** 护主：主人刚打的人、以及刚打主人的人 */
    private class AssistOwnerGoal extends TargetGoal {

        @Nullable
        private LivingEntity candidate;

        AssistOwnerGoal() {
            super(ZuoFloodLongEntity.this, false);
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = ownerPlayer();
            if (owner == null) {
                return false;
            }
            // 优先打主人亲手打的目标（用户要求：主人打谁它就打谁），其次才替主人报仇
            LivingEntity target = owner.getLastHurtMob();
            if (target == null || !target.isAlive()) {
                target = owner.getLastHurtByMob();
            }
            if (target == null || !target.isAlive() || target == owner || target == ZuoFloodLongEntity.this) {
                return false;
            }
            if (!canAttackTarget(target)) {
                return false;
            }
            this.candidate = target;
            return true;
        }

        @Override
        public void start() {
            ZuoFloodLongEntity.this.setTarget(this.candidate);
            super.start();
        }
    }

    /**
     * 唯一的禁忌：不能咬主人本人（引用 + UUID 双保险，重连后引用刷新期间也挡得住）。
     * 除此之外<b>任何目标都打</b> —— 不管是不是同类 / 尸兄 / 尸王，主人砍谁它就咬谁。
     */
    private boolean canAttackTarget(LivingEntity target) {
        return target != this.owner && !isOwnedBy(target);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof LivingEntity living && !canAttackTarget(living)) {
            return false;
        }
        return super.doHurtTarget(level, target);
    }

    // ==================== 生命周期 ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }

        Player owner = ownerPlayer();
        if (owner == null) {
            // 缓存的主人实体引用失效了（掉线 / 区块重载后实体引用还没接上）：
            // 拿存档 UUID 去在线玩家里重新认主
            ServerPlayer online = this.ownerUuid == null ? null
                    : ((ServerLevel) this.level()).getServer().getPlayerList().getPlayer(this.ownerUuid);
            if (online == null) {
                // 主人真的掉线 / 退游戏了：原地守候（实体随区块存盘，不会消失；AI 目标全空，自然待机）
                return;
            }
            setOwner(online);
            owner = online;
        }

        // 主人在别的维度：留在原维度守候，不跨维度传送（跨维度 distanceToSqr 无意义，
        // 直接 teleportTo 会把蛟龙拽进别的维度坐标）；主人回到本维度后自动恢复跟随。
        // ★ 任何情况下都不在此 discard —— 蛟龙只有"被打死"一条消失途径。
        if (owner.level() != this.level()) {
            return;
        }

        // 跟丢了就盘回主人身边（原版宠物那套手感）
        if (distanceToSqr(owner) > RECALL_DISTANCE * RECALL_DISTANCE) {
            this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            this.getNavigation().stop();
        }
    }

    /** 主人打自己宠物不掉血 */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Player owner = this.owner;
        if (owner != null && (source.getEntity() == owner || source.getDirectEntity() == owner)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    /**
     * 本人的客户端跳过自己宠物的箱子（否则宠物挡在身前时一刀都砍不到敌人）。
     * 服务端恒 true —— 别人该能打得到这只 BOSS。
     */
    @Override
    public boolean isPickable() {
        return !this.level().isClientSide() || !corpseorigin$isLocalPlayersPet();
    }

    @Environment(EnvType.CLIENT)
    private boolean corpseorigin$isLocalPlayersPet() {
        String uuid = this.entityData.get(DATA_OWNER_UUID);
        Player local = Minecraft.getInstance().player;
        return local != null && !uuid.isEmpty() && uuid.equals(local.getUUID().toString());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;   // 消散与否则由 tick 里的"主人有效性"决定
    }

    @Override
    public boolean shouldBeSaved() {
        return true;   // 随区块存盘：主人退出游戏后蛟龙留在原地，再上线还在
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.ownerUuid = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        if (this.ownerUuid != null) {
            // 同步串也补回去：客户端"跳过自己宠物碰撞箱"和 findOwned 在重新认主前都靠它
            this.entityData.set(DATA_OWNER_UUID, this.ownerUuid.toString());
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (this.ownerUuid != null) {
            output.store("Owner", UUIDUtil.CODEC, this.ownerUuid);
        }
    }

    /** 免掉"尸族不敢动你"那套之外的多余交互 */
    @Override
    public boolean is(Entity entity) {
        return this == entity || this.owner == entity;
    }
}
