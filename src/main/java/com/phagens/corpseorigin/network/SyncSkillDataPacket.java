package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.skill.SkillAttachment;
import com.phagens.corpseorigin.skill.SkillHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步技能数据包 - 服务器到客户端
 */
public record SyncSkillDataPacket(
        int playerId,
        List<ResourceLocation> learnedSkills,
        int evolutionPoints,
        Map<ResourceLocation, Integer> cooldowns
) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            CorpseOrigin.MODID, "sync_skill_data");

    public static final Type<SyncSkillDataPacket> TYPE = new Type<>(ID);

    private static final StreamCodec<ByteBuf, List<ResourceLocation>> SKILL_LIST_CODEC =
            ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC);

    private static final StreamCodec<ByteBuf, Map<ResourceLocation, Integer>> COOLDOWN_MAP_CODEC =
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT);

    public static final StreamCodec<ByteBuf, SyncSkillDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SyncSkillDataPacket::playerId,
            SKILL_LIST_CODEC,
            SyncSkillDataPacket::learnedSkills,
            ByteBufCodecs.VAR_INT,
            SyncSkillDataPacket::evolutionPoints,
            COOLDOWN_MAP_CODEC,
            SyncSkillDataPacket::cooldowns,
            SyncSkillDataPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端处理 - 修复数据同步
     */
    public static void handleClient(SyncSkillDataPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
//            CorpseOrigin.LOGGER.info("客户端收到技能同步包 - 玩家ID: {}, 技能数量: {}, 进化点: {}",
//                    packet.playerId(), packet.learnedSkills().size(), packet.evolutionPoints());

            Level level = context.player().level();
            Entity entity = level.getEntity(packet.playerId());

            if (entity instanceof Player player) {
                // 获取或创建技能处理器
                SkillHandler handler = (SkillHandler) SkillAttachment.getSkillHandler(player);

                if (handler != null) {
                    // 加载同步数据
                    handler.loadFromSyncData(packet.learnedSkills(), packet.evolutionPoints(), packet.cooldowns());

                    // 重新应用被动技能
//                    handler.reapplyPassiveSkills();

//                    CorpseOrigin.LOGGER.info("客户端技能数据更新完成 - 玩家: {}, 技能数量: {}, 进化点: {}",
//                            player.getName().getString(),
//                            handler.getLearnedSkills().size(),
//                            handler.getEvolutionPoints());
                }
            }
        });
    }
}