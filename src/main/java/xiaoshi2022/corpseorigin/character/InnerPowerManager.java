package xiaoshi2022.corpseorigin.character;

import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 内力管理器 - 服务端运行时内力值管理（不持久化，上线即满）。
 * <p>
 * 内力上限由角色的 {@link ICharacter#getMaxInnerPower()} 决定：
 * <ul>
 *   <li>0 → 无气感，不能施放需要内力的技能；气血不能代替内力。</li>
 *   <li>> 0 → 有内力，技能激活时消耗内力，每 tick 自然回复</li>
 * </ul>
 */
public final class InnerPowerManager {

    private InnerPowerManager() {
    }

    /** 每玩家当前内力值 */
    private static final Map<UUID, Integer> INNER_POWER = new HashMap<>();

    /** 每多少 tick 回复 1 点内力 */
    private static final int REGEN_INTERVAL = 20;

    /**
     * 获取玩家当前内力值。
     * 如果玩家是无内力角色或从未记录过，返回 0。
     */
    public static int getInnerPower(ServerPlayer player) {
        int max = getMaxInnerPower(player);
        if (max <= 0) {
            return 0;
        }
        return Math.min(max, INNER_POWER.getOrDefault(player.getUUID(), max));
    }

    /**
     * 获取玩家内力上限。
     */
    public static int getMaxInnerPower(ServerPlayer player) {
        return Math.max(CharacterManager.getInstance().getPlayerCharacter(player).getMaxInnerPower(),
                xiaoshi2022.corpseorigin.growth.FreeGrowth.innerPower(player));
    }

    /**
     * 消耗内力。
     *
     * @return true 表示内力足够并已扣除
     */
    public static boolean consume(ServerPlayer player, int amount) {
        if (amount < 0) return false;
        if (amount == 0) {
            return true;
        }
        int max = getMaxInnerPower(player);
        if (max <= 0) {
            return false;
        }
        int current = getInnerPower(player);
        if (current < amount) {
            return false;
        }
        INNER_POWER.put(player.getUUID(), current - amount);
        sync(player);
        return true;
    }

    /**
     * 设置内力值（ clamped 到 [0, max]）。
     */
    public static void set(ServerPlayer player, int value) {
        int max = getMaxInnerPower(player);
        if (max <= 0) {
            INNER_POWER.remove(player.getUUID());
            // ★ 无内力也要发一次（current=0, max=0）：
            //   HUD 完全靠这个包刷新，早退不发的话客户端还留着上一具身体的内力条 ——
            //   换身到无内力角色/身体时表现为"内力 HUD 没同步过来"。
            sync(player);
            return;
        }
        int clamped = Math.max(0, Math.min(max, value));
        INNER_POWER.put(player.getUUID(), clamped);
        sync(player);
    }

    /**
     * 回复内力（自然回复用，不超过上限）。
     */
    public static void regen(ServerPlayer player, int amount) {
        int max = getMaxInnerPower(player);
        if (max <= 0) {
            return;
        }
        int current = getInnerPower(player);
        int next = Math.min(max, current + amount);
        if (next != current) {
            INNER_POWER.put(player.getUUID(), next);
            sync(player);
        }
    }

    /**
     * 玩家切换角色时调用：重置内力为上限。
     */
    public static void reset(ServerPlayer player) {
        int max = getMaxInnerPower(player);
        if (max > 0) {
            INNER_POWER.put(player.getUUID(), max);
        } else {
            INNER_POWER.remove(player.getUUID());
        }
        sync(player);
    }

    /**
     * 服务端每 tick 调用：自然回复内力。
     */
    public static void tickRegen(ServerPlayer player) {
        if (xiaoshi2022.corpseorigin.skill.zhaoritian.TianGangKeySkill.isChanneling(player)) return;
        if (xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager.hasActiveAPS(player)) return;
        int max = getMaxInnerPower(player);
        if (max <= 0) {
            return;
        }
        if (player.tickCount % REGEN_INTERVAL != 0) {
            return;
        }
        regen(player, 1);
    }

    /** 玩家断开连接时清理缓存 */
    public static void cleanupDisconnect(UUID uuid) {
        INNER_POWER.remove(uuid);
    }

    /**
     * 把当前内力状态补发给客户端（登录、换身体这类"客户端缓存的还是旧值"的时机用）。
     */
    public static void syncTo(ServerPlayer player) {
        sync(player);
    }

    private static void sync(ServerPlayer player) {
        CorpseNetwork.sendInnerPowerSync(player);
    }
}
