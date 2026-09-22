package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.model.ExoskeletonModel;
import xiaoshi2022.corpseorigin.client.render.CorpsePlayerRenderHandler;
import xiaoshi2022.corpseorigin.client.render.layer.ExoskeletonRenderLayer;
import xiaoshi2022.corpseorigin.client.renderer.CloneArmorSupport;
import xiaoshi2022.corpseorigin.client.skin.clone.ClientSkinCache;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.mixin.LivingEntityRendererLayersAccessor;
import xiaoshi2022.corpseorigin.registry.ModModelLayers;

import java.util.List;

/**
 * 克隆分身的渲染器。
 * <p>
 * 泛型（{@link AvatarRenderState} + {@link PlayerModel}）与真玩家的 AvatarRenderer 完全一致，
 * 所以能直接挂同一套层：盔甲层 + 模组的尸兄外骨骼层，
 * 以及从玩家渲染器复制过来的层（其他模组挂上去的面部/装饰渲染也能跟着生效）。
 */
public class CloneAvatarRenderer
        extends LivingEntityRenderer<CloneAvatarEntity, AvatarRenderState, PlayerModel> {

    /** 是否已经从玩家渲染器复制过层 */
    private boolean copiedPlayerLayers;

    public CloneAvatarRenderer(EntityRendererProvider.Context context, boolean slim) {
        super(context,
                new PlayerModel(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim),
                0.5F);

        // ★ 顺序与真玩家一致：先盔甲，再外骨骼（外骨骼画在上层）
        //   盔甲的模型集/装备渲染器同时存一份给方块实体渲染器用（仓内克隆人也要穿）
        CloneArmorSupport.init(context);
        this.addLayer(new HumanoidArmorLayer<>(this, CloneArmorSupport.armorSet(),
                context.getEquipmentRenderer()));

        this.addLayer(new ExoskeletonRenderLayer(this,
                new ExoskeletonModel(context.getModelSet().bakeLayer(ModModelLayers.EXOSKELETON))));
    }

    @Override
    public AvatarRenderState createRenderState() { return new AvatarRenderState(); }

    @Override
    public void extractRenderState(CloneAvatarEntity entity, AvatarRenderState state, float partialTick) {
        this.copyPlayerLayers();

        super.extractRenderState(entity, state, partialTick);
        state.skin = ClientSkinCache.resolve(entity.getSkinUuid());
        // ★ AvatarRenderState.id 是原版 AvatarRenderer 自己填的，我们是 LivingEntityRenderer 子类，
        //   不填的话尸兄外骨骼层拿不到实体（getEntityUuid(state.id) 会查不到），外骨骼就不渲染
        state.id = entity.getId();
        state.isSpectator = false;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showCape = false;

        // ★ 盔甲：取这具身体自己穿的那套（顺序：头/胸/腿/脚）
        List<ItemStack> equipment = entity.clientEquipment();
        state.headEquipment = equipmentAt(equipment, 0);
        state.chestEquipment = equipmentAt(equipment, 1);
        state.legsEquipment = equipmentAt(equipment, 2);
        state.feetEquipment = equipmentAt(equipment, 3);
    }

    private static ItemStack equipmentAt(List<ItemStack> equipment, int index) {
        return index < equipment.size() ? equipment.get(index) : ItemStack.EMPTY;
    }

    // ==================== 复制玩家渲染器上的层 ====================

    /**
     * 把玩家渲染器上已注册的层复制一份过来（包含其他模组挂上去的，例如吸血鬼面部）。
     * <p>
     * 惰性执行：玩家渲染器的构造顺序不早于本渲染器，拿不到就下次再试。
     * 盔甲层与本模组的外骨骼层跳过——这两个本渲染器自带，复制会重复渲染。
     */
    @SuppressWarnings("unchecked")
    private void copyPlayerLayers() {
        if (this.copiedPlayerLayers) {
            return;
        }
        AvatarRenderer<?> vanilla = CorpsePlayerRenderHandler.LAYER_MAP.keySet().stream()
                .findFirst().orElse(null);
        if (vanilla == null) {
            return;   // 玩家渲染器还没建好，下次再试
        }
        this.copiedPlayerLayers = true;

        int added = 0;
        List<RenderLayer<?, ?>> layers =
                ((LivingEntityRendererLayersAccessor) vanilla).getLayers();
        for (RenderLayer<?, ?> layer : List.copyOf(layers)) {
            if (layer instanceof HumanoidArmorLayer || layer instanceof ExoskeletonRenderLayer) {
                continue;
            }
            this.addLayer(new SafeRenderLayer<>(this,
                    (RenderLayer<AvatarRenderState, PlayerModel>) layer));
            added++;
        }
        CorpseOrigin.LOGGER.info("[CorpseOrigin] 克隆分身已复制 {} 个玩家渲染层", added);
    }

    /**
     * 复制过来的第三方层的保险壳：某一层抛异常时只停用它自己，不让整帧渲染崩掉。
     */
    private static final class SafeRenderLayer<S extends LivingEntityRenderState, M extends EntityModel<S>>
            extends RenderLayer<S, M> {

        private final RenderLayer<S, M> delegate;
        private boolean broken;

        SafeRenderLayer(RenderLayerParent<S, M> parent, RenderLayer<S, M> delegate) {
            super(parent);
            this.delegate = delegate;
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light,
                           S state, float yRot, float xRot) {
            if (this.broken) {
                return;
            }
            try {
                this.delegate.submit(pose, collector, light, state, yRot, xRot);
            } catch (Throwable t) {
                this.broken = true;
                CorpseOrigin.LOGGER.warn("[CorpseOrigin] 克隆分身跳过异常的渲染层 {}: {}",
                        this.delegate.getClass().getSimpleName(), t.toString());
            }
        }
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }

}
