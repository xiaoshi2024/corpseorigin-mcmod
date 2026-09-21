package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.CorpseOriginClient;
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.model.entity.ShiChaoZhiZiBodyModel;
import xiaoshi2022.corpseorigin.client.skin.ShiChaoSkinBuilder;

public final class ShiChaoZhiZiBodyRenderer
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, AbstractClientPlayer, AvatarRenderState> {
    /** 兜底底图：组合纹理还没合成出来（或皮肤取不到）时先用它，不至于画出个没贴图的东西 */
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            CorpseOrigin.MOD_ID, "textures/entity/shichaozhizi.png");
    private static ShiChaoZhiZiBodyRenderer instance;

    private ShiChaoZhiZiBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new ShiChaoZhiZiBodyModel(), null);
        this.shadowRadius = 1.2F;
    }

    public static ShiChaoZhiZiBodyRenderer get() { return instance; }

    public static void createIfAbsent(EntityRendererProvider.Context context) {
        if (instance == null) instance = new ShiChaoZhiZiBodyRenderer(context);
    }

    public static void writeRenderData(AvatarRenderState state, AbstractClientPlayer player, float partialTick) {
        if (instance == null) return;
        // 整身纹理 = 底图 + 顶上那具人形叠玩家自己的皮肤（见 ShiChaoSkinBuilder）；
        // 合成失败就退回静态底图，这一帧照常出画。
        Identifier skinTexture = state.skin == null ? null : state.skin.body().texturePath();
        Identifier bodyTexture = ShiChaoSkinBuilder.acquire(skinTexture);
        state.addGeckolibData(ShiChaoBodyRenderData.BODY_TEXTURE,
                bodyTexture != null ? bodyTexture : TEXTURE);
        state.addGeckolibData(ShiChaoBodyRenderData.ACTIVE, true);
        // 「千眼万目」的凝视窗口一到，控制器就整段改播 special（见 ClientPlayerGeoAnimatableMixin）
        state.addGeckolibData(ShiChaoBodyRenderData.SPECIAL,
                CorpseOriginClient.isShiChaoSpecial(player.getUUID()));
        state.addGeckolibData(MutantBodyRenderData.MOVING, state.walkAnimationSpeed > 0.02F);
        state.addGeckolibData(LimbRenderData.ATTACKING, state.attackTime > 0.0F);
        state.addGeckolibData(DataTickets.PACKED_LIGHT, state.lightCoords);
        instance.extractRenderState(player, state, partialTick);
    }

    @Override
    public AvatarRenderState fillRenderState(PlayerGeoAnimatable animatable, AbstractClientPlayer entity,
                                             AvatarRenderState state, float partialTick) {
        return super.fillRenderState((PlayerGeoAnimatable) entity, entity, state, partialTick);
    }

    @Override
    public AvatarRenderState createRenderState(PlayerGeoAnimatable animatable, AbstractClientPlayer entity) {
        return super.createRenderState((PlayerGeoAnimatable) entity, entity);
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        Identifier texture = state.getGeckolibData(ShiChaoBodyRenderData.BODY_TEXTURE);
        return texture != null ? texture : TEXTURE;
    }

    @Override public RenderType getRenderType(AvatarRenderState state, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
