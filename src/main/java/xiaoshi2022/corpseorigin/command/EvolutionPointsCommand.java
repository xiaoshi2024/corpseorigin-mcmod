package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.EvolutionStats;
import xiaoshi2022.corpseorigin.skill.EvolutionTier;

import java.util.UUID;

/**
 * 进化点调试指令（OP / 创造模式管理员可用，权限等级 2）。
 *
 * <pre>
 * /corpsepoints                      查看自己的进化点、可用点与当前阶层
 * /corpsepoints get [玩家]            查询指定玩家
 * /corpsepoints add &lt;数量&gt; [玩家]     增加进化点（数量可为负，即扣除；不会低于 0）
 * /corpsepoints set &lt;数量&gt; [玩家]     直接设置累计进化点总数（已花掉的点数保留）
 * </pre>
 * <p>
 * 数据写在 {@link PlayerCharacterData} 里，改完立刻
 * {@link CorpseNetwork#sendEvolutionSync} 推给客户端，HUD / 技能树马上刷新。
 */
public final class EvolutionPointsCommand {

    private EvolutionPointsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("corpsepoints")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                // 不带子指令 = 查自己
                .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("get")
                        .executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(ctx -> add(ctx.getSource(),
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> add(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> set(ctx.getSource(),
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> set(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "amount")))))));
    }

    /** 查询：累计点 / 可用点 / 当前阶层 / 离下一级还差多少 */
    private static int show(CommandSourceStack source, ServerPlayer target) {
        PlayerCharacterData data = PlayerCharacterData.get(target);
        UUID uuid = target.getUUID();
        int earned = data.getEarnedPoints(uuid);
        int available = data.getAvailablePoints(uuid);
        int level = EvolutionManager.getLevel(earned);
        var tier = EvolutionTier.formatFullName(level);

        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.points.get",
                target.getName(), earned, available, tier,
                level >= EvolutionManager.MAX_LEVEL
                        ? Component.translatable("command.corpseorigin.points.max_level")
                        : Component.literal(String.valueOf(EvolutionManager.pointsToNextLevel(earned)))),
                false);
        return 1;
    }

    /** 增减点数：累计点和可用点同步变化，都不允许低于 0 */
    private static int add(CommandSourceStack source, ServerPlayer target, int delta) {
        PlayerCharacterData data = PlayerCharacterData.get(target);
        UUID uuid = target.getUUID();
        int levelBefore = EvolutionManager.getLevel(data.getEarnedPoints(uuid));
        int newEarned = Math.max(0, data.getEarnedPoints(uuid) + delta);
        int newAvailable = Math.max(0, data.getAvailablePoints(uuid) + delta);
        data.setPoints(uuid, newEarned, newAvailable);
        CorpseNetwork.sendEvolutionSync(target);
        refreshGrowth(target, levelBefore);

        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.points.added",
                target.getName(), delta, newEarned, newAvailable), true);
        return 1;
    }

    /** 直接设置累计点总数；可用点 = 旧可用点 + 差值，保证"已经花掉的点数"不被吐回来 */
    private static int set(CommandSourceStack source, ServerPlayer target, int total) {
        PlayerCharacterData data = PlayerCharacterData.get(target);
        UUID uuid = target.getUUID();
        int levelBefore = EvolutionManager.getLevel(data.getEarnedPoints(uuid));
        int oldEarned = data.getEarnedPoints(uuid);
        int newAvailable = Math.max(0, data.getAvailablePoints(uuid) + (total - oldEarned));
        data.setPoints(uuid, total, newAvailable);
        CorpseNetwork.sendEvolutionSync(target);
        refreshGrowth(target, levelBefore);

        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.points.set",
                target.getName(), total, newAvailable), true);
        return 1;
    }

    /** 点数变动后重算进化属性；真的升了级就给玩家发一条阶层提示（调试指令也走正式升级反馈） */
    private static void refreshGrowth(ServerPlayer target, int levelBefore) {
        if (EvolutionStats.reconcileAfterPointGain(target, levelBefore)) {
            int levelNow = EvolutionManager.getLevel(
                    PlayerCharacterData.get(target).getEarnedPoints(target.getUUID()));
            target.sendOverlayMessage(Component.translatable(
                    "message.corpseorigin.evolution.levelup",
                    EvolutionTier.formatFullName(levelNow)));
        }
    }
}
