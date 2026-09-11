package xiaoshi2022.corpseorigin.skill.weixin;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.List;

/**
 * 唯欣·内力化形·血云 - 将内力化成血红色云冲向对手
 */
public class BloodCloudSkill implements ISkill {

    private static final double RANGE = 8.0;
    private static final float DAMAGE = 6.0f;

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "blood_cloud");
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin.blood_cloud");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin.blood_cloud.desc");
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
        return 400;  // 20s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // 前方扇形范围伤害
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.position();
        AABB area = player.getBoundingBox().inflate(RANGE);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(origin).normalize();
            double dot = look.dot(toTarget);
            if (dot > 0.5) {  // 前方约 60 度扇形
                target.hurt(serverLevel.damageSources().playerAttack(player), DAMAGE);
                // 击退效果
                target.push(look.x * 2, 0.3, look.z * 2);
            }
        }

        // 自身短暂加速，体现"迅速冲向对手"
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1, false, false, true));
    }
}