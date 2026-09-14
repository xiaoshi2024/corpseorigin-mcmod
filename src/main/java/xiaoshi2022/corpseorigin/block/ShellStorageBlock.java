package xiaoshi2022.corpseorigin.block;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.block.entity.ShellStorageBlockEntity;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.shell.ServerShell;
import xiaoshi2022.corpseorigin.shell.ShellState;

public class ShellStorageBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ShellStorageBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public static final MapCodec<ShellStorageBlock> CODEC = simpleCodec(ShellStorageBlock::new);

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShellStorageBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof ShellStorageBlockEntity storage)) {
            return InteractionResult.PASS;
        }

        // ★ 潜行右键 + 有身体 → 完全转移
        if (player.isSecondaryUseActive() && !storage.isEmpty()) {
            ServerShell shell = ServerShell.of(serverPlayer);
            Either<ShellState, String> result = shell.sync(storage);
            result.ifLeft(stored -> {
                CorpseNetwork.refreshShellStates(serverPlayer);
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.corpseorigin.shell_storage.transferred"),
                        true);
            });
            return InteractionResult.SUCCESS;
        }

        // ★ 普通右键 + 空仓 → 培育一具空壳
        if (storage.isEmpty()) {
            ShellState blank = ShellState.blank(serverPlayer, pos);
            storage.setStoredState(blank);
            CorpseNetwork.refreshShellStates(serverPlayer);
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.corpseorigin.shell_storage.stored"),
                    true);
            return InteractionResult.SUCCESS;
        }

        // 普通右键 + 有身体 → 提示
        serverPlayer.sendSystemMessage(
                Component.translatable("message.corpseorigin.shell_storage.occupied"),
                true);
        return InteractionResult.SUCCESS;
    }
}