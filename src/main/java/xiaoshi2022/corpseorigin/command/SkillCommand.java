package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.component.PlayerRelicComponent;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockManager;
import xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockSource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 技能 / 获取物指令。
 *
 * <pre>
 * /corpseskill                        查看自己的技能与解锁状态
 * /corpseskill list [玩家]            同上，可指定玩家
 * /corpseskill all                    列出所有已注册角色及其全部技能
 * /corpseskill unlock &lt;玩家&gt; &lt;技能&gt;  强制学会（跳过一切条件）
 * /corpseskill relic &lt;玩家&gt; list                查看已获得的器官 / 收藏品
 * /corpseskill relic &lt;玩家&gt; add &lt;器官&gt;        授予器官（会立刻触发获取式解锁）
 * /corpseskill relic &lt;玩家&gt; remove &lt;器官&gt;     收回器官
 * </pre>
 */
public final class SkillCommand {

    private SkillCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("corpseskill")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> listSkills(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("list")
                        .executes(ctx -> listSkills(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> listSkills(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("all")
                        .executes(ctx -> listAllCharacters(ctx.getSource())))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            for (ISkill skill : CharacterManager.getInstance()
                                                    .getPlayerCharacter(target).getSkills()) {
                                                builder.suggest(skill.getId().getPath());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            String path = StringArgumentType.getString(ctx, "skill");
                                            // 作弊解锁：跳过进化点、等级与前置
                                            PlayerCharacterData data = PlayerCharacterData.get(target);
                                            data.learnSkill(target.getUUID(), path);
                                            xiaoshi2022.corpseorigin.network.CorpseNetwork.sendEvolutionSync(target);
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "command.corpseorigin.skill.unlocked",
                                                    target.getName(), path), true);
                                            return 1;
                                        }))))
                .then(Commands.literal("relic")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("list").executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    java.util.Set<String> relics = PlayerRelicComponent.all(target);
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            "command.corpseorigin.relic.list",
                                            target.getName(),
                                            relics.isEmpty() ? "-" : String.join(", ", relics)), false);
                                    return relics.size();
                                }))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("relic", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    String relicId = StringArgumentType.getString(ctx, "relic");
                                                    boolean added = PlayerRelicComponent.grant(target, relicId);
                                                    // 授予后立刻判定，玩家不用等下一个扫描周期
                                                    int unlocked = SkillUnlockManager.grantUnlocked(target, false);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                                            "command.corpseorigin.relic.added",
                                                            target.getName(), relicId, unlocked), true);
                                                    return added ? 1 : 0;
                                                })))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("relic", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    String relicId = StringArgumentType.getString(ctx, "relic");
                                                    boolean removed = PlayerRelicComponent.revoke(target, relicId);
                                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                                            "command.corpseorigin.relic.removed",
                                                            relicId, target.getName()), true);
                                                    return removed ? 1 : 0;
                                                })))))
        );
    }

    // ==================== 输出 ====================

    /** 某个玩家的当前角色技能清单 + 解锁状态 */
    private static int listSkills(CommandSourceStack source, ServerPlayer target) {
        ICharacter character = CharacterManager.getInstance().getPlayerCharacter(target);
        PlayerCharacterData data = PlayerCharacterData.get(target);
        List<ISkill> skills = character.getSkills();

        int learned = 0;
        MutableComponent out = Component.literal("===== ")
                .append(target.getName())
                .append(Component.literal(" / "))
                .append(character.getName())
                .append(Component.translatable("message.corpseorigin.skill_command.text_01"));

        for (ISkill skill : skills) {
            if (data.hasLearned(target.getUUID(), skill.getId().getPath())) {
                learned++;
            }
        }
        out.append(Component.literal(learned + "/" + skills.size() + ") ====="));

        for (ISkill skill : skills) {
            boolean ok = data.hasLearned(target.getUUID(), skill.getId().getPath());
            out.append(Component.literal("\n" + (ok ? "[x] " : "[ ] ")))
                    .append(skill.getName())
                    .append(Component.literal(" (" + skill.getId().getPath() + ") "));

            if (ok) {
                out.append(Component.translatable("command.corpseorigin.skill.state.learned"));
            } else {
                out.append(describeUnlock(target, skill));
            }
        }

        source.sendSuccess(() -> out, false);
        return learned;
    }

    /** 未学会时说明"怎么才能解锁"：获取式条件优先，否则退回技能树要求 */
    private static MutableComponent describeUnlock(ServerPlayer target, ISkill skill) {
        List<SkillUnlockSource> sources = skill.getUnlockSources();
        if (sources.isEmpty()) {
            return Component.translatable("command.corpseorigin.skill.state.tree",
                    skill.getCost(), skill.getRequiredLevel());
        }
        MutableComponent out = Component.translatable("command.corpseorigin.skill.state.source");
        for (SkillUnlockSource src : sources) {
            out.append(src.describe());
            out.append(Component.translatable(src.isSatisfiedBy(target)?"command.corpseorigin.skill.met":"command.corpseorigin.skill.missing"));
            out.append(Component.literal(" "));
        }
        return out;
    }

    /** 所有已注册角色及其全部技能（"查看所有角色技能"用） */
    private static int listAllCharacters(CommandSourceStack source) {
        MutableComponent out = Component.translatable("message.corpseorigin.skill_command.text_02");
        int total = 0;
        for (ICharacter character : CharacterManager.getInstance().getRegisteredCharacters()) {
            List<ISkill> skills = character.getSkills();
            total += skills.size();
            out.append(Component.literal("\n- "))
                    .append(character.getName())
                    .append(Component.literal(" (" + character.getId() + "): "))
                    .append(Component.literal(skills.stream()
                            .map(s -> s.getId().getPath())
                            .collect(Collectors.joining(", "))));
        }
        out.append(Component.translatable("message.corpseorigin.skill_command.text_03", total));
        int shown = total;
        source.sendSuccess(() -> out, false);
        return shown == 0 ? 0 : 1;
    }
}
