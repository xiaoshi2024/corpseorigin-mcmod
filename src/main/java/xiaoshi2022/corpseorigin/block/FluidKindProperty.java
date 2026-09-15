package xiaoshi2022.corpseorigin.block;

import net.minecraft.world.level.block.state.properties.Property;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 取值为 {@link FluidKind} 的方块状态属性 */
public final class FluidKindProperty extends Property<FluidKind> {

    private final List<FluidKind> values;
    private final Map<String, FluidKind> byName;

    public FluidKindProperty(String name, List<FluidKind> values) {
        super(name, FluidKind.class);
        this.values = List.copyOf(values);
        Map<String, FluidKind> map = new LinkedHashMap<>();
        for (FluidKind kind : this.values) {
            map.put(kind.name(), kind);
        }
        this.byName = Map.copyOf(map);
    }

    @Override
    public List<FluidKind> getPossibleValues() {
        return this.values;
    }

    @Override
    public String getName(FluidKind value) {
        return value.name();
    }

    @Override
    public Optional<FluidKind> getValue(String name) {
        return Optional.ofNullable(this.byName.get(name));
    }

    @Override
    public int getInternalIndex(FluidKind value) {
        return this.values.indexOf(value);
    }
}
