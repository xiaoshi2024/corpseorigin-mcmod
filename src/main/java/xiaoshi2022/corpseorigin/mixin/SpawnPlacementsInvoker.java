package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 把 {@code SpawnPlacements.register} 接出来。
 * <p>
 * 26.2 里这个方法被改成了 {@code private}（原版只在自己的静态块里给原版生物注册），
 * 模组侧没有任何公开入口 —— 而<b>不注册</b>的话，{@code getPlacementType} 会退回
 * {@code NO_RESTRICTIONS}、{@code checkSpawnRules} 会直接返回 {@code true}，
 * 结果是这种怪白天也刷、还能刷在半空里。
 * <p>
 * 所以这里用 Mixin 的静态 {@link Invoker} 开一个访问器，由 {@code ModSpawns} 调用。
 */
@Mixin(SpawnPlacements.class)
public interface SpawnPlacementsInvoker {

    @Invoker("register")
    static <T extends Mob> void corpseorigin$register(EntityType<T> type, SpawnPlacementType placement,
                                                      Heightmap.Types heightmap,
                                                      SpawnPlacements.SpawnPredicate<T> predicate) {
        throw new AssertionError("SpawnPlacementsInvoker 没有被 Mixin 注入");
    }
}
