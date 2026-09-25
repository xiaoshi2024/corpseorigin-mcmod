package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared targeting for the corpse-nest chapter: walls, pets and teams are respected. */
public final class ChapterCombat {
    private ChapterCombat() {}
    /** Visible release even when the directional attack misses; never invent a target. */
    public static void emptyCast(ServerPlayer player) {
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        ChapterScenes.action(player,"cast",12);
        Vec3 start=player.getEyePosition();
        Vec3 end=player.level().clip(new ClipContext(start,start.add(player.getLookAngle().scale(2)),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getLocation();
        for(int i=1;i<=8;i++)dust((ServerLevel)player.level(),start.lerp(end,i/8.0),0xaaccee,1);
    }
    public static boolean canHit(ServerPlayer owner, LivingEntity target) {
        if (target instanceof xiaoshi2022.corpseorigin.entity.OwnerBound body && body.isOwnedBy(owner)) return false;
        if (target == owner || !target.isAlive() || target.isSpectator() || owner.isAlliedTo(target)) return false;
        if (target instanceof Player player && (player.isCreative() || !owner.canHarmPlayer(player))) return false;
        return !(target instanceof TamableAnimal pet && pet.isOwnedBy(owner));
    }
    public static LivingEntity aim(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        var wall = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        end = wall.getLocation();
        var hit = ProjectileUtil.getEntityHitResult(player, start, end,
                new AABB(start, end).inflate(.4), e -> e instanceof LivingEntity living && canHit(player, living),
                start.distanceToSqr(end));
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }
    /** 水平绕 Y 轴旋转一个方向向量。 */
    public static Vec3 rotateY(Vec3 dir, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(dir.x * c + dir.z * s, dir.y, -dir.x * s + dir.z * c).normalize();
    }
    /**
     * 面朝方向的扇形近战：命中 {@code halfAngleDeg} 半角内、{@code range} 格内且可见的敌人，
     * 命中后沿受击方向击退。粒子由各招式自行绘制。返回命中数。
     */
    public static int arc(ServerPlayer player, Vec3 dir, double range, double halfAngleDeg, float damage, double push) {
        Vec3 face = new Vec3(dir.x, 0, dir.z);
        if (face.lengthSqr() < 1.0E-6) face = Vec3.directionFromRotation(0, player.getYRot());
        face = face.normalize();
        double minDot = Math.cos(Math.toRadians(halfAngleDeg));
        var level = (ServerLevel) player.level();
        int hit = 0;
        for (var t : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                target -> canHit(player, target) && player.hasLineOfSight(target))) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter());
            double dist = Math.sqrt(to.x * to.x + to.z * to.z);
            if (dist > range || dist < .05) continue;
            if (to.multiply(1 / dist, 0, 1 / dist).dot(face) < minDot) continue;
            if (t.hurtServer(level, player.damageSources().playerAttack(player), damage)) {
                t.push(to.x / dist * push, .32, to.z / dist * push);
                t.hurtMarked = true;
                hit++;
            }
        }
        return hit;
    }
    /** 在玩家身前画一道扇形棍光/刀光粒子。 */
    public static void arcDust(ServerLevel level, Vec3 origin, Vec3 dir, double radius, double halfAngleDeg, int color) {
        Vec3 face = new Vec3(dir.x, 0, dir.z);
        if (face.lengthSqr() < 1.0E-6) face = Vec3.directionFromRotation(0, 0);
        face = face.normalize();
        Vec3 right = new Vec3(-face.z, 0, face.x);
        for (int i = 0; i <= 24; i++) {
            double a = Math.toRadians(-halfAngleDeg + 2 * halfAngleDeg * i / 24.0);
            Vec3 at = origin.add(face.scale(Math.cos(a) * radius)).add(right.scale(Math.sin(a) * radius));
            dust(level, at, color, 1.1f);
        }
    }
    public static void dust(ServerLevel level, Vec3 point, int color, float size) {
        level.sendParticles(new DustParticleOptions(color, size), point.x, point.y, point.z, 1, 0, 0, 0, 0);
    }
    public static void ring(ServerLevel level, Vec3 center, double radius, int color, int points) {
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            dust(level, center.add(Math.cos(angle) * radius, .12, Math.sin(angle) * radius), color, 1.1f);
        }
    }
}
