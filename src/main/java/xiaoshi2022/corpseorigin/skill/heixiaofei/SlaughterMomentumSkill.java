package xiaoshi2022.corpseorigin.skill.heixiaofei;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 黑小飞·杀势（杀戮觉醒·红甲杀气状态）
 * <p>
 * 进入红甲杀气状态：全面提升战力，代价是持续的饥饿消耗；
 * 红纹杀气粒子 + 红眼特效由 {@link CorpseNetwork#broadcastTempRedEye} 与粒子表现。
 */
public class SlaughterMomentumSkill implements ISkill {

    public static final String PATH = "slaughter_momentum";

    /** 杀气状态持续时间（12 秒） */
    private static final int DURATION = 400;
    private static final int COOLDOWN = 1200;   // 60s

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
        return COOLDOWN;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // ==================== 红甲杀气状态 ====================
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, DURATION, 2, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION, 2, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, DURATION, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, DURATION, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, DURATION, 2, false, true, true));

        // ==================== 代价：杀气反噬饥饿 ====================
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, DURATION, 2, false, true, true));

        // ==================== 红眼 + 红纹杀气粒子 ====================
        CorpseNetwork.broadcastTempRedEye(player, DURATION);
        spawnMomentumParticles(level, player);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** 红纹杀气粒子：以玩家为中心向外扩散的血红纹路 */
    private void spawnMomentumParticles(ServerLevel level, ServerPlayer player) {
        double centerX = player.getX();
        double centerY = player.getY();
        double centerZ = player.getZ();

        for (int i = 0; i < 48; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 0.6 + Math.random() * 1.6;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            double y = centerY + 0.2 + Math.random() * 1.8;
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 1, 0, 0.02, 0, 0.0);
        }
        for (int i = 0; i < 24; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 1.0 + Math.random() * 1.2;
            level.sendParticles(ParticleTypes.CRIT,
                    centerX + Math.cos(angle) * radius,
                    centerY + 0.5 + Math.random() * 1.5,
                    centerZ + Math.sin(angle) * radius,
                    1, 0, 0.05, 0, 0.05);
        }
    }
}
