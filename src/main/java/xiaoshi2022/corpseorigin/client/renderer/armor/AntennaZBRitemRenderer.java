package xiaoshi2022.corpseorigin.client.renderer.armor;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.loading.math.MolangQueries;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.item.armor.AntennaZBRitem;

/**
 * 天线宝宝尸兄盔甲渲染器。
 * <p>
 * 模型/贴图走 GeckoLib 默认约定（{@code armor/antennazbr}），这里额外做两件事：
 * 每帧把「穿戴着在不在挥击 / 在不在吸食 / 吸食目标在哪个方向」写进这套盔甲的 render state，
 * 以及把动画里用到的 {@code query.target_*_rotation} 注册成 GeckoLib 的 actor 变量。
 */
public class AntennaZBRitemRenderer<R extends HumanoidRenderState & GeoRenderState>
        extends GeoArmorRenderer<AntennaZBRitem, R> {

    public AntennaZBRitemRenderer() {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "armor/antennazbr")));

        withRenderLayer(AutoGlowingGeoLayer::new);
    }

    /**
     * 把 {@code query.target_x_rotation} / {@code query.target_y_rotation} / {@code query.target_stretch}
     * 注册成 GeckoLib 的 actor 变量。
     * <p>
     * 这几个名字 GeckoLib 5.5.5 里<b>没有</b>内置实现（扫过整个 jar，MolangQueries 里一个都没有），
     * 所以 animation.json 里直接写它们只会恒等于 0，触手永远不转向、也不会按距离伸长。
     * <p>
     * 注册成 actor 变量而不是 {@code setVariableValue} 那种全局值，是因为 resolver 每次渲染都会拿到
     * 当前动画宿主：{@code Actor} 里就带着本次渲染用的 render state，直接读上面写进去的值即可 ——
     * 多人同时穿这套盔甲、同屏多个目标也不会互相串值。
     */
    public static void registerMolangQueries() {
        MolangQueries.setActorVariable("query.target_x_rotation",
                actor -> readAngle(actor.renderState(), AntennaArmorRenderData.TARGET_X_ROTATION));
        MolangQueries.setActorVariable("query.target_y_rotation",
                actor -> readAngle(actor.renderState(), AntennaArmorRenderData.TARGET_Y_ROTATION));
        MolangQueries.setActorVariable("query.target_stretch",
                actor -> readAngle(actor.renderState(), AntennaArmorRenderData.TARGET_STRETCH));
    }

    private static double readAngle(GeoRenderState state, DataTicket<Double> ticket) {
        return state == null ? 0.0 : state.getOrDefaultGeckolibData(ticket, 0.0);
    }

    /**
     * 给这套盔甲的 render state 填动画信号。
     * <p>
     * 先摆清楚 GeckoLib 5.5.5 的盔甲渲染是怎么拿到 state 的：
     * {@code EntityRendererMixin} 用 {@code @WrapMethod} 包住
     * {@code EntityRenderer#createRenderState(Entity, float)}，把结果（也就是<b>玩家那份</b>
     * {@code HumanoidRenderState}）交给 {@code GeoArmorRenderer#captureRenderStates} 去填每件盔甲；
     * 其中 HEAD 槽直接复用同一个 state 对象，其它槽位则是把原版 createRenderState 再跑一遍、
     * 拿到同一套 extract 产出的另一份 state。{@code GeoArmorRenderer#createRenderState} 里那句
     * {@code new HumanoidRenderState()} 只是调用方没状态时的兜底，正常路径走不到。
     * <p>
     * <b>为什么信号写在这个回调里</b>：{@code captureDefaultRenderState} 是盔甲自己 fill 的第一步，
     * 一定早于控制器求值（{@code fillRenderState} 里先 capture 再跑控制器快照），
     * 而且换成僵尸/盔甲架之类的穿戴者同样成立；不像写进玩家 extract 那样只对玩家有效、
     * 还得依赖"重新 extract 会再跑一遍注入"这种隐式行为。
     */
    @Override
    public void captureDefaultRenderState(AntennaZBRitem animatable,
                                          GeoArmorRenderer.RenderData renderData,
                                          R renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, renderData, renderState, partialTick);

        LivingEntity wearer = renderData.entity();
        if (wearer == null) {
            return;
        }

        // 挥击：沿用 GeckoLib 自己的 ticket，取值口径和实体渲染器一致
        renderState.addGeckolibData(DataTickets.SWINGING_ARM, wearer.swinging);
        // 光照：不补的话 GeckoLib 的 PACKED_LIGHT 会退回"全亮"，盔甲在暗处自带发光
        renderState.addGeckolibData(DataTickets.PACKED_LIGHT, renderState.lightCoords);

        // 吸食：本模组的状态由服务端广播、客户端缓存，渲染时只负责转成 ticket
        boolean sucking = CorpseOriginClient.isAntennaSucking(wearer.getUUID());
        renderState.addGeckolibData(AntennaArmorRenderData.ABSORBING, sucking);

        // 瞄准：算出"天线根部 → 目标脑门"的方向与距离，喂给动画里的 query.target_*
        // （动画里的表达式是不带正负号的，符号统一由下面的 *_SIGN 常量控制，见常量区的注释）
        Vec3 root = new Vec3(wearer.getX(), wearer.getY() + ANTENNA_ROOT_HEIGHT, wearer.getZ());
        Vec3 aimPoint = resolveAimPoint(wearer, sucking);
        double reach = aimPoint == null ? 0.0 : root.distanceTo(aimPoint) + REACH_OFFSET;

        renderState.addGeckolibData(AntennaArmorRenderData.TARGET_X_ROTATION,
                aimTilt(root, aimPoint) * TILT_SIGN);
        renderState.addGeckolibData(AntennaArmorRenderData.TARGET_Y_ROTATION,
                aimYaw(wearer, aimPoint) * YAW_SIGN);
        // 末帧伸长量：按距离反算，让骨链伸直后的尖端正好落在脑门上
        renderState.addGeckolibData(AntennaArmorRenderData.TARGET_STRETCH,
                stretchFor(wearer, sucking, reach));
    }

    // ==================== 朝向调试用常量 ====================
    // 实测生效的是 animation.json 里的 query.target_* （调整骨骼的那次代码写入会被动画覆盖，
    // 因为 GeoArmorRenderer 的 adjustModelBonesForRender 跑在动画之前）。
    // 所以"偏了"只需要翻这里的符号 —— animation.json 里不用动：
    //   TILT_SIGN：前后偏反了就翻它（当前 +1 = 朝目标压下去）
    //   YAW_SIGN ：左右偏反了就翻它（当前 -1 = 朝目标那一侧转过去）
    //   REACH_OFFSET：尖端想再往前啃进头里给正值、想短一点给负值（单位：格）
    private static final double TILT_SIGN = 1.0;
    private static final double YAW_SIGN = 1.0;
    private static final double REACH_OFFSET = 0.0;

    /*
     * ⚠️ 这里刻意【不】做 adjustModelBonesForRender 的骨骼覆写。
     *
     * 曾经在这里直接写 bone2 的角度、bone6 的缩放，想绕开 Molang 那条链路 —— 实测证明<b>无效</b>：
     * 盔甲的 adjustModelBonesForRender 是 GeckoLib 用来"把穿戴者模型的姿势拷到
     * armorHead/armorBody… 上"的钩子，跑在动画【之前】，这里写的值随后就被动画覆盖了。
     * 证据：只改 animation.json 里的符号、完全不动这里的值，模型朝向就跟着变了；
     * 而只改这里的符号、不动 JSON，朝向毫无反应。
     *
     * 所以触手的方向与伸长量统一由 animation.json 的 query.target_* 决定，
     * 数值来源是 registerMolangQueries() 注册的 actor 变量，符号在下面 *_SIGN 常量上调。
     */

    /**
     * 天线根部在模型里的高度：{@code Antennazbr} 的 pivot 是 {@code [0, 32, 0]}，
     * 也就是脚下往上 32px ÷ 16 = 2 格（正好是头顶）。
     * <p>
     * 瞄准必须从这个点算起：拿胸口当原点的话，俯仰角会整体偏一截，天线就戳不到脑门。
     */
    private static final double ANTENNA_ROOT_HEIGHT = 2.0;

    /**
     * 天线根部到 {@code bone6} 枢轴的长度（格）：枢轴从 y=32 到 y=49.7，17.7px ÷ 16。
     * 这一段被 bone3/bone4/bone5 的旋转弯折过，伸直后贡献这么多长度。
     */
    private static final double ROOT_TO_BONE6 = 17.7 / 16.0;

    /**
     * {@code bone6} 枢轴往上那串骨骼的长度（格）：y=49.7 → y=67.1，17.4px ÷ 16。
     * {@code bone6.scale.y} 乘的就是这一段，所以"够不够长"全看这个系数。
     */
    private static final double CHAIN_ABOVE_BONE6 = 17.4 / 16.0;

    /** {@code absorb} 自己的节奏：1.0s 缩到 0.22、1.25s 刺出 —— 复刻时间轴用 */
    private static final double RETRACT_END_SECONDS = 1.0;
    private static final double THRUST_END_SECONDS = 1.25;
    private static final double RETRACTED_SCALE = 0.22;

    /** 目标实体没同步到客户端时，退而求其次瞄"正前方这么远"的虚拟点 */
    private static final double FALLBACK_AIM_DISTANCE = 6.0;

    /**
     * 末尾"刺出去"那几帧该用的 {@code bone6.scale.y}：让伸直后的骨链尖端正好落在目标脑门上。
     * <p>
     * 几何上尖端距根部 = {@link #ROOT_TO_BONE6} + {@link #CHAIN_ABOVE_BONE6} × scale，
     * 令它等于"根部到脑门的距离"就解出 scale —— 这就是所谓的"算准最后那个拉长帧"。
     * <p>
     * 只给一个恒定值会把动画自己"先缩回、再刺出"的节奏抹平，所以这里按 {@code absorb} 的时间轴
     * （1.0s 缩到 0.22、1.25s 弹出）复刻一条同样的曲线，只把最终值换成按距离算出来的数。
     * 时间取自服务端同步过来的"已吸食 tick 数"，与动画的起播时刻是同一刻。
     */
    private static double stretchFor(LivingEntity wearer, boolean sucking, double reach) {
        if (!sucking) {
            return 1.0;
        }

        double needed = Math.max(RETRACTED_SCALE, (reach - ROOT_TO_BONE6) / CHAIN_ABOVE_BONE6);

        int elapsedTicks = CorpseOriginClient.getAntennaSuckElapsed(wearer.getUUID());
        if (elapsedTicks < 0) {
            return needed;
        }

        double elapsed = elapsedTicks / 20.0;
        if (elapsed < RETRACT_END_SECONDS) {
            return Mth.lerp(elapsed / RETRACT_END_SECONDS, 1.0, RETRACTED_SCALE);
        }

        double thrust = Mth.clamp(
                (elapsed - RETRACT_END_SECONDS) / (THRUST_END_SECONDS - RETRACT_END_SECONDS), 0.0, 1.0);
        return Mth.lerp(thrust, RETRACTED_SCALE, needed);
    }

    /**
     * 吸食时的瞄准点。
     * <p>
     * 优先取被吸目标的脑门（眼睛高度）。目标实体在客户端拿不到时（还没同步过来之类），
     * 退回"穿戴着正前方 6 格、视线高度"的虚拟点 —— 这样天线至少是平着伸出去的，
     * 绝不会出现"竖直朝天"那种最难看的样子。
     *
     * @return null = 这次不瞄准（没在吸食）
     */
    @Nullable
    private static Vec3 resolveAimPoint(LivingEntity wearer, boolean sucking) {
        if (!sucking) {
            return null;
        }

        LivingEntity target = CorpseOriginClient.getAntennaSuckTarget(wearer.getUUID());
        if (target != null) {
            return target.getEyePosition();
        }

        return wearer.getEyePosition().add(wearer.getLookAngle().scale(FALLBACK_AIM_DISTANCE));
    }

    /**
     * 目标方向相对穿戴者身体朝向的水平偏角（度），喂给 {@code query.target_y_rotation}。
     * <p>
     * 约定和原版「看向目标」一致：0 = 正前方，正数 = 目标在右手边（wrap 进 ±180）。
     */
    private static double aimYaw(LivingEntity wearer, @Nullable Vec3 aimPoint) {
        if (aimPoint == null) {
            return 0.0;
        }

        double dx = aimPoint.x - wearer.getX();
        double dz = aimPoint.z - wearer.getZ();
        // 原版朝向约定：yaw 0 = +Z，顺时针为正 → yaw = atan2(-dx, dz)
        double worldYaw = Math.toDegrees(Mth.atan2(-dx, dz));

        return Mth.wrapDegrees(worldYaw - wearer.yBodyRot);
    }

    /**
     * 天线需要<b>从竖直向上偏出去多少度</b>才能指向目标，喂给 {@code query.target_x_rotation}。
     * <p>
     * 天线这一串骨骼是在模型里朝 <b>+Y（正上方）</b> 长出去的，不是朝前的枪管，
     * 所以这里不能给"俯仰角"那种 0 = 水平的数值 —— 目标跟天线同高时要偏 90° 才指得过去。
     * 具体：
     * <ul>
     *   <li>0° = 目标在正上方；</li>
     *   <li>90° = 目标与天线根部同高（完全放平，插脑门基本就是这个角度）；</li>
     *   <li>&gt;90° = 目标在下方。</li>
     * </ul>
     * 方向：先按这个 X 角把天线从竖直压下来，再按 {@link #aimYaw} 绕 Y 转到目标那一侧
     * （GeckoLib 的骨骼旋转就是这样逐轴合成的）。
     * <p>
     * <b>实测约定</b>：bone2 的 X 要取<b>负</b>这个角、Y 取<b>正</b>这个角才朝向目标
     * （先前前后翻过车、左右是对的）。代码里 {@code -tilt / +yaw} 与动画里
     * {@code [-query.target_x_rotation, query.target_y_rotation, 0]} 现在是一套符号，
     * 以后换了模型轴向的话这两处要一起改。
     */
    private static double aimTilt(Vec3 root, @Nullable Vec3 aimPoint) {
        if (aimPoint == null) {
            return 0.0;
        }

        double horizontal = Math.sqrt(
                (aimPoint.x - root.x) * (aimPoint.x - root.x) + (aimPoint.z - root.z) * (aimPoint.z - root.z));
        double dy = aimPoint.y - root.y;

        return Math.toDegrees(Mth.atan2(horizontal, dy));
    }
}
