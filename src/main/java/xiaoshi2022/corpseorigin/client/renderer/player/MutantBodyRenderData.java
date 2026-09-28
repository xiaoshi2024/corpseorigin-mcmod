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
 * 保存左护法变异体身体的渲染数据。
 * <p>
 * 变异形态由服务端状态同步，并缓存在
 * {@link CorpseOriginClient#corpseDataCache} 中，因此其他玩家也能看到相同外观。
 * 两个 ticket 都是每帧生成的临时数据：{@link #BODY_TEXTURE} 控制是否替换身体纹理，
 * {@link #ACTIVE} 为动画控制器提供形态状态。
 */
@Environment(EnvType.CLIENT)
public final class MutantBodyRenderData {

    /** 变异体身体纹理；为 null 时本帧不替换身体。 */
    public static final DataTicket<Identifier> BODY_TEXTURE =
            DataTicket.create("corpse_mutant_body_texture", Identifier.class);

    /** 本帧是否渲染变异体，由动画控制器据此选择动画。 */
    public static final DataTicket<Boolean> ACTIVE =
            DataTicket.create("corpse_mutant_body_active", Boolean.class);

    /** 玩家是否正在移动，用于切换待机和爬行动画。 */
    public static final DataTicket<Boolean> MOVING =
            DataTicket.create("corpse_mutant_body_moving", Boolean.class);

    /** 玩家是否浸在水中，用于播放游泳动画。 */
    public static final DataTicket<Boolean> SWIMMING =
            DataTicket.create("corpse_mutant_body_swimming", Boolean.class);

    /** 玩家是否正在潜行，用于控制探头动画。 */
    public static final DataTicket<Boolean> SNEAKING =
            DataTicket.create("corpse_mutant_body_sneaking", Boolean.class);

    private MutantBodyRenderData() {
    }

    /** 玩家当前是否为左护法变异体形态。 */
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
