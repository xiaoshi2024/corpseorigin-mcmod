
package com.phagens.corpseorigin.Item;

import com.phagens.corpseorigin.GongFU.GongFaZL.GongFaCategory;
import com.phagens.corpseorigin.GongFU.GongFaZL.SlotConfigManager;
import com.phagens.corpseorigin.GongFU.PackGongFu.Paket.SyncSlotConfigPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 功法槽位扩展物品
 * 使用后永久增加指定类别的功法容器槽位数量
 * 每个玩家对同类型扩展物品只能使用一次，使用后物品消失
 */
public class SlotExpansionItem extends Item {

    private static final String PLAYER_USED_PREFIX = "SlotExpansionUsed_";

    private final int slotCountToAdd;
    private final GongFaCategory targetCategory;
    private final String expansionName;

    /**
     * 构造函数 - 指定类别的槽位扩展
     * @param properties 物品属性
     * @param slotCountToAdd 增加的槽位数量
     * @param targetCategory 目标类别
     * @param expansionName 扩展包名称
     */
    public SlotExpansionItem(Properties properties, int slotCountToAdd, GongFaCategory targetCategory, String expansionName) {
        super(properties.stacksTo(1));
        this.slotCountToAdd = slotCountToAdd;
        this.targetCategory = targetCategory;
        this.expansionName = expansionName;
    }

    /**
     * 构造函数 - 通用槽位扩展（增加到UNIVERSAL类别）
     */
    public SlotExpansionItem(Properties properties, int slotCountToAdd, String expansionName) {
        this(properties, slotCountToAdd, GongFaCategory.UNIVERSAL, expansionName);
    }

    /**
     * 检查玩家是否已使用过此类型的扩展物品
     */
    private boolean hasPlayerUsed(Player player) {
        CompoundTag playerData = player.getPersistentData();
        String key = PLAYER_USED_PREFIX + targetCategory.getName() + "_" + slotCountToAdd;
        return playerData.getBoolean(key);
    }

    /**
     * 标记玩家已使用过此类型的扩展物品
     */
    private void markPlayerAsUsed(Player player) {
        CompoundTag playerData = player.getPersistentData();
        String key = PLAYER_USED_PREFIX + targetCategory.getName() + "_" + slotCountToAdd;
        playerData.putBoolean(key, true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (hasPlayerUsed(player)) {
            player.sendSystemMessage(Component.literal("§c你已经使用过此类扩展包了！无法重复生效。"));
            return InteractionResultHolder.fail(stack);
        }

        if (!SlotConfigManager.canUnlockMoreSlots(player)) {
            player.sendSystemMessage(Component.literal("§c功法槽位已达上限（60/60）！"));
            return InteractionResultHolder.fail(stack);
        }

        int actualAdded = SlotConfigManager.addCategorySlot(player, targetCategory, slotCountToAdd);

        if (actualAdded <= 0) {
            player.sendSystemMessage(Component.literal("§c无法增加槽位！"));
            return InteractionResultHolder.fail(stack);
        }

        markPlayerAsUsed(player);

        int currentTotal = SlotConfigManager.getUnlockedSlotCount(player);
        int categoryCount = SlotConfigManager.getCategorySlotCount(player, targetCategory);

        // 同步槽位配置到客户端
        if (!player.level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            SyncSlotConfigPacket packet =
                    SyncSlotConfigPacket.create(serverPlayer);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
        player.sendSystemMessage(Component.literal(
                "§a【" + expansionName + "】已激活！" +
                        "§b" + targetCategory.getDisplayName().getString() + " §f槽位 +" + actualAdded +
                        "（当前: " + categoryCount + "）" +
                        " §7总槽位: " + currentTotal + "/36"
        ));

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.literal("§7右键使用以永久增加修行槽位"));
        tooltipComponents.add(Component.literal("§b增加类别: §f" + targetCategory.getDisplayName().getString()));
        tooltipComponents.add(Component.literal("§b增加槽位数: §f+" + slotCountToAdd));
        tooltipComponents.add(Component.literal("§e使用后物品消失"));
        tooltipComponents.add(Component.literal("§c§l警告: 每个玩家只能使用一次"));
    }
}