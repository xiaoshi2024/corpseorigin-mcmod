package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.dataticket.DataTicket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

import java.util.UUID;

/**
 * 宸︽姢娉曘€屽彉寮備綋韬綋銆嶇殑娓叉煋鏁版嵁 / 鍒ゅ畾銆? * <p>
 * 褰㈡€佹湰韬槸鏈嶅姟绔姸鎬侊紙{@code PLAYER_CORPSE} 闄勪欢閲岀殑鍙樼鍙凤級锛岄殢
 * {@code PlayerCorpseSyncS2C} 骞挎挱鍒版墍鏈変汉锛屽鎴风浠? * {@link CorpseOriginClient#corpseDataCache} 璇?鈥斺€?鎵€浠?鑷繁"鍜?鍒汉鐪嬩綘"閮借兘鐪嬪埌鍚屼竴鍏峰彉寮備綋锛? * 涓嶉渶瑕侀澶栫殑缃戠粶鍖呫€? * <p>
 * 涓や釜 ticket 鏄瘡甯ч噸寤虹殑涓存椂鏁版嵁锛屽彧鍦ㄦ覆鏌撶绾块噷浼狅細
 * {@link #BODY_TEXTURE} 鍚屾椂鍏呭綋"杩欎竴甯ц涓嶈鏁磋韩鏇挎崲"鐨勫紑鍏筹紝{@link #ACTIVE} 缁欏姩鐢绘帶鍒跺櫒鍒囨祦娲剧敤銆? */
@Environment(EnvType.CLIENT)
public final class MutantBodyRenderData {

    /** 鏁磋韩绾圭悊锛坽@code zuo_guardian.png} 搴曞浘 + 楠戞墜澶勫彔鍔犵帺瀹剁毊鑲わ級锛涚己鐪?= 杩欎竴甯т笉鏇挎崲韬綋 */
    public static final DataTicket<Identifier> BODY_TEXTURE =
            DataTicket.create("corpse_mutant_body_texture", Identifier.class);

    /** 杩欎竴甯ф覆鏌撶殑鏄彉寮備綋 鈥斺€?鍔ㄧ敾鎺у埗鍣ㄦ嵁姝ゆ敼鎾?{@code zuo_guardian} 鑷繁鐨?idle / move / riderx_attack */
    public static final DataTicket<Boolean> ACTIVE =
            DataTicket.create("corpse_mutant_body_active", Boolean.class);

    /** 鐜╁鏄笉鏄湪绉诲姩 鈥斺€?椹卞姩 寰呮満 / 鐖 鍒囨崲锛堣繖濂楁ā鍨嬫病鏈?walk锛岀敤 reptile 褰?璧拌矾"锛?*/
    public static final DataTicket<Boolean> MOVING =
            DataTicket.create("corpse_mutant_body_moving", Boolean.class);

    /** 韬綋鏄笉鏄场鍦ㄦ按閲?鈥斺€?椹卞姩 娓稿姩锛坰wim锛?*/
    public static final DataTicket<Boolean> SWIMMING =
            DataTicket.create("corpse_mutant_body_swimming", Boolean.class);

    /** 鏄笉鏄湪娼滆锛堣共涓嬶級鈥斺€?椹卞姩 鎺㈠ご锛坮aised锛?*/
    public static final DataTicket<Boolean> SNEAKING =
            DataTicket.create("corpse_mutant_body_sneaking", Boolean.class);

    private MutantBodyRenderData() {
    }

    /** 杩欎綅鐜╁鐜板湪鏄笉鏄乏鎶ゆ硶鍙樺紓浣撳舰鎬侊紙閰嶇疆鍏虫帀鍙樺紓浣撳瑙傛椂涓€寰?false锛?*/
    public static boolean isMutantBody(UUID uuid) {
        if (uuid == null || !CorpseConfig.get().mutantBody.enabled) {
            return false;
        }
        xiaoshi2022.corpseorigin.client.ClientCorpseData data = CorpseOriginClient.corpseDataCache.get(uuid);
        return data != null && data.isCorpse && !data.isDisguised()
                && data.getVariant() == PlayerCorpseComponent.VARIANT_ZUO_GUARDIAN;
    }

    public static boolean isMutantBody(AbstractClientPlayer player) {
        return player != null && isMutantBody(player.getUUID());
    }
}
