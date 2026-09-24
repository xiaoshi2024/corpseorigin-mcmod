package xiaoshi2022.corpseorigin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.network.QiAuraPayload;
import xiaoshi2022.corpseorigin.skill.chapter.QiDensity;
import java.util.*;

/** Layered continuous mist surfaces outside, distance fog inside; no particle emitters. */
public final class QiAuraRenderer {
    private static final List<Aura> AURAS=new ArrayList<>();
    private static ClientLevel world;
    private static final class Aura {
        QiAuraPayload data; long until; final long born;
        Aura(QiAuraPayload data,long now){this.data=data;born=now;until=now+data.ticks();}
    }
    private QiAuraRenderer() {}
    private static boolean outward(Aura aura){
        return switch(aura.data.channel()){
            case "incarnation","blood_cloud","round_dance","sword_flower",
                    "tiangang","tiangang_charge","slaughter","aps_release" -> true;
            default -> aura.data.anchor()<0;
        };
    }
    public static int coatingColor(int entityId){
        world();if(world==null)return 0;
        for(var aura:AURAS)if(aura.data.anchor()==entityId && !outward(aura) && fade(aura)>0)
            return ((int)(50*fade(aura))<<24)|(aura.data.color()&0xffffff);
        return 0;
    }
    private static void world(){
        var current=Minecraft.getInstance().level;
        if(world!=current){AURAS.clear();world=current;}
    }
    public static void accept(QiAuraPayload data){
        world();if(world==null || !Float.isFinite(data.radius()) || data.radius()<=0 || data.radius()>16 || data.ticks()<=0)return;
        long now=world.getGameTime();
        if(data.anchor()>=0)for(var aura:AURAS)if(aura.data.anchor()==data.anchor() && aura.data.channel().equals(data.channel())){
            aura.data=data;aura.until=now+Math.min(data.ticks(),1200);return;
        }
        if(AURAS.size()>=128)AURAS.removeFirst();
        AURAS.add(new Aura(new QiAuraPayload(data.anchor(),data.channel(),data.center(),data.color(),data.radius(),Math.min(data.ticks(),1200)),now));
    }
    public static void tick(){
        world();if(world==null)return;
        AURAS.removeIf(a->a.until<=world.getGameTime() || a.data.anchor()>=0 &&
                (world.getEntity(a.data.anchor())==null || !world.getEntity(a.data.anchor()).isAlive()));
    }
    private static Vec3 center(Aura a){
        var entity=world.getEntity(a.data.anchor());
        return entity==null?a.data.center():entity.getBoundingBox().getCenter();
    }
    private static float fade(Aura a){
        return (float)Math.min(1,Math.max(0,(a.until-world.getGameTime())/6.0));
    }
    public static void fog(Camera camera,FogData fog){
        world();if(world==null || camera.getFluidInCamera()!=FogType.NONE)return;
        if(camera.entity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)))return;
        float weight=0,r=0,g=0,b=0;
        for(var aura:AURAS){
            if(aura.data.anchor()>=0)continue;
            Vec3 origin=center(aura);
            float density=QiDensity.at(camera.position().distanceTo(origin),aura.data.radius(),fade(aura));
            if(density<=0)continue;
            if(world.clip(new net.minecraft.world.level.ClipContext(camera.position(),origin,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,camera.entity()))
                    .getType()!=net.minecraft.world.phys.HitResult.Type.MISS)continue;
            int color=aura.data.color();weight+=density;
            r+=((color>>16)&255)/255f*density;g+=((color>>8)&255)/255f*density;b+=(color&255)/255f*density;
        }
        if(weight<.001)return;
        float blend=Math.min(.8f,weight*.8f);
        fog.color.x=fog.color.x*(1-blend)+r/weight*blend;
        fog.color.y=fog.color.y*(1-blend)+g/weight*blend;
        fog.color.z=fog.color.z*(1-blend)+b/weight*blend;
        float end=6/Math.max(.05f,Math.min(1,weight));
        fog.environmentalEnd=Math.min(fog.environmentalEnd,end);
        fog.environmentalStart=Math.min(fog.environmentalStart,fog.environmentalEnd*.15f);
    }
    public static void render(PoseStack stack,SubmitNodeCollector collector){
        world();if(world==null)return;
        Vec3 camera=Minecraft.getInstance().gameRenderer.mainCamera().position();
        stack.pushPose();stack.translate(-camera.x,-camera.y,-camera.z);
        var sorted=new ArrayList<>(AURAS);
        sorted.sort(Comparator.comparingDouble((Aura a)->center(a).distanceToSqr(camera)).reversed());
        for(var aura:sorted){
            Vec3 center=center(aura);if(center.distanceToSqr(camera)>9216)continue;
            if(aura.data.anchor()>=0){
                var entity=world.getEntity(aura.data.anchor());
                if(entity!=null && !(entity==Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()))
                    flame(stack,collector,aura,entity);
                continue;
            }
            float radius=aura.data.radius(),alpha=.13f*fade(aura);int color=aura.data.color();
            double time=(world.getGameTime()+Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)-aura.born)*.035;
            // Intersecting, slowly undulating translucent shells create a coherent volume.
            collector.submitCustomGeometry(stack,RenderTypes.entityTranslucent(CorpseOrigin.id("textures/effect/qi_mist.png")),(pose,out)->{
                for(int layer=0;layer<3;layer++)for(int latitude=0;latitude<8;latitude++)for(int longitude=0;longitude<24;longitude++){
                    for(int corner=0;corner<4;corner++){
                        int ix=longitude+(corner>=2?1:0),iy=latitude+((corner==1 || corner==2)?1:0);
                        double theta=ix*Math.PI/12,phi=-Math.PI/2+iy*Math.PI/8;
                        double size=radius*(.65+layer*.16)*(1+.035*Math.sin(theta*3+phi*2+time));
                        float x=(float)(center.x+Math.cos(theta)*Math.cos(phi)*size);
                        float y=(float)(center.y+Math.sin(phi)*size);
                        float z=(float)(center.z+Math.sin(theta)*Math.cos(phi)*size);
                        out.addVertex(pose.pose(),x,y,z).setColor((color>>16)&255,(color>>8)&255,color&255,(int)(alpha*255))
                                .setUv(ix/24f,iy/8f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,1,0);
                    }
                }
            });
        }
        stack.popPose();
    }
    private static void flame(PoseStack stack,SubmitNodeCollector collector,Aura aura,net.minecraft.world.entity.Entity entity){
        float partial=Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 base=entity.getPosition(partial);
        boolean release=outward(aura);
        double width=entity.getBbWidth()*(release?1.65:.65)+.12;
        double height=entity.getBbHeight()*(release?2.5:1.13);
        if(aura.data.channel().equals("incarnation")){
            width=Math.max(width,aura.data.radius()*.45);
            height=Math.max(height,aura.data.radius()*1.15);
        }
        final double flameWidth=width,flameHeight=height;
        double time=(world.getGameTime()+partial)*(release?.24:.14);
        int color=aura.data.color();float opacity=fade(aura);
        collector.submitCustomGeometry(stack,RenderTypes.entityTranslucentEmissive(CorpseOrigin.id("textures/effect/qi_mist.png")),(pose,out)->{
            // Tapered ribbons, with different heights and phases, rise from the body like an anime power-up.
            for(int layer=0;layer<2;layer++)for(int tongue=0;tongue<18;tongue++)for(int segment=0;segment<8;segment++){
                double angle=tongue*Math.PI/9;
                double peak=flameHeight*(release ? .9+.1*Math.sin(tongue*2.4+time*.8)
                        : .72+.28*Math.sin(tongue*2.4+time*.55));
                int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
                if(layer==1){r=Math.min(255,r+65);g=Math.min(255,g+70);b=Math.min(255,b+55);}
                for(int corner=0;corner<4;corner++){
                    double t=(segment+((corner==1 || corner==2)?1:0))/8.0;
                    double side=corner>=2?1:-1;
                    double spread=(1-t)*.16;
                    double theta=angle+side*spread+.1*Math.sin(t*7-time+tongue)*t;
                    double radius=flameWidth*(layer==0?1: .9)*(1+.2*Math.sin(t*Math.PI)-.65*t*t);
                    double sway=(release?.25:.06)*Math.sin(time+tongue+t*5)*t;
                    float x=(float)(base.x+Math.cos(theta)*radius+sway);
                    float z=(float)(base.z+Math.sin(theta)*radius);
                    float y=(float)(base.y+peak*t);
                    int alpha=(int)(opacity*(release ? (layer==0?85:110) : (layer==0?65:85))
                            *Math.pow(Math.sin(Math.PI*t),release?.65:1)* (release?1:.55));
                    out.addVertex(pose.pose(),x,y,z).setColor(r,g,b,alpha).setUv((float)((side+1)/2),(float)t)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,1,0);
                }
            }
        });
    }
}
