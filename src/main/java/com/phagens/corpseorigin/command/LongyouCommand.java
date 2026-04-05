package com.phagens.corpseorigin.command;

import com.phagens.corpseorigin.GongFU.JSskill.JSSkillEngine;
import com.phagens.corpseorigin.entity.LongyouEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 尸王命令系统
 * 用于测试尸王的技能
 */
public class LongyouCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("longyou")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("testjs")
                        .executes(LongyouCommand::testJsSkill))
        );
    }

    /**
     * 测试尸王的 JS 技能
     */
    private static int testJsSkill(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("只有玩家可以使用此命令"));
            return 0;
        }

        // 获取玩家附近的尸王
        List<LongyouEntity> longyouEntities = player.level().getEntitiesOfClass(
                LongyouEntity.class,
                player.getBoundingBox().inflate(64.0D)
        );

        if (longyouEntities.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c附近没有尸王！"));
            return 0;
        }

        // 对第一个尸王执行 JS 技能测试
        LongyouEntity longyou = longyouEntities.get(0);
        boolean success = JSSkillEngine.getInstance().executeEntitySkill("尸巢召唤", longyou);

        if (success) {
            player.sendSystemMessage(Component.literal("§a已成功触发尸王的 JS 技能！"));
        } else {
            player.sendSystemMessage(Component.literal("§c触发尸王的 JS 技能失败！"));
        }

        return success ? 1 : 0;
    }
}