package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.growth.OrganDefinition;
import xiaoshi2022.corpseorigin.growth.OrganResourceIds;

import java.util.HashMap;
import java.util.Map;

/**
 * 克隆分身的「自定义器官」渲染器 —— {@code CustomOrganLayer.Renderer} 的分身版。
 * <p>
 * 玩家侧那套的实体类型参数写死 {@code AbstractClientPlayer}（数据也只在玩家渲染管线里生成），
 * 分身既拿不到数据、类型也对不上，所以器官在克隆人身上一直不显示。这里补一份：
 * 模型 / 贴图 / 动画走同一个 {@link OrganDefinition}，只有实体类型换成 {@link CloneAvatarEntity}。
 * <p>
 * <b>每个器官一个实例</b>（器官的模型与动画是随定义变的），并按 {@code def.id()+索引} 缓存；
 * 每个实例还各自占一个 {@code instanceId} —— 这样分身的动画缓存会为它们分别建管理器，
 * 多个器官的动画不会互相串台（与玩家侧的 {@code prepareOrgan} 是同一个目的）。
 * <p>
 * ⚠️ 资源路径必须走 {@link OrganResourceIds} 的转换（目录里的器官表存的是物理文件路径，
 * 而 GeckoLib 要的是去前缀去后缀的裸 id）—— 直接塞完整路径 GeckoLib 会报
 * "Superfluous prefix or suffix" 然后找不到文件。
 * <p>
 * ⚠️ 用法是 {@code performRenderPass}（调用方自己摆 pose），所以
 * {@link #adjustRenderPose} / {@link #scaleModelForRender} 必须留空，
 * 否则 GeckoLib 会再套一次实体变换、把模型推到别处。
 */
@Environment(EnvType.CLIENT)
public final class CloneOrganRenderer
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, CloneAvatarEntity, AvatarRenderState> {

    private static final Map<String, CloneOrganRenderer> RENDERERS = new HashMap<>();
    private static EntityRendererProvider.Context context;

    private final OrganDefinition def;
    private final int slot;

    private CloneOrganRenderer(EntityRendererProvider.Context ctx, OrganDefinition def, int slot) {
        super(ctx, new Model(def), null);
        this.def = def;
        this.slot = slot;
        this.shadowRadius = 0.0F;
    }

    /** 在分身渲染器构造里调用一次：拿到创建渲染器要用到的 context */
    public static void init(EntityRendererProvider.Context ctx) {
        if (context != ctx) RENDERERS.clear();
        context = ctx;
    }

    public static boolean ready() {
        return context != null;
    }

    /** 取（或创建）某个器官在第 {@code index} 个槽位上的渲染器 */
    public static CloneOrganRenderer get(OrganDefinition def, int index) {
        if (context == null) {
            return null;
        }
        String key = def.id() + ":" + index;
        CloneOrganRenderer renderer = RENDERERS.get(key);
        if (renderer == null) {
            renderer = new CloneOrganRenderer(context, def, index);
            RENDERERS.put(key, renderer);
        }
        return renderer;
    }

    /** 每个器官一个独立的动画管理器（否则多器官共用一条时间轴，动画会互相覆盖） */
    @Override
    public long getInstanceId(PlayerGeoAnimatable animatable, CloneAvatarEntity entity) {
        return Long.MIN_VALUE + 1 + this.slot;
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

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return Identifier.parse(this.def.texture());
    }

    // ==================== 不要重复做实体变换（performRenderPass 专用） ====================

    @Override
    public void adjustRenderPose(RenderPassInfo<AvatarRenderState> info) {
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<AvatarRenderState> info, float widthScale, float heightScale) {
    }

    /** geo / 贴图 / 动画全部按器官定义走，与玩家侧同源（含路径转换）。 */
    private static final class Model extends DefaultedEntityGeoModel<PlayerGeoAnimatable> {
        private final OrganDefinition def;

        Model(OrganDefinition def) {
            super(Identifier.parse("corpseorigin:organ_bat"));
            this.def = def;
        }

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return Identifier.parse(OrganResourceIds.model(this.def.model()));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return Identifier.parse(this.def.texture());
        }

        @Override
        public Identifier getAnimationResource(PlayerGeoAnimatable animatable) {
            return Identifier.parse(OrganResourceIds.animation(this.def.animation()));
        }
    }
}
