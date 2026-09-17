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
     * 把 {@code query.target_x_rotation} / {@code query.target_y_rotation} 注册成 GeckoLib 的 actor 变量。
     * <p>
     * 这两个名字 GeckoLib 5.5.5 里<b>没有</b>内置实现（扫过整个 jar，MolangQueries 里一个都没有），
     * 所以 animation.json 里直接写它们只会恒等于 0，触手永远不转向。
     * <p>
     * 注册成 actor 变量而不是 {@code setVariableValue} 那种全局值，是因为 resolver 每次渲染都会拿到
     * 当前动画宿主：{@code Actor} 里就带着本次渲染用的 render state，直接读上面写进去的角度即可 ——
     * 多人同时穿这套盔甲、同屏多个目标也不会互相串值。
     */
    public static void registerMolangQueries() {
        MolangQueries.setActorVariable("query.target_x_rotation",
                actor -> readAngle(actor.renderState(), AntennaArmorRenderData.TARGET_X_ROTATION));
        MolangQueries.setActorVariable("query.target_y_rotation",
                actor -> readAngle(actor.renderState(), AntennaArmorRenderData.TARGET_Y_ROTATION));
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
        renderState.addGeckolibData(AntennaArmorRenderData.ABSORBING,
                CorpseOriginClient.isAntennaSucking(wearer.getUUID()));

        // 吸食目标方向 → 驱动 absorb 里 bone2 的 query.target_*_rotation
        LivingEntity target = CorpseOriginClient.getAntennaSuckTarget(wearer.getUUID());
        renderState.addGeckolibData(AntennaArmorRenderData.TARGET_Y_ROTATION, aimYaw(wearer, target));
        renderState.addGeckolibData(AntennaArmorRenderData.TARGET_X_ROTATION, aimTilt(wearer, target));
    }

    /**
     * 天线根部在模型里的高度：{@code Antennazbr} 的 pivot 是 {@code [0, 32, 0]}，
     * 也就是脚下往上 32px ÷ 16 = 2 格（正好是头顶）。
     * <p>
     * 瞄准必须从这个点算起：拿胸口当原点的话，俯仰角会整体偏一截，天线就戳不到脑门。
     */
    private static final double ANTENNA_ROOT_HEIGHT = 2.0;

    /**
     * 目标方向相对穿戴者身体朝向的水平偏角（度），喂给 {@code query.target_y_rotation}。
     * <p>
     * 约定和原版「看向目标」一致：0 = 正前方，正数 = 目标在右手边（wrap 进 ±180）。
     */
    private static double aimYaw(LivingEntity wearer, LivingEntity target) {
        if (target == null) {
            return 0.0;
        }

        double dx = target.getX() - wearer.getX();
        double dz = target.getZ() - wearer.getZ();
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
     * （GeckoLib 的骨骼旋转就是这样逐轴合成的，动画里 {@code bone2} 用
     * {@code [-query.target_x_rotation, -query.target_y_rotation, 0]} 正好对上）。
     * 如果实测仰/俯反了，把 animation.json 里那个负号去掉；左右反了就改 Y 那个负号。
     */
    private static double aimTilt(LivingEntity wearer, LivingEntity target) {
        if (target == null) {
            return 0.0;
        }

        double dx = target.getX() - wearer.getX();
        double dz = target.getZ() - wearer.getZ();
        // 目标取"脑门"＝眼睛高度，比身体中心更贴"插脑门"这个动作
        double dy = target.getEyeY() - (wearer.getY() + ANTENNA_ROOT_HEIGHT);

        return Math.toDegrees(Mth.atan2(Math.sqrt(dx * dx + dz * dz), dy));
    }
}
