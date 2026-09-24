package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.ICharacter;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.EvolutionStats;
import xiaoshi2022.corpseorigin.skill.EvolutionTier;

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

            awardPoints(player, points);

//            CorpseOrigin.LOGGER.info("玩家 {}（{}）击杀 {}，获得 {} 进化点",
//                    player.getName().getString(),
//                    character.getId(),
//                    target.getName().getString(),
//                    points);
        });
    }

    /** 升级反馈：弹幕显示新阶层 + 音效 + 环绕粒子（跨越"人→地→天→神…"大境界时音效更隆重） */
    public static int awardPoints(ServerPlayer player, int requested) {
        PlayerCharacterData data = PlayerCharacterData.get(player);
        int earned = data.getEarnedPoints(player.getUUID());
        int points = xiaoshi2022.corpseorigin.growth.GrowthRules.reward(earned, requested);
        if (points <= 0) return 0;
        int before = EvolutionManager.getLevel(earned);
        data.addEarnedPoints(player.getUUID(), points);
        CorpseNetwork.sendEvolutionSync(player);
        if (EvolutionStats.reconcileAfterPointGain(player, before)) announceLevelUp(player);
        return points;
    }
    private static void announceLevelUp(ServerPlayer player) {
        PlayerCharacterData data = PlayerCharacterData.get(player);
        int level = EvolutionManager.getLevel(data.getEarnedPoints(player.getUUID()));
        String tierName = EvolutionTier.formatFullName(level);

        player.sendOverlayMessage(Component.translatable(
                "message.corpseorigin.evolution.levelup", tierName)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        boolean majorBreakthrough = EvolutionTier.fromAbsoluteLevel(level)
                != EvolutionTier.fromAbsoluteLevel(Math.max(1, level - 1));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                majorBreakthrough ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE
                                 : SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.0F, majorBreakthrough ? 1.0F : 1.3F);

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    24, 0.8, 1.0, 0.8, 0.05);
        }
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
