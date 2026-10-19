package com.xiaoshi2022.corpseorigin.mixin;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reuse the host's separate baby head/body transforms without mapped field-name reflection. */
@Mixin(AgeableListModel.class)
public interface NativeCorpseAgeableAccessor {
    @Invoker("headParts") Iterable<ModelPart> corpseorigin$headParts();
    @Invoker("bodyParts") Iterable<ModelPart> corpseorigin$bodyParts();
    @Accessor("scaleHead") boolean corpseorigin$scaleHead();
    @Accessor("babyYHeadOffset") float corpseorigin$babyYHeadOffset();
    @Accessor("babyZHeadOffset") float corpseorigin$babyZHeadOffset();
    @Accessor("babyHeadScale") float corpseorigin$babyHeadScale();
    @Accessor("babyBodyScale") float corpseorigin$babyBodyScale();
    @Accessor("bodyYOffset") float corpseorigin$bodyYOffset();
}
