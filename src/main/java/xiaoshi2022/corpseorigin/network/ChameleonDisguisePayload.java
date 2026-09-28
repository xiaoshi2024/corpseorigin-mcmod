package xiaoshi2022.corpseorigin.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.SkillManager;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class ChameleonDisguisePayload {
    public record Select(String playerId) implements CustomPacketPayload {
        public static final Type<Select> TYPE=new Type<>(CorpseOrigin.id("chameleon_select"));
        public static final StreamCodec<ByteBuf,Select> CODEC=StreamCodec.composite(ByteBufCodecs.stringUtf8(36),Select::playerId,Select::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Result(String error) implements CustomPacketPayload {
        public static final Type<Result> TYPE=new Type<>(CorpseOrigin.id("chameleon_result"));
        public static final StreamCodec<ByteBuf,Result> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,Result::error,Result::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static final Map<UUID,UUID> PENDING=new HashMap<>();
    private static final Map<UUID,Long> LAST_REQUEST=new HashMap<>();
    private ChameleonDisguisePayload(){}
    public static void register(){
        PayloadTypeRegistry.serverboundPlay().register(Select.TYPE,Select.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Result.TYPE,Result.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Select.TYPE,(payload,context)->context.server().execute(()->select(context.player(),payload.playerId())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{PENDING.remove(handler.player.getUUID());LAST_REQUEST.remove(handler.player.getUUID());});
        ServerLifecycleEvents.SERVER_STOPPED.register(server->{PENDING.clear();LAST_REQUEST.clear();});
    }
    private static void reply(ServerPlayer p,String error){ServerPlayNetworking.send(p,new Result(error));}
    public static void select(ServerPlayer p,String input){
        if(input.equals("#cancel")){PENDING.remove(p.getUUID());return;}
        if(input.equals("#restore")){
            p.setAttached(ChapterActorState.DISGUISE,"");
            p.setAttached(ChapterActorState.DISGUISE_UNTIL,0L);
            p.removeAttached(ChapterActorState.DISGUISE_PROFILE);
            reply(p,"");
            return;
        }
        if(!p.isAlive() || p.isSpectator() || !"bianselong_zb".equals(CharacterManager.getInstance().getPlayerCharacterId(p))){reply(p,"unavailable");return;}
        if(!xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(p).hasLearned(p.getUUID(),"chameleon_disguise")
                || SkillManager.snapshotRemaining(p).containsKey("chameleon_disguise")){reply(p,"unavailable");return;}
        if(PENDING.containsKey(p.getUUID())){reply(p,"busy");return;}
        long now=System.currentTimeMillis();
        if(now-LAST_REQUEST.getOrDefault(p.getUUID(),0L)<1000){reply(p,"busy");return;}
        LAST_REQUEST.put(p.getUUID(),now);
        String value=input.strip();
        if(value.isEmpty()){finish(p,null);return;}
        // 小惠是变色龙伪装的固定剧情皮肤；允许界面或命令直接输入 xiaohui。
        if(value.equalsIgnoreCase("xiaohui") || value.equalsIgnoreCase("小惠")){
            finish(p,null);
            return;
        }
        UUID id=null;
        try{id=UUID.fromString(value);}catch(IllegalArgumentException ignored){}
        if(id==null && !value.matches("[A-Za-z0-9_]{1,16}")){reply(p,"invalid");return;}
        var server=p.level().getServer();
        ServerPlayer online=id==null?server.getPlayerList().getPlayerByName(value):server.getPlayerList().getPlayer(id);
        if(online!=null){finish(p,online.getGameProfile());return;}
        UUID token=UUID.randomUUID();PENDING.put(p.getUUID(),token);
        final UUID lookup=id;
        CompletableFuture.supplyAsync(()->lookup==null?server.services().profileResolver().fetchByName(value):server.services().profileResolver().fetchById(lookup),
                net.minecraft.util.Util.nonCriticalIoPool()).orTimeout(20,TimeUnit.SECONDS).whenComplete((profile,error)->server.execute(()->{
            if(!token.equals(PENDING.get(p.getUUID())))return;
            PENDING.remove(p.getUUID());
            if(server.getPlayerList().getPlayer(p.getUUID())!=p)return;
            if(error!=null || profile==null || profile.isEmpty()){reply(p,"not_found");return;}
            finish(p,profile.get());
        }));
    }
    private static void finish(ServerPlayer p,com.mojang.authlib.GameProfile profile){
        if(!p.isAlive() || p.isSpectator() || !SkillManager.activate(p,"chameleon_disguise")){reply(p,"unavailable");return;}
        if(profile!=null){
            p.setAttached(ChapterActorState.DISGUISE_PROFILE,profile);
            p.setAttached(ChapterActorState.DISGUISE,profile.id().toString());
        }
        reply(p,"");
    }
}
