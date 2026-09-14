package xiaoshi2022.corpseorigin.skill.xiaolu;

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
import java.util.Optional;

/**
 * 小鹿·锇冰梭 - 对视线命中的实体造成 8 点伤害
 */
public class OsmiumIceSpikeSkill implements ISkill {

    private static final double REACH = 16.0;
    private static final float DAMAGE = 8.0f;

    @Override
    public Identifier getId() {
        return Identifier.fromNamespaceAndPath(CorpseOrigin.MOD_ID, "osmium_ice_spike");
    }

    @Override
    public Component getName() {
        return Component.translatable("skill.corpseorigin.osmium_ice_spike");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill.corpseorigin.osmium_ice_spike.desc");
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
        return 600;  // 30s
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(REACH));

        AABB sweep = new AABB(eyePos, endPos).inflate(1.0);
        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(
                LivingEntity.class, sweep, e -> e != player && e.isAlive());

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity e : entities) {
            AABB box = e.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = box.clip(eyePos, endPos);
            if (hit.isPresent()) {
                double dist = eyePos.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = e;
                }
            }
        }

        if (closest != null) {
            closest.hurt(serverLevel.damageSources().playerAttack(player), DAMAGE);
        }
    }
}
