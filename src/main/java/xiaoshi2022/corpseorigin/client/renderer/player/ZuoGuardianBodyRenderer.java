package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.DataTickets;
import com.geckolib.animation.state.AnimationPoint;
import com.geckolib.animation.state.ControllerState;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.model.entity.ZuoGuardianBodyModel;
import xiaoshi2022.corpseorigin.client.skin.ZuoGuardianSkinBuilder;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 左护法变异体形态下，用 {@code zuo_guardian}（巨蛇 + 骑手）整身替换玩家。
 * <p>
 * 和断肢的 {@link CorpsePlayerGeoRenderer} 同一套路：无状态单例，不在
 * {@code EntityRendererRegistry} 里替换注册，而是由 {@code AvatarRendererMixin} 在
 * {@code AvatarRenderer} 建好时创建，再由 {@code LivingEntityRendererSubmitMixin} 在 submit 时调用。
 * <p>
 * 差别只有两点：
 * <ul>
 *   <li>断肢是"分肢体替换"（原版模型继续出画），这里是<b>整体替换</b> —— 原版模型、盔甲、披风一律不画，
 *       所以提交点是 submit 的 HEAD 并 cancel（见 {@code LivingEntityRendererSubmitMixin}），
 *       代价是这个名字牌也不画；</li>
 *   <li>纹理是每帧按玩家皮肤合成的组合纹理（{@link ZuoGuardianSkinBuilder}），
 *       而不是现成的 zuo_guardian.png。</li>
 * </ul>
 * ⚠️ 两个类型参数都是 {@code AbstractClientPlayer} 的原因同 {@link CorpsePlayerGeoRenderer}：
 * GeckoLib 的 Molang 查询会把 animatable 强转成 {@code LivingEntity}，而
 * {@code GeoReplacedEntityRenderer} 又禁止 animatable 是 {@code Entity} —— 构造时先塞 null 绕开，
 * 再由 {@link #fillRenderState} 换成真正的玩家。
 */
@Environment(EnvType.CLIENT)
public class ZuoGuardianBodyRenderer
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, AbstractClientPlayer, AvatarRenderState> {

    private static ZuoGuardianBodyRenderer instance;

    /** 已经打过"变异体已启用"日志的玩家，避免每帧刷屏；退出形态时清掉（见 {@link #forget}） */
    private static final Set<UUID> LOGGED = ConcurrentHashMap.newKeySet();

    public ZuoGuardianBodyRenderer(EntityRendererProvider.Context context) {
        // 第三个参数传 null：见类注释，真正的宿主在 fillRenderState 里换
        super(context, new ZuoGuardianBodyModel(), null);
        this.shadowRadius = 0.7F;   // 巨蛇体型比玩家宽，影子给大一点

        // 手持物品渲染层（模型骨骼名须为 RightHandItem / LeftHandItem）。
        // ⚠️ 内置层把泛型约束成 "<T extends LivingEntity & GeoAnimatable>"，替换渲染器的 T
        //    在编译期只是 PlayerGeoAnimatable 接口，直接 new 会推断失败；但运行时传到层里的 T
        //    就是 AbstractClientPlayer 本人（mixin 让玩家实现了 PlayerGeoAnimatable，
        //    见 ClientPlayerGeoAnimatableMixin），强转成立，所以用裸类型桥接编译期约束。
        @SuppressWarnings({"rawtypes", "unchecked"})
        GeoRenderLayer<PlayerGeoAnimatable, AbstractClientPlayer, AvatarRenderState> handLayer =
                (GeoRenderLayer) new ItemInHandGeoLayer(context, this);
        this.withRenderLayer(handLayer);
    }

    /** 已初始化好的单例；AvatarRenderer 还没建过时为 null */
    public static ZuoGuardianBodyRenderer get() {
        return instance;
    }

    /** 在 AvatarRenderer 的构造里调用，只建一次 */
    public static ZuoGuardianBodyRenderer createIfAbsent(EntityRendererProvider.Context context) {
        if (instance == null) {
            instance = new ZuoGuardianBodyRenderer(context);
            CorpseOrigin.LOGGER.info("✅ 左护法变异体渲染器已创建");
        }
        return instance;
    }

    /**
     * 把这一帧的整身纹理与动画信号写进玩家的 render state，并让 GeckoLib 当场求值控制器、产出动画快照。
     * <p>
     * ⚠️ ticket 必须写在 {@link #extractRenderState} <b>之前</b>：控制器求值时读的就是它们
     * （{@code ACTIVE} 决定播 zuo_guardian 的 idle / riderx_attack，见 {@code ClientPlayerGeoAnimatableMixin}）。
     * 组合纹理合成失败（底图缺失之类）时不写 ticket，这一帧就退回原版渲染，而不是画个没贴图的东西。
     *
     * @return 是否真的接管了这一帧
     */
    public static boolean writeBodyRenderData(AvatarRenderState state, AbstractClientPlayer player, float partialTick) {
        ZuoGuardianBodyRenderer renderer = instance;
        if (renderer == null) {
            return false;
        }

        Identifier skinTexture = state.skin == null ? null : state.skin.body().texturePath();
        Identifier bodyTexture = ZuoGuardianSkinBuilder.acquire(skinTexture);
        if (bodyTexture == null) {
            return false;
        }

        state.addGeckolibData(MutantBodyRenderData.BODY_TEXTURE, bodyTexture);
        state.addGeckolibData(MutantBodyRenderData.ACTIVE, true);
        // 状态信号：专属的 mutant_body 控制器据此查对应表挑动画（见 MutantBodyAnimations）
        state.addGeckolibData(MutantBodyRenderData.MOVING, state.walkAnimationSpeed > 0.02F);
        state.addGeckolibData(MutantBodyRenderData.SWIMMING, state.isInWater);
        state.addGeckolibData(MutantBodyRenderData.SNEAKING, state.isCrouching);
        state.addGeckolibData(LimbRenderData.ATTACKING, state.attackTime > 0.0F);
        // 不补的话 GeoRenderState.getPackedLight() 会退回"全亮"，模型在暗处自带发光
        state.addGeckolibData(DataTickets.PACKED_LIGHT, state.lightCoords);

        renderer.extractRenderState(player, state, partialTick);

        // 每个玩家只在刚变成变异体时打一条：把这一帧真正求值出来的动画列出来。
        // 「0 条」= 控制器压根没跑（动画自然不播），列出名字 = GeckoLib 已经在放这几条动画。
        if (LOGGED.add(player.getUUID())) {
            CorpseOrigin.LOGGER.info("左护法变异体已启用：{}，当前动画 = {}",
                    player.getName().getString(), describeAnimations(state));
        }
        return true;
    }

    /** 这一帧求值出来的动画名（停住的控制器不列） */
    private static String describeAnimations(AvatarRenderState state) {
        ControllerState[] states = state.getGeckolibData(DataTickets.ANIMATION_CONTROLLER_STATES);
        if (states == null || states.length == 0) {
            return "0 条（控制器没有求值）";
        }
        StringBuilder names = new StringBuilder();
        for (ControllerState controllerState : states) {
            AnimationPoint point = controllerState == null ? null : controllerState.animationPoint();
            if (point == null || point.animation() == null) {
                continue;
            }
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(point.animation().name());
        }
        return names.length() == 0 ? "全部空闲" : names.toString();
    }

    /** 退出变异体形态时调用：下次再变进来会重新打一条动画状态日志 */
    public static void forget(UUID uuid) {
        LOGGED.remove(uuid);
    }

    /** 同 {@link CorpsePlayerGeoRenderer}：动画宿主不能用渲染器字段里那个 null */
    @Override
    public AvatarRenderState fillRenderState(PlayerGeoAnimatable animatable, AbstractClientPlayer entity,
                                            AvatarRenderState renderState, float partialTick) {
        return super.fillRenderState((PlayerGeoAnimatable) entity, entity, renderState, partialTick);
    }

    @Override
    public AvatarRenderState createRenderState(PlayerGeoAnimatable animatable, AbstractClientPlayer entity) {
        return super.createRenderState((PlayerGeoAnimatable) entity, entity);
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState renderState) {
        Identifier texture = renderState.getGeckolibData(MutantBodyRenderData.BODY_TEXTURE);
        return texture != null ? texture : super.getTextureLocation(renderState);
    }

    @Override
    public RenderType getRenderType(AvatarRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
