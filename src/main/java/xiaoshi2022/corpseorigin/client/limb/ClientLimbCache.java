package xiaoshi2022.corpseorigin.client.limb;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.limb.LimbSlots;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 瀹㈡埛绔柇鑲㈢姸鎬佺紦瀛樸€? * <p>
 * 鏁版嵁鏉ユ簮灏辨槸鏈嶅姟绔凡缁忎笅鍙戠殑 PlayerCorpseSyncS2C锛堟暣浠?PLAYER_CORPSE tag锛夛紝
 * 鎵€浠ヤ笉闇€瑕侀澶栫殑缃戠粶鍖呫€傛湇鍔＄姣忔鐘舵€佸彉鍖栨墠鍙戜竴娆★紝涓棿鐨勪竴绉掗潬鏈湴 tick 澶栨帹琛ラ棿锛? * 閬垮厤鎸夊寘棰戠巼涓€璺充竴璺冲湴闀裤€? */
@Environment(EnvType.CLIENT)
public final class ClientLimbCache {

    /** 姣忛儴浣嶈繘搴﹀父閲忥細鍊?< 0 琛ㄧず"鏂簡浣嗕笉浼氳嚜鎰? */
    public static final float PERMANENT = -1.0F;
    /** 瀹屽ソ */
    public static final float INTACT = -2.0F;

    private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();

    private ClientLimbCache() {
    }

    /** 涓€娆℃覆鏌撶敤鐨勬柇鑲㈣鍥?*/
    public static final class Entry {
        private final CompoundTag tag;
        private final int mask;
        private final int[] remaining;
        private final int[] totals;
        private final long baseTick;

        private Entry(CompoundTag tag, int mask, int[] remaining, int[] totals, long baseTick) {
            this.tag = tag;
            this.mask = mask;
            this.remaining = remaining;
            this.totals = totals;
            this.baseTick = baseTick;
        }

        public int mask() {
            return mask;
        }

        /** 璇ラ儴浣嶆槸鍚︽柇浜?*/
        public boolean isSevered(int slot) {
            return (mask & (1 << slot)) != 0;
        }

        /** 鍐嶇敓杩涘害锛? = 鍒氭柇锛? = 闀垮ソ锛涜礋鏁拌 {@link #PERMANENT} / {@link #INTACT} */
        public float progress(int slot, float partialTick) {
            if (!isSevered(slot)) {
                return INTACT;
            }
            if (totals[slot] <= 0) {
                return PERMANENT;
            }
            float now = remaining[slot] - (currentTick() - baseTick) - partialTick;
            return Mth.clamp(1.0F - now / totals[slot], 0.0F, 1.0F);
        }
    }

    /**
     * 鍙栬鐜╁鐨勬柇鑲㈢姸鎬侊紱杩斿洖 null 琛ㄧず鎸夊師鐗堢帺瀹舵覆鏌撱€?     * <p>
     * 鍙湁鏈嶅姟绔啓浜?limb_mask 鐨勮韩浣撴墠浼氳蛋鏂偄妯″瀷 鈥斺€?鑰屾湇鍔＄鍙銆岄粦灏忛 + 灏稿厔銆嶅啓杩欎唤鏁版嵁锛?     * 鎵€浠ュ鎴风涓嶉渶瑕併€佷篃鏃犳硶鑷鍒ゆ柇瑙掕壊銆?     */
    public static Entry get(AbstractClientPlayer player) {
        UUID uuid = player.getUUID();
        xiaoshi2022.corpseorigin.client.ClientCorpseData data = CorpseOriginClient.corpseDataCache.get(uuid);
        if (data == null || !data.isCorpse || data.isDisguised()) {
            CACHE.remove(uuid);
            return null;
        }

        CompoundTag tag = data.data;
        if (tag.getByte("limb_mask").isEmpty()) {
            CACHE.remove(uuid);
            return null;
        }

        int mask = tag.getByteOr("limb_mask", (byte) 0) & LimbSlots.MASK_ALL;
        int[] remaining = tag.getIntArray("limb_regrow_ticks").orElse(null);
        int[] totals = tag.getIntArray("limb_regrow_totals").orElse(null);
        if (mask == 0 || remaining == null || totals == null
                || remaining.length < LimbSlots.COUNT || totals.length < LimbSlots.COUNT) {
            CACHE.remove(uuid);
            return null;
        }

        Entry cached = CACHE.get(uuid);
        // 姣忔鍚屾閮芥槸涓€涓柊 tag 瀹炰緥锛岀敤韬唤姣旇緝鍒ゆ柇鏈夋病鏈夋洿鏂?        if (cached == null || cached.tag != tag) {
            cached = new Entry(tag, mask, remaining, totals, currentTick());
            CACHE.put(uuid, cached);
        }
        return cached;
    }

    public static void clear() {
        CACHE.clear();
    }

    private static long currentTick() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }
}
