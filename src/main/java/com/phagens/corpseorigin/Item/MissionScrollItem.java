package com.phagens.corpseorigin.Item;

import com.phagens.corpseorigin.register.Moditems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

public class MissionScrollItem extends Item {
    public static final String KEY_MISSION_TYPE = "mission_type";
    public static final String KEY_TARGET_COUNT = "target_count";
    public static final String KEY_CURRENT_COUNT = "current_count";
    public static final String KEY_MISSION_ID = "mission_id";
    public static final String KEY_REWARD_EVOLUTION_POINTS = "reward_evolution_points";
    public static final String KEY_REWARD_LEVELS = "reward_levels";

    public MissionScrollItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static ItemStack createMissionScroll(String missionType, int targetCount, int rewardEvolutionPoints, int rewardLevels) {
        ItemStack stack = new ItemStack(Moditems.MISSION_SCROLL.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_MISSION_TYPE, missionType);
        tag.putInt(KEY_TARGET_COUNT, targetCount);
        tag.putInt(KEY_CURRENT_COUNT, 0);
        tag.putString(KEY_MISSION_ID, java.util.UUID.randomUUID().toString());
        tag.putInt(KEY_REWARD_EVOLUTION_POINTS, rewardEvolutionPoints);
        tag.putInt(KEY_REWARD_LEVELS, rewardLevels);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static CompoundTag getTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }

    private static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static String getMissionType(ItemStack stack) {
        return getTag(stack).getString(KEY_MISSION_TYPE);
    }

    public static int getTargetCount(ItemStack stack) {
        return getTag(stack).getInt(KEY_TARGET_COUNT);
    }

    public static int getCurrentCount(ItemStack stack) {
        return getTag(stack).getInt(KEY_CURRENT_COUNT);
    }

    public static void setCurrentCount(ItemStack stack, int count) {
        CompoundTag tag = getTag(stack);
        tag.putInt(KEY_CURRENT_COUNT, Math.min(count, getTargetCount(stack)));
        setTag(stack, tag);
    }

    public static void incrementCount(ItemStack stack) {
        int current = getCurrentCount(stack);
        setCurrentCount(stack, current + 1);
    }

    public static boolean isCompleted(ItemStack stack) {
        return getCurrentCount(stack) >= getTargetCount(stack);
    }

    public static int getRewardEvolutionPoints(ItemStack stack) {
        return getTag(stack).getInt(KEY_REWARD_EVOLUTION_POINTS);
    }

    public static int getRewardLevels(ItemStack stack) {
        return getTag(stack).getInt(KEY_REWARD_LEVELS);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        String missionType = getMissionType(stack);
        int targetCount = getTargetCount(stack);
        int currentCount = getCurrentCount(stack);
        int rewardPoints = getRewardEvolutionPoints(stack);
        int rewardLevels = getRewardLevels(stack);

        String missionName = getMissionDisplayName(missionType);
        tooltipComponents.add(Component.translatable("item.corpseorigin.mission_scroll.type", missionName)
                .withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("item.corpseorigin.mission_scroll.progress", currentCount, targetCount)
                .withStyle(ChatFormatting.GREEN));
        
        if (isCompleted(stack)) {
            tooltipComponents.add(Component.translatable("item.corpseorigin.mission_scroll.completed")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        } else {
            tooltipComponents.add(Component.translatable("item.corpseorigin.mission_scroll.incomplete")
                    .withStyle(ChatFormatting.YELLOW));
        }
        
        tooltipComponents.add(Component.translatable("item.corpseorigin.mission_scroll.reward_points", rewardPoints)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltipComponents.add(Component.translatable("item.corpseorigin.mission_scroll.reward_levels", rewardLevels)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        
        if (!level.isClientSide) {
            if (isCompleted(stack)) {
                player.sendSystemMessage(Component.translatable("item.corpseorigin.mission_scroll.can_submit"));
            } else {
                int remaining = getTargetCount(stack) - getCurrentCount(stack);
                player.sendSystemMessage(Component.translatable("item.corpseorigin.mission_scroll.remaining", remaining));
            }
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isCompleted(stack);
    }

    private String getMissionDisplayName(String missionType) {
        return switch (missionType) {
            case "kill_villager" -> Component.translatable("mission.corpseorigin.kill_villager").getString();
            case "kill_zombie" -> Component.translatable("mission.corpseorigin.kill_zombie").getString();
            case "infect_player" -> Component.translatable("mission.corpseorigin.infect_player").getString();
            case "collect_item" -> Component.translatable("mission.corpseorigin.collect_item").getString();
            case "collect_block" -> Component.translatable("mission.corpseorigin.collect_block").getString();
            default -> missionType;
        };
    }
}
