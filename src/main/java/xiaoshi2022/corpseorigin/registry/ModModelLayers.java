package xiaoshi2022.corpseorigin.registry;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.client.model.ExoskeletonModel;

public final class ModModelLayers {

    public static final ModelLayerLocation EXOSKELETON = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "exoskeleton"), "main"
    );

    public static final ModelLayerLocation CLONE_DUMMY = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "clone_dummy"), "main"
    );

    public static void register() {
        ModelLayerRegistry.registerModelLayer(EXOSKELETON, ExoskeletonModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(CLONE_DUMMY, () -> {
            MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
            return LayerDefinition.create(mesh, 64, 64);
        });
    }
}
