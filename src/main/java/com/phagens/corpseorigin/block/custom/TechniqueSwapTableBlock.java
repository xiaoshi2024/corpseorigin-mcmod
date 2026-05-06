package com.phagens.corpseorigin.block.custom;

import com.mojang.serialization.MapCodec;
import com.phagens.corpseorigin.block.entity.TechniqueSwapTableEntity;
import com.phagens.corpseorigin.client.gui.TechniqueSwapTable.TechniqueSwapTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TechniqueSwapTableBlock extends BaseEntityBlock {

    public static final MapCodec<TechniqueSwapTableBlock> CODEC = simpleCodec(TechniqueSwapTableBlock::new);

    public TechniqueSwapTableBlock(Properties properties) {
        super(Properties.of().strength(2.5F, 5.0F).sound(SoundType.WOOD).noOcclusion());
    }
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TechniqueSwapTableEntity(pos, state);
    }
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    /**
     * 方块被移除时的处理
     * 当方块被破坏或替换时，将容器内的物品掉落到世界中
     * 注意：只处理真正的移除情况（新旧方块不同）
     *
     * @param state 旧方块状态
     * @param level 世界实例
     * @param pos 方块位置
     * @param newState 新方块状态
     * @param movedByPiston 是否由活塞推动
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TechniqueSwapTableEntity tableEntity) {
                tableEntity.dropItems();
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // 只在服务端打开菜单，客户端会自动同步
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TechniqueSwapTableEntity tableEntity) {
                MenuProvider menuProvider = this.createMenuProvider(level, pos);
                if (menuProvider != null && player instanceof ServerPlayer serverPlayer) {
                    // 打开GUI界面
                    serverPlayer.openMenu(menuProvider);
                }
            }
        }
        // 返回成功结果，sidedSuccess 表示根据环境返回不同的结果
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    //菜单提供
    public MenuProvider createMenuProvider(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TechniqueSwapTableEntity tableEntity) {
            return new SimpleMenuProvider(
                    (containerId, inventory, player) ->
                            new TechniqueSwapTableMenu(containerId, inventory, tableEntity),
                    tableEntity.getDisplayName()
            );
        }
        return null;
    }
}
