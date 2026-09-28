package xiaoshi2022.corpseorigin.skill.chapter;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared visual terrain wave, adapted from 1.21.1 LongyouEarthquakeEntity/Renderer.
 * Native block displays replace the old NeoForge renderer and tracking packets.
 * Real surface and subsurface states are sampled without mutating the world.
 */
public final class GroundShockwave {
    private GroundShockwave() {}
    private static final int MAX_WAVES = 8, MAX_BLOCKS = 96, SPREAD_TICKS = 14, LIFE = GroundShockwaveMath.LIFE;
    private static final List<Wave> WAVES = new ArrayList<>();
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> WAVES.removeIf(Wave::tick));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            WAVES.forEach(Wave::clear);
            WAVES.clear();
        });
    }

    /** Radius is capped at 12 blocks; strength controls lift and deformation. */
    public static void spawn(ServerPlayer owner, Vec3 center, double radius, double strength) {
        if (!Double.isFinite(radius) || !Double.isFinite(strength) || !Double.isFinite(center.lengthSqr())) return;
        if (!owner.isAlive() || owner.isSpectator()) return;
        // A skill slam and god-tier landing can both be reported in the same tick.
        if (WAVES.stream().anyMatch(w -> w.owner == owner && w.age < 5)) return;
        if (WAVES.size() >= MAX_WAVES) return;
        Wave wave = new Wave(owner, center, Math.clamp(radius, 2, 12), Math.clamp(strength, .5, 3));
        WAVES.add(wave);
        wave.level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.5f, .7f);
    }

    private record Sample(BlockPos pos, net.minecraft.world.level.block.state.BlockState state, BlockPos lightPos, int delay, int depth, double angle) {}
    private record Piece(Display.BlockDisplay display, Sample sample, int start) {}

    private static final class Wave {
        final ServerPlayer owner;
        final ServerLevel level;
        final Vec3 center;
        final double strength;
        final List<Sample> samples = new ArrayList<>();
        final List<Piece> pieces = new ArrayList<>();
        int age;

        Wave(ServerPlayer owner, Vec3 center, double radius, double strength) {
            this.owner = owner; this.level = owner.level(); this.center = center; this.strength = strength;
            Set<BlockPos> seen = new HashSet<>();
            // Evenly distribute a fixed budget across the entire disk, including its outer rim.
            int columns = MAX_BLOCKS / 3;
            for (int i = 0; i < columns; i++) {
                double distance = i == 0 ? 0 : radius * Math.sqrt((double)i / (columns - 1));
                double angle = i * 2.399963229728653;
                BlockPos base = BlockPos.containing(center.x + Math.cos(angle) * distance,
                        center.y - .05, center.z + Math.sin(angle) * distance);
                BlockPos surface = null;
                // Start directly below the feet, never above the impact plane: nearby
                // walls, tree tops and low cave ceilings are not the struck ground.
                for (int dy = 0; dy >= -3; dy--) {
                    BlockPos at = base.above(dy);
                    if (!level.hasChunkAt(at) || !level.getWorldBorder().isWithinBounds(at)) continue;
                    var state = level.getBlockState(at);
                    if (!state.getCollisionShape(level, at).isEmpty()
                            && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
                        surface = at; break;
                    }
                }
                if (surface == null) continue;
                for (int depth = 0; depth <= 2; depth++) {
                    BlockPos at = surface.below(depth);
                    var state = level.getBlockState(at);
                    if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                            || state.getDestroySpeed(level, at) < 0 || state.getCollisionShape(level, at).isEmpty()) continue;
                    if (seen.add(at)) samples.add(new Sample(at, state, surface.above(),
                            (int) (distance / radius * SPREAD_TICKS) + depth, depth, angle));
                }
            }
        }

        boolean tick() {
            if (!owner.isAlive() || owner.isRemoved() || owner.level() != level || age > SPREAD_TICKS + LIFE + 4) {
                clear(); return true;
            }
            for (Sample sample : samples) {
                if (sample.delay != age || !level.hasChunkAt(sample.pos)) continue;
                // Snapshot before an originating skill changes terrain.
                var state = sample.state;
                Display.BlockDisplay display = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level) {
                    @Override public boolean shouldBeSaved() { return false; }
                };
                display.setBlockState(state);
                display.setPos(Vec3.atCenterOf(sample.pos));
                display.setBrightnessOverride(lightAt(sample.lightPos));
                display.setWidth(12); display.setHeight(16);
                display.setTransformationInterpolationDuration(2);
                display.setTransformation(new Transformation(new Vector3f(-.5f), new Quaternionf(),
                        new Vector3f(1), new Quaternionf()));
                if (level.addFreshEntity(display)) pieces.add(new Piece(display, sample, age));
            }
            pieces.removeIf(piece -> {
                int elapsed = age - piece.start;
                if (elapsed >= LIFE || piece.display.isRemoved() || !level.hasChunkAt(piece.sample.pos)) {
                    piece.display.discard(); return true;
                }
                // Rise, hold, settle: continuous envelope fixes the old peak-height discontinuity.
                float lift = GroundShockwaveMath.envelope(elapsed);
                Sample s = piece.sample;
                float height = lift * (float) (1.2 * strength + s.depth * 1.25);
                float outward = lift * (float) (.45 * strength + s.depth * .3);
                // Transformations move only the model; the entity stays buried at its
                // original block. Sample exposed ground and the animated model position
                // explicitly so the renderer does not light flying debris from inside rock.
                BlockPos visiblePos=BlockPos.containing(s.pos.getX()+.5+Math.cos(s.angle)*outward,
                        s.pos.getY()+.5+height,s.pos.getZ()+.5+Math.sin(s.angle)*outward);
                var surfaceLight=lightAt(s.lightPos);
                var airborneLight=level.hasChunkAt(visiblePos)?lightAt(visiblePos):surfaceLight;
                piece.display.setBrightnessOverride(new net.minecraft.util.Brightness(
                        Math.max(surfaceLight.block(),airborneLight.block()),Math.max(surfaceLight.sky(),airborneLight.sky())));
                float size = s.depth == 0 ? 1 : .65f;
                Vector3f scale = new Vector3f(size * (1 + .18f * lift), size * (1 + .4f * lift), size);
                Quaternionf rotation = new Quaternionf().rotationAxis((float) Math.toRadians(75) * lift,
                        (float) -Math.sin(s.angle), 0, (float) Math.cos(s.angle));
                Vector3f offset = rotation.transform(new Vector3f(scale).mul(-.5f));
                offset.add((float) Math.cos(s.angle) * outward, height, (float) Math.sin(s.angle) * outward);
                piece.display.setTransformationInterpolationDelay(0);
                piece.display.setTransformation(new Transformation(offset, rotation, scale, new Quaternionf()));
                return false;
            });
            age++;
            return false;
        }

        net.minecraft.util.Brightness lightAt(BlockPos pos) {
            return new net.minecraft.util.Brightness(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos),
                    level.getBrightness(net.minecraft.world.level.LightLayer.SKY,pos));
        }

        void clear() { pieces.forEach(p -> p.display.discard()); pieces.clear(); }
    }

}
