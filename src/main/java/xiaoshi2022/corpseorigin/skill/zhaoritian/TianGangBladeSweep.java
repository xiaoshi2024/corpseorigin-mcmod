package xiaoshi2022.corpseorigin.skill.zhaoritian;

import net.minecraft.world.phys.Vec3;

/** Bounded angular sampling: rotate the full blade instead of shrinking its tip across a chord. */
public final class TianGangBladeSweep {
    public static final double LENGTH = 50, MAX_ROOT_OFFSET = 6;
    public static final int MAX_STEPS = 512;
    private TianGangBladeSweep() {}

    public record Pose(Vec3 root, Vec3 direction) {
        public Vec3 tip() { return root.add(direction.scale(LENGTH)); }
    }

    public static boolean valid(Vec3 rootOffset, Vec3 direction) {
        return finite(rootOffset) && finite(direction) && rootOffset.lengthSqr() <= MAX_ROOT_OFFSET * MAX_ROOT_OFFSET
                && direction.lengthSqr() >= .99 && direction.lengthSqr() <= 1.01;
    }
    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
    public static int steps(Pose from, Pose to) {
        double angle = Math.acos(Math.clamp(from.direction.dot(to.direction), -1, 1));
        return Math.clamp((int) Math.ceil((from.root.distanceTo(to.root) + LENGTH * angle) / .25), 1, MAX_STEPS);
    }
    public static Pose interpolate(Pose from, Pose to, double t) {
        if (t <= 0) return from;
        if (t >= 1) return to;
        Vec3 a = from.direction, b = to.direction;
        double dot = Math.clamp(a.dot(b), -1, 1);
        Vec3 direction;
        if (dot > .9999) {
            direction = a.lerp(b, t).normalize();
        } else {
            double angle = Math.acos(dot);
            Vec3 tangent = b.subtract(a.scale(dot));
            if (tangent.lengthSqr() < 1.0E-10) {
                Vec3 axis = Math.abs(a.y) < .9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                tangent = a.cross(axis);
            }
            direction = a.scale(Math.cos(angle * t)).add(tangent.normalize().scale(Math.sin(angle * t))).normalize();
        }
        return new Pose(from.root.lerp(to.root, t), direction);
    }
}
