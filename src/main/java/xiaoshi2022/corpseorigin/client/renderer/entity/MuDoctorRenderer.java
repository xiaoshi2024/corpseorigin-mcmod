package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.model.entity.MuDoctorModel;
import xiaoshi2022.corpseorigin.entity.MuDoctorEntity;

/**
 * 穆博士绘制器 —— 二阶段（金属）切贴图：每帧把 {@code getPhase() == PHASE_METAL}
 * 写进 {@link MuDoctorModel#METAL_TICKET}，模型按它切金属贴图。
 */
@Environment(EnvType.CLIENT)
public class MuDoctorRenderer extends GeoEntityRenderer<MuDoctorEntity, LivingEntityRenderState> {

    public MuDoctorRenderer(EntityRendererProvider.Context context) {
        super(context, new MuDoctorModel());
        this.shadowRadius = 0.6F;
    }

    @Override
    public void extractRenderState(MuDoctorEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.addGeckolibData(MuDoctorModel.METAL_TICKET,
                entity.getPhase() == MuDoctorEntity.PHASE_METAL);
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}