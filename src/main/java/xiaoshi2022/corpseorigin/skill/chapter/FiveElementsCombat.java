package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.InnerPowerManager;
import java.util.*;

/** Short-lived, server-authoritative formations, bindings, marks and charged fireballs. */
public final class FiveElementsCombat {
    private record Formation(ServerPlayer owner, ServerLevel level, Vec3 center, long until) {}
    private record Bound(ServerPlayer owner, LivingEntity target, Vec3 anchor, long until) {}
    private record Mark(ServerPlayer owner, LivingEntity target, long until) {}
    private record Charge(ServerPlayer owner, ServerLevel level, List<UUID> donors, int started) {}
    private static final Map<UUID, Formation> FORMATIONS = new HashMap<>();
    private static final Map<UUID, Bound> BINDINGS = new HashMap<>();
    private static final Map<UUID, Mark> MARKS = new HashMap<>();
    private static final Map<UUID, Charge> CHARGES = new HashMap<>();
    private static final List<Fireball> FIREBALLS = new ArrayList<>();
    private static final Map<UUID, Sustain> SUSTAINS = new HashMap<>();
    private static boolean member(ServerPlayer player) {
        return role(player,"muxi") || role(player,"formation_metal") || role(player,"formation_water") || role(player,"formation_earth");
    }
    private static final int[] COLORS = {0xe9db9b, 0x57b84f, 0x4399e8, 0xfa5b35, 0xbc946a};
    private FiveElementsCombat() {}
    private static boolean role(ServerPlayer player, String id) {
        return player.isAlive() && !player.isRemoved()
                && (id.equals(CharacterManager.getInstance().getPlayerCharacterId(player))
                || xiaoshi2022.corpseorigin.growth.FreeGrowth.learnedFrom(player,id));
    }
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            FORMATIONS.clear(); BINDINGS.clear(); MARKS.clear(); CHARGES.clear(); FIREBALLS.clear(); SUSTAINS.clear();
        });
    }
    public static void formation(ServerPlayer player) {
        FORMATIONS.put(player.getUUID(), new Formation(player, (ServerLevel)player.level(), player.position(), player.level().getGameTime() + 200));
    }
    public static void bind(ServerPlayer player, LivingEntity target) {
        xiaoshi2022.corpseorigin.entity.SkillConstructEntity.spawn(player,
                xiaoshi2022.corpseorigin.registry.ModEntities.VINE_BIND,target,160);
        BINDINGS.put(target.getUUID(), new Bound(player, target, target.position(), player.level().getGameTime() + 160));
    }
    public static void mark(ServerPlayer player, LivingEntity target) {
        MARKS.put(target.getUUID(), new Mark(player, target, player.level().getGameTime() + 200));
    }
    public static void strike(ServerPlayer player, LivingEntity target) {
        ServerLevel level = (ServerLevel) player.level();
        Mark mark = MARKS.get(target.getUUID());
        boolean primed = mark != null && mark.owner == player && mark.until > level.getGameTime();
        if (target.hurtServer(level, player.damageSources().playerAttack(player), primed ? 12 : 6)) {
            if (primed) { MARKS.remove(target.getUUID()); target.igniteForSeconds(6); }
            QiEffects.burst(level, target.getX(), target.getY() + .7, target.getZ(), 0xff7a1a, primed ? 35 : 12, .5);
        }
    }
    /** Casting five-elements formation is explicit consent to contribute; ungrouped strangers are excluded. */
    private static List<ServerPlayer> donors(ServerPlayer yan) {
        return FORMATIONS.values().stream().filter(f -> f.owner.level() == f.level).map(Formation::owner)
                .filter(p -> p != yan && member(p) && p.level() == yan.level()
                        && yan.getTeam() != null && yan.isAlliedTo(p) && p.distanceToSqr(yan) <= 100
                        && p.hasLineOfSight(yan) && InnerPowerManager.getInnerPower(p) > 0
                        && FORMATIONS.get(p.getUUID()).until > p.level().getGameTime()).toList();
    }
    public static Component chargeError(ServerPlayer player) {
        if (player.isInWater()) return Component.translatable("skill.corpseorigin.chapter.need_dry");
        if (SUSTAINS.containsKey(player.getUUID())) return Component.translatable("skill.corpseorigin.reverse_formation_fireball.sustaining");
        if (CHARGES.containsKey(player.getUUID())) return Component.translatable("skill.corpseorigin.chapter.charging");
        return donors(player).isEmpty() ? Component.translatable("skill.corpseorigin.reverse_formation_fireball.need_donor") : null;
    }
    public static void charge(ServerPlayer player) {
        ChapterScenes.action(player,"charge",40);
        CHARGES.put(player.getUUID(), new Charge(player, (ServerLevel)player.level(), donors(player).stream().map(ServerPlayer::getUUID).toList(), player.tickCount));
    }
    private static void tick() {
        FORMATIONS.values().removeIf(f -> !member(f.owner) || f.owner.level() != f.level || f.owner.level().getGameTime() >= f.until
                || f.owner.distanceToSqr(f.center) > 144);
        for (Formation f : FORMATIONS.values()) {
            if (f.owner.tickCount % 10 != 0) continue;
            ServerLevel level = (ServerLevel) f.owner.level();
            for (int i = 0; i < 5; i++) {
                double angle = i * Math.PI * 2 / 5;
                Vec3 node = f.center.add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4);
                ChapterCombat.ring(level, node, .7, COLORS[i], 8);
                Vec3 next = f.center.add(Math.cos(angle + Math.PI * 4 / 5) * 4, 0, Math.sin(angle + Math.PI * 4 / 5) * 4);
                for (int j = 0; j < 8; j++) ChapterCombat.dust(level, node.lerp(next, j / 8.0).add(0,.1,0), COLORS[i], 1);
            }
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(f.center, f.center).inflate(5))) {
                if (!target.isAlive() || target.distanceToSqr(f.center) > 25 || !f.owner.hasLineOfSight(target)) continue;
                if (target == f.owner || f.owner.isAlliedTo(target)) {
                    target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 25, 0));
                } else if (target instanceof Monster) target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 1));
            }
        }
        BINDINGS.values().removeIf(b -> !role(b.owner,"muxi") || !b.target.isAlive()
                || b.target.level() != b.owner.level() || b.owner.level().getGameTime() >= b.until
                || !ChapterCombat.canHit(b.owner, b.target));
        for (Bound b : BINDINGS.values()) {
            // Keep vertical physics, but stop walking, sprinting and horizontal knockback.
            Vec3 movement=b.target.getDeltaMovement();
            b.target.setDeltaMovement(0,Math.min(0,movement.y),0);
            if (b.target.position().subtract(b.anchor).horizontalDistanceSqr() > .01) {
                var box=b.target.getBoundingBox().move(b.anchor.x-b.target.getX(),0,b.anchor.z-b.target.getZ());
                if (b.target.level().noCollision(b.target,box))
                    b.target.teleportTo(b.anchor.x,b.target.getY(),b.anchor.z);
            }
            b.target.hurtMarked=true;
            if (b.owner.tickCount % 5 != 0) continue;
            b.target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 7, 6));
            ServerLevel level = (ServerLevel) b.target.level();
            if (b.owner.tickCount % 20 == 0) b.target.hurtServer(level,b.owner.damageSources().playerAttack(b.owner),10);
            for (int i = 0; i < 12; i++) {
                double angle = i * .95 + b.owner.tickCount * .08;
                ChapterCombat.dust(level, b.target.position().add(Math.cos(angle)*.5, i*.13, Math.sin(angle)*.5), 0x368b40, 1.3f);
            }
        }
        MARKS.values().removeIf(m -> !role(m.owner,"yanyan") || !m.target.isAlive()
                || m.target.level() != m.owner.level() || m.owner.level().getGameTime() >= m.until);
        for (Mark m : MARKS.values()) if (m.owner.tickCount % 5 == 0)
            QiEffects.burst((ServerLevel)m.target.level(), m.target.getX(), m.target.getY()+.7, m.target.getZ(), 0xb9162c, 8, .5);
        var iterator = CHARGES.values().iterator();
        while (iterator.hasNext()) {
            Charge c = iterator.next();
            ServerPlayer p = c.owner;
            if (!role(p,"yanyan") || p.level() != c.level || p.isInWater()) { iterator.remove(); p.setAttached(ChapterScenes.ACTION,""); continue; }
            int elapsed = p.tickCount - c.started;
            List<ServerPlayer> contributors = donors(p).stream().filter(d -> c.donors.contains(d.getUUID())).toList();
            if (contributors.isEmpty()) { iterator.remove(); p.setAttached(ChapterScenes.ACTION,""); p.sendOverlayMessage(Component.translatable("skill.corpseorigin.reverse_formation_fireball.interrupted")); continue; }
            ServerLevel level = (ServerLevel)p.level();
            if (elapsed % 4 == 0) {
                ChapterCombat.ring(level, p.position(), 1 + elapsed / 40.0, 0xff592a, 20);
                for (ServerPlayer donor : contributors)
                    for (int i = 0; i < 8; i++) ChapterCombat.dust(level, donor.getEyePosition().lerp(p.getEyePosition(), i/8.0), 0xffbc49, 1.3f);
            }
            if (elapsed < 40) continue;
            int power = 0;
            // Spend only at release. There is no fabricated energy for the three unimplemented members.
            for (ServerPlayer donor : contributors) {
                int available = InnerPowerManager.getInnerPower(donor);
                power += available;
                InnerPowerManager.set(donor, 0);
                FORMATIONS.remove(donor.getUUID());
            }
            double multiplier = Math.min(4, 1 + power / 60.0);
            SUSTAINS.put(p.getUUID(),new Sustain(p,level,contributors,power));
            ChapterScenes.action(p,"release",15);
            FIREBALLS.add(new Fireball(p, level, p.getEyePosition(), p.getLookAngle().scale(.7), multiplier));
            iterator.remove();
        }
        FIREBALLS.removeIf(Fireball::tick);
        SUSTAINS.values().removeIf(Sustain::tick);
    }
    private static final class Sustain {
        final ServerPlayer yan;
        final ServerLevel level;
        final List<ServerPlayer> members;
        int reserve,age;
        Sustain(ServerPlayer yan,ServerLevel level,List<ServerPlayer> members,int reserve){
            this.yan=yan;this.level=level;this.members=List.copyOf(members);this.reserve=reserve;
        }
        boolean tick(){
            if(!role(yan,"yanyan") || yan.level()!=level || ++age>400 || yan.isInWater())return true;
            var present=members.stream().filter(p->member(p) && p.level()==level && p.distanceToSqr(yan)<=144
                    && yan.getTeam()!=null && yan.isAlliedTo(p) && p.hasLineOfSight(yan)).toList();
            if(present.isEmpty())return true;
            if(age%20!=0)return false;
            // The initial transfer is stored, then paid out as upkeep; no extra energy is created.
            int needed=Math.max(0,4-reserve);
            for(ServerPlayer member:present) {
                int amount=Math.min(needed,InnerPowerManager.getInnerPower(member));
                if(amount>0 && InnerPowerManager.consume(member,amount)){reserve+=amount;needed-=amount;}
            }
            if(reserve<4){yan.sendOverlayMessage(Component.translatable("skill.corpseorigin.reverse_formation_fireball.exhausted"));return true;}
            reserve-=4;
            yan.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,25,0));
            yan.addEffect(new MobEffectInstance(MobEffects.STRENGTH,25,1));
            ChapterCombat.ring(level,yan.position(),1.2,0xff642c,16);
            return false;
        }
    }
    private static final class Fireball {
        final ServerPlayer owner;
        final ServerLevel level;
        final Vec3 velocity;
        final double multiplier;
        Vec3 position;
        int age;
        Fireball(ServerPlayer owner, ServerLevel level, Vec3 position, Vec3 velocity, double multiplier) {
            this.owner=owner; this.level=level; this.position=position; this.velocity=velocity; this.multiplier=multiplier;
        }
        boolean tick() {
            if (++age > 80 || !role(owner,"yanyan") || owner.level()!=level) return true;
            Vec3 end=position.add(velocity);
            if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(end))) return true;
            var block=level.clip(new ClipContext(position,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
            var hit=ProjectileUtil.getEntityHitResult(owner,position,block.getLocation(),
                    new AABB(position,end).inflate(.7), e -> e instanceof LivingEntity l && ChapterCombat.canHit(owner,l),
                    position.distanceToSqr(block.getLocation()));
            if (hit!=null || block.getType()!=HitResult.Type.MISS) {
                position=hit==null ? block.getLocation() : hit.getLocation();
                double radius=3+multiplier;
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,new AABB(position,position).inflate(radius))) {
                    if (!ChapterCombat.canHit(owner,target) || target.distanceToSqr(position)>radius*radius) continue;
                    Vec3 blastOrigin=position.subtract(velocity.normalize().scale(.15));
                    if (level.clip(new ClipContext(blastOrigin,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner)).getType()!=HitResult.Type.MISS) continue;
                    if (target.hurtServer(level,owner.damageSources().playerAttack(owner),(float)(8*multiplier))) target.igniteForSeconds(5);
                }
                QiEffects.burst(level,position.x,position.y,position.z,0xd8552c,1,0);
                return true;
            }
            position=end;
            double radius=.65+multiplier*.15;
            QiEffects.cloud(level,position,0xff7a1a,(float)radius,6);
            return false;
        }
    }
}
