package xiaoshi2022.corpseorigin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.network.SwordImpactPayload;
import xiaoshi2022.corpseorigin.skill.chapter.SwordQiRules;

import java.util.*;

/** Short authored mesh strokes, real-time hit feedback, bounded independently of server work. */
public final class SwordImpactRenderer {
    private SwordImpactRenderer() {}
    private record Effect(SwordImpactPayload data,long born) {}
    private record Pose(double x,double y,double z,float age) {}
    private record Motion(float body,float yaw,float pitch,float walk,float speed) {}
    private static final List<Effect> EFFECTS=new ArrayList<>();
    private static final Map<Integer,Pose> POSES=new HashMap<>();
    private static final Map<Integer,Motion> MOTIONS=new HashMap<>();
    private static ClientLevel world;
    private static long impulse,freezeUntil,lastImpulse;
    private static float strength;
    private static int target=-1,caster=-1;
    private static long now(){return System.nanoTime()/1000000L;}
    private static void world(){
        var current=Minecraft.getInstance().level;
        if(world!=current){world=current;EFFECTS.clear();POSES.clear();MOTIONS.clear();impulse=freezeUntil=lastImpulse=0;strength=0;}
    }
    public static int effectCount(){world();return EFFECTS.size();}
    public static int postStage(){
        world();if(world==null || !CorpseConfig.get().swordVisuals.screenEffects || Minecraft.getInstance().gui.screen()!=null)return 0;
        long age=now()-impulse;if(age<0 || age>360)return 0;
        return Math.clamp((int)Math.ceil((1-age/360.0)*strength*3),0,3);
    }
    public static void accept(SwordImpactPayload p){
        world();if(world==null || p.kind()<0 || p.kind()>2 || p.tier()<1 || p.tier()>20
                || !Double.isFinite(p.center().lengthSqr()) || !Double.isFinite(p.direction().lengthSqr()))return;
        long t=now();var cfg=CorpseConfig.get().swordVisuals;
        while(EFFECTS.size()>=cfg.maxImpactEffects)EFFECTS.removeFirst();
        EFFECTS.add(new Effect(p,t));
        var mc=Minecraft.getInstance();
        double distance=mc.gameRenderer.mainCamera().position().distanceTo(p.center());
        float attenuation=(float)Math.max(0,1-distance/(p.kind()==1?120:48));
        if(mc.player!=null && (mc.player.getId()==p.caster() || mc.player.getId()==p.target()))attenuation=Math.max(.55f,attenuation);
        if(p.kind()==2 || attenuation<=0 || t-lastImpulse<250)return;
        lastImpulse=impulse=t;strength=attenuation*(.35f+p.tier()*.045f);
        target=p.target();caster=p.caster();POSES.clear();MOTIONS.clear();
        freezeUntil=cfg.hitStop?t+Math.min(95,35+p.tier()*3):t;
    }
    public static float shake(int axis){
        world();if(world==null || Minecraft.getInstance().gui.screen()!=null)return 0;
        long age=now()-impulse;if(age<0 || age>420)return 0;
        double fade=1-age/420.0;
        return (float)(Math.sin(age*(axis==0?.16:.21)+axis*1.7)*fade*fade*strength*1.4*CorpseConfig.get().swordVisuals.cameraShake);
    }
    private static boolean frozen(int id){world();return CorpseConfig.get().swordVisuals.hitStop && now()<freezeUntil && (id==target || id==caster);}
    public static void freeze(int id,EntityRenderState state){
        if(!frozen(id))return;
        Pose pose=POSES.computeIfAbsent(id,k->new Pose(state.x,state.y,state.z,state.ageInTicks));
        state.x=pose.x;state.y=pose.y;state.z=pose.z;state.ageInTicks=pose.age;
    }
    public static void freezeMotion(int id,LivingEntityRenderState state){
        if(!frozen(id))return;
        Motion m=MOTIONS.computeIfAbsent(id,k->new Motion(state.bodyRot,state.yRot,state.xRot,state.walkAnimationPos,state.walkAnimationSpeed));
        state.bodyRot=m.body;state.yRot=m.yaw;state.xRot=m.pitch;state.walkAnimationPos=m.walk;state.walkAnimationSpeed=m.speed;
    }
    public static void register(){
        HudElementRegistry.addLast(CorpseOrigin.id("sword_screen_impact"),(g,delta)->{
            world();var mc=Minecraft.getInstance();var cfg=CorpseConfig.get().swordVisuals;
            if(world==null || !cfg.screenEffects || mc.gui.screen()!=null || mc.player==null)return;
            long age=now()-impulse;if(age<0 || age>420)return;
            int w=g.guiWidth(),h=g.guiHeight();
            float fade=(float)Math.pow(1-age/420.0,2)*strength;
            if(age<90){int a=(int)(70*(1-age/90f)*strength*cfg.flashIntensity);g.fill(0,0,w,h,(Math.min(100,a)<<24)|0xfff4d5);}
            // A warm compression vignette and split-color radial speed marks; never solid blackout.
            for(int i=0;i<7;i++){
                int a=(int)(fade*(7-i)*3),edge=i*3;
                g.fill(edge,edge,w-edge,edge+3,(a<<24)|0x24112f);g.fill(edge,h-edge-3,w-edge,h-edge,(a<<24)|0x24112f);
                g.fill(edge,edge,edge+3,h-edge,(a<<24)|0x24112f);g.fill(w-edge-3,edge,w-edge,h-edge,(a<<24)|0x24112f);
            }
            for(int i=0;i<16;i++){
                double a=i*Math.PI/8;int x=(int)(w*.5+Math.cos(a)*w*.49),y=(int)(h*.5+Math.sin(a)*h*.48);
                for(int j=0;j<9;j++){
                    int xx=(int)(x-Math.cos(a)*j*3),yy=(int)(y-Math.sin(a)*j*3),alpha=(int)(fade*(9-j)*10);
                    g.fill(xx,yy,xx+2,yy+2,(alpha<<24)|0xffdb89);g.fill(xx+2,yy,xx+3,yy+2,((alpha/2)<<24)|0x80dfff);
                }
            }
        });
    }
    public static void render(PoseStack stack,SubmitNodeCollector collector){
        world();if(world==null)return;
        long time=now();EFFECTS.removeIf(e->time-e.born>1400);
        Vec3 camera=Minecraft.getInstance().gameRenderer.mainCamera().position();
        stack.pushPose();stack.translate(-camera.x,-camera.y,-camera.z);
        for(Effect e:List.copyOf(EFFECTS)){
            var p=e.data;double age=(time-e.born)/50.0;
            if(p.center().distanceToSqr(camera)>512*512)continue;
            Vec3 origin=p.center();
            var entity=world.getEntity(p.target());
            if(entity!=null && entity.isAlive() && age<5)origin=entity.getBoundingBox().getCenter();
            final Vec3 center=origin;
            final Vec3 direction=p.direction().lengthSqr()<.001?new Vec3(0,0,1):p.direction().normalize();
            collector.submitCustomGeometry(stack,RenderTypes.entityTranslucentEmissive(CorpseOrigin.id("textures/effect/sword_stroke.png")),(pose,out)->{
                var random=new Random(p.seed());
                int strokes=p.kind()==0?SwordQiRules.slashes(p.tier()):p.kind()==1?12:6;
                double scale=p.kind()==0?2+p.tier()*.34:p.kind()==1?SwordQiRules.height(p.tier())*.32:4+p.tier()*.5;
                for(int i=0;i<strokes;i++){
                    double delay=i%5*.75,progress=(age-delay)/12;
                    double theta=random.nextDouble()*Math.PI*2,tilt=random.nextDouble()*2-1;
                    if(progress<0 || progress>=1)continue;
                    Vec3 u=new Vec3(Math.cos(theta),Math.sin(theta),tilt*.5).normalize();
                    Vec3 v=u.cross(direction);if(v.lengthSqr()<.01)v=u.cross(new Vec3(0,1,0));v=v.normalize();
                    Vec3 offset=center.add(direction.scale((i%3-1)*.4));
                    double length=scale*(.7+random.nextDouble()*.6),alpha=Math.sin(Math.PI*progress)*.85;
                    for(int s=0;s<20;s++){
                        double t0=-1+s*.1,t1=t0+.1;
                        Vec3 a=offset.add(u.scale(t0*length)).add(v.scale((1-t0*t0)*length*.18));
                        Vec3 b=offset.add(u.scale(t1*length)).add(v.scale((1-t1*t1)*length*.18));
                        ribbon(out,pose,a,b,v,length*.045*Math.sin((s+.5)/20*Math.PI),alpha,255,177,55);
                        ribbon(out,pose,a,b,v,length*.009*Math.sin((s+.5)/20*Math.PI),alpha,255,255,230);
                    }
                }
                // Shock rings expand after the slash, on two different planes.
                double progress=age/24,alpha=Math.max(0,1-progress);
                if(progress>=0 && progress<1)for(int ring=0;ring<2;ring++){
                    double radius=scale*(.25+progress*1.7)*(ring==0?1:.65);
                    for(int i=0;i<64;i++){
                        double a=i*Math.PI/32,b=(i+1)*Math.PI/32;
                        Vec3 one=ring==0?new Vec3(Math.cos(a)*radius,0,Math.sin(a)*radius):new Vec3(Math.cos(a)*radius,Math.sin(a)*radius,0);
                        Vec3 two=ring==0?new Vec3(Math.cos(b)*radius,0,Math.sin(b)*radius):new Vec3(Math.cos(b)*radius,Math.sin(b)*radius,0);
                        ribbon(out,pose,center.add(one),center.add(two),one.normalize(),Math.max(.025,radius*.012),alpha*.6,140,218,255);
                    }
                }
                if(p.kind()==1 && age<24){
                    Vec3 along=new Vec3(direction.x,0,direction.z).normalize();
                    Vec3 end=center.add(along.scale(SwordQiRules.riftLength(p.tier())));
                    ribbon(out,pose,center,end,new Vec3(0,1,0),SwordQiRules.height(p.tier())*.5,Math.max(0,1-age/24)*.32,255,185,75);
                    ribbon(out,pose,center,end,new Vec3(0,1,0),1.5,Math.max(0,1-age/24)*.8,255,255,238);
                }
            });
        }
        stack.popPose();
    }
    private static void ribbon(VertexConsumer out,PoseStack.Pose pose,Vec3 a,Vec3 b,Vec3 normal,double width,double alpha,int r,int g,int blue){
        Vec3 n=normal.scale(width);
        vertex(out,pose,a.subtract(n),0,0,alpha,r,g,blue);vertex(out,pose,b.subtract(n),1,0,alpha,r,g,blue);
        vertex(out,pose,b.add(n),1,1,alpha,r,g,blue);vertex(out,pose,a.add(n),0,1,alpha,r,g,blue);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,Vec3 p,float u,float v,double a,int r,int g,int b){
        out.addVertex(pose.pose(),(float)p.x,(float)p.y,(float)p.z).setColor(r,g,b,(int)Math.clamp(a*255,0,255))
                .setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0,1,0);
    }
}
