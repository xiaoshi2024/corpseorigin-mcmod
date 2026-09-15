package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * 读取原版物品冷却表。
 * <p>
 * {@link ItemCooldowns} 是纯运行时数据（没有 save/load，不进玩家 NBT），
 * 只提供 addCooldown / removeCooldown / isOnCooldown，无法枚举。
 * 为了把冷却跟着身体搬走，这里开个只读口子。
 */
@Mixin(ItemCooldowns.class)
public interface ItemCooldownsAccessor {

    /** 键是冷却组 id（{@code ItemCooldowns#getCooldownGroup}），值是包私有的 CooldownInstance */
    @Accessor("cooldowns")
    Map<Identifier, ?> getCooldowns();

    @Accessor("tickCount")
    int getTickCount();
}
