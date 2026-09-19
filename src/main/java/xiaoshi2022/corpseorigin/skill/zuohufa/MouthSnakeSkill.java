package xiaoshi2022.corpseorigin.skill.zuohufa;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.compat.SnakesAliveCompat;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 左护法·嘴里吐蛇（金旒龙）—— 原著里他嘴里那条黄色蛇（硬度媲美钢筋、速度极快攻击强），
 * 用来一口干掉血滴堂、贯穿尘风，后来还放出金旒龙贯穿了赵日天。
 * <p>
 * 表现：朝视线方向吐出一条金蛇，<b>沿直线贯穿</b>路径上的敌人（最多 5 个），
 * 每个目标都吃一次伤害并被轻微顶开。
 * <p>
 * ★ 软联动：装了 <b>Snakes Alive</b>（且配置开着）时会<b>真的吐出一条它家的蛇</b> ——
 * 那条蛇会认你为主，由它自家的"跟随主人 / 护主"AI 接管；没装就退回纯粒子表现。
 * 这样"金旒龙"这条蛇我们一行模型都不用做。冷却 10 秒，消耗内力 5。
 */
public class MouthSnakeSkill implements ISkill {

    public static final String PATH = "mouth_snake";

    /** 蛇能窜多远（格） */
    private static final double REACH = 20.0;
    /** 每一步前进的距离：小一点才不会从目标身上"跳过去" */
    private static final double STEP = 0.4;
    private static final float DAMAGE = 10.0F;
    private static final double KNOCKBACK = 0.6;
    private static final int MAX_TARGETS = 5;
    private static final int INNER_POWER_COST = 5;

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
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 200;   // 10s
    }

    @Override
    public int getInnerPowerCost() {
        return INNER_POWER_COST;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.4F);

        // ★ 软联动：装了 Snakes Alive 就真吐一条它家的蛇（借它家的实体，我们不用建模）。
        //   那条蛇会认玩家为主，之后由它自家的"跟随主人 / 护主"AI 接管；
        //   没装 / 关掉联动时这里什么都不做，技能照样有下面的贯穿打击与蛇影轨迹。
        SnakesAliveCompat.spitSnake(player, look);

        // 已被咬过的目标不再重复伤害（一条蛇对一个目标只咬一口）
        Set<Integer> hitIds = new HashSet<>();
        int hits = 0;

        for (double travelled = 0.0; travelled <= REACH && hits < MAX_TARGETS; travelled += STEP) {
            Vec3 point = start.add(look.scale(travelled));

            // 蛇身轨迹：金色碎光 + 一点末端气流
            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
            if (((int) (travelled / STEP)) % 3 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
            }

            AABB probe = new AABB(point, point).inflate(0.6);
            List<LivingEntity> caught = level.getEntitiesOfClass(
                    LivingEntity.class, probe, e -> e != player && e.isAlive() && !hitIds.contains(e.getId()));

            for (LivingEntity target : caught) {
                if (hits >= MAX_TARGETS) {
                    break;
                }
                hitIds.add(target.getId());
                hits++;

                target.hurt(level.damageSources().playerAttack(player), DAMAGE);

                Vec3 push = new Vec3(look.x, 0.0, look.z);
                if (push.lengthSqr() > 1.0E-4) {
                    push = push.normalize().scale(KNOCKBACK);
                    target.push(push.x, 0.15, push.z);
                    target.hurtMarked = true;
                }
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        6, 0.25, 0.25, 0.25, 0.0);
            }
        }

        if (hits > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.5F);
        }
    }
}
