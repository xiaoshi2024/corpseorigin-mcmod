package xiaoshi2022.corpseorigin.client.model.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;

/**
 * 开胃奶「拦腰斩断」形态的玩家模型（{@code niunai_link_player}）。
 * <p>
 * geo / 动画直接取
 * {@code geckolib/models/entity/niunai_link_player.geo.json} 与
 * {@code geckolib/animations/entity/niunai_link_player.animation.json}（同一个 id 派生）。
 * <p>
 * 骨骼就是原版玩家那套（头 0/0、身 16/16、四肢 40/16 这类 UV 与原版一致），
 * 另加一根 {@code link} 骨挂"接回"用的绳子 —— 所以<b>只换形不换皮</b>，
 * 纹理直接用玩家本人皮肤（{@link AvatarRenderState#skin} 上现成就有，零延迟）。
 */
@Environment(EnvType.CLIENT)
public class NiunaiLinkPlayerModel extends DefaultedEntityGeoModel<PlayerGeoAnimatable> {

    public NiunaiLinkPlayerModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "niunai_link_player"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        if (renderState instanceof AvatarRenderState avatarState && avatarState.skin != null) {
            return avatarState.skin.body().texturePath();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
