package com.phagens.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.phagens.corpseorigin.character.BaiXiaoFei;
import com.phagens.corpseorigin.character.CharacterManager;
import com.phagens.corpseorigin.character.ICharacter;
import com.phagens.corpseorigin.character.LongYou;
import com.phagens.corpseorigin.character.MortalCharacter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class CharacterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("character")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("list")
                                .executes(context -> {
                                    StringBuilder list = new StringBuilder("§6===== 可用角色列表 =====\n");
                                    for (ICharacter character : CharacterManager.getInstance().getRegisteredCharacters()) {
                                        list.append("§b- ").append(character.getName().getString())
                                                .append(" §7(ID: ").append(character.getId()).append(")\n");
                                    }
                                    context.getSource().sendSuccess(
                                            () -> net.minecraft.network.chat.Component.literal(list.toString()), false
                                    );
                                    return 1;
                                })
                        )

                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("baixiaofei")
                                                .executes(context -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                    CharacterManager.getInstance().setPlayerCharacter(player, new BaiXiaoFei());
                                                    context.getSource().sendSuccess(
                                                            () -> net.minecraft.network.chat.Component.literal(
                                                                    "§a已将玩家 " + player.getName().getString() + " 设置为白小飞"
                                                            ), true
                                                    );
                                                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                                            "§a你已成为白小飞！\n" +
                                                                    "§7技能：热血、格斗术、生存本能、飞踢、剑术精通\n" +
                                                                    "§7按 R 键打开技能轮盘查看技能！"
                                                    ));
                                                    return 1;
                                                })
                                        )
                                        .then(Commands.literal("longyou")
                                                .executes(context -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                    CharacterManager.getInstance().setPlayerCharacter(player, new LongYou());
                                                    context.getSource().sendSuccess(
                                                            () -> net.minecraft.network.chat.Component.literal(
                                                                    "§a已将玩家 " + player.getName().getString() + " 设置为龙右"
                                                            ), true
                                                    );
                                                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                                            "§c§l你已成为龙右！尸王降临！§r\n" +
                                                                    "§7技能：尸王威压、黑暗能量、血继限界、不死不灭、怨灵缠身、恐惧支配\n" +
                                                                    "§7按 R 键打开技能轮盘查看技能！"
                                                    ));
                                                    return 1;
                                                })
                                        )
                                        .then(Commands.literal("mortal")
                                                .executes(context -> {
                                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                    CharacterManager.getInstance().setPlayerCharacter(player, MortalCharacter.getInstance());
                                                    context.getSource().sendSuccess(
                                                            () -> net.minecraft.network.chat.Component.literal(
                                                                    "§a已将玩家 " + player.getName().getString() + " 设置为凡人"
                                                            ), true
                                                    );
                                                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                                            "§a你已恢复为凡人状态"
                                                    ));
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                            CharacterManager.getInstance().clearPlayerCharacter(player);
                                            context.getSource().sendSuccess(
                                                    () -> net.minecraft.network.chat.Component.literal(
                                                            "§a已清除玩家 " + player.getName().getString() + " 的角色状态"
                                                    ), true
                                            );
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("info")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                            ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);

                                            StringBuilder info = new StringBuilder();
                                            info.append("§6===== 玩家 ").append(player.getName().getString()).append(" 当前角色 =====\n");
                                            info.append("§e角色名称: §f").append(character.getName().getString()).append("\n");
                                            info.append("§e角色ID: §f").append(character.getId()).append("\n");
                                            info.append("§e角色描述: §f").append(character.getDescription().getString()).append("\n");
                                            info.append("§e技能数量: §f").append(character.getSkills().size()).append("\n");
                                            info.append("§e是否被动: §f").append(character.isPassive()).append("\n");

                                            if (!character.getTraits().isEmpty()) {
                                                info.append("§e角色特性:\n");
                                                for (var trait : character.getTraits()) {
                                                    info.append("  §b- ").append(trait.getString()).append("\n");
                                                }
                                            }

                                            context.getSource().sendSuccess(
                                                    () -> net.minecraft.network.chat.Component.literal(info.toString()), false
                                            );
                                            return 1;
                                        })
                                )
                        )
        );
    }
}