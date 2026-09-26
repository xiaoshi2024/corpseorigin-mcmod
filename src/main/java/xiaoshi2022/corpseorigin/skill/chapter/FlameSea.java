package xiaoshi2022.corpseorigin.skill.chapter;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseFireBlock;

/**
 * 「烈焰火海」铺下的那一层火。
 * <p>
 * 用的是原版火焰（外观、灼烧、光照都对），但<b>不许它蔓延</b>：这些位置登记在这里，
 * {@code ServerLevel#canSpreadFireAround} 对它们一律返回 {@code false}
 * （见 {@code ServerLevelFireSpreadMixin}），所以火不会点着旁边的草、木头、房子。
 * <p>
 * 代价是 {@code FireBlock.tick} 在那一句就提前返回、火不再自然烧尽 —— 所以由 {@link #tick}
 * 到点熄灭：火海是"烧一阵就灭"的战场效果，不该在世界里留下永久的火墙。
 */
public final class FlameSea {

    /** 一处火持续多久（tick）后自行熄灭。 */
    public static final int LIFETIME_TICKS = 100;

    /** 关卡 →（火方块位置 → 熄灭时刻的游戏刻）。 */
    private static final Map<ServerLevel, Map<Long, Long>> FIRES = new IdentityHashMap<>();

    private FlameSea() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FlameSea::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FIRES.clear());
    }

    /**
     * 在这一格铺一把火，并登记它的熄灭时刻。
     * <p>
     * 放不住火（底下是空气之类）就什么也不做 —— 先让原版的 {@code canSurvive} 判一次，
     * 不会留下违规的火焰方块。
     */
    public static void place(ServerLevel level, BlockPos pos) {
        var fire = BaseFireBlock.getState(level, pos);
        if (!fire.canSurvive(level, pos)) return;
        level.setBlockAndUpdate(pos, fire);
        FIRES.computeIfAbsent(level, key -> new HashMap<>())
                .put(pos.asLong(), level.getGameTime() + LIFETIME_TICKS);
    }

    /** 这一格的火是不是"火海"铺的 —— 是的话它不参与蔓延。 */
    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        Map<Long, Long> fires = FIRES.get(level);
        return fires != null && fires.containsKey(pos.asLong());
    }

    /** 到点的火熄灭并清掉登记。 */
    private static void tick(MinecraftServer server) {
        if (FIRES.isEmpty()) return;
        for (Map.Entry<ServerLevel, Map<Long, Long>> entry : FIRES.entrySet()) {
            ServerLevel level = entry.getKey();
            long now = level.getGameTime();
            var fires = entry.getValue().entrySet().iterator();
            while (fires.hasNext()) {
                var fire = fires.next();
                if (fire.getValue() > now) continue;
                BlockPos pos = BlockPos.of(fire.getKey());
                // 只熄自己那层火：中途有人在这一格点了别的火，别替人家灭掉
                if (level.getBlockState(pos).getBlock() instanceof BaseFireBlock) {
                    level.removeBlock(pos, false);
                }
                fires.remove();
            }
        }
    }
}
