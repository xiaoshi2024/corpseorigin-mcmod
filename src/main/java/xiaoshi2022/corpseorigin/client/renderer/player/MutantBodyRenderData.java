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
 * 左护法「变异体身体」的渲染数据 / 判定。
 * <p>
 * 形态本身是服务端状态（{@code PLAYER_CORPSE} 附件里的变种号），随
 * {@code PlayerCorpseSyncS2C} 广播到所有人，客户端从
 * {@link CorpseOriginClient#corpseDataCache} 读 —— 所以"自己"和"别人看你"都能看到同一具变异体，
 * 不需要额外的网络包。
 * <p>
 * 两个 ticket 是每帧重建的临时数据，只在渲染管线里传：
 * {@link #BODY_TEXTURE} 同时充当"这一帧要不要整身替换"的开关，{@link #ACTIVE} 给动画控制器切流派用。
 */
@Environment(EnvType.CLIENT)
public final class MutantBodyRenderData {

    /** 整身纹理（{@code zuo_guardian.png} 底图 + 骑手处叠加玩家皮肤）；缺省 = 这一帧不替换身体 */
    public static final DataTicket<Identifier> BODY_TEXTURE =
            DataTicket.create("corpse_mutant_body_texture", Identifier.class);

    /** 这一帧渲染的是变异体 —— 动画控制器据此改播 {@code zuo_guardian} 自己的 idle / move / riderx_attack */
    public static final DataTicket<Boolean> ACTIVE =
            DataTicket.create("corpse_mutant_body_active", Boolean.class);

    /** 玩家是不是在移动 —— 驱动 待机 / 爬行 切换（这套模型没有 walk，用 reptile 当"走路"） */
    public static final DataTicket<Boolean> MOVING =
            DataTicket.create("corpse_mutant_body_moving", Boolean.class);

    /** 身体是不是泡在水里 —— 驱动 游动（swim） */
    public static final DataTicket<Boolean> SWIMMING =
            DataTicket.create("corpse_mutant_body_swimming", Boolean.class);

    /** 是不是在潜行（蹲下）—— 驱动 探头（raised） */
    public static final DataTicket<Boolean> SNEAKING =
            DataTicket.create("corpse_mutant_body_sneaking", Boolean.class);

    private MutantBodyRenderData() {
    }

    /** 这位玩家现在是不是左护法变异体形态（配置关掉变异体外观时一律 false） */
    public static boolean isMutantBody(UUID uuid) {
        if (uuid == null || !CorpseConfig.get().mutantBody.enabled) {
            return false;
        }
        CorpseOriginClient.ClientCorpseData data = CorpseOriginClient.corpseDataCache.get(uuid);
        return data != null && data.isCorpse && !data.isDisguised()
                && data.getVariant() == PlayerCorpseComponent.VARIANT_ZUO_GUARDIAN;
    }

    public static boolean isMutantBody(AbstractClientPlayer player) {
        return player != null && isMutantBody(player.getUUID());
    }
}
