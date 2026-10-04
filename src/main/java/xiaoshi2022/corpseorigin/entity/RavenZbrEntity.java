package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.registry.ModParticles;
import xiaoshi2022.corpseorigin.registry.ModSounds;

import java.util.EnumSet;
import java.util.List;

/**
 * 乌鸦尸兄（{@code raven_zbr}）——中立型尸兄，原型是原著里啄食尸体的黑乌鸦。
 * <p>
 * 平时是只普通的黑鸟：地面踱步（{@code idle}）、打盹（{@code eye_blink}，眼皮一开一合）、
 * 偶尔振翅绕圈（{@code fly}）。<b>只有饿了才会攻击玩家和村民</b>——饥饿值随时间累积，
 * 攒满后亮出喙（{@code attack}）主动猎食；啄中 3 口或直接咬死猎物后落地进食
 * （{@code feed}），吃饱恢复中立。
 * <p>
 * <b>叫声规则（{@code caw}）</b>：乌鸦在地上睁着眼时<b>绝对不会叫</b>——
 * 叫声只会出现在飞行途中，或打盹动画里眼皮闭合的安全窗口内
 * （服务端按 {@code eye_blink} 循环相位计算，留有余量防延迟错帧）。
 * <p>
 * 动画全部用上：idle / eye_blink / fly / attack / feed。
 */
public class RavenZbrEntity extends PathfinderMob implements GeoEntity, RealmRated {

    // ==================== 数值 ====================

    /** 饥饿攒满所需 tick（2 分钟不吃东西就饿） */
    public static final int HUNGER_MAX = 2400;
    /** 啄中一口抵掉的饥饿值（3 口就饱） */
    private static final int HUNGER_PER_PECK = HUNGER_MAX / 3;
    /** 进食时长（feed 动画 1.8s ×2 循环） */
    private static final int EAT_DURATION = 72;
    /** 打盹时长（eye_blink 动画 2.7917s ×2~3 循环） */
    private static final int DOZE_MIN = 112;
    private static final int DOZE_MAX = 168;
    /** 树上打盹时长（栖树小憩更久） */
    private static final int DOZE_TREE_MIN = 280;
    private static final int DOZE_TREE_MAX = 440;
    /** 飞行总时长上限（tick），超时强制找栖木降落 */
    private static final int FLY_MAX_TICKS = 400;
    /** 飞行推进速度 */
    private static final double FLY_SPEED = 0.32D;

    // ==================== 姿态状态（同步，驱动动画 + 叫声规则） ====================

    /** 地面清醒：睁眼 —— 不许叫 */
    public static final int STATE_AWAKE = 0;
    /** 地面打盹：eye_blink 循环，闭眼窗口内可叫 */
    public static final int STATE_DOZE = 1;
    /** 飞行：fly 循环，可叫 */
    public static final int STATE_FLY = 2;
    /** 进食：feed 循环 */
    public static final int STATE_EAT = 3;

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(RavenZbrEntity.class, EntityDataSerializers.INT);

    // ==================== 动画 ====================
    // 注意：raven_zbr.animation.json 里的 key 是 Bedrock 完整名（animation.raven_zbr.xxx），
    // 而非本项目其他文件常用的裸名（idle/attack），请求名必须与 JSON key 精确一致，否则报
    // "Unable to find animation"。

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.raven_zbr.idle");
    private static final RawAnimation ANIM_EYE_BLINK = RawAnimation.begin().thenLoop("animation.raven_zbr.eye_blink");
    private static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("animation.raven_zbr.fly");
    private static final RawAnimation ANIM_FEED = RawAnimation.begin().thenLoop("animation.raven_zbr.feed");
    private static final RawAnimation ANIM_ATTACK = RawAnimation.begin().thenPlay("animation.raven_zbr.attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ==================== 服务端状态 ====================

    /** 饥饿值：随时间累积，满则开始猎食玩家/村民 */
    private int hunger = 0;
    /** 打盹剩余 tick */
    private int dozeTimer = 0;
    /** 打盹已进行 tick（用于对齐 eye_blink 循环相位） */
    private int dozeElapsed = 0;
    /** 进食剩余 tick */
    private int eatTimer = 0;
    /** 飞行剩余 tick（本次飞行总预算，超时强制降落） */
    private int flyTimer = 0;
    /** 飞行目标点（当前航点） */
    private Vec3 flyTarget = null;
    /** 到达当前航点后还继续飞的剩余段数（0 = 到点后找栖木降落） */
    private int flyWaypoints = 0;
    /** 当前是否栖息在树叶上（影响小憩时长；读档重置） */
    private boolean perchedOnLeaves = false;
    /** 航点漂移倒计时（"飞行目标迷失"效果） */
    private int driftTimer = 0;
    /** 群聚警报冷却：受击广播同伴的间隔（防每刀刷屏） */
    private int alarmCooldown = 0;
    /** 俯冲阶段：0=爬升抢占上空 1=俯冲啄击 2=拉起脱离 */
    private int swoopPhase = 0;
    /** 当前俯冲阶段已进行 tick */
    private int swoopTimer = 0;
    /** 俯冲啄击冷却（每次啄击后拉起再俯冲） */
    private int swoopCooldown = 0;
    /** 战斗起飞叫的冷却（防 goal 重启循环导致鬼畜叫声） */
    private int battleCawCooldown = 0;
    /** 饿着啄饱/咬死猎物：待落地后开吃 */
    private boolean pendingFeast = false;
    /** 叫声倒计时（随机间隔） */
    private int cawTimer = 100;
    /** 掉羽毛倒计时（随机间隔，纯视觉效果） */
    private int featherTimer = 200;
    /** 打盹/清醒循环倒计时 */
    private int idleCycleTimer = 300;

    public RavenZbrEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.setPersistenceRequired();
    }

    // ==================== 属性 ====================

    /** 境界评级：人1（境界表绝对等级 1），Jade 准星联动显示 */
    private static final int REALM_LEVEL = 1;

    @Override
    public int corpseRealmLevel() {
        return REALM_LEVEL;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    // ==================== 同步数据 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, STATE_AWAKE);
    }

    public int getPostureState() {
        return this.entityData.get(DATA_STATE);
    }

    private void setPostureState(int state) {
        this.entityData.set(DATA_STATE, state);
    }

    public boolean isHungry() {
        return this.hunger >= HUNGER_MAX;
    }

    // ==================== AI 目标 ====================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 有目标（被激怒/饥饿）：起飞俯冲啄击
        this.goalSelector.addGoal(2, new SwoopAttackGoal());
        // 无目标：中立时偶尔起飞绕圈漫游
        this.goalSelector.addGoal(3, new FlyWanderGoal());
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // 中立但记仇：被打会还手（群聚警报见 hurtServer）
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // 饿了才猎食玩家和村民（谓词挡住不饿时的主动索敌）
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false, (target, lvl) -> this.isHungry()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this, Villager.class, 10, true, false, (target, lvl) -> this.isHungry()));
    }

    // ==================== 音效 ====================

    /**
     * 不覆写 {@code getAmbientSound}——原版环境音会在地上睁眼时随机叫，违反叫声规则。
     * caw 完全由服务端 tick 按"飞行 / 打盹闭眼窗口"手动触发（见 {@link #tryCaw}）。
     */
    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CAW;
    }

    @Override
    public SoundEvent getDeathSound() {
        return ModSounds.CAW;
    }

    @Override
    public float getSoundVolume() {
        return 1.0F;
    }

    @Override
    public float getVoicePitch() {
        return 1.1F;
    }

    // ==================== 存档 ====================

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Hunger", this.hunger);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.hunger = in.getIntOr("Hunger", 0);
        // 读档一律回到地面清醒态，避免悬空飞行态残留
        this.flyTimer = 0;
        this.flyTarget = null;
        this.flyWaypoints = 0;
        this.perchedOnLeaves = false;
        this.pendingFeast = false;
        this.swoopPhase = 0;
        this.dozeTimer = 0;
        this.eatTimer = 0;
        setPostureState(STATE_AWAKE);
        this.setNoGravity(false);
    }

    // ==================== 服务端主循环 ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();

        // 饥饿累积（进食状态不长）
        if (getPostureState() != STATE_EAT && this.hunger < HUNGER_MAX) {
            this.hunger++;
        }
        if (this.alarmCooldown > 0) {
            this.alarmCooldown--;
        }
        if (this.swoopCooldown > 0) {
            this.swoopCooldown--;
        }
        if (this.battleCawCooldown > 0) {
            this.battleCawCooldown--;
        }

        // 进食：定身吃完，恢复中立
        if (getPostureState() == STATE_EAT) {
            this.getNavigation().stop();
            if (--this.eatTimer <= 0) {
                this.hunger = 0;
                this.idleCycleTimer = this.random.nextInt(200) + 200;
                setPostureState(STATE_AWAKE);
            }
        }

        // 打盹计时
        if (getPostureState() == STATE_DOZE) {
            this.dozeElapsed++;
            if (--this.dozeTimer <= 0) {
                setPostureState(STATE_AWAKE);
                this.idleCycleTimer = this.random.nextInt(400) + 200;
            }
        }

        // 飞行：多段航点漫游 + 迷失漂移 + 降落栖木
        if (getPostureState() == STATE_FLY) {
            this.setNoGravity(true);
            if (this.flyTarget != null) {
                if (--this.flyTimer <= 0) {
                    // 飞累了：就近找栖木（树上优先）降落
                    beginDescent();
                } else {
                    // "目标迷失"：航点定期随机漂移，到处飞
                    if (--this.driftTimer <= 0) {
                        this.driftTimer = this.random.nextInt(40) + 30;
                        this.flyTarget = this.flyTarget.add(
                                (this.random.nextDouble() - 0.5D) * 6.0D,
                                (this.random.nextDouble() - 0.5D) * 4.0D,
                                (this.random.nextDouble() - 0.5D) * 6.0D);
                        keepWaypointClear();
                    }
                    // 撞墙/撞树冠：换向重选航点，别悬停硬撞
                    if (this.horizontalCollision && this.random.nextInt(3) == 0) {
                        pickNextWaypoint();
                    }
                    // 先判到点（到点即切换航段/降落），再推进——
                    // 顺序反了会被 steerFlight 置空的航点吞掉切换时机，导致悬停坠落
                    if (this.flyTarget != null
                            && this.distanceToSqr(this.flyTarget) < 4.0D) {
                        if (this.flyWaypoints > 0) {
                            this.flyWaypoints--;
                            pickNextWaypoint();
                        } else {
                            beginDescent();
                        }
                    }
                    if (this.flyTarget != null) {
                        steerFlight();
                    }
                }
            } else if (this.onGround()) {
                // 落到栖木（树顶/地面）收翅；落点决定小憩时长
                this.perchedOnLeaves = this.level().getBlockState(this.blockPosition().below())
                        .is(BlockTags.LEAVES);
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.4D, 0.0D, 0.4D));
                setPostureState(STATE_AWAKE);
                this.idleCycleTimer = this.perchedOnLeaves
                        ? this.random.nextInt(800) + 600
                        : this.random.nextInt(400) + 200;
                // 饿着啄饱/咬死猎物：落地后开吃
                if (this.pendingFeast) {
                    this.pendingFeast = false;
                    startEating();
                }
            }
        } else {
            this.setNoGravity(false);
        }

        // 中立时的作息：清醒一阵 → 打盹（饿着不睡；树上小憩更久）
        if (getPostureState() == STATE_AWAKE && !this.isHungry() && this.getTarget() == null) {
            if (--this.idleCycleTimer <= 0) {
                this.dozeElapsed = 0;
                setPostureState(STATE_DOZE);
                this.dozeTimer = this.perchedOnLeaves
                        ? this.random.nextInt(DOZE_TREE_MAX - DOZE_TREE_MIN) + DOZE_TREE_MIN
                        : this.random.nextInt(DOZE_MAX - DOZE_MIN) + DOZE_MIN;
            }
        }

        // 叫声规则：飞行可叫；打盹只在 eye_blink 闭眼窗口叫；地上睁眼绝不叫
        if (--this.cawTimer <= 0) {
            boolean cawed = tryCaw(level);
            // 打盹中没赶上闭眼窗口 → 10 tick 后再试（打盹一共才十几秒）
            this.cawTimer = !cawed && getPostureState() == STATE_DOZE ? 10
                    : this.random.nextInt(160) + 60;
        }

        // 随机掉羽毛：待机时偶尔飘一根黑羽（纯视觉效果）
        if (--this.featherTimer <= 0) {
            this.featherTimer = this.random.nextInt(200) + 100;
            dropFeatherParticles(level, 1);
        }
    }

    // ==================== 受击：掉羽毛 ====================

    /**
     * 受击反应：掉一撮黑羽 + 尖叫警报召集同伴群殴来犯者。
     * 乌鸦是群体动物——打一只，一片乌鸦一起扑你。
     * 警报只在<b>首击</b>（自己还没记仇时）发出：战斗中持续挨打不再反复叫/广播，
     * 否则贴脸互殴时每只乌鸦警报冷却一过就叫一轮，听感鬼畜。
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            dropFeatherParticles(level, this.isDeadOrDying() ? 12 : 3 + this.random.nextInt(3));
            if (!this.isDeadOrDying() && this.alarmCooldown <= 0 && this.getTarget() == null
                    && source.getEntity() instanceof LivingEntity attacker) {
                this.alarmCooldown = 100;
                alertAllies(level, attacker);
            }
        }
        return hurt;
    }

    /**
     * 尖叫警报：把 32 格内所有空闲同伴的仇恨都拉到攻击者身上。
     * 警报叫声属于战斗叫声，豁免"地面睁眼不叫"的日常规则；
     * 扫描带 32 格 AABB 上界（严禁全维度扫描）。
     */
    private void alertAllies(ServerLevel level, LivingEntity attacker) {
        caw(level);
        List<RavenZbrEntity> allies = level.getEntitiesOfClass(RavenZbrEntity.class,
                this.getBoundingBox().inflate(32.0D),
                raven -> raven != this && raven.isAlive() && raven.getTarget() == null);
        for (RavenZbrEntity ally : allies) {
            ally.setTarget(attacker);
            ally.alarmCooldown = 100;
        }
    }

    // ==================== 叫声 ====================

    /**
     * caw 只允许两类时机：
     * <ul>
     *   <li>飞行中（fly 循环）；</li>
     *   <li>打盹时处于 {@code eye_blink} 的<b>眼皮闭合</b>窗口内——按动画关键帧
     *       （循环 2.7917s，闭眼段 0~0.5s、1.2083~2.0833s、2.375s~循环尾）取中间安全带，
     *       留余量防止客户端延迟错帧，保证"睁眼不叫"。</li>
     * </ul>
     * 地面清醒（idle，睁眼）与进食状态直接跳过。
     *
     * @return 本次是否真的叫了
     */
    private boolean tryCaw(ServerLevel level) {
        int state = getPostureState();
        if (state == STATE_FLY) {
            caw(level);
            return true;
        }
        if (state == STATE_DOZE && inDozeClosedWindow(dozePhaseSeconds())) {
            caw(level);
            return true;
        }
        return false;
    }

    private void caw(ServerLevel level) {
        level.playSound(null, this.blockPosition(), ModSounds.CAW,
                SoundSource.NEUTRAL, this.getSoundVolume(), this.getVoicePitch());
        // 原版没有"黑色音符"渲染值（NOTE 颜色由坐标哈希决定，改不了色），黑烟最贴合乌鸦
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                this.getX(), this.getY() + this.getBbHeight() + 0.2D, this.getZ(),
                2, 0.15D, 0.05D, 0.15D, 0.005D);
    }

    /** 打盹动画当前相位（秒，0~2.7917 循环，按 dozeElapsed 对齐） */
    private double dozePhaseSeconds() {
        return (this.dozeElapsed / 20.0D) % 2.7917D;
    }

    /**
     * 是否处于 eye_blink 闭眼安全窗口（取闭眼段中间 70%，两端留 0.08s 余量）：
     * 0.10~0.40s 与 1.33~1.96s。
     */
    private static boolean inDozeClosedWindow(double phase) {
        return (phase >= 0.10D && phase <= 0.40D) || (phase >= 1.33D && phase <= 1.96D);
    }

    // ==================== 飞行 ====================

    /**
     * 朝当前航点推进：每帧直接设定速度（蝙蝠模式）。
     * 不能用 lerp 渐变——地面摩擦会拉锯，起飞阶段慢得像爬。
     */
    private void steerFlight() {
        Vec3 diff = this.flyTarget.subtract(this.position());
        this.setDeltaMovement(diff.normalize().scale(FLY_SPEED));
        this.getLookControl().setLookAt(
                this.flyTarget.x, this.flyTarget.y, this.flyTarget.z);
        if (diff.length() < 1.5D) {
            // 到点：收力滑落
            this.flyTarget = null;
            this.setNoGravity(false);
        }
    }

    // ==================== 飞行航点 ====================

    /** 随机选下一个航点：四处乱飞，高度避开地面与方块 */
    private void pickNextWaypoint() {
        ServerLevel level = (ServerLevel) this.level();
        double dist = 4.0D + this.random.nextDouble() * 7.0D;
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        int x = (int) Math.floor(this.getX() + Math.cos(angle) * dist);
        int z = (int) Math.floor(this.getZ() + Math.sin(angle) * dist);
        // 目标高度：当前位置随机上下浮动，但保持在地面 2 格以上
        double y = this.getY() + (this.random.nextDouble() - 0.4D) * 5.0D;
        y = Math.max(y, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 2.0D);
        y = Math.min(y, level.getMaxY() - 2.0D);
        this.flyTarget = new Vec3(x + 0.5D, y, z + 0.5D);
    }

    /** 航点漂移后兜底：确保航点在可通行空间，别钻进地里或卡进方块 */
    private void keepWaypointClear() {
        ServerLevel level = (ServerLevel) this.level();
        BlockPos pos = BlockPos.containing(this.flyTarget);
        if (!level.getBlockState(pos).canOcclude()) {
            return;
        }
        // 航点卡在实体方块里：抬高到地表上方
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        this.flyTarget = new Vec3(this.flyTarget.x, Math.max(surface + 2.0D, this.flyTarget.y), this.flyTarget.z);
    }

    /** 收翅降落：以当前位置正下方（树顶/地面）为栖木点 */
    private void beginDescent() {
        ServerLevel level = (ServerLevel) this.level();
        int x = (int) Math.floor(this.getX());
        int z = (int) Math.floor(this.getZ());
        // 水面上不降落：再飞一段换个地方试
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (level.getFluidState(new BlockPos(x, top - 1, z)).is(FluidTags.WATER)) {
            this.flyWaypoints = 1;
            this.flyTimer = Math.max(this.flyTimer, 100);
            pickNextWaypoint();
            return;
        }
        // 栖木点 = 地表/树顶站立面上方一点，到点后关力自然落到枝头
        this.flyTarget = new Vec3(x + 0.5D, top + 0.4D, z + 0.5D);
        this.flyWaypoints = 0;
        this.flyTimer = Math.max(this.flyTimer, 120);
    }

    /** 乌鸦振翅落地不受摔伤 */
    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    // ==================== 近战：啄食 ====================

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        triggerAnim("action", "attack");
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            level.sendParticles(ParticleTypes.CRIT,
                    living.getX(), living.getEyeY(), living.getZ(), 4, 0.2D, 0.2D, 0.2D, 0.1D);
            // 只有"饿着出门猎食"才走进食结算；被激怒的群殴纯伤害，不吃
            if (this.isHungry()) {
                if (living.isDeadOrDying()) {
                    // 咬死猎物：落地开吃
                    this.pendingFeast = true;
                } else {
                    // 啄一口抵一部分饥饿，3 口吃饱
                    this.hunger = Math.max(0, this.hunger - HUNGER_PER_PECK);
                    if (!this.isHungry()) {
                        this.pendingFeast = true;
                    }
                }
            }
        }
        return hit;
    }

    /** 落地进食：feed 循环、清空目标，吃完恢复中立 */
    private void startEating() {
        this.eatTimer = EAT_DURATION;
        this.getNavigation().stop();
        this.setTarget(null);
        setPostureState(STATE_EAT);
    }

    // ==================== 掉羽毛（粒子视觉效果） ====================

    /**
     * "随机掉羽毛"——纯视觉效果：乌鸦身上飘落黑色羽毛粒子（自定义粒子
     * {@code black_feather}，飘落/摇摆/自旋）。待机偶尔掉一根，受击掉一小撮，
     * 死亡爆一撮。
     */
    private void dropFeatherParticles(ServerLevel level, int count) {
        level.sendParticles(ModParticles.BLACK_FEATHER,
                this.getX(), this.getY() + this.getBbHeight() * 0.6D, this.getZ(),
                count, 0.25D, 0.15D, 0.25D, 0.02D);
    }

    // ==================== 死亡掉落 ====================

    /** 死亡随机掉落：腐肉 1~2 根 + 原版羽毛 0~2 根 */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(level, new ItemStack(Items.ROTTEN_FLESH,
                1 + this.random.nextInt(2)), 0.0F);
        int feathers = this.random.nextInt(3);
        if (feathers > 0) {
            this.spawnAtLocation(level, new ItemStack(Items.FEATHER, feathers), 0.0F);
        }
    }

    // ==================== 飞行漫游 ====================

    /**
     * 中立时偶尔起飞：直线飞向随机空中点，到点后收翅滑落。
     * 饿着 / 有目标 / 进食时不飞。
     */
    class FlyWanderGoal extends Goal {

        FlyWanderGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (getPostureState() != STATE_AWAKE || RavenZbrEntity.this.isHungry()
                    || RavenZbrEntity.this.getTarget() != null) {
                return false;
            }
            return RavenZbrEntity.this.random.nextInt(80) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            // 被打 / 变饿：立刻收翅去处理
            return getPostureState() == STATE_FLY && RavenZbrEntity.this.getTarget() == null;
        }

        @Override
        public void start() {
            ServerLevel level = (ServerLevel) RavenZbrEntity.this.level();
            flyTimer = FLY_MAX_TICKS;
            // 一次飞行漫游 2~4 段航点，到末段找栖木（树上优先）降落
            flyWaypoints = 1 + RavenZbrEntity.this.random.nextInt(3);
            driftTimer = RavenZbrEntity.this.random.nextInt(40) + 30;
            setPostureState(STATE_FLY);
            pickNextWaypoint();
            // 起飞关键：立即关重力 + 给足向上初速。
            // 否则站在地上时 travel() 用 0.546 地面摩擦，上浮被吃成龟速贴地蠕动。
            RavenZbrEntity.this.setNoGravity(true);
            RavenZbrEntity.this.setDeltaMovement(
                    RavenZbrEntity.this.getDeltaMovement().x, 0.4D,
                    RavenZbrEntity.this.getDeltaMovement().z);
            // 起飞抖毛 + 叫一声（飞行中允许叫）
            dropFeatherParticles(level, 4);
            caw(level);
        }

        @Override
        public void stop() {
            // 中断（被打/变饿）：收翅滑落
            if (getPostureState() == STATE_FLY) {
                flyTarget = null;
                RavenZbrEntity.this.setNoGravity(false);
            }
        }
    }

    /**
     * 俯冲啄击：有目标（被激怒/饥饿）时像真乌鸦一样起飞扑击——
     * 爬升抢占上空 → 俯冲啄一口 → 拉起脱离 → 再俯冲，循环到目标死亡/吃管够。
     * 目标消失后保持飞行态自然落地（落地是否进食由 pendingFeast 决定）。
     */
    private class SwoopAttackGoal extends Goal {

        SwoopAttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = RavenZbrEntity.this.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            // 地面（清醒/打盹）或漫游中被激怒（已在空中）都可发起
            int state = getPostureState();
            return state == STATE_AWAKE || state == STATE_DOZE || state == STATE_FLY;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = RavenZbrEntity.this.getTarget();
            return target != null && target.isAlive()
                    && getPostureState() == STATE_FLY && !pendingFeast
                    && flyTimer > 0;
        }

        @Override
        public void start() {
            flyTimer = FLY_MAX_TICKS;
            flyTarget = null;   // 俯冲由本 goal 每帧控速，不走漫游航点
            swoopPhase = 0;
            swoopTimer = 0;
            setPostureState(STATE_FLY);
            setNoGravity(true);
            // 起飞爬升初速（与漫游起飞同理：地面摩擦会吃掉缓慢加速）
            RavenZbrEntity.this.setDeltaMovement(
                    RavenZbrEntity.this.getDeltaMovement().x, 0.42D,
                    RavenZbrEntity.this.getDeltaMovement().z);
            // 战斗叫声（豁免"地面睁眼不叫"的日常规则），带冷却防 goal 重启循环鬼畜
            if (battleCawCooldown <= 0) {
                battleCawCooldown = 100;
                caw((ServerLevel) RavenZbrEntity.this.level());
            }
        }

        @Override
        public void stop() {
            swoopPhase = 0;
            // 保持 FLY 态与 noGravity，由实体 tick 的落栖木逻辑收尾
        }

        @Override
        public void tick() {
            LivingEntity target = RavenZbrEntity.this.getTarget();
            if (target == null) {
                return;
            }
            Vec3 want;
            switch (swoopPhase) {
                case 1 -> {
                    // 俯冲：直扑目标眼睛
                    Vec3 diff = target.getEyePosition().subtract(RavenZbrEntity.this.position());
                    if (diff.length() < 2.2D && swoopCooldown <= 0) {
                        RavenZbrEntity.this.doHurtTarget((ServerLevel) RavenZbrEntity.this.level(), target);
                        swoopCooldown = 20;
                        swoopPhase = 2;
                        swoopTimer = 0;
                    } else if (++swoopTimer > 60) {
                        // 一冲没咬到：拉起再来
                        swoopPhase = 2;
                        swoopTimer = 0;
                    }
                    want = diff.lengthSqr() < 1.0e-4D
                            ? Vec3.ZERO : diff.normalize().scale(0.42D);
                }
                case 2 -> {
                    // 拉起脱离：向上绕圈，避免贴脸互殴
                    want = new Vec3(
                            (RavenZbrEntity.this.random.nextDouble() - 0.5D) * 0.7D,
                            0.5D,
                            (RavenZbrEntity.this.random.nextDouble() - 0.5D) * 0.7D)
                            .normalize().scale(0.36D);
                    if (++swoopTimer > 25) {
                        swoopPhase = 1;
                        swoopTimer = 0;
                    }
                }
                default -> {
                    // 爬升：抢到目标上空 4 格左右再俯冲
                    Vec3 diff = new Vec3(
                            target.getX() - RavenZbrEntity.this.getX(),
                            (target.getY() + 4.5D) - RavenZbrEntity.this.getY(),
                            target.getZ() - RavenZbrEntity.this.getZ());
                    if (RavenZbrEntity.this.getY() > target.getY() + 3.5D || ++swoopTimer > 50) {
                        swoopPhase = 1;
                        swoopTimer = 0;
                    }
                    want = diff.lengthSqr() < 1.0e-4D
                            ? Vec3.ZERO : diff.normalize().scale(0.34D);
                }
            }
            if (RavenZbrEntity.this.horizontalCollision) {
                // 撞墙兜底：向上蹭过去
                want = want.add(0.0D, 0.12D, 0.0D);
            }
            RavenZbrEntity.this.setDeltaMovement(want);
            RavenZbrEntity.this.getLookControl().setLookAt(
                    target.getX(), target.getEyeY(), target.getZ());
            // 俯冲模式航点为空，实体 tick 不递减 flyTimer，这里自己管理总预算
            flyTimer--;
        }
    }

    // ==================== GeckoLib 动画 ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 姿态控制器：由同步状态驱动四个循环动画
        controllers.add(new AnimationController<>("movement", 4, state -> {
            return switch (getPostureState()) {
                case STATE_EAT -> state.setAndContinue(ANIM_FEED);
                case STATE_FLY -> state.setAndContinue(ANIM_FLY);
                case STATE_DOZE -> state.setAndContinue(ANIM_EYE_BLINK);
                default -> state.setAndContinue(ANIM_IDLE);
            };
        }));
        // 动作控制器：attack 一次性啄击
        controllers.add(new AnimationController<>("action", 0, state -> PlayState.CONTINUE)
                .triggerableAnim("attack", ANIM_ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
