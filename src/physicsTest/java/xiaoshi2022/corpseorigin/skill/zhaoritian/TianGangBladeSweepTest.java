package xiaoshi2022.corpseorigin.skill.zhaoritian;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.skill.zhaoritian.TianGangBladeSweep.Pose;

public final class TianGangBladeSweepTest {
    private static int checks;
    public static void main(String[] args) {
        Pose a = new Pose(Vec3.ZERO, new Vec3(1, 0, 0));
        Pose b = new Pose(Vec3.ZERO, new Vec3(0, 0, 1));
        // A target near the outside of the arc is missed by either endpoint and by tip-chord interpolation.
        double diagonal = 49 / Math.sqrt(2);
        AABB target = new AABB(diagonal-.2, -.2, diagonal-.2, diagonal+.2, .2, diagonal+.2);
        check(!hits(a, target) && !hits(b, target), "endpoint rays miss the intermediate target");
        boolean swept = false;
        int steps = TianGangBladeSweep.steps(a, b);
        for (int i=0; i<=steps; i++) swept |= hits(TianGangBladeSweep.interpolate(a, b, i/(double)steps), target);
        check(swept, "angular sweep hits a target 49 blocks away between poses");
        Pose middle = TianGangBladeSweep.interpolate(a, b, .5);
        check(Math.abs(middle.tip().length()-50) < 1e-8, "sweep preserves full blade length");
        Pose reverse = TianGangBladeSweep.interpolate(a, new Pose(Vec3.ZERO, new Vec3(-1,0,0)), .5);
        check(Double.isFinite(reverse.tip().length()) && Math.abs(reverse.tip().length()-50)<1e-8, "180 degree swing remains finite");
        check(!hits(a, new AABB(51,-.1,-.1,52,.1,.1)), "cannot hit beyond the blade tip");
        check(!hits(a, new AABB(-2,-.1,-.1,-1,.1,.1)), "cannot hit behind the blade root");
        check(TianGangBladeSweep.valid(new Vec3(0,3,0),a.direction()), "normal hand pose accepted");
        check(!TianGangBladeSweep.valid(new Vec3(7,0,0),a.direction()), "remote muzzle rejected");
        check(!TianGangBladeSweep.valid(Vec3.ZERO,Vec3.ZERO), "zero axis rejected");
        check(!TianGangBladeSweep.valid(Vec3.ZERO,new Vec3(Double.NaN,0,0)), "NaN rejected");
        check(!TianGangBladeSweep.valid(new Vec3(Double.POSITIVE_INFINITY,0,0),a.direction()), "infinity rejected");
        check(!TianGangBladeSweep.valid(Vec3.ZERO,new Vec3(50,0,0)), "unbounded direction rejected");
        Pose translated = TianGangBladeSweep.interpolate(a,new Pose(new Vec3(2,4,6),a.direction()),.5);
        check(translated.root().distanceTo(new Vec3(1,2,3))<1e-8, "moving root is interpolated");
        check(TianGangBladeSweep.steps(a,new Pose(new Vec3(10,0,0),new Vec3(-1,0,0)))<=512, "work per tick is bounded");
        System.out.println("Tian Gang blade sweep: " + checks + " regression checks passed.");
    }
    private static boolean hits(Pose pose,AABB target) {
        return target.contains(pose.root()) || target.clip(pose.root(),pose.tip()).isPresent();
    }
    private static void check(boolean result,String label) {
        if(!result) throw new AssertionError(label);
        checks++;
    }
}
