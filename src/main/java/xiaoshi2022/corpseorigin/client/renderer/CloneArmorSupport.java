package xiaoshi2022.corpseorigin.client.renderer;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jetbrains.annotations.Nullable;

/**
 * 盔甲渲染需要的东西（模型集 + 装备渲染器）只能从**实体**渲染器的 context 里拿，
 * 但仓内克隆人是方块实体渲染器画的，拿不到 context。
 * <p>
 * 所以由实体侧的 {@code CloneAvatarRenderer} 在构造时存一份，这里供方块实体渲染器取用。
 */
public final class CloneArmorSupport {

    @Nullable
    private static EquipmentLayerRenderer equipmentRenderer;
    @Nullable
    private static ArmorModelSet<PlayerModel> armorSet;

    private CloneArmorSupport() {
    }

    public static void init(EntityRendererProvider.Context context) {
        equipmentRenderer = context.getEquipmentRenderer();
        armorSet = ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(),
                part -> new PlayerModel(part, false));
    }

    /** 取共享的盔甲模型集（可能是 null：实体渲染器还没构造） */
    @Nullable
    public static ArmorModelSet<PlayerModel> armorSet() {
        return armorSet;
    }

    /**
     * 构造一个挂在任意父模型上的盔甲层。
     * 还没准备好时返回 null，那一帧就不画盔甲，之后会自动补上。
     */
    @Nullable
    public static HumanoidArmorLayer<AvatarRenderState, PlayerModel, PlayerModel> armorLayer(
            RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        if (equipmentRenderer == null || armorSet == null) {
            return null;
        }
        return new HumanoidArmorLayer<>(parent, armorSet, equipmentRenderer);
    }
}
