package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

/**
 * {@code /corpsespawn}：一键禁用/启用原版苦力怕、骷髅、末影人的生成。
 * <ul>
 *   <li>{@code /corpsespawn} —— 报告三种生物当前的生成状态；</li>
 *   <li>{@code /corpsespawn disable} —— 一键禁用三种生物的自动生成（自然生成 + 刷怪笼/试炼刷怪笼），并写回配置文件；</li>
 *   <li>{@code /corpsespawn enable} —— 一键恢复三种生物的生成，并写回配置文件。</li>
 * </ul>
 * 实时生效，不用重启服务器；刷怪蛋、{@code /summon} 等人工来源不受影响。
 * 权限 2 级（GAMEMASTERS）。
 */
public final class VanillaSpawnCommand {
    private static final Permission OP_PERMISSION = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    private VanillaSpawnCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("corpsespawn")
                .requires(source -> source.permissions().hasPermission(OP_PERMISSION))
                .executes(context -> report(context.getSource()))
                .then(Commands.literal("disable").executes(context -> setAll(context.getSource(), true)))
                .then(Commands.literal("enable").executes(context -> setAll(context.getSource(), false))));
    }

    private static int report(CommandSourceStack source) {
        var spawn = CorpseConfig.get().spawn;
        String creeper = spawn.disableVanillaCreeperSpawns ? "disabled" : "enabled";
        String skeleton = spawn.disableVanillaSkeletonSpawns ? "disabled" : "enabled";
        String enderman = spawn.disableVanillaEndermanSpawns ? "disabled" : "enabled";
        source.sendSuccess(() -> Component.translatable("commands.corpseorigin.spawn.creeper." + creeper), false);
        source.sendSuccess(() -> Component.translatable("commands.corpseorigin.spawn.skeleton." + skeleton), false);
        source.sendSuccess(() -> Component.translatable("commands.corpseorigin.spawn.enderman." + enderman), false);
        return 1;
    }

    /**
     * @param disable {@code true}=一键禁用三种生物的自动生成；{@code false}=恢复
     */
    private static int setAll(CommandSourceStack source, boolean disable) {
        var spawn = CorpseConfig.get().spawn;
        spawn.disableVanillaCreeperSpawns = disable;
        spawn.disableVanillaSkeletonSpawns = disable;
        spawn.disableVanillaEndermanSpawns = disable;
        CorpseConfig.save();
        source.sendSuccess(() -> Component.translatable(
                "commands.corpseorigin.spawn.all." + (disable ? "disabled" : "enabled")), true);
        return 1;
    }
}
