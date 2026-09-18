package xiaoshi2022.corpseorigin.mixin;

import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xiaoshi2022.corpseorigin.client.limb.ClientLimbCache;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.limb.LimbSlots;

/**
 * 让玩家实体本身成为断肢模型的动画宿主（GeoAnimatable）。
 * <p>
 * <b>为什么必须挂在实体上</b>：GeckoLib 的 Molang 查询（{@code MolangQueries}）会把
 * {@code Actor.animatable} 无条件强转成 {@code LivingEntity} 去读 {@code query.is_blocking} /
 * {@code query.head_y_rotation} 之类；而 {@code GeoReplacedEntityRenderer} 的构造函数又禁止
 * {@code animatable} 是 {@code Entity}。两条约束不可调和 —— 所以只能让"动画宿主"就是玩家实体：
 * 渲染器构造时先塞 null（绕开那个 instanceof 检查），再在 {@code fillRenderState} 里换成真玩家。
 * <p>
 * 关节动作走标准 GeckoLib 流程：动画定义在
 * {@code assets/corpseorigin/geckolib/animations/entity/corpse_player.animation.json}（Blockbench 可直接编辑），
 * 这里只负责"什么状态播哪一条"。
 */
@Mixin(AbstractClientPlayer.class)
public abstract class ClientPlayerGeoAnimatableMixin implements PlayerGeoAnimatable {

    private static final RawAnimation CORPSEORIGIN$IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CORPSEORIGIN$WALK = RawAnimation.begin().thenLoop("walk");
    /** 只播一次的攻击动画（每次挥击由上升沿把时间轴拉回 0 帧重播） */
    private static final RawAnimation CORPSEORIGIN$ATTACK = RawAnimation.begin().thenPlay("attack");

    // 再生动画：每条只播一次，播完停在末帧（动画自己的速度就是播放速度）
    private static final RawAnimation CORPSEORIGIN$REGROW_RIGHT_ARM =
            RawAnimation.begin().thenPlay("regrow_right_arm");
    private static final RawAnimation CORPSEORIGIN$REGROW_LEFT_ARM =
            RawAnimation.begin().thenPlay("regrow_left_arm");
    private static final RawAnimation CORPSEORIGIN$REGROW_RIGHT_LEG =
            RawAnimation.begin().thenPlay("regrow_right_leg");
    private static final RawAnimation CORPSEORIGIN$REGROW_LEFT_LEG =
            RawAnimation.begin().thenPlay("regrow_left_leg");
    private static final RawAnimation CORPSEORIGIN$REGROW_HEAD =
            RawAnimation.begin().thenPlay("regrow_head");

    @Unique
    private AnimatableInstanceCache corpseorigin$animatableCache;

    /**
     * 进度倒退超过这个量就认定"又断了一次"（正常推进时进度单调不减，所以不会误判）。
     * 取 0.5 是为了容忍饥饿停长、网络补包之类的抖动。
     */
    private static final float CORPSEORIGIN$RESTART_DROP = 0.5F;

    /**
     * 上一帧各部位的再生进度，只用来识别"又断了一次"。
     * <p>
     * 注意它<b>不能</b>靠"上次是不是负数"来判断新一次断开 —— 四肢完好时 controller 根本不会被求值，
     * 这个值会一直停在 1.0。
     */
    @Unique
    private final float[] corpseorigin$lastRegrow = corpseorigin$newRegrowArray();

    /** 上一帧各部位的再生进度都从"完好"（-1）开始 */
    @Unique
    private static float[] corpseorigin$newRegrowArray() {
        float[] array = new float[LimbSlots.COUNT];
        java.util.Arrays.fill(array, -1.0F);
        return array;
    }

    /** 上一帧是否在挥击 —— 用来抓"挥击开始"的上升沿 */
    @Unique
    private boolean corpseorigin$attacking;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 四肢的摆动走 corpse_player 自己的 JSON 动画（idle / walk / attack）；
        // 躯干与头则由 CorpsePlayerGeoRenderer 每帧对齐到原版骨架（盔甲才贴得住）。
        controllers.add(new AnimationController<PlayerGeoAnimatable>("movement", 5, this::corpseorigin$movement));
        controllers.add(new AnimationController<PlayerGeoAnimatable>("attack", 2, this::corpseorigin$attack));
        // 再生：每个部位一条独立动画，每次断开都从头播一遍
        controllers.add(corpseorigin$regrowController(
                "regrow_right_arm", CORPSEORIGIN$REGROW_RIGHT_ARM, LimbRenderData.REGROW_RIGHT_ARM, 0));
        controllers.add(corpseorigin$regrowController(
                "regrow_left_arm", CORPSEORIGIN$REGROW_LEFT_ARM, LimbRenderData.REGROW_LEFT_ARM, 1));
        controllers.add(corpseorigin$regrowController(
                "regrow_right_leg", CORPSEORIGIN$REGROW_RIGHT_LEG, LimbRenderData.REGROW_RIGHT_LEG, 2));
        controllers.add(corpseorigin$regrowController(
                "regrow_left_leg", CORPSEORIGIN$REGROW_LEFT_LEG, LimbRenderData.REGROW_LEFT_LEG, 3));
        controllers.add(corpseorigin$regrowController(
                "regrow_head", CORPSEORIGIN$REGROW_HEAD, LimbRenderData.REGROW_HEAD, LimbSlots.HEAD));
    }

    @Unique
    private PlayState corpseorigin$movement(AnimationTest<PlayerGeoAnimatable> test) {
        return test.getDataOrDefault(LimbRenderData.MOVING, false)
                ? test.setAndContinue(CORPSEORIGIN$WALK)
                : test.setAndContinue(CORPSEORIGIN$IDLE);
    }

    /**
     * 攻击控制器：每次挥击都要从头播。
     * <p>
     * {@code setAndContinue} 单独干不行 —— 它只在"目标动画 ≠ 当前动画"时才切换，
     * 一次挥击播完后 controller 仍持有 attack，后续挥击会被当成"已经在播"忽略掉；
     * {@code triggerableAnim} 那条路也会被 handler 的 STOP 挡掉后续触发。
     * 所以这里直接在**挥击开始的上升沿**把时间轴拉回 0 帧。
     */
    @Unique
    private PlayState corpseorigin$attack(AnimationTest<PlayerGeoAnimatable> test) {
        boolean attacking = test.getDataOrDefault(LimbRenderData.ATTACKING, false);
        if (!attacking) {
            corpseorigin$attacking = false;
            return PlayState.STOP;
        }

        boolean newSwing = !corpseorigin$attacking;
        corpseorigin$attacking = true;

        test.setAndContinue(CORPSEORIGIN$ATTACK);
        if (newSwing) {
            test.controller().setAnimationTime(0.0D);
        }
        return PlayState.CONTINUE;
    }

    /** 给某个部位建一条再生动画的控制器 */
    @Unique
    private AnimationController<PlayerGeoAnimatable> corpseorigin$regrowController(
            String animName, RawAnimation animation, DataTicket<Float> progressTicket, int slot) {
        return new AnimationController<PlayerGeoAnimatable>(animName, 0,
                test -> corpseorigin$regrow(test, animation, progressTicket, slot));
    }

    /**
     * regrow_* 动画控制器 —— 整段交给动画自己播，且<b>每次断开都从头重播</b>。
     * <p>
     * 时长、节奏、每一帧长什么样全在 {@code corpse_player.animation.json} 里，代码只负责
     * "什么时候开始播、什么时候不播"。刻意不做进度对齐（`setAnimationTime` 拖时间轴、或
     * `setControllerSpeed` 按服务端秒数调速）：那些都要拿渲染帧率去跟服务端 tick 比对，
     * 而 60fps 远高于 20 tick/s，同一个 tick 内会连着渲染好几帧、进度值完全一样，很容易被误判成
     * "进度停滞"而把动画卡住。
     * <p>
     * <b>为什么要检测"重新开始"</b>：动画是 {@code hold_on_last_frame}，播完停在末帧，controller
     * 仍然持有这条动画；再次断开时再喂同一个 {@link RawAnimation}，{@code setAndContinue} 会判定
     * "已经在播"而什么都不做（表现：只有第一次断会播）。检测到新一次断开时直接
     * {@code controller.reset()} 把状态清干净，动画才会被当成"换了片子"从头播。
     * <p>
     * <b>判据为什么是"进度倒退"而不是"上帧是负数"</b>：四肢完好时整个 extract 流程都不跑
     * （{@code ClientLimbCache.get} 返回 null，{@code AvatarRendererMixin} 直接 return），
     * controller 也就不会被求值，进度会一直停留在上次长好时的 1.0 —— 只判"上帧是负数"根本抓不到
     * 第二次断开。改成"明显倒退"既不受这个影响，也不会被同 tick 内的多帧渲染误判
     * （正常推进时进度单调不减）。
     */
    @Unique
    private PlayState corpseorigin$regrow(AnimationTest<PlayerGeoAnimatable> test,
                                         RawAnimation animation, DataTicket<Float> progressTicket, int slot) {
        float progress = test.getDataOrDefault(progressTicket, ClientLimbCache.INTACT);
        if (progress < 0.0F) {
            // 完好（INTACT）或断了不会自愈（PERMANENT）→ 不播，由 renderer 决定显示方块骨还是残桩
            corpseorigin$lastRegrow[slot] = progress;
            return PlayState.STOP;
        }

        float last = corpseorigin$lastRegrow[slot];
        boolean restarted = last < 0.0F || progress < last - CORPSEORIGIN$RESTART_DROP;
        corpseorigin$lastRegrow[slot] = progress;

        if (restarted) {
            test.controller().reset();
        }
        test.setAndContinue(animation);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        if (corpseorigin$animatableCache == null) {
            corpseorigin$animatableCache = GeckoLibUtil.createInstanceCache(this);
        }
        return corpseorigin$animatableCache;
    }
}
