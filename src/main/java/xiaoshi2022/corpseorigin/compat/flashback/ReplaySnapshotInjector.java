package xiaoshi2022.corpseorigin.compat.flashback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.client.OrganClient;
import xiaoshi2022.corpseorigin.growth.OrganLibrary;
import xiaoshi2022.corpseorigin.growth.SurvivalGrowth;
import xiaoshi2022.corpseorigin.network.CorpsePayloads;
import xiaoshi2022.corpseorigin.network.OrganEditorPayload;

import java.util.function.Consumer;

/**
 * Flashback 褰曞埗绔揩鐓х姸鎬佹敞鍏ャ€? * <p>
 * Flashback 鍦ㄥ綍鍒跺紑濮嬪強姣忎釜鍥炴斁鍧楄竟鐣岄兘浼氬啓涓€娆′笘鐣屽揩鐓э紝渚涘垵濮嬪姞杞?/ seek 鏃堕噸寤恒€? * 蹇収閲屽彧鏈夊師鐗堝疄浣撴暟鎹紙AddEntity + SynchedEntityData + 灞炴€?+ 瑁呭锛夛紝
 * <b>娌℃湁</b> Fabric 闄勪欢锛屼篃娌℃湁绗笁鏂规ā缁勭殑鐧诲綍鍚屾鍖咃紱鏈ā缁勯┍鍔ㄦ覆鏌撶殑涓夌被鏁版嵁鍥犳鍦ㄥ洖鏀句腑鍏ㄤ涪锛? * <ul>
 *   <li>{@code corpseDataCache}锛堝案鍏勫舰鎬?/ 鍙樼锛岄棬鎺у楠ㄩ銆佽繘鍖栬韩浣撴覆鏌擄級锛?/li>
 *   <li>{@code corpseorigin:evolution_parts} 闄勪欢锛堝櫒瀹樿閰嶆柟妗堛€佸櫒瀹橀樁娈点€亀ings/gills銆佽懌鑺︾姸鎬侊級锛?/li>
 *   <li>{@code OrganClient.catalog}锛堝櫒瀹樼洰褰曪紝杩涘洖鏀炬椂杩炴帴鏂紑杩樹細琚竻绌猴級銆?/li>
 * </ul>
 * Recorder#writeCustomSnapshot 鏄?Flashback 棰勭暀缁欐ā缁勭殑蹇収鎵╁睍鐐癸紙0.43.4 涓负绌哄疄鐜帮級锛? * 鎶婂綋鍓嶅鎴风宸叉湁鐨勬覆鏌撶姸鎬佸寘鎴愭櫘閫?S2C 鍖呬氦缁?consumer锛屽氨浼氶殢蹇収涓€璧峰啓鍏ュ綍鍍忥紱
 * 鍥炴斁绔?viewer 鐨?sendLevelInfo 闃舵杩欎簺鍖呬細缁?FlashbackRawCustomPayload 杩樺師骞堕噸鏂拌蛋
 * 妯＄粍鑷繁鐨勫鎴风鎺ユ敹鍣紝鏁版嵁鍗宠閲嶅缓銆? */
public final class ReplaySnapshotInjector {

    private ReplaySnapshotInjector() {
    }

    public static void inject(Consumer<Packet<? super ClientGamePacketListener>> out) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) return;
        try {
            // 1. 鍣ㄥ畼鐩綍鏄函瀹㈡埛绔厓鏁版嵁锛堟湇鍔＄ JOIN 鏃朵笅鍙戯級锛岃繘鍏ュ洖鏀捐繛鎺ユ椂浼氳 DISCONNECT 娓呯┖
            if (!OrganClient.catalog.isEmpty()) {
                out.accept(new ClientboundCustomPayloadPacket(
                        new OrganEditorPayload.Catalog(OrganLibrary.JSON.toJson(OrganClient.catalog))));
            }

            // 2. CharacterSyncS2C 鍙惡甯︽湰鍦扮帺瀹惰嚜宸辩殑瑙掕壊锛堝鎴风缂撳瓨鏄崟鍊硷級
            String characterId = CharacterManager.getInstance().getClientCachedCharacterId();
            if (characterId != null && !characterId.isEmpty()) {
                out.accept(new ClientboundCustomPayloadPacket(new CorpsePayloads.CharacterSyncS2C(characterId)));
            }

            // 3. 涓庡揩鐓х浉鍚岀殑瀹炰綋闆嗗悎锛氬案鍏勭紦瀛橈紙鍚垎韬瓑闈炵帺瀹跺疄浣擄級+ 鐜╁鐨?evolution_parts 闄勪欢
            for (Entity entity : level.entitiesForRendering()) {
                xiaoshi2022.corpseorigin.client.ClientCorpseData corpse =
                        CorpseOriginClient.corpseDataCache.get(entity.getUUID());
                if (corpse != null) {
                    out.accept(new ClientboundCustomPayloadPacket(new CorpsePayloads.PlayerCorpseSyncS2C(
                            entity.getUUID(), corpse.isCorpse, corpse.corpseType, corpse.data)));
                }
                if (entity instanceof Player player) {
                    CompoundTag body = player.getAttachedOrCreate(SurvivalGrowth.BODY);
                    if (!body.isEmpty()) {
                        out.accept(new ClientboundCustomPayloadPacket(
                                new CorpsePayloads.ReplayPlayerBodyS2C(player.getUUID(), body.copy())));
                    }
                }
            }
        } catch (Exception e) {
            // 蹇収娉ㄥ叆澶辫触鍙奖鍝嶅洖鏀惧瑙傦紝缁濅笉鑳借褰曞埗娴佺▼宕╂帀
            CorpseOrigin.LOGGER.warn("Flashback snapshot injection failed", e);
        }
    }
}
