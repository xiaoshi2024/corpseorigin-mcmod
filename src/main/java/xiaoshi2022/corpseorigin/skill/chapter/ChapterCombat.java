package xiaoshi2022.corpseorigin.skill.chapter;

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
    public static String actorRole(LivingEntity actor) {
        return actor instanceof xiaoshi2022.corpseorigin.entity.CloneAvatarEntity clone ? clone.getBodyRole()
                : actor instanceof ServerPlayer player ? xiaoshi2022.corpseorigin.character.CharacterManager.getInstance().getPlayerCharacterId(player) : "";
    }
    public static net.minecraft.world.damagesource.DamageSource attackSource(LivingEntity actor) {
        return actor instanceof Player player ? actor.damageSources().playerAttack(player) : actor.damageSources().mobAttack(actor);
    }
    /** Visible release even when the directional attack misses; never invent a target. */
    public static void emptyCast(ServerPlayer player) {
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        ChapterScenes.action(player,"cast",12);
        Vec3 start=player.getEyePosition();
        Vec3 end=player.level().clip(new ClipContext(start,start.add(player.getLookAngle().scale(2)),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getLocation();
        QiEffects.cloud((ServerLevel)player.level(),start.lerp(end,.5),0xaaccee,1.1f,10);
    }
    public static boolean canHit(ServerPlayer owner, LivingEntity target) {
        if (target instanceof Player player && !owner.canHarmPlayer(player)) return false;
        return canHit((LivingEntity) owner, target);
    }
    /**
     * 任意生物版本：克隆分身放技能时用这套 —— 创造/旁观、同盟、主人的仆从都不可命中。
     * 玩家专属的"队伍友军伤害规则"（{@link ServerPlayer#canHarmPlayer}）仍走上面的重载。
     */
    public static boolean canHit(LivingEntity owner, LivingEntity target) {
        if (owner instanceof ServerPlayer player && target instanceof Player other && !player.canHarmPlayer(other)) return false;
        if (owner instanceof xiaoshi2022.corpseorigin.entity.CloneAvatarEntity clone && !clone.canAttackWithSkills(target)) return false;
        if (target instanceof xiaoshi2022.corpseorigin.entity.OwnerBound body && body.isOwnedBy(owner)) return false;
        if (target == owner || !target.isAlive() || target.isSpectator() || owner.isAlliedTo(target)) return false;
        if (target instanceof Player player && player.isCreative()) return false;
        return !(target instanceof TamableAnimal pet && pet.isOwnedBy(owner));
    }
    public static LivingEntity aim(ServerPlayer player, double range) {
        return aim((LivingEntity) player, range);
    }
    /** 任意生物版本的视线索敌：克隆分身放指向性招式时用。 */
    public static LivingEntity aim(LivingEntity player, double range) {
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
        return arc((LivingEntity) player, dir, range, halfAngleDeg, damage, push);
    }
    /**
     * 任意生物版本的扇形近战：伤害来源在玩家时仍是 playerAttack，
     * 克隆分身这类生物走 mobAttack（药水 / 击退 / 仇恨都按生物攻击处理）。
     */
    public static int arc(LivingEntity player, Vec3 dir, double range, double halfAngleDeg, float damage, double push) {
        Vec3 face = new Vec3(dir.x, 0, dir.z);
        if (face.lengthSqr() < 1.0E-6) face = Vec3.directionFromRotation(0, player.getYRot());
        face = face.normalize();
        double minDot = Math.cos(Math.toRadians(halfAngleDeg));
        var level = (ServerLevel) player.level();
        var damageSource = player instanceof ServerPlayer serverPlayer
                ? serverPlayer.damageSources().playerAttack(serverPlayer)
                : player.damageSources().mobAttack(player);
        int hit = 0;
        for (var t : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                target -> canHit(player, target) && player.hasLineOfSight(target))) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(player.getBoundingBox().getCenter());
            double dist = Math.sqrt(to.x * to.x + to.z * to.z);
            if (dist > range || dist < .05) continue;
            if (to.multiply(1 / dist, 0, 1 / dist).dot(face) < minDot) continue;
            if (t.hurtServer(level, damageSource, damage)) {
                t.push(to.x / dist * push, .32, to.z / dist * push);
                t.hurtMarked = true;
                hit++;
            }
        }
        return hit;
    }
    /** 在玩家身前扫出一道扇形气浪（原来是一整排粒子，现在整片只发 3 朵气团）。 */
    public static void arcDust(ServerLevel level, Vec3 origin, Vec3 dir, double radius, double halfAngleDeg, int color) {
        Vec3 face = new Vec3(dir.x, 0, dir.z);
        if (face.lengthSqr() < 1.0E-6) face = Vec3.directionFromRotation(0, 0);
        face = face.normalize();
        Vec3 right = new Vec3(-face.z, 0, face.x);
        for (int i = -1; i <= 1; i++) {
            double a = Math.toRadians(halfAngleDeg * i);
            Vec3 at = origin.add(face.scale(Math.cos(a) * radius)).add(right.scale(Math.sin(a) * radius));
            QiEffects.cloud(level, at, color, (float) (radius * .8), 10);
        }
    }
    /** 单点气（顶替原来的一颗 dust 粒子）。 */
    public static void dust(ServerLevel level, Vec3 point, int color, float size) {
        QiEffects.cloud(level, point, color, Math.max(.5f, size), 8);
    }
    /** 一圈气浪：整圈只发一朵半径等于圈半径的气团。 */
    public static void ring(ServerLevel level, Vec3 center, double radius, int color, int points) {
        QiEffects.cloud(level, center.add(0, .12, 0), color, (float) radius, 12);
    }
}
