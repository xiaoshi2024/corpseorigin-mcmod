package xiaoshi2022.corpseorigin.growth;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.core.Holder;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/** Only equipped, unlocked organs contribute; repeated slots never duplicate bonuses. */
public final class OrganAnatomy {
    private OrganAnatomy() {}
    public static List<OrganDefinition> equipped(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator()||!xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.enabled
            || !p.isCreative()&&!xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.isCorpse(p))return List.of();
        try{
            var ids=new HashSet<String>();
            for(var slot:OrganLibrary.parseSlots(p.getAttachedOrCreate(SurvivalGrowth.BODY).getStringOr(OrganLibrary.BODY_KEY,"[]")))ids.add(slot.organ());
            return OrganLibrary.definitions().stream().filter(d->ids.contains(d.id())&&(p.isCreative()||OrganEvolution.stage(p,d.id())>0)).toList();
        }catch(Exception e){return List.of();}
    }
    public static int capacity(ServerPlayer p,int base){
        return base+equipped(p).stream().filter(d->d.anatomy()!=null).mapToInt(d->d.anatomy().stomachBonus()).max().orElse(0);
    }
    public static void tick(ServerPlayer p){
        var parts=equipped(p).stream().filter(d->d.anatomy()!=null).map(OrganDefinition::anatomy).toList();
        double damage=Math.min(8,parts.stream().mapToDouble(a->a.extraArms()*a.damagePerArm()).sum());
        double speed=Math.min(.20,parts.stream().mapToDouble(a->a.extraLegs()*a.speedPerLeg()).sum());
        apply(p,Attributes.ATTACK_DAMAGE,"organ_extra_arms",damage,AttributeModifier.Operation.ADD_VALUE);
        apply(p,Attributes.MOVEMENT_SPEED,"organ_extra_legs",speed,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if(p.tickCount%20==0&&parts.stream().anyMatch(OrganDefinition.Anatomy::nightVision)
            &&(p.isCreative()||xiaoshi2022.corpseorigin.skill.longyou.BloodReserve.spend(p,1)))
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,40,0,true,false));
    }
    private static void apply(ServerPlayer p,Holder<Attribute> type,String key,double value,AttributeModifier.Operation op){
        var attr=p.getAttribute(type);if(attr==null)return;
        var id=CorpseOrigin.id(key);var old=attr.getModifier(id);
        if(value==0){if(old!=null)attr.removeModifier(id);}
        else if(old==null||old.amount()!=value)attr.addOrReplacePermanentModifier(new AttributeModifier(id,value,op));
    }
}
