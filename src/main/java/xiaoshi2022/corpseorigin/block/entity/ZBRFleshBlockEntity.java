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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.registry.ModBlockEntities;

import java.util.UUID;

/**
 * 尸兄肉块方块实体 —— 只负责两件事：动画（呼吸脉动）与"自我吞噬"计数。
 * <p>
 * 计数攒到 {@link #REQUIRED_KILLS} 时，由 {@code ZBRFleshBlock#tick} 在原地生成尸巢结构。
 * <p>
 * 【移植说明】1.21.1 → 26.2：GeckoLib 4 的
 * {@code new AnimationController<>(this, this::handler)} 在 5.5.5 里变成
 * {@code new AnimationController<>(名字, 过渡tick, test -> ...)}，回调参数也从
 * {@code AnimationState} 换成了 {@code AnimationTest}。
 */
public class ZBRFleshBlockEntity extends BlockEntity implements GeoBlockEntity {

    /** idle：持续的呼吸脉动 */
    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("idle");

    /** 生成尸巢所需的"吞噬"次数 */
    public static final int REQUIRED_KILLS = 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int kills;
    private UUID owner;
    private boolean corpseNestGateway;
    private int structureVersion;

    public ZBRFleshBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZBR_FLESH, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ZBRFleshBlockEntity>("idle", 0,
                test -> test.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== 吞噬计数 / 主人 ====================

    public int getKills() {
        return this.kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
        setChanged();
    }

    public void addKills() {
        this.kills++;
        setChanged();
    }

    /** 尸巢主人（用于"非主人破坏就反击"）；没有被认领时返回 null */
    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public boolean isCorpseNestGateway() {
        return this.corpseNestGateway;
    }

    public void setCorpseNestGateway(boolean gateway) {
        this.corpseNestGateway = gateway;
        setChanged();
    }

    public int getStructureVersion() {
        return this.structureVersion;
    }

    public void setStructureVersion(int version) {
        this.structureVersion = version;
        setChanged();
    }

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        // 吞噬计数与主人必须存：不然区块一卸载，攒了一半的进度就白攒了
        out.putInt("Kills", this.kills);
        if (this.owner != null) {
            out.putString("Owner", this.owner.toString());
        }
        out.putBoolean("CorpseNestGateway", this.corpseNestGateway);
        out.putInt("StructureVersion", this.structureVersion);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.kills = in.getIntOr("Kills", 0);
        this.corpseNestGateway = in.getBooleanOr("CorpseNestGateway", false);
        this.structureVersion = in.getIntOr("StructureVersion", 0);

        String owner = in.getString("Owner").orElse("");
        if (!owner.isEmpty()) {
            try {
                this.owner = UUID.fromString(owner);
            } catch (IllegalArgumentException e) {
                this.owner = null;
            }
        }
    }
}
