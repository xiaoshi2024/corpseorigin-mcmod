package xiaoshi2022.corpseorigin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 「蚊香」—— 蚊子尸兄（{@code mosquito_zbr}）战场的核心克制道具，放置型方块。
 * <p>
 * <b>烟雾区域</b>：点燃（放置）后持续散出烟雾（客户端 {@link #animateTick} 表现），
 * 有效半径 {@link #SMOKE_RADIUS}。判定入口：
 * <ul>
 *   <li>{@link #isSmokeAround} / {@link #findSmoke}：给蚊子尸兄、蚊群子实体、
 *       蚊子尸兄卵共用——进入烟雾的蚊子掉血消散、移速大减并试图绕开；</li>
 *   <li>卵在烟雾里会被直接清除。</li>
 * </ul>
 * <p>
 * <b>限时燃尽</b>：AGE 0→{@link #BURN_MAX} 每 {@link #BURN_STEP} tick 走一格
 * （总计约 60 秒），烧完自动消失。被蚊子叮咬（{@link #burnFaster}）会加速烧毁
 * ——蚊群会试图摧毁蚊香，玩家得不断换位重新布置。
 */
public class MosquitoCoilBlock extends Block {

    /** 燃烧进度（0..BURN_MAX），蚊子叮咬会 +2 加速燃尽 */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);
    public static final int BURN_MAX = 7;
    /** 每 150 tick（7.5 秒）推进一格，总计约 60 秒 */
    public static final int BURN_STEP = 150;
    /** 烟雾有效半径（格） */
    public static final int SMOKE_RADIUS = 4;

    /** 蚊香是贴地的一小圈盘香，选中箱压扁 */
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 2.0D, 14.0D);

    public MosquitoCoilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // ==================== 点燃 / 燃尽 ====================

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // 放下即点燃：安排第一次燃尽检查
        level.scheduleTick(pos, this, BURN_STEP);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age >= BURN_MAX) {
            // 烧完了：一团烟散去
            level.destroyBlock(pos, false);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D, 8, 0.3D, 0.2D, 0.3D, 0.01D);
            return;
        }
        BlockState next = state.setValue(AGE, age + 1);
        level.setBlock(pos, next, 3);
        level.scheduleTick(pos, this, BURN_STEP);
    }

    /**
     * 蚊子叮咬蚊香：加速燃烧（AGE +2）。烧过上限直接熄灭。
     * 由 {@code MosquitoSwarmEntity} 在贴近蚊香时调用——"蚊群会试图摧毁蚊香"。
     */
    public static void burnFaster(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MosquitoCoilBlock)) return;
        int age = state.getValue(AGE) + 2;
        if (age >= BURN_MAX) {
            level.destroyBlock(pos, false);
            level.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D, 12, 0.3D, 0.2D, 0.3D, 0.02D);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.2F);
        } else {
            level.setBlock(pos, state.setValue(AGE, age), 3);
        }
    }

    // ==================== 烟雾判定（给蚊子 / 卵共用） ====================

    /**
     * {@code pos} 的水平 {@code radius} 格、竖直 -1~+2 格范围内有没有点燃的蚊香。
     * 范围是局部小盒扫描（蚊子每 10 tick 调一次），没有全局 AABB 查询，代价可控。
     */
    public static boolean isSmokeAround(ServerLevel level, BlockPos pos, int radius) {
        return findSmoke(level, pos, radius) != null;
    }

    /**
     * 找 {@code pos} 附近最近的点燃蚊香；没有返回 {@code null}。
     * 返回值可直接用于：蚊子减速判定、核心绕行推力、{@link #burnFaster}。
     */
    public static BlockPos findSmoke(ServerLevel level, BlockPos pos, int radius) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(
                pos.offset(-radius, -1, -radius), pos.offset(radius, 2, radius))) {
            if (level.getBlockState(p).getBlock() instanceof MosquitoCoilBlock) {
                double d = p.distSqr(pos);
                if (d < bestDist) {
                    bestDist = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    // ==================== 客户端表现 ====================

    /** 燃烧中的烟雾粒子：以蚊香为中心在烟雾半径内随机飘出 */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * SMOKE_RADIUS * 1.6D;
        double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * SMOKE_RADIUS * 1.6D;
        double y = pos.getY() + 0.2D + random.nextDouble() * 1.2D;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0D, 0.02D, 0.0D);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 0.15D, pos.getZ() + 0.5D,
                    (random.nextDouble() - 0.5D) * 0.02D, 0.05D, (random.nextDouble() - 0.5D) * 0.02D);
        }
    }
}
