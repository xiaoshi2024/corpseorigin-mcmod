package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.limb.DismembermentLogic;
import xiaoshi2022.corpseorigin.limb.LimbAccess;
import xiaoshi2022.corpseorigin.limb.LimbRegenProfiles;
import xiaoshi2022.corpseorigin.limb.LimbSlots;
import xiaoshi2022.corpseorigin.limb.LimbState;

import java.util.Collection;

/**
 * 断肢调试命令：/corpselimb &lt;targets&gt; sever|regen &lt;部位&gt; | clear | info
 * <p>
 * 注意：这只是调试入口，直接改服务端数据、不看黑小飞 / 尸兄身份门槛；
 * 客户端渲染仍然要求这具身体 is_corpse && !disguised 才会切到断肢模型。
 */
public final class LimbCommand {

    private LimbCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("corpselimb")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.literal("sever")
                                .then(Commands.argument("slot", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String key : LimbSlots.KEYS) {
                                                builder.suggest(key);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> sever(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                StringArgumentType.getString(ctx, "slot")))))
                        .then(Commands.literal("regen")
                                .then(Commands.argument("slot", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String key : LimbSlots.KEYS) {
                                                builder.suggest(key);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> regen(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                StringArgumentType.getString(ctx, "slot")))))
                        .then(Commands.literal("clear").executes(ctx -> clear(
                                ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"))))
                        .then(Commands.literal("info").executes(ctx -> info(
                                ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"))))));
    }

    private static int sever(CommandSourceStack source, Collection<ServerPlayer> targets, String slotKey) {
        int slot = LimbSlots.slotFromKey(slotKey);
        if (slot < 0) {
            source.sendFailure(Component.literal("未知部位: " + slotKey));
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            if (DismembermentLogic.forceSever(target, slot)) {
                count++;
            }
        }
        final int total = count;
        source.sendSuccess(() -> Component.literal(
                "已截断 " + total + " 名玩家的" + LimbSlots.DISPLAY_NAMES[slot]), true);
        return total;
    }

    private static int regen(CommandSourceStack source, Collection<ServerPlayer> targets, String slotKey) {
        int slot = LimbSlots.slotFromKey(slotKey);
        if (slot < 0) {
            source.sendFailure(Component.literal("未知部位: " + slotKey));
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            if (DismembermentLogic.forceRegrow(target, slot)) {
                count++;
            }
        }
        final int total = count;
        source.sendSuccess(() -> Component.literal(
                "已让 " + total + " 名玩家的" + LimbSlots.DISPLAY_NAMES[slot] + "重新长好"), true);
        return total;
    }

    private static int clear(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int count = 0;
        for (ServerPlayer target : targets) {
            if (DismembermentLogic.clearLimbs(target)) {
                count++;
            }
        }
        final int total = count;
        source.sendSuccess(() -> Component.literal("已清空 " + total + " 名玩家的断肢状态"), true);
        return total;
    }

    private static int info(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            PlayerCorpseComponent comp = PlayerCorpseComponent.get(target);
            LimbState state = comp.readLimbs();

            StringBuilder sb = new StringBuilder(target.getName().getString());
            // 资格自查：断肢要求「角色在白名单里 + 尸兄 + 未伪装」三者同时成立
            sb.append(" 角色=").append(CharacterManager.getInstance().getPlayerCharacterId(target));
            sb.append(" 尸兄=").append(comp.isCorpse() ? "是" : "否");
            sb.append(" 伪装=").append(comp.isDisguised() ? "是" : "否");
            sb.append(" 资格=").append(LimbAccess.canDismember(target) ? "✔" : "✘");
            sb.append(" 再生=").append(LimbRegenProfiles.forPlayer(target).id());
            sb.append(" 饥饿=").append(comp.getHunger());

            if (!state.hasSevered()) {
                sb.append(" | 四肢完好");
            } else {
                sb.append(" |");
                for (int slot = 0; slot < LimbSlots.COUNT; slot++) {
                    if (!state.isSevered(slot)) {
                        continue;
                    }
                    sb.append(' ').append(LimbSlots.DISPLAY_NAMES[slot]).append('(');
                    if (state.regrowTicks()[slot] <= 0) {
                        sb.append("永久断");
                    } else {
                        sb.append("剩余 ").append(state.regrowTicks()[slot] / 20).append("s");
                    }
                    sb.append(')');
                }
            }
            source.sendSuccess(() -> Component.literal(sb.toString()), false);
        }
        return targets.size();
    }
}
