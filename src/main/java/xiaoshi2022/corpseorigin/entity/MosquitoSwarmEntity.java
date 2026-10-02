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

        // 目标：核心的目标 > 野生时最近的玩家
        LivingEntity target = core != null ? core.getTarget() : null;
        if (target == null && core == null) {
            target = level.getNearestPlayer(this, 16.0D);
            if (target != null && (target.isSpectator()
                    || (target instanceof Player p && p.isCreative()))) {
                target = null;
            }
        }

        Vec3 desired;
        if (target != null && target.isAlive()) {
            // 有目标：贴脸叮咬（叮咬判定在这里，伤害无视无敌帧）
            desired = target.getEyePosition();
            if (this.distanceToSqr(target) < 1.44D
                    && (this.tickCount + this.getId()) % BITE_COOLDOWN == 0) {
                // 清零无敌帧 = 无视无敌帧的持续叮咬
                target.invulnerableTime = 0;
                target.hurtServer(level, level.damageSources().mobAttack(this), BITE_DAMAGE);
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

        Vec3 push = desired.subtract(this.position());
        if (push.lengthSqr() > 1.0E-4D) {
            push = push.normalize().scale(0.22D);
        }
        // 保留一点惯性 + 轻微上下浮动，看起来是活的
        this.setDeltaMovement(this.getDeltaMovement().scale(0.6D).add(push)
                .add(0.0D, Math.sin(this.tickCount * 0.4D + this.getId()) * 0.01D, 0.0D));
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
