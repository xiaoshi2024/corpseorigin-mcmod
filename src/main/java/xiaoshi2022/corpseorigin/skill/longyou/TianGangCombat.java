package xiaoshi2022.corpseorigin.skill.longyou;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.chapter.*;
import java.util.*;
import static xiaoshi2022.corpseorigin.skill.longyou.TianGangSkill.Form;

public final class TianGangCombat {
    public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean> SHEN_ACTIVE=
            net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(CorpseOrigin.id("tiangang_shen_active"),
                    b->b.initializer(()->false).syncWith(net.minecraft.network.codec.ByteBufCodecs.BOOL,
                            net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all()));
    private record Aura(ServerPlayer player,ServerLevel level,Form form,int until){}
    private record Action(ServerPlayer player,ServerLevel level,Form form,int start,Vec3 origin,Vec3 direction,Set<UUID> hits){}
    private static final Map<UUID,Aura> AURAS=new HashMap<>();
    private static final List<Action> ACTIONS=new ArrayList<>();
    public static boolean isShen(ServerPlayer p){var a=AURAS.get(p.getUUID());return a!=null && a.player==p && a.form==Form.SHEN && valid(p,a.level) && p.tickCount<a.until;}
    /** 御与八重合一的神可化解天罡匙；必须是当前身体仍在生效的护体。 */
    public static boolean blocksTianGangBlade(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) return false;
        Aura aura = AURAS.get(player.getUUID());
        return aura != null && aura.player == player
                && (aura.form == Form.YU || aura.form == Form.SHEN)
                && valid(player, aura.level) && player.tickCount < aura.until;
    }
    private static boolean valid(ServerPlayer p,ServerLevel level){
        return p.isAlive() && !p.isRemoved() && p.level()==level && ("longyou".equals(CharacterManager.getInstance().getPlayerCharacterId(p))
                || xiaoshi2022.corpseorigin.growth.FreeGrowth.learnedFrom(p,"longyou"));
    }
    public static boolean canLeap(ServerPlayer p){return p.level().noCollision(p,p.getBoundingBox().expandTowards(0,2,0));}
    public static void cast(ServerPlayer p,Form form){
        ServerLevel level=(ServerLevel)p.level();
        if(Set.of(Form.JI,Form.LI,Form.YU,Form.SHEN).contains(form)){
            removeAura(p);int ticks=form==Form.YU?300:600;
            AURAS.put(p.getUUID(),new Aura(p,level,form,p.tickCount+ticks));p.setAttached(SHEN_ACTIVE,form==Form.SHEN);apply(p,form);
            ChapterScenes.action(p,"charge",20);return;
        }
        if(form==Form.POGANG || form==Form.WU){
            var target=ChapterCombat.aim(p,form==Form.WU?6:4);if(target!=null) {
                breakGuard(target);
                target.hurtServer(level,p.damageSources().playerAttack(p),form==Form.WU?36:48);
                QiEffects.cloud(level,target.getBoundingBox().getCenter(),0x88aaff,1.6f,16);
            }
            ChapterScenes.action(p,"tiangang_left",16);p.swing(net.minecraft.world.InteractionHand.OFF_HAND,true);return;
        }
        ACTIONS.removeIf(a->a.player==p);
        ACTIONS.add(new Action(p,level,form,p.tickCount,p.position(),p.getLookAngle(),new HashSet<>()));
        if(form==Form.HUI || form==Form.TIANGANGPO){
            p.setDeltaMovement(0,form==Form.HUI?1.25:1.55,0);p.hurtMarked=true;ChapterScenes.action(p,"tiangang_slam",55);
        }else ChapterScenes.action(p,form==Form.MIE?"tiangang_right":"tiangang_combo",form==Form.MIE?20:50);
    }
    public static void breakGuard(LivingEntity target){
        target.stopUsingItem();target.removeEffect(MobEffects.RESISTANCE);target.removeEffect(MobEffects.ABSORPTION);
        target.setAbsorptionAmount(0);target.setAttached(SkillRework.LOTUS_ARMOR,0L);
        if(target instanceof ServerPlayer p && AURAS.containsKey(p.getUUID()))removeAura(p);
    }
    private static void modifier(ServerPlayer p,Holder<Attribute> key,String suffix,double amount,AttributeModifier.Operation op){
        var a=p.getAttribute(key);if(a==null)return;
        var id=CorpseOrigin.id("tiangang_"+suffix);a.removeModifier(id);
        if(amount!=0)a.addTransientModifier(new AttributeModifier(id,amount,op));
    }
    private static void apply(ServerPlayer p,Form f){
        modifier(p,Attributes.MOVEMENT_SPEED,"speed",f==Form.JI?1:f==Form.SHEN?.5:0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(p,Attributes.ATTACK_DAMAGE,"strength",f==Form.LI?1.5:f==Form.SHEN?1:0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(p,Attributes.SCALE,"scale",f==Form.LI?-.25:0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(p,Attributes.ARMOR,"armor",f==Form.SHEN?10:0,AttributeModifier.Operation.ADD_VALUE);
    }
    private static void removeAura(ServerPlayer p){AURAS.remove(p.getUUID());p.setAttached(SHEN_ACTIVE,false);apply(p,Form.ZHI);}
    public static void register(){
        // Attribute modifiers are transient; cleanup also runs for offline player objects before releasing them.
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{for(var a:List.copyOf(AURAS.values()))removeAura(a.player);ACTIONS.clear();});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var a:List.copyOf(AURAS.values())){
                var p=a.player;if(!valid(p,a.level) || p.tickCount>=a.until){removeAura(p);continue;}
                if(p.tickCount%5==0){
                    QiEffects.aura(p,"tiangang",a.form==Form.SHEN?0xffd779:0x88aaff,2,12);
                    if(a.form==Form.YU || a.form==Form.SHEN)SkillRework.buff(p,MobEffects.RESISTANCE,6,a.form==Form.YU?3:2);
                }
            }
            ACTIONS.removeIf(a->{
                var p=a.player;int age=p.tickCount-a.start;
                if(!valid(p,a.level) || age>70 || (a.form.advanced() && !isShen(p)))return true;
                if(a.form==Form.NIPO){
                    if(age%10==0){var target=ChapterCombat.aim(p,4);
                        if(target!=null)target.hurtServer(a.level,p.damageSources().playerAttack(p),24);
                        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);}
                    return age>=50;
                }
                if(a.form==Form.MIE){
                    Vec3 start=a.origin.add(0,1.2,0).add(a.direction.scale((age-1)*1.5));
                    Vec3 end=start.add(a.direction.scale(1.5));
                    if(!a.level.hasChunkAt(BlockPos.containing(end)))return true;
                    var block=a.level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
                    end=block.getLocation();
                    QiEffects.cloud(a.level,start.lerp(end,.5),0x88aaff,1.2f,12);
                    for(var target:a.level.getEntitiesOfClass(LivingEntity.class,new AABB(start,end).inflate(1))){
                        if(!ChapterCombat.canHit(p,target) || a.hits.contains(target.getUUID())
                                || a.level.clip(new ClipContext(start,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS)continue;
                        a.hits.add(target.getUUID());
                        if(target.hurtServer(a.level,p.damageSources().playerAttack(p),40))ImpactTerrain.launch(p,target,2);
                    }
                    return age>=16 || block.getType()!=HitResult.Type.MISS;
                }
                if(a.form==Form.HUI || a.form==Form.TIANGANGPO){
                    p.fallDistance=0;
                    if(age>=12 && !p.onGround()){p.setDeltaMovement(0,-1.8,0);p.hurtMarked=true;}
                    if(age<5 || !p.onGround())return false;
                    double radius=a.form==Form.HUI?8:12;
                    GroundShockwave.spawn(p,p.position(),radius,a.form==Form.HUI?1.5:2.5);
                    SkillRework.area(p,radius,a.form==Form.HUI?45:70,1.8);
                    var center=p.blockPosition().below();int budget=128;
                    for(var pos:BlockPos.betweenClosed(center.offset(-(int)radius,-1,-(int)radius),center.offset((int)radius,0,(int)radius))){
                        if(budget<=0)break;
                        if(pos.distSqr(center)<=radius*radius && ImpactTerrain.breakBlock(p,pos,false))budget--;
                    }
                    QiEffects.cloud(a.level,p.position(),0xffd779,(float)radius,20);
                    ChapterScenes.action(p,"release",10);return true;
                }
                return true;
            });
        });
    }
}
