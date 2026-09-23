package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.*;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.registry.ModItems;
import java.util.*;

/** Server-owned timed skills. State is discarded on death, role change and dimension change. */
public final class SkillRework {
    public static final AttachmentType<Long> GOLD = timer("gold_until");
    public static final AttachmentType<Long> LOTUS_ARMOR = timer("lotus_armor_until");
    private static AttachmentType<Long> timer(String id) { return AttachmentRegistry.create(CorpseOrigin.id(id),
            b -> b.initializer(() -> 0L).syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.all())); }
    private record Cast(ServerPlayer owner, ServerLevel level, String role, String kind, int start, int duration) {}
    private static final Map<String,Cast> CASTS = new HashMap<>();
    private static final Map<UUID,Siphon> SIPHONS = new HashMap<>();
    private static final class Siphon {
        final ServerPlayer owner; final LivingEntity target; final ServerLevel level;
        float absorbed;
        Siphon(ServerPlayer p, LivingEntity t) { owner=p; target=t; level=(ServerLevel)p.level(); }
    }
    public static void start(ServerPlayer p, String kind, int duration) {
        CASTS.put(p.getUUID()+kind,new Cast(p,(ServerLevel)p.level(),CharacterManager.getInstance().getPlayerCharacterId(p),kind,p.tickCount,duration));
    }
    public static boolean siphoning(ServerPlayer p) { return SIPHONS.containsKey(p.getUUID()); }
    public static void siphon(ServerPlayer p) {
        if(SIPHONS.remove(p.getUUID())!=null)return;
        var target=ChapterCombat.aim(p,3.5);
        if(target!=null)SIPHONS.put(p.getUUID(),new Siphon(p,target));
    }
    public static void area(ServerPlayer p, double radius, float damage, double push) {
        var level=(ServerLevel)p.level();
        for(var t:level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius))) {
            if(!ChapterCombat.canHit(p,t) || p.distanceToSqr(t)>radius*radius || !p.hasLineOfSight(t))continue;
            if(t.hurtServer(level,p.damageSources().playerAttack(p),damage)) {
                Vec3 direction=t.position().subtract(p.position()).normalize().scale(push);
                t.push(direction.x,.25,direction.z);t.hurtMarked=true;
            }
        }
    }
    public static void buff(LivingEntity p, net.minecraft.core.Holder<MobEffect> effect, int ticks, int amplifier) {
        p.addEffect(new MobEffectInstance(effect,ticks,amplifier,false,false,true));
    }
    public static void register() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->{
            dispatcher.register(net.minecraft.commands.Commands.literal("tengu_laser")
                    .then(net.minecraft.commands.Commands.argument("target",net.minecraft.commands.arguments.EntityArgument.entity())
                    .executes(ctx->{
                        var p=ctx.getSource().getPlayerOrException();
                        var entity=net.minecraft.commands.arguments.EntityArgument.getEntity(ctx,"target");
                        if(!"fengmohuitailang".equals(CharacterManager.getInstance().getPlayerCharacterId(p))
                                || !(entity instanceof LivingEntity target) || target.level()!=p.level()
                                || !ChapterCombat.canHit(p,target) || p.distanceToSqr(target)>4096 || !p.hasLineOfSight(target)) {
                            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("需要风魔灰太狼角色及64格内可见敌对目标。"));return 0;
                        }
                        var ships=p.level().getEntitiesOfClass(xiaoshi2022.corpseorigin.entity.GreatTenguEntity.class,
                                p.getBoundingBox().inflate(64),s->s.isOwnedBy(p));
                        for(var ship:ships)if(ship.callLaser(target))return 1;
                        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("请先释放大天狗神御，或等待激光冷却（2秒）。"));return 0;
                    })));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{CASTS.clear();SIPHONS.clear();});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var p:server.getPlayerList().getPlayers()) {
                String role=CharacterManager.getInstance().getPlayerCharacterId(p);
                attributes(p,role);
                if(!p.isAlive()) {p.setAttached(GOLD,0L);p.setAttached(LOTUS_ARMOR,0L);continue;}
                if(!role.equals("xiaolu"))p.setAttached(GOLD,0L);
                if(!role.equals("shichaozhizi"))p.setAttached(LOTUS_ARMOR,0L);
                if(role.equals("heixiaofei") && PlayerCharacterData.get(p).hasLearned(p.getUUID(),"black_gold_heart") && p.tickCount%20==0) {
                    p.heal(2);buff(p,MobEffects.REGENERATION,25,1);
                    // The heart is now a client-only inventory preview; its passive effects stay server-side.
                }
                if(p.tickCount%10==0 && p.getAttachedOrCreate(LOTUS_ARMOR)>p.level().getGameTime())
                    QiEffects.aura(p,"lotus_armor",0xcc184f,1.8f,18);
            }
            CASTS.values().removeIf(c->!c.owner.isAlive() || c.owner.isRemoved() || c.owner.level()!=c.level
                    || !c.role.equals(CharacterManager.getInstance().getPlayerCharacterId(c.owner)) || c.owner.tickCount-c.start>=c.duration);
            for(Cast c:CASTS.values()) {
                var p=c.owner;int age=p.tickCount-c.start;
                if(c.kind.equals("slaughter_qi")){
                    if(age%5==0)QiEffects.aura(p,"slaughter",p.getHealth()<p.getMaxHealth()*.35f?0xe61928:0x991d42,2.5f,12);
                }else if(c.kind.equals("sword_flower") || c.kind.equals("round_dance")) {
                    if(age%5==0)QiEffects.aura(p,c.kind,c.kind.equals("sword_flower")?0xff76b3:0xdf203c,6,12);
                    if(age%10==0)area(p,6,c.kind.equals("sword_flower")?18:20,.35);
                } else if(c.kind.equals("blood_cloud")) {
                    if(age%5==0)QiEffects.aura(p,"blood_cloud",0xb51236,4,12);
                    if(age%10==0) {
                        area(p,4,14,.5);
                        for(var t:c.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(4)))
                            if(t==p || p.isAlliedTo(t)){buff(t,MobEffects.STRENGTH,30,2);buff(t,MobEffects.SPEED,30,1);buff(t,MobEffects.RESISTANCE,30,1);}
                    }
                }
            }
            SIPHONS.values().removeIf(s->{
                var p=s.owner;var t=s.target;
                if(!p.isAlive() || p.isRemoved() || p.level()!=s.level || t.level()!=s.level || !ChapterCombat.canHit(p,t)
                        || !CharacterManager.getInstance().getPlayerCharacterId(p).equals("heixiaofei")
                        || !p.getMainHandItem().is(ModItems.BLOOD_WING_BLADE) || p.distanceToSqr(t)>12.25 || !p.hasLineOfSight(t) || p.isShiftKeyDown())return true;
                if(p.tickCount%5==0){
                    QiEffects.aura(t,"siphon_target",0xa80d27,1.3f,12);
                    QiEffects.cloud(s.level,t.getEyePosition().lerp(p.getEyePosition(),.5),0xa80d27,1.2f,12);
                }
                if(p.tickCount%10==0) {
                    float before=t.getHealth();
                    t.hurtServer(s.level,p.damageSources().playerAttack(p),Math.max(16,t.getMaxHealth()*.12f));
                    float drained=Math.max(0,before-t.getHealth());s.absorbed+=drained;p.heal(drained*.6f);
                    if(s.absorbed>p.getMaxHealth()*2) {
                        p.hurtServer(s.level,p.damageSources().magic(),p.getMaxHealth()*.65f);
                        p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("躯体承受不住吸收的气血，黑暗虹吸反噬！"));return true;
                    }
                }
                return !t.isAlive();
            });
        });
    }
    private static void attributes(ServerPlayer p,String role) {
        boolean enhanced=!Set.of("mortal","xiaohui","xiaoyanzi","chuangshang_xingcunzhe","yanhuang_budui").contains(role);
        double health=enhanced?40:0,attack=enhanced?9:0,armor=enhanced?6:0;
        if(Set.of("heixiaofei","tushu","zhaoritian","chongmu","jingang_zb").contains(role)){health=80;attack=17;armor=12;}
        if(role.equals("heixiaofei") && PlayerCharacterData.get(p).hasLearned(p.getUUID(),"black_gold_heart")){health+=60;attack+=8;armor+=8;}
        // Existing boss/form attributes already establish their own much higher baseline.
        if(Set.of("longyou","zuohufa","shichaozhizi").contains(role)){health=0;attack=9;armor=0;}
        modifier(p,Attributes.MAX_HEALTH,"rework_health",health);
        modifier(p,Attributes.ATTACK_DAMAGE,"rework_attack",attack);
        modifier(p,Attributes.ARMOR,"rework_armor",armor);
        modifier(p,Attributes.ARMOR_TOUGHNESS,"rework_toughness",enhanced?4:0);
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
    }
    private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> key,String name,double value) {
        var a=p.getAttribute(key);if(a==null)return;var id=CorpseOrigin.id(name);var old=a.getModifier(id);
        if(old!=null && old.amount()==value)return;
        a.removeModifier(id);if(value!=0)a.addTransientModifier(new AttributeModifier(id,value,AttributeModifier.Operation.ADD_VALUE));
    }
}
