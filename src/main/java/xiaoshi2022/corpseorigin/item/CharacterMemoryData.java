package xiaoshi2022.corpseorigin.item;

import net.minecraft.nbt.CompoundTag;

/** Decodes both shell snapshots ({Uuid, Data}) and older flat memory payloads. */
public final class CharacterMemoryData {
    private CharacterMemoryData() {}

    public static CompoundTag characterData(CompoundTag memory) {
        if (memory == null) return null;
        CompoundTag data = memory.contains("Data")
                ? memory.getCompound("Data").orElse(null) : memory;
        if (data == null || data.getString("CharacterId").filter(id -> !id.isBlank()).isEmpty()) {
            return null;
        }
        return data.copy();
    }
}
