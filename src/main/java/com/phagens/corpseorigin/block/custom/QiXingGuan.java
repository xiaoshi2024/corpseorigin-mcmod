/**
 * 七星棺方块类 - 尸王龙右的召唤核心
 * 
 * 【功能说明】
 * 1. 海洋环境生成：当放置在海洋中时，会感染周围32格范围内的水源
 * 2. 尸王召唤机制：当棺材周围8格内有至少3个尸兄时，开馆召唤尸王龙右
 * 3. 水源感染系统：使用BFS算法传播感染，能量随距离衰减
 * 
 * 【工作原理】
 * - 放置时：检测是否在海洋环境(16x16范围内超过100个水方块)，开始感染周围水源
 * - 移除时：清除该棺材造成的所有水源感染
 * - tick时：检测周围尸兄数量，满足条件则召唤尸王
 * 
 * 【重要参数】
 * - MAX_ENERGY: 15 - 棺材位置的能量最大值
 * - ENERGY_DECAY: 1 - 每格距离的能量衰减值
 * - MAX_DISTANCE: 32 - 感染传播的最大距离
 * - DETECTION_RADIUS: 8 - 检测尸兄的半径
 * - REQUIRED_ZB_COUNT: 3 - 召唤尸王所需的最小尸兄数量
 * 
 * 【关联系统】
 * - QiXingGuanBlockEntity: 处理棺材的动画渲染
 * - InfectionData: 存储水源感染数据的世界保存数据
 * - LowerLevelZbEntity: 被检测的尸兄实体
 * 
 * @author Phagens
 * @version 1.0
 */
package com.phagens.corpseorigin.block.custom;


import com.phagens.corpseorigin.block.entity.QiXingGuanBlockEntity;
import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import com.phagens.corpseorigin.data.InfectionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

/**
 * 七星棺方块主类
 * 继承Block实现基础方块功能，实现EntityBlock接口以支持方块实体
 */
public class QiXingGuan extends Block implements EntityBlock , LiquidBlockContainer, SimpleWaterloggedBlock {
    /** 要召唤的实体类型(尸王龙右) */
    private final Supplier<EntityType<?>> ENTITY;

    /** 方块状态属性：是否已召唤 */
    public static final BooleanProperty SUMMONED = BooleanProperty.create("summoned");

    /** 方块状态属性：是否含水 */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * 构造函数
     * @param entity 要召唤的实体类型供应器
     */
    public QiXingGuan(Supplier<EntityType<?>> entity) {
        super(BlockBehaviour.Properties.of()
                .strength(1.5f,6.0f)
                .sound(SoundType.WOOD)
                .mapColor(MapColor.WOOD)
                .noOcclusion()
                .randomTicks()
        );

        ENTITY = entity;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SUMMONED, false)
                .setValue(WATERLOGGED, false));
    }


    @Override
    protected FluidState getFluidState(BlockState state) {
        // 根据 WATERLOGGED 属性动态返回
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }


    /**
     * 注册方块状态属性
     * 添加SUMMONED属性用于控制召唤状态
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SUMMONED, WATERLOGGED);
    }

    /**
     * 方块被放置时的处理
     * - 服务器端：开始感染周围水源
     * - 安排20tick后的首次检测
     */
    /**
     * 方块被放置时的处理
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide) {
            // 自动检测是否被放置在水中
            FluidState fluidState = level.getFluidState(pos);
            if (fluidState.getType() == Fluids.WATER && !state.getValue(WATERLOGGED)) {
                level.setBlock(pos, state.setValue(WATERLOGGED, true), 3);
            }
            spreadWaterInfection(level, pos);
        }
        level.scheduleTick(pos, this, 20);
    }

    /**
     * 方块被放置时设置朝向（玩家放置时）
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());

        return this.defaultBlockState()
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }
    /**
     * 方块被移除时的处理
     * 清除该棺材造成的所有水源感染
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && state.is(state.getBlock())) {
            clearInfection(pos, level);
        }
    }

    /**
     * 方块tick处理 - 每20tick执行一次
     * 如果尚未召唤，检测周围尸兄数量
     */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);

        if (!state.getValue(SUMMONED)) {
            // 检测周围是否有足够尸兄
            detectEntitiesInInfectedArea(level, pos);
        }
        // 重新安排下次检测(每20tick = 1秒)
        level.scheduleTick(pos, this, 20);
    }

    /** 最大能量值（类似红石强度） */
    private static final int MAX_ENERGY = 15;
    /** 每格距离能量衰减值 */
    private static final int ENERGY_DECAY = 1;

    /**
     * 传播水源感染 - 使用BFS算法
     * 从棺材位置开始向周围水源传播感染，能量随距离衰减
     * 
     * @param level 世界实例
     * @param pos 棺材位置
     */
    private void spreadWaterInfection(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        InfectionData data = InfectionData.get(serverLevel);
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> infectedPositions = new HashSet<>();
        queue.offer(pos);
        visited.add(pos);
        data.setWaterEnergy(pos, MAX_ENERGY); // 棺材位置能量最高
        final int MAX_DISTANCE = 32;

        // BFS遍历周围水源
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            int currentEnergy = data.getWaterEnergy(current);
            // 计算切比雪夫距离
            int distance = Math.max(
                    Math.abs(current.getX() - pos.getX()),
                    Math.max(
                            Math.abs(current.getY() - pos.getY()),
                            Math.abs(current.getZ() - pos.getZ())
                    )
            );
            if (distance >= MAX_DISTANCE || currentEnergy <= 0) {
                continue;
            }

            // 遍历6个方向
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (visited.contains(neighbor)) {
                    continue;
                }
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.getFluidState().is(FluidTags.WATER)) {
                    int neighborEnergy = currentEnergy - ENERGY_DECAY;
                    if (neighborEnergy > 0) {
                        markInfectedWithEnergy(data, pos, neighbor, neighborEnergy, infectedPositions);
                        visited.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }
        }
    }
    
    /**
     * 标记感染的水位置并设置能量
     */
    private void markInfectedWithEnergy(InfectionData data, BlockPos coffinPos, BlockPos waterPos, int energy, Set<BlockPos> infectedPositions) {
        infectedPositions.add(waterPos);
        data.addInfectedWater(coffinPos, waterPos, energy);
    }

    /**
     * 标记水为感染状态（静态方法，供外部调用）
     * 注意：实际使用时会通过事件处理器传入 ServerLevel
     */
    public static void markWaterInfected(BlockPos pos) {
        // 预留接口
    }

    /**
     * 检查水是否被感染
     * 注意：实际使用时会通过事件处理器传入 ServerLevel
     */
    public static boolean isWaterInfected(BlockPos pos) {
        return false;
    }

    /**
     * 清除棺材的感染
     * 当棺材被移除时调用，清除该棺材造成的所有水源感染
     */
    private void clearInfection(BlockPos coffinPos, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!isInOcean(level, coffinPos)) {
            return;
        }

        InfectionData data = InfectionData.get(serverLevel);
        data.removeCoffinInfections(coffinPos);
    }

    /**
     * 检查棺材是否在海洋环境中
     * 检测16x16x16范围内是否有超过100个水方块
     *
     * @param level 世界实例
     * @param pos 棺材位置
     * @return 是否在海洋环境中
     */
    private boolean isInOcean(Level level, BlockPos pos) {
        final int RANGE = 8; // 16x16范围，半径8格
        final int WATER_THRESHOLD = 100; // 水方块数量阈值

        int waterCount = 0;
        for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-RANGE, -RANGE, -RANGE), pos.offset(RANGE, RANGE, RANGE))) {
            if (level.getBlockState(checkPos).getFluidState().is(FluidTags.WATER)) {
                waterCount++;
                if (waterCount >= WATER_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 召唤逻辑 - 检测周围尸兄并召唤尸王
     * 只有棺材周围有足够数量的尸兄时才允许开馆
     *
     * @param level 世界实例
     * @param posE 棺材位置
     */
    private void detectEntitiesInInfectedArea(Level level, BlockPos posE) {
        final int DETECTION_RADIUS = 8;  // 检测半径8格（棺材周围）
        final int REQUIRED_ZB_COUNT = 3; // 需要至少3个尸兄

        // 检测棺材周围的尸兄
        List<LowerLevelZbEntity> zbEntities = level.getEntitiesOfClass(
                LowerLevelZbEntity.class,
                new AABB(posE).inflate(DETECTION_RADIUS)
        );

        // 如果棺材周围有足够数量的尸兄且未召唤过，则开馆召唤尸王
        if (zbEntities.size() >= REQUIRED_ZB_COUNT && !level.getBlockState(posE).getValue(SUMMONED)) {
            CorpseOrigin.LOGGER.info("七星棺周围检测到 {} 个尸兄，开馆召唤尸王龙右！", zbEntities.size());
            triggerAction((ServerLevel) level, posE);
            level.setBlock(posE, this.stateDefinition.any().setValue(SUMMONED, true), 3);
        }
    }

    /**
     * 获取渲染形状 - 使用方块实体动画渲染
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /**
     * 创建方块实体
     * 实现 EntityBlock 接口的方法
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QiXingGuanBlockEntity(pos, state);
    }

    /**
     * 触发召唤动作
     * 在棺材上方生成尸王实体
     *
     * @param level 服务器世界
     * @param pos 棺材位置
     */
    private void triggerAction(ServerLevel level, BlockPos pos) {
        ENTITY.get().spawn(level, null, null, pos.above(), MobSpawnType.EVENT, false, false);
    }

    /**
     * 创建完整的七星棺碰撞箱形状 - 简化长方体
     * 尺寸：宽2格（X轴），高1格（Y轴），长1格（Z轴）
     */
    private VoxelShape makeShape() {
        // 长方体：从 x=0 到 x=2 (宽2)
        //        从 y=0 到 y=1 (高1)
        //        从 z=0 到 z=1 (长1)
        return Shapes.box(0, 0, 0, 2, 1, 1);
    }

    /**
     * 获取碰撞箱形状 - 固定形状
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return makeShape();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return makeShape();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return makeShape();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return makeShape();
    }

    /**
     * 处理液体放置（玩家用水桶右键）
     */
    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!state.getValue(WATERLOGGED) && fluidState.getType() == Fluids.WATER) {
            // 设置含水状态
            level.setBlock(pos, state.setValue(WATERLOGGED, true), 3);
            // 安排水的 tick（让水流动）
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            return true;
        }
        return false;
    }

    /**
     * 是否可以放置液体
     */
    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return !state.getValue(WATERLOGGED) && fluid == Fluids.WATER;
    }
}
