package xiaoshi2022.corpseorigin.mixin;

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
import xiaoshi2022.corpseorigin.client.model.ExoskeletonModel;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;
import xiaoshi2022.corpseorigin.client.render.layer.ExoskeletonRenderLayer;
import xiaoshi2022.corpseorigin.client.renderer.player.CorpsePlayerGeoRenderer;
import xiaoshi2022.corpseorigin.client.renderer.player.MutantBodyRenderData;
import xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderData;
import xiaoshi2022.corpseorigin.client.renderer.player.NiunaiXRenderer;
import xiaoshi2022.corpseorigin.client.renderer.player.ZuoGuardianBodyRenderer;
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

        // ✅ 左护法变异体渲染器（同样是单例，整体替换玩家身体）
        try {
            ZuoGuardianBodyRenderer.createIfAbsent(context);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("❌ 创建左护法变异体渲染器失败: {}", e.getMessage(), e);
        }

        // ✅ 开胃奶背挂渲染器（单例，在玩家背后补画一层 niunaix）
        try {
            NiunaiXRenderer.createIfAbsent(context);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("❌ 创建开胃奶背挂渲染器失败: {}", e.getMessage(), e);
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
     * <p>
     * 具体动作全在 {@link CorpsePlayerGeoRenderer#writeLimbRenderData} 里 ——
     * 因为 GeckoLib 的盔甲管线会在之后把同一份 render state 覆盖掉，需要在那里再补写一次。
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
        // 左护法变异体形态：整身换成 zuo_guardian，断肢那套不参与（身体都不是同一具了）
        if (MutantBodyRenderData.isMutantBody(player)) {
            ZuoGuardianBodyRenderer.writeBodyRenderData(state, player, partialTick);
            return;
        }
        // 不在变异体形态：清掉日志标记，下次再变进来会重新打一条动画状态
        ZuoGuardianBodyRenderer.forget(player.getUUID());

        // 开胃奶背挂：与断肢那套互不相干（骨骼、动画、贴图都是另一套），可以同时存在
        if (NiunaiXRenderData.isNiunaiX(player)) {
            NiunaiXRenderer.writeRenderData(state, player, partialTick);
        }

        CorpsePlayerGeoRenderer.writeLimbRenderData(state, player, partialTick);
    }
}
