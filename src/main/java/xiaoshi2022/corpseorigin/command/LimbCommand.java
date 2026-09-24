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
import xiaoshi2022.corpseorigin.limb.*;

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
            source.sendFailure(Component.translatable("message.corpseorigin.limb_command.text_01", slotKey));
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            if (DismembermentLogic.forceSever(target, slot)) {
                count++;
            }
        }
        final int total = count;
        source.sendSuccess(() -> Component.translatable("message.corpseorigin.limb_command.text_02", total, LimbSlots.DISPLAY_NAMES[slot]), true);
        return total;
    }

    private static int regen(CommandSourceStack source, Collection<ServerPlayer> targets, String slotKey) {
        int slot = LimbSlots.slotFromKey(slotKey);
        if (slot < 0) {
            source.sendFailure(Component.translatable("message.corpseorigin.limb_command.text_01", slotKey));
            return 0;
        }
        int count = 0;
        for (ServerPlayer target : targets) {
            if (DismembermentLogic.forceRegrow(target, slot)) {
                count++;
            }
        }
        final int total = count;
        source.sendSuccess(() -> Component.translatable("message.corpseorigin.limb_command.text_03", total, LimbSlots.DISPLAY_NAMES[slot]), true);
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
        source.sendSuccess(() -> Component.translatable("message.corpseorigin.limb_command.text_04", total), true);
        return total;
    }

    private static int info(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            PlayerCorpseComponent comp = PlayerCorpseComponent.get(target);
            LimbState state = comp.readLimbs();

            var out=Component.translatable("command.corpseorigin.limb.info", target.getName(), CharacterManager.getInstance().getPlayerCharacterId(target), comp.isCorpse(), comp.isDisguised(), LimbAccess.canDismember(target), LimbRegenProfiles.forPlayer(target).id(), comp.getHunger());
            if(!state.hasSevered())out.append(Component.translatable("command.corpseorigin.limb.intact"));
            else for(int slot=0;slot<LimbSlots.COUNT;slot++)if(state.isSevered(slot)){
                out.append(" | ").append(LimbSlots.DISPLAY_NAMES[slot]).append(" (");
                if(state.regrowTicks()[slot]<=0)out.append(Component.translatable("command.corpseorigin.limb.permanent"));
                else out.append(Component.translatable("command.corpseorigin.limb.remaining", state.regrowTicks()[slot]/20));
                out.append(")");
            }
            source.sendSuccess(()->out,false);
        }
        return targets.size();
    }
}
