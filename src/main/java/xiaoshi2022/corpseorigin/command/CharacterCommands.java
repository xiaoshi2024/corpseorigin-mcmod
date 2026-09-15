package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.skill.SkillManager;

import java.util.Collection;

/**
 * 角色命令 /character current|list|select|clear|unlockall
 */
public final class CharacterCommands {

    private CharacterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("character")
                // 直接 /character 等同于查看当前角色
                .executes(ctx -> showCurrent(ctx.getSource()))
                .then(Commands.literal("current").executes(ctx -> showCurrent(ctx.getSource())))
                .then(Commands.literal("list").executes(ctx -> {
                    StringBuilder list = new StringBuilder("===== ");
                    list.append(Component.translatable("command.corpseorigin.character.list_header").getString())
                            .append(" =====");
                    for (ICharacter character : CharacterManager.getInstance().getRegisteredCharacters()) {
                        list.append("\n- ").append(character.getName().getString())
                                .append(" (ID: ").append(character.getId()).append(")");
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal(list.toString()), false);
                    return 1;
                }))
                .then(Commands.literal("select")
                        .then(Commands.argument("id", StringArgumentType.word())
                                // ✅ 关键：注册 Tab 补全建议
                                .suggests((ctx, builder) -> {
                                    for (ICharacter character : CharacterManager.getInstance().getRegisteredCharacters()) {
                                        builder.suggest(character.getId());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    boolean ok = CharacterManager.getInstance().setPlayerCharacter(player, id);
                                    if (ok) {
                                        ctx.getSource().sendSuccess(() -> Component.translatable(
                                                "command.corpseorigin.character.selected",
                                                CharacterManager.getInstance().getPlayerCharacter(player).getName()), false);
                                    } else {
                                        ctx.getSource().sendFailure(Component.translatable(
                                                "command.corpseorigin.character.not_found", id));
                                    }
                                    return ok ? 1 : 0;
                                })))
                .then(Commands.literal("clear").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    CharacterManager.getInstance().clearPlayerCharacter(player);
                    ctx.getSource().sendSuccess(() ->
                            Component.translatable("command.corpseorigin.character.cleared"), false);
                    return 1;
                }))
                // ✅ 作弊：一键解锁全部技能（默认作用于所有在线玩家）
                .then(Commands.literal("unlockall")
                        .executes(ctx -> unlockAll(ctx.getSource(),
                                ctx.getSource().getServer().getPlayerList().getPlayers()))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> unlockAll(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets"))))));
    }

    /** 为目标玩家解锁其当前角色的全部技能（跳过进化点与前置） */
    private static int unlockAll(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int granted = 0;
        for (ServerPlayer target : targets) {
            granted += SkillManager.grantAllSkills(target);
        }

        final int total = granted;
        final int playerCount = targets.size();
        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.character.unlock_all", playerCount, total), true);
        return total;
    }

    /** 查看当前角色（没有显式选择时就是默认的凡人） */
    private static int showCurrent(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.character.current",
                character.getName(), character.getId()), false);
        return 1;
    }
}