package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterActorState;

/**
 * 变色龙尸兄：受到 9 点及以上伤害时被打回原形。
 * <p>
 * 只清伪装，<b>不挡伤害</b> —— 该掉多少血掉多少血。也不限次数，每次大伤害都会触发。
 */
public final class BianSeLongZbEventHandler {

    /** 触发“打回原形”的伤害门槛 */
    private static final float REVEAL_THRESHOLD = 9.0F;

    private BianSeLongZbEventHandler() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }
            if (blocked || damageTaken < REVEAL_THRESHOLD) {
                return;
            }
            // ★ 不再限制变色龙角色（2026-09-30）：任何角色伪装中受重击都会显形 —— 伪装通用机制
            // 必须正在伪装
            if (player.getAttachedOrCreate(ChapterActorState.DISGUISE).isEmpty()) {
                return;
            }

            reveal(player);
        });

        CorpseOrigin.LOGGER.info("BianSeLongZb events registered");
    }

    private static void reveal(ServerPlayer player) {
        // 恢复本体
        player.setAttached(ChapterActorState.DISGUISE, "");
        player.setAttached(ChapterActorState.DISGUISE_UNTIL, 0L);
        player.removeAttached(ChapterActorState.DISGUISE_PROFILE);

        // 反馈
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8F, 0.9F);
        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin.chameleon_disguise.revealed"));
    }
}