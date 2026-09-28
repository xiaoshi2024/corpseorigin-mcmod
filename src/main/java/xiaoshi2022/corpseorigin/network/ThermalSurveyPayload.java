package xiaoshi2022.corpseorigin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.growth.ThermalSurvey;

public record ThermalSurveyPayload(String dimension, boolean active, ThermalSurvey.Result result) implements CustomPacketPayload {
    public static final Type<ThermalSurveyPayload> TYPE = new Type<>(CorpseOrigin.id("thermal_survey"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ThermalSurveyPayload> CODEC = StreamCodec.ofMember(
            (p,b)->{b.writeUtf(p.dimension);b.writeBoolean(p.active);b.writeVarInt(p.result.explored());b.writeVarInt(p.result.loaded());b.writeVarInt(p.result.corpses());b.writeVarInt(p.result.corpsePlayers());b.writeVarInt(p.result.incubating());},
            b->new ThermalSurveyPayload(b.readUtf(),b.readBoolean(),new ThermalSurvey.Result(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt())));
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
