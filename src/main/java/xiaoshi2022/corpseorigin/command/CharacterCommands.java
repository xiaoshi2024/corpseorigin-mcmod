package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;

/**
 * 角色命令 /character list|select|clear
 */
public final class CharacterCommands {

    private CharacterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("character")
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
                })));
    }
}