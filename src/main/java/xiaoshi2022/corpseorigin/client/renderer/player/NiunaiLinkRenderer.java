package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.model.entity.NiunaiLinkPlayerModel;

/**
 * 开胃奶「拦腰斩断」形态下，用 {@code niunai_link_player} <b>整身替换</b>玩家。
 * <p>
 * 和左护法变异体（{@link ZuoGuardianBodyRenderer}）同一套路：无状态单例，不在
 * {@code EntityRendererRegistry} 里替换注册，而是由 {@code AvatarRendererMixin} 在
 * {@code AvatarRenderer} 建好时创建，再由 {@code LivingEntityRendererSubmitMixin} 在 submit 的
 * HEAD 接管并 cancel —— 原版模型、盔甲、披风、外骨骼层一律不画，只出这具"被斩成两截"的身体。
 * <p>
 * 与变异体的差别只有贴图：那套是每帧按玩家皮肤合成的组合纹理，这里直接是<b>玩家本人皮肤</b>
 * （模型骨骼就是原版玩家那套 UV，见 {@link NiunaiLinkPlayerModel}）。
 * <p>
 * ⚠️ 两个类型参数都是 {@code AbstractClientPlayer} 的原因同 {@link CorpsePlayerGeoRenderer}：
 * GeckoLib 的 Molang 查询会把 animatable 强转成 {@code LivingEntity}，而
 * {@code GeoReplacedEntityRenderer} 又禁止 animatable 是 {@code Entity} —— 构造时先塞 null 绕开，
 * 再由 {@link #fillRenderState} 换成真正的玩家。
 */
@Environment(EnvType.CLIENT)
public class NiunaiLinkRenderer
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, AbstractClientPlayer, AvatarRenderState> {

    private static NiunaiLinkRenderer instance;

    public NiunaiLinkRenderer(EntityRendererProvider.Context context) {
        // 第三个参数传 null：见类注释，真正的宿主在 fillRenderState 里换
        super(context, new NiunaiLinkPlayerModel(), null);
        PlayerHandLayers.attach(context, this);
        // 原版 submit 被 cancel 掉了，影子得由我们自己出，给回玩家默认值
        this.shadowRadius = 0.5F;
    }

    /** 已初始化好的单例；AvatarRenderer 还没建过时为 null */
    public static NiunaiLinkRenderer get() {
        return instance;
    }

    /** 在 AvatarRenderer 的构造里调用，只建一次 */
    public static NiunaiLinkRenderer createIfAbsent(EntityRendererProvider.Context context) {
        if (instance == null) {
            instance = new NiunaiLinkRenderer(context);
            CorpseOrigin.LOGGER.info("✅ 开胃奶拦腰斩断渲染器已创建");
        }
        return instance;
    }

    /**
     * 把"身体那趟"的动画信号写进玩家的 render state，并让 GeckoLib 当场求值控制器、产出动画快照。
     * <p>
     * ⚠️ ticket 必须写在 {@link #extractRenderState} <b>之前</b>，控制器求值时读的就是它们。
     * {@code ACTIVE} 由 {@code AvatarRendererMixin} 每帧统一写（它决定这一帧要不要整身替换）；
     * 这里只补 {@code LINKING}（播 {@code broken_off} 还是 {@code link}）和 {@code BODY_PASS}（本趟是谁）。
     *
     * @return 是否真的接管了这一帧
     */
    public static boolean writeRenderData(AvatarRenderState state, AbstractClientPlayer player, float partialTick) {
        NiunaiLinkRenderer renderer = instance;
        if (renderer == null || !NiunaiLinkRenderData.isNiunaiLink(player)) {
            return false;
        }

        state.addGeckolibData(NiunaiLinkRenderData.LINKING,
                NiunaiLinkRenderData.isLinking(player.getUUID()));
        // 本趟是"身体"那趟：broken_off / link 在这一趟解析
        state.addGeckolibData(NiunaiLinkRenderData.BODY_PASS, true);
        // 不补的话 GeoRenderState.getPackedLight() 会退回"全亮"，模型在暗处自带发光
        state.addGeckolibData(DataTickets.PACKED_LIGHT, state.lightCoords);

        renderer.extractRenderState(player, state, partialTick);
        return true;
    }

    /** 同 {@link CorpsePlayerGeoRenderer}：动画宿主不能用渲染器字段里那个 null */
    @Override
    public AvatarRenderState fillRenderState(PlayerGeoAnimatable animatable, AbstractClientPlayer entity,
                                            AvatarRenderState renderState, float partialTick) {
        return super.fillRenderState((PlayerGeoAnimatable) entity, entity, renderState, partialTick);
    }

    /** 同理，createRenderState 也别用那个 null 宿主 */
    @Override
    public AvatarRenderState createRenderState(PlayerGeoAnimatable animatable, AbstractClientPlayer entity) {
        return super.createRenderState((PlayerGeoAnimatable) entity, entity);
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState renderState) {
        if (renderState.skin != null) {
            return renderState.skin.body().texturePath();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    public RenderType getRenderType(AvatarRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
