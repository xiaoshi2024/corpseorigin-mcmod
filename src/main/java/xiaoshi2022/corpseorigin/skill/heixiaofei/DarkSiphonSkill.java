package xiaoshi2022.corpseorigin.skill.heixiaofei;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.List;

/**
 * 黑小飞·黑暗虹吸 - 吸取周围敌人的气血回复自身。
 * <p>
 * 对强力目标（BOSS / 玩家）虹吸会被反噬：吸血失效并对自己造成伤害。
 */
public class DarkSiphonSkill implements ISkill {

    public static final String PATH = "dark_siphon";

    private static final double RANGE = 8.0;
    private static final float DRAIN_DAMAGE = 6.0f;
    /** 每点虹吸伤害转化为的生命值 */
    private static final float HEAL_RATIO = 0.35f;
    /** 判定为 BOSS 的最大生命值阈值 */
    private static final float BOSS_HEALTH = 40.0f;
    /** 反噬伤害 */
    private static final float BACKLASH_DAMAGE = 3.0f;

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, PATH);
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin." + PATH);
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin." + PATH + ".desc");
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.COMBAT;
    }

    @Override
    public int getCost() {
        return 2;
    }

    @Override
    public int getRequiredLevel() {
        return 1;
    }

    @Override
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 600;  // 30s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        AABB area = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player && e.isAlive());

        float drained = 0.0f;
        boolean backlash = false;

        for (LivingEntity target : targets) {
            boolean boss = target instanceof Player || target.getMaxHealth() >= BOSS_HEALTH;

            target.hurt(level.damageSources().playerAttack(player), DRAIN_DAMAGE);

            // 从目标身上流向施术者的血色粒子
            for (int i = 0; i < 6; i++) {
                double t = i / 6.0;
                double x = target.getX() + (player.getX() - target.getX()) * t;
                double y = (target.getY() + target.getBbHeight() * 0.5)
                        + (player.getY() + 1.0 - target.getY()) * t;
                double z = target.getZ() + (player.getZ() - target.getZ()) * t;
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 1, 0, 0, 0, 0.0);
            }

            if (boss) {
                backlash = true;
            } else {
                drained += DRAIN_DAMAGE * HEAL_RATIO;
            }
        }

        // 回复生命（不超过上限）
        if (drained > 0.0f) {
            player.heal(Math.min(drained, player.getMaxHealth() - player.getHealth()));
        }

        // 对 BOSS 虹吸被反噬
        if (backlash) {
            player.hurtServer(level, level.damageSources().generic(), BACKLASH_DAMAGE);
            player.sendSystemMessage(Component.translatable(
                    "skill.corpseorigin." + PATH + ".backlash"));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9F, 0.7F);
    }
}
