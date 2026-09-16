package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockManager;

/**
 * 获取式技能解锁的驱动。
 * <p>
 * 器官、宠物、背包物品这些来源随时可能变化（丢地上、宠物死了、东西给了别人），
 * 所以定期扫一遍来兜底；而"授予器官"这种明确动作会在指令里立即调用
 * {@link SkillUnlockManager#grantUnlocked}，玩家不用等下一个扫描周期。
 */
public final class SkillUnlockEvents {

    private SkillUnlockEvents() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                SkillUnlockManager.tick(player);
            }
        });
        CorpseOrigin.LOGGER.info("CorpseOrigin skill unlock events registered");
    }
}
