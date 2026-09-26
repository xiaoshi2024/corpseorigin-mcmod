package xiaoshi2022.corpseorigin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import xiaoshi2022.corpseorigin.block.entity.CNChessZbrsBlockEntity;

/**
 * 象棋尸兄方块。
 * <p>
 * 外观全部交给 GeckoLib 的 BER（{@link xiaoshi2022.corpseorigin.client.renderer.blockentity.CNChessZbrsRenderer}），
 * 所以原版渲染形状是 INVISIBLE。
 * <p>
 * 交互：
 * <ul>
 *   <li>空手右键：播放/推进处决动画 {@code put_death}（尖刺夹碎背上的人）；</li>
 *   <li>潜行右键：直接播放 {@code die} 并破坏方块。</li>
 * </ul>
 */
public class CNChessZbrsBlock extends Block implements EntityBlock {

    /** 方块碰撞箱：原竖直箱绕 X 轴放倒 90° 后拉平，沿 z 轴的低平板 */
    private static final VoxelShape SHAPE = makeShape();

    /** 爆炸威力每 1 点折算的 HP 伤害（苦力怕 3 威力 = 18 点） */
    private static final float EXPLOSION_DAMAGE_PER_RADIUS = 6.0F;

    public CNChessZbrsBlock(Properties properties) {
        super(properties);
    }

    private static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        // 放倒拉平后的两块低平板（y 0~0.25），沿 z 连续铺开
        shape = Shapes.join(shape, Shapes.box(0.25, 0.0, 0.0, 0.75, 0.25, 0.5), net.minecraft.world.phys.shapes.BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.25, 0.0, 0.4375, 0.75, 0.25, 1.375), net.minecraft.world.phys.shapes.BooleanOp.OR);
        return shape;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // 26.2 没有 ENTITYBLOCK_ANIMATED：不画原版模型，全部交给 GeckoLib BER
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CNChessZbrsBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // 战斗/行动 tick：无敌帧与走子冷却需要较细粒度，每 4 tick 一次
        level.scheduleTick(pos, this, 4);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, 4);
        if (level.getBlockEntity(pos) instanceof CNChessZbrsBlockEntity zbrs) {
            zbrs.serverTick(level, pos, state);
        }
    }

    /**
     * 被爆炸"炸中"时：不直接销毁方块，折成 HP 伤害喂给方块实体，
     * 生死由它的血量决定。
     */
    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        if (level.getBlockEntity(pos) instanceof CNChessZbrsBlockEntity zbrs) {
            Entity direct = explosion.getDirectSourceEntity();
            DamageSource source = Explosion.getDefaultDamageSource(level, direct);
            float damage = Math.max(1.0F, explosion.radius()) * EXPLOSION_DAMAGE_PER_RADIUS;
            zbrs.hurt(level, source, damage);
        }
        // 不调用 super：方块不由爆炸直接移除
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CNChessZbrsBlockEntity zbrs)) {
            return InteractionResult.PASS;
        }

        // 潜行右键：直接死亡并移除方块
        if (player.isShiftKeyDown()) {
            zbrs.playDie();
            level.playSound(null, pos, SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 1.0F, 0.7F);
            level.scheduleTick(pos, this, 20);
            return InteractionResult.CONSUME;
        }

        // 普通右键：处决动画（尖刺夹碎）
        zbrs.playPutDeath();
        level.playSound(null, pos, SoundEvents.WARDEN_ROAR, SoundSource.BLOCKS, 1.0F, 0.6F);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof CNChessZbrsBlockEntity zbrs) {
            // 被破坏时也播一次 die，让动画收尾
            zbrs.playDie();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}