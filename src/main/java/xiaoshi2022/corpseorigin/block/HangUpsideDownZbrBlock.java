package xiaoshi2022.corpseorigin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.block.entity.HangUpsideDownZbrBlockEntity;

/**
 * 倒挂尸兄（{@code hang_upside_down_zbr}）—— 剧情方块：倒挂在天花板上的尸兄。
 * <p>
 * 【原著场景】H 市小区走廊里，一群尸兄倒挂吸附在天花板上，垂着舌头等着路过的猎物
 * （见设定图：昏暗走廊顶部垂下的黑色躯体与触须状组织）。
 * <p>
 * 【行为】
 * <ul>
 *   <li>外观完全由 GeckoLib 方块实体渲染（本体 {@code RenderShape.INVISIBLE}）；</li>
 *   <li>只能挂在天花板下：放置时要求上方方块底面完整支撑，天花板没了就自己掉落；</li>
 *   <li>玩家右键 → 播放 {@code tongue} 吐舌动画（约 4.08s，播完自动回到 {@code idle} 静止姿态）；
 *       吐舌期间再右键不会打断重播。</li>
 * </ul>
 * 动画触发同步套路同 {@link QiXingGuanBlock}：服务端翻转方块状态属性，
 * 客户端方块实体的动画控制器直接读方块状态，无需额外同步。
 */
public class HangUpsideDownZbrBlock extends Block implements EntityBlock {

    /** 是否正在吐舌（true 时播放 tongue 动画，false 时播 idle） */
    public static final BooleanProperty TONGUE = BooleanProperty.create("tongue");

    /** tongue 动画时长（4.0816s → 82 tick），到点自动翻回 false 回 idle */
    public static final int TONGUE_TICKS = 82;

    private static final VoxelShape FULL_CUBE = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public HangUpsideDownZbrBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TONGUE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TONGUE);
    }

    // ==================== 右键吐舌 ====================

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS; // 客户端：挥手
        }
        if (state.getValue(TONGUE)) {
            return InteractionResult.CONSUME; // 正在吐舌，不打断重播
        }
        level.setBlock(pos, state.setValue(TONGUE, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, TONGUE_TICKS);
        level.playSound(null, pos, SoundEvents.ZOMBIE_AMBIENT, SoundSource.BLOCKS, .8f, 1.3f);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(TONGUE)) {
            level.setBlock(pos, state.setValue(TONGUE, false), Block.UPDATE_ALL);
        }
    }

    // ==================== 倒挂支撑 ====================

    /** 挂在天花板上：上方方块的底面必须完整支撑。 */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        // 天花板没了 → 尸兄掉下来（返回 AIR 让原版正常掉落战利品）
        if (direction == Direction.UP && !neighborState.isFaceSturdy(level, neighborPos, Direction.DOWN)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    // ==================== 形状 / 渲染 / 方块实体 ====================

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_CUBE;
    }

    /** 方块本体不渲染——外观由 {@link HangUpsideDownZbrBlockEntity} 的 GeckoLib 渲染器绘制。 */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HangUpsideDownZbrBlockEntity(pos, state);
    }
}
