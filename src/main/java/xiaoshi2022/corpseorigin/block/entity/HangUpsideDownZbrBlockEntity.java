package xiaoshi2022.corpseorigin.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import xiaoshi2022.corpseorigin.block.HangUpsideDownZbrBlock;
import xiaoshi2022.corpseorigin.registry.ModBlockEntities;

/**
 * 倒挂尸兄方块实体 —— 套路同 {@code MosquitoEggsBlockEntity}：
 * 控制器每次求值直接读方块状态，服务端翻转 {@code TONGUE} 属性后客户端自动切换动画。
 * <ul>
 *   <li>{@code TONGUE=false} —— {@code idle}：静止倒挂姿态（蜷着的舌头）</li>
 *   <li>{@code TONGUE=true} —— {@code tongue}：吐舌动画，播一遍并保持末帧
 *       （末帧为归零姿态，方块状态翻回 false 后切回 idle）</li>
 * </ul>
 */
public class HangUpsideDownZbrBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation TONGUE = RawAnimation.begin().thenPlayAndHold("tongue");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HangUpsideDownZbrBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HANG_UPSIDE_DOWN_ZBR, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<HangUpsideDownZbrBlockEntity>("main", 0, test -> {
            BlockState state = this.getBlockState();
            if (state.getBlock() instanceof HangUpsideDownZbrBlock
                    && state.getValue(HangUpsideDownZbrBlock.TONGUE)) {
                return test.setAndContinue(TONGUE);
            }
            return test.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
