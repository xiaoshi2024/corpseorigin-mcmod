package com.xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.xiaoshi2022.corpseorigin.event.BloodRainManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 尸兄血雨指令（需要 OP 权限等级 2）。
 * <ul>
 *   <li>{@code /bloodrain start [秒数] [概率]} —— 立即开启，不填秒数 = 无限</li>
 *   <li>{@code /bloodrain stop} —— 停止</li>
 *   <li>{@code /bloodrain status} —— 查看状态</li>
 *   <li>{@code /bloodrain chance <0~1>} —— 运行中临时调整变异概率</li>
 * </ul>
 */
public final class BloodRainCommand {

    private BloodRainCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bloodrain")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(context -> start(context.getSource(), -1, -1.0F))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 86400))
                                .executes(context -> start(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds"), -1.0F))
                                .then(Commands.argument("chance", DoubleArgumentType.doubleArg(0.0D, 1.0D))
                                        .executes(context -> start(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "seconds"),
                                                (float) DoubleArgumentType.getDouble(context, "chance"))))))
                .then(Commands.literal("stop")
                        .executes(context -> stop(context.getSource())))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource())))
                .then(Commands.literal("chance")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 1.0D))
                                .executes(context -> chance(context.getSource(),
                                        (float) DoubleArgumentType.getDouble(context, "value"))))));
    }

    private static int start(CommandSourceStack source, int seconds, float chance) {
        BloodRainManager.start(source.getServer(), seconds, chance, true);
        String duration = seconds > 0 ? seconds + "s" : "∞";
        int percent = Math.round(BloodRainManager.currentChance() * 100.0F);
        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.blood_rain.started", duration, percent), true);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        boolean wasActive = BloodRainManager.stop(source.getServer(), true);
        source.sendSuccess(() -> Component.translatable(wasActive
                ? "command.corpseorigin.blood_rain.stopped"
                : "command.corpseorigin.blood_rain.inactive"), false);
        return wasActive ? 1 : 0;
    }

    private static int status(CommandSourceStack source) {
        if (!BloodRainManager.isActive()) {
            source.sendSuccess(() -> Component.translatable(
                    "command.corpseorigin.blood_rain.inactive"), false);
            return 0;
        }
        long remaining = BloodRainManager.remainingSeconds(source.getServer());
        String display = remaining < 0 ? "∞" : remaining + "s";
        int percent = Math.round(BloodRainManager.currentChance() * 100.0F);
        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.blood_rain.status", display, percent), false);
        return 1;
    }

    private static int chance(CommandSourceStack source, float value) {
        if (!BloodRainManager.setChance(value)) {
            source.sendSuccess(() -> Component.translatable(
                    "command.corpseorigin.blood_rain.inactive"), false);
            return 0;
        }
        int percent = Math.round(value * 100.0F);
        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.blood_rain.chance_set", percent), true);
        return 1;
    }
}
