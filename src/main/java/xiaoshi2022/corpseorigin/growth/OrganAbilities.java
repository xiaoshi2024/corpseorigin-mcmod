package xiaoshi2022.corpseorigin.growth;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.tags.FluidTags;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;

/** Abilities are selected and paid on the server, only from the saved equipped loadout. */
public final class OrganAbilities {
    private OrganAbilities() {}
    public static String use(ServerPlayer p,boolean drink){
        if(!p.isAlive()||p.isSpectator())return "当前状态不能使用器官特性";
        if(!xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.enabled)return "服务器未启用器官成长";
        if(!p.isCreative()&&!xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.isCorpse(p))return "需要尸兄身体";
        var body=p.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
        java.util.List<OrganSlot> slots;
        try{slots=OrganLibrary.parseSlots(body.getStringOr(OrganLibrary.BODY_KEY,"[]"));}catch(Exception e){return "器官装配数据无效";}
        OrganDefinition def=null;
        for(var slot:slots){
            var match=OrganLibrary.definitions().stream().filter(d->d.id().equals(slot.organ())&&d.waterJet()!=null).findFirst();
            if(match.isPresent()&&(p.isCreative()||OrganEvolution.stage(p,slot.organ())>0)){def=match.get();break;}
        }
        if(def==null)return "请先解锁并保存带吞吐特性的器官";
        var jet=def.waterJet();String id=def.id()+":"+jet.materialType();
        int capacity=OrganAnatomy.capacity(p,jet.capacity());
        boolean waterMaterial=jet.materialType().equals("water");
        int water=Math.clamp(body.getIntOr("organ_water:"+id,waterMaterial?body.getIntOr("organ_water:"+def.id(),0):0),0,capacity);
        body.remove("organ_water:"+def.id());
        long now=p.level().getGameTime(),until=body.getLongOr("organ_water_cd:"+id,0);
        if(until>now && until-now<=1200)return "器官正在恢复，请稍候";
        var start=p.getEyePosition();
        if(drink){
            if(water>=capacity)return "储量已满："+water+" / "+capacity;
            var hit=p.level().clip(new ClipContext(start,start.add(p.getLookAngle().scale(4)),ClipContext.Block.COLLIDER,ClipContext.Fluid.SOURCE_ONLY,p));
            if(waterMaterial&&!p.isCreative()&&(hit.getType()!=HitResult.Type.BLOCK||!p.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER)))return "对准4格内的水源，潜行按G吸水";
            if(!waterMaterial&&!p.isCreative()){
                var stack=p.getMainHandItem();
                boolean allowed=jet.materialType().equals("mud")?(stack.is(net.minecraft.world.item.Items.DIRT)||stack.is(net.minecraft.world.item.Items.MUD))
                    :(stack.is(net.minecraft.world.item.Items.SAND)||stack.is(net.minecraft.world.item.Items.RED_SAND));
                if(!allowed)return "主手拿泥土/泥巴或沙子，再潜行按特性键吞入对应材料";
                stack.shrink(1);
            }
            water=Math.min(capacity,water+jet.refill());
            body.putLong("organ_water_cd:"+id,now+20);
        }else{
            if(!p.isCreative()&&water<jet.cost())return "储量不足："+water+" / "+capacity+"，潜行按特性键补充材料";
            if(!p.isCreative())water-=jet.cost();
            body.putLong("organ_water_cd:"+id,now+jet.cooldownTicks());
            var target=ChapterCombat.aim(p,jet.range());
            var end=p.level().clip(new ClipContext(start,start.add(p.getLookAngle().scale(jet.range())),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();
            if(target!=null){end=target.getBoundingBox().getCenter();target.hurtServer(p.level(),p.damageSources().playerAttack(p),jet.damage());}
            int particles=Math.max(1,(int)Math.ceil(start.distanceTo(end)*3));
            for(int i=0;i<=particles;i++){var at=start.lerp(end,(double)i/particles);p.level().sendParticles(waterMaterial?net.minecraft.core.particles.ParticleTypes.SPLASH:net.minecraft.core.particles.ParticleTypes.CRIT,at.x,at.y,at.z,2,.03,.03,.03,.03);}
            p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
        }
        body.putInt("organ_water:"+id,water);p.setAttached(SurvivalGrowth.BODY,body);
        return (drink?"材料补充完成":"吞吐攻击")+" · "+jet.materialType()+" 储量 "+water+" / "+capacity;
    }
}
