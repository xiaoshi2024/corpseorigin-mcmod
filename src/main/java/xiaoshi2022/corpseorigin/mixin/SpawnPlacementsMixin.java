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
 * 打开时，把原版僵尸（含尸壳、村民僵尸）的<b>一切非人工生成</b>直接挡掉。
 * <p>
 * 这个判是<b>实时</b>的 —— 配置文件改完跑 {@code /corpseconfig reload} 或者
 * {@code /corpseconfig zombies on|off} 之后下个生成判定就生效，不用重启服务器。
 * <p>
 * <b>白名单放行</b>（人工来源，生成原因在 {@link #ALLOWED_REASONS} 里的照常过）：
 * <ul>
 *   <li>{@link EntitySpawnReason#SPAWN_EGG} / {@link EntitySpawnReason#SPAWNER_EGG} —— 手持/发射器刷怪蛋；</li>
 *   <li>{@link EntitySpawnReason#COMMAND} —— {@code /summon}；</li>
 *   <li>{@link EntitySpawnReason#MOB_SUMMONED} / {@link EntitySpawnReason#TRIGGERED} —— 代码触发式召唤；</li>
 *   <li>{@link EntitySpawnReason#CONVERSION} —— 村民被感染转化成僵尸村民（感染玩法核心，必须保留）。</li>
 * </ul>
 * <b>白名单之外一律拦截</b>。此前只拦 NATURAL/SPAWNER/TRIAL_SPAWNER，实际服务器上僵尸
 * 仍会从以下"漏网"路径刷出来（2026-10-08 服务器反馈"禁用后还在刷僵尸"的根因）：
 * <ul>
 *   <li>{@link EntitySpawnReason#REINFORCEMENT} —— 僵尸增援：攻击僵尸时概率刷出的支援僵尸；</li>
 *   <li>{@link EntitySpawnReason#STRUCTURE} —— 结构生成：失落城市（Lost Cities）建筑内自带的僵尸；</li>
 *   <li>{@link EntitySpawnReason#PATROL} / {@link EntitySpawnReason#RAID} / {@link EntitySpawnReason#EVENT} —— 袭击类事件。</li>
 * </ul>
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

    /** 人工来源白名单：这些生成原因不受"禁用原版僵尸"影响（26.2 实际枚举名） */
    private static final Set<EntitySpawnReason> ALLOWED_REASONS = Set.of(
            EntitySpawnReason.SPAWN_ITEM_USE,   // 手持刷怪蛋右键
            EntitySpawnReason.DISPENSER,        // 发射器发射刷怪蛋
            EntitySpawnReason.COMMAND,          // /summon
            EntitySpawnReason.MOB_SUMMONED,     // 代码召唤
            EntitySpawnReason.TRIGGERED,        // 代码触发式
            EntitySpawnReason.CONVERSION,       // 村民被感染转化成僵尸村民（感染玩法核心）
            EntitySpawnReason.BUCKET,           // 桶装生物（僵尸无桶，防误伤）
            EntitySpawnReason.LOAD,             // 从存档加载
            EntitySpawnReason.DIMENSION_TRAVEL  // 维度传送
    );

    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void corpseorigin$blockVanillaZombieSpawns(EntityType<?> type, ServerLevelAccessor level,
                                                              EntitySpawnReason reason, BlockPos pos,
                                                              RandomSource random,
                                                              CallbackInfoReturnable<Boolean> cir) {
        // 白名单之内（刷怪蛋/指令/召唤/感染转化）放行
        if (ALLOWED_REASONS.contains(reason)) {
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
