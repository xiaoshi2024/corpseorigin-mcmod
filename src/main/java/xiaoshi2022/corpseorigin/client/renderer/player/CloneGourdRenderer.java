package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;

/**
 * 克隆分身的「葫芦」（{@code zbr_gourd}）渲染器 —— 葫芦小金刚背上的那套骨骼。
 * <p>
 * 玩家侧它是作为<b>器官</b>挂在 {@code body} 骨骼上渲染的（{@code CustomOrganLayer} +
 * {@code GourdOrganState}），不是整身替换，所以那条通道只认 {@code AbstractClientPlayer}、
 * 分身用不了 —— 这里单独复刻一份：模型 / 动画与玩家侧同名同源，只有<b>贴图</b>按形态变色
 * （dark / gold / purple / red）。
 * <p>
 * ⚠️ 类型参数与构造传 null 的原因见 {@link CloneCreatureRenderer}。
 */
@Environment(EnvType.CLIENT)
public final class CloneGourdRenderer
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, CloneAvatarEntity, AvatarRenderState> {

    /** 这一帧的贴图颜色（dark / gold / purple / red），由形态决定 */
    public static final DataTicket<String> COLOR = DataTicket.create("clone_gourd_color", String.class);
    /** 这一帧播哪条 clip（idle / eyez / hand / snake / power …） */
    public static final DataTicket<String> CLIP = DataTicket.create("clone_gourd_clip", String.class);

    private static CloneGourdRenderer instance;

    private CloneGourdRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), null);
        // 葫芦挂在身体上，影子由本体出一份
        this.shadowRadius = 0.0F;
    }

    public static void createIfAbsent(EntityRendererProvider.Context context) {
        if (instance == null) {
            instance = new CloneGourdRenderer(context);
        }
    }

    public static CloneGourdRenderer get() {
        return instance;
    }

    @Override public boolean shouldShowName(CloneAvatarEntity entity, double distanceSquared) { return false; }

    /** The owning body submits entity labels; an attached model must never submit them again. */
    @Override public void postRenderPass(com.geckolib.renderer.base.RenderPassInfo<AvatarRenderState> info,
                                        net.minecraft.client.renderer.SubmitNodeCollector collector) {}

    @Override
    public long getInstanceId(PlayerGeoAnimatable animatable, CloneAvatarEntity entity) {
        return Long.MIN_VALUE + 17;
    }

    @Override
    public AvatarRenderState fillRenderState(PlayerGeoAnimatable animatable, CloneAvatarEntity entity,
                                            AvatarRenderState state, float partialTick) {
        var host = (PlayerGeoAnimatable) entity;
        if (host.getAnimatableInstanceCache() instanceof xiaoshi2022.corpseorigin.client.limb.PlayerLayerAnimationCache cache)
            cache.prepareOrgan(getInstanceId(host, entity), this);
        return super.fillRenderState(host, entity, state, partialTick);
    }

    @Override
    public AvatarRenderState createRenderState(PlayerGeoAnimatable animatable, CloneAvatarEntity entity) {
        return super.createRenderState((PlayerGeoAnimatable) entity, entity);
    }

    // ==================== 关键：不要重复做实体变换 ====================

    /*
     * 葫芦走的是 performRenderPass —— 调用方（分身的层 / 仓内渲染）已经自己摆好了 pose：
     * 模型根 → body 骨骼 → 锚点偏移 → 缩放。
     * <p>
     * 而 GeoReplacedEntityRenderer 默认还会在这两处再套一次"实体变换"（实体朝向 +
     * scale(-1,-1,1) + 垂直位移），于是模型被transform两遍、落到看不见的地方。
     * 玩家侧的器官渲染器（CustomOrganLayer.Renderer / ZbOrganLayer.OrganRenderer）
     * 同样是这么处理的：把这两个钩子留空。
     */

    @Override
    public void adjustRenderPose(com.geckolib.renderer.base.RenderPassInfo<AvatarRenderState> info) {
    }

    @Override
    public void scaleModelForRender(com.geckolib.renderer.base.RenderPassInfo<AvatarRenderState> info,
                                    float widthScale, float heightScale) {
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return textureOf(state.getGeckolibData(COLOR));
    }

    public static Identifier textureOf(String color) {
        return CorpseOrigin.id("textures/entity/zbr_gourd/" + (color == null ? "dark" : color) + ".png");
    }

    /** geo / 动画与玩家侧的葫芦器官完全同一份文件，只有贴图随形态变色。 */
    private static final class Model extends DefaultedEntityGeoModel<PlayerGeoAnimatable> {
        Model() {
            // ★ 用葫芦自己的 id：模型 / 动画都按 GeckoLib 的约定派生
            //   （geckolib/models/entity/zbr_gourd.geo.json、geckolib/animations/entity/zbr_gourd.animation.json），
            //   与玩家侧的葫芦器官是同一份文件。
            //   ⚠️ 不能借用 organ_bat 之类别的 id —— GeckoLib 的模型是按 id 缓存烘焙结果的，
            //   两个渲染器共用一个 id 会互相串台（玩家的器官渲染也会一起遭殃）。
            super(CorpseOrigin.id("zbr_gourd"));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            // 只有贴图按形态变色（dark / gold / purple / red）
            return textureOf(state.getGeckolibData(COLOR));
        }
    }
}
