package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.entity.ZombieKin;

/**
 * 尸族事件处理（服务端）
 */
public class ZombieKinEventHandler {

    public static void register() {
        // ==================== 1. 阻止尸族之间互相伤害（不饥饿时） ====================
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            Entity attacker = source.getEntity();

            if (attacker == null) return true;

            if (attacker instanceof LivingEntity livingAttacker) {
                // 目标是尸族 + 攻击者不能攻击 → 禁止伤害
                if (ZombieKin.isZombieKin(entity) && !ZombieKin.canAttack(livingAttacker, entity)) {
                    return false;
                }
            }

            return true;
        });

        // ==================== 2. 尸兄玩家攻击生物时涨饥饿值 ====================
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            Entity attacker = source.getEntity();

            // 只处理玩家攻击者
            if (!(attacker instanceof Player player)) return;

            // 只处理尸兄玩家
            if (!PlayerCorpseComponent.isCorpse(player)) return;

            // 目标必须是活体
            if (!(entity instanceof LivingEntity target)) return;

            // 计算饥饿值增益
            int gain;
            if (ZombieKin.isZombieKin(target)) {
                gain = 10;  // 同类相食，回复少
            } else {
                gain = 20;  // 正常食物
            }

            // 涨饥饿值
            PlayerCorpseComponent comp = PlayerCorpseComponent.get(player);
            int oldHunger = comp.getHunger();
            int newHunger = Math.min(100, oldHunger + gain);
            comp.setHunger(newHunger);

            CorpseOrigin.LOGGER.info("尸兄玩家 {} 进食：饥饿值 {} -> {}（+{}）",
                    player.getName().getString(), oldHunger, newHunger, gain);

            // ✅ 同步给客户端（更新饥饿值显示）
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                // 可选：发送饥饿值同步包
                // CorpseNetwork.sendPlayerCorpseSync(serverPlayer);
            }
        });
    }
}