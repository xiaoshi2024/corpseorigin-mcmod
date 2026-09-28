package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.growth.GourdOrganState;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillResourceRules;
import xiaoshi2022.corpseorigin.skill.SkillType;

public final class NewChapterSkill extends AbstractSkill {
    private final int form,blood;

    /** 「烈焰火海」的铺火范围：以玩家为中心、沿视线铺一段扇环（不是向前喷一条长线）。 */
    private static final double FIRE_INNER_RADIUS=1.5,FIRE_OUTER_RADIUS=5.0,FIRE_RADIUS_STEP=1.0;
    /** 一次脉冲铺多宽的一段弧；10 次脉冲 ≈ 480°，所以原地转一圈就能把火连成环。 */
    private static final float FIRE_ARC_DEGREES=48.0F,FIRE_ARC_STEP_DEGREES=12.0F;
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
        if(form==5)ignite(p);
        var mid=origin.lerp(destination,.5);
        QiEffects.cloud((net.minecraft.server.level.ServerLevel)p.level(),mid,form==5?0xff7a1a:form==4?0x4fc3f7:0xc0182a,(float)Math.max(.5,Math.min(8,origin.distanceTo(destination)/2)),12);
        ChapterCombat.emptyCast(p);
    }
    private void devour(ServerPlayer p,net.minecraft.world.phys.Vec3 origin){
        var end=p.level().clip(new net.minecraft.world.level.ClipContext(origin,origin.add(p.getLookAngle().scale(8)),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getLocation();
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p,origin,end,new net.minecraft.world.phys.AABB(origin,end).inflate(.7),e->GourdCapture.validPrey(p,GourdCapture.target(e)),origin.distanceToSqr(end));
        if(hit==null){p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd_devour.no_target"));return;}
        var target=GourdCapture.target(hit.getEntity());
        if(!GourdCapture.edible(target)){p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd_devour.too_strong"));return;}
        if(!GourdCapture.begin(p,target))p.sendOverlayMessage(Component.translatable("skill.corpseorigin.gourd_devour.busy"));
    }

    /**
     * 「烈焰火海」铺火：以玩家为中心、沿视线方向铺一段扇环。
     * <p>
     * 每次脉冲只铺一小段弧，而脉冲之间玩家能转视角 —— 所以原地转一圈，火就围成一个环。
     * 铺下的火登记在 {@link FlameSea}：不蔓延、{@link FlameSea#LIFETIME_TICKS} 后自行熄灭。
     */
    private void ignite(ServerPlayer p){
        if(!p.mayBuild()||p.isSpectator()||!(p.level() instanceof net.minecraft.server.level.ServerLevel level))return;
        var look=p.getLookAngle();
        var flat=new net.minecraft.world.phys.Vec3(look.x,0,look.z);
        if(flat.lengthSqr()<1.0E-6)flat=new net.minecraft.world.phys.Vec3(0,0,1);
        flat=flat.normalize();
        for(float offset=-FIRE_ARC_DEGREES/2;offset<=FIRE_ARC_DEGREES/2;offset+=FIRE_ARC_STEP_DEGREES){
            var direction=flat.yRot(offset);
            for(double radius=FIRE_INNER_RADIUS;radius<=FIRE_OUTER_RADIUS;radius+=FIRE_RADIUS_STEP){
                var point=p.position().add(direction.x*radius,0,direction.z*radius);
                var ground=level.clip(new net.minecraft.world.level.ClipContext(point,point.add(0,-3,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p));
                if(ground.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)continue;
                var pos=ground.getBlockPos().relative(ground.getDirection());
                if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(p,pos)||!level.isEmptyBlock(pos))continue;
                FlameSea.place(level,pos);
            }
        }
    }
}
