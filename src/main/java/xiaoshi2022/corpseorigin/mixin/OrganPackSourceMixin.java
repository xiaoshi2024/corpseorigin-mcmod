package xiaoshi2022.corpseorigin.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Expose organ packs through the ordinary client resource pack selection/reload flow. */
@Mixin(FolderRepositorySource.class)
public abstract class OrganPackSourceMixin {
    @Shadow @Final private Path folder;
    @Inject(method="loadPacks",at=@At("TAIL"))
    private void corpseorigin$organPacks(Consumer<Pack> output, CallbackInfo ci) {
        Path root=FabricLoader.getInstance().getConfigDir().resolve("corpseorigin/organ");
        if (!folder.toAbsolutePath().normalize().equals(FabricLoader.getInstance().getGameDir().resolve("resourcepacks").toAbsolutePath().normalize())) return;
        try {
            Files.createDirectories(root);
            new FolderRepositorySource(root,PackType.CLIENT_RESOURCES,PackSource.DEFAULT,
                    new DirectoryValidator(path->false)).loadPacks(output);
        } catch(Exception e) { xiaoshi2022.corpseorigin.CorpseOrigin.LOGGER.warn("Cannot read organ resource packs",e); }
    }
}
