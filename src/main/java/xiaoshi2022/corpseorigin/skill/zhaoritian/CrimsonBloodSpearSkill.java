package xiaoshi2022.corpseorigin.skill.zhaoritian;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative charge and swept spear strike, with visible converging qi. */
public final class CrimsonBloodSpearSkill extends AbstractSkill {
    public static final String PATH = "crimson_blood_spear";
    private static final int CHARGE_TICKS = 24;
    private static final double SPEED = 2.4, RANGE = 40;
    private static final float DAMAGE = 160;
    private static final Map<UUID, Cast> CASTS = new HashMap<>();
    public CrimsonBloodSpearSkill() { super(PATH, SkillType.COMBAT, 360); }
    @Override public int getInnerPowerCost() { return 30; }
    @Override public int getCost() { return 3; }
    @Override public net.minecraft.network.chat.Component checkUsable(ServerPlayer p) {
        return CASTS.containsKey(p.getUUID()) ? net.minecraft.network.chat.Component.translatable("skill.corpseorigin.crimson_blood_spear.busy") : null;
    }
    @Override public void onActivate(ServerPlayer p) { CASTS.put(p.getUUID(), new Cast(p)); }
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> CASTS.values().removeIf(Cast::tick));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CASTS.clear());
    }
    private static final class Cast {
        final ServerPlayer player;
        final ServerLevel level;
        int age;
        double distance;
        Vec3 position, direction;
        Cast(ServerPlayer player) { this.player=player; this.level=player.level(); }
        boolean tick() {
            if (player.isRemoved() || !player.isAlive() || player.isSpectator() || player.level()!=level
                    || !"zhaoritian".equals(CharacterManager.getInstance().getPlayerCharacterId(player))) return true;
            if (age++ < CHARGE_TICKS) {
                direction=player.getLookAngle().normalize();
                Vec3 center=player.getEyePosition().add(direction.scale(0.9));
                Vec3 right=right(direction), up=direction.cross(right).normalize();
                double radius=1.1*(1-age/(double)CHARGE_TICKS);
                for(int i=0;i<4;i++) {
                    double angle=i*Math.PI/2+age*.22;
                    Vec3 orb=center.add(right.scale(Math.cos(angle)*radius)).add(up.scale(Math.sin(angle)*radius));
                    ChapterCombat.dust(level,orb,0xD30B30,1.5f);
                    if(age%4==0)QiEffects.cloud(level,orb,0xB70824,.18f,5);
                }
                if(age>=16) drawSpear(level,center,direction, (age-15)/9.0);
                return false;
            }
            if(position==null) {
                // Launch from the eyes so even a wall immediately in front blocks the strike.
                position=player.getEyePosition();
                direction=player.getLookAngle().normalize();
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
            }
            Vec3 next=position.add(direction.scale(Math.min(SPEED,RANGE-distance)));
            if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(next)))return true;
            var wall=level.clip(new ClipContext(position,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            Vec3 end=wall.getLocation();
            var hit=ProjectileUtil.getEntityHitResult(player,position,end,new AABB(position,end).inflate(.5),
                    e -> e instanceof LivingEntity living && ChapterCombat.canHit(player,living),position.distanceToSqr(end));
            Vec3 tip=hit==null?end:hit.getLocation();
            drawSpear(level,tip,direction,1);
            if(hit!=null) {
                LivingEntity target=(LivingEntity)hit.getEntity();
                if(target.hurtServer(level,player.damageSources().playerAttack(player),DAMAGE))
                    target.knockback(1.4,-direction.x,-direction.z,player.damageSources().playerAttack(player),DAMAGE);
                impact(tip);
                return true;
            }
            if(wall.getType()!=HitResult.Type.MISS) { impact(tip); return true; }
            distance+=position.distanceTo(next);position=next;
            return distance>=RANGE;
        }
        void impact(Vec3 point) {
            QiEffects.cloud(level,point,0xD30B30,.9f,12);
            QiEffects.burst(level,point.x,point.y,point.z,0xD30B30,1,0);
        }
    }
    private static Vec3 right(Vec3 forward) {
        Vec3 axis=Math.abs(forward.y)>.95?new Vec3(1,0,0):new Vec3(0,1,0);
        return forward.cross(axis).normalize();
    }
    private static void drawSpear(ServerLevel level,Vec3 tip,Vec3 forward,double size) {
        Vec3 right=right(forward), up=forward.cross(right).normalize();
        // Bright narrow shaft and four tapered head edges form a long spear, not a round projectile.
        for(int i=0;i<=16;i++)ChapterCombat.dust(level,tip.subtract(forward.scale(i*.18*size)),0xFF2446,.8f);
        for(int i=1;i<=5;i++) {
            double t=i/5.0;
            Vec3 center=tip.subtract(forward.scale(t*.85*size));
            for(Vec3 side:new Vec3[]{right,right.scale(-1),up,up.scale(-1)})
                ChapterCombat.dust(level,center.add(side.scale(t*.28*size)),0xB50020,1.1f);
        }
    }
}
