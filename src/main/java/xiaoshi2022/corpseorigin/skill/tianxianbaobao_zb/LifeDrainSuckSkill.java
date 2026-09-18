package xiaoshi2022.corpseorigin.skill.tianxianbaobao_zb;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

/**
 * 天线宝宝尸兄·吸食——近战抓住玩家持续吸血，可用打断技解救。
 * <p>
 * 主动技能，无冷却（0 ticks）。
 * <p>
 * 核心逻辑已抽到 {@link EntityAntennaSuckHandler}，玩家和穿戴套装的生物共用。
 * 这里只负责玩家主动激活的入口（消息反馈 + 音效）。
 */
public class LifeDrainSuckSkill extends AbstractSkill {

    public static final String PATH = "life_drain_suck";

    public LifeDrainSuckSkill() {
        super(PATH, SkillType.COMBAT, 0);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (EntityAntennaSuckHandler.isSucking(player.getUUID())) {
            return;
        }
        boolean started = EntityAntennaSuckHandler.start(player);
        if (!started) {
            player.sendOverlayMessage(Component.translatable(msgKey("no_target")));
        } else {
            // 找目标给提示
            LivingEntity target = findTarget(player);
            if (target != null) {
                player.sendOverlayMessage(Component.translatable(msgKey("grab"), target.getName()));
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 0.6F);
        }
    }

    /** 复用 handler 的目标查找，仅用于提示 */
    private static LivingEntity findTarget(ServerPlayer player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        LivingEntity best = null;
        double bestDistance = 8.0;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(8.0),
                e -> e != player && e.isAlive() && !(e instanceof Player p && p.isSpectator()))) {
            double d = center.distanceTo(candidate.position().add(0.0, candidate.getBbHeight() * 0.5, 0.0));
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        return best;
    }

    private static String msgKey(String suffix) {
        return "skill.corpseorigin." + PATH + "." + suffix;
    }
}

