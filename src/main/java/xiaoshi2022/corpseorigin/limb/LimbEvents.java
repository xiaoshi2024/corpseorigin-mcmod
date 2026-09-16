package xiaoshi2022.corpseorigin.limb;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import xiaoshi2022.corpseorigin.CorpseOrigin;

/**
 * 断肢 / 再生的服务端事件入口。
 */
public final class LimbEvents {

    private LimbEvents() {
    }

    public static void register() {
        // 扩展点：角色白名单 / 截断来源规则 / 再生策略
        LimbAccess.registerDefaults();
        LimbSeverRules.registerDefaults();
        LimbRegenProfiles.registerDefaults();

        // 伤害结算之后才判定截断：黑金心脏在 ALLOW_DAMAGE 里拦截致命伤害，别和它抢同一个事件
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!(entity instanceof ServerPlayer victim)) {
                return;
            }
            DismembermentLogic.tryDismember(victim, source, baseDamage, damageTaken);
        });

        LimbRegenTickHandler.register();

        CorpseOrigin.LOGGER.info("CorpseOrigin limb events registered");
    }
}
