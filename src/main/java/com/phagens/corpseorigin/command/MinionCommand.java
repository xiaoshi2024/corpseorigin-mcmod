package com.phagens.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 尸兄手下命令系统
 * 玩家可以通过命令控制尸兄
 */
public class MinionCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("minion")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("follow")
                        .executes(context -> executeCommand(context, LowerLevelZbEntity.CommandState.FOLLOW)))
                .then(Commands.literal("attack")
                        .executes(context -> executeCommand(context, LowerLevelZbEntity.CommandState.ATTACK)))
                .then(Commands.literal("defend")
                        .executes(context -> executeCommand(context, LowerLevelZbEntity.CommandState.DEFEND)))
                .then(Commands.literal("stay")
                        .executes(context -> executeCommand(context, LowerLevelZbEntity.CommandState.STAY)))
                .then(Commands.literal("list")
                        .executes(MinionCommand::listMinions))
        );
    }

    /**
     * 执行命令
     */
    private static int executeCommand(CommandContext<CommandSourceStack> context, LowerLevelZbEntity.CommandState command) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("只有玩家可以使用此命令"));
            return 0;
        }

        // 获取玩家附近的手下尸兄
        List<LowerLevelZbEntity> minions = getNearbyMinions(player);

        if (minions.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c附近没有你的手下的尸兄！"));
            return 0;
        }

        // 对所有附近的尸兄执行命令
        int count = 0;
        for (LowerLevelZbEntity minion : minions) {
            if (minion.executeCommand(player, command)) {
                count++;
            }
        }

        if (count > 0) {
            String message = switch (command) {
                case FOLLOW -> "§a已命令 " + count + " 个尸兄跟随你！";
                case ATTACK -> "§c已命令 " + count + " 个尸兄进入攻击模式！";
                case DEFEND -> "§b已命令 " + count + " 个尸兄进入防御模式！";
                case STAY -> "§7已命令 " + count + " 个尸兄停留在原地！";
            };
            player.sendSystemMessage(Component.literal(message));
        } else {
            player.sendSystemMessage(Component.literal("§c命令执行失败！"));
        }

        return count;
    }

    /**
     * 列出手下尸兄
     */
    private static int listMinions(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("只有玩家可以使用此命令"));
            return 0;
        }

        List<LowerLevelZbEntity> minions = getNearbyMinions(player);

        if (minions.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7你附近没有手下的尸兄"));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§e=== 附近的尸兄手下 ==="));
        for (LowerLevelZbEntity minion : minions) {
            String state = switch (minion.getCommand()) {
                case FOLLOW -> "§a跟随";
                case ATTACK -> "§c攻击";
                case DEFEND -> "§b防御";
                case STAY -> "§7停留";
            };
            player.sendSystemMessage(Component.literal(
                    "§7- 尸兄 (" + minion.getVariant().name() + ") - " + state
            ));
        }

        return minions.size();
    }

    /**
     * 获取玩家附近的手下尸兄
     */
    private static List<LowerLevelZbEntity> getNearbyMinions(ServerPlayer player) {
        // 32格范围内的尸兄
        AABB searchBox = player.getBoundingBox().inflate(32.0D);
        return player.level().getEntitiesOfClass(
                LowerLevelZbEntity.class,
                searchBox,
                entity -> entity.hasMaster() && 
                         entity.getMasterUUID().equals(player.getUUID())
        );
    }
}
