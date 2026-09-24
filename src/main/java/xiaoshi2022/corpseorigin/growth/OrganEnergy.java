package xiaoshi2022.corpseorigin.growth;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.skill.longyou.BloodReserve;

/** Server-owned flight permission and prepaid organ activity, in active ticks. */
public final class OrganEnergy {
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean> OWNED=
            net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(xiaoshi2022.corpseorigin.CorpseOrigin.id("organ_flight_permission"),
                    b->b.initializer(()->false).persistent(com.mojang.serialization.Codec.BOOL));
    private static final class State {final ServerPlayer player;int flightTicks,waterTicks;boolean wasFlying;State(ServerPlayer p){player=p;}}
    private OrganEnergy() {}
    private static void mark(ServerPlayer p,boolean owned){
        p.setAttached(OWNED,owned);
    }
    public static void tick(ServerPlayer p){
        boolean owned=p.getAttachedOrCreate(OWNED);
        if(p.isCreative()||p.isSpectator()){
            if(owned)mark(p,false);
            STATES.remove(p.getUUID());return;
        }
        var cfg=CorpseConfig.get().growth;
        boolean active=cfg.enabled&&p.isAlive()&&PlayerCorpseComponent.isCorpse(p);
        State state=STATES.compute(p.getUUID(),(k,old)->old==null||old.player!=p?new State(p):old);
        int flightCost=Math.max(1,Math.clamp(cfg.flightBloodPerSecond,1,600)-OrganEvolution.efficiency(p,"wings")/2);
        boolean wings=active&&SurvivalGrowth.has(p,"wings")&&!p.isInWater()&&!p.isPassenger();
        boolean canFly=wings&&(state.flightTicks>0||BloodReserve.get(p)>=flightCost);
        var abilities=p.getAbilities();
        // Do not claim flight already granted by another source.
        if(canFly&&!abilities.mayfly){abilities.mayfly=true;mark(p,true);owned=true;p.onUpdateAbilities();}
        if(owned&&canFly&&abilities.flying){
            if(state.flightTicks<=0){
                if(BloodReserve.spend(p,flightCost))state.flightTicks=20;
                else canFly=false;
            }
            if(canFly){state.flightTicks--;p.fallDistance=0;
                if(OrganEvolution.power(p,"wings") && p.isSprinting()) p.setDeltaMovement(p.getDeltaMovement().add(p.getLookAngle().scale(.025)));
            }
        }
        if(owned&&!canFly){
            boolean falling=abilities.flying||state.wasFlying;
            abilities.flying=false;abilities.mayfly=false;mark(p,false);p.onUpdateAbilities();
            if(falling&&p.isAlive()&&!p.onGround()&&!p.isInWater()){
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,100,0,false,false,true));
                p.sendOverlayMessage(Component.literal("翅膀供能停止：短暂缓降，请尽快着陆"));
            }
        }
        state.wasFlying=owned&&canFly&&abilities.flying;
        if(active&&SurvivalGrowth.has(p,"gills")&&p.isInWater()){
            if(state.waterTicks<=0&&BloodReserve.spend(p,Math.max(1,Math.clamp(cfg.aquaticBloodPerSecond,1,600)-OrganEvolution.efficiency(p,"gills")/2)))state.waterTicks=20;
            if(state.waterTicks>0){
                state.waterTicks--;
                if(p.isUnderWater())p.setAirSupply(p.getMaxAirSupply());
                p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,2,0,true,false));
                if(OrganEvolution.power(p,"gills") && p.isSprinting())p.setDeltaMovement(p.getDeltaMovement().add(p.getLookAngle().scale(.015)));
            }
        }
        if(!active||!SurvivalGrowth.has(p,"wings"))state.flightTicks=0;
        if(!active||!SurvivalGrowth.has(p,"gills"))state.waterTicks=0;
    }
    public static void register(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers())tick(p);});
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{
            var p=h.player;
            if(p.getAttachedOrCreate(OWNED)){
                if(!p.isCreative()&&!p.isSpectator()){p.getAbilities().flying=false;p.getAbilities().mayfly=false;}
                mark(p,false);
            }
            STATES.remove(p.getUUID());
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(s->STATES.clear());
    }
}
