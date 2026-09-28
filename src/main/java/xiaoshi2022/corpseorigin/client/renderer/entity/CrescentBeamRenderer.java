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

/** A swept crescent with a smoothly rounded leading ridge and long, tapered tips. */
public class CrescentBeamRenderer<T extends JuQueBeamEntity> extends EntityRenderer<T, CrescentBeamRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("corpseorigin", "textures/effect/qi_mist.png");
    private static final int SEGMENTS = 48;
    private static final float HEIGHT = 1.25F, WIDTH = 1.2F;
    private final int red, green, blue;

    public static final class State extends EntityRenderState {
        float yaw, pitch, power, time, roll, bladeHeight, bladeDepth;
    }

    protected CrescentBeamRenderer(EntityRendererProvider.Context context, int red, int green, int blue) {
        super(context);
        this.red=red; this.green=green; this.blue=blue;
        this.shadowRadius=0;
    }

    @Override public State createRenderState() { return new State(); }

    @Override protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(T entity) {
        // Include the entire rotated mist so large tips remain visible when the center is off-screen.
        return super.getBoundingBoxForCulling(entity).inflate(entity.getBladeHeight()+entity.getBladeDepth());
    }

    @Override public void extractRenderState(T entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.bladeHeight=entity.getBladeHeight();state.bladeDepth=entity.getBladeDepth();
        state.power=entity.getPower();
        state.roll=entity.getSlashRoll();
        state.time=entity.tickCount+partialTick;
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
        poses.mulPose(Axis.ZP.rotationDegrees(state.roll));
        poses.mulPose(Axis.YP.rotationDegrees(90));
        // Spread the two tips more than the blade depth: stronger qi opens wider rather than just inflating.
        poses.scale(state.bladeDepth,state.bladeHeight/2.5f,1+2*state.power);
        collector.submitCustomGeometry(poses,RenderTypes.entityTranslucentEmissive(TEXTURE),(pose,out)-> {
            // Shape the same translucent mist used by QiAuraRenderer into a moving volume.
            // Multiple flowing sheets fade on every boundary; there is no opaque blade or silhouette image.
            for(int layer=0;layer<7;layer++) for(int i=0;i<SEGMENTS;i++) {
                float a=-1+2F*i/SEGMENTS, b=-1+2F*(i+1)/SEGMENTS;
                for(int band=0;band<8;band++) {
                    float inner=band/8F, outer=(band+1)/8F;
                    vertex(out,pose,a,inner,layer,state.time); vertex(out,pose,b,inner,layer,state.time);
                    vertex(out,pose,b,outer,layer,state.time); vertex(out,pose,a,outer,layer,state.time);
                }
            }
        });
        // White-hot leading edge and two delayed spectral blades, with tapered ends.
        collector.submitCustomGeometry(poses,RenderTypes.entityTranslucentEmissive(Identifier.fromNamespaceAndPath("corpseorigin","textures/effect/sword_stroke.png")),(pose,out)->{
            for(int echo=0;echo<3;echo++)for(int i=0;i<SEGMENTS;i++){
                float a=-1+2f*i/SEGMENTS,b=-1+2f*(i+1)/SEGMENTS;
                for(int corner=0;corner<4;corner++){
                    float h=(corner==1 || corner==2)?b:a;
                    float side=corner>=2?1:-1;
                    float taper=1-h*h;
                    float x=crescentX(h,.91f)-echo*.12f+side*(echo==0?.015f:.035f)*taper;
                    out.addVertex(pose.pose(),x,h*HEIGHT,-echo*.1f)
                            .setColor(255,echo==0?255:210,echo==0?240:100,(int)((echo==0?225:65)*taper))
                            .setUv((h+1)*.5f,side>0?1:0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,0,1);
                }
            }
        });
        poses.popPose();
    }

    /** Round the central ridge while preserving the swept-back silhouette and sharp endpoints. */
    public static float crescentX(float height, float across) {
        return JuQueBeamEntity.crescentX(height,across);
    }

    private void vertex(VertexConsumer out, PoseStack.Pose pose, float height, float across, int layer, float time) {
        float core=Math.max(0,1-Math.abs(across-.8F)/.5F);
        int r=(int)(red+(255-red)*core*.85F);
        int g=(int)(green+(255-green)*core*.85F);
        int b=(int)(blue+(255-blue)*core*.85F);
        float taper=1-height*height;
        float flow=(float)Math.sin(height*12+across*8-time*.65F+layer*1.7F);
        float envelope=(float)Math.pow(Math.sin(Math.PI*across),.7)* (float)Math.pow(taper,.3);
        int alpha=(int)((layer==3?100:48)*envelope*(.8F+.2F*flow));
        float depth=((layer-3)*.105F+.045F*flow)*taper;
        // The inner mist streams behind the advancing front, retaining the crescent at its leading edge.
        depth-=(1-across)*(1-across)*(.3F+layer*.09F)*taper;
        float x=crescentX(height,across)+.025F*flow*envelope;
        out.addVertex(pose.pose(),x,height*HEIGHT,depth)
                .setColor(r,g,b,alpha).setUv(across,(height+1)*.5F+time*.025F+layer*.13F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(0,0,1);
    }
}
