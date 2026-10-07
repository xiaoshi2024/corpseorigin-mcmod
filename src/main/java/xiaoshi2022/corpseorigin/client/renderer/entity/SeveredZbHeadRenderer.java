package xiaoshi2022.corpseorigin.client.renderer.entity;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.skin.CombinedSkinBuilder;
import xiaoshi2022.corpseorigin.entity.SeveredZbHeadEntity;
import xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class SeveredZbHeadRenderer extends GeoEntityRenderer<SeveredZbHeadEntity, LivingEntityRenderState> {
    private static final com.geckolib.constant.dataticket.DataTicket<Identifier> TEXTURE =
            com.geckolib.constant.dataticket.DataTicket.create("severed_head_texture", Identifier.class);
    private final Map<Integer, Binding> bindings = new HashMap<>();
    private record Binding(Identifier skin, Identifier combined, int variant, boolean cracked, long seen) {}

    public SeveredZbHeadRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(CorpseOrigin.id("severed_zb_head")));
        shadowRadius = 0.2f;
    }
    @Override public void extractRenderState(SeveredZbHeadEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        String name = entity.skinName();
        Identifier skin = name.isEmpty() ? DefaultPlayerSkin.getDefaultTexture()
                : DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes())).body().texturePath();
        if (entity.level().getEntity(entity.sourceId()) instanceof LowerLevelZbEntity source
                && source.getSkinTexture() != null) skin = source.getSkinTexture();
        Identifier fallback = name.isEmpty() ? DefaultPlayerSkin.getDefaultTexture()
                : DefaultPlayerSkin.get(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes())).body().texturePath();
        int variant = !name.isEmpty() && skin.equals(fallback)
                && xiaoshi2022.corpseorigin.config.CorpseConfig.get().skin.tintUnresolvedSkins ? name.hashCode() : 0;
        long now = entity.level().getGameTime();
        Binding previous = bindings.get(entity.getId());
        if (previous == null || !previous.skin.equals(skin) || previous.variant != variant || previous.cracked != entity.isCracked()) {
            if (previous != null) CombinedSkinBuilder.release(previous.skin, previous.variant, previous.cracked);
            previous = new Binding(skin, CombinedSkinBuilder.acquire(skin, variant, entity.isCracked()), variant, entity.isCracked(), now);
        }
        bindings.put(entity.getId(), new Binding(previous.skin, previous.combined, previous.variant, previous.cracked, now));
        state.addGeckolibData(TEXTURE, previous.combined);
        if (now % 100 == 0) bindings.entrySet().removeIf(entry -> {
            if (now - entry.getValue().seen <= 100) return false;
            Binding old = entry.getValue();
            CombinedSkinBuilder.release(old.skin, old.variant, old.cracked);
            return true;
        });
    }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state) {
        Identifier texture = state.getGeckolibData(TEXTURE);
        return texture == null ? DefaultPlayerSkin.getDefaultTexture() : texture;
    }
}
