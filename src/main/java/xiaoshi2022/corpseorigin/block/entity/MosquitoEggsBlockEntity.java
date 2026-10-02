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
import xiaoshi2022.corpseorigin.block.MosquitoEggsBlock;
import xiaoshi2022.corpseorigin.registry.ModBlockEntities;

/**
 * 蚊子尸兄卵方块实体 —— 走 GeckoLib 的 {@link GeoBlockEntity}（套路同 {@code ZBRFleshBlockEntity}）。
 * <p>
 * 只负责动画：按方块的 {@code age} 分段播放
 * <ul>
 *   <li>age 0 —— {@code idle}（静置微动）</li>
 *   <li>age 1~2 —— {@code wriggle}（卵内幼虫扭动）</li>
 *   <li>age 3 —— {@code incubate}（临孵化蠕动，随后方块孵化成蚊子）</li>
 * </ul>
 * 方块状态由服务端 {@code MosquitoEggsBlock#tick} 推进，客户端 BE 的 state 会同步更新，
 * 控制器每次求值直接读 {@link #getBlockState()}，无需额外同步数据。
 */
public class MosquitoEggsBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WRIGGLE = RawAnimation.begin().thenLoop("wriggle");
    private static final RawAnimation INCUBATE = RawAnimation.begin().thenLoop("incubate");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MosquitoEggsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOSQUITO_ZBR_EGGS, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MosquitoEggsBlockEntity>("main", 0,
                test -> {
                    int age = this.getBlockState().getValue(MosquitoEggsBlock.AGE);
                    return test.setAndContinue(switch (age) {
                        case 0 -> IDLE;
                        case 1, 2 -> WRIGGLE;
                        default -> INCUBATE;
                    });
                }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
