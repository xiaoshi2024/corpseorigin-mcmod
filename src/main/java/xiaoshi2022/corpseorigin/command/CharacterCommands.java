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
import xiaoshi2022.corpseorigin.character.ShiChaoZhiZi;
import xiaoshi2022.corpseorigin.skill.shichaozhizi.SonOfCorpseNestSkill;

import java.util.Collection;

/**
 * 角色命令 /character current|list|select|clear|unlockall
 */
public final class CharacterCommands {

    private CharacterCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shichaoform")
                .executes(ctx -> toggleShiChaoForm(ctx.getSource())));

        dispatcher.register(Commands.literal("character")
                // 直接 /character 等同于查看当前角色
                .executes(ctx -> showCurrent(ctx.getSource()))
                .then(Commands.literal("current").executes(ctx -> showCurrent(ctx.getSource())))
                .then(Commands.literal("unlock")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        CharacterManager.getInstance().getRegisteredCharacters().stream()
                                                .flatMap(character -> character.getSkills().stream())
                                                .map(skill -> skill.getId().getPath()).distinct(), builder))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> unlockSkills(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "skill"),
                                                EntityArgument.getPlayers(ctx, "targets"))))))
                .then(Commands.literal("list").executes(ctx -> {
                    var list = Component.literal("===== ");
                    list.append(Component.translatable("command.corpseorigin.character.list_header"))
                            .append(" =====");
                    for (ICharacter character : CharacterManager.getInstance().getRegisteredCharacters()) {
                        list.append("\n- ").append(character.getName())
                                .append(" (ID: ").append(character.getId()).append(")");
                    }
                    ctx.getSource().sendSuccess(() -> list, false);
                    return 1;
                }))
                .then(Commands.literal("select")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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

    /** Operator-only unlock; validate each selected player's own character. */
    private static int unlockSkills(CommandSourceStack source, String requested, Collection<ServerPlayer> targets) {
        int granted = 0;
        for (ServerPlayer target : targets) granted += unlockSkill(source, requested, target);
        return granted;
    }

    private static int unlockSkill(CommandSourceStack source, String requested, ServerPlayer player) {
        var skill = CharacterManager.getInstance().getPlayerCharacter(player).getSkills().stream()
                .filter(s -> s.getId().getPath().equals(requested) || s.getId().toString().equals(requested))
                .findFirst().orElse(null);
        if (skill == null) {
            source.sendFailure(Component.translatable("command.corpseorigin.character.unlock_target_invalid", player.getName(), requested));
            return 0;
        }
        var data = xiaoshi2022.corpseorigin.character.PlayerCharacterData.get(player);
        String path = skill.getId().getPath();
        if (data.hasLearned(player.getUUID(), path)) {
            source.sendSuccess(() -> Component.translatable(
                    "command.corpseorigin.character.unlock_target_known", player.getName(), skill.getName()), false);
            return 0;
        }
        data.learnSkill(player.getUUID(), path);
        xiaoshi2022.corpseorigin.network.CorpseNetwork.sendEvolutionSync(player);
        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.skill.unlocked", player.getName(), path), false);
        return 1;
    }

    private static int toggleShiChaoForm(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ShiChaoZhiZi.ID.equals(
                CharacterManager.getInstance().getPlayerCharacterId(player))) {
            source.sendFailure(Component.translatable(
                    "command.corpseorigin.shichaoform.wrong_character"));
            return 0;
        }
        new SonOfCorpseNestSkill().onActivate(player);
        source.sendSuccess(() -> Component.translatable(
                "command.corpseorigin.shichaoform.toggled"), false);
        return 1;
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
            source.sendFailure(Component.translatable("message.corpseorigin.character_commands.text_01"));
            return 0;
        }
        var text=Component.translatable("command.corpseorigin.hitbox.header");
        for (var segment : segments) {
            text.append("\n").append(describeHitbox(segment));
        }
        text.append(Component.translatable("command.corpseorigin.hitbox.help"));
        source.sendSuccess(() -> text, false);
        return 1;
    }

    private static int setHitbox(CommandSourceStack source, String name, String field, float value) {
        var target = hitboxSegments().stream()
                .filter(segment -> segment.name.equalsIgnoreCase(name))
                .findFirst().orElse(null);
        if (target == null) {
            source.sendFailure(Component.translatable("message.corpseorigin.character_commands.text_02", name));
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
                source.sendFailure(Component.translatable("message.corpseorigin.character_commands.text_03", String.join(" / ", HITBOX_FIELDS), field));
                return 0;
            }
        }

        source.sendSuccess(() -> Component.translatable("message.corpseorigin.character_commands.text_04", describeHitbox(target)), true);
        return 1;
    }

    private static Component describeHitbox(xiaoshi2022.corpseorigin.config.CorpseConfig.MutantBody.Hitboxes.Segment s) {
        return Component.translatable("command.corpseorigin.hitbox.segment", s.name, String.format(java.util.Locale.ROOT,"%.2f",s.forward), String.format(java.util.Locale.ROOT,"%.2f",s.up), String.format(java.util.Locale.ROOT,"%.2f",s.right), String.format(java.util.Locale.ROOT,"%.2f",s.width), String.format(java.util.Locale.ROOT,"%.2f",s.height));
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
