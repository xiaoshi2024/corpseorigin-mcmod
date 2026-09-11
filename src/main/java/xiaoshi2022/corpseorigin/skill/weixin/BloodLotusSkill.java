package xiaoshi2022.corpseorigin.skill.weixin;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.List;

/**
 * 唯欣·血莲大法·血莲金瓣 - 变出红白相间的莲瓣状物体攻击
 */
public class BloodLotusSkill implements ISkill {

    private static final double RANGE = 6.0;
    private static final float DAMAGE = 10.0f;

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "blood_lotus");
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin.blood_lotus");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin.blood_lotus.desc");
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ULTIMATE;
    }

    @Override
    public boolean isActivatable() {
        return true;
    }

    @Override
    public int getCooldownTicks() {
        return 1200;  // 60s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // 以玩家为中心的范围攻击
        AABB area = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(serverLevel.damageSources().playerAttack(player), DAMAGE);
            // 击退
            Vec3 dir = target.position().subtract(player.position()).normalize();
            target.push(dir.x * 3, 0.5, dir.z * 3);
        }
    }
}