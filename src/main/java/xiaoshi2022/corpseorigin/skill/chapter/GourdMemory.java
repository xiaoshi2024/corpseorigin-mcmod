package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.nbt.CompoundTag;
import java.util.Arrays;
import java.util.List;

/** Small deterministic save-format helper, shared by gameplay and round-trip tests. */
public final class GourdMemory {
    private GourdMemory() {}
    public static boolean knows(CompoundTag memory, GourdTrait trait) {
        return trait != null && memory.getCompound("Known").map(t -> t.getBooleanOr(trait.id, false)).orElse(false);
    }
    public static List<GourdTrait> known(CompoundTag memory) {
        return Arrays.stream(GourdTrait.values()).filter(t -> knows(memory, t)).toList();
    }
    public static GourdTrait selected(CompoundTag memory) {
        GourdTrait trait = GourdTrait.byId(memory.getStringOr("Selected", ""));
        return knows(memory, trait) ? trait : null;
    }
    public static CompoundTag learn(CompoundTag memory, GourdTrait trait) {
        CompoundTag result = memory.copy();
        CompoundTag known = result.getCompound("Known").orElseGet(CompoundTag::new).copy();
        known.putBoolean(trait.id, true);
        result.put("Known", known);
        if (selected(result) == null) result.putString("Selected", trait.id);
        return result;
    }
    public static CompoundTag cycle(CompoundTag memory, boolean backwards) {
        CompoundTag result = memory.copy();
        List<GourdTrait> list = known(memory);
        if (list.isEmpty()) { result.remove("Selected"); return result; }
        int index = list.indexOf(selected(memory));
        int next = index < 0 ? 0 : Math.floorMod(index + (backwards ? -1 : 1), list.size());
        result.putString("Selected", list.get(next).id);
        return result;
    }
}
