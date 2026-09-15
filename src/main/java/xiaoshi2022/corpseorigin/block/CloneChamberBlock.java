package xiaoshi2022.corpseorigin.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.block.entity.CloneChamberBlockEntity;
import xiaoshi2022.corpseorigin.registry.ModBlockEntities;

import java.util.Map;
import java.util.Optional;

public class CloneChamberBlock extends BaseEntityBlock implements BucketPickup, LiquidBlockContainer {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** 仓内液体：取值来自注册表里所有"有桶"的流体，因此任意液体桶都能装进来 */
    public static final FluidKindProperty FLUID = new FluidKindProperty("fluid", FluidKind.all());
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public enum Part implements StringRepresentable {
        BODY_LOWER("body_lower"),
        BODY_UPPER("body_upper"),
        DOOR_LEFT_LOWER("door_left_lower"),
        DOOR_LEFT_UPPER("door_left_upper"),
        DOOR_RIGHT_LOWER("door_right_lower"),
        DOOR_RIGHT_UPPER("door_right_upper");

        private final String name;
        Part(String name) { this.name = name; }
        @Override public String getSerializedName() { return this.name; }
        @Override public String toString() { return this.name; }
    }

    // ==================== 碰撞形状 ====================

    /** Blockbench 导出的完整形状（面朝 NORTH，y ∈ [0, 2]） */
    private static final VoxelShape SHAPE_FULL = makeShape();

    private static final VoxelShape CLIP_LOWER = Shapes.box(0, 0, 0, 1, 1, 1);
    private static final VoxelShape CLIP_UPPER = Shapes.box(0, 1, 0, 1, 2, 1);

    /** 下半格：4 个朝向各自的形状 */
    private static final Map<Direction, VoxelShape> SHAPE_LOWER_BY_FACING;

    /** 上半格：4 个朝向各自的形状 */
    private static final Map<Direction, VoxelShape> SHAPE_UPPER_BY_FACING;

    static {
        VoxelShape baseLower = Shapes.join(SHAPE_FULL, CLIP_LOWER, BooleanOp.AND);
        VoxelShape baseUpper = Shapes.join(SHAPE_FULL, CLIP_UPPER, BooleanOp.AND).move(0, -1, 0);

        SHAPE_LOWER_BY_FACING = Shapes.rotateHorizontal(baseLower);
        SHAPE_UPPER_BY_FACING = Shapes.rotateHorizontal(baseUpper);
    }

    private static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0, 1.91875, 0.00625, 1, 1.98125, 1.00625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 1, 0.94375, 1, 2, 1.00625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.94375, 1, 0.00625, 1.00625, 2, 0.94375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 1, 0.00625, 0.0625, 2, 0.94375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0, 0.00625, 1, 0.0625, 1.00625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0, 0.94375, 1, 1, 1.00625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.94375, 0, 0.00625, 1.00625, 1, 0.94375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0, 0.00625, 0.0625, 1, 0.94375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-0.41875, 0.5625, 0.6875, 0.01875, 0.6875, 0.8125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-0.41875, 0.5625, 0.80625, -0.23125, 0.6875, 1.24375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-0.41875, 0.24375, 0.6875, 0.01875, 0.36875, 0.8125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(-0.41875, 0.24375, 0.80625, -0.29375, 0.36875, 1.30625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(1.01875, 0.24375, 0.6875, 1.45625, 0.36875, 0.8125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(1.2875, 0.24375, 0.80625, 1.475, 0.36875, 1.30625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(1, 0.65, 0.6875, 1.4375, 0.775, 0.8125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(1.26875, 0.65, 0.80625, 1.45625, 0.775, 1.30625), BooleanOp.OR);
        return shape;
    }

    private static VoxelShape getShapeFor(BlockState state) {
        Map<Direction, VoxelShape> map = isLower(state) ? SHAPE_LOWER_BY_FACING : SHAPE_UPPER_BY_FACING;
        Direction facing = state.getValue(FACING);
        // Shapes.rotateHorizontal 的旋转方向和 BER 相反：EAST/WEST 互换
        Direction lookup = switch (facing) {
            case EAST  -> Direction.WEST;
            case WEST  -> Direction.EAST;
            default    -> facing;
        };
        return map.getOrDefault(lookup, map.get(Direction.NORTH));
    }

    // ==================== 构造 ====================

    public CloneChamberBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false)
                .setValue(FLUID, FluidKind.NONE)
                .setValue(PART, Part.BODY_LOWER));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CloneChamberBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, OPEN, FLUID, PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // ==================== 碰撞/轮廓 ====================

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 轮廓/流体裁剪用整格：原版流体是按方块形状裁的，用空心框架会让水贴在仓壁里几乎看不见。
        // 碰撞形状仍然是空心（见 getCollisionShape），人能站进仓里。
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShapeFor(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        // 玻璃仓体：不遮挡光照
        return Shapes.empty();
    }

    // ==================== 液体 / 液体桶交互 ====================

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(FLUID).fluidState();
    }

    /** 空仓才能注入；能被装的流体必须是注册表里"有桶"的那种 */
    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity entity, BlockGetter level, BlockPos pos,
                                  BlockState state, Fluid fluid) {
        return state.getValue(FLUID).isEmpty() && !FluidKind.of(fluid).isEmpty();
    }

    /**
     * 手持液体桶右键注液（任意流体，判断走 {@link #canPlaceLiquid}）。
     * <p>
     * 原版桶不会给"已放置好的方块"含水，所以这里补上。
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof BucketItem bucket) {
            if (this.canPlaceLiquid(player, level, pos, state, bucket.getContent())) {
                if (!level.isClientSide()) {
                    setFluid(state, level, pos, FluidKind.of(bucket.getContent()));
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.setItemInHand(hand, BucketItem.getEmptySuccessItem(stack, player));
                }
                return InteractionResult.SUCCESS;
            }
            // 装不进去：明确告诉玩家，别让右键看起来"没反应"
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable(state.getValue(FLUID).isEmpty()
                        ? "message.corpseorigin.clone_chamber.liquid_rejected"
                        : "message.corpseorigin.clone_chamber.already_filled"));
            }
            return InteractionResult.CONSUME;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    /** 上下两半要一起换液体，避免只剩半截 */
    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        FluidKind kind = FluidKind.of(fluidState.getType());
        if (!state.getValue(FLUID).isEmpty() || kind.isEmpty()) {
            return false;
        }
        if (!level.isClientSide()) {
            setFluid(state, level, pos, kind);
        }
        return true;
    }

    /** 空桶取液：返还对应的桶 */
    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity entity, LevelAccessor level, BlockPos pos, BlockState state) {
        FluidKind kind = state.getValue(FLUID);
        if (kind.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!level.isClientSide()) {
            setFluid(state, level, pos, FluidKind.NONE);
        }
        return kind.bucketStack();
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }

    /** 液体状态上下两半同步（和 {@link #setOpen} 同样的做法） */
    public static void setFluid(BlockState state, LevelAccessor level, BlockPos pos, FluidKind kind) {
        setFluid(state, level, pos, kind, null);
    }

    /**
     * 液体状态上下两半同步，并记下"具体是哪种液体"。
     * <p>
     * 方块状态的取值表定死在本模组注册方块那一刻，注册顺序更晚的模组，它的流体存不进方块状态
     * （只能记 {@link FluidKind#OTHER}），所以具体流体交给方块实体存——桶、加速、是否尸水都靠它。
     */
    public static void setFluid(BlockState state, LevelAccessor level, BlockPos pos, FluidKind kind,
                               @Nullable Fluid fluid) {
        if (state.getValue(FLUID) != kind) {
            level.setBlock(pos, state.setValue(FLUID, kind), Block.UPDATE_ALL);
            BlockPos otherPos = isLower(state) ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(state.getBlock())) {
                level.setBlock(otherPos, otherState.setValue(FLUID, kind), Block.UPDATE_ALL);
            }
        }

        CloneChamberBlockEntity chamber = chamberAt(level, pos, state);
        if (chamber != null) {
            chamber.setStoredFluid(kind.isUnknown() ? fluid : null);
        }
    }

    /** 该位置（上/下半格）对应的方块实体（在下半格那个） */
    @Nullable
    private static CloneChamberBlockEntity chamberAt(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockPos lowerPos = isLower(state) ? pos : pos.below();
        return level.getBlockEntity(lowerPos) instanceof CloneChamberBlockEntity chamber ? chamber : null;
    }

    // ==================== 方块实体 ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CloneChamberBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (!isLower(state)) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.CLONE_CHAMBER, CloneChamberBlockEntity::tick);
    }

    // ==================== 双高方块逻辑 ====================

    public static boolean isLower(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(HALF, DoubleBlockHalf.LOWER)
                    .setValue(FLUID, FluidKind.of(level.getFluidState(pos).getType()));
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        BlockPos upperPos = pos.above();
        level.setBlock(upperPos, state.setValue(HALF, DoubleBlockHalf.UPPER)
                .setValue(FLUID, FluidKind.of(level.getFluidState(upperPos).getType())), Block.UPDATE_ALL);

        // 直接放在模组液体里时，具体液体同样要记进方块实体（方块状态存不下）
        Fluid lowerFluid = level.getFluidState(pos).getType();
        CloneChamberBlockEntity chamber = chamberAt(level, pos, state);
        if (chamber != null) {
            chamber.setStoredFluid(FluidKind.of(lowerFluid).isUnknown() ? lowerFluid : null);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            if (neighborState.is(this) && neighborState.getValue(HALF) != half) {
                return state.setValue(FACING, neighborState.getValue(FACING));
            }
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        // 只有下半格才掉落，避免双高方块掉两次
        if (isLower(state)) {
            super.playerDestroy(level, player, pos, state, blockEntity, tool);
        }
        // 上半格：什么都不掉，但依然移除方块
        else {
            // 不调 super，避免 loot table 掉落
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player.isCreative()) {
            boolean lower = isLower(state);
            BlockPos otherPos = lower ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(HALF) != state.getValue(HALF)) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ==================== 交互 ====================

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos lowerPos = isLower(state) ? pos : pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);
        if (lowerState.getBlock() instanceof CloneChamberBlock
                && level.getBlockEntity(lowerPos) instanceof CloneChamberBlockEntity chamber) {
            return chamber.useByPlayer(level, lowerState, lowerPos, player);
        }
        return InteractionResult.PASS;
    }

    // ==================== 舱门开关 ====================

    public static void setOpen(BlockState state, Level level, BlockPos pos, boolean open) {
        if (state.getValue(OPEN) == open) {
            return;
        }
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);

        BlockPos otherPos = isLower(state) ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(otherPos);
        if (otherState.is(state.getBlock())) {
            level.setBlock(otherPos, otherState.setValue(OPEN, open), Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockPos lowerPos = isLower(state) ? pos : pos.below();
        if (level.getBlockEntity(lowerPos) instanceof CloneChamberBlockEntity chamber) {
            return chamber.getComparatorOutput();
        }
        return 0;
    }
}