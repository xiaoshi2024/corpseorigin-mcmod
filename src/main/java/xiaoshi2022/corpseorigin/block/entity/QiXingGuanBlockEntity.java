package xiaoshi2022.corpseorigin.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.block.QiXingGuanBlock;
import xiaoshi2022.corpseorigin.registry.ModBlockEntities;

/**
 * 七星棺方块实体 —— 封印尸王龙右的千年古尸棺。
 * <p>
 * 外观完全由 GeckoLib 渲染（方块本体 {@code RenderShape.INVISIBLE}）。
 * 方块状态 {@code SUMMONED=true}（开馆）时播放 {@code open} 开棺动画，
 * 并用 {@code thenPlayAndHold} 保持棺盖开启的末帧——封印已破，棺盖不再合上。
 */
public class QiXingGuanBlockEntity extends BlockEntity implements GeoBlockEntity {

    /**
     * 开馆动画：播放一次并保持在末帧（棺盖崩开后不再合上——封印已破）。
     * 动画文件里只有 {@code open} 一个 key（与 1.21.1 一致），未开馆时不播动画（绑定姿势=棺盖合上）。
     */
    private static final RawAnimation OPEN = RawAnimation.begin().thenPlayAndHold("open");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 首次落水记录的游戏时间（-1 = 未落水），用于自动开馆倒计时 */
    public long waterloggedSinceGameTime = -1;
    /** 沉棺事件（播报 + 染尸水）是否已触发过——只触发一次 */
    public boolean sinkingEventDone = false;

    public QiXingGuanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QI_XING_GUAN, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<QiXingGuanBlockEntity>("main", 0, test -> {
            BlockState state = this.getBlockState();
            if (state.getBlock() instanceof QiXingGuanBlock
                    && state.getValue(QiXingGuanBlock.SUMMONED)) {
                return test.setAndContinue(OPEN);
            }
            // 未开馆：不播动画（回绑定姿势，棺盖合着）
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putLong("WaterloggedSinceGameTime", this.waterloggedSinceGameTime);
        out.putBoolean("SinkingEventDone", this.sinkingEventDone);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.waterloggedSinceGameTime = in.getLongOr("WaterloggedSinceGameTime", -1L);
        this.sinkingEventDone = in.getBooleanOr("SinkingEventDone", false);
    }
}
