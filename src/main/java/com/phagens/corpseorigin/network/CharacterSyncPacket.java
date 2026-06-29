package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.character.CharacterManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CharacterSyncPacket(int playerId, String characterId) implements CustomPacketPayload {
    public static final Type<CharacterSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "character_sync"));

    public static final StreamCodec<FriendlyByteBuf, CharacterSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            CharacterSyncPacket::playerId,
            ByteBufCodecs.STRING_UTF8,
            CharacterSyncPacket::characterId,
            CharacterSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CharacterSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            CharacterManager.getInstance().setClientCachedCharacter(packet.characterId());

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§a角色已切换为: " + CharacterManager.getInstance().getPlayerCharacter(mc.player).getName().getString()
                ));
            }
        });
    }
}