package xiaoshi2022.corpseorigin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.block.entity.QiXingGuanBlockEntity;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;
import xiaoshi2022.corpseorigin.registry.ModBlocks;
import xiaoshi2022.corpseorigin.registry.ModFluids;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 七星棺 —— 封印尸王龙右的千年古尸棺（自 1.21.1 旧版移植）。
 * <p>
 * 原著：盗墓贼运输途中棺材掉进河里，封印松动，龙右苏醒、尸水污染整条河。
 * <p>
 * 【行为】
 * <ul>
 *   <li>外观由 GeckoLib 方块实体渲染（本体 {@code RenderShape.INVISIBLE}）；</li>
 *   <li>落水（含水 размещения / 掉落物入水 / Create 载具解体落地）触发<b>沉棺事件</b>：
 *       气泡水花粒子 + 全服播报，并把周边水源染成尸水（{@link ModFluids#INFECTED_WATER_BLOCK}，
 *       玩家泡水中毒、村民自动走 {@code CorpseInfection} 感染链）；</li>
 *   <li>开馆（SUMMONED=true，播 open 动画）条件二选一：
 *       周围 {@value #DETECTION_RADIUS} 格内有 ≥{@value #REQUIRED_ZB_COUNT} 只尸兄；
 *       或落水后 {@code config.qiXingGuan.autoOpenSeconds} 秒自动开馆（0 = 禁用）；</li>
 *   <li>开馆<b>不再自动生成龙右实体</b>——龙右由扮演玩家自行登场
 *       （旁观切生存），全服播报提示；</li>
 *   <li>与 Create（机械动力）联动为纯软声明：contraption 搬运棺方块、解体落水即触发以上流程，
 *       模组侧零 API 依赖。</li>
 * </ul>
 */
public class QiXingGuanBlock extends Block implements EntityBlock, LiquidBlockContainer {

    /** 是否已开馆（播放开棺动画、进入龙右登场阶段） */
    public static final BooleanProperty SUMMONED = BooleanProperty.create("summoned");
    /** 是否含水（沉在水里） */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /** 开馆检测半径（格） */
    private static final int DETECTION_RADIUS = 8;
    /** 触发开馆所需的最低尸兄数量 */
    private static final int REQUIRED_ZB_COUNT = 3;

    public QiXingGuanBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SUMMONED, false)
                .setValue(WATERLOGGED, false));
    }

    // ==================== 方块状态 / 含水 ====================

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SUMMONED, WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!state.getValue(WATERLOGGED) && fluidState.getType() == Fluids.WATER) {
            level.setBlock(pos, state.setValue(WATERLOGGED, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            return true;
        }
        return false;
    }

    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity entity, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return !state.getValue(WATERLOGGED) && fluid == Fluids.WATER;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide()) {
            return;
        }
        // 自动含水：放置在水中时补上 WATERLOGGED（掉落物入水放置 / Create 解体落地都会走到这）
        if (level.getFluidState(pos).getType() == Fluids.WATER && !state.getValue(WATERLOGGED)) {
            level.setBlock(pos, state.setValue(WATERLOGGED, true), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, 20);
    }

    // ==================== 主循环：沉棺事件 + 开馆检测 ====================

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        level.scheduleTick(pos, this, 20);

        QiXingGuanBlockEntity be = level.getBlockEntity(pos) instanceof QiXingGuanBlockEntity guan ? guan : null;
        boolean inWater = state.getValue(WATERLOGGED) || level.getFluidState(pos).is(FluidTags.WATER);

        if (inWater && be != null) {
            // 首次落水：记录时间并触发一次沉棺事件
            if (be.waterloggedSinceGameTime < 0) {
                be.waterloggedSinceGameTime = level.getGameTime();
                be.setChanged();
            }
            if (!be.sinkingEventDone) {
                be.sinkingEventDone = true;
                be.setChanged();
                startSinkingEvent(level, pos);
            }
            // 落水倒计时自动开馆（贴合原著"运输途中掉河里"无人喂怪的情况）
            if (!state.getValue(SUMMONED)) {
                int autoOpenSeconds = CorpseConfig.get().qiXingGuan.autoOpenSeconds;
                if (autoOpenSeconds > 0
                        && level.getGameTime() - be.waterloggedSinceGameTime >= autoOpenSeconds * 20L) {
                    openCoffin(level, pos, state);
                    return;
                }
            }
        }

        // 周围尸兄足够 → 立即开馆（保留 1.21.1 行为）
        if (!state.getValue(SUMMONED)) {
            List<LowerLevelZbEntity> zombies = level.getEntitiesOfClass(LowerLevelZbEntity.class,
                    new AABB(pos).inflate(DETECTION_RADIUS));
            if (zombies.size() >= REQUIRED_ZB_COUNT) {
                CorpseOrigin.LOGGER.info("七星棺周围检测到 {} 只尸兄，开馆！", zombies.size());
                openCoffin(level, pos, state);
            }
        }
    }

    // ==================== 沉棺事件：粒子 + 播报 + 染尸水 ====================

    private void startSinkingEvent(ServerLevel level, BlockPos pos) {
        int radius = Math.max(1, CorpseConfig.get().qiXingGuan.infectRadius);

        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.5F, 0.6F);
        level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 1.2F, 0.5F);
        level.sendParticles(ParticleTypes.CURRENT_DOWN, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                60, 0.8, 0.4, 0.8, 0.05);
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                80, 1.2, 0.6, 1.2, 0.08);

        int infected = infectNearbyWater(level, pos, radius);
        CorpseOrigin.LOGGER.info("七星棺坠入水中！已感染周边 {} 处水源（半径 {}）", infected, radius);

        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.corpseorigin.qi_xing_guan.sunk"), false);
    }

    /** 把半径内的<b>水源</b>方块替换为尸水（流动的水保持原样，由尸水自己流动扩散）。 */
    private int infectNearbyWater(ServerLevel level, BlockPos pos, int radius) {
        int infected = 0;
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
            BlockState state = level.getBlockState(target);
            if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                level.setBlock(target.immutable(), ModFluids.INFECTED_WATER_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                infected++;
            }
        }
        return infected;
    }

    // ==================== 开馆 ====================

    private void openCoffin(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(SUMMONED)) {
            return;
        }
        level.setBlock(pos, state.setValue(SUMMONED, true), Block.UPDATE_ALL);

        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 1.2F, 0.7F);
        level.playSound(null, pos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.BLOCKS, 1.5F, 0.5F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                40, 0.6, 0.5, 0.6, 0.02);
        level.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                30, 0.8, 0.6, 0.8, 0.03);

        CorpseOrigin.LOGGER.info("七星棺开启！龙右封印已破（等待扮演玩家登场）");
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.corpseorigin.qi_xing_guan.open"), false);
    }

    // ==================== 掉落物入水自动放置 ====================

    /**
     * 七星棺掉落物入水 → 原地放置为方块（替代 1.21.1 的 NeoForge ItemTossEvent，
     * 挂在 [ItemEntityMixin] 的 tick 钩子上）。每 tick 由掉落物自己调用。
     * <p>
     * 只要掉落物还在水里且所在格可替换（水面漂浮也算），就立即落棺。
     */
    public static void tickDropped(ItemEntity self) {
        if (self.level().isClientSide()) {
            return;
        }
        ItemStack stack = self.getItem();
        if (!stack.is(ModBlocks.QI_XING_GUAN.asItem())) {
            return;
        }
        ServerLevel level = (ServerLevel) self.level();
        BlockPos pos = self.blockPosition();
        BlockState current = level.getBlockState(pos);
        boolean replaceable = current.isAir() || current.canBeReplaced();
        if (!replaceable || !level.getFluidState(pos).is(FluidTags.WATER)) {
            return;
        }
        level.setBlock(pos, ModBlocks.QI_XING_GUAN.defaultBlockState(), Block.UPDATE_ALL);
        self.discard();
        CorpseOrigin.LOGGER.info("七星棺掉落物入水，已在 {} 自动放置沉棺", pos.toShortString());
    }

    // ==================== 形状 / 渲染 / 方块实体 ====================

    /**
     * 两格长碰撞箱（X 轴 0~2，对齐 1.21.1 旧版）：逻辑格只有一格，
     * 另一格是 geo 模型的视觉/碰撞延伸，所以 onPlace/tick 等逻辑仍以单格坐标为准。
     */
    private VoxelShape shape() {
        return Shapes.box(0, 0, 0, 2, 1, 1);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return shape();
    }

    /** 方块本体不渲染——外观由 {@link QiXingGuanBlockEntity} 的 GeckoLib 渲染器绘制。 */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QiXingGuanBlockEntity(pos, state);
    }
}
