package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

import java.util.Collection;
import java.util.Collections;

/**
 * 尸眼显隐作弊指令：/corpseeye show|hide|auto [targets]
 * <p>
 * show / hide 写入手动开关（作弊覆盖，优先于境界）；auto 清除覆盖，回到生存默认 ——
 * 尸眼跟随境界（境界 ≥5 自动收起尸眼骨骼，见 {@code PlayerCorpseComponent#syncEvolvedEye}）。
 * <p>
 * 仅对「会长尸眼骨骼」的尸兄生效：非尸兄、或走自带外观变种（天线宝宝 / 左护法 /
 * 开胃奶 / 尸巢之子）的目标本来就不渲染尸眼，会被跳过。
 */
public final class CorpseEyeCommand {

    private CorpseEyeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("corpseeye")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(mode("show", PlayerCorpseComponent.EYE_PREF_SHOW))
                .then(mode("hide", PlayerCorpseComponent.EYE_PREF_HIDE))
                .then(mode("auto", PlayerCorpseComponent.EYE_PREF_AUTO)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> mode(String name, int pref) {
        return Commands.literal(name)
                // 不带 targets：操作自己
                .executes(ctx -> set(ctx.getSource(),
                        Collections.singleton(ctx.getSource().getPlayerOrException()), pref))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> set(ctx.getSource(),
                                EntityArgument.getPlayers(ctx, "targets"), pref)));
    }

    private static int set(CommandSourceStack source, Collection<ServerPlayer> targets, int pref) {
        int applied = 0;
        int skipped = 0;
        for (ServerPlayer target : targets) {
            if (PlayerCorpseComponent.setEyePref(target, pref)) {
                applied++;
            } else {
                skipped++;
            }
        }
        String modeKey;
        if (pref == PlayerCorpseComponent.EYE_PREF_SHOW) {
            modeKey = "message.corpseorigin.eye.mode.show";
        } else if (pref == PlayerCorpseComponent.EYE_PREF_HIDE) {
            modeKey = "message.corpseorigin.eye.mode.hide";
        } else {
            modeKey = "message.corpseorigin.eye.mode.auto";
        }
        if (applied > 0) {
            var msg = Component.translatable("message.corpseorigin.eye.set", applied,
                    Component.translatable(modeKey));
            if (skipped > 0) {
                msg.append(Component.translatable("message.corpseorigin.eye.skipped", skipped));
            }
            source.sendSuccess(() -> msg, true);
        } else {
            source.sendFailure(Component.translatable("message.corpseorigin.eye.none"));
        }
        return applied;
    }
}
