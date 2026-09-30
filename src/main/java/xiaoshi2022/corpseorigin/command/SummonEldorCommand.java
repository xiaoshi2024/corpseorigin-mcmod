package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.EldorKingZbrEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/**
 * {@code /summoneldor} —— 管理员指令召唤尸兄·尔多兽王 BOSS。
 * <p>
 * 为什么不挂刷怪蛋 / 不进自然生成表：600 血的 BOSS 玩家随手放出来 / 野外随机刷到都会很离谱，
 * 正经 ARPG 风格 BOSS 该走剧情触发或玩家自己显式召唤。这个指令就是"显式召唤"的入口，
 * 供剧情 / 自定义 BOSS 战地图 / 测试用。
 * <p>
 * 权限：GAMEMASTERS（OP 等级 2）—— 与 {@link SummonZbCommand} 一致。
 */
public class SummonEldorCommand {

    private static final Permission OP_PERMISSION =
            new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("summoneldor")
                        .requires(source -> source.permissions().hasPermission(OP_PERMISSION))
                        .executes(SummonEldorCommand::summonEldor)
        );
    }

    /**
     * 在执行者脚下召唤尸兄·尔多兽王 BOSS。
     * <p>
     * 不走 {@link net.minecraft.world.entity.EntitySpawnReason#NATURAL}，
     * 因此不会触发 {@code corpseorigin$zbSpawnRules} 那条"必须在尸水泉边上"的判定 ——
     * 想在哪刷就在哪刷。
     */
    private static int summonEldor(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        EldorKingZbrEntity boss = new EldorKingZbrEntity(ModEntities.ELDOR_KING_ZBR, level);
        boss.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(boss);

        source.sendSuccess(() -> Component.translatable(
                "message.corpseorigin.summon_eldor.text_01"), true);
        CorpseOrigin.LOGGER.info("管理员 {} 召唤了尸兄·尔多兽王（位置: {} {} {}）",
                source.getTextName(), pos.x, pos.y, pos.z);

        return 1;
    }
}
