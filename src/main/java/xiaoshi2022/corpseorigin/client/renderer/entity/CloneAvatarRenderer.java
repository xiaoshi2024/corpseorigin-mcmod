package xiaoshi2022.corpseorigin.client.renderer.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;

import java.util.UUID;

public class CloneAvatarRenderer
        extends LivingEntityRenderer<CloneAvatarEntity, AvatarRenderState, PlayerModel> {

    public CloneAvatarRenderer(EntityRendererProvider.Context context, boolean slim) {
        super(context,
                new PlayerModel(context.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim),
                0.5F);
    }

    @Override
    public AvatarRenderState createRenderState() { return new AvatarRenderState(); }

    @Override
    public void extractRenderState(CloneAvatarEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = resolveSkin(entity.getOwnerUuid());
        state.isSpectator = false;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showCape = false;
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }

    private static PlayerSkin resolveSkin(UUID ownerUuid) {
        if (ownerUuid != null) {
            var conn = Minecraft.getInstance().getConnection();
            if (conn != null) {
                var info = conn.getPlayerInfo(ownerUuid);
                if (info != null) {
                    return info.getSkin();
                }
            }
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }
}
