package xiaoshi2022.corpseorigin.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterScenes;
import java.util.Set;

/** Short local close-ups restore the exact camera and FOV that preceded the scene. */
public final class ChapterCinematics {
    private static CameraType previousCamera;
    private static int previousFov;
    private ChapterCinematics(){}
    private static void restore(Minecraft client){
        if(previousCamera==null)return;
        client.options.setCameraType(previousCamera);client.options.fov().set(previousFov);previousCamera=null;
    }
    public static void register(){
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->restore(client));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            var p=client.player;
            boolean scene=p!=null && p.isAlive() && p.level().getGameTime()<p.getAttachedOrCreate(ChapterScenes.UNTIL)
                    && Set.of("drain","knockback","entrance","ambush").contains(p.getAttachedOrCreate(ChapterScenes.ACTION));
            if(!scene){restore(client);return;}
            if(previousCamera==null){
                previousCamera=client.options.getCameraType();previousFov=client.options.fov().get();
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);client.options.fov().set(45);
            }
        });
    }
}
