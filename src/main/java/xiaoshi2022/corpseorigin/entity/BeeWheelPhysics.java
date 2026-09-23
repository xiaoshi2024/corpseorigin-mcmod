package xiaoshi2022.corpseorigin.entity;

import net.minecraft.world.phys.Vec3;

/** Rope tension preserves tangential momentum and never teleports through terrain. */
public final class BeeWheelPhysics {
    private BeeWheelPhysics() {}
    public static Vec3 climbVelocity(Vec3 velocity,Vec3 delta,boolean belowLip,boolean blocked){
        Vec3 pull=pullVelocity(velocity,delta,true,1.15);
        if(!belowLip)return pull;
        // An upward launch beats gravity; reduce horizontal pressure when touching the trunk.
        double horizontal=blocked?.25:1;
        Vec3 lifted=new Vec3(pull.x*horizontal,Math.max(.55,pull.y),pull.z*horizontal);
        return lifted.lengthSqr()>1.15*1.15?lifted.normalize().scale(1.15):lifted;
    }
    public static Vec3 pullVelocity(Vec3 velocity,Vec3 delta,boolean reel,double cap){
        if(delta.lengthSqr()<1.0e-8)return velocity;
        Vec3 direction=delta.normalize();
        double radial=velocity.dot(direction);
        if(radial<0)velocity=velocity.subtract(direction.scale(radial));
        velocity=velocity.add(direction.scale(reel?.16:.08));
        if(velocity.lengthSqr()>cap*cap)velocity=velocity.normalize().scale(cap);
        return velocity;
    }
}
