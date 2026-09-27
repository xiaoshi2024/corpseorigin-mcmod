package xiaoshi2022.corpseorigin.mixin;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.client.compat.FirstPersonArmorCompat;
import xiaoshi2022.corpseorigin.item.armor.AntennaZBRitem;
import xiaoshi2022.corpseorigin.item.armor.LongYouClothItem;
import xiaoshi2022.corpseorigin.item.armor.XiaoluArmorItem;

import java.util.function.BiFunction;

@Mixin(GeoArmorRenderer.class)
public abstract class GeoArmorFirstPersonMixin {
    @Inject(method = "tryRenderGeoArmorPiece", at = @At("HEAD"), cancellable = true)
    private static <R extends HumanoidRenderState, A extends HumanoidModel<R>> void corpseorigin$firstPersonState(
            BiFunction<R, EquipmentSlot, A> model, PoseStack poses, SubmitNodeCollector collector,
            ItemStack stack, EquipmentSlot slot, int light, R root, CallbackInfoReturnable<Boolean> cir) {
        if (!(stack.getItem() instanceof AntennaZBRitem || stack.getItem() instanceof LongYouClothItem
                || stack.getItem() instanceof XiaoluArmorItem)) return;
        var states = root.getGeckolibData(DataTickets.PER_SLOT_RENDER_DATA);
        if (states == null || states.get(slot) == null) return;
        int mask = FirstPersonArmorCompat.prepare(root, states.get(slot));
        // Returning true consumes the geo piece without falling back to vanilla armor.
        if (mask >= 0 && (slot != EquipmentSlot.CHEST || mask == 0)) cir.setReturnValue(true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "adjustModelBonesForRender", at = @At("TAIL"))
    private void corpseorigin$firstPersonBones(RenderPassInfo<?> pass, BoneSnapshots bones, CallbackInfo ci) {
        var state = (HumanoidRenderState) pass.renderState();
        Integer mask = state.getGeckolibData(FirstPersonArmorCompat.ARM_MASK);
        if (mask == null || mask < 0) return;
        GeoArmorRenderer renderer = (GeoArmorRenderer) (Object) this;
        for (GeoArmorRenderer.ArmorSegment segment : GeoArmorRenderer.ArmorSegment.values()) {
            boolean show = switch (segment) {
                case RIGHT_ARM -> (mask & FirstPersonArmorCompat.RIGHT_ARM) != 0;
                case LEFT_ARM -> (mask & FirstPersonArmorCompat.LEFT_ARM) != 0;
                default -> false;
            };
            if (!show) {
                String name = renderer.getBoneNameForSegment(state, segment);
                if (name != null && !name.isEmpty())
                    bones.ifPresent(name, bone -> bone.skipRender(true).skipChildrenRender(true));
            }
        }
    }
}
