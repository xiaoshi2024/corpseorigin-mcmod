package xiaoshi2022.corpseorigin.clone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.shell.ShellStateComponent;

import java.util.UUID;

/**
 * 克隆体状态（对应 Sync 的 ShellState，做了剧情化精简）。
 * <p>
 * 原著设定（百度百科·黑色火线 / 黑小飞）：
 * 黑小飞是天·博士用白小飞的基因在绿色培养液的克隆容器中培育出的克隆体，
 * 完成度只有 96%，缺失了白小飞的部分记忆，因意外光柱惊动而提前破仓苏醒。
 * <p>
 * 因此本类把"培育完成"阈值定为 {@link #COMPLETE_PROGRESS} = 0.96：
 * <ul>
 *     <li>progress 从 0 增长到 0.96：克隆体培育阶段（绿色培养液中）</li>
 *     <li>body：克隆完成时承载的"身体快照"（玩家完整 NBT：背包/血量/经验/状态效果）</li>
 *     <li>component：这具身体自己的尸兄状态 / 角色数据，随身体独立保存</li>
 *     <li>意识转换时，玩家意识载入 body，玩家原来的身体快照再存回仓中</li>
 * </ul>
 */
public class CloneState {
    /** 培育完成度：96% —— 致敬黑小飞 96% 完成度即提前苏醒的剧情 */
    public static final float COMPLETE_PROGRESS = 0.96F;

    private UUID owner;
    private String ownerName;
    private float progress;
    /** 克隆体的身体快照（玩家 NBT）；培育中为供体本体的快照，转换后变为原身体 */
    private CompoundTag body;
    /** 这具身体独立的尸兄状态 / 角色数据（ShellStateComponent 体系） */
    private ShellStateComponent component = ShellStateComponent.empty();
    /** 这具身体穿的四件盔甲（头/胸/腿/脚），用于克隆人渲染 */
    private java.util.List<net.minecraft.world.item.ItemStack> equipment = java.util.List.of();

    public CloneState(UUID owner, String ownerName, float progress, CompoundTag body,
                      ShellStateComponent component) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.progress = progress;
        this.body = body;
        if (component != null) {
            this.component = component;
        }
    }

    public UUID getOwner() {
        return this.owner;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public float getProgress() {
        return this.progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public boolean isReady() {
        return this.progress >= COMPLETE_PROGRESS;
    }

    public CompoundTag getBody() {
        return this.body;
    }

    public void setBody(CompoundTag body) {
        this.body = body;
    }

    /** 这具身体自己的尸兄状态 / 角色数据 */
    public ShellStateComponent getComponent() {
        return this.component;
    }

    public void setComponent(ShellStateComponent component) {
        if (component != null) {
            this.component = component;
        }
    }

    /** 这具身体穿的四件盔甲（头/胸/腿/脚） */
    public java.util.List<net.minecraft.world.item.ItemStack> getEquipment() {
        return this.equipment;
    }

    public void setEquipment(java.util.List<net.minecraft.world.item.ItemStack> equipment) {
        this.equipment = equipment == null ? java.util.List.of() : java.util.List.copyOf(equipment);
    }

    /** 写入磁盘存档（完整数据，包含可能很大的身体快照） */
    public void writeTo(ValueOutput out) {
        out.putString("Owner", this.owner.toString());
        if (this.ownerName != null) {
            out.putString("OwnerName", this.ownerName);
        }
        out.putFloat("Progress", this.progress);
        if (this.body != null) {
            out.store("Body", CompoundTag.CODEC, this.body);
        }
        if (this.component != null) {
            CompoundTag compTag = new CompoundTag();
            this.component.writeNbt(compTag);
            out.store("Component", CompoundTag.CODEC, compTag);
        }
        if (!this.equipment.isEmpty()) {
            out.store("Equipment", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.listOf(), this.equipment);
        }
    }

    /** 从磁盘存档或方块实体更新包读取（更新包里没有 Body 时身体快照为 null） */
    public static CloneState read(ValueInput in) {
        UUID owner = UUID.fromString(in.getStringOr("Owner", new UUID(0L, 0L).toString()));
        String ownerName = in.getString("OwnerName").orElse(null);
        float progress = in.getFloatOr("Progress", 0.0F);
        CompoundTag body = in.read("Body", CompoundTag.CODEC).orElse(null);
        ShellStateComponent component = ShellStateComponent.empty();
        in.read("Component", CompoundTag.CODEC).ifPresent(component::readNbt);
        CloneState state = new CloneState(owner, ownerName, progress, body, component);
        state.setEquipment(in.read("Equipment", net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.listOf())
                .orElse(java.util.List.of()));
        return state;
    }
}
