package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record LongyouDialogueOptionsPacket(String question, List<String> options) implements CustomPacketPayload {
    public static final Type<LongyouDialogueOptionsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "longyou_dialogue_options"));

    public static final StreamCodec<FriendlyByteBuf, LongyouDialogueOptionsPacket> STREAM_CODEC = 
            StreamCodec.ofMember(LongyouDialogueOptionsPacket::write, LongyouDialogueOptionsPacket::new);

    public LongyouDialogueOptionsPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readList(FriendlyByteBuf::readUtf));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(question);
        buf.writeCollection(options, FriendlyByteBuf::writeUtf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LongyouDialogueOptionsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientHandler.handleDialogueOptionsPacket(packet.question, packet.options, context);
        });
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }
}
