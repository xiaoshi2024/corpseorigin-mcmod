package xiaoshi2022.corpseorigin.client.renderer.player;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.limb.PlayerGeoAnimatable;
import xiaoshi2022.corpseorigin.growth.EvolutionAppearance;
import xiaoshi2022.corpseorigin.growth.SurvivalGrowth;

/** Same additive Geo renderer path as Niunai, with a separate render snapshot. */
public final class EvolutionGeoRenderer extends GeoReplacedEntityRenderer<PlayerGeoAnimatable, AbstractClientPlayer, AvatarRenderState> {
    public static final DataTicket<AvatarRenderState> SNAPSHOT = DataTicket.create("evolution_snapshot", AvatarRenderState.class);
    public static final DataTicket<String> MODEL = DataTicket.create("evolution_model", String.class);
    public static final DataTicket<String> COLOR = DataTicket.create("evolution_color", String.class);
    public static final DataTicket<String> MOTION = DataTicket.create("evolution_motion", String.class);
    private static EvolutionGeoRenderer instance;
    private EvolutionGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new xiaoshi2022.corpseorigin.client.model.entity.EvolutionGeoModel(), null);
        shadowRadius = 0;
    }
    public static void create(EntityRendererProvider.Context context) { if (instance == null) instance = new EvolutionGeoRenderer(context); }
    public static EvolutionGeoRenderer get() { return instance; }
    @Override public long getInstanceId(PlayerGeoAnimatable animatable, AbstractClientPlayer entity) {
        // Separate controller manager from the body, armor and Niunai back mount.
        return xiaoshi2022.corpseorigin.client.limb.PlayerLayerAnimationCache.EVOLUTION;
    }
    public static void extract(AbstractClientPlayer player, AvatarRenderState parent, float partial) {
        var corpse = xiaoshi2022.corpseorigin.client.CorpseOriginClient.corpseDataCache.get(player.getUUID());
        var body = player.getAttachedOrCreate(SurvivalGrowth.BODY);
        if(xiaoshi2022.corpseorigin.client.render.layer.CustomOrganLayer.hasCustomFrames(parent))return;
        if (instance == null || parent.isInvisible || corpse == null || !corpse.isCorpse || corpse.isDisguised()
                || !(body.getBooleanOr("wings", false) || body.getBooleanOr("gills", false))) return;
        AvatarRenderState state = new AvatarRenderState();
        state.addGeckolibData(MODEL, EvolutionAppearance.model(body));
        state.addGeckolibData(COLOR, EvolutionAppearance.color(body));
        state.addGeckolibData(MOTION, player.isInWater() ? "swim"
                : !player.onGround() && !player.isPassenger() ? (player.isShiftKeyDown() ? "glide" : "fly")
                : player.isShiftKeyDown() ? "crouch" : "idle");
        state.addGeckolibData(DataTickets.PACKED_LIGHT, parent.lightCoords);
        instance.extractRenderState(player, state, partial);
        parent.addGeckolibData(SNAPSHOT, state);
    }
    @Override public AvatarRenderState fillRenderState(PlayerGeoAnimatable a, AbstractClientPlayer e, AvatarRenderState s, float p) {
        return super.fillRenderState((PlayerGeoAnimatable)e, e, s, p);
    }
    @Override public AvatarRenderState createRenderState(PlayerGeoAnimatable a, AbstractClientPlayer e) {
        return super.createRenderState((PlayerGeoAnimatable)e, e);
    }
    @Override public Identifier getTextureLocation(AvatarRenderState state) {
        return CorpseOrigin.id("textures/entity/evolution/" + state.getGeckolibData(COLOR) + ".png");
    }
}
