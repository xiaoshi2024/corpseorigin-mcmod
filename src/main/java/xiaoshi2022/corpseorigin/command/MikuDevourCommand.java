package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.MikuZbEntity;

public final class MikuDevourCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("miku_devour")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("eater", EntityArgument.entity())
                        .then(Commands.argument("miku", EntityArgument.entity()).executes(context -> {
                            var eater = EntityArgument.getEntity(context, "eater");
                            var victim = EntityArgument.getEntity(context, "miku");
                            if (!(eater instanceof LowerLevelZbEntity zb) || !(victim instanceof MikuZbEntity miku)
                                    || !zb.commandEatMiku(miku)) {
                                context.getSource().sendFailure(Component.literal("请选择活着、有头、可行动且尚未吞噬进化的普通尸兄，以及同维度未被吞噬的初音尸兄。"));
                                return 0;
                            }
                            context.getSource().sendSuccess(() -> Component.literal("尸兄开始追逐并啃食初音；致命一击后进入 5 秒吞噬演出。"), true);
                            return 1;
                        }))));
    }
}
