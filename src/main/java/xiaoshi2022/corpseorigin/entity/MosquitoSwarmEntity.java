package xiaoshi2022.corpseorigin.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.block.MosquitoCoilBlock;

import java.util.UUID;

/**
 * 蚊子尸兄的<b>环绕蚊子（蚊群子实体）</b>——攻击判定真正挂载的地方。
 * <p>
 * 设计（对应"一个 Boss，多个判定"）：
 * <ul>
 *   <li>本体极小（0.2 × 0.2 碰撞箱）、血量极低（4 点），一巴掌就能拍死一只；</li>
 *   <li>叮咬伤害极低（1 点 = 半颗心），但<b>清零目标的 {@code invulnerableTime}</b>，
 *       即完全无视受伤无敌帧——多只蚊子贴脸时就是每 tick 持续掉血，玩家无法靠
 *       单次受击后的无敌帧逃脱；</li>
 *   <li>围绕蚊群核心（{@link MosquitoZbrEntity}）盘旋飞行：核心有目标就跟着冲目标，
 *       没有目标就绕核心转圈，视觉上是一团会移动的黑云；</li>
 *   <li>进入<b>蚊香烟雾</b>（{@link MosquitoCoilBlock#isSmokeAround}）后每秒掉血，
 *       濒死时还会去叮咬蚊香加速它烧完（蚊群会试图摧毁蚊香）；</li>
 *   <li>{@code noSave}：不写进存档——核心会自动补员，蚊子丢了无所谓；</li>
 *   <li>生命周期 {@link #LIFETIME}：野生蚊子（从卵孵出但附近没有核心）也会过期消散，
 *       防止无限堆积。</li>
 * </ul>
 * 移动方式与 {@link VampireBatEntity} 同款：{@code customServerAiStep} 里直接
 * {@code setDeltaMovement} 朝目标方向推进，{@link #travel} 关闭重力按当前速度滑行。
 */
public class MosquitoSwarmEntity extends PathfinderMob implements GeoEntity {

    /** 存活时长（tick）：核心每几秒补一只新的，旧的自然过期；野生蚊子同理防堆积 */
    public static final int LIFETIME = 2400;
    /** 叮咬冷却（tick），按实体 id 错开让不同蚊子不同相位咬人 */
    public static final int BITE_COOLDOWN = 12;
    /** 单次叮咬伤害：极低，但无视无敌帧 */
    public static final float BITE_DAMAGE = 1.0F;

    /** 蚊群核心的 UUID；null = 野生蚊子（从卵孵出、附近没有核心） */
    private UUID swarmOwner;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MosquitoSwarmEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    // ==================== 属性 ====================

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // ==================== 归属 ====================

    public void setSwarmOwner(UUID owner) {
        this.swarmOwner = owner;
    }

    public UUID getSwarmOwner() {
        return this.swarmOwner;
    }

    // ==================== 服务端行为 ====================

    @Override
    protected void customServerAiStep(ServerLevel level) {
        // 过期 / 归属丢失 → 消散
        if (this.tickCount > LIFETIME) {
            dissipate(level);
            return;
        }
        MosquitoZbrEntity core = findCore(level);
        if (this.swarmOwner != null && core == null) {
            dissipate(level);
            return;
        }

        // 每 10 tick 做一次小半径蚊香扫描（7³=343 个点，代价可控）
        if (this.tickCount % 10 == 0) {
            var smoke = MosquitoCoilBlock.findSmoke(level, this.blockPosition(), 3);
            if (smoke != null) {
                // 烟雾里蚊子持续掉血（4 血 ≈ 4 秒内消散），顺便去祸害蚊香
                this.hurtServer(level, level.damageSources().magic(), 1.0F);
                if (this.isAlive() && this.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(smoke)) < 1.44D
                        && this.getRandom().nextInt(5) == 0) {
                    MosquitoCoilBlock.burnFaster(level, smoke);
                }
                if (!this.isAlive()) return; // 已经被熏死了
            }
        }

        // 目标：核心的目标 > 野生时 16 格内任意活物（饥饿的蚊子不挑食：
        // 玩家、村民、动物、人形尸兄……见谁咬谁；只放过同类、蚊群核心和创造/观察模式）
        LivingEntity target = core != null ? core.getTarget() : null;
        if (target == null && core == null) {
            if (this.tickCount % 10 == 0 || (this.wildTarget != null && !this.wildTarget.isAlive())) {
                this.wildTarget = pickWildPrey(level);
            }
            target = this.wildTarget;
            if (target != null && target.isRemoved()) {
                this.wildTarget = null;
                target = null;
            }
        }

        Vec3 desired;
        boolean feeding = false;
        if (target != null && target.isAlive()) {
            if (this.distanceToSqr(target) < 1.44D && this.hurtTime <= 0) {
                // 贴身 → 趴脸吸血：位置锁定在目标脸周围一圈，参考 CorpseMaggotEntity.tickAttachment
                feedOnFace(level, target);
                feeding = true;
                desired = null;
            } else {
                desired = target.getEyePosition();
            }
        } else if (core != null) {
            // 没目标：绕核心盘旋
            double angle = (this.tickCount * 0.08D) + this.getId() * 1.3D;
            desired = core.position().add(Math.cos(angle) * 1.2D, 0.4D, Math.sin(angle) * 1.2D);
        } else {
            // 野生且没玩家：原地打转
            double angle = (this.tickCount * 0.05D) + this.getId();
            desired = this.position().add(Math.cos(angle) * 0.5D, Math.sin(angle * 0.7D) * 0.2D,
                    Math.sin(angle) * 0.5D);
        }

        if (feeding) {
            return;   // 趴脸时速度与朝向已在 feedOnFace 里锁定，不再走通用推进
        }

        Vec3 push = desired.subtract(this.position());
        if (push.lengthSqr() > 1.0E-4D) {
            push = push.normalize().scale(0.22D);
        }
        // 保留一点惯性 + 轻微上下浮动，看起来是活的
        this.setDeltaMovement(this.getDeltaMovement().scale(0.6D).add(push)
                .add(0.0D, Math.sin(this.tickCount * 0.4D + this.getId()) * 0.01D, 0.0D));

        // 飞行时也要正面对着目标（lookControl 只负责转 yRot，模型正面建模与原版相反，需 +180 补偿）
        if (target != null && target.isAlive()) {
            float faceYaw = faceYawTo(target.getX(), target.getZ());
            this.setYRot(faceYaw);
            this.setYHeadRot(faceYaw);
            this.setYBodyRot(faceYaw);
        }
    }

    /**
     * 趴脸吸血：把蚊子锁在目标<b>脸部</b>周围一圈（套路参考 {@code CorpseMaggotEntity.tickAttachment}）。
     * <ul>
     *   <li>均匀分布：按黄金角（{@code id × 2.4 弧度}）绕脸排开——任意数量都不扎堆，
     *       再加随时间缓慢爬动（{@code tickCount × 0.02}），像一群蚊子在脸上爬；</li>
     *   <li>正面对着目标：朝向按"指向目标"计算再 +180（geo 模型正面建模朝 +Z，
     *       与原版实体"面朝 -Z"相反——直接用 lookControl 的朝向会屁股对着玩家）；</li>
     *   <li>每 {@link #BITE_COOLDOWN} tick 咬一口，清零无敌帧；</li>
     *   <li>被拍（{@code hurtTime > 0}）时外层会自动切回飞行追逐，相当于拍一下蚊子就飞开。</li>
     * </ul>
     */
    private void feedOnFace(ServerLevel level, LivingEntity host) {
        // 黄金角均匀分布 + 缓慢爬动
        double angle = this.getId() * 2.399963D + this.tickCount * 0.02D;
        float yawRad = host.getYRot() * ((float) Math.PI / 180F);
        double fx = -Math.sin(yawRad), fz = Math.cos(yawRad);   // 目标前方
        double rx = -fz, rz = fx;                                // 目标右方
        double radius = host.getBbWidth() * 0.5D + 0.35D;
        double ox = Math.cos(angle) * radius;
        double oy = Math.sin(angle) * radius * 0.6D;             // 椭圆：横向一圈、纵向压扁（脸是竖的）
        double px = host.getX() + fx * 0.25D + rx * ox;
        double py = host.getEyeY() - 0.05D + oy;
        double pz = host.getZ() + fz * 0.25D + rz * ox;

        this.setPos(px, py, pz);
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();

        // 正面对着目标的脸（+180 补偿模型正反）
        float faceYaw = faceYawTo(host.getX(), host.getZ());
        this.setYRot(faceYaw);
        this.setYHeadRot(faceYaw);
        this.setYBodyRot(faceYaw);

        // 无视无敌帧的持续叮咬（跟蛆虫一样咬出血）
        if ((this.tickCount + this.getId()) % BITE_COOLDOWN == 0) {
            host.invulnerableTime = 0;
            if (host.hurtServer(level, level.damageSources().mobAttack(this), BITE_DAMAGE)) {
                xiaoshi2022.corpseorigin.growth.CorpseHorror.blood(level, host.position(), 3);
                level.playSound(null, host.getX(), host.getY(), host.getZ(),
                        xiaoshi2022.corpseorigin.registry.ModSounds.MOSQUITO_DING,
                        net.minecraft.sounds.SoundSource.HOSTILE, 0.5F,
                        1.1F + level.getRandom().nextFloat() * 0.2F);
            }
        }
    }

    /** 从当前位置指向 (x, z) 的 yaw（标准原版公式：atan2(dz, dx) − 90°） */
    private float faceYawTo(double x, double z) {
        return (float) (Math.atan2(z - this.getZ(), x - this.getX()) * (180F / Math.PI)) - 90.0F + 180.0F;
    }

    /** 野生蚊子的猎物缓存（每 10 tick 刷新一次，避免每 tick 全局扫描） */
    private LivingEntity wildTarget;

    /**
     * 16 格内挑最近的猎物：任何 LivingEntity 都行（村民/动物/玩家/人形尸兄……），
     * 只排除蚊子同类、蚊群核心（那自己人）和创造/观察模式的玩家。
     * 局部 inflate 盒扫描（16 格），每 10 tick 一次，符合项目的性能惯例。
     */
    private LivingEntity pickWildPrey(ServerLevel level) {
        LivingEntity best = null;
        double bestDist = 256.0D;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(16.0D))) {
            if (e == this || !e.isAlive() || e.isSpectator()
                    || e instanceof MosquitoSwarmEntity || e instanceof MosquitoZbrEntity) {
                continue;
            }
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
                continue;
            }
            double d = this.distanceToSqr(e);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    /** 找到所属核心（32 格内） */
    private MosquitoZbrEntity findCore(ServerLevel level) {
        if (this.swarmOwner == null) return null;
        return level.getEntitiesOfClass(MosquitoZbrEntity.class,
                        this.getBoundingBox().inflate(32.0D),
                        m -> m.isAlive() && this.swarmOwner.equals(m.getUUID()))
                .stream().findFirst().orElse(null);
    }

    /** 消散：烟 + 拍死音效感的粒子 */
    private void dissipate(ServerLevel level) {
        level.sendParticles(ParticleTypes.POOF,
                this.getX(), this.getY() + 0.1D, this.getZ(), 3, 0.1D, 0.1D, 0.1D, 0.01D);
        this.discard();
    }

    // ==================== 飞行移动 ====================

    /**
     * 无重力飞行：服务端按当前速度推进再指数衰减；客户端不做本地模拟
     * （位置由实体同步驱动，避免被客户端重力往下拽）。
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.85D));
            this.calculateEntityAnimation(false);
        }
    }

    @Override
    protected void registerGoals() {
        // 移动全由 customServerAiStep 推，不需要 Goal
    }

    // ==================== 音效 ====================

    /** 环境音：翅膀嗡嗡声（音量压低——场上有几十只，每只都响会震耳朵） */
    @Override
    public net.minecraft.sounds.SoundEvent getAmbientSound() {
        return xiaoshi2022.corpseorigin.registry.ModSounds.MOSQUITO_BUZZING;
    }

    @Override
    public float getSoundVolume() {
        return 0.25F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 60;
    }

    // ==================== GeckoLib 动画 ====================

    private static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation SUCK_ANIM = RawAnimation.begin().thenLoop("suck");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 3, test -> {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && this.distanceToSqr(target) < 2.25D) {
                return test.setAndContinue(SUCK_ANIM);
            }
            return test.setAndContinue(FLY_ANIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
