package com.xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

/**
 * ModelPart Accessor：暴露 cubes 和 children 字段用于 veins/ribs 叠加渲染。
 */
@Mixin(ModelPart.class)
public interface NativeCorpseModelPartAccessor {
    @Accessor("children")
    Map<String, ModelPart> corpseorigin$children();

    @Accessor("cubes")
    List<ModelPart.Cube> corpseorigin$cubes();
}
