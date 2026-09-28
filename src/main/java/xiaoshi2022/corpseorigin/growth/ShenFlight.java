package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.character.InnerPowerManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.longyou.BloodReserve;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 神级御空飞行（神·前期 = 进化 10 级解锁）。
 * <p>
 * 规则（刻意做得"久飞但不白嫖"）：
 * <ul>
 *   <li>10 级起所有角色获得原版创造式飞行许可（双击空格起飞）；</li>
 *   <li>实际升空（abilities.flying）期间才计费，悬停站立不扣 —— 每 tick 自然回 1/20 内力，
 *       飞行每秒净耗 1 点，神·前期自由角色 280 内力可连续飞约 4 分半，等级越高上限越大；</li>
 *   <li>无内力角色已在天级（9 级）统一觉醒气感，由 {@link InnerPowerManager} 管理；</li>
 *   <li>保留无内力状态的气血兜底：每秒扣气血储备，
 *       内力/气血任一见底都会强制落地 + 缓降，资源回到安全线以上才能再起飞，防止空槽抖动；</li>
 *   <li>翅膀飞行（{@link OrganEnergy}）激活时完全让位：同一时间只有一套系统计费，
 *       翅膀飞不了了只要神级资格还在，mayfly 不收走，无缝切回内力计费。</li>
 * </ul>
 */
public final class ShenFlight {

    private ShenFlight() {}

    /** 解锁御空的进化等级：神·前期 */
    public static final int SHEN_LEVEL = 10;

    /** 每多少 tick 结算一次飞行消耗（1 秒） */
    private static final int CHARGE_INTERVAL = 20;
    /** 每秒内力消耗（自然回复 1/秒，净耗 = 该值 - 1） */
    private static final int INNER_COST_PER_SECOND = 2;
    /** 无内力角色每秒消耗的气血 */
    private static final int BLOOD_COST_PER_SECOND = 3;
    /** 资源回到这条线以上才允许再次起飞（两种资源共用，数值含义不同但都很小） */
    private static final int REARM_THRESHOLD = 20;
    /** 强制落地后禁止再次起飞的 tick 数（和回能阈值双保险） */
    private static final int LOCKOUT_TICKS = 40;

    private record FlightState(boolean owned, boolean wasFlying, int chargeTick, int lockout) {
        FlightState billing() { return new FlightState(owned, wasFlying, (chargeTick + 1) % CHARGE_INTERVAL, lockout); }
        FlightState withCharge(int tick) { return new FlightState(owned, wasFlying, tick, lockout); }
        FlightState withOwned(boolean value) { return new FlightState(value, wasFlying, chargeTick, lockout); }
        FlightState withFlying(boolean value) { return new FlightState(owned, value, chargeTick, lockout); }
        FlightState withLockout(int value) { return new FlightState(owned, wasFlying, chargeTick, value); }
    }

    private static final Map<UUID, FlightState> STATES = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUUID();
            FlightState state = STATES.remove(uuid);
            // 断线时把可能还挂着的飞行许可收回来，避免重连后残留创造飞行
            if (state != null && state.owned() && !handler.player.isCreative() && !handler.player.isSpectator()) {
                var abilities = handler.player.getAbilities();
                abilities.flying = false;
                abilities.mayfly = false;
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> STATES.clear());
    }

    /** 玩家当前是否具备神级御空资格（翅膀系统借此判断要不要保留 mayfly）。 */
    public static boolean isAllowed(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
            return false;
        }
        int level = EvolutionManager.getLevel(
                PlayerCharacterData.get(player).getEarnedPoints(player.getUUID()));
        return level >= SHEN_LEVEL;
    }

    private static void tick(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            STATES.remove(player.getUUID());
            return;
        }
        FlightState state = STATES.computeIfAbsent(player.getUUID(),
                k -> new FlightState(false, false, 0, 0));
        if (state.lockout() > 0) {
            state = state.withLockout(state.lockout() - 1);
        }

        boolean allowed = isAllowed(player);

        // 首次解锁给一条提示（持久化标记，不刷屏）
        if (allowed) {
            noticeOnce(player);
        }

        // 翅膀正在计费时整套让位（含 mayfly 回收都交给 OrganEnergy）
        if (OrganEnergy.ownsFlight(player)) {
            STATES.put(player.getUUID(), state.withFlying(false).withOwned(false));
            return;
        }

        var abilities = player.getAbilities();

        if (!allowed) {
            if (state.owned()) {
                boolean falling = abilities.flying || state.wasFlying();
                abilities.flying = false;
                abilities.mayfly = false;
                player.onUpdateAbilities();
                if (falling) {
                    safeLanding(player, false);
                }
                state = state.withOwned(false);
            }
            STATES.put(player.getUUID(), state.withFlying(false));
            return;
        }

        boolean innerPower = InnerPowerManager.getMaxInnerPower(player) > 0;

        // 授予 / 维持飞行许可：刚被强制落地或资源还没回到安全线时先不给
        boolean enoughToTakeoff = innerPower
                ? InnerPowerManager.getInnerPower(player) >= REARM_THRESHOLD
                : BloodReserve.get(player) >= REARM_THRESHOLD;
        if (!abilities.mayfly && state.lockout() <= 0 && enoughToTakeoff) {
            abilities.mayfly = true;
            player.onUpdateAbilities();
            state = state.withOwned(true);
        }

        if (abilities.flying) {
            player.fallDistance = 0;
            state = state.withFlying(true);
            if (state.chargeTick() + 1 >= CHARGE_INTERVAL) {
                boolean paid = innerPower
                        ? InnerPowerManager.consume(player, INNER_COST_PER_SECOND)
                        : spendBlood(player, BLOOD_COST_PER_SECOND);
                if (!paid) {
                    // 资源见底：强制落地 + 缓降，进锁定冷却
                    abilities.flying = false;
                    abilities.mayfly = false;
                    player.onUpdateAbilities();
                    safeLanding(player, !innerPower);
                    state = state.withOwned(false).withLockout(LOCKOUT_TICKS);
                }
            }
            state = state.billing();
        } else {
            state = state.withFlying(false).withCharge(0);
        }

        STATES.put(player.getUUID(), state);
    }

    /** 扣气血储备（没有内力的尸兄角色御空时用）。 */
    private static boolean spendBlood(ServerPlayer player, int cost) {
        int current = BloodReserve.get(player);
        if (current < cost) {
            return false;
        }
        BloodReserve.add(player, -cost);
        return true;
    }

    /** 断飞保护：缓降 5 秒 + 资源见底提示。 */
    private static void safeLanding(ServerPlayer player, boolean blood) {
        if (!player.isAlive() || player.onGround() || player.isInWater()) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
        player.sendOverlayMessage(Component.translatable(blood
                ? "message.corpseorigin.shen_flight.blood_exhausted"
                : "message.corpseorigin.shen_flight.inner_exhausted"));
    }

    /** 御空首次解锁的一次性提示（写进持久化 growth journal）。 */
    private static void noticeOnce(ServerPlayer player) {
        var journal = player.getAttachedOrCreate(SurvivalGrowth.JOURNAL);
        if (journal.getBooleanOr("shen_flight_notice", false)) {
            return;
        }
        var copy = journal.copy();
        copy.putBoolean("shen_flight_notice", true);
        player.setAttached(SurvivalGrowth.JOURNAL, copy);
        player.sendSystemMessage(Component.translatable("message.corpseorigin.shen_flight.unlocked"));
    }
}
