package com.xiaoshi2022.corpseorigin.growth;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Server-authoritative abilities are derived from equipped, approved definitions only. */
@Mod.EventBusSubscriber(modid = CorpseOrigin.MODID)
public final class OrganTraits {
    private static final UUID SWIM = UUID.fromString("543acdca-730b-49fe-8f10-ad9acec72fa0");
    private static final Map<ServerPlayer, Boolean> FLIGHT = new WeakHashMap<>();
    private static final Map<LivingEntity, Long> DRAIN = new WeakHashMap<>();
    private OrganTraits() {}
    public static boolean has(LivingEntity entity, String trait) {
        return NativeCorpseState.organSlots(entity).stream().map(s -> OrganLibrary.find(s.organ()))
                .anyMatch(d -> d != null && trait.equals(d.trait()));
    }
    public static void update(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        boolean active=entity.isAlive();
        var swim=entity.getAttribute(ForgeMod.SWIM_SPEED.get());
        if(swim!=null) {
            boolean enabled=active&&has(entity,"gills");
            if(enabled&&swim.getModifier(SWIM)==null)swim.addTransientModifier(new AttributeModifier(SWIM,"corpseorigin.organ_swim",1.0,AttributeModifier.Operation.MULTIPLY_TOTAL));
            else if(!enabled&&swim.getModifier(SWIM)!=null)swim.removeModifier(SWIM);
        }
        if(active&&has(entity,"gills")&&entity.isInWaterOrBubble())entity.setAirSupply(entity.getMaxAirSupply());
        if(entity instanceof ServerPlayer player) {
            boolean wings=active&&has(player,"wings");
            var abilities=player.getAbilities();boolean nativeFlight=player.isCreative()||player.isSpectator();
            if(wings&&!nativeFlight) {
                FLIGHT.putIfAbsent(player,abilities.mayfly);
                player.getPersistentData().putBoolean("co_organ_flight",!FLIGHT.get(player));
                if(!abilities.mayfly){abilities.mayfly=true;player.onUpdateAbilities();}
            } else if(FLIGHT.containsKey(player)) {
                boolean prior=FLIGHT.remove(player);
                player.getPersistentData().putBoolean("co_organ_flight",false);
                if(!nativeFlight&&!prior){abilities.mayfly=false;abilities.flying=false;player.fallDistance=0;player.onUpdateAbilities();}
            }
        } else if(active&&has(entity,"wings")&&entity instanceof net.minecraft.world.entity.Mob mob&&!entity.isInWaterOrBubble()) {
            // Assist existing combat navigation without replacing another mob's AI or gravity.
            var velocity=mob.getDeltaMovement();var target=mob.getTarget();
            if(target!=null&&target.isAlive()&&mob.distanceToSqr(target)<1024) {
                var toward=target.position().add(0,1.5,0).subtract(mob.position());
                var horizontal=new net.minecraft.world.phys.Vec3(toward.x,0,toward.z);
                if(horizontal.lengthSqr()>.01)horizontal=horizontal.normalize().scale(.12);
                double lift=toward.y>.3?.16:toward.y<-.5?-.12:.08;
                mob.setDeltaMovement(velocity.x*.7+horizontal.x,Math.max(-.15,Math.min(.28,velocity.y*.6+lift)),velocity.z*.7+horizontal.z);
                mob.hasImpulse=true;mob.fallDistance=0;
            } else if(velocity.y<-.12) {mob.setDeltaMovement(velocity.x,-.12,velocity.z);mob.fallDistance=0;}
        }
    }
    @SubscribeEvent public static void living(LivingEvent.LivingTickEvent event){update(event.getEntity());}
    @SubscribeEvent public static void fall(LivingFallEvent event){
        if(!event.getEntity().level().isClientSide()&&has(event.getEntity(),"wings"))event.setCanceled(true);
    }
    @SubscribeEvent public static void drowned(LivingAttackEvent event){
        if(!event.getEntity().level().isClientSide()&&event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)&&has(event.getEntity(),"gills"))event.setCanceled(true);
    }
    @SubscribeEvent public static void vampire(LivingDamageEvent event){
        if(event.getEntity().level().isClientSide()||event.getAmount()<=0)return;
        var source=event.getSource();
        if(source.getEntity() instanceof LivingEntity attacker&&source.getDirectEntity()==attacker&&attacker!=event.getEntity()
                &&attacker.isAlive()&&has(attacker,"vampire")){
            long tick=attacker.level().getGameTime();
            if(tick-DRAIN.getOrDefault(attacker,Long.MIN_VALUE/2)>=20){attacker.heal(Math.min(2,event.getAmount()*.25f));DRAIN.put(attacker,tick);}
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p){
            // Vanilla persists mayfly: remove our prior survival grant before recalculating.
            if(p.getPersistentData().getBoolean("co_organ_flight")&&!p.isCreative()&&!p.isSpectator()){
                p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();
            }
            update(p);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer p){
            boolean owns=FLIGHT.containsKey(p)&&!FLIGHT.get(p);
            p.getPersistentData().putBoolean("co_organ_flight",owns);
            FLIGHT.remove(p);
        }
    }
}
