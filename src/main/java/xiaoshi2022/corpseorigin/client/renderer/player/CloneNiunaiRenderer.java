package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.model.entity.NiunaiXModel;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;

/**
 * 克隆分身的「开胃奶背挂」渲染器 —— {@link NiunaiXRenderer} 的分身版。
 * <p>
 * 模型与贴图直接复用 {@link NiunaiXModel}（{@code niunaix}：触角 / 捆仙索 / 菊花盾），
 * 只在实体类型上换成 {@link CloneAvatarEntity}：分身是开胃奶身体时，
 * 背后同样该挂出这一套骨骼。
 * <p>
 * ⚠️ 类型参数与构造传 null 的原因见 {@link CloneCreatureRenderer}。
 */
@Environment(EnvType.CLIENT)
public final class CloneNiunaiRenderer
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, CloneAvatarEntity, AvatarRenderState> {

    private static CloneNiunaiRenderer instance;

    private CloneNiunaiRenderer(EntityRendererProvider.Context context) {
        super(context, new NiunaiXModel(), null);
        // 这一层是"补画"在原版模型上的，影子已经由本体出一份了
        this.shadowRadius = 0.0F;
    }

    public static void createIfAbsent(EntityRendererProvider.Context context) {
        if (instance == null) {
            instance = new CloneNiunaiRenderer(context);
        }
    }

    public static CloneNiunaiRenderer get() {
        return instance;
    }

    @Override
    public AvatarRenderState fillRenderState(PlayerGeoAnimatable animatable, CloneAvatarEntity entity,
                                            AvatarRenderState state, float partialTick) {
        return super.fillRenderState((PlayerGeoAnimatable) entity, entity, state, partialTick);
    }

    @Override
    public AvatarRenderState createRenderState(PlayerGeoAnimatable animatable, CloneAvatarEntity entity) {
        return super.createRenderState((PlayerGeoAnimatable) entity, entity);
    }

    @Override
    public void adjustRenderPose(com.geckolib.renderer.base.RenderPassInfo<AvatarRenderState> info) {}

    @Override
    public void scaleModelForRender(com.geckolib.renderer.base.RenderPassInfo<AvatarRenderState> info, float x, float y) {}

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return NiunaiXModel.TEXTURE;
    }

    @Override
    public RenderType getRenderType(AvatarRenderState state, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
