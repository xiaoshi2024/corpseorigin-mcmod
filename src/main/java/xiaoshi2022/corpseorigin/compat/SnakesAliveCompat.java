package xiaoshi2022.corpseorigin.compat;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xiaoshi2022.corpseorigin.config.CorpseConfig;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 与 <b>Snakes Alive</b>（模组 id {@code snakesalive}）的软联动：借它家的蛇来做「嘴里吐蛇」。
 * <p>
 * 为什么是软联动而不是硬依赖：
 * <ul>
 *   <li>没有编译期依赖 —— 只按<b>注册名</b>从实体注册表里取它家的蛇（{@code snakesalive:king_snake} 之类），
 *       没装那个模组时取不到，返回 false，技能自动退回内置的贯穿表现；</li>
 *   <li>认主也只用<b>原版</b> {@link TamableAnimal} 的 API。它们的蛇正好就是 {@code TamableAnimal}
 *       且没有重写 {@code isTame()}，所以原版这一套喂进去，它自家的
 *       {@code SnakeFollowOwnerGoal} / {@code SnakeDefendPlayerGoal} 就会接管 ——
 *       跟着你、替你咬人，全不用我们操心。</li>
 * </ul>
 * 于是"金旒龙"这条蛇我们<b>一行模型都不用做</b>，借它家的真实体就行。
 */
public final class SnakesAliveCompat {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Snakes Alive 的模组 id / 实体命名空间 */
    public static final String MOD_ID = "snakesalive";

    /** 上次吐出来的蛇：玩家 → 实体 id。再吐一条时把旧的收回来，免得满地都是蛇 */
    private static final Map<UUID, Integer> LAST_SPIT = new ConcurrentHashMap<>();

    /** 名字填错时只提醒一次，别每 10 秒刷一行日志 */
    private static boolean warnedUnknownSpecies = false;

    private SnakesAliveCompat() {
    }

    /** 那个模组在不在 */
    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /**
     * 朝玩家视线方向吐出一条 Snakes Alive 的蛇，并让它认玩家为主。
     *
     * @return 真的吐出来了才返回 true（没装模组 / 关掉了联动 / 物种名填错都返回 false）
     */
    public static boolean spitSnake(ServerPlayer player, Vec3 look) {
        if (!isLoaded() || !CorpseConfig.get().compat.mouthSnakeUsesSnakesAlive) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }

        EntityType<?> type = resolveSpecies(CorpseConfig.get().compat.mouthSnakeSpecies);
        if (type == null) {
            return false;
        }

        Entity snake = type.create(level, EntitySpawnReason.TRIGGERED);
        if (snake == null) {
            return false;
        }

        // 从"嘴"的位置朝视线方向甩出去：给一点初速，落点就在视线前方
        Vec3 spawn = player.getEyePosition().add(look.scale(0.6));
        snake.setPos(spawn.x, spawn.y, spawn.z);
        snake.setYRot(player.getYRot());
        snake.setXRot(player.getXRot());
        if (snake instanceof LivingEntity living) {
            living.setYHeadRot(player.getYRot());
            living.setYBodyRot(player.getYRot());
        }
        snake.setDeltaMovement(look.scale(1.1D).add(0.0D, 0.15D, 0.0D));

        // 认主：Snakes Alive 的蛇是原版 TamableAnimal（且没重写 isTame），
        // 所以喂原版这套即可，它自家的"跟随主人 / 护主"goal 会接手
        if (snake instanceof TamableAnimal tameable) {
            tameable.setTame(true, false);
            tameable.setOwner(player);
            tameable.setOrderedToSit(false);
        }

        if (!level.addFreshEntity(snake)) {
            return false;
        }

        // 同一玩家只留一条：把上一次吐出来的收回来（"金旒龙回口"）
        Integer previous = LAST_SPIT.put(player.getUUID(), snake.getId());
        if (previous != null && previous != snake.getId()) {
            Entity old = level.getEntity(previous);
            if (old != null && old.isAlive() && old instanceof TamableAnimal) {
                old.discard();
            }
        }
        return true;
    }

    /** 玩家下线时清掉记录（那条蛇由它自己的生命周期管，不用我们动） */
    public static void forget(UUID playerUuid) {
        LAST_SPIT.remove(playerUuid);
    }

    /** 按注册名取它家的蛇；取不到（没装 / 名字写错）返回 null */
    @Nullable
    private static EntityType<?> resolveSpecies(String species) {
        Optional<EntityType<?>> found = BuiltInRegistries.ENTITY_TYPE.getOptional(
                Identifier.fromNamespaceAndPath(MOD_ID, species));
        if (found.isPresent()) {
            return found.get();
        }
        if (!warnedUnknownSpecies) {
            warnedUnknownSpecies = true;
            LOGGER.warn("Snakes Alive 里没有物种 '{}'（配置 compat.mouthSnakeSpecies）；"
                    + "名字见它家 lang 里的 entity.snakesalive.*，本次不吐蛇", species);
        }
        return null;
    }
}
