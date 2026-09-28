package xiaoshi2022.corpseorigin.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes;

@Mixin(LivingEntityRenderer.class)
public class SkillPoseMixin {
    @Inject(method="setupRotations",at=@At("TAIL"))
    private void corpseorigin$roundDance(LivingEntityRenderState state,PoseStack poses,float bodyYaw,float scale,CallbackInfo ci){
        if(!(state instanceof AvatarRenderState avatar))return;
        var level=Minecraft.getInstance().level;var p=level==null?null:level.getEntity(avatar.id);if(p==null)return;
        if(!"round_dance".equals(p.getAttachedOrCreate(ChapterScenes.ACTION)))return;
        long until=p.getAttachedOrCreate(ChapterScenes.UNTIL);
        if(until<=level.getGameTime())return;
        float elapsed=40-(until-level.getGameTime())+(state.ageInTicks-(float)Math.floor(state.ageInTicks));
        poses.translate(0,.9,0);
        poses.mulPose(Axis.YP.rotationDegrees(elapsed*18));
        poses.mulPose(Axis.XP.rotationDegrees(elapsed*18));
        poses.translate(0,-.9,0);
    }
}
