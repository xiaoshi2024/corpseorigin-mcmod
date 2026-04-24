package com.phagens.corpseorigin.network;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.compat.curios.CuriosIntegration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * 巨阙剑取出网络包
 * 客户端 -> 服务端
 */
public record JuQueTakeOutPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JuQueTakeOutPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CorpseOrigin.MODID, "juque_take_out"));

    public static final StreamCodec<FriendlyByteBuf, JuQueTakeOutPacket> STREAM_CODEC =
            StreamCodec.ofMember(JuQueTakeOutPacket::encode, JuQueTakeOutPacket::decode);

    private static final String BACK_SLOT_IDENTIFIER = "back";

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(FriendlyByteBuf buf) {
        // 无数据需要编码
    }

    private static JuQueTakeOutPacket decode(FriendlyByteBuf buf) {
        return new JuQueTakeOutPacket();
    }

    /**
     * 服务端处理
     */
    public static void handle(JuQueTakeOutPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            // 执行物品交换
            performJuQueTakeOut(serverPlayer);
        });
    }

    /**
     * 执行巨阙剑取出逻辑（服务端）
     */
    private static void performJuQueTakeOut(ServerPlayer player) {
        try {
            // Curios 不可用时直接返回
            if (!CuriosIntegration.isCuriosAvailable()) {
                CorpseOrigin.LOGGER.debug("Curios 未安装，无法取出巨阙剑");
                return;
            }

            // 1. 查找背部槽位中的巨阙剑
            Optional<SlotResult> juQueSlot = findFirstCurioInBackSlot(player,
                    stack -> isJuQueItem(stack));

            if (juQueSlot.isEmpty()) {
                CorpseOrigin.LOGGER.debug("背部槽位未找到巨阙剑");
                return;
            }

            SlotResult slotResult = juQueSlot.get();
            ItemStack juQueStack = slotResult.stack();
            ItemStack mainHandStack = player.getMainHandItem();

            // 2. 从背部槽位移除巨阙剑
            removeCurioFromSlot(player, slotResult);

            // 3. 将巨阙剑装备到主手
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, juQueStack);

            // 4. 处理原主手物品：如果原主手有物品，且不是巨阙剑，则放入背包
            if (!mainHandStack.isEmpty()) {
                // 如果原主手物品也是巨阙剑（不同槽位的），直接放到背部槽位
                if (isJuQueItem(mainHandStack)) {
                    equipToBackSlot(player, mainHandStack);
                } else {
                    // 其他物品：尝试放入背包，如果背包满了就掉落到地上
                    if (!player.getInventory().add(mainHandStack)) {
                        // 背包满了，丢到地上
                        player.drop(mainHandStack, false);
                        CorpseOrigin.LOGGER.info("主手物品 {} 已掉落", mainHandStack.getDisplayName().getString());
                    } else {
                        CorpseOrigin.LOGGER.info("主手物品 {} 已放入背包", mainHandStack.getDisplayName().getString());
                    }
                }
            }

            // 5. 同步物品栏到客户端
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();

            CorpseOrigin.LOGGER.info("巨阙剑已从背部槽位取出到主手 - 玩家: {}", player.getName().getString());

        } catch (Exception e) {
            CorpseOrigin.LOGGER.warn("取出巨阙剑失败: {}", e.getMessage());
        }
    }

    /**
     * 查找背部槽位中符合条件的饰品
     */
    private static Optional<SlotResult> findFirstCurioInBackSlot(Player player, Predicate<ItemStack> predicate) {
        if (player == null) return Optional.empty();

        var curiosInventory = CuriosApi.getCuriosInventory(player);
        if (curiosInventory.isEmpty()) return Optional.empty();

        var handler = curiosInventory.get();
        // 检查标准 back 槽位
        var stacksHandler = handler.getStacksHandler(BACK_SLOT_IDENTIFIER);

        if (stacksHandler.isPresent()) {
            var stacks = stacksHandler.get().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stackInSlot = stacks.getStackInSlot(i);
                if (!stackInSlot.isEmpty() && predicate.test(stackInSlot)) {
                    return Optional.of(new SlotResult(
                            new top.theillusivec4.curios.api.SlotContext(
                                    BACK_SLOT_IDENTIFIER, player, i, false, true
                            ),
                            stackInSlot
                    ));
                }
            }
        }

        // 如果标准 back 槽位没找到，尝试其他可能的背部相关槽位
        String[] backRelatedSlots = {"back", "cape", "body"};
        for (String slotType : backRelatedSlots) {
            var otherHandler = handler.getStacksHandler(slotType);
            if (otherHandler.isPresent()) {
                var stacks = otherHandler.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stackInSlot = stacks.getStackInSlot(i);
                    if (!stackInSlot.isEmpty() && predicate.test(stackInSlot)) {
                        return Optional.of(new SlotResult(
                                new top.theillusivec4.curios.api.SlotContext(
                                        slotType, player, i, false, true
                                ),
                                stackInSlot
                        ));
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * 从 Curios 槽位移除物品
     */
    private static void removeCurioFromSlot(Player player, SlotResult slotResult) {
        if (player == null || slotResult == null) return;

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getStacksHandler(slotResult.slotContext().identifier()).ifPresent(stackHandler -> {
                stackHandler.getStacks().setStackInSlot(slotResult.slotContext().index(), ItemStack.EMPTY);
                stackHandler.update();
            });
        });
    }

    /**
     * 装备物品到背部槽位
     */
    private static void equipToBackSlot(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return;

        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            // 找到第一个可用的背部槽位
            var backHandler = handler.getStacksHandler(BACK_SLOT_IDENTIFIER);
            if (backHandler.isPresent()) {
                var stacks = backHandler.get().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i).isEmpty()) {
                        handler.setEquippedCurio(BACK_SLOT_IDENTIFIER, i, stack);
                        return;
                    }
                }
                // 如果没有空槽位，装备到第一个槽位（覆盖）
                handler.setEquippedCurio(BACK_SLOT_IDENTIFIER, 0, stack);
            } else {
                // 如果背部槽位不存在，尝试其他槽位
                handler.setEquippedCurio(BACK_SLOT_IDENTIFIER, 0, stack);
            }
        });
    }

    /**
     * 判断物品是否为巨阙剑
     */
    private static boolean isJuQueItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return itemId.contains("ming_juque") || itemId.contains("ming_juque_tw");
    }
}