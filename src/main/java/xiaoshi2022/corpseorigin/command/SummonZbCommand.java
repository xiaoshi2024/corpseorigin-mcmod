package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;

public class SummonZbCommand {

    // ✅ OP 权限（GAMEMASTERS 对应等级2）
    private static final Permission OP_PERMISSION = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("summonzb")
                        .requires(source -> source.permissions().hasPermission(OP_PERMISSION))
                        .executes(context -> summonZbRandom(context))
                        .then(Commands.argument("playerName", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String playerName = StringArgumentType.getString(context, "playerName");
                                    return summonZbByName(context, playerName);
                                })
                        )
        );
    }

    private static int summonZbRandom(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        LowerLevelZbEntity zb = new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB, level);
        zb.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(zb);

        source.sendSuccess(() -> Component.literal("§a成功召唤尸兄！§7(随机皮肤)"), true);
        CorpseOrigin.LOGGER.info("管理员 {} 召唤了尸兄（随机皮肤）", source.getTextName());

        return 1;
    }

    private static int summonZbByName(CommandContext<CommandSourceStack> context, String playerName) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        LowerLevelZbEntity zb = new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB, level);
        zb.setPlayerSkinName(playerName);
        zb.setCustomId(playerName);
        zb.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(zb);

        source.sendSuccess(() -> Component.literal("§a成功召唤尸兄！皮肤玩家: §e" + playerName), true);
        CorpseOrigin.LOGGER.info("管理员 {} 召唤了尸兄，使用玩家 {} 的皮肤", source.getTextName(), playerName);

        return 1;
    }
}