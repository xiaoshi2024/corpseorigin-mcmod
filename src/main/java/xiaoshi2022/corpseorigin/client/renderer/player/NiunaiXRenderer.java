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
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.model.entity.NiunaiXModel;

/**
 * 开胃奶尸兄化后，在玩家背后<b>补画</b>一层 {@code niunaix}（触角 / 捆仙索 / 菊花盾）。
 * <p>
 * 和断肢 {@link CorpsePlayerGeoRenderer} 同一套路：无状态单例，不在
 * {@code EntityRendererRegistry} 里替换注册，而是由 {@code AvatarRendererMixin} 在
 * {@code AvatarRenderer} 建好时创建，再由 {@code LivingEntityRendererSubmitMixin} 在 submit 的
 * TAIL 调一次 —— 原版模型、盔甲、披风照常渲染，我们只在最上面补这层背挂。
 * <p>
 * 与断肢 / 左护法变异体的差别：
 * <ul>
 *   <li>贴图是<b>独立的</b> {@code niunaix.png}，与玩家皮肤无关（那两套是贴玩家皮肤的）；</li>
 *   <li>骨骼用的是绝对坐标（脚底 0 / 肩 24 / 头顶 32，与 {@code corpse_player.geo.json} 同一套），
 *       所以不需要任何垂直补偿就能和玩家对齐。</li>
 * </ul>
 * ⚠️ 两个类型参数都是 {@code AbstractClientPlayer} 的原因同 {@link CorpsePlayerGeoRenderer}：
 * GeckoLib 的 Molang 查询会把 animatable 强转成 {@code LivingEntity}，而
 * {@code GeoReplacedEntityRenderer} 又禁止 animatable 是 {@code Entity} —— 构造时先塞 null 绕开，
 * 再由 {@link #fillRenderState} 换成真正的玩家。
 */
@Environment(EnvType.CLIENT)
public class NiunaiXRenderer
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, AbstractClientPlayer, AvatarRenderState> {

    private static NiunaiXRenderer instance;

    public NiunaiXRenderer(EntityRendererProvider.Context context) {
        // 第三个参数传 null：见类注释，真正的宿主在 fillRenderState 里换
        super(context, new NiunaiXModel(), null);
        // 这一层是"补画"在原版玩家身上的，影子已经由原版渲染出一份了，这里不要再出一份
        this.shadowRadius = 0.0F;
    }

    /** 已初始化好的单例；AvatarRenderer 还没建过时为 null */
    public static NiunaiXRenderer get() {
        return instance;
    }

    /** 在 AvatarRenderer 的构造里调用，只建一次 */
    public static NiunaiXRenderer createIfAbsent(EntityRendererProvider.Context context) {
        if (instance == null) {
            instance = new NiunaiXRenderer(context);
            CorpseOrigin.LOGGER.info("✅ 开胃奶背挂渲染器已创建");
        }
        return instance;
    }

    /**
     * 把这一帧的动画信号写进玩家的 render state，并让 GeckoLib 当场求值控制器、产出动画快照。
     * <p>
     * ⚠️ ticket 必须写在 {@link #extractRenderState} <b>之前</b>：控制器求值时读的就是它们
     * （{@code ACTIVE} 决定这套控制器的归属，{@code ATTACKING} / {@code PARRYING} 决定播哪条）。
     */
    public static void writeRenderData(AvatarRenderState state, AbstractClientPlayer player, float partialTick) {
        NiunaiXRenderer renderer = instance;
        if (renderer == null) {
            return;
        }

        state.addGeckolibData(NiunaiXRenderData.ACTIVE, true);
        state.addGeckolibData(NiunaiXRenderData.ATTACKING, state.attackTime > 0.0F);
        state.addGeckolibData(NiunaiXRenderData.PARRYING,
                CorpseOriginClient.isNiunaiParrying(player.getUUID()));
        // 控制器没有时钟：attack 要"播满再切回 idle"，得靠这个时间基准算挥击起点
        state.addGeckolibData(NiunaiXRenderData.AGE_TICKS, state.ageInTicks);
        // 不补的话 GeoRenderState.getPackedLight() 会退回"全亮"，模型在暗处自带发光
        state.addGeckolibData(DataTickets.PACKED_LIGHT, state.lightCoords);

        renderer.extractRenderState(player, state, partialTick);
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
        return NiunaiXModel.TEXTURE;
    }

    @Override
    public RenderType getRenderType(AvatarRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
