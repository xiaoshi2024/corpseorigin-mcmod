package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Raise only health/attack capacity; default/base values of other entities stay unchanged. */
@Mixin(RangedAttribute.class)
public interface RealmAttributeRangeMixin {
    @Mutable @Accessor("maxValue") void corpseorigin$setMaximum(double maximum);
}
