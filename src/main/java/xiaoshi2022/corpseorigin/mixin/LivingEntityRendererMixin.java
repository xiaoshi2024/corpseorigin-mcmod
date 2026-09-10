package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// ✅ 3个泛型参数：T=实体, S=渲染状态, M=模型
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Invoker("addLayer")
    boolean callAddLayer(RenderLayer<S, M> layer);
}