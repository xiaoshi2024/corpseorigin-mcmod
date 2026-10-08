package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.entity.ZbNameGenerator;
import xiaoshi2022.corpseorigin.entity.evolution.ZbOrganGrowth;
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skin.LocalSkinNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 尸兄召唤指令：灵活组合式。
 * <p>
 * 用户可以任意顺序、任意组合地传参，支持：
 * <pre>
 * /summonzb                                    -- 随机皮肤 + 随机等级(1-3)
 * /summonzb UFO_by_shi                         -- 指定皮肤
 * /summonzb level 5                            -- 指定进化等级（1-10）
 * /summonzb level 5 UFO_by_shi                 -- 指定等级 + 皮肤（关键词可在任意位置）
 * /summonzb level 5 variant corpseEye 1        -- 指定变种标记
 * /summonzb variant cracked 1 headless 1       -- 多个变种标记连续写
 * /summonzb level 5 organ bat_black neck        -- 指定器官（关节可选，默认自动）
 * /summonzb level 5 variant corpseEye 1 organ aquatic_tail body UFO_by_shi
 * </pre>
 * <p>
 * <b>变种标记</b>（布尔开关，写 1/0 或 true/false）：
 * <ul>
 *   <li>{@code corpseEye} —— 显示尸眼骨骼（感染视觉标识）</li>
 *   <li>{@code cracked} —— 龟裂外观（临界突破后常见）</li>
 *   <li>{@code headless} —— 无头（低阶尸兄被斧砍头后假死复活）</li>
 *   <li>{@code feigning} —— 假死（4-8秒后复活为裂口尸兄）</li>
 * </ul>
 * <b>器官</b>：走 {@link ZbOrganGrowth#grantOrgan} 注册路径，支持海陆空/翅膀/水肺/自定义全身替换。
 * 关节（可选）：body / head / left_arm / right_arm / left_leg / right_leg / full_body。
 * <p>
 * 实现原理：用 {@code greedyString()} 吞掉所有参数，然后按关键词手动拆分解析，
 * 第一个非关键词 token 自动识别为皮肤名，其余按顺序匹配。
 */
public class SummonZbCommand {

    private static final Permission OP_PERMISSION = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    /** 合法变种标记 */
    private static final Set<String> VARIANTS = Set.of("corpseEye", "cracked", "headless", "feigning");

    /** 合法器官关节 */
    private static final Set<String> JOINTS = Set.of(
            "body", "head", "left_arm", "right_arm", "left_leg", "right_leg", "full_body");

    /** 参数解析里识别的关键词 */
    private static final Set<String> KEYWORDS = Set.of("level", "variant", "organ");

    /** 默认等级：随机 1-3（对应"服务器刚开时的尸兄"，避免新手一来就被 5 级以上秒） */
    private static final int DEFAULT_LEVEL_MIN = 1;
    private static final int DEFAULT_LEVEL_MAX = 3;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 主指令：所有参数走 greedyString，在 Java 里手动解析
        dispatcher.register(
                Commands.literal("summonzb")
                        .requires(source -> source.permissions().hasPermission(OP_PERMISSION))
                        .executes(context -> summonZb(context, ""))
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                .executes(context -> summonZb(context,
                                        StringArgumentType.getString(context, "args")))
                        )
        );

        // 保留旧的 /summonmanzhan
        dispatcher.register(
                Commands.literal("summonmanzhan")
                        .requires(source -> source.permissions().hasPermission(OP_PERMISSION))
                        .executes(context -> summonManzhan(context, defaultCount(), defaultRadius()))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 128))
                                .executes(context -> summonManzhan(context,
                                        IntegerArgumentType.getInteger(context, "count"),
                                        defaultRadius()))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> summonManzhan(context,
                                                IntegerArgumentType.getInteger(context, "count"),
                                                IntegerArgumentType.getInteger(context, "radius"))))
                        )
        );
    }

    // ==================== 参数解析 ====================

    private static int summonZb(CommandContext<CommandSourceStack> context, String rawArgs) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        // 1. 解析参数
        ParsedArgs args = parseArgs(rawArgs, level);

        // 2. 构造实体
        LowerLevelZbEntity zb = new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB, level);
        zb.setPos(pos.x, pos.y, pos.z);

        // 皮肤
        String skinName;
        if (args.playerName != null && !args.playerName.isEmpty()) {
            skinName = args.playerName;
            zb.setPlayerSkinName(skinName);
            zb.setCustomId(skinName);
        } else {
            skinName = ZbNameGenerator.random(level.getRandom());
            zb.setPlayerSkinName(skinName);
        }

        // 等级
        zb.setEvolutionLevel(args.level);

        // 变种标记（booleans）
        if (args.variants.containsKey("corpseEye")) zb.setCorpseEye(args.variants.get("corpseEye"));
        if (args.variants.containsKey("cracked")) zb.setCracked(args.variants.get("cracked"));
        if (args.variants.containsKey("headless")) zb.setHeadless(args.variants.get("headless"));
        if (args.variants.containsKey("feigning")) zb.setFeigning(args.variants.get("feigning"));

        level.addFreshEntity(zb);

        // 器官（必须在 addFreshEntity 之后，grantOrgan 内部要读器官库）
        if (args.organId != null && !args.organId.isEmpty()) {
            boolean ok = ZbOrganGrowth.grantOrgan(zb, args.organId, args.joint);
            if (!ok) {
                source.sendFailure(Component.translatable(
                        "message.corpseorigin.zborgans.invalid_organ", args.organId));
            }
        }

        // 反馈
        String summary = summarize(args, skinName);
        source.sendSuccess(() -> Component.translatable(
                "message.corpseorigin.summon_zb_command.text_03", args.level, summary), true);
        CorpseOrigin.LOGGER.info("管理员 {} 召唤尸兄：{}", source.getTextName(), summary);

        return 1;
    }

    /**
     * 把 greedyString 拆成 token，按关键词解析。
     * <p>
     * 解析规则：
     * <ul>
     *   <li>第一个非关键词 token → 皮肤名；</li>
     *   <li>{@code level N} → 等级；</li>
     *   <li>{@code variant NAME [0|1|true|false]} → 变种标记（变种名是布尔开关，值默认 true）；</li>
     *   <li>{@code organ ORGAN_ID [joint]} → 器官（关节可选）。</li>
     * </ul>
     */
    private static ParsedArgs parseArgs(String raw, ServerLevel level) {
        ParsedArgs args = new ParsedArgs();
        args.level = DEFAULT_LEVEL_MIN + level.getRandom().nextInt(DEFAULT_LEVEL_MAX - DEFAULT_LEVEL_MIN + 1);

        if (raw == null || raw.isBlank()) return args;

        List<String> tokens = new ArrayList<>(Arrays.asList(raw.trim().split("\\s+")));
        int i = 0;

        // 先扫一遍：找出皮肤名（第一个非关键词）
        boolean skinFound = false;
        while (i < tokens.size()) {
            String t = tokens.get(i);
            if (KEYWORDS.contains(t)) break;  // 遇到关键词停止
            if (!skinFound && t.matches("[A-Za-z0-9_]{1,16}")) {
                // 符合玩家 ID 格式的 token 当皮肤名（长度 1-16，字母数字下划线）
                args.playerName = t;
                skinFound = true;
                i++;
            } else {
                break;  // 未知前缀，不强行消费
            }
        }

        // 然后按关键词解析
        while (i < tokens.size()) {
            String kw = tokens.get(i);

            switch (kw) {
                case "level" -> {
                    i++;
                    if (i < tokens.size()) {
                        try {
                            int lv = Integer.parseInt(tokens.get(i));
                            args.level = Math.max(1, Math.min(10, lv));
                        } catch (NumberFormatException ignored) {
                        }
                        i++;
                    }
                }
                case "variant" -> {
                    i++;
                    // 可以连续写多个 variant NAME VALUE 对
                    while (i < tokens.size() && VARIANTS.contains(tokens.get(i))) {
                        String vName = tokens.get(i);
                        boolean vVal = true;  // 默认 true
                        i++;
                        if (i < tokens.size() && isBoolLike(tokens.get(i))) {
                            vVal = parseBool(tokens.get(i));
                            i++;
                        }
                        args.variants.put(vName, vVal);
                    }
                }
                case "organ" -> {
                    i++;
                    if (i < tokens.size()) {
                        args.organId = tokens.get(i);
                        i++;
                        // 下一个 token 如果是合法关节就当关节，否则忽略
                        if (i < tokens.size() && JOINTS.contains(tokens.get(i))) {
                            args.joint = tokens.get(i);
                            i++;
                        }
                    }
                }
                default -> {
                    // 未知 token，跳过
                    i++;
                }
            }
        }
        return args;
    }

    private static boolean isBoolLike(String s) {
        return s.equals("0") || s.equals("1") || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false");
    }

    private static boolean parseBool(String s) {
        return s.equals("1") || s.equalsIgnoreCase("true");
    }

    private static String summarize(ParsedArgs args, String skinName) {
        StringBuilder sb = new StringBuilder("level=").append(args.level);
        sb.append(", skin=").append(skinName);
        if (!args.variants.isEmpty()) {
            sb.append(", variant=[");
            args.variants.forEach((k, v) -> sb.append(k).append('=').append(v).append(','));
            sb.setLength(sb.length() - 1);
            sb.append(']');
        }
        if (args.organId != null) {
            sb.append(", organ=").append(args.organId);
            if (args.joint != null) sb.append('@').append(args.joint);
        }
        return sb.toString();
    }

    // ==================== 内部类 ====================

    private static final class ParsedArgs {
        String playerName;
        int level;
        final java.util.Map<String, Boolean> variants = new java.util.HashMap<>();
        String organId;
        String joint;
    }

    // ==================== 漫展群召（不变） ====================

    private static int defaultCount() {
        return xiaoshi2022.corpseorigin.config.CorpseConfig.get().spawn.manzhan.count;
    }
    private static int defaultRadius() {
        return xiaoshi2022.corpseorigin.config.CorpseConfig.get().spawn.manzhan.maxRadius;
    }

    private static int summonManzhan(CommandContext<CommandSourceStack> context, int count, int radius) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        List<String> names = LocalSkinNames.listNames();
        if (names.isEmpty()) {
            source.sendFailure(Component.translatable("message.corpseorigin.summonmanzhan.empty",
                    LocalSkinNames.folder().toAbsolutePath().toString()));
            return 0;
        }

        var random = level.getRandom();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double x = pos.x + Math.cos(angle) * dist;
            double z = pos.z + Math.sin(angle) * dist;
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);

            LowerLevelZbEntity zb = new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB, level);
            String skinName = names.get(random.nextInt(names.size()));
            zb.setPlayerSkinName(skinName);
            zb.setCustomId(skinName);
            zb.setPos(x, y, z);
            level.addFreshEntity(zb);
            spawned++;
        }

        int finalSpawned = spawned;
        source.sendSuccess(() -> Component.translatable(
                "message.corpseorigin.summonmanzhan.done", finalSpawned, names.size()), true);
        CorpseOrigin.LOGGER.info("管理员 {} 一键召唤了 {} 只漫展尸兄（本地皮肤名单 {} 个，半径 {}）",
                source.getTextName(), spawned, names.size(), radius);
        return spawned;
    }
}
