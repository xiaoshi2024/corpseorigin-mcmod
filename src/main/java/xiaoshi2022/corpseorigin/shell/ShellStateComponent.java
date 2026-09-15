package xiaoshi2022.corpseorigin.shell;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ShellStateComponent {

    public abstract String getId();

    /** 组件里带的可掉落物品（可选） */
    public Collection<net.minecraft.world.item.ItemStack> getItems() {
        return List.of();
    }

    /** 组件里带的经验（可选） */
    public int getXp() { return 0; }

    /** 从另一个组件复制状态到当前组件 */
    public abstract void clone(ShellStateComponent component);

    /**
     * 把这具身体自带的状态应用回玩家实体（默认无操作）。
     * <p>
     * 需要写回实体的组件（尸兄状态、角色数据、技能状态……）覆盖这个方法，
     * 这样夺舍时只需统一调一次 {@code state.getComponent().applyTo(player)}。
     */
    public void applyTo(net.minecraft.server.level.ServerPlayer player) {
    }

    /** 把当前组件状态写进 NBT */
    public abstract void writeNbt(CompoundTag tag);

    /** 从 NBT 读回组件状态 */
    public abstract void readNbt(CompoundTag tag);

    public <T> T as(Class<T> type) {
        return type.isInstance(this) ? type.cast(this) : null;
    }

    // ==================== 工厂 ====================

    public static ShellStateComponent empty() {
        List<ShellStateComponent> list = new ArrayList<>();
        for (var factory : ShellStateComponentRegistry.getInstance().getValues()) {
            list.add(factory.empty());
        }
        return combine(list);
    }

    public static ShellStateComponent of(net.minecraft.server.level.ServerPlayer player) {
        List<ShellStateComponent> list = new ArrayList<>();
        for (var factory : ShellStateComponentRegistry.getInstance().getValues()) {
            list.add(factory.of(player));
        }
        return combine(list);
    }

    public static ShellStateComponent combine(Collection<ShellStateComponent> components) {
        return switch (components.size()) {
            case 0 -> EmptyComponent.INSTANCE;
            case 1 -> components.iterator().next();
            default -> new CombinedComponent(components);
        };
    }

    // ==================== 内置实现 ====================

    private static final class EmptyComponent extends ShellStateComponent {
        static final EmptyComponent INSTANCE = new EmptyComponent();
        @Override public String getId() { return "corpseorigin:empty"; }
        @Override public void clone(ShellStateComponent c) {}
        @Override public void writeNbt(CompoundTag tag) {}
        @Override public void readNbt(CompoundTag tag) {}
    }

    private static final class CombinedComponent extends ShellStateComponent {
        private final List<ShellStateComponent> components;

        CombinedComponent(Collection<ShellStateComponent> components) {
            this.components = List.copyOf(components);
        }

        @Override public String getId() { return "corpseorigin:combined"; }

        @Override
        public void applyTo(net.minecraft.server.level.ServerPlayer player) {
            for (ShellStateComponent inner : this.components) inner.applyTo(player);
        }

        @Override
        public void clone(ShellStateComponent c) {
            for (ShellStateComponent inner : this.components) inner.clone(c);
        }

        @Override
        public void writeNbt(CompoundTag tag) {
            for (ShellStateComponent inner : this.components) {
                CompoundTag child = new CompoundTag();
                inner.writeNbt(child);
                tag.put(inner.getId(), child);
            }
        }

        @Override
        public void readNbt(CompoundTag tag) {
            for (ShellStateComponent inner : this.components) {
                CompoundTag child = tag.getCompound(inner.getId()).orElse(new CompoundTag());
                inner.readNbt(child);
            }
        }

        @Override
        public <T> T as(Class<T> type) {
            for (ShellStateComponent inner : this.components) {
                T result = inner.as(type);
                if (result != null) return result;
            }
            return null;
        }
    }
}