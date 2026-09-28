package xiaoshi2022.corpseorigin.client;

import net.minecraft.nbt.CompoundTag;

public class ClientCorpseData {
    public final boolean isCorpse;
    public final int corpseType;
    public final CompoundTag data;
    public ClientCorpseData(boolean isCorpse, int corpseType, CompoundTag data) {
        this.isCorpse = isCorpse;
        this.corpseType = corpseType;
        this.data = data;
    }
    public boolean isDisguised() { return data.getBoolean("is_disguised").orElse(false); }
    public boolean hasConsciousness() { return data.getBoolean("has_consciousness").orElse(false); }
    public int getExtraEyeCount() { return data.getInt("extra_eye_count").orElse(0); }
    public boolean hasWing() { return data.getBoolean("has_wing").orElse(false); }
    public boolean hasTail() { return data.getBoolean("has_tail").orElse(false); }
    public int getVariant() { return data.getInt("variant").orElse(0); }
    public boolean showsCorpseEye() {
        return isCorpse && !isDisguised() && !data.getBooleanOr("evolved_eye_hidden", false)
                && xiaoshi2022.corpseorigin.component.PlayerCorpseComponent.hasExoskeleton(getVariant());
    }
}
