package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;

import java.util.Set;

/** Explicit scene cues, with no automatic injuries based on arbitrary combat thresholds. */
public final class ChapterScenes {
    public static final AttachmentType<String> CONDITION=AttachmentRegistry.create(CorpseOrigin.id("chapter_condition"),
            b->b.initializer(()->"").syncWith(ByteBufCodecs.STRING_UTF8,AttachmentSyncPredicate.all()));
    public static final AttachmentType<String> ACTION=AttachmentRegistry.create(CorpseOrigin.id("chapter_action"),
            b->b.initializer(()->"").syncWith(ByteBufCodecs.STRING_UTF8,AttachmentSyncPredicate.all()));
    public static final AttachmentType<Long> UNTIL=AttachmentRegistry.create(CorpseOrigin.id("chapter_action_until"),
            b->b.initializer(()->0L).syncWith(ByteBufCodecs.VAR_LONG,AttachmentSyncPredicate.all()));
    private ChapterScenes(){}
    public static void action(ServerPlayer p,String action,int ticks){
        p.setAttached(ACTION,action);p.setAttached(UNTIL,p.level().getGameTime()+ticks);
    }
    public static void register(){
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->{
            var root=Commands.literal("corpse_scene").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
            for(String cue:new String[]{"injured","parasitized","drain","poisoned","knockback","entrance","threat","bound","fear","black_general","infant","adult","clear"})
                root.then(Commands.literal(cue).then(Commands.argument("players",EntityArgument.players()).executes(ctx->{
                    int count=0;
                    for(ServerPlayer p:EntityArgument.getPlayers(ctx,"players")) {
                        String role=CharacterManager.getInstance().getPlayerCharacterId(p);
                        boolean valid=switch(cue){
                            case "injured" -> Set.of("tushu","muxi").contains(role);
                            case "parasitized" -> Set.of("tushu","fengmohuitailang").contains(role);
                            case "drain" -> role.equals("tushu");
                            case "poisoned","knockback" -> role.equals("k");
                            case "entrance" -> role.equals("zhaoritian");
                            case "threat" -> role.equals("hujie");
                            case "bound","fear","black_general" -> role.equals("bianselong_zb") && p.getAttachedOrCreate(ChapterActorState.DISGUISE).equals("xiaohui");
                            case "infant","adult" -> role.equals("jingang_zb");
                            default -> true;
                        };
                        if(!valid)continue;
                        if(cue.equals("infant") || cue.equals("adult")){p.setAttached(CreatureAbilities.INFANT,cue.equals("infant"));action(p,"transform",32);}
                        else if(cue.equals("clear")){p.setAttached(CONDITION,"");p.setAttached(ACTION,"");p.setAttached(UNTIL,0L);}
                        else if(Set.of("injured","parasitized","poisoned","bound","fear","black_general").contains(cue))p.setAttached(CONDITION,cue);
                        else {
                            action(p,cue,cue.equals("drain")?80:30);
                            if(cue.equals("threat"))p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("scene.corpseorigin.hujie.threat"));
                            if(cue.equals("knockback")){p.setDeltaMovement(p.getLookAngle().scale(-1.1).add(0,.45,0));p.hurtMarked=true;}
                        }
                        count++;
                    }
                    final int applied=count;
                    ctx.getSource().sendSuccess(()->net.minecraft.network.chat.Component.translatable("command.corpseorigin.scene.applied",applied),false);
                    return count;
                })));
            dispatcher.register(root);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(ServerPlayer p:server.getPlayerList().getPlayers()) {
                String action=p.getAttachedOrCreate(ACTION),condition=p.getAttachedOrCreate(CONDITION);
                String role=CharacterManager.getInstance().getPlayerCharacterId(p);
                if(!p.isAlive() || !role.equals(p.getAttachedOrCreate(ChapterActorState.ROLE))) {
                    p.setAttached(CONDITION,"");p.setAttached(ACTION,"");continue;
                }
                if(!action.isEmpty() && p.level().getGameTime()>=p.getAttachedOrCreate(UNTIL))p.setAttached(ACTION,"");
                if(Set.of("bound","fear","black_general").contains(condition)
                        && (!role.equals("bianselong_zb") || !p.getAttachedOrCreate(ChapterActorState.DISGUISE).equals("xiaohui"))) {
                    p.setAttached(CONDITION,"");continue;
                }
                if(p.tickCount%10!=0)continue;
                var level=(ServerLevel)p.level();
                if(condition.equals("parasitized")) {
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,15,0));
                    for(int i=0;i<6;i++) ChapterCombat.dust(level,p.position().add(Math.sin(i+p.tickCount*.1)*.35,.4+i*.2,Math.cos(i+p.tickCount*.1)*.35),0x801331,1);
                }
                if(condition.equals("poisoned")) {
                    p.addEffect(new MobEffectInstance(MobEffects.POISON,30,0));
                    ChapterCombat.dust(level,p.position().add(0,1,0),0x73a43b,1.5f);
                }
                if(action.equals("drain") && p.level().getGameTime()<p.getAttachedOrCreate(UNTIL)) {
                    xiaoshi2022.corpseorigin.character.InnerPowerManager.consume(p,5);
                    if(p.tickCount%20==0 && p.getHealth()>1)
                        p.hurtServer(level,p.damageSources().magic(),Math.min(1,p.getHealth()-1));
                    for(int i=0;i<10;i++) ChapterCombat.dust(level,p.position().add(0,.8+i*.15,0),0xc31536,1.2f);
                }
            }
        });
    }
}
