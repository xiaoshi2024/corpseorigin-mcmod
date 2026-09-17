package xiaoshi2022.corpseorigin.limb;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

/**
 * 再生计时（每秒结算一次）。
 * <p>
 * 具体"长多快、拿什么当代价"全部交给 {@link LimbRegenProfiles} 选出的策略决定 ——
 * 普通尸兄按类型 / 进化等级并消耗饱食度，黑小飞的吸血鬼血脉走 {@link VampireRegenProfile}
 * 不消耗饱食度且更快。这个类只负责推进计时、写回、同步。
 * <p>
 * ⚠️ 饥饿值的坑：PlayerCorpseComponent 的饥饿是原版 foodLevel × 5，写回时 h/5 又是整数除法，
 * 所以"每秒 -1 尸兄饥饿"实际会变成每秒 -1 格原版饥饿（60 秒掉光三条命）。
 * 这里按"已再生时长"跨过 {@link LimbRegenProfile#hungerCostInterval} 的边界时扣 1 格。
 */
public final class LimbRegenTickHandler {

    /** 每秒结算一次 */
    private static final int INTERVAL = 20;

    private static int counter;

    private LimbRegenTickHandler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++counter < INTERVAL) {
                return;
            }
            counter = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(server, player);
            }
        });
    }

    private static void tickPlayer(MinecraftServer server, ServerPlayer player) {
        if (!LimbAccess.canDismember(player)) {
            return;
        }

        PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
        LimbState state = comp.readLimbs();
        if (!state.hasSevered()) {
            return;
        }

        LimbRegenProfile profile = LimbRegenProfiles.forPlayer(player);
        boolean blockedByHunger = !profile.hungerGate(player, comp);
        boolean costsHunger = profile.consumesHunger(player);
        int costInterval = Math.max(1, profile.hungerCostInterval(player));

        boolean changed = false;
        boolean hungerDrained = false;
        boolean finished = false;
        boolean anyGrowing = false;

        byte mask = state.mask();
        int[] regrow = state.regrowTicks().clone();
        int[] totals = state.totals().clone();
        int[] cooldowns = state.cooldowns().clone();

        for (int slot = 0; slot < LimbSlots.COUNT; slot++) {
            if (!LimbSlots.isSevered(mask, slot)) {
                if (cooldowns[slot] > 0) {
                    cooldowns[slot] = Math.max(0, cooldowns[slot] - INTERVAL);
                    changed = true;
                }
                continue;
            }
            if (regrow[slot] <= 0) {
                continue;   // REGROW_PERMANENT：不能自愈，等道具
            }
            if (blockedByHunger) {
                continue;   // 暂停，不回退
            }

            anyGrowing = true;
            int elapsedBefore = totals[slot] - regrow[slot];
            regrow[slot] -= INTERVAL;

            // 跨过消耗边界就扣 1 格；不依赖整除，自定义策略给任意间隔都成立
            if (costsHunger) {
                int elapsedAfter = totals[slot] - regrow[slot];
                if (elapsedAfter / costInterval > elapsedBefore / costInterval) {
                    hungerDrained = true;
                }
            }

            if (regrow[slot] <= 0) {
                regrow[slot] = LimbSlots.REGROW_PERMANENT;
                totals[slot] = 0;
                cooldowns[slot] = DismembermentLogic.VULNERABLE_TICKS;
                mask = LimbSlots.withoutSevered(mask, slot);
                finished = true;
                player.sendOverlayMessage(Component.translatable(
                        "limb.corpseorigin.regrown", LimbSlots.DISPLAY_NAMES[slot]));
            }
            changed = true;
        }

        if (hungerDrained) {
            comp.setHunger(comp.getHunger() - 5);
        }

        if (blockedByHunger && anyGrowing && server.getTickCount() % 100 == 0) {
            player.sendOverlayMessage(
                    Component.translatable("limb.corpseorigin.starving"));
        }

        if (!changed) {
            return;
        }

        LimbState updated = new LimbState(mask, regrow, totals, cooldowns);
        comp.writeLimbs(updated);
        DismembermentLogic.applyLimbEffects(player, updated);
        CorpseNetwork.broadcastPlayerCorpseSync(player);

        if (finished && player.level() instanceof ServerLevel level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
    }
}
