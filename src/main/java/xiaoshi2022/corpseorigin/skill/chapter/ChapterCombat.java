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
