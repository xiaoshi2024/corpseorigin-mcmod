package com.xiaoshi2022.corpseorigin.event;

import com.xiaoshi2022.corpseorigin.CorpseOrigin;
import com.xiaoshi2022.corpseorigin.growth.NativeCorpseState;
import com.xiaoshi2022.corpseorigin.registry.ModSounds;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Shared 26.2 sounds adapted to Forge's retained, infected vanilla mobs. */
@Mod.EventBusSubscriber(modid = CorpseOrigin.MODID)
public final class CorpseSoundEvents {
    private CorpseSoundEvents() {}

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()
                || !mob.isAlive() || !NativeCorpseState.infected(mob) || mob.getTarget() != null) return;
        // Match 26.2's cadence, volume and pitch; broadcast only once from the server.
        if (mob.tickCount % (90 + Math.floorMod(mob.getId(), 30)) == 0)
            mob.playSound(ModSounds.CORPSE_BREATH.get(), .28f, .72f + mob.getRandom().nextFloat() * .12f);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void meleeHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0
                || !(event.getSource().getEntity() instanceof Mob mob)
                || event.getSource().getDirectEntity() != mob || !mob.isAlive()
                || !NativeCorpseState.infected(mob)) return;
        mob.playSound(ModSounds.GROUND_CHI.get(), 1.0f, .8f + mob.getRandom().nextFloat() * .4f);
    }
}
