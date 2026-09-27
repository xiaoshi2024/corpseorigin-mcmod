package xiaoshi2022.corpseorigin.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import xiaoshi2022.corpseorigin.entity.JuQueBeamEntity;

/** Golden crescent shared by player and clone sword techniques. */
public final class JuQueBeamRenderer extends CrescentBeamRenderer<JuQueBeamEntity> {
    public JuQueBeamRenderer(EntityRendererProvider.Context context) {
        super(context,255,188,48);
    }
}
