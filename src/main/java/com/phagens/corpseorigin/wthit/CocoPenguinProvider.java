package com.phagens.corpseorigin.wthit;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.Animals.CocoPenguinEntity;
import mcp.mobius.waila.api.*;
import mcp.mobius.waila.api.component.ItemComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * 企鹅信息提供者 - 为WTHIT高亮模组提供企鹅的详细信息
 */
public enum CocoPenguinProvider implements IEntityComponentProvider {

    INSTANCE;

    private static final ItemStack PENGUIN_ICON = new ItemStack(Items.COD);

    @Nullable
    @Override
    public ITooltipComponent getIcon(IEntityAccessor accessor, IPluginConfig config) {
        if (accessor.getEntity() instanceof CocoPenguinEntity) {
            return new ItemComponent(PENGUIN_ICON);
        }
        return null;
    }

    @Override
    public void appendHead(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof CocoPenguinEntity)) {
            return;
        }

        // 获取企鹅名称
        String name = accessor.getEntity().getName().getString();
        tooltip.addLine(Component.literal("🐧 " + name).withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof CocoPenguinEntity)) {
            return;
        }

        // 优先使用服务器同步的数据
        CompoundTag serverData = accessor.getData().raw();
        CocoPenguinEntity penguin = (CocoPenguinEntity) accessor.getEntity();

        // 获取饥饿值（优先从同步数据获取）
        int hunger = serverData.contains("hunger") ? serverData.getInt("hunger") : penguin.getHunger();
        int maxHunger = serverData.contains("maxHunger") ? serverData.getInt("maxHunger") : 720;
        boolean isHungry = serverData.contains("isHungry") ? serverData.getBoolean("isHungry") : penguin.isHungry();
        boolean canEat = serverData.contains("canEat") ? serverData.getBoolean("canEat") : penguin.canEat();

        // 计算饥饿百分比
        int hungerPercent = (int) ((double) hunger / maxHunger * 100);

        // 根据饥饿值选择颜色
        ChatFormatting hungerColor;
        if (hungerPercent <= 30) {
            hungerColor = ChatFormatting.RED;      // 饥饿
        } else if (hungerPercent <= 60) {
            hungerColor = ChatFormatting.GOLD;     // 有点饿
        } else {
            hungerColor = ChatFormatting.GREEN;    // 饱腹
        }

        // 饥饿条显示（包含百分比）
        String hungerBar = getHungerBar(hunger, maxHunger);
        tooltip.addLine(Component.literal("🍗 饥饿: ")
                .append(Component.literal(hungerBar).withStyle(hungerColor))
                .append(Component.literal(" " + hunger + "/" + maxHunger + " (" + hungerPercent + "%)").withStyle(ChatFormatting.WHITE)));

        // 饥饿状态警告
        if (isHungry) {
            tooltip.addLine(Component.literal("⚠️ 饥饿中! 需要捕鱼!").withStyle(ChatFormatting.RED));
        }

        // 进食冷却状态
        if (!canEat) {
            tooltip.addLine(Component.literal("⏳ 进食冷却中...").withStyle(ChatFormatting.GRAY));
        }

        // 健康度
        float health = penguin.getHealth();
        float maxHealth = penguin.getMaxHealth();
        int healthPercent = (int) (health / maxHealth * 100);
        ChatFormatting healthColor = healthPercent <= 30 ? ChatFormatting.RED :
                healthPercent <= 60 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
        tooltip.addLine(Component.literal("❤️ 生命: ")
                .append(Component.literal(String.format("%.1f", health) + "/" + String.format("%.1f", maxHealth) + " (" + healthPercent + "%)")
                        .withStyle(healthColor)));

        // 氧气显示（仅在水下或近水时显示？可以一直显示）
        int airSupply = serverData.contains("airSupply") ? serverData.getInt("airSupply") : penguin.getAirSupply();
        int maxAir = serverData.contains("maxAir") ? serverData.getInt("maxAir") : 300;
        int airPercent = (int) ((double) airSupply / maxAir * 100);

        ChatFormatting airColor;
        if (airSupply <= 60) {
            airColor = ChatFormatting.RED;      // 氧气不足
        } else if (airSupply <= 150) {
            airColor = ChatFormatting.YELLOW;   // 氧气警告
        } else {
            airColor = ChatFormatting.AQUA;     // 氧气充足
        }

        String airBar = getAirBar(airSupply, maxAir);
        tooltip.addLine(Component.literal("💧 氧气: ")
                .append(Component.literal(airBar).withStyle(airColor))
                .append(Component.literal(" " + airSupply + "/" + maxAir + " (" + airPercent + "%)").withStyle(ChatFormatting.WHITE)));

        // 额外信息
        if (penguin.isBaby()) {
            tooltip.addLine(Component.literal("🐣 幼年企鹅").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        if (penguin.isInLove()) {
            tooltip.addLine(Component.literal("💕 求爱模式").withStyle(ChatFormatting.RED));
        }
    }

    /**
     * 生成氧气条显示
     */
    private String getAirBar(int air, int maxAir) {
        int barLength = 10;
        int filledBars = (int) ((double) air / maxAir * barLength);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");

        return bar.toString();
    }

    /**
     * 生成饥饿条显示
     */
    private String getHungerBar(int hunger, int maxHunger) {
        int barLength = 10;
        int filledBars = (int) ((double) hunger / maxHunger * barLength);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");

        return bar.toString();
    }
}