package xiaoshi2022.corpseorigin.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 皮肤更新网络包 - Fabric 26.2
 * 客户端 → 服务端：通知服务端实体皮肤加载状态
 */
public record ZbSkinUpdatePacket(
        int entityId,          // 实体ID
        String skinTexture,    // 皮肤纹理路径 (空字符串表示默认)
        int skinStateCode      // 皮肤状态码 (ZbSkinState)
) implements CustomPacketPayload {

    public static final Type<ZbSkinUpdatePacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "skin_update")
    );

    public static final StreamCodec<ByteBuf, ZbSkinUpdatePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ZbSkinUpdatePacket::entityId,
            ByteBufCodecs.STRING_UTF8,
            ZbSkinUpdatePacket::skinTexture,
            ByteBufCodecs.INT,
            ZbSkinUpdatePacket::skinStateCode,
            ZbSkinUpdatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}