package xiaoshi2022.corpseorigin.skill.baixiaofei;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import xiaoshi2022.corpseorigin.registry.ModDataAttachments;
import xiaoshi2022.corpseorigin.skill.baixiaofei.aps.APSTerrainManager;

/**
 * 诗仙剑·连招模式：左键攻击推进段数
 */
public class APSComboHandler {

    // ✅ 5 分钟 = 5 * 60 * 20 = 6000 tick
    private static final long IDLE_TIMEOUT_TICKS = 6000L;        // 连招超时
    private static final long REALM_AUTO_RESTORE_TICKS = 6000L;  // 剑意自动消散

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

        // 每 tick 检查超时 + 剑意自动消散
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

        if (!active && !hasRealm) return;
        if (APSTerrainManager.isBusy(player)) return;

        long lastCast = state.getLong(AncientPoetrySwordSkill.CD_KEY).orElse(0L);
        long now = player.level().getGameTime();

        // ✅ 情况 1：连招开启 → 30 秒超时
        if (active) {
            if (now - lastCast <= IDLE_TIMEOUT_TICKS) return;

            state.putBoolean(AncientPoetrySwordSkill.ACTIVE_KEY, false);
            state.putInt(AncientPoetrySwordSkill.STAGE_KEY, 0);
            player.setAttached(ModDataAttachments.APS_STATE, state);
            player.sendOverlayMessage(Component.translatable(
                    "skill.corpseorigin.ancient_poetry_sword.timeout"));

            if (hasRealm) {
                APSTerrainManager.forceRestore(player, player.level());
            }
            return;
        }

        // ✅ 情况 2：剑意存在但连招关（第 4 段后）→ 60 秒自动消散
        if (hasRealm && now - lastCast > REALM_AUTO_RESTORE_TICKS) {
            player.sendOverlayMessage(Component.translatable(
                    "skill.corpseorigin.ancient_poetry_sword.auto_restore"));
            APSTerrainManager.forceRestore(player, player.level());
        }
    }

    private static final long MIN_STAGE_INTERVAL = 10L;  // 两段之间最少 10 tick

    private static void tryAdvance(ServerPlayer player) {
        ServerLevel level = player.level();
        long now = level.getGameTime();

        CompoundTag state = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();

        if (!state.getBoolean(AncientPoetrySwordSkill.ACTIVE_KEY).orElse(false)) {
            return;
        }

        long lastCast = state.getLong(AncientPoetrySwordSkill.CD_KEY).orElse(0L);

        // ✅ 防抖：两段之间最少间隔 10 tick
        if (now - lastCast < MIN_STAGE_INTERVAL) {
            return;
        }

        // 30 秒没左键 → 自动回收剑意
        if (now - lastCast > IDLE_TIMEOUT_TICKS) {
            state.putBoolean(AncientPoetrySwordSkill.ACTIVE_KEY, false);
            state.putInt(AncientPoetrySwordSkill.STAGE_KEY, 0);
            player.setAttached(ModDataAttachments.APS_STATE, state);
            player.sendOverlayMessage(Component.translatable(
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
                CompoundTag s = player.getAttachedOrCreate(ModDataAttachments.APS_STATE).copy();
                s.putBoolean(AncientPoetrySwordSkill.ACTIVE_KEY, false);
                s.putInt(AncientPoetrySwordSkill.STAGE_KEY, 0);
                player.setAttached(ModDataAttachments.APS_STATE, s);
            }
        }
    }
}