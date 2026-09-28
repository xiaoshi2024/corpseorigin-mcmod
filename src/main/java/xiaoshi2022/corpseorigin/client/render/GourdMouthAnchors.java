package xiaoshi2022.corpseorigin.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/** Client visual positions only. Never sends client bone coordinates to the server. */
public final class GourdMouthAnchors {
    public static final DataTicket<Integer> ENTITY=DataTicket.create("gourd_anchor_entity",Integer.class);
    private record Sample(Vec3 position,long time) {}
    private static final Map<Integer,Sample> MOUTHS=new HashMap<>();
    private static Object world;
    private GourdMouthAnchors() {}
    private static void checkWorld(){var current=Minecraft.getInstance().level;if(world!=current){world=current;MOUTHS.clear();}}
    public static <R extends GeoRenderState> void listen(RenderPassInfo<R> pass,boolean attached){
        checkWorld();Integer id=pass.renderState().getGeckolibData(ENTITY);
        var level=Minecraft.getInstance().level;if(id==null||level==null)return;
        // The attached renderer starts inside the player's already transformed joint pose.
        // GeckoLib's ordinary world position omits that incoming pose; restore it here.
        var root=new Matrix4f(pass.poseStack().last().pose());
        var camera=Minecraft.getInstance().gameRenderer.mainCamera().position();
        long time=level.getGameTime();
        MOUTHS.entrySet().removeIf(e->time-e.getValue().time()>2);
        pass.addBonePositionListener("snake_head",(worldPosition,modelPosition,localPosition)->{
            Vec3 at=worldPosition;
            if(attached){var v=root.transformPosition(new Vector3f((float)localPosition.x,(float)localPosition.y,(float)localPosition.z));at=new Vec3(v.x,v.y,v.z).add(camera);}
            if(at!=null&&Double.isFinite(at.x)&&Double.isFinite(at.y)&&Double.isFinite(at.z))MOUTHS.put(id,new Sample(at,time));
        });
    }
    public static Vec3 get(int id){checkWorld();var level=Minecraft.getInstance().level;var sample=MOUTHS.get(id);return level!=null&&sample!=null&&level.getGameTime()-sample.time()<=2?sample.position():null;}
}
