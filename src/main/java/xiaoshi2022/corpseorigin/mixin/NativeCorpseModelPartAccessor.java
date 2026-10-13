package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public interface NativeCorpseModelPartAccessor {
    @Accessor("children") Map<String, ModelPart> corpseorigin$children();
    @Accessor("cubes") List<ModelPart.Cube> corpseorigin$cubes();
}
