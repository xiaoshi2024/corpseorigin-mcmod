package xiaoshi2022.corpseorigin.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.HeiXiaoFei;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.skill.heixiaofei.BlackGoldHeartSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 黑小飞专属事件处理
 * <p>
 * 目前只承载「黑金心脏」被动：致命伤害时锁血并震退周围敌人。
 * 需要该玩家是黑小飞、且在技能树中已学会黑金心脏才会触发。
 */
public final class HeiXiaoFeiEventHandler {

    private HeiXiaoFeiEventHandler() {
    }

    /** 玩家 → 锁血冷却结束时间戳（毫秒） */
    private static final Map<UUID, Long> LOCK_COOLDOWNS = new HashMap<>();

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return true;
            }
            if (!hasBlackGoldHeart(player)) {
                return true;
            }
            // 不致命 → 放行
            if (player.getHealth() - amount > 0.0F) {
                return true;
            }
            // 冷却中 → 放行（正常受死）
            long now = System.currentTimeMillis();
            if (LOCK_COOLDOWNS.getOrDefault(player.getUUID(), 0L) > now) {
                return true;
            }

            lockHealth(player, now);
            return false;   // 取消这次致命伤害
        });

        CorpseOrigin.LOGGER.info("HeiXiaoFei events registered");
    }

    /** 该玩家是否为已学会黑金心脏的黑小飞 */
    private static boolean hasBlackGoldHeart(ServerPlayer player) {
        if (!HeiXiaoFei.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            return false;
        }
        return PlayerCharacterData.get(player)
                .hasLearned(player.getUUID(), BlackGoldHeartSkill.PATH);
    }

    /** 锁血：保留 1 点生命、震退周围敌人、给短暂抗性并进入冷却 */
    private static void lockHealth(ServerPlayer player, long now) {
        LOCK_COOLDOWNS.put(player.getUUID(),
                now + BlackGoldHeartSkill.LOCK_COOLDOWN_TICKS * 50L);

        player.setHealth(BlackGoldHeartSkill.LOCK_HEALTH);
        player.addEffect(new MobEffectInstance(
                MobEffects.RESISTANCE, 40, 2, false, true, true));

        if (player.level() instanceof ServerLevel level) {
            knockbackEnemies(level, player);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.5, 0.5, 0.5, 0.0);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7F, 1.8F);
        }

        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin." + BlackGoldHeartSkill.PATH + ".locked"));
    }

    /** 黑金心脏的击退：把周围敌人向外推开 */
    private static void knockbackEnemies(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(BlackGoldHeartSkill.KNOCKBACK_RADIUS);
        for (LivingEntity enemy : level.getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player && e.isAlive())) {
            Vec3 dir = enemy.position().subtract(player.position());
            if (dir.lengthSqr() < 1.0E-4) {
                dir = new Vec3(1.0, 0.0, 0.0);
            }
            dir = dir.normalize().scale(1.8);
            enemy.push(dir.x, 0.5, dir.z);
            enemy.hurtMarked = true;
        }
    }

    /** 玩家断开连接时清理锁血冷却缓存 */
    public static void cleanupDisconnect(UUID uuid) {
        LOCK_COOLDOWNS.remove(uuid);
    }
}
