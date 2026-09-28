package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.BeeWheelEntity;
import xiaoshi2022.corpseorigin.entity.SkillConstructEntity;

public class SkillConstructRenderer extends GeoEntityRenderer<SkillConstructEntity,LivingEntityRenderState> {
    public SkillConstructRenderer(EntityRendererProvider.Context context,String name){
        super(context,new DefaultedEntityGeoModel<>(CorpseOrigin.id(name)));shadowRadius=0;
    }
    @Override public void extractRenderState(SkillConstructEntity entity,LivingEntityRenderState state,float partialTick){
        if (entity instanceof xiaoshi2022.corpseorigin.client.render.HeartPreviewEntity preview) {
            // InventoryScreen passes 1, which GeckoLib interprets as zero animation delta.
            // Set the clock before super extracts ageInTicks and controller snapshots.
            partialTick = preview.advancePreviewClock();
        }
        super.extractRenderState(entity,state,partialTick);
        if(!(entity instanceof BeeWheelEntity wheel))return;
        state.leashStates=null;
        var owner=wheel.ropeOwner();
        if(owner==null)return;
        var rope=new EntityRenderState.LeashState();
        rope.offset=Vec3.ZERO;
        rope.start=wheel.getPosition(partialTick);
        rope.end=owner.getRopeHoldPosition(partialTick);
        rope.slack=wheel.phase()==BeeWheelEntity.FLYING || wheel.phase()==BeeWheelEntity.RETURNING;
        var start=BlockPos.containing(rope.start);var end=BlockPos.containing(rope.end);
        rope.startBlockLight=entity.level().getBrightness(LightLayer.BLOCK,start);
        rope.endBlockLight=entity.level().getBrightness(LightLayer.BLOCK,end);
        rope.startSkyLight=entity.level().getBrightness(LightLayer.SKY,start);
        rope.endSkyLight=entity.level().getBrightness(LightLayer.SKY,end);
        state.leashStates=java.util.List.of(rope);
    }
    @Override public boolean shouldRender(SkillConstructEntity entity,net.minecraft.client.renderer.culling.Frustum frustum,
                                           double x,double y,double z){
        // Keep the connecting rope visible while its hook is just outside the camera frustum.
        return entity instanceof BeeWheelEntity && entity.shouldRender(x,y,z) || super.shouldRender(entity,frustum,x,y,z);
    }
}
