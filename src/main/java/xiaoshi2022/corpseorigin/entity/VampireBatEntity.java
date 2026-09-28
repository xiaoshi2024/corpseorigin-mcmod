package xiaoshi2022.corpseorigin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;

import java.util.UUID;

/** A temporary, killable vanilla bat variant with owner-aware hunting behaviour. */
public class VampireBatEntity extends Bat {
    private UUID owner;
    public static final int LIFETIME = 300;
    private int remaining = LIFETIME;
    private int biteOffset;
    public VampireBatEntity(EntityType<? extends Bat> type, Level level) { super(type, level); }
    public void setOwner(net.minecraft.world.entity.LivingEntity player) { owner = player.getUUID(); }
    public void setBiteSlot(int slot) { biteOffset = Math.floorMod(slot, 5) * 10; }
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createVampireAttributes() {
        return Bat.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 20);
    }
    @Override protected void customServerAiStep(ServerLevel level) {
        if (--remaining <= 0 || owner == null || !(level.getEntity(owner) instanceof LivingEntity player)
                || !player.isAlive() || !"k".equals(ChapterCombat.actorRole(player))) {
            discard(); return;
        }
        setResting(false);
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20),
                e -> !(e instanceof VampireBatEntity) && ChapterCombat.canHit(player,e) && hasLineOfSight(e))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        var destination = target == null ? player.getEyePosition().add(Math.sin(tickCount*.15+getId()),.7,Math.cos(tickCount*.15+getId()))
                : target.getBoundingBox().getCenter();
        var direction = destination.subtract(position());
        setDeltaMovement(getDeltaMovement().scale(.65).add(direction.normalize().scale(.23)));
        // Five bats bite ten ticks apart, respecting vanilla damage immunity.
        if (target != null && position().distanceToSqr(target.getBoundingBox().getCenter()) < 2.25
                && tickCount % 50 == biteOffset) {
            float health = target.getHealth();
            float damage = (float) Math.clamp(player.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * .35, 6, 14);
            var source = new net.minecraft.world.damagesource.DamageSource(
                    ChapterCombat.attackSource(player).typeHolder(), this, player);
            if (target.hurtServer(level, source, damage)) {
                player.heal(Math.min(2, Math.max(0, health-target.getHealth())*.35f));
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.SLOWNESS, 40, 0));
            }
        }
        if (distanceToSqr(player)>1024) discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out); out.putString("Summoner",owner==null?"":owner.toString()); out.putInt("Remaining",remaining);
        out.putInt("BiteOffset", biteOffset);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        try { owner=UUID.fromString(in.getStringOr("Summoner","")); } catch (IllegalArgumentException e) { owner=null; }
        remaining=Math.min(LIFETIME,in.getIntOr("Remaining",0));
        biteOffset=Math.floorMod(in.getIntOr("BiteOffset",0),50);
    }
}
