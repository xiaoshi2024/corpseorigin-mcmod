package xiaoshi2022.corpseorigin.skill.xiaolu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.OsmiumIceSpearEntity;
import xiaoshi2022.corpseorigin.item.MedusaEyeItem;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.unlock.SkillUnlockSource;

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

    /**
     * 只有吃下「美杜莎之眼」才能觉醒。
     * <p>
     * 声明获取式来源即关闭技能树的点数路径：{@code SkillManager.learn} 对有来源的技能
     * 一律转交 {@code SkillUnlockManager}，不扣点、不检查等级；技能界面那行也会显示
     * 「需要 美杜莎之眼」而不是「点击学习」。
     */
    @Override
    public List<SkillUnlockSource> getUnlockSources() {
        return List.of(SkillUnlockSource.relic(MedusaEyeItem.RELIC_ID));
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

        Vec3 impact = closest != null ? closest.position().add(0, closest.getBbHeight() + 8, 0) : endPos.add(0, 8, 0);
        serverLevel.addFreshEntity(OsmiumIceSpearEntity.create(serverLevel, player, impact, new Vec3(0, -0.65, 0)));
    }
}
