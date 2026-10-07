package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.registry.ModSpawns;

/**
 * {@code /corpseconfig}：实时操作 {@code config/corpseorigin.json}。
 * <ul>
 *   <li>{@code /corpseconfig reload} —— 改完配置文件后跑这条，下个生成判定就生效，不用重启服务器。</li>
 *   <li>{@code /corpseconfig zombies on|off} —— 直接开关原版僵尸生成（自然生成 + 刷怪笼，同时写回文件）。</li>
 *   <li>{@code /corpseconfig} —— 报告当前状态。</li>
 * </ul>
 * 权限 2 级（GAMEMASTERS）。
 */
public final class CorpseConfigCommand {
    private static final Permission OP_PERMISSION = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private CorpseConfigCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("corpseconfig")
                .requires(source -> source.permissions().hasPermission(OP_PERMISSION))
                .executes(context -> report(context.getSource()))
                .then(Commands.literal("reload").executes(context -> reload(context.getSource())))
                .then(Commands.literal("zombies")
                        .executes(context -> reportZombies(context.getSource()))
                        .then(Commands.literal("on").executes(context -> setZombies(context.getSource(), false)))
                        .then(Commands.literal("off").executes(context -> setZombies(context.getSource(), true)))));
    }

    private static int report(CommandSourceStack source) {
        boolean disabled = CorpseConfig.get().spawn.disableVanillaZombieSpawns;
        source.sendSuccess(() -> Component.translatable(
                "commands.corpseorigin.config.zombies." + (disabled ? "disabled" : "enabled")), false);
        return disabled ? 0 : 1;
    }

    private static int reportZombies(CommandSourceStack source) {
        boolean disabled = CorpseConfig.get().spawn.disableVanillaZombieSpawns;
        source.sendSuccess(() -> Component.translatable(
                "commands.corpseorigin.config.zombies." + (disabled ? "disabled" : "enabled")), false);
        return disabled ? 0 : 1;
    }

    /**
     * @param disable {@code true}=禁用原版僵尸生成（自然生成 + 刷怪笼）；{@code false}=放行
     */
    private static int setZombies(CommandSourceStack source, boolean disable) {
        CorpseConfig.get().spawn.disableVanillaZombieSpawns = disable;
        CorpseConfig.save();
        source.sendSuccess(() -> Component.translatable(
                "commands.corpseorigin.config.zombies." + (disable ? "disabled" : "enabled")), true);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        CorpseConfig.reload();
        ModSpawns.refreshAfterReload();
        source.sendSuccess(() -> Component.translatable("commands.corpseorigin.config.reloaded"), true);
        return 1;
    }
}
