package xiaoshi2022.corpseorigin.registry;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.model.ExoskeletonModel;

public final class ModModelLayers {

    public static final ModelLayerLocation EXOSKELETON = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "exoskeleton"), "main"
    );

    public static void register() {
        ModelLayerRegistry.registerModelLayer(EXOSKELETON, ExoskeletonModel::createBodyLayer);
    }
}