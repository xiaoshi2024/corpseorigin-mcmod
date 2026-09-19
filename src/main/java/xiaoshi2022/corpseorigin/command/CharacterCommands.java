package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.skill.SkillManager;

import java.util.Collection;

/**
 * 角色命令 /character current|list|select|clear|unlockall
 */
public final class CharacterCommands {

    private CharacterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("character")
                // 直接 /character 等同于查看当前角色
                .executes(ctx -> showCurrent(ctx.getSource()))
                .then(Commands.literal("current").executes(ctx -> showCurrent(ctx.getSource())))
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
                }))
                // ✅ 作弊：一键解锁全部技能（默认作用于所有在线玩家）
                .then(Commands.literal("unlockall")
                        .executes(ctx -> unlockAll(ctx.getSource(),
                                ctx.getSource().getServer().getPlayerList().getPlayers()))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> unlockAll(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets")))))
                // ✅ 左护法蛟龙的多段碰撞箱：现场微调（改的是内存里的值，调好请抄回配置文件）
                .then(Commands.literal("hitbox")
                        .executes(ctx -> listHitboxes(ctx.getSource()))
                        .then(Commands.argument("segment", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (var segment : hitboxSegments()) {
                                        builder.suggest(segment.name);
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("field", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String field : HITBOX_FIELDS) {
                                                builder.suggest(field);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(-32.0F, 32.0F))
                                                .executes(ctx -> setHitbox(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "segment"),
                                                        StringArgumentType.getString(ctx, "field"),
                                                        FloatArgumentType.getFloat(ctx, "value"))))))));
    }

    // ==================== 蛟龙节碰撞箱微调 ====================

    private static final String[] HITBOX_FIELDS = {"forward", "up", "right", "width", "height"};

    private static java.util.List<xiaoshi2022.corpseorigin.config.CorpseConfig.MutantBody.Hitboxes.Segment>
    hitboxSegments() {
        return xiaoshi2022.corpseorigin.config.CorpseConfig.get().mutantBody.hitboxes.segments;
    }

    /** 列出每一节当前的偏移与尺寸 —— 开 F3+B 对着箱子调 */
    private static int listHitboxes(CommandSourceStack source) {
        var segments = hitboxSegments();
        if (segments.isEmpty()) {
            source.sendFailure(Component.literal("配置里没有任何一节碰撞箱（mutantBody.hitboxes.segments 是空的）"));
            return 0;
        }
        StringBuilder text = new StringBuilder("===== 蛟龙节碰撞箱（相对玩家、按朝向旋转；up 是箱子底面高度）=====");
        for (var segment : segments) {
            text.append('\n').append(describeHitbox(segment));
        }
        text.append("\n用 /character hitbox <节名> <字段> <数值> 微调（下一 tick 生效）；")
                .append("调好后把数值抄进 config/corpseorigin.json —— 命令改的是内存，重启会丢。")
                .append("想看箱子按 F3+B。");
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return 1;
    }

    private static int setHitbox(CommandSourceStack source, String name, String field, float value) {
        var target = hitboxSegments().stream()
                .filter(segment -> segment.name.equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (target == null) {
            source.sendFailure(Component.literal("没有叫 " + name + " 的这一节（用 /character hitbox 看列表）"));
            return 0;
        }

        switch (field.toLowerCase(java.util.Locale.ROOT)) {
            case "forward" -> target.forward = value;
            case "up" -> target.up = value;
            case "right" -> target.right = value;
            // 尺寸给正数：0 会让箱子退化成一条线
            case "width" -> target.width = Math.max(0.1F, value);
            case "height" -> target.height = Math.max(0.1F, value);
            default -> {
                source.sendFailure(Component.literal(
                        "字段只能是 " + String.join(" / ", HITBOX_FIELDS) + "（当前填的是 " + field + "）"));
                return 0;
            }
        }

        source.sendSuccess(() -> Component.literal("已更新：" + describeHitbox(target)), true);
        return 1;
    }

    private static String describeHitbox(xiaoshi2022.corpseorigin.config.CorpseConfig.MutantBody.Hitboxes.Segment s) {
        return String.format(java.util.Locale.ROOT, "%s: forward=%.2f up=%.2f right=%.2f 尺寸=%.2f×%.2f",
                s.name, s.forward, s.up, s.right, s.width, s.height);
    }

    /** 为目标玩家解锁其当前角色的全部技能（跳过进化点与前置） */
    private static int unlockAll(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int granted = 0;
        for (ServerPlayer target : targets) {
            granted += SkillManager.grantAllSkills(target);
        }

        final int total = granted;
        final int playerCount = targets.size();
        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.character.unlock_all", playerCount, total), true);
        return total;
    }

    /** 查看当前角色（没有显式选择时就是默认的凡人） */
    private static int showCurrent(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.character.current",
                character.getName(), character.getId()), false);
        return 1;
    }
}