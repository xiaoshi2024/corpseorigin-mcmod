package xiaoshi2022.corpseorigin.entity;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.growth.OrganDefinition;
import xiaoshi2022.corpseorigin.growth.OrganLibrary;
import xiaoshi2022.corpseorigin.growth.SurvivalGrowth;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Physical abilities are resolved from this body's equipped organs, never from its owner. */
public final class CloneOrganEffects {
    private CloneOrganEffects() {}

    public static List<OrganDefinition> equipped(CloneAvatarEntity clone) {
        if (!xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.enabled) return List.of();
        try {
            Set<String> ids = new HashSet<>();
            for (var slot : OrganLibrary.parseSlots(clone.getAttachedOrCreate(SurvivalGrowth.BODY)
                    .getStringOr(OrganLibrary.BODY_KEY, "[]"))) ids.add(slot.organ());
            return OrganLibrary.definitions().stream().filter(d -> ids.contains(d.id())).toList();
        } catch (IllegalArgumentException ex) { return List.of(); }
    }

    public static boolean hasTrait(CloneAvatarEntity clone, String trait) {
        if (!xiaoshi2022.corpseorigin.config.CorpseConfig.get().growth.enabled) return false;
        var body = clone.getAttachedOrCreate(SurvivalGrowth.BODY);
        return body.getBooleanOr(trait, false) || body.getIntOr(trait, 0) > 0
                || equipped(clone).stream().anyMatch(d -> trait.equals(d.trait()));
    }

    public static void tick(CloneAvatarEntity clone) {
        if (clone.tickCount % 20 == 0) {
            var anatomy = equipped(clone).stream().map(OrganDefinition::anatomy).filter(Objects::nonNull).toList();
            modifier(clone, Attributes.ATTACK_DAMAGE, "clone_organ_arms",
                    Math.min(8, anatomy.stream().mapToDouble(a -> a.extraArms() * a.damagePerArm()).sum()), AttributeModifier.Operation.ADD_VALUE);
            modifier(clone, Attributes.MOVEMENT_SPEED, "clone_organ_legs",
                    Math.min(.2, anatomy.stream().mapToDouble(a -> a.extraLegs() * a.speedPerLeg()).sum()), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            if (anatomy.stream().anyMatch(OrganDefinition.Anatomy::nightVision))
                clone.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 60, 0, true, false));
            if (hasTrait(clone, "gills")) clone.setAirSupply(clone.getMaxAirSupply());
        }
        if (hasTrait(clone, "wings") && !clone.onGround() && clone.getDeltaMovement().y < 0) {
            clone.fallDistance = 0;
            if (!clone.isOrganFlying() && clone.getDeltaMovement().y < -.15)
                clone.setDeltaMovement(clone.getDeltaMovement().multiply(1, .6, 1));
        }
        if (clone.tickCount % 10 == 0) fireJet(clone);
    }

    private static void modifier(CloneAvatarEntity clone, Holder<Attribute> type, String key,
                                 double amount, AttributeModifier.Operation operation) {
        var attribute = clone.getAttribute(type);
        if (attribute == null) return;
        var id = CorpseOrigin.id(key);
        var old = attribute.getModifier(id);
        if (amount == 0) attribute.removeModifier(id);
        else if (old == null || old.amount() != amount)
            attribute.addOrReplacePermanentModifier(new AttributeModifier(id, amount, operation));
    }

    private static void fireJet(CloneAvatarEntity clone) {
        LivingEntity target = clone.getTarget();
        if (!(clone.level() instanceof ServerLevel level) || target == null
                || !ChapterCombat.canHit(clone, target) || !clone.hasLineOfSight(target)) return;
        for (var organ : equipped(clone)) {
            var jet = organ.waterJet();
            if (jet == null || clone.distanceToSqr(target) > jet.range() * jet.range()) continue;
            var body = clone.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
            String key = organ.id() + ":" + jet.materialType();
            long now = level.getGameTime();
            if (body.getLongOr("organ_water_cd:" + key, 0) > now) continue;
            int capacity = jet.capacity() + equipped(clone).stream().filter(d -> d.anatomy() != null)
                    .mapToInt(d -> d.anatomy().stomachBonus()).max().orElse(0);
            int stored = Math.clamp(body.getIntOr("organ_water:" + key,
                    jet.materialType().equals("water") ? body.getIntOr("organ_water:" + organ.id(), 0) : 0), 0, capacity);
            body.remove("organ_water:" + organ.id());
            if (stored < jet.cost()) {
                if (jet.materialType().equals("water") && clone.isInWater()) {
                    body.putInt("organ_water:" + key, Math.min(capacity, stored + jet.refill()));
                    body.putLong("organ_water_cd:" + key, now + 20);
                    clone.setAttached(SurvivalGrowth.BODY, body);
                }
                continue;
            }
            body.putInt("organ_water:" + key, stored - jet.cost());
            body.putLong("organ_water_cd:" + key, now + jet.cooldownTicks());
            clone.setAttached(SurvivalGrowth.BODY, body);
            CloneWeaponArts.face(clone, target);
            target.hurtServer(level, clone.damageSources().mobAttack(clone), jet.damage());
            Vec3 start = clone.getEyePosition(), end = target.getBoundingBox().getCenter();
            QiEffects.cloud(level, start.lerp(end, .5), jet.materialType().equals("water") ? 0x4fc3f7 : 0x97764e,
                    (float) Math.min(8, Math.max(.5, start.distanceTo(end) / 2)), 12);
            clone.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            break;
        }
    }
}
