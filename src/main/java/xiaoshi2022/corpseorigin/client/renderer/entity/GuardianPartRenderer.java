package xiaoshi2022.corpseorigin.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import xiaoshi2022.corpseorigin.entity.GuardianPartEntity;

/**
 * 蛟龙节碰撞箱的绘制器：<b>什么都不画</b>。
 * <p>
 * 这些节是隐形实体，只为"能被射线选中 + 挨打"存在；但实体本身要同步到客户端才能选中，
 * 所以仍得给它注册一个绘制器（{@code submit} 用父类空实现），否则渲染分发会找不到绘制器。
 * <p>
 * 想看这些箱子的位置：进游戏按 {@code F3+B}（原版碰撞箱调试显示）。
 */
@Environment(EnvType.CLIENT)
public class GuardianPartRenderer extends EntityRenderer<GuardianPartEntity, EntityRenderState> {

    public GuardianPartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
