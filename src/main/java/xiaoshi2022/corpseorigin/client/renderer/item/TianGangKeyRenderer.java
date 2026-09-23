package xiaoshi2022.corpseorigin.client.renderer.item;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.render.laser.BloodLotusLaserRenderer;
import xiaoshi2022.corpseorigin.client.render.laser.TianGangBeamState;
import xiaoshi2022.corpseorigin.item.weapon.TianGangKeyItem;
import xiaoshi2022.corpseorigin.network.TianGangBeamPayload;
import java.util.function.BiConsumer;

public final class TianGangKeyRenderer extends GeoItemRenderer<TianGangKeyItem> {
    public TianGangKeyRenderer() {
        super(new DefaultedItemGeoModel<>(CorpseOrigin.id("tian_gang_key")));
        withRenderLayer(new LaserLayer(this));
    }

    private static final class LaserLayer extends GeoRenderLayer<TianGangKeyItem, RenderData, GeoRenderState> {
        private static final DataTicket<TianGangBeamPayload> BEAM = DataTickets.create("corpseorigin_tiangang_beam", TianGangBeamPayload.class);
        LaserLayer(TianGangKeyRenderer renderer) { super(renderer); }
        @Override public void addRenderData(TianGangKeyItem item, RenderData data, GeoRenderState state, float partialTick) {
            if (!(data.itemOwner() instanceof LivingEntity owner)) return;
            var beam = TianGangBeamState.get(owner.getUUID());
            if (beam == null || owner.getItemInHand(beam.hand()) != data.itemStack()) return;
            ItemDisplayContext context = data.renderPerspective();
            var arm = beam.hand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? owner.getMainArm() : owner.getMainArm().getOpposite();
            boolean left = arm == net.minecraft.world.entity.HumanoidArm.LEFT;
            if (context != (left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                    && context != (left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) return;
            if (beam != null) {
                state.addGeckolibData(BEAM, beam);
            }
        }
        @Override public void addPerBoneRender(RenderPassInfo<GeoRenderState> pass,
                BiConsumer<GeoBone, PerBoneRender<GeoRenderState>> registrar) {
            if (!pass.willRender() || !pass.renderState().hasGeckolibData(BEAM)) return;
            pass.model().getBone("laser_muzzle").ifPresent(bone -> registrar.accept(bone, (posed, muzzle, collector) -> {
                var beam = posed.getGeckolibData(BEAM);
                // Per-bone pose includes the animated muzzle pivot, parents and item display transform.
                var point = posed.poseStack().last().pose().transformPosition(new org.joml.Vector3f());
                Vec3 origin = new Vec3(point.x, point.y, point.z);
                // The key's blade runs along model +Y. Transform that axis with the SAME
                // animated bone matrix as the muzzle, including either hand's display pose.
                // Never substitute the camera/look direction: that makes a bent gun barrel.
                var axis = posed.poseStack().last().pose().transformDirection(new org.joml.Vector3f(0, 1, 0));
                Vec3 direction = new Vec3(axis.x, axis.y, axis.z);
                if (direction.lengthSqr() < 1.0E-8) return;
                var perspective = posed.getGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE);
                TianGangBeamState.submitPose(beam, origin, direction,
                        perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                                || perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
                // Normalize after the item transform so first-person model scaling cannot
                // turn a 50-block blade into a 15-block blade. Sightline hits do not resize it.
                double length = beam.firing() ? 50 : .2;
                Vec3 end = origin.add(direction.normalize().scale(length));
                var bladeAxis = posed.poseStack().last().pose().transformDirection(new org.joml.Vector3f(1, 0, 0));
                BloodLotusLaserRenderer.submitCuttingBlade(new PoseStack(), collector, origin, end,
                        new Vec3(bladeAxis.x, bladeAxis.y, bladeAxis.z), beam.firing() ? .55f : .12f);
            }));
        }
    }
}
