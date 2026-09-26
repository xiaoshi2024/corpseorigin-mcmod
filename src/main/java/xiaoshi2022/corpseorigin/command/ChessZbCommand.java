package xiaoshi2022.corpseorigin.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.block.entity.CNChessZbrsBlockEntity;

/**
 * 象棋尸兄指令（玩家 / 命令方块 / 命令矿车均可用）。
 * <ul>
 *   <li>{@code /chesszb execute}：玩家处决视线所指、否则脚下踩着的象棋尸兄；
 *       命令方块处决其正下方的方块；</li>
 *   <li>{@code /chesszb execute <坐标>}：处决指定位置（支持 ~ 相对坐标）；</li>
 *   <li>{@code /chesszb move <方向> [格数]}：让目标象棋尸兄走子
 *       （up/down/north/south/east/west，默认 1 格，最多 16 格）。</li>
 * </ul>
 * 处决走 {@code put_death}，和空手右键完全同一路径：尖刺把背上生物夹碎。
 */
public class ChessZbCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> move = Commands.literal("move");
        for (Direction direction : Direction.values()) {
            move.then(Commands.literal(direction.getSerializedName())
                    .executes(context -> doMove(context, direction, 1))
                    .then(Commands.argument("steps", IntegerArgumentType.integer(1, 16))
                            .executes(context -> doMove(context, direction,
                                    IntegerArgumentType.getInteger(context, "steps")))));
        }

        dispatcher.register(
                Commands.literal("chesszb")
                        .then(Commands.literal("execute")
                                .executes(ChessZbCommand::executeLookedAt)
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ChessZbCommand::executeAt)))
                        .then(move)
        );
    }

    // ==================== 处决 ====================

    private static int executeLookedAt(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        return execute(source.getLevel(), resolveTarget(source), source);
    }

    private static int executeAt(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        return execute(level, pos, source);
    }

    private static int execute(ServerLevel level, BlockPos pos, CommandSourceStack source) {
        if (!(level.getBlockEntity(pos) instanceof CNChessZbrsBlockEntity zbrs)) {
            source.sendFailure(Component.translatable("command.corpseorigin.chesszb.not_found",
                    pos.getX(), pos.getY(), pos.getZ()));
            return 0;
        }

        // 和空手右键完全一致：处决动画 + 音效；夹碎伤害立刻结算
        zbrs.playPutDeath();
        level.playSound(null, pos, SoundEvents.WARDEN_ROAR, SoundSource.BLOCKS, 1.0F, 0.6F);

        source.sendSuccess(() -> Component.translatable("command.corpseorigin.chesszb.executed",
                pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    // ==================== 走子 ====================

    private static int doMove(CommandContext<CommandSourceStack> context,
                               Direction direction, int steps) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos piecePos = resolveTarget(source);

        if (!(level.getBlockEntity(piecePos) instanceof CNChessZbrsBlockEntity zbrs)) {
            source.sendFailure(Component.translatable("command.corpseorigin.chesszb.not_found",
                    piecePos.getX(), piecePos.getY(), piecePos.getZ()));
            return 0;
        }

        BlockPos destination = piecePos.relative(direction, steps);
        if (!zbrs.movePiece(level, destination)) {
            source.sendFailure(Component.translatable("command.corpseorigin.chesszb.move_blocked"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.corpseorigin.chesszb.moved",
                direction.getSerializedName(), steps), true);
        return 1;
    }

    // ==================== 目标定位 ====================

    /**
     * 找到指令作用的象棋尸兄位置：
     * 玩家 → 视线所指（20 格内），没看着就取脚下；
     * 命令方块等非玩家来源 → 来源位置正下方。
     */
    private static BlockPos resolveTarget(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            BlockPos looked = rayTraceBlock(source.getLevel(), player, 20.0);
            return looked != null ? looked : player.blockPosition().below();
        }
        return BlockPos.containing(source.getPosition()).below();
    }

    /** 视线检测：只返回看到的方块位置（之后再判断是不是象棋尸兄）。 */
    private static BlockPos rayTraceBlock(ServerLevel level, ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.x * reach, look.y * reach, look.z * reach);
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }
        return null;
    }
}
