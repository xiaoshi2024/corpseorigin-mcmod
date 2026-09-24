package xiaoshi2022.corpseorigin.skill.chapter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.skill.*;
import xiaoshi2022.corpseorigin.entity.GourdOrganEntity;
import xiaoshi2022.corpseorigin.growth.GourdOrganState;
public final class NewChapterSkill extends AbstractSkill {
    private final int form,blood;
    public NewChapterSkill(String id,int cooldown,int blood,int form){
        super(id,SkillType.COMBAT,cooldown);this.form=form;this.blood=blood;
    }
    @Override public Component getName(){return Component.translatable("skill.corpseorigin."+getId().getPath());}
    @Override public Component getDescription(){return Component.translatable("skill.corpseorigin."+getId().getPath()+".desc");}
    @Override public int getCost(){return form<0 || form==0?0:super.getCost();}
    @Override public SkillResourceRules.Cost getResourceCost(){return new SkillResourceRules.Cost(0,blood);}
    @Override public Component checkUsable(ServerPlayer p){
        if(GourdCapture.busy(p))return Component.translatable("skill.corpseorigin.gourd_devour.busy");
        if(form<0)return Component.translatable("message.corpseorigin.new_chapter_skill.text_01");
        if(!"xiaojingang".equals(xiaoshi2022.corpseorigin.character.CharacterManager.getInstance().getPlayerCharacterId(p)))return Component.translatable("message.corpseorigin.new_chapter_skill.text_02");
        if(form==5&&!p.getMainHandItem().is(net.minecraft.world.item.Items.FLINT_AND_STEEL)&&!p.getOffhandItem().is(net.minecraft.world.item.Items.FLINT_AND_STEEL))return Component.translatable("message.corpseorigin.new_chapter_skill.text_03");
        if(form!=0 && GourdOrganState.dead(p))return Component.translatable("skill.corpseorigin.gourd.dead");
        if(form!=0 && GourdOrganState.detached(p) && (GourdOrganState.find(p)==null || GourdOrganState.find(p).level()!=p.level()))return Component.translatable("skill.corpseorigin.gourd.away");
        if(form!=0 && GourdOrganState.windingUp(p))return Component.translatable("message.corpseorigin.new_chapter_skill.text_04");
        return null;
    }
    @Override public void onActivate(ServerPlayer p){
        if(form==0){GourdOrganState.toggle(p);return;}
        GourdOrganState.play(p,form,GourdBalance.duration(form));
        // Acquire once on the input tick, before the snake's wind-up animation.
        if(form==3){var g=GourdOrganState.detached(p)?GourdOrganState.find(p):null;devour(p,g==null?p.getEyePosition():g.position().add(0,1.65,0));return;}
        if(form==6){p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE,240,1));p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION,240,1));var g=GourdOrganState.find(p);if(g!=null)g.heal(8);return;}
        if(form==1){p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,400,0));for(var t:p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,p.getBoundingBox().inflate(20),t->ChapterCombat.canHit(p,t)&&p.hasLineOfSight(t)))t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,200,0));return;}
        GourdOrganState.schedule(p,form==5?10:form==4?4:1,form==4?20:10,this::attack);
    }
    private void attack(ServerPlayer p){
        var gourd=GourdOrganState.detached(p)?GourdOrganState.find(p):null;
        if(GourdOrganState.detached(p) && (gourd==null || gourd.level()!=p.level()))return;
        var origin=gourd==null?p.getEyePosition():gourd.position().add(0,form==2?.8:1.65,0);
        if(form==3){devour(p,origin);return;}
        var destination=p.level().clip(new net.minecraft.world.level.ClipContext(origin,origin.add(p.getLookAngle().scale(form==2?5:16)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getLocation();
        var direction=p.getLookAngle();
        for(var target:p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(origin,destination).inflate(form==2?2.5:2),t->ChapterCombat.canHit(p,t))){
            var offset=target.getBoundingBox().getCenter().subtract(origin);double along=offset.dot(direction);
            if(along<0 || along>origin.distanceTo(destination)+.5 || offset.subtract(direction.scale(along)).length()> (form==2?2.5:1+along*.06))continue;
            if(p.level().clip(new net.minecraft.world.level.ClipContext(origin,target.getBoundingBox().getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()!=net.minecraft.world.phys.HitResult.Type.MISS)continue;
            if(!target.hurtServer(p.level(),p.damageSources().playerAttack(p),form==2?26:form==5?5:7))continue;
            if(form==2){target.push(direction.x*1.2,.35,direction.z*1.2);target.hurtMarked=true;}
            if(form==4){target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,160,1));target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS,80,1));}
            if(form==5)target.igniteForSeconds(8);
        }
        if(form==5)ignite(p,origin,destination);
        for(int i=0;i<32;i++){var at=origin.lerp(destination,i/31.0);p.level().sendParticles(form==5?net.minecraft.core.particles.ParticleTypes.FLAME:form==4?net.minecraft.core.particles.ParticleTypes.SPLASH:net.minecraft.core.particles.ParticleTypes.CRIT,at.x,at.y,at.z,2,.04,.04,.04,.03);}
        ChapterCombat.emptyCast(p);
    }
    private void devour(ServerPlayer p,net.minecraft.world.phys.Vec3 origin){
        var end=p.level().clip(new net.minecraft.world.level.ClipContext(origin,origin.add(p.getLookAngle().scale(8)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getLocation();
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p,origin,end,new net.minecraft.world.phys.AABB(origin,end).inflate(.7),e->e instanceof net.minecraft.world.entity.LivingEntity t && !(t instanceof net.minecraft.world.entity.player.Player) && !(t instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) && !(t instanceof net.minecraft.world.entity.boss.wither.WitherBoss) && !(t instanceof net.minecraft.world.entity.decoration.ArmorStand) && !(t instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isTame()) && ChapterCombat.canHit(p,t),origin.distanceToSqr(end));
        if(hit==null){p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd_devour.no_target"));return;}
        var target=(net.minecraft.world.entity.LivingEntity)hit.getEntity();
        if(!GourdCapture.edible(target)){p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd_devour.too_strong"));return;}
        if(!GourdCapture.begin(p,target))p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd_devour.busy"));
    }

    private void ignite(ServerPlayer p,net.minecraft.world.phys.Vec3 origin,net.minecraft.world.phys.Vec3 end){
        if(!p.mayBuild()||p.isSpectator())return;
        for(int i=2;i<=Math.ceil(origin.distanceTo(end));i+=2){
            var point=origin.lerp(end,Math.min(1,i/Math.max(1,origin.distanceTo(end))));
            var ground=p.level().clip(new net.minecraft.world.level.ClipContext(point,point.add(0,-3,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
            if(ground.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)continue;
            var pos=ground.getBlockPos().relative(ground.getDirection());
            if(!p.level().hasChunkAt(pos)||!p.level().getWorldBorder().isWithinBounds(pos)||!p.level().mayInteract(p,pos)||!p.level().isEmptyBlock(pos))continue;
            var fire=net.minecraft.world.level.block.BaseFireBlock.getState(p.level(),pos);
            if(fire.canSurvive(p.level(),pos))p.level().setBlockAndUpdate(pos,fire);
        }
    }
}
