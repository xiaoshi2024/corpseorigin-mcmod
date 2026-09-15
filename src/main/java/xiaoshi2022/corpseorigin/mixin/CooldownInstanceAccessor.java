package xiaoshi2022.corpseorigin.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code ItemCooldowns.CooldownInstance} 是包私有嵌套类，外部拿不到类型，
 * 所以用 targets 字符串定位，只取需要的两个 int。
 */
@Mixin(targets = "net.minecraft.world.item.ItemCooldowns$CooldownInstance")
public interface CooldownInstanceAccessor {

    @Accessor("startTime")
    int getStartTime();

    @Accessor("endTime")
    int getEndTime();
}
