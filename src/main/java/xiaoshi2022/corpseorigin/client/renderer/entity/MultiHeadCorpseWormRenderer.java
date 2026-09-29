package xiaoshi2022.corpseorigin.client.renderer.entity;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.model.entity.MultiHeadCorpseWormModel;
import xiaoshi2022.corpseorigin.entity.MultiHeadCorpseWormEntity;
public class MultiHeadCorpseWormRenderer extends GeoEntityRenderer<MultiHeadCorpseWormEntity, LivingEntityRenderState> {
    public MultiHeadCorpseWormRenderer(EntityRendererProvider.Context context) {
        super(context, new MultiHeadCorpseWormModel());
        shadowRadius = 2.2f;
        withRenderLayer(new xiaoshi2022.corpseorigin.client.render.layer.WormFacesLayer(this));
    }
    @Override public RenderType getRenderType(LivingEntityRenderState state, Identifier texture) { return RenderTypes.entityCutout(texture); }
    @Override public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float width, float height) {
        info.poseStack().scale(4f, 4f, 4f);
    }
}
