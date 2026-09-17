package xiaoshi2022.corpseorigin.mixin;

import com.geckolib.constant.DataTickets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.ClientLimbCache;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.client.model.ExoskeletonModel;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;
import xiaoshi2022.corpseorigin.client.render.layer.ExoskeletonRenderLayer;
import xiaoshi2022.corpseorigin.client.renderer.player.CorpsePlayerGeoRenderer;
import xiaoshi2022.corpseorigin.limb.LimbSlots;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityRendererProvider.Context context, boolean slimSteve, CallbackInfo ci) {
        AvatarRenderer self = (AvatarRenderer) (Object) this;

        try {
            var modelSet = Minecraft.getInstance().getEntityModels();
            var exoskeletonModel = new ExoskeletonModel(modelSet.bakeLayer(ModModelLayers.EXOSKELETON));

            var layer = new ExoskeletonRenderLayer(self, exoskeletonModel);

            ((LivingEntityRendererMixin) self).callAddLayer(layer);

            // ✅ 保存引用
            CorpsePlayerRenderHandler.LAYER_MAP.put(self, layer);

            CorpseOrigin.LOGGER.info("✅ 外骨骼渲染层已添加到 AvatarRenderer: {}", self);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("❌ 添加外骨骼渲染层失败: {}", e.getMessage(), e);
        }

        // ✅ 断肢玩家渲染器（无状态单例，submit 时由 LivingEntityRendererSubmitMixin 调用）
        try {
            CorpsePlayerGeoRenderer.createIfAbsent(context);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("❌ 创建断肢玩家渲染器失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 把客户端缓存的断肢状态和动画信号塞进 AvatarRenderState。
     * <p>
     * GeckoLib 5.5.5 已把 GeoRenderState 混进原版所有 EntityRenderState，
     * 所以这里可以直接 addGeckolibData，无需 Supplier / 假实体那一套。
     * <p>
     * ⚠️ 两个坑：
     * <ol>
     *   <li>{@code fillRenderState} 不能省 —— 动画控制器快照
     *       （{@code DataTickets.ANIMATION_CONTROLLER_STATES}）是 GeckoLib 在 extract 阶段算好塞进
     *       render state 的，渲染时的 {@code applyAnimationControllers} 只是读这个数组，不跑这一遍就一根骨骼都不动。
     *       而这一步必须走 {@code extractRenderState(entity, ...)}（泛型保证第一个参数是实体）——
     *       直接调 {@code fillRenderState(animatable, ...)} 会在它内部把 animatable 当实体 cast，抛 ClassCastException。</li>
     *   <li><b>所有 ticket 都必须写在它之前。</b> extract 过程会当场求值动画控制器，控制器读的就是这些值；
     *       晚填的话控制器只能读到默认值 —— 比如断肢进度拿不到就会退回 {@code INTACT}（负数），
     *       于是 {@code regrow_*} 一律 STOP，动画一次都不播、断掉的手臂还完整挂在身上。</li>
     * </ol>
     */
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("RETURN")
    )
    private void corpseorigin$extractLimbState(Avatar avatar, AvatarRenderState state, float partialTick,
                                              CallbackInfo ci) {
        if (!(avatar instanceof AbstractClientPlayer player)) {
            return;
        }

        ClientLimbCache.Entry limbs = ClientLimbCache.get(player);
        if (limbs == null) {
            return;   // 四肢完好 → 照原版玩家渲染，不碰 GeckoLib
        }

        CorpsePlayerGeoRenderer renderer = CorpsePlayerGeoRenderer.get();
        if (renderer == null) {
            return;
        }

        // ① 先把控制器要读的值全部写好（此刻 AvatarRenderer 已经填好 walkAnimationSpeed / attackTime）
        //    断肢数据
        state.addGeckolibData(LimbRenderData.LIMB_MASK, limbs.mask());
        for (int slot = 0; slot < LimbSlots.COUNT; slot++) {
            state.addGeckolibData(LimbRenderData.REGROW_BY_SLOT.get(slot),
                    limbs.progress(slot, partialTick));
        }
        //    移动 / 攻击信号
        state.addGeckolibData(LimbRenderData.MOVING, state.walkAnimationSpeed > 0.02F);
        state.addGeckolibData(LimbRenderData.ATTACKING, state.attackTime > 0.0F);
        //    不补的话 GeoRenderState.getPackedLight() 会退回"全亮"，模型在暗处自带发光
        state.addGeckolibData(DataTickets.PACKED_LIGHT, state.lightCoords);

        // ② 再走官方路径：GeckoLib 补齐渲染数据 + 当场求值控制器、产出动画快照
        renderer.extractRenderState(player, state, partialTick);
    }
}
