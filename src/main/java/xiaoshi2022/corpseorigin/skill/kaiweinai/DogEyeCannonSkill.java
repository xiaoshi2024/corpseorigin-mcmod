package xiaoshi2022.corpseorigin.skill.kaiweinai;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.entity.HamEntity;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;

import java.util.Comparator;

/**
 * 开胃奶·狗眼炮：将附近最近的一只哈姆实体朝瞄准方向丢出去。
 * 主动技能，冷却 12 秒（240 ticks）；没有可投掷的哈姆时不进入冷却。
 */
public class DogEyeCannonSkill extends AbstractSkill {

    public static final String PATH = "dog_eye_cannon";
    private static final double SEARCH_RADIUS = 6.0;
    private static final double THROW_SPEED = 1.8;

    public DogEyeCannonSkill() {
        super(PATH, SkillType.COMBAT, 240);
    }

    @Override
    public Component checkUsable(ServerPlayer player) {
        return findNearbyHam(player) == null
                ? Component.translatable("skill.corpseorigin.dog_eye_cannon.no_ham") : null;
    }

    @Override
    public void onActivate(ServerPlayer player) {
        HamEntity ham = findNearbyHam(player);
        if (ham == null) return;

        ham.getNavigation().stop();
        ham.setTarget(null);
        ham.setOrderedToSit(false);
        ham.setInSittingPose(false);
        // Throw the existing entity from its current position, preserving its identity and owner.
        Vec3 velocity = player.getLookAngle().scale(THROW_SPEED);
        ham.setDeltaMovement(velocity.add(0, 0.35, 0));
        ham.hurtMarked = true;
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    private static HamEntity findNearbyHam(ServerPlayer player) {
        return player.level().getEntitiesOfClass(HamEntity.class,
                        player.getBoundingBox().inflate(SEARCH_RADIUS),
                        ham -> ham.isAlive() && !ham.isSpectator()
                                && !ham.isPassenger() && !ham.isVehicle() && !ham.isLeashed()
                                && player.distanceToSqr(ham) <= SEARCH_RADIUS * SEARCH_RADIUS
                                && player.hasLineOfSight(ham))
                .stream()
                .min(Comparator.comparingDouble(ham -> player.distanceToSqr(ham)))
                .orElse(null);
    }
}
