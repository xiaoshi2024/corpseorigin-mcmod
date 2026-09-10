package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;

/**
 * 进化点获得（所有角色通用）
 */
public final class EvolutionEventHandler {

    private EvolutionEventHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            Entity attacker = damageSource.getEntity();
            if (!(attacker instanceof ServerPlayer player)) return;
            if (!(entity instanceof LivingEntity target)) return;

            ICharacter character = CharacterManager.getInstance().getPlayerCharacter(player);
            if (character.isPassive()) return;

            int points = calcPoints(player, target, character);
            if (points <= 0) return;

            PlayerCharacterData data = PlayerCharacterData.get(player);
            data.addEarnedPoints(player.getUUID(), points);
            CorpseNetwork.sendEvolutionSync(player);

            CorpseOrigin.LOGGER.info("玩家 {}（{}）击杀 {}，获得 {} 进化点",
                    player.getName().getString(),
                    character.getId(),
                    target.getName().getString(),
                    points);
        });
    }

    /**
     * 根据击杀者和目标计算进化点
     */
    private static int calcPoints(ServerPlayer player, LivingEntity target, ICharacter character) {
        boolean isCorpsePlayer = PlayerCorpseComponent.isCorpse(player);

        // ===== 尸兄角色：吃同类少给，吃人类多给 =====
        if (isCorpsePlayer) {
            if (ZombieKin.isZombieKin(target)) return 1;
            if (target instanceof Player) return 3;
            if (target.getMaxHealth() >= 40) return 5;
            return 2;
        }

        // ===== 人类角色（白小飞/小鹿）：杀尸兄给点多 =====
        if (ZombieKin.isZombieKin(target)) {
            if (target.getMaxHealth() >= 40) return 5;  // 精英/尸王级
            return 3;                                    // 普通尸兄
        }

        // 人类杀普通生物，给少一点
        if (target instanceof Player) return 2;
        return 1;
    }
}