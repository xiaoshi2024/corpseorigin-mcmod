package xiaoshi2022.corpseorigin.skill.baixiaofei;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager;

/**
 * 古仙剑·连招模式：左键攻击推进段数
 */
public class APSComboHandler {

    private static final long IDLE_TIMEOUT_TICKS = 600L;  // 30 秒

    public static void register() {
        // 左键攻击实体
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (player instanceof ServerPlayer sp) {
                tryAdvance(sp);
            }
            return InteractionResult.PASS;
        });

        // 左键攻击方块
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (player instanceof ServerPlayer sp) {
                tryAdvance(sp);
            }
            return InteractionResult.PASS;
        });

        // ✅ 每 tick 检查超时
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkTimeout(player);
            }
        });
    }

    private static void checkTimeout(ServerPlayer player) {
        CompoundTag state = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
        boolean active = state.getBoolean(AncientPoetrySwordSkill.ACTIVE_KEY).orElse(false);
        boolean hasRealm = APSTerrainManager.hasActiveAPS(player);

        // ✅ 没开连招，也没剑意 → 不检查
        if (!active && !hasRealm) {
            return;
        }

        // ✅ 正在展开/还原中 → 不检查
        if (APSTerrainManager.isBusy(player)) {
            return;
        }

        long lastCast = state.getLong(AncientPoetrySwordSkill.CD_KEY).orElse(0L);
        long now = player.level().getGameTime();

        if (now - lastCast <= IDLE_TIMEOUT_TICKS) {
            return;
        }

        // 关连招模式
        if (active) {
            state.putBoolean(AncientPoetrySwordSkill.ACTIVE_KEY, false);
            state.putInt(AncientPoetrySwordSkill.STAGE_KEY, 0);
            player.setAttached(ModDataAttachments.APS_STATE, state);
            player.sendSystemMessage(Component.translatable(
                    "skill.corpseorigin.ancient_poetry_sword.timeout"));
        }

        // 回收剑意
        if (hasRealm) {
            APSTerrainManager.forceRestore(player, player.level());
        }
    }

    private static void tryAdvance(ServerPlayer player) {
        ServerLevel level = player.level();
        long now = level.getGameTime();

        CompoundTag state = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();

        if (!state.getBoolean(AncientPoetrySwordSkill.ACTIVE_KEY).orElse(false)) {
            return;
        }

        long lastCast = state.getLong(AncientPoetrySwordSkill.CD_KEY).orElse(0L);

        // ✅ 30 秒没左键 → 自动回收剑意
        if (now - lastCast > IDLE_TIMEOUT_TICKS) {
            state.putBoolean(AncientPoetrySwordSkill.ACTIVE_KEY, false);
            state.putInt(AncientPoetrySwordSkill.STAGE_KEY, 0);
            player.setAttached(ModDataAttachments.APS_STATE, state);
            player.sendSystemMessage(Component.translatable(
                    "skill.corpseorigin.ancient_poetry_sword.timeout"));
            APSTerrainManager.forceRestore(player, level);
            return;
        }

        // 推进段数
        int stage = state.getInt(AncientPoetrySwordSkill.STAGE_KEY).orElse(0);
        stage = (stage % 4) + 1;
        state.putInt(AncientPoetrySwordSkill.STAGE_KEY, stage);
        state.putLong(AncientPoetrySwordSkill.CD_KEY, now);
        player.setAttached(ModDataAttachments.APS_STATE, state);

        switch (stage) {
            case 1 -> APSSubSkills.castZhaoCiBaiDi(player, level);
            case 2 -> APSSubSkills.castQianLiJiangLing(player, level);
            case 3 -> APSSubSkills.castLiangAnYuanSheng(player, level);
            case 4 -> {
                APSSubSkills.castQingZhouYiGuo(player, level);
                // 第 4 段后关闭连招模式，但保留剑意
                CompoundTag s = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
                s.putBoolean(AncientPoetrySwordSkill.ACTIVE_KEY, false);
                s.putInt(AncientPoetrySwordSkill.STAGE_KEY, 0);
                player.setAttached(ModDataAttachments.APS_STATE, s);
            }
        }
    }
}