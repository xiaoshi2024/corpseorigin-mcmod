package xiaoshi2022.corpseorigin.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import java.util.UUID;

/** A temporary, killable vanilla bat variant with owner-aware hunting behaviour. */
public class VampireBatEntity extends Bat {
    private UUID owner;
    private int remaining = 160;
    public VampireBatEntity(EntityType<? extends Bat> type, Level level) { super(type, level); }
    public void setOwner(ServerPlayer player) { owner = player.getUUID(); }
    @Override protected void customServerAiStep(ServerLevel level) {
        if (--remaining <= 0 || owner == null || !(level.getEntity(owner) instanceof ServerPlayer player)
                || !player.isAlive() || !"k".equals(CharacterManager.getInstance().getPlayerCharacterId(player))) {
            discard(); return;
        }
        setResting(false);
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12),
                e -> !(e instanceof VampireBatEntity) && ChapterCombat.canHit(player,e) && hasLineOfSight(e))
                .stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        var destination = target == null ? player.getEyePosition().add(Math.sin(tickCount*.15+getId()),.7,Math.cos(tickCount*.15+getId()))
                : target.getEyePosition();
        var direction = destination.subtract(position());
        setDeltaMovement(getDeltaMovement().scale(.65).add(direction.normalize().scale(.16)));
        if (target != null && distanceToSqr(target) < 2.25 && tickCount % 20 == 0) {
            float health = target.getHealth();
            if (target.hurtServer(level, damageSources().playerAttack(player), 2))
                player.heal(Math.min(1, Math.max(0, health-target.getHealth())*.5f));
        }
        if (distanceToSqr(player)>1024) discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out); out.putString("Summoner",owner==null?"":owner.toString()); out.putInt("Remaining",remaining);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        try { owner=UUID.fromString(in.getStringOr("Summoner","")); } catch (IllegalArgumentException e) { owner=null; }
        remaining=Math.min(160,in.getIntOr("Remaining",0));
    }
}
