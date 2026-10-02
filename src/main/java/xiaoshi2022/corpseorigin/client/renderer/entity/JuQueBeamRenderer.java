package xiaoshi2022.corpseorigin.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import xiaoshi2022.corpseorigin.entity.JuQueBeamEntity;

/** Crescent shared by player and clone sword techniques; color follows the caster's aura. */
public final class JuQueBeamRenderer extends CrescentBeamRenderer<JuQueBeamEntity> {
    public JuQueBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
