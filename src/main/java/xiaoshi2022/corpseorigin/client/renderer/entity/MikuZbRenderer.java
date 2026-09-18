package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.model.entity.MikuZbModel;
import xiaoshi2022.corpseorigin.entity.MikuZbEntity;

public class MikuZbRenderer extends GeoEntityRenderer<MikuZbEntity, LivingEntityRenderState> {

    public MikuZbRenderer(EntityRendererProvider.Context context) {
        super(context, new MikuZbModel());
        this.shadowRadius = 0.5f;
        // ✅ 添加手持物品渲染层
        this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
    }

    @Override
    public void extractRenderState(MikuZbEntity entity, LivingEntityRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);
        // 饱食度过高 → 切换为「消化不良」大肚子模型
        renderState.addGeckolibData(RenderStateData.MIKU_OVERFULL, entity.isOverfull());
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}

