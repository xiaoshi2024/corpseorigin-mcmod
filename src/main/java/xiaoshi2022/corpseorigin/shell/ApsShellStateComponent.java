package xiaoshi2022.corpseorigin.shell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;

/**
 * 诗仙剑（APS）技能状态：跟着身体走，不再每次夺舍都沿用玩家当前那一份。
 * <p>
 * 数据源是玩家身上的 {@code corpseorigin:aps_state} 附件（技能阶段 / 连招计数等），
 * 这里只做快照与写回，所以新增技能状态字段不需要改这里。
 */
public class ApsShellStateComponent extends ShellStateComponent {

    private CompoundTag data = new CompoundTag();

    public ApsShellStateComponent() {}

    public ApsShellStateComponent(ServerPlayer player) {
        this.data = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
    }

    @Override
    public String getId() { return "corpseorigin:aps"; }

    @Override
    public void clone(ShellStateComponent component) {
        ApsShellStateComponent other = component.as(ApsShellStateComponent.class);
        if (other != null) {
            this.data = other.data.copy();
        }
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        tag.put("Data", this.data.copy());
    }

    @Override
    public void readNbt(CompoundTag tag) {
        this.data = tag.getCompound("Data").orElse(new CompoundTag());
    }

    @Override
    public void applyTo(ServerPlayer player) {
        player.setAttached(ModDataAttachments.APS_STATE, this.data.copy());
    }
}
