package xiaoshi2022.corpseorigin.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.render.QiAuraRenderer;

/** Copy the posed player's actual body surfaces, rather than wrapping a sphere around them. */
public class QiCoatingLayer extends RenderLayer<AvatarRenderState,PlayerModel> {
    @SuppressWarnings({"rawtypes","unchecked"})
    public QiCoatingLayer(RenderLayerParent parent){super(parent);}
    @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch){
        int color=QiAuraRenderer.coatingColor(state.id);
        if(color==0 || state.isInvisible)return;
        var model=getParentModel();
        for(var part:java.util.List.of(model.head,model.body,model.leftArm,model.rightArm,model.leftLeg,model.rightLeg)){
            if(!part.visible)continue;
            poses.pushPose();
            // Expand about each joint so arms and legs retain their individual animated poses.
            poses.translate(part.x/16.0,part.y/16.0,part.z/16.0);
            poses.scale(1.055f,1.055f,1.055f);
            poses.translate(-part.x/16.0,-part.y/16.0,-part.z/16.0);
            // The coating must not write depth ahead of replacement player bodies and armor.
            collector.submitModelPart(part,poses,RenderTypes.entityTranslucentEmissive(CorpseOrigin.id("textures/effect/qi_mist.png")),
                    0xF000F0,OverlayTexture.NO_OVERLAY,null,color,null,0);
            poses.popPose();
        }
    }
}
