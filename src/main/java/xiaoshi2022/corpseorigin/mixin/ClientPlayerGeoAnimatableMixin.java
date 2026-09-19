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
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.renderer.player.MutantBodyAnimations;
import xiaoshi2022.corpseorigin.client.renderer.player.MutantBodyRenderData;
import xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderData;
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
    // 注意：左护法变异体（zuo_guardian）的动画不在这里 ——
    // 它那套名字（reptile / swim / raised / riderx_attack）走专属控制器 +
    // 配置化的对应表，见 MutantBodyAnimations。

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

    // ==================== 开胃奶背挂（niunaix） ====================
    // 这套骨骼跟 corpse_player 的重名（都有 idle / attack），靠 NiunaiXRenderData.ACTIVE 门控隔开：
    // 不是开胃奶背挂形态就整条停掉，免得动画名泄漏到别的模型上刷日志。
    /** 收拢隐藏（贴在背后）——只有一帧的静态姿态，循环播即可 */
    private static final RawAnimation CORPSEORIGIN$NIUNAI_IDLE = RawAnimation.begin().thenLoop("idle");
    /** 尖刺：每次挥击从头播一遍 */
    private static final RawAnimation CORPSEORIGIN$NIUNAI_ATTACK = RawAnimation.begin().thenPlay("attack");
    /** 格挡：播一次停在末帧（花瓣张开成盾的姿态），整个菊花盾窗口都维持这个姿势 */
    private static final RawAnimation CORPSEORIGIN$NIUNAI_PARRY = RawAnimation.begin().thenPlay("parry");

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

    /** 变异体：上一帧是否在挥击 / 潜行 —— 这两条都是"播一次停在末帧"，靠上升沿重播 */
    @Unique
    private boolean corpseorigin$mutantAttacking;
    @Unique
    private boolean corpseorigin$mutantSneaking;

    /** 开胃奶背挂：上一帧是否在挥击 —— 抓"挥击开始"的上升沿重播 */
    @Unique
    private boolean corpseorigin$niunaiAttacking;

    /**
     * attack 这条 clip 比原版那一下挥击长得多，所以不能只按 {@code attackTime > 0} 判断。
     * <p>
     * 原版 {@code attackTime}（{@code swingTime / getCurrentSwingDuration()}）只覆盖挥击本身
     * （玩家约 6 tick），而 {@code niunaix.animation.json} 的 {@code attack} 有 1 秒 ——
     * 按 attackTime 切的话尖刺会在刚伸出去的时候被切回收拢姿态，看着像抽一下。
     * 所以这里记下<b>本次挥击的起点</b>，往后撑满这条 clip 再切回 idle。
     * <p>
     * 单位是实体年龄（tick），-1 = 当前没有在撑的那一次。
     */
    @Unique
    private float corpseorigin$niunaiSwingStart = -1.0F;

    /** 尖刺要播满的时长（tick）：{@code attack} 是 1 秒，给 21 让末帧也走完 */
    private static final float NIUNAI_ATTACK_TICKS = 21.0F;

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
        // ★ 左护法变异体专属控制器：放在最后注册，让它出的骨骼值盖在这几条之上；
        //   变异体形态下 movement / attack 会主动让开（见各自方法开头）。
        controllers.add(new AnimationController<PlayerGeoAnimatable>("mutant_body", 0, this::corpseorigin$mutantBody));

        // ★ 开胃奶背挂（niunaix）：同样放最后。
        //   ⚠️ 只能开**一条**控制器：这套骨骼里 idle 动的是 petal / group*，attack 动的是 petal*2，
        //   两条一起播会各写一套花瓣的旋转与缩放，姿态直接混掉。所以三段是"切换"关系，不同时播。
        controllers.add(new AnimationController<PlayerGeoAnimatable>("niunai", 0, this::corpseorigin$niunai));
    }

    /**
     * 开胃奶背挂的唯一控制器：<b>三段互相切换</b>，任何时刻只有一条在播。
     * <p>
     * 优先级 {@code parry}（菊花盾）&gt; {@code attack}（尖刺）&gt; {@code idle}（收拢隐藏）。
     * 这里不能像断肢那样"打底 + 覆盖"地分多条控制器 —— 这套骨骼里 idle 动的是
     * {@code petal} / {@code group*}，attack 动的是 {@code petal*2}，两条同时播会各写一套花瓣的
     * 旋转与缩放，张开的花盾会被 idle 的收拢姿态拉回去，姿态直接混掉。
     */
    @Unique
    private PlayState corpseorigin$niunai(AnimationTest<PlayerGeoAnimatable> test) {
        Boolean active = test.getDataOrDefault(NiunaiXRenderData.ACTIVE, null);
        if (active == null) {
            // ★ 不是我的回合（盔甲 / 断肢 / 变异体那条管线拿同一个 animatable 求值，
            //   那份 render state 上没有我们的 ticket）→ 原样返回、不碰任何内部状态。
            return PlayState.CONTINUE;
        }
        if (!active) {
            corpseorigin$niunaiAttacking = false;
            corpseorigin$niunaiSwingStart = -1.0F;
            return PlayState.STOP;   // 不是背挂形态：整条停掉，免得动画名泄漏到别的模型上刷日志
        }

        // ① 菊花盾最高：盾在就不播别的（盾期间本来就该一直张着）
        if (Boolean.TRUE.equals(test.getDataOrDefault(NiunaiXRenderData.PARRYING, Boolean.FALSE))) {
            corpseorigin$niunaiAttacking = false;
            corpseorigin$niunaiSwingStart = -1.0F;
            return test.setAndContinue(CORPSEORIGIN$NIUNAI_PARRY);
        }

        // ② 尖刺：挥击期间播，并且要"播完再切"（见 corpseorigin$niunaiSwingStart 的注释）
        float now = test.getDataOrDefault(NiunaiXRenderData.AGE_TICKS, 0.0F);
        boolean attacking = Boolean.TRUE.equals(
                test.getDataOrDefault(NiunaiXRenderData.ATTACKING, Boolean.FALSE));
        if (attacking) {
            corpseorigin$niunaiSwingStart = now;
        }
        boolean swinging = attacking
                || (corpseorigin$niunaiSwingStart >= 0.0F
                    // now < 起点 = 时钟被重置了（挥击那一下死了、重生换了新实体，ageInTicks 归零），
                    // 不加这道判断的话差值恒为负数、尖刺会一直卡在张开姿态
                    && now >= corpseorigin$niunaiSwingStart
                    && now - corpseorigin$niunaiSwingStart < NIUNAI_ATTACK_TICKS);

        if (swinging) {
            boolean newSwing = attacking && !corpseorigin$niunaiAttacking;
            corpseorigin$niunaiAttacking = attacking;
            test.setAndContinue(CORPSEORIGIN$NIUNAI_ATTACK);
            if (newSwing) {
                test.controller().setAnimationTime(0.0D);
            }
            return PlayState.CONTINUE;
        }

        // ③ 打底：收拢贴在背后（"hidden behind the back"）
        corpseorigin$niunaiAttacking = false;
        corpseorigin$niunaiSwingStart = -1.0F;
        return test.setAndContinue(CORPSEORIGIN$NIUNAI_IDLE);
    }

    /**
     * 左护法变异体（{@code zuo_guardian}）的专属动画控制器。
     * <p>
     * <b>为什么单独开一条</b>：这套模型的动画名跟玩家断肢模型（corpse_player）完全不同 ——
     * 它没有 walk / regrow_*，却有 reptile / swim / raised。共用 movement 那条控制器时，
     * "什么状态播哪条"会和断肢那套搅在一起；开一条独立的，变异体形态下让 movement / attack 停掉，
     * 由这里统一按对应表（见 {@link MutantBodyAnimations}）选动画，两个形态就互不干扰。
     * <p>
     * 状态来自 {@code ZuoGuardianBodyRenderer.writeBodyRenderData} 每帧写入的 ticket：
     * 移动 / 水里 / 潜行 / 挥击。映射关系（哪条动画）写在配置 {@code mutantBody.animations} 里。
     * <p>
     * 两条"播一次停在末帧"的动画（探头、攻击）在<b>上升沿把时间轴拉回 0 帧</b>重播，
     * 否则蹲下→站起→再蹲下、连挥两刀都会卡在末帧不动。
     */
    @Unique
    private PlayState corpseorigin$mutantBody(AnimationTest<PlayerGeoAnimatable> test) {
        Boolean active = test.getDataOrDefault(MutantBodyRenderData.ACTIVE, null);
        if (active == null) {
            return PlayState.CONTINUE;   // 别人的回合（盔甲 / 断肢管线），不碰状态
        }
        if (!active) {
            // 不是变异体形态：显式停掉 —— 这几条动画名别的模型里没有，泄漏过去只会刷日志
            corpseorigin$mutantAttacking = false;
            corpseorigin$mutantSneaking = false;
            return PlayState.STOP;
        }

        boolean attacking = Boolean.TRUE.equals(
                test.getDataOrDefault(LimbRenderData.ATTACKING, Boolean.FALSE));
        boolean inWater = Boolean.TRUE.equals(
                test.getDataOrDefault(MutantBodyRenderData.SWIMMING, Boolean.FALSE));
        boolean sneaking = Boolean.TRUE.equals(
                test.getDataOrDefault(MutantBodyRenderData.SNEAKING, Boolean.FALSE));
        boolean moving = Boolean.TRUE.equals(
                test.getDataOrDefault(MutantBodyRenderData.MOVING, Boolean.FALSE));

        boolean restart = (attacking && !corpseorigin$mutantAttacking)
                || (sneaking && !corpseorigin$mutantSneaking);
        corpseorigin$mutantAttacking = attacking;
        corpseorigin$mutantSneaking = sneaking;

        MutantBodyAnimations.State state = MutantBodyAnimations.select(attacking, inWater, sneaking, moving);
        test.setAndContinue(MutantBodyAnimations.animation(state));
        if (restart && MutantBodyAnimations.oneShot(state)) {
            test.controller().setAnimationTime(0.0D);
        }
        return PlayState.CONTINUE;
    }

    @Unique
    private PlayState corpseorigin$movement(AnimationTest<PlayerGeoAnimatable> test) {
        // 变异体形态交给 mutant_body 那条控制器，这里让开（两套一起出会互相盖骨头）
        if (Boolean.TRUE.equals(test.getDataOrDefault(MutantBodyRenderData.ACTIVE, null))) {
            return PlayState.STOP;
        }
        Boolean moving = test.getDataOrDefault(LimbRenderData.MOVING, null);
        if (moving == null) {
            // ★ 这次求值没带玩家身体的信号（典型情况：穿着 GeoLib 套装时，
            //   GeoArmorRenderer 会拿同一个 animatable 用"盔甲那份 render state"再求一次值，
            //   而那份 state 上没有我们的 ticket）。这种"别人的回合"必须原样返回、不碰内部状态，
            //   否则会把身体这边的动画打断/每帧重置。
            return PlayState.CONTINUE;
        }
        return moving ? test.setAndContinue(CORPSEORIGIN$WALK) : test.setAndContinue(CORPSEORIGIN$IDLE);
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
        Boolean attackingData = test.getDataOrDefault(LimbRenderData.ATTACKING, null);
        if (attackingData == null) {
            return PlayState.CONTINUE;   // 别人的回合（盔甲那条管线），不碰状态
        }
        // 变异体形态交给 mutant_body 那条控制器（它按对应表播 riderx_attack），这里让开
        if (Boolean.TRUE.equals(test.getDataOrDefault(MutantBodyRenderData.ACTIVE, null))) {
            corpseorigin$attacking = false;
            return PlayState.STOP;
        }
        boolean attacking = attackingData;
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
        Float progressData = test.getDataOrDefault(progressTicket, null);
        if (progressData == null) {
            // ★ 关键：这次求值不是"玩家身体"那条管线。
            //   穿着 GeoLib 套装时，GeoArmorRenderer 会拿同一个 animatable、用盔甲自己的
            //   render state 再求一次值，而那份 state 上没有任何 REGROW_* / MOVING ticket。
            //   以前这里写 getDataOrDefault(..., ClientLimbCache.INTACT)，于是那种求值被当成
            //   "这肢完好"→ 直接 STOP，顺带把 lastRegrow 写成 -1；等身体那边再求值时又变成
            //   "进度倒退 → 重播"，动画每帧从第 0 帧重新开始 —— 表现就是
            //   "穿着 GeoLib 套装时四肢的再生动画完全不播"（原版盔甲没有这条管线，所以正常）。
            //   正确做法：不是我的回合就原样返回，不碰任何内部状态。
            return PlayState.CONTINUE;
        }
        float progress = progressData;
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
