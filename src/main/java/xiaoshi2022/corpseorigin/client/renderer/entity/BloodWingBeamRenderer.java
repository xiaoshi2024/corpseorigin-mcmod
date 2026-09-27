package xiaoshi2022.corpseorigin.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import xiaoshi2022.corpseorigin.entity.BloodWingBeamEntity;

/** Blood-red version of the same tapered crescent geometry. */
public final class BloodWingBeamRenderer extends CrescentBeamRenderer<BloodWingBeamEntity> {
    public BloodWingBeamRenderer(EntityRendererProvider.Context context) {
        super(context,255,35,60);
    }
}
