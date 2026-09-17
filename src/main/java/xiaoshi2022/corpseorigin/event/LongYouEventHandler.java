package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.LongYou;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.registry.ModFluids;
import xiaoshi2022.corpseorigin.skill.longyou.WaterPollutionSkill;

/**
 * 龙右专属事件处理。
 * <p>
 * 目前承载「尸水之源」被动：走过的水源被污染成尸水。
 * 和黑金心脏一个套路 —— 要该玩家是龙右、且已学会这个技能才会触发；
 * 想要"是龙右就常驻"的话，把 {@link #hasWaterPollution} 里的 {@code hasLearned} 判断去掉即可。
 */
public final class LongYouEventHandler {

    /** 扫描间隔（tick）：走路时 4 次/秒足够跟上，不必每 tick 都扫 */
    private static final int SCAN_INTERVAL = 5;

    private LongYouEventHandler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                // 用玩家自己的 id 错开相位，避免所有龙右挤在同一 tick 里扫
                if ((player.tickCount + player.getId()) % SCAN_INTERVAL != 0) {
                    continue;
                }
                if (!hasWaterPollution(player)) {
                    continue;
                }
                polluteAround(player);
            }
        });

        CorpseOrigin.LOGGER.info("LongYou events registered");
    }

    /** 该玩家是否为已学会「尸水之源」的龙右 */
    private static boolean hasWaterPollution(ServerPlayer player) {
        if (!LongYou.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            return false;
        }
        return PlayerCharacterData.get(player)
                .hasLearned(player.getUUID(), WaterPollutionSkill.PATH);
    }

    /** 扫玩家脚下一小圈，把普通水源换成尸水源 */
    private static void polluteAround(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int changed = 0;

        int radius = WaterPollutionSkill.RADIUS;
        int vertical = WaterPollutionSkill.VERTICAL_RADIUS;

        for (int dx = -radius; dx <= radius && changed < WaterPollutionSkill.MAX_PER_SCAN; dx++) {
            for (int dz = -radius; dz <= radius && changed < WaterPollutionSkill.MAX_PER_SCAN; dz++) {
                for (int dy = -vertical; dy <= vertical && changed < WaterPollutionSkill.MAX_PER_SCAN; dy++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (polluteAt(level, pos)) {
                        changed++;
                    }
                }
            }
        }
    }

    /**
     * 把这一格的水源换成尸水源。
     *
     * @return true = 这格真的被污染了
     */
    private static boolean polluteAt(ServerLevel level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);

        // 只污染"水源"：流动水不用管，它上游的源被换掉之后自己会重新流成尸水
        if (!fluid.isSource()) {
            return false;
        }

        Fluid type = fluid.getType();
        if (type != Fluids.WATER && type != Fluids.FLOWING_WATER) {
            return false;   // 已经是尸水，或者是别的流体，别乱动
        }

        level.setBlock(pos, ModFluids.INFECTED_WATER.defaultFluidState().createLegacyBlock(),
                Block.UPDATE_ALL);
        return true;
    }
}
