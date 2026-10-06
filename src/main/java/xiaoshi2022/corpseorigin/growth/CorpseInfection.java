package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.npc.villager.Villager;
import xiaoshi2022.corpseorigin.effect.BYeffect;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.registry.ModEffects;
import xiaoshi2022.corpseorigin.registry.ModEntities;

/** One infection roll per successful direct hit; the same hook covers grapple bites. */
public final class CorpseInfection {
    private static final java.util.Map<LivingEntity, Long> nextWaterRoll = new java.util.WeakHashMap<>();
    private CorpseInfection() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, taken, blocked) -> {
            if (!blocked && taken > 0 && source.getDirectEntity() == source.getEntity()
                    && source.getEntity() instanceof Mob attacker && ZombieKin.isZombieKin(attacker)) {
                tryInfectVillager(attacker, target);
                tryInfectPlayer(attacker, target);
            }
        });
    }

    /**
     * 尸族直接攻击命中玩家：按 {@code playerBiteInfectionChance} 概率上 QIANS（60~300 秒后尸化，
     * 感染度涨满自动转尸兄角色——下游复用 {@link BYeffect} 现成链路）。牛奶可清效果自救。
     * 蛆虫的寄生感染（必感）走 {@code CorpseMaggotEntity.tickAttachment} 自己的路径，这里是普攻兜底。
     */
    public static void tryInfectPlayer(Mob attacker, LivingEntity target) {
        if (!(target instanceof net.minecraft.server.level.ServerPlayer player) || !target.isAlive()
                || player.isCreative() || !BYeffect.canInfect(player)
                || !(target.level() instanceof ServerLevel level)) return;
        if (attacker.getRandom().nextDouble() < CorpseHorror.config().playerBiteInfectionChance) {
            BYeffect.applyInfection(player, level, attacker.getUUID());
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.corpseorigin.player_bitten"));
        }
    }

    public static void tryInfectVillager(Mob attacker, LivingEntity target) {
        if (!(target instanceof Villager) || !target.isAlive() || target.hasEffect(ModEffects.QIANS)
                || attacker instanceof xiaoshi2022.corpseorigin.entity.CorpseMaggotEntity
                || !ZombieKin.isZombieKin(attacker) || !ZombieKin.canAttack(attacker,target)
                || attacker.isAlliedTo(target) || !(target.level() instanceof ServerLevel level)) return;
        if (attacker.getRandom().nextDouble() < CorpseHorror.config().villagerInfectionChance)
            BYeffect.applyInfection(target, level, attacker.getUUID());
    }

    /** Called by the actual infected-fluid block contact, never for ordinary water. */
    public static void touchInfectedWater(LivingEntity target) {
        if (!(target instanceof AbstractFish) || ZombieKin.isZombieKin(target) || !target.isAlive()
                || target.hasEffect(ModEffects.QIANS) || !(target.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (now < nextWaterRoll.getOrDefault(target, Long.MIN_VALUE)) return;
        nextWaterRoll.put(target, now + 20);
        // Once infected, the effect is not refreshed; it must be allowed to reach transformation.
        if (target.getRandom().nextDouble() < CorpseHorror.config().fishWaterInfectionChance)
            BYeffect.applyInfection(target, level, CorpseHorror.config().fishInfectionTicks, null);
    }

    public static void transformFish(AbstractFish fish, ServerLevel level) {
        if (!fish.isAlive() || ZombieKin.isZombieKin(fish)) return;
        var infected = ModEntities.ZBR_FISH.create(level, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);
        if (infected == null) return;
        infected.setPos(fish.position());
        infected.setYRot(fish.getYRot());
        infected.setXRot(fish.getXRot());
        infected.setDeltaMovement(fish.getDeltaMovement());
        infected.setCustomName(fish.getCustomName());
        infected.setCustomNameVisible(fish.isCustomNameVisible());
        infected.setFromBucket(fish.fromBucket());
        infected.setNoAi(fish.isNoAi());
        if (fish.isPersistenceRequired()) infected.setPersistenceRequired();
        if (level.addFreshEntity(infected)) {
            CorpseHorror.blood(level, fish.position(), 6);
            fish.discard();
        }
    }
}
