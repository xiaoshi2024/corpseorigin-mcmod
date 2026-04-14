package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.GongFU.GongFaZL.BaseGongFaItem;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaData;
import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaSkillManager;
import com.phagens.corpseorigin.GongFU.ModUtlis.GongFUDataUtlis;
import com.phagens.corpseorigin.skill.ISkillHandler;
import com.phagens.corpseorigin.skill.SkillAttachment;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

/**
 * 同步功法容器数据包 - 客户端到服务器
 * 用于将功法容器数据同步到服务器（解决首次加入服务器时没有数据的问题）
 */
public record SyncGongFuContainerPacket(CompoundTag containerData) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            CorpseOrigin.MODID, "sync_gongfu_container");

    public static final Type<SyncGongFuContainerPacket> TYPE = new Type<>(ID);

    // 使用 NBT 的 StreamCodec
    public static final StreamCodec<ByteBuf, SyncGongFuContainerPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            SyncGongFuContainerPacket::containerData,
            SyncGongFuContainerPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 创建同步包（从客户端容器数据）
     */
    public static SyncGongFuContainerPacket create(NonNullList<ItemStack> items) {
        CompoundTag containerData = new CompoundTag();
        // 使用 ContainerHelper 保存物品（保存完整的 NBT 数据）
        ContainerHelper.saveAllItems(containerData, items, null);
        return new SyncGongFuContainerPacket(containerData);
    }

    /**
     * 从 packet 数据恢复物品列表
     */
    public NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> items = NonNullList.withSize(6, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(containerData, items, null);
        return items;
    }

    /**
     * 服务器端处理
     */
    public static void handleServer(SyncGongFuContainerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // 保存到玩家持久化数据
                CompoundTag playerData = serverPlayer.getPersistentData();
                playerData.put("GongFuContainer", packet.containerData());

                // 应用功法属性
                GongFUDataUtlis.applyGongFaAttributes(serverPlayer);

                // ⭐ 学习功法技能
                updateGongFuSkills(serverPlayer, packet.getItems());

                CorpseOrigin.LOGGER.info("【功法同步】玩家 {} 的功法容器数据已同步到服务器，学习了 {} 个功法技能",
                        serverPlayer.getName().getString(),
                        packet.getItems().stream().filter(s -> !s.isEmpty()).count());
            }
        });
    }

    /**
     * 更新玩家的功法技能学习状态
     */
    private static void updateGongFuSkills(ServerPlayer player, NonNullList<ItemStack> items) {
        ISkillHandler handler = SkillAttachment.getSkillHandler(player);
        if (handler == null) return;

        // 获取当前容器中的所有功法技能
        Set<ResourceLocation> equippedGongFuSkills = new HashSet<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.getItem() instanceof BaseGongFaItem gongFaItem) {
                GongFaData data = gongFaItem.getDataFromItem(stack);
                if (data != null) {
                    ResourceLocation skillId = GongFaSkillManager.getInstance()
                            .getGongFuSkillId(data.getTypeId(), data.getRarity(), data.getCeng());
                    if (skillId != null) {
                        equippedGongFuSkills.add(skillId);
                        // 学习技能 - 使用 learnGongFuSkill 绕过尸兄检查
                        if (!handler.hasLearned(skillId)) {
                            if (handler instanceof com.phagens.corpseorigin.skill.SkillHandler skillHandler) {
                                skillHandler.learnGongFuSkill(skillId);
                                CorpseOrigin.LOGGER.info("【功法同步】玩家 {} 学习功法技能 {}",
                                        player.getName().getString(), skillId);
                            }
                        }
                    }
                }
            }
        }

        // 遗忘不再装备的功法技能
        for (var skill : handler.getLearnedSkills()) {
            if (skill.getId().getPath().startsWith("gongfu_")) {
                if (!equippedGongFuSkills.contains(skill.getId())) {
                    handler.forgetSkill(skill);
                    CorpseOrigin.LOGGER.info("【功法同步】玩家 {} 遗忘功法技能 {}",
                            player.getName().getString(), skill.getId());
                }
            }
        }
    }
}
