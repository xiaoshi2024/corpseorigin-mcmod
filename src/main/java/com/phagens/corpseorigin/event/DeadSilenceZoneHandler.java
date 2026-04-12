package com.phagens.corpseorigin.event;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.block.custom.AlienatedFragmentBlock;
import com.phagens.corpseorigin.register.BiomeRegistry;
import com.phagens.corpseorigin.register.EntityRegistry;
import com.phagens.corpseorigin.register.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;

@EventBusSubscriber(modid = CorpseOrigin.MODID, value = Dist.CLIENT)
public class DeadSilenceZoneHandler {

    private static final int FOG_CHUNK_DISTANCE = 3;
    private static final float FOG_START = 32.0F;
    private static final float FOG_END = FOG_CHUNK_DISTANCE * 16.0F * 2.0F;

    private static boolean isInDeadSilenceZone = false;
    private static int checkCooldown = 0;
    private static final int CHECK_INTERVAL = 40;

    private static final Set<DeferredHolder<EntityType<?>, ? extends EntityType<?>>> CORPSE_BROTHER_HOLDERS = Set.of(
            EntityRegistry.LOWER_LEVEL_ZB,
            EntityRegistry.LONGYOU,
            EntityRegistry.ZBR_FISH,
            EntityRegistry.GUIGUN
    );

    private static SimpleSoundInstance ambienceSound = null;
    private static boolean wasInDeadSilenceZone = false;
    private static int musicCheckCooldown = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            isInDeadSilenceZone = false;
            stopAmbience();
            return;
        }

        checkCooldown++;
        if (checkCooldown < CHECK_INTERVAL) return;
        checkCooldown = 0;

        BlockPos playerPos = mc.player.blockPosition();
        isInDeadSilenceZone = checkDeadSilence(mc.level, playerPos);

        updateAmbience(mc);

        if (isInDeadSilenceZone && !isSoundPlaying(mc)) {
            musicCheckCooldown++;
            if (musicCheckCooldown >= 40) {
                playAmbience(mc);
                musicCheckCooldown = 0;
            }
        }
    }

    private static boolean checkDeadSilence(Level level, BlockPos pos) {
        if (level.getBiome(pos).is(BiomeRegistry.DEAD_SILENCE)) {
            return true;
        }
        return checkDeadSilenceNearby(level, pos);
    }

    private static boolean isSoundPlaying(Minecraft mc) {
        return ambienceSound != null && mc.getSoundManager().isActive(ambienceSound);
    }

    private static void updateAmbience(Minecraft mc) {
        if (isInDeadSilenceZone && !wasInDeadSilenceZone) {
            playAmbience(mc);
        } else if (!isInDeadSilenceZone && wasInDeadSilenceZone) {
            stopAmbience();
        }
        wasInDeadSilenceZone = isInDeadSilenceZone;
    }

    private static void playAmbience(Minecraft mc) {
        if (ambienceSound != null && isSoundPlaying(mc)) return;

        stopAmbience();

        SoundEvent soundEvent = ModSounds.DEAD_SILENCE_AMBIENCE.get();
        ambienceSound = new SimpleSoundInstance(
            soundEvent.getLocation(),
            SoundSource.AMBIENT,
            1.0F,
            1.0F,
            mc.player.getRandom(),
            true,
            0,
            SimpleSoundInstance.Attenuation.NONE,
            0.0,
            0.0,
            0.0,
            true
        );

        mc.getSoundManager().play(ambienceSound);
    }

    private static void stopAmbience() {
        if (ambienceSound != null) {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().stop(ambienceSound);
            ambienceSound = null;
        }
    }

    private static boolean checkDeadSilenceNearby(Level level, BlockPos center) {
        int radius = 29;
        int step = 8;

        for (int x = -radius; x <= radius; x += step) {
            for (int z = -radius; z <= radius; z += step) {
                BlockPos checkPos = center.offset(x, 0, z);
                BlockState state = level.getBlockState(checkPos);
                if (state.getBlock() instanceof AlienatedFragmentBlock &&
                    state.getValue(AlienatedFragmentBlock.DEAD_SILENCE)) {
                    return true;
                }
            }
        }

        return false;
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!isInDeadSilenceZone) return;

        event.setNearPlaneDistance(FOG_START);
        event.setFarPlaneDistance(FOG_END);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColors(ViewportEvent.ComputeFogColor event) {
        if (!isInDeadSilenceZone) return;

        float gray = 0.35F;
        event.setRed(gray);
        event.setGreen(gray);
        event.setBlue(gray);
    }

    public static boolean isInDeadSilenceZone() {
        return isInDeadSilenceZone;
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
