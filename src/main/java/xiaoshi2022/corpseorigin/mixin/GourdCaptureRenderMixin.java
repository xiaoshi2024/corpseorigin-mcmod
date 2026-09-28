package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Visual shrink only: health, physical size and other scale modifiers remain unchanged. */
@Mixin(LivingEntityRenderer.class)
public abstract class GourdCaptureRenderMixin {
    private static final com.geckolib.constant.dataticket.DataTicket<net.minecraft.world.phys.Vec3> OFFSET=com.geckolib.constant.dataticket.DataTicket.create("gourd_capture_offset",net.minecraft.world.phys.Vec3.class);
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("RETURN"))
    private void corpseorigin$captureScale(LivingEntity entity,LivingEntityRenderState state,float partial,CallbackInfo ci){
        float shrink=Math.clamp(entity.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.GourdCapture.SCALE),.06f,1f);
        state.scale*=shrink;
        var at=xiaoshi2022.corpseorigin.client.render.GourdMouthAnchors.get(entity.getAttachedOrCreate(xiaoshi2022.corpseorigin.skill.chapter.GourdCapture.ANCHOR));
        var offset=net.minecraft.world.phys.Vec3.ZERO;
        if(at!=null&&shrink<1){double progress=(1-shrink)/.94,blend=progress*progress*(3-2*progress);offset=at.add(0,-entity.getBbHeight()*shrink*.5,0).subtract(entity.getPosition(partial)).scale(blend);}
        ((com.geckolib.renderer.base.GeoRenderState)state).addGeckolibData(OFFSET,offset);
    }
    @Inject(method="submit",at=@At("HEAD"))
    private void corpseorigin$mouthStart(LivingEntityRenderState state,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera,CallbackInfo ci){
        var offset=((com.geckolib.renderer.base.GeoRenderState)state).getGeckolibData(OFFSET);if(offset!=null&&offset.lengthSqr()>0){poses.pushPose();poses.translate(offset.x,offset.y,offset.z);}
    }
    @Inject(method="submit",at=@At("RETURN"))
    private void corpseorigin$mouthEnd(LivingEntityRenderState state,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera,CallbackInfo ci){var offset=((com.geckolib.renderer.base.GeoRenderState)state).getGeckolibData(OFFSET);if(offset!=null&&offset.lengthSqr()>0)poses.popPose();}
}
