package xiaoshi2022.corpseorigin.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import xiaoshi2022.corpseorigin.entity.BloodWingBeamEntity;

/** Same tapered crescent geometry; color follows the caster's aura. */
public final class BloodWingBeamRenderer extends CrescentBeamRenderer<BloodWingBeamEntity> {
    public BloodWingBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
