package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.model.entity.ZuoFloodLongModel;
import xiaoshi2022.corpseorigin.entity.ZuoFloodLongEntity;

/**
 * 尸蛟龙（宠物 BOSS）的绘制器。
 * <p>
 * 模型用的是它自己那套资源（{@code zuo_flood_long}，纯龙、没有骑手），
 * 和玩家蛟龙形态（{@code zuo_guardian}，龙 + 骑手）分开，所以不需要在这里做任何贴图合成。
 * <p>
 * 缩放不用在这里管：实体构造时把配置里的缩放写进了 {@code SCALE} 属性，
 * GeckoLib 的 {@code GeoEntityRenderer} 会自己乘上 render state 的 scale（碰撞箱同理）。
 */
@Environment(EnvType.CLIENT)
public class ZuoFloodLongRenderer extends GeoEntityRenderer<ZuoFloodLongEntity, LivingEntityRenderState> {

    public ZuoFloodLongRenderer(EntityRendererProvider.Context context) {
        super(context, new ZuoFloodLongModel());
        this.shadowRadius = 0.7F;
    }

    @Override
    public RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
