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
 * 断肢形态的玩家模型：<b>只换形不换皮</b>，纹理直接用玩家本人皮肤。
 * <p>
 * 玩家自己的皮肤在 {@link AvatarRenderState#skin} 上现成就有（零延迟、不需要像 NPC 那样走异步皮肤链），
 * 所以这里不需要额外的 DataTicket。
 */
@Environment(EnvType.CLIENT)
public class CorpsePlayerGeoModel extends DefaultedEntityGeoModel<PlayerGeoAnimatable> {

    public CorpsePlayerGeoModel() {
        super(Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "corpse_player"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        if (renderState instanceof AvatarRenderState avatarState && avatarState.skin != null) {
            return avatarState.skin.body().texturePath();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
