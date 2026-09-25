package xiaoshi2022.corpseorigin.growth;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.util.LocalizedException;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.tags.FluidTags;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

/** Abilities are selected and paid on the server, only from the saved equipped loadout. */
public final class OrganAbilities {
    private OrganAbilities() {}
    public static Component use(ServerPlayer p,boolean drink){
        if(!p.isAlive()||p.isSpectator())return Component.translatable("message.corpseorigin.organ.feedback.9");
        if(!xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.enabled)return Component.translatable("message.corpseorigin.organ.feedback.10");
        if(!p.isCreative()&&!xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.isCorpse(p))return Component.translatable("message.corpseorigin.organ.feedback.11");
        var body=p.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
        java.util.List<OrganSlot> slots;
        try{slots=OrganLibrary.parseSlots(body.getStringOr(OrganLibrary.BODY_KEY,"[]"));}catch(Exception e){return Component.translatable("message.corpseorigin.organ.feedback.12");}
        OrganDefinition def=null;
        for(var slot:slots){
            var match=OrganLibrary.definitions().stream().filter(d->d.id().equals(slot.organ())&&d.waterJet()!=null).findFirst();
            if(match.isPresent()&&(p.isCreative()||OrganEvolution.stage(p,slot.organ())>0)){def=match.get();break;}
        }
        if(def==null)return Component.translatable("message.corpseorigin.organ.feedback.13");
        var jet=def.waterJet();String id=def.id()+":"+jet.materialType();
        int capacity=OrganAnatomy.capacity(p,jet.capacity());
        boolean waterMaterial=jet.materialType().equals("water");
        int water=Math.clamp(body.getIntOr("organ_water:"+id,waterMaterial?body.getIntOr("organ_water:"+def.id(),0):0),0,capacity);
        body.remove("organ_water:"+def.id());
        long now=p.level().getGameTime(),until=body.getLongOr("organ_water_cd:"+id,0);
        if(until>now && until-now<=1200)return Component.translatable("message.corpseorigin.organ.feedback.14");
        var start=p.getEyePosition();
        if(drink){
            if(water>=capacity)return Component.translatable("message.corpseorigin.organ.full", water, capacity);
            var hit=p.level().clip(new ClipContext(start,start.add(p.getLookAngle().scale(4)),ClipContext.Block.COLLIDER,ClipContext.Fluid.SOURCE_ONLY,p));
            if(waterMaterial&&!p.isCreative()&&(hit.getType()!=HitResult.Type.BLOCK||!p.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER)))return Component.translatable("message.corpseorigin.organ.feedback.15");
            if(!waterMaterial&&!p.isCreative()){
                var stack=p.getMainHandItem();
                boolean allowed=jet.materialType().equals("mud")?(stack.is(net.minecraft.world.item.Items.DIRT)||stack.is(net.minecraft.world.item.Items.MUD))
                    :(stack.is(net.minecraft.world.item.Items.SAND)||stack.is(net.minecraft.world.item.Items.RED_SAND));
                if(!allowed)return Component.translatable("message.corpseorigin.organ.feedback.16");
                stack.shrink(1);
            }
            water=Math.min(capacity,water+jet.refill());
            body.putLong("organ_water_cd:"+id,now+20);
        }else{
            if(!p.isCreative()&&water<jet.cost())return Component.translatable("message.corpseorigin.organ.empty", water, capacity);
            if(!p.isCreative())water-=jet.cost();
            body.putLong("organ_water_cd:"+id,now+jet.cooldownTicks());
            var target=ChapterCombat.aim(p,jet.range());
            var end=p.level().clip(new ClipContext(start,start.add(p.getLookAngle().scale(jet.range())),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();
            if(target!=null){end=target.getBoundingBox().getCenter();target.hurtServer(p.level(),p.damageSources().playerAttack(p),jet.damage());}
            // 沿整条水柱只发一朵气（取中点，半径≈线长一半，上限 8）
            QiEffects.cloud((ServerLevel) p.level(), start.add(end).scale(0.5), waterMaterial?0x4fc3f7:0xc0182a,
                    (float)Math.min(8.0,Math.max(0.5,start.distanceTo(end)*0.5)),12);
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        }
        body.putInt("organ_water:"+id,water);p.setAttached(SurvivalGrowth.BODY,body);
        return Component.translatable("message.corpseorigin.organ.jet_result", drink?Component.translatable("message.corpseorigin.organ.refilled"):Component.translatable("message.corpseorigin.organ.fired"), Component.translatable("organ.corpseorigin.material."+jet.materialType()), water, capacity);
    }
}
