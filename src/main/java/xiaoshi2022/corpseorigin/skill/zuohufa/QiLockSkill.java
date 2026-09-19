package xiaoshi2022.corpseorigin.skill.zuohufa;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.List;

/**
 * 左护法·锁气 —— 原著里他即使被闪瞎（致盲模式）、热感应器官被干扰，
 * 依然能"锁定敌人身上的气、感受空气的流动"找出敌人的位置。
 * <p>
 * 表现：把周围敌人的"气"锁住 —— 给半径内所有<b>非尸族</b>活体挂发光效果
 * （发光 = 隔着墙也看得见轮廓，正好对应"锁定气"），并在快捷栏报出锁定了几个。
 * 冷却 20 秒，不消耗内力。
 */
public class QiLockSkill implements ISkill {

    public static final String PATH = "qi_lock";

    private static final double RADIUS = 24.0;
    /** 锁定持续时间：10 秒 */
    private static final int LOCK_TICKS = 200;

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
        return SkillType.UTILITY;
    }

    @Override
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 400;   // 20s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // 尸族是"自己人"，不锁 —— 锁的是敌人身上的气
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(RADIUS),
                e -> e != player && e.isAlive() && ZombieKin.isNotZombieKin(e));

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, LOCK_TICKS, 0, false, true, true));
            level.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.9, target.getZ(),
                    6, 0.2, 0.2, 0.2, 0.01);
        }

        // 以自身为中心扩散一圈"气"的波纹，示意范围
        for (int ring = 0; ring < 3; ring++) {
            double radius = RADIUS * (ring + 1) / 3.0;
            for (int i = 0; i < 32; i++) {
                double angle = Math.PI * 2 * i / 32.0;
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY() + 1.0,
                        player.getZ() + Math.sin(angle) * radius,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);

        player.sendOverlayMessage(Component.translatable(
                "skill.corpseorigin." + PATH + ".locked", targets.size()));
    }
}
