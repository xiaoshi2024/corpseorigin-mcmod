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
import xiaoshi2022.corpseorigin.registry.ModEntities;
import xiaoshi2022.corpseorigin.skin.LocalSkinNames;

import java.util.List;

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

        // 一键召唤一群漫展尸兄：皮肤来自 config/corpseorigin/skins/<manzhan.folder>/*.png（文件名=皮肤名）
        dispatcher.register(
                Commands.literal("summonmanzhan")
                        .requires(source -> source.permissions().hasPermission(OP_PERMISSION))
                        .executes(context -> summonManzhan(context,
                                defaultCount(), defaultRadius()))
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

    private static int defaultCount() {
        return xiaoshi2022.corpseorigin.config.CorpseConfig.get().spawn.manzhan.count;
    }

    private static int defaultRadius() {
        return xiaoshi2022.corpseorigin.config.CorpseConfig.get().spawn.manzhan.maxRadius;
    }

    /**
     * 漫展尸兄群：从本地皮肤名单里随机抽名字，在指令源周围随机散布召唤。
     * 皮肤由客户端 {@code LocalSkinStore} 从本地 PNG 加载（每端都需要放同一份皮肤文件）。
     */
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
            // 圆盘内均匀散布：sqrt 保证不扎堆圆心
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

    /**
     * 不带参数：随机分配一个"玩家 ID"。
     * <p>
     * ⚠️ 这里必须<b>显式</b>分配名字 —— 随机 ID 的自动分配挂在 {@code finalizeSpawn} 上，
     * 而且只认"自然生成"（避免覆盖 {@code /summon} 里手写的名字），
     * 而本指令走的是 {@code addFreshEntity}，压根不经过 {@code finalizeSpawn}。
     */
    private static int summonZbRandom(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        String skinName = ZbNameGenerator.random(level.getRandom());

        LowerLevelZbEntity zb = new LowerLevelZbEntity(ModEntities.LOWER_LEVEL_ZB, level);
        zb.setPlayerSkinName(skinName);
        zb.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(zb);

        source.sendSuccess(() -> Component.translatable("message.corpseorigin.summon_zb_command.text_01", skinName), true);
        CorpseOrigin.LOGGER.info("管理员 {} 召唤了尸兄（随机皮肤: {}）", source.getTextName(), skinName);

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

        source.sendSuccess(() -> Component.translatable("message.corpseorigin.summon_zb_command.text_02", playerName), true);
        CorpseOrigin.LOGGER.info("管理员 {} 召唤了尸兄，使用玩家 {} 的皮肤", source.getTextName(), playerName);

        return 1;
    }
}