package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.EvolutionStats;
import xiaoshi2022.corpseorigin.skill.EvolutionTier;

/** Panel requests never carry balances, prices, ranks, or configuration from the client. */
public final class RealmNetworking {
    private RealmNetworking() {}
    public record Action(String action, String stat, int count) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(CorpseOrigin.id("realm_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.ofMember(
                (p,b)->{b.writeUtf(p.action,32);b.writeUtf(p.stat,32);b.writeVarInt(p.count);},
                b->new Action(b.readUtf(32),b.readUtf(32),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record State(CompoundTag data, boolean open) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(CorpseOrigin.id("realm_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.ofMember(
                (p,b)->{b.writeNbt(p.data);b.writeBoolean(p.open);}, b->new State(b.readNbt(),b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE,Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(State.TYPE,State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE,(payload,context)->context.server().execute(()->{
            ServerPlayer p=context.player();
            switch(payload.action) {
                case "train" -> RealmProgression.purchase(p,payload.stat,payload.count);
                case "meditate" -> RealmProgression.meditate(p);
                case "refine" -> RealmProgression.refine(p,payload.stat);
                case "burst" -> RealmProgression.burst(p);
                case "recharge" -> RealmProgression.recharge(p);
                case "view" -> { }
                case "refresh" -> { }   // 面板打开期间的实时轮询：只回数据，不重开屏
                default -> { return; }
            }
            send(p,payload.action.equals("view"));
        }));
    }
    public static void send(ServerPlayer p, boolean open) {
        var data=PlayerCharacterData.get(p); var tag=data.cultivation(p.getUUID()); var cfg=RealmProgression.config();
        for(String stat:RealmProgression.STATS) tag.putInt("total_"+stat,RealmProgression.rank(p,stat));
        tag.putInt("available",data.getAvailablePoints(p.getUUID()));
        tag.putInt("base_cost",cfg.trainingCostBase); tag.putInt("step_cost",cfg.trainingCostStep);
        tag.putInt("rank_limit",cfg.maxTrainingRank); tag.putInt("xp_per_rank",cfg.practiceXpPerRank);
        tag.putInt("recharge_cost",cfg.rechargePointCost);
        int level=RealmProgression.level(p);
        tag.putInt("level",level); tag.putBoolean("enabled",cfg.enabled);
        // 6A 面板：境界名 / 累计 / 距下阶 + 六维最终加成（含成长原型与角色特调，服务端权威计算）
        tag.putString("tier", EvolutionTier.formatFullName(level).getString());
        int earned=data.getEarnedPoints(p.getUUID());
        tag.putInt("earned",earned); tag.putInt("to_next",EvolutionManager.pointsToNextLevel(earned));
        tag.putDouble("bonus_vitality", EvolutionStats.tunedHealth(p));
        tag.putDouble("bonus_power", EvolutionStats.tunedAttack(p));
        tag.putDouble("bonus_guard", RealmProgression.protection(p));
        tag.putDouble("bonus_qi", RealmProgression.qiBonus(p));
        tag.putDouble("bonus_recovery", RealmProgression.regeneration(p));
        tag.putDouble("bonus_agility", RealmProgression.speedBonus(p));
        ServerPlayNetworking.send(p,new State(tag,open));
    }
}
