package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.register.BiomeRegistry;
import com.phagens.corpseorigin.register.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;

@EventBusSubscriber(modid = CorpseOrigin.MODID)
public class DeadSilenceSpawnHandler {

    private static final Set<DeferredHolder<EntityType<?>, ? extends EntityType<?>>> CORPSE_BROTHER_HOLDERS = Set.of(
            EntityRegistry.LOWER_LEVEL_ZB,
            EntityRegistry.LONGYOU,
            EntityRegistry.ZBR_FISH
    );

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMobSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        Level level = mob.level();

        if (level.isClientSide) return;

        EntityType<?> entityType = mob.getType();
        boolean isCorpseBrother = isCorpseBrotherType(entityType);

        if (isInDeadSilenceBiome(level, mob.blockPosition())) {
            if (!isCorpseBrother && mob.getType().getCategory() == MobCategory.MONSTER) {
                event.setSpawnCancelled(true);
            }
            return;
        }

        if (isCorpseBrother) return;

        if (mob.getType().getCategory() != MobCategory.MONSTER) return;

        if (!DeadSilenceCache.hasDeadSilenceBlocks()) return;

        BlockPos spawnPos = mob.blockPosition();

        if (DeadSilenceCache.isInDeadSilenceZone(spawnPos)) {
            event.setSpawnCancelled(true);
        }
    }

    public static boolean isInDeadSilenceBiome(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(BiomeRegistry.DEAD_SILENCE);
    }

    public static boolean isCorpseBrotherType(EntityType<?> type) {
        for (DeferredHolder<EntityType<?>, ? extends EntityType<?>> holder : CORPSE_BROTHER_HOLDERS) {
            if (holder.get() == type) {
                return true;
            }
        }
        return false;
    }
}
