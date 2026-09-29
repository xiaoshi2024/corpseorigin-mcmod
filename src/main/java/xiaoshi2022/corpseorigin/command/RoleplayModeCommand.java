package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import xiaoshi2022.corpseorigin.event.RoleplayMode;

public final class RoleplayModeCommand {
    private static final Permission OP_PERMISSION = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private RoleplayModeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("roleplaymode")
                .requires(source -> source.permissions().hasPermission(OP_PERMISSION))
                .executes(context -> report(context.getSource()))
                .then(Commands.literal("on").executes(context -> set(context.getSource(), true)))
                .then(Commands.literal("off").executes(context -> set(context.getSource(), false))));
    }

    private static int report(CommandSourceStack source) {
        boolean enabled = RoleplayMode.isEnabled(source.getServer());
        source.sendSuccess(() -> Component.translatable(enabled
                ? "message.corpseorigin.roleplay_mode.on" : "message.corpseorigin.roleplay_mode.off"), false);
        return enabled ? 1 : 0;
    }

    private static int set(CommandSourceStack source, boolean enabled) {
        RoleplayMode.setEnabled(source.getServer(), enabled);
        source.sendSuccess(() -> Component.translatable(enabled
                ? "message.corpseorigin.roleplay_mode.on" : "message.corpseorigin.roleplay_mode.off"), true);
        return 1;
    }
}
