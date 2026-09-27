package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.entity.JuQueBeamEntity;

/** A tapered, slightly thick crescent: the mesh defines the silhouette on both sides. */
public class CrescentBeamRenderer<T extends JuQueBeamEntity> extends EntityRenderer<T, CrescentBeamRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("corpseorigin", "textures/entity/qi_white.png");
    private static final int SEGMENTS = 32;
    private static final float HEIGHT = 1.25F, WIDTH = 1.2F;
    private static final float[] RINGS = {0, .3F, .8F, 1};
    private final int red, green, blue;

    public static final class State extends EntityRenderState {
        float yaw, pitch;
    }

    protected CrescentBeamRenderer(EntityRendererProvider.Context context, int red, int green, int blue) {
        super(context);
        this.red=red; this.green=green; this.blue=blue;
        this.shadowRadius=0;
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(T entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        var direction=entity.getDeltaMovement();
        if(direction.lengthSqr()<1.0E-8) direction=entity.getLookAngle();
        direction=direction.normalize();
        state.yaw=(float)Math.toDegrees(Math.atan2(direction.x,direction.z));
        state.pitch=(float)-Math.toDegrees(Math.asin(Math.clamp(direction.y,-1,1)));
    }

    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state,poses,collector,camera);
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poses.mulPose(Axis.XP.rotationDegrees(state.pitch));
        poses.mulPose(Axis.ZP.rotationDegrees(-25));
        collector.submitCustomGeometry(poses,RenderTypes.entityTranslucentEmissive(TEXTURE),(pose,out)-> {
            for(int i=0;i<SEGMENTS;i++) {
                float a=-1+2F*i/SEGMENTS, b=-1+2F*(i+1)/SEGMENTS;
                for(int band=0;band<RINGS.length-1;band++) {
                    float inner=RINGS[band], outer=RINGS[band+1];
                    // Front and back wind in opposite directions; both viewing sides remain visible.
                    vertex(out,pose,a,inner,1); vertex(out,pose,b,inner,1);
                    vertex(out,pose,b,outer,1); vertex(out,pose,a,outer,1);
                    vertex(out,pose,a,outer,-1); vertex(out,pose,b,outer,-1);
                    vertex(out,pose,b,inner,-1); vertex(out,pose,a,inner,-1);
                }
                // The joined outer edge also shows the blade in grazing views.
                vertex(out,pose,a,1,1); vertex(out,pose,b,1,1);
                vertex(out,pose,b,1,-1); vertex(out,pose,a,1,-1);
            }
        });
        poses.popPose();
    }

    /** Outer and recessed inner arcs meet at both tips, unlike a uniform-width horseshoe. */
    public static float crescentX(float height, float across) {
        float round=(float)Math.sqrt(Math.max(0,1-height*height));
        float outer=WIDTH*(round-.55F);
        float thickness=WIDTH*.52F*(1-height*height);
        return outer-thickness*(1-across);
    }

    private void vertex(VertexConsumer out, PoseStack.Pose pose, float height, float across, int side) {
        float core=Math.max(0,1-Math.abs(across-.8F)/.5F);
        int r=(int)(red+(255-red)*core*.85F);
        int g=(int)(green+(255-green)*core*.85F);
        int b=(int)(blue+(255-blue)*core*.85F);
        int alpha=across==0?35:across==1?210:245;
        float depth=side*.055F*(1-height*height);
        out.addVertex(pose.pose(),crescentX(height,across),height*HEIGHT,depth)
                .setColor(r,g,b,alpha).setUv(.5F,.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(0,0,side);
    }
}
