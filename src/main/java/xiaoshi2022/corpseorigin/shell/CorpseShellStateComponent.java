package xiaoshi2022.corpseorigin.shell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

public class CorpseShellStateComponent extends ShellStateComponent {

    private CompoundTag data = new CompoundTag();

    public CorpseShellStateComponent() {}

    public CorpseShellStateComponent(ServerPlayer player) {
        this.data = PlayerCorpseComponent.get(player).getDataPublic();
    }

    @Override public String getId() { return "corpseorigin:corpse"; }

    @Override
    public void clone(ShellStateComponent component) {
        CorpseShellStateComponent other = component.as(CorpseShellStateComponent.class);
        if (other != null) this.data = other.data.copy();
    }

    @Override public void writeNbt(CompoundTag tag) { tag.put("Data", this.data.copy()); }

    /** 这具身体的尸兄状态原始 NBT（渲染克隆人时用） */
    public CompoundTag getData() { return this.data; }

    @Override public void readNbt(CompoundTag tag) {
        this.data = tag.getCompound("Data").orElse(new CompoundTag());
    }

    @Override
    public void applyTo(ServerPlayer player) {
        PlayerCorpseComponent.get(player).readNbt(this.data);
    }
}