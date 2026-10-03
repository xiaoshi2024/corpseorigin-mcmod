package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import xiaoshi2022.corpseorigin.client.model.entity.FrogZbrMcModel;
import xiaoshi2022.corpseorigin.entity.FrogZbrMcEntity;

/**
 * 青蛙奇葩尸兄绘制器 —— {@code frog_zbr_mc} geo 资源。
 * 大青蛙 + 头顶骑手（rider → Waist → Headx → brains），被射中后大脑飞出并隐藏。
 */
@Environment(EnvType.CLIENT)
public class FrogZbrMcRenderer extends GeoEntityRenderer<FrogZbrMcEntity, LivingEntityRenderState> {

    public FrogZbrMcRenderer(EntityRendererProvider.Context context) {
        super(context, new FrogZbrMcModel<>());
        this.shadowRadius = 1.0F;
    }
}
