package xiaoshi2022.corpseorigin.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.shell.ShellState;

import java.util.List;

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

    /** 仓内克隆人用的离屏假身（见 {@link #dummyWearer}） */
    @Nullable
    private static CloneAvatarEntity dummy;

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

    /**
     * 给"没有实体"的渲染路径（仓内克隆人）用的离屏假身，顺便把这具身体穿的盔甲塞进它的真实槽位。
     * <p>
     * GeckoLib 的盔甲渲染有两个前提：装备要从 {@code LivingEntity.getItemBySlot} 读、
     * 每槽位的盔甲渲染数据要在<b>实体渲染状态</b>创建时由 GeckoLib 的 {@code EntityRendererMixin} 填。
     * 仓内克隆人是方块实体渲染器画的，两条都不占 —— 于是盔甲会掉回原版盔甲层，按
     * {@code ArmorMaterial} 画成钻石甲。这里补一具<b>不加入世界</b>的假身补上前提，
     * 再由调用方手动跑一次 {@code GeoArmorRenderer.captureRenderStates} 补上后者。
     */
    @Nullable
    public static LivingEntity dummyWearer(List<ItemStack> equipment) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }
        if (dummy == null || dummy.level() != client.level) {
            dummy = new CloneAvatarEntity(ModEntities.CLONE_AVATAR, client.level);
        }
        for (int i = 0; i < ShellState.EQUIPMENT_SLOTS.length; i++) {
            dummy.setItemSlot(ShellState.EQUIPMENT_SLOTS[i],
                    i < equipment.size() ? equipment.get(i) : ItemStack.EMPTY);
        }
        return dummy;
    }
}
