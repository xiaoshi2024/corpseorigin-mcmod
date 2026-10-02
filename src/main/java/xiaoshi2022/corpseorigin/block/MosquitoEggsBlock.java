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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import xiaoshi2022.corpseorigin.entity.MosquitoSwarmEntity;
import xiaoshi2022.corpseorigin.entity.MosquitoZbrEntity;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/**
 * 蚊子尸兄卵（{@code mosquito_zbr_eggs}）—— 蚊群战斗中的持续骚扰源。
 * <p>
 * 原著里蚊子尸兄的卵堵住了楼梯；这里做成战斗中持续产下的地面方块，
 * 逼玩家在"打核心"与"清卵"之间做选择：
 * <ul>
 *   <li><b>孵化</b>：放置后每 {@link #HATCH_DELAY} tick 推进一格 AGE（约 6 秒/格），
 *       AGE 满 {@link #HATCH_AGE} 后孵化出 1~2 只 {@link MosquitoSwarmEntity}
 *       （附近 48 格有蚊群核心就归它管，否则是野生蚊子）；不管卵的话蚊子会越打越多；</li>
 *   <li><b>火焰清除</b>：周围 3×3×3 有火 / 熔岩 → 直接销毁；</li>
 *   <li><b>蚊香清除</b>：烟雾范围内 → 直接销毁（{@link MosquitoCoilBlock#isSmokeAround}）。</li>
 * </ul>
 * 用<b>计划刻</b>（scheduleTick）而不是随机刻推进：孵化节奏精确，且随区块持久化。
 * 外观是独立的 geo 模型（{@code mosquito_zbr_eggs.geo.json}，Ber 渲染），原版渲染形状 INVISIBLE。
 * 无战利品表：被打掉不掉任何东西（防止刷卵刷蚊子无限循环）。
 */
public class MosquitoEggsBlock extends Block implements EntityBlock {

    /** 孵化进度 0..HATCH_AGE */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    public static final int HATCH_AGE = 3;
    /** 每 120 tick（6 秒）推进一格 → 总孵化约 24 秒 */
    public static final int HATCH_DELAY = 120;

    /** 卵堆比整块略矮一点，选中箱匹配 */
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 12.0D, 15.0D);

    public MosquitoEggsBlock(Properties properties) {
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

    /** 26.2 没有 ENTITYBLOCK_ANIMATED：不画原版静态模型，外观全交给 GeckoLib BER */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new xiaoshi2022.corpseorigin.block.entity.MosquitoEggsBlockEntity(pos, state);
    }

    // ==================== 孵化推进 ====================

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, HATCH_DELAY);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // ① 火焰清除：周围有火 / 熔岩 → 卵烧毁
        if (isBurningNearby(level, pos)) {
            destroyWithPuff(level, pos);
            return;
        }
        // ② 蚊香清除：烟雾范围内卵直接消掉（蚊香类物品克制）
        if (MosquitoCoilBlock.isSmokeAround(level, pos, 3)) {
            destroyWithPuff(level, pos);
            return;
        }

        int age = state.getValue(AGE);
        if (age >= HATCH_AGE) {
            // age=3 的 incubate（破壳蠕动）已播完 → 孵化
            hatch(level, pos);
            return;
        }
        level.setBlock(pos, state.setValue(AGE, age + 1), 3);
        // 最后一档只留 incubate 动画一个来回（约 3 秒）就破壳
        level.scheduleTick(pos, this, age + 1 >= HATCH_AGE ? HATCH_DELAY / 2 : HATCH_DELAY);
    }

    /** 孵化：销毁方块 + 生成 1~2 只蚊子（归属最近的核心，没有就是野生） */
    private void hatch(ServerLevel level, BlockPos pos) {
        level.destroyBlock(pos, false);
        level.playSound(null, pos, SoundEvents.SLIME_BLOCK_BREAK, SoundSource.HOSTILE, 0.8F, 0.5F);

        MosquitoZbrEntity core = level.getEntitiesOfClass(MosquitoZbrEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(48.0D),
                        MosquitoZbrEntity::isAlive)
                .stream().findFirst().orElse(null);

        // 一卵爆 3~5 只（原著里卵一破就是一小团蚊子）
        int count = 3 + level.getRandom().nextInt(3);
        for (int i = 0; i < count; i++) {
            MosquitoSwarmEntity mosquito = ModEntities.MOSQUITO_SWARM.create(
                    level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (mosquito == null) continue;
            double ox = (level.getRandom().nextDouble() - 0.5D) * 0.6D;
            double oz = (level.getRandom().nextDouble() - 0.5D) * 0.6D;
            mosquito.setPos(pos.getX() + 0.5D + ox, pos.getY(), pos.getZ() + 0.5D + oz);
            mosquito.setYRot(level.getRandom().nextFloat() * 360.0F);
            if (core != null) {
                mosquito.setSwarmOwner(core.getUUID());
            }
            level.addFreshEntity(mosquito);
            level.sendParticles(ParticleTypes.POOF,
                    mosquito.getX(), mosquito.getY() + 0.1D, mosquito.getZ(), 4, 0.15D, 0.1D, 0.15D, 0.01D);
        }
    }

    private void destroyWithPuff(ServerLevel level, BlockPos pos) {
        level.destroyBlock(pos, false);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 8, 0.25D, 0.25D, 0.25D, 0.02D);
    }

    /** 周围 3×3×3 有火方块或熔岩 → true */
    private static boolean isBurningNearby(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState s = level.getBlockState(p);
            if (s.is(net.minecraft.tags.BlockTags.FIRE) || !level.getFluidState(p).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
