package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.dataticket.DataTicket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.AbstractClientPlayer;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;

import java.util.UUID;

/**
 * 寮€鑳冨ザ銆岃儗鎸傘€嶏紙{@code niunaix}锛夌殑娓叉煋鏁版嵁 / 鍒ゅ畾銆?
 * <p>
 * 褰㈡€佹湰韬槸鏈嶅姟绔姸鎬侊紙{@code PLAYER_CORPSE} 闄勪欢閲岀殑鍙樼鍙凤級锛岄殢 {@code PlayerCorpseSyncS2C}
 * 骞挎挱鍒版墍鏈変汉锛屽鎴风浠?{@link CorpseOriginClient#corpseDataCache} 璇?鈥斺€?鎵€浠?鑷繁"鍜?
 * "鍒汉鐪嬩綘"鐪嬪埌鐨勬槸鍚屼竴濂楄儗鎸傦紝涓嶉渶瑕侀澶栫綉缁滃寘銆?
 * <p>
 * 涓変釜 ticket 閮芥槸姣忓抚閲嶅缓鐨勪复鏃舵暟鎹紝鍙湪娓叉煋绠＄嚎閲屼紶锛?
 * {@link #ACTIVE} 鍚屾椂鍏呭綋"杩欎竴甯ц涓嶈鐢昏儗鎸?鐨勫紑鍏充笌鍔ㄧ敾鎺у埗鍣ㄧ殑闂ㄦ帶锛堜笉鏄紑鑳冨ザ灏辨暣鏉″仠鎺夛紝
 * 鍏嶅緱 {@code idle}/{@code attack} 杩欎簺閲嶅悕鍔ㄧ敾娉勬紡鍒板埆鐨勬ā鍨嬩笂鍒锋棩蹇楋級锛?
 * {@link #ATTACKING} 椹卞姩灏栧埡锛寋@link #PARRYING} 椹卞姩鑿婅姳鐩炬牸鎸°€?
 */
@Environment(EnvType.CLIENT)
public final class NiunaiXRenderData {

    /** 杩欎竴甯ф覆鏌撶殑鏄紑鑳冨ザ鑳屾寕 鈥斺€?鍐冲畾鐢讳笉鐢伙紝涔熷喅瀹氬姩鐢绘帶鍒跺櫒褰掍笉褰掑畠绠?*/
    public static final DataTicket<Boolean> ACTIVE =
            DataTicket.create("corpse_niunaix_active", Boolean.class);

    /** 鐜╁姝ｅ湪鎸ュ嚮 鈥斺€?椹卞姩 {@code attack}锛堝皷鍒猴級 */
    public static final DataTicket<Boolean> ATTACKING =
            DataTicket.create("corpse_niunaix_attacking", Boolean.class);

    /** 鑿婅姳鐩炬牸鎸＄獥鍙ｅ唴 鈥斺€?椹卞姩 {@code parry}锛堣姳鐡ｅ紶寮€鎴愮浘锛?*/
    public static final DataTicket<Boolean> PARRYING =
            DataTicket.create("corpse_niunaix_parrying", Boolean.class);

    /**
     * 褰撳墠鏃堕棿鍩哄噯锛堝疄浣撳勾榫勶紝tick锛夈€?
     * <p>
     * 鎺у埗鍣ㄦ湰韬嬁涓嶅埌鏃堕挓锛岃€?{@code attack} 閭ｆ潯 clip锛? 绉掞級姣斿師鐗堜竴涓嬫尌鍑伙紙绾?6 tick锛夐暱寰楀锛?
     * 闇€瑕?鎾弧鍐嶅垏" 鈥斺€?闈犲畠绠楀嚭"绂昏繖娆℃尌鍑诲紑濮嬭繃浜嗗涔?銆傝
     * {@code ClientPlayerGeoAnimatableMixin#corpseorigin$niunai}銆?
     */
    public static final DataTicket<Float> AGE_TICKS =
            DataTicket.create("corpse_niunaix_age_ticks", Float.class);

    private NiunaiXRenderData() {
    }

    /** 杩欎綅鐜╁鐜板湪鏄笉鏄紑鑳冨ザ鑳屾寕褰㈡€侊紙灏稿厔 + 闈炰吉瑁?+ 鍙樼 4锛?*/
    public static boolean isNiunaiX(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        xiaoshi2022.corpseorigin.client.ClientCorpseData data = CorpseOriginClient.corpseDataCache.get(uuid);
        return data != null && data.isCorpse && !data.isDisguised()
                && data.getVariant() == PlayerCorpseComponent.VARIANT_NIUNAIX;
    }

    public static boolean isNiunaiX(AbstractClientPlayer player) {
        return player != null && isNiunaiX(player.getUUID());
    }
}
