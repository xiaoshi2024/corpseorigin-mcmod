package xiaoshi2022.corpseorigin.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.LongYou;
import xiaoshi2022.corpseorigin.entity.CloneAvatarEntity;
import xiaoshi2022.corpseorigin.entity.ZombieKin;

import java.util.List;
import java.util.Optional;

/**
 * 尸兄模组的进度（成就）触发器与解锁入口。
 * <p>
 * {@code data/corpseorigin/advancement/*.json} 引用下面这几个触发器 id。
 * 触发器本身不带条件（JSON 里不用写 conditions）——判定条件都在代码里：
 * <ul>
 *   <li>{@code corpseorigin:become_corpse} —— 变成尸兄（尸水感染 / 选到尸兄角色），
 *       统一汇到 {@code PlayerCorpseComponent#setPlayerAsCorpse} 里触发；</li>
 *   <li>{@code corpseorigin:cannibalism_discovery} —— 目睹尸族同类相残（见 {@link #init()}）；</li>
 *   <li>{@code corpseorigin:meet_corpse_king} —— 靠近"尸王"（见 {@link #init()}）。</li>
 * </ul>
 * ⚠️ 触发器必须在数据包加载之前注册好，所以 {@link #init()} 要写在模组初始化里 ——
 * 否则进度 JSON 会因为"未知的触发器"整份加载失败。
 */
public final class CorpseAdvancements {

    private CorpseAdvancements() {
    }

    /** "遇见尸王"的判定距离（格） */
    private static final double CORPSE_KING_RANGE = 50.0;
    /** "目睹同类相食"的判定距离（格）：发生在附近的玩家都算目睹 */
    private static final double CANNIBALISM_RANGE = 30.0;

    // ==================== 触发器 ====================

    /**
     * 通用触发器：条件恒真。
     * <p>
     * 四个成就"什么时候解锁、解锁给谁"都在代码里判定，数据包只负责展示，
     * 所以 JSON 里不需要写 conditions —— 结构沿用原版 {@code PlayerTrigger}。
     */
    public static class SimpleTrigger extends SimpleCriterionTrigger<SimpleTrigger.Instance> {

        @Override
        public Codec<Instance> codec() {
            return Instance.CODEC;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, instance -> true);
        }

        public record Instance(Optional<ContextAwarePredicate> player)
                implements SimpleCriterionTrigger.SimpleInstance {

            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                    ContextAwarePredicate.CODEC.optionalFieldOf("player").forGetter(Instance::player)
            ).apply(inst, Instance::new));
        }
    }

    public static final SimpleTrigger BECOME_CORPSE = register("become_corpse");
    public static final SimpleTrigger CANNIBALISM_DISCOVERY = register("cannibalism_discovery");
    public static final SimpleTrigger MEET_CORPSE_KING = register("meet_corpse_king");

    private static SimpleTrigger register(String name) {
        return Registry.register(BuiltInRegistries.TRIGGER_TYPES, CorpseOrigin.id(name), new SimpleTrigger());
    }

    // ==================== 跨实体的触发 ====================

    /**
     * 注册触发器（类加载即完成）+ 挂上两条需要"扫描/监听"的解锁规则。
     * <p>
     * 变成尸兄有明确的调用点（{@code PlayerCorpseComponent#setPlayerAsCorpse}），直接在那边触发。
     */
    public static void init() {
        // 初见尸王：每 20 tick 扫一次在线玩家
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) {
                return;
            }
            var players = server.getPlayerList().getPlayers();
            for (ServerPlayer viewer : players) {
                if (hasNearbyCorpseKingPlayer(viewer, players) || hasNearbyCorpseKingAvatar(viewer)) {
                    MEET_CORPSE_KING.trigger(viewer);
                }
            }
        });

        // 同类相食：尸族杀死尸族时，把成就发给 30 格内目睹的玩家
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level) || !ZombieKin.isZombieKin(entity)) {
                return;
            }
            Entity killer = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
            if (!ZombieKin.isZombieKin(killer)) {
                return;
            }
            for (ServerPlayer witness : level.getEntitiesOfClass(ServerPlayer.class,
                    entity.getBoundingBox().inflate(CANNIBALISM_RANGE))) {
                CANNIBALISM_DISCOVERY.trigger(witness);
            }
        });
    }

    /** 附近 50 格内有扮演龙右的玩家 */
    private static boolean hasNearbyCorpseKingPlayer(ServerPlayer viewer, List<ServerPlayer> players) {
        for (ServerPlayer king : players) {
            if (king == viewer || king.level() != viewer.level()) {
                continue;
            }
            if (LongYou.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(king))
                    && viewer.distanceTo(king) <= CORPSE_KING_RANGE) {
                return true;
            }
        }
        return false;
    }

    /**
     * 附近 50 格内有"龙右身体"的克隆分身。
     * <p>
     * 龙右的克隆分身也算尸王身体 —— 玩家自己培育一具龙右分身放在身边，照样算"遇见尸王"，
     * 单人模式下这也是唯一的解锁途径。
     */
    private static boolean hasNearbyCorpseKingAvatar(ServerPlayer viewer) {
        if (!(viewer.level() instanceof ServerLevel level)) {
            return false;
        }
        for (CloneAvatarEntity avatar : level.getEntitiesOfClass(CloneAvatarEntity.class,
                viewer.getBoundingBox().inflate(CORPSE_KING_RANGE))) {
            if (avatar.isCorpseKingBody()) {
                return true;
            }
        }
        return false;
    }
}
