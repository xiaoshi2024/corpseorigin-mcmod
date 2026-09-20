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
import xiaoshi2022.corpseorigin.client.limb.LimbRenderData;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.client.model.entity.ShiChaoZhiZiBodyModel;

public final class ShiChaoZhiZiBodyRenderer
        extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, AbstractClientPlayer, AvatarRenderState> {
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
        state.addGeckolibData(ShiChaoBodyRenderData.ACTIVE, true);
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

    @Override public Identifier getTextureLocation(AvatarRenderState state) { return TEXTURE; }
    @Override public RenderType getRenderType(AvatarRenderState state, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
