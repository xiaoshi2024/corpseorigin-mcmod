package xiaoshi2022.corpseorigin.skill.chapter;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.effect.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.entity.CorpseAntEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import java.util.*;

public final class CreatureAbilities {
    public static final AttachmentType<Boolean> INFANT=AttachmentRegistry.create(CorpseOrigin.id("jingang_infant"),b->b.initializer(()->false).persistent(Codec.BOOL).copyOnDeath().syncWith(ByteBufCodecs.BOOL,AttachmentSyncPredicate.all()));
    public static final AttachmentType<Integer> BEAR_ARMS=AttachmentRegistry.create(CorpseOrigin.id("bear_arms"),b->b.initializer(()->0).persistent(Codec.INT).copyOnDeath().syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.all()));
    public static final AttachmentType<Long> RAGE_UNTIL=AttachmentRegistry.create(CorpseOrigin.id("muscle_rage_until"),b->b.initializer(()->0L).syncWith(ByteBufCodecs.VAR_LONG,AttachmentSyncPredicate.all()));
    private record Gas(ServerPlayer owner,ServerLevel level,Vec3 origin,long until){}
    private record Rush(ServerPlayer owner,ServerLevel level,String role,Vec3 direction,long start,boolean combo,Set<UUID> hit){}
    private record Capture(ServerPlayer owner,LivingEntity target,ServerLevel level,Vec3 origin,long until){}
    private record Bleed(ServerPlayer owner,LivingEntity target,ServerLevel level,long until){}
    private static final List<Gas> GASES=new ArrayList<>();
    private static final List<Rush> RUSHES=new ArrayList<>();
    private static final List<Capture> CAPTURES=new ArrayList<>();
    private static final List<Bleed> BLEEDS=new ArrayList<>();
    private CreatureAbilities(){}
    public static boolean role(ServerPlayer p,String role){return p.isAlive() && role.equals(CharacterManager.getInstance().getPlayerCharacterId(p));}
    public static void gas(ServerPlayer p){GASES.removeIf(g->g.owner==p);GASES.add(new Gas(p,(ServerLevel)p.level(),p.position(),p.level().getGameTime()+120));ChapterScenes.action(p,"cast",32);p.sendOverlayMessage(Component.translatable("skill.corpseorigin.killing_gas.amplified"));}
    public static void summon(ServerPlayer p){
        ServerLevel level=(ServerLevel)p.level();
        int existing=level.getEntitiesOfClass(CorpseAntEntity.class,p.getBoundingBox().inflate(40),a->a.ownedBy(p)).size();
        for(int i=0;i<Math.min(6,8-existing);i++){
            var ant=(i%3==0?ModEntities.BULLET_ANT:ModEntities.RED_FIRE_ANT).create(level,EntitySpawnReason.TRIGGERED);
            double angle=i*Math.PI/3;ant.setOwner(p);ant.setPos(p.position().add(Math.cos(angle)*2,0,Math.sin(angle)*2));
            if(!level.noCollision(ant))continue;
            level.addFreshEntity(ant);level.sendParticles(ParticleTypes.POOF,ant.getX(),ant.getY()+.4,ant.getZ(),16,.35,.3,.35,.02);
            ChapterCombat.ring(level,ant.position(),.7,0x8cbe4c,12);
        }
        ChapterScenes.action(p,"cast",32);
    }
    public static Component targetError(ServerPlayer p){return ChapterCombat.aim(p,5)==null?Component.translatable("skill.corpseorigin.chapter.need_target"):null;}
    public static void capture(ServerPlayer p){LivingEntity target=ChapterCombat.aim(p,5);if(target==null)return;CAPTURES.removeIf(c->c.target==target || c.owner==p);CAPTURES.add(new Capture(p,target,(ServerLevel)p.level(),target.position(),p.level().getGameTime()+60));ChapterScenes.action(p,"cast",32);}
    public static void rush(ServerPlayer p,boolean combo){RUSHES.removeIf(r->r.owner==p);RUSHES.add(new Rush(p,(ServerLevel)p.level(),CharacterManager.getInstance().getPlayerCharacterId(p),p.getLookAngle().multiply(1,0,1).normalize(),p.level().getGameTime(),combo,new HashSet<>()));ChapterScenes.action(p,combo?"pounce":"charge",combo?32:20);}
    public static void rage(ServerPlayer p){if(xiaoshi2022.corpseorigin.skill.longyou.UndeadBodyState.sealed(p))return;p.setAttached(xiaoshi2022.corpseorigin.skill.longyou.BodyPossession.HUMAN,false);p.setAttached(INFANT,false);p.setAttached(RAGE_UNTIL,p.level().getGameTime()+160);ChapterScenes.action(p,"transform",32);p.sendOverlayMessage(Component.translatable("skill.corpseorigin.muscle_rage.weakpoint"));}
    public static void swarmBite(ServerPlayer p){
        var level=(ServerLevel)p.level();ChapterScenes.action(p,"attack",18);
        for(var target:level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(3),e->ChapterCombat.canHit(p,e) && p.hasLineOfSight(e)))
            if(target.hurtServer(level,p.damageSources().playerAttack(p),4))target.addEffect(new MobEffectInstance(MobEffects.POISON,60,0));
        level.sendParticles(ParticleTypes.EXPLOSION,p.getX(),p.getY()+.5,p.getZ(),1,0,0,0,0);
    }
    public static void register(){
        ServerLivingEntityEvents.AFTER_DEATH.register((victim,source)->{
            if(victim instanceof Villager && source.getEntity() instanceof ServerPlayer p && role(p,"xiongxing_zb"))
                p.setAttached(BEAR_ARMS,Math.min(6,p.getAttachedOrCreate(BEAR_ARMS)+1));
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->{
            if(!(entity instanceof ServerPlayer p) || !role(p,"jingang_zb") || !PlayerCharacterData.get(p).hasLearned(p.getUUID(),"iron_body"))return true;
            if(p.getAttachedOrCreate(RAGE_UNTIL)>p.level().getGameTime())return true;
            return !(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) && !(source.getEntity() instanceof LivingEntity && amount<=4);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,taken,blocked)->{
            if(taken>0 && entity instanceof ServerPlayer p && role(p,"jingang_zb")){
                ((ServerLevel)p.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR,p.getX(),p.getY()+1,p.getZ(),8,.25,.6,.25,.02);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(s->{
            for(var p:s.getPlayerList().getPlayers()){
                if(!role(p,"jingang_zb")){if(p.getAttachedOrCreate(RAGE_UNTIL)!=0)p.setAttached(RAGE_UNTIL,0L);continue;}
                if(p.getAttachedOrCreate(RAGE_UNTIL)>p.level().getGameTime()){
                    p.addEffect(new MobEffectInstance(MobEffects.STRENGTH,2,1));p.addEffect(new MobEffectInstance(MobEffects.SPEED,2,0));
                    if(p.tickCount%5==0)ChapterCombat.dust((ServerLevel)p.level(),p.position().add(0,1.2,-.3),0xffd25e,1.5f);
                }
            }
            GASES.removeIf(g->{
                if(!role(g.owner,"qingwa_zb") || g.owner.level()!=g.level || g.level.getGameTime()>=g.until)return true;
                if(g.owner.tickCount%5!=0)return false;
                ChapterCombat.ring(g.level,g.origin,4,0x88c75a,30);
                g.level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,g.origin.x,g.origin.y+1,g.origin.z,25,3,.8,3,.01);
                for(var e:g.level.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(g.origin,g.origin).inflate(4))) {
                    if(e.distanceToSqr(g.origin)>16 || !g.owner.hasLineOfSight(e))continue;
                    if(e==g.owner || e.isAlliedTo(g.owner)){e.addEffect(new MobEffectInstance(MobEffects.STRENGTH,30,0));continue;}
                    if(!ChapterCombat.canHit(g.owner,e))continue;
                    e.addEffect(new MobEffectInstance(MobEffects.NAUSEA,100,0));e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,40,0));
                }return false;
            });
            RUSHES.removeIf(r->{
                if(!role(r.owner,r.role) || r.owner.level()!=r.level)return true;
                long age=r.level.getGameTime()-r.start;if(age>=(r.combo?30:18))return true;
                if(r.combo && age%10==0)r.hit.clear();
                r.owner.setDeltaMovement(r.direction.scale(r.combo?.5:.75).add(0,r.owner.getDeltaMovement().y,0));r.owner.hurtMarked=true;
                for(var target:r.level.getEntitiesOfClass(LivingEntity.class,r.owner.getBoundingBox().inflate(1.1),e->ChapterCombat.canHit(r.owner,e) && r.owner.hasLineOfSight(e))){
                    if(!r.hit.add(target.getUUID()))continue;
                    if(target.hurtServer(r.level,r.owner.damageSources().playerAttack(r.owner),r.combo?4:8)){
                        target.push(r.direction.x*.6,.15,r.direction.z*.6);
                        if(r.combo){BLEEDS.removeIf(b->b.target==target);BLEEDS.add(new Bleed(r.owner,target,r.level,r.level.getGameTime()+60));}
                    }
                }return false;
            });
            CAPTURES.removeIf(c->{
                if(!role(c.owner,"chongmu") || !c.target.isAlive() || c.target.level()!=c.level || c.owner.level()!=c.level || c.level.getGameTime()>=c.until || !ChapterCombat.canHit(c.owner,c.target))return true;
                c.target.setDeltaMovement(Vec3.ZERO);c.target.teleportTo(c.origin.x,c.origin.y,c.origin.z);
                if(c.target instanceof Mob mob)mob.getNavigation().stop();
                if(c.level.getGameTime()%10==0){c.target.addEffect(new MobEffectInstance(MobEffects.POISON,25,0));for(int i=0;i<14;i++)ChapterCombat.ring(c.level,c.target.position().add(0,i*.12,0),.6,0xb8ac88,12);}
                return false;
            });
            BLEEDS.removeIf(b->{
                if(!role(b.owner,"jingang_zb") || b.owner.level()!=b.level || b.target.level()!=b.level || !b.target.isAlive() || b.level.getGameTime()>=b.until || !ChapterCombat.canHit(b.owner,b.target))return true;
                if(b.level.getGameTime()%20==0){b.target.hurtServer(b.level,b.owner.damageSources().playerAttack(b.owner),1);b.level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,b.target.getX(),b.target.getY()+.7,b.target.getZ(),5,.2,.3,.2,.01);}return false;
            });
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{GASES.clear();RUSHES.clear();CAPTURES.clear();BLEEDS.clear();});
    }
}
