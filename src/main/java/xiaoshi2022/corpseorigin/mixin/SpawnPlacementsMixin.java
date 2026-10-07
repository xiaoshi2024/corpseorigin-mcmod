package xiaoshi2022.corpseorigin.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

/**
 * 拦截原版 {@link SpawnPlacements#checkSpawnRules}：当配置 {@code spawn.disableVanillaZombieSpawns}
 * 打开时，把原版僵尸（含尸壳、村民僵尸）的<b>自然生成 + 刷怪笼生成</b>直接挡掉。
 * <p>
 * 这个判是<b>实时</b>的 —— 配置文件改完跑 {@code /corpseconfig reload} 或者
 * {@code /corpseconfig zombies on|off} 之后下个生成判定就生效，不用重启服务器。
 * <p>
 * 拦两类来源：
 * <ul>
 *   <li>{@link EntitySpawnReason#NATURAL} —— 自然生成；</li>
 *   <li>{@link EntitySpawnReason#SPAWNER} / {@link EntitySpawnReason#TRIAL_SPAWNER} —— 刷怪笼 /
 *       试炼刷怪笼（26.2 里 {@code BaseSpawner.serverTick} 和 {@code TrialSpawner}
 *       在无自定义刷怪规则时都会调 {@code SpawnPlacements.checkSpawnRules} 做生成闸门）。</li>
 * </ul>
 * 刷怪蛋、{@code /summon} 等人工召唤照常放行，否则会破坏玩家自己的命令链路。
 * <p>
 * 模组自己的尸兄走的是 {@code ModSpawns} 注册的另一个 {@code SpawnPredicate}，
 * 不经过这个拦截点，所以"禁用僵尸"和"尸兄继续刷"互不影响。
 * <p>
 * 26.2 里 {@code EntityType} 不再常量持有 {@code ZOMBIE/HUSK/ZOMBIE_VILLAGER} 静态字段，
 * 所以这里按 {@link BuiltInRegistries#ENTITY_TYPE} 注册表里的 id path 比对。
 */
@Mixin(SpawnPlacements.class)
public class SpawnPlacementsMixin {

    /** 开关打开时被拦的原版僵尸 id path（zombified_piglin 在下界，不算"禁用僵尸"的语义范围） */
    private static final Set<String> CORPSE_ORIGIN$VANILLA_ZOMBIES = Set.of("zombie", "husk", "zombie_villager");

    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void corpseorigin$blockVanillaZombieSpawns(EntityType<?> type, ServerLevelAccessor level,
                                                              EntitySpawnReason reason, BlockPos pos,
                                                              RandomSource random,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (reason != EntitySpawnReason.NATURAL
                && reason != EntitySpawnReason.SPAWNER
                && reason != EntitySpawnReason.TRIAL_SPAWNER) {
            return;
        }
        if (!CorpseConfig.get().spawn.disableVanillaZombieSpawns) {
            return;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id != null && CORPSE_ORIGIN$VANILLA_ZOMBIES.contains(id.getPath())) {
            cir.setReturnValue(false);
        }
    }
}
