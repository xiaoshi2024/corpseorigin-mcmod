package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.animation.RawAnimation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 左护法变异体的<b>动画对应表</b>：什么状态播 {@code zuo_guardian} 的哪条动画。
 * <p>
 * 这套模型自带 {@code idle / reptile / swim / raised / riderx_attack / eat / speak}，
 * 但没有 {@code walk} —— 所以"走路"是借 {@code reptile}（蛇在爬）。映射关系写在配置里
 * （{@code mutantBody.animations}），改名字不用重新编译。
 * <table border="1">
 *   <tr><th>状态</th><th>默认动画</th><th>怎么触发</th><th>播法</th></tr>
 *   <tr><td>{@link State#IDLE}</td><td>idle</td><td>站着不动</td><td>循环</td></tr>
 *   <tr><td>{@link State#MOVE}</td><td>reptile</td><td>陆地移动</td><td>循环</td></tr>
 *   <tr><td>{@link State#SWIM}</td><td>swim</td><td>身体在水里</td><td>循环</td></tr>
 *   <tr><td>{@link State#RAISED}</td><td>raised</td><td>潜行（蹲下）= 探头</td><td>播一次，停在末帧</td></tr>
 *   <tr><td>{@link State#ATTACK}</td><td>riderx_attack</td><td>挥击</td><td>播一次</td></tr>
 * </table>
 * <p>
 * 优先级：攻击 &gt; 水里 &gt; 潜行 &gt; 移动 &gt; 待机。
 * <p>
 * 谁在用：{@code ClientPlayerGeoAnimatableMixin} 里那条专属的 {@code mutant_body} 控制器
 * （变异体形态下断肢那几条控制器会主动让开，见 {@link #select}）。
 */
@Environment(EnvType.CLIENT)
public final class MutantBodyAnimations {

    /** 对应表的"行" */
    public enum State {
        IDLE, MOVE, SWIM, RAISED, ATTACK
    }

    /**
     * 按当前状态挑一条动画。
     * <p>
     * 「播一次」的两条（{@link State#RAISED} / {@link State#ATTACK}）由调用方负责在"进入该状态"的
     * 上升沿把时间轴拉回 0 帧，这样蹲下→站起→再蹲下会重新探一次头，而不是一直卡在末帧。
     */
    public static State select(boolean attacking, boolean inWater, boolean sneaking, boolean moving) {
        if (attacking) {
            return State.ATTACK;
        }
        if (inWater) {
            return State.SWIM;
        }
        if (sneaking) {
            return State.RAISED;
        }
        return moving ? State.MOVE : State.IDLE;
    }

    /** 这条是不是"播一次就停在末帧"（否则是循环动画） */
    public static boolean oneShot(State state) {
        return state == State.RAISED || state == State.ATTACK;
    }

    /** 该状态对应的动画名（读配置，留空时配置那边已经补好了默认值） */
    public static String name(State state) {
        CorpseConfig.MutantBody.Animations table = CorpseConfig.get().mutantBody.animations;
        return switch (state) {
            case IDLE -> table.idle;
            case MOVE -> table.move;
            case SWIM -> table.swim;
            case RAISED -> table.raised;
            case ATTACK -> table.attack;
        };
    }

    /** 缓存 RawAnimation：这两类动画每帧都要喂给控制器，别每帧新建对象 */
    private static final Map<String, RawAnimation> CACHE = new ConcurrentHashMap<>();

    public static RawAnimation animation(State state) {
        String name = name(state);
        boolean loop = !oneShot(state);
        return CACHE.computeIfAbsent((loop ? "loop:" : "play:") + name,
                key -> loop ? RawAnimation.begin().thenLoop(name) : RawAnimation.begin().thenPlay(name));
    }

    private MutantBodyAnimations() {
    }
}
