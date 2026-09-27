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
import xiaoshi2022.corpseorigin.client.renderer.player.*;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityRendererProvider.Context context, boolean slimSteve, CallbackInfo ci) {
        AvatarRenderer self = (AvatarRenderer) (Object) this;
        xiaoshi2022.corpseorigin.client.renderer.player.MutantSalmonRenderer.create(context);
        xiaoshi2022.corpseorigin.client.renderer.player.CreaturePlayerRenderer.create(context);
        ((LivingEntityRendererMixin)self).callAddLayer(new xiaoshi2022.corpseorigin.client.render.layer.ChameleonHeadLayer(self));
        ((LivingEntityRendererMixin)self).callAddLayer(new xiaoshi2022.corpseorigin.client.render.layer.ChapterCostumeLayer(self));
        ((LivingEntityRendererMixin)self).callAddLayer(new xiaoshi2022.corpseorigin.client.render.layer.QiCoatingLayer(self));
        EvolutionGeoRenderer.create(context);
        ((LivingEntityRendererMixin)self).callAddLayer(new xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer(self,context));

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

        try {
            TianGangHaloRenderer.createIfAbsent(context);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("❌ 创建天罡光环渲染器失败: {}", e.getMessage(), e);
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

        try {
            ShiChaoZhiZiBodyRenderer.createIfAbsent(context);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("Failed to create Shi Chao Zhi Zi body renderer", e);
        }

        // ✅ 开胃奶背挂渲染器（单例，在玩家背后补画一层 niunaix）
        try {
            NiunaiXRenderer.createIfAbsent(context);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("❌ 创建开胃奶背挂渲染器失败: {}", e.getMessage(), e);
        }

        // ✅ 开胃奶「拦腰斩断」渲染器（单例，整身换成 niunai_link_player）
        try {
            NiunaiLinkRenderer.createIfAbsent(context);
        } catch (Exception e) {
            CorpseOrigin.LOGGER.error("❌ 创建开胃奶拦腰斩断渲染器失败: {}", e.getMessage(), e);
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
        state.addGeckolibData(EvolutionGeoRenderer.SNAPSHOT, null);
        xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.extract(player,state,partialTick);
        EvolutionGeoRenderer.extract(player, state, partialTick);
        if (xiaoshi2022.corpseorigin.skill.chapter.GourdInheritance.disguised(player)) {
            state.skin = player.getSkin();
            state.addGeckolibData(xiaoshi2022.corpseorigin.client.limb.LimbRenderData.LIMB_MASK, null);
            state.addGeckolibData(MutantBodyRenderData.BODY_TEXTURE, null);
            state.addGeckolibData(MutantSalmonRenderer.ACTIVE, false);
            state.addGeckolibData(NiunaiXRenderData.ACTIVE, false);
            state.addGeckolibData(NiunaiLinkRenderData.ACTIVE, false);
            state.addGeckolibData(TianGangHaloRenderData.ACTIVE, false);
            state.addGeckolibData(ShiChaoBodyRenderData.ACTIVE, false);
            return;
        }
        String disguise=player.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.DISGUISE);
        String possessedSkin = player.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.longyou.BodyPossession.SKIN);
        if (!possessedSkin.isEmpty()) {
            try { state.skin = xiaoshi2022.corpseorigin.client.skin.clone.ClientSkinCache.resolve(java.util.UUID.fromString(possessedSkin)); }
            catch (IllegalArgumentException ignored) { }
        }
        if("xiaohui".equals(player.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.ROLE)))state.skin=xiaoshi2022.corpseorigin.client.skin.ChameleonSkins.XIAOHUI;
        if(!disguise.isEmpty()) {
            // Replace only a visible overhead label; keep vanilla team/distance visibility rules.
            if (state.nameTag != null) {
                if (disguise.equals(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.XIAOHUI)) {
                    state.nameTag = net.minecraft.network.chat.Component.literal("xiaohui");
                } else {
                    var disguiseProfile = player.getAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.DISGUISE_PROFILE);
                    if (disguiseProfile != null && disguiseProfile.id().toString().equals(disguise)) {
                        state.nameTag = net.minecraft.network.chat.Component.literal(disguiseProfile.name());
                    }
                }
            }
            if(disguise.equals(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.XIAOHUI))state.skin=xiaoshi2022.corpseorigin.client.skin.ChameleonSkins.XIAOHUI;
            else try {
                var profile=player.getAttached(xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState.DISGUISE_PROFILE);
                state.skin=profile!=null && profile.id().toString().equals(disguise)
                        ? xiaoshi2022.corpseorigin.client.skin.ChameleonSkins.resolve(profile)
                        : xiaoshi2022.corpseorigin.client.skin.clone.ClientSkinCache.resolve(java.util.UUID.fromString(disguise));
            }
            catch(IllegalArgumentException ignored) { }
        }

        // ★ 每帧先把两套开胃奶形态的门控显式写成 false。
        //   下面那些分支（尤其是变异体那条早退）不一定会写它们，而控制器一旦读到 null 就会
        //   "原样 CONTINUE"——于是上一条动画会一直挂在控制器上，被别的角色 / 形态的模型也读到
        //   （表现：左护法变异体的动画列表里混进了开胃奶的 link，像是"感染"了别的模型）。
        //   显式 false 能让它们整条停掉，不再泄漏。
        state.addGeckolibData(NiunaiXRenderData.ACTIVE, false);
        state.addGeckolibData(NiunaiLinkRenderData.ACTIVE, false);
        state.addGeckolibData(TianGangHaloRenderData.ACTIVE, false);
        state.addGeckolibData(ShiChaoBodyRenderData.ACTIVE, false);
        if (xiaoshi2022.corpseorigin.client.renderer.player.CreaturePlayerRenderer.extract(player,state,partialTick)) return;
        if (xiaoshi2022.corpseorigin.client.renderer.player.MutantSalmonRenderer.extract(player,state,partialTick)) return;

        if (ShiChaoBodyRenderData.isActive(player)) {
            ShiChaoZhiZiBodyRenderer.writeRenderData(state, player, partialTick);
            return;
        }

        // 左护法变异体形态：整身换成 zuo_guardian，断肢那套不参与（身体都不是同一具了）
        if (MutantBodyRenderData.isMutantBody(player)) {
            ZuoGuardianBodyRenderer.writeBodyRenderData(state, player, partialTick);
            return;
        }
        // 不在变异体形态：清掉日志标记，下次再变进来会重新打一条动画状态
        ZuoGuardianBodyRenderer.forget(player.getUUID());

        // ==================== 开胃奶（背挂 / 拦腰斩断） ====================
        // ★ 两套模型共用玩家这一份动画控制器，而 GeckoLib 一帧内只有**先求值**的那个模型能决定
        //   动画名怎么解析（动画没变时它直接推进旧时间轴，不会为新模型重新解析）——
        //   所以这里固定按「① 背挂（niunaix）→ ② 腰斩身体（niunai_link_player）」两趟来，
        //   并用 NiunaiLinkRenderData.BODY_PASS 标出每趟是谁，两条控制器各认各的动画名。
        boolean severed = NiunaiLinkRenderData.isNiunaiLink(player);
        if (severed) {
            // 上面统一写的 false 在这里翻成 true：既决定这一帧要不要整身替换，
            // 也是"腰斩中"给两条控制器看的门控
            state.addGeckolibData(NiunaiLinkRenderData.ACTIVE, true);
        }

        // ① 背后的菊花盾：idle / attack / parry / link 只有它自己认得。
        //    腰斩期间它照常出画，还要播自己那条 link（见 ClientPlayerGeoAnimatableMixin）。
        if (NiunaiXRenderData.isNiunaiX(player)) {
            state.addGeckolibData(NiunaiLinkRenderData.BODY_PASS, false);
            NiunaiXRenderer.writeRenderData(state, player, partialTick);
        }
        // ② 腰斩的身体：broken_off / link 在这一趟解析
        if (severed) {
            NiunaiLinkRenderer.writeRenderData(state, player, partialTick);
        }

        CorpsePlayerGeoRenderer.writeLimbRenderData(state, player, partialTick);
        // Evaluate the halo last: all attached layers share the player's GeckoLib controller cache.
        if (player.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.longyou.TianGangCombat.SHEN_ACTIVE))
            TianGangHaloRenderer.writeRenderData(state,player,partialTick);
    }
}
