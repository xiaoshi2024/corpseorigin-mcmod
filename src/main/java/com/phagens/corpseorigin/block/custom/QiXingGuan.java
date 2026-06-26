/**
 * 七星棺方块类 - 尸王龙右的召唤核心
 *
 * 【功能说明】
 * 1. 群系替换：当放置时，将周围区域替换为尸兄群系（死寂群系）
 * 2. 尸王召唤机制：当棺材周围8格内有至少3个尸兄时，开馆召唤尸王龙右
 * 3. 水源感染系统：尸兄群系内的所有水源自动被感染，玩家接触会中毒
 *
 * 【工作原理】
 * - 放置时：检测周围群系并替换为死寂群系
 * - 移除时：清除该棺材造成的所有水源感染（但群系不会恢复）
 * - tick时：检测周围尸兄数量，满足条件则召唤尸王
 *
 * 【重要参数】
 * - BIOME_REPLACEMENT_RADIUS: 16 - 群系替换的半径范围
 * - DETECTION_RADIUS: 8 - 检测尸兄的半径
 * - REQUIRED_ZB_COUNT: 3 - 召唤尸王所需的最小尸兄数量
 *
 * 【关联系统】
 * - QiXingGuanBlockEntity: 处理棺材的动画渲染
 * - InfectionData: 存储水源感染数据的世界保存数据
 * - LowerLevelZbEntity: 被检测的尸兄实体
 * - BiomeRegistry: 死寂群系注册
 *
 * @author Phagens
 * @version 2.1
 */
package com.phagens.corpseorigin.block.custom;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.block.entity.QiXingGuanBlockEntity;
import com.phagens.corpseorigin.entity.LowerLevelZbEntity;
import com.phagens.corpseorigin.register.BiomeRegistry;
import com.phagens.corpseorigin.effect.BYeffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
public class QiXingGuan extends Block implements EntityBlock, LiquidBlockContainer, SimpleWaterloggedBlock {
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
     * - 服务器端：替换周围群系为尸兄群系，并感染该区域的水源
     * - 安排20tick后的首次检测
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

            // 替换周围群系为尸兄群系，并感染水源
            replaceBiomeAndInfectWater((ServerLevel) level, pos);
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
     * 注意：群系不会被恢复，这是永久性改变
     * 水源感染通过群系检查实现，无需清理
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && state.is(state.getBlock())) {
            CorpseOrigin.LOGGER.info("七星棺已移除");
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

    /** 群系替换半径 - 棺材周围16格范围内替换群系 */
    private static final int BIOME_REPLACEMENT_RADIUS = 16;

    /**
     * 替换周围群系为尸兄群系
     *
     * 【工作流程】
     * 1. 获取死寂群系的Holder
     * 2. 在指定半径范围内遍历所有方块列
     * 3. 通过Chunk API替换每列的群系为死寂群系
     * 4. 标记受影响的区块为未保存
     * 5. 向附近玩家发送区块更新包
     *
     * @param level 服务器世界
     * @param pos 棺材位置
     */
    private void replaceBiomeAndInfectWater(ServerLevel level, BlockPos pos) {
        // 获取死寂群系
        var biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        var deadSilenceBiome = biomeRegistry.getHolder(BiomeRegistry.DEAD_SILENCE);

        if (deadSilenceBiome.isEmpty()) {
            CorpseOrigin.LOGGER.error("无法获取死寂群系！");
            return;
        }

        // 遍历棺材周围半径范围内的所有区块
        int minChunkX = (pos.getX() - BIOME_REPLACEMENT_RADIUS) >> 4;
        int maxChunkX = (pos.getX() + BIOME_REPLACEMENT_RADIUS) >> 4;
        int minChunkZ = (pos.getZ() - BIOME_REPLACEMENT_RADIUS) >> 4;
        int maxChunkZ = (pos.getZ() + BIOME_REPLACEMENT_RADIUS) >> 4;

        int replacedBiomeCount = 0;
        Set<ChunkPos> affectedChunks = new HashSet<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ);

                // 获取区块内所有 LevelChunkSection
                for (int sectionY = 0; sectionY < chunk.getSectionsCount(); sectionY++) {
                    LevelChunkSection section = chunk.getSection(sectionY);
                    if (section != null) {
                        // 获取 biomes 容器并重新创建它
                        PalettedContainer<Holder<Biome>> biomesContainer = section.getBiomes().recreate();

                        // 遍历 section 内的所有生物群系位置 (4x4x4 网格)
                        for (int biomeX = 0; biomeX < 4; biomeX++) {
                            for (int biomeY = 0; biomeY < 4; biomeY++) {
                                for (int biomeZ = 0; biomeZ < 4; biomeZ++) {
                                    // 计算世界坐标，检查是否在替换半径内
                                    int worldX = (chunkX << 4) + (biomeX << 2);
                                    int worldZ = (chunkZ << 4) + (biomeZ << 2);

                                    // 检查是否在半径范围内（只检查XZ平面）
                                    double dx = worldX - pos.getX();
                                    double dz = worldZ - pos.getZ();
                                    if (dx * dx + dz * dz <= BIOME_REPLACEMENT_RADIUS * BIOME_REPLACEMENT_RADIUS) {
                                        // 设置生物群系
                                        biomesContainer.getAndSetUnchecked(biomeX, biomeY, biomeZ, deadSilenceBiome.get());
                                        replacedBiomeCount++;
                                    }
                                }
                            }
                        }

                        // 重新设置 section 的 biomes
                        try {
                            java.lang.reflect.Field biomesField = LevelChunkSection.class.getDeclaredField("biomes");
                            biomesField.setAccessible(true);
                            biomesField.set(section, biomesContainer);
                        } catch (Exception e) {
                            CorpseOrigin.LOGGER.error("无法设置生物群系: {}", e.getMessage());
                        }
                    }
                }

                chunk.setUnsaved(true);
                affectedChunks.add(chunkPos);
            }
        }

        // 向附近玩家发送区块更新包，让客户端同步群系变化
        // 使用 ClientboundLevelChunkPacketData 来更新整个区块
        for (ChunkPos chunkPos : affectedChunks) {
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);

            // 注意：ClientboundLevelChunkPacketData 不能直接发送，需要通过 ClientboundLevelChunkWithLightPacket
            // 或者直接发送给玩家
            var packet = new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(
                    chunk,
                    level.getLightEngine(),
                    null,
                    null
            );

            // 向附近的玩家发送更新包
            Vec3 chunkCenter = Vec3.atLowerCornerOf(chunkPos.getWorldPosition()).add(8, 0, 8);
            for (ServerPlayer player : level.getPlayers(p ->
                    p.distanceToSqr(chunkCenter) < 256 * 256)) {
                player.connection.send(packet);
            }
        }

        CorpseOrigin.LOGGER.info("七星棺已放置！替换群系: {} 个位置，影响区块: {} 个",
                replacedBiomeCount, affectedChunks.size());

        infectNearbyVillagers(level, pos);
    }

    /** 村民感染检测半径 - 棺材周围64格范围内检测村庄和村民 */
    private static final int VILLAGER_INFECTION_RADIUS = 64;

    /** 村民感染概率 - 每个村民有30%概率被感染 */
    private static final float VILLAGER_INFECTION_CHANCE = 0.3f;

    /**
     * 检测周围村庄和村民，概率性感染村民
     *
     * 【工作流程】
     * 1. 检测棺材周围64格范围内的所有村民
     * 2. 对每个村民进行概率判定（30%概率感染）
     * 3. 被感染的村民获得尸兄感染buff（BYeffect）
     * 4. 记录感染日志
     *
     * @param level 服务器世界
     * @param pos 棺材位置
     */
    private void infectNearbyVillagers(ServerLevel level, BlockPos pos) {
        // 检测周围村民
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                new AABB(pos).inflate(VILLAGER_INFECTION_RADIUS)
        );

        if (nearbyVillagers.isEmpty()) {
            CorpseOrigin.LOGGER.debug("七星棺周围 {} 格内未检测到村民", VILLAGER_INFECTION_RADIUS);
            return;
        }

        int infectedCount = 0;
        for (Villager villager : nearbyVillagers) {
            // 检查村民是否已经被感染
            if (villager.hasEffect(com.phagens.corpseorigin.register.EffectRegister.QIANS)) {
                continue;
            }

            // 概率性感染
            if (level.getRandom().nextFloat() < VILLAGER_INFECTION_CHANCE) {
                BYeffect.applyInfection(villager, level);
                infectedCount++;
                CorpseOrigin.LOGGER.info("村民 {} 被七星棺感染！", villager.getName().getString());
            }
        }

        if (infectedCount > 0) {
            CorpseOrigin.LOGGER.info("七星棺周围检测到 {} 个村民，成功感染 {} 个",
                    nearbyVillagers.size(), infectedCount);
        } else {
            CorpseOrigin.LOGGER.info("七星棺周围检测到 {} 个村民，但本次没有村民被感染（概率判定）",
                    nearbyVillagers.size());
        }
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