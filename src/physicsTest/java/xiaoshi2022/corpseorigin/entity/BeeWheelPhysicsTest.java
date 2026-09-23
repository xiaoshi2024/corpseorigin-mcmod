package xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.phys.Vec3;

/** Standalone physics regression checks; no game world or test-library dependency required. */
public final class BeeWheelPhysicsTest {
    public static void main(String[] args){
        Vec3 anchor=new Vec3(0,10,0);
        Vec3 swing=BeeWheelPhysics.pullVelocity(new Vec3(.4,-.7,.3),anchor,false,1.15);
        close(swing.x,.4,"swing keeps lateral momentum");
        close(swing.z,.3,"swing keeps sideways momentum");
        close(swing.y,.08,"taut rope cancels outward fall");
        Vec3 reel=BeeWheelPhysics.pullVelocity(Vec3.ZERO,anchor,true,1.15);
        close(reel.y,.16,"reeling accelerates toward anchor");
        Vec3 movingIn=BeeWheelPhysics.pullVelocity(new Vec3(0,.3,0),anchor,true,1.15);
        close(movingIn.y,.46,"inward momentum is preserved");
        Vec3 capped=BeeWheelPhysics.pullVelocity(new Vec3(7,8,9),anchor,true,.85);
        close(capped.length(),.85,"target pull is speed-limited");
        Vec3 zero=BeeWheelPhysics.pullVelocity(new Vec3(.1,.2,.3),Vec3.ZERO,true,1.15);
        close(zero.y,.2,"coincident endpoints do not introduce motion");
        for(int i=0;i<1000;i++){
            reel=BeeWheelPhysics.pullVelocity(reel,anchor,true,1.15);
            if(!Double.isFinite(reel.length()) || reel.length()>1.150001)
                throw new AssertionError("Sustained reeling must remain finite and bounded");
        }
        Vec3 launch=BeeWheelPhysics.climbVelocity(Vec3.ZERO,new Vec3(5,8,0),true,false);
        if(launch.y<.55)throw new AssertionError("Grappling a tree must launch upward");
        Vec3 wall=BeeWheelPhysics.climbVelocity(new Vec3(.8,-.3,0),new Vec3(2,4,0),true,true);
        if(wall.y<.55 || wall.x>.3)throw new AssertionError("Climb past trunk instead of pressing into it");
        Vec3 top=BeeWheelPhysics.climbVelocity(Vec3.ZERO,new Vec3(1,-.2,0),false,false);
        if(top.x<=0 || top.y>=0)throw new AssertionError("Above the lip, steer onto the landing surface");
        Vec3 fast=BeeWheelPhysics.climbVelocity(new Vec3(7,8,9),anchor,true,false);
        if(fast.length()>1.150001)throw new AssertionError("Climb speed must remain capped");
        System.out.println("Bee wheel physics: 12 regression checks passed.");
    }
    private static void close(double actual,double expected,String label){
        if(Math.abs(actual-expected)>1e-6)throw new AssertionError(label+": "+actual+" != "+expected);
    }
}
