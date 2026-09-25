package xiaoshi2022.corpseorigin.skill.longyou;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import xiaoshi2022.corpseorigin.block.entity.ZBRFleshBlockEntity;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.registry.ModBlocks;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CorpseNestConstructionHandler {
    /** Mature rally nest footprint. 10^3 - 8^3 = 488 shell blocks, below 1000. */
    public static final int SIZE = 10;
    private static final int RADIUS = 64;
    private static final int REQUIRED_CORPSES = 8;
    private static final Map<UUID, Job> JOBS = new ConcurrentHashMap<>();

    private CorpseNestConstructionHandler() {}

    public static Component canStart(ServerPlayer player) {
        if (JOBS.containsKey(player.getUUID())) {
            return Component.translatable("skill.corpseorigin.corpse_brother_rally.active");
        }
        int count = findCorpses(player).size();
        return count < REQUIRED_CORPSES
                ? Component.translatable("skill.corpseorigin.corpse_brother_rally.need_corpses",
                    count, REQUIRED_CORPSES)
                : null;
    }

    public static void start(ServerPlayer player) {
        List<Mob> corpses = findCorpses(player);
        if (corpses.size() < REQUIRED_CORPSES) return;
        BlockPos center = player.blockPosition().relative(player.getDirection(), 8);
        JOBS.put(player.getUUID(), new Job((ServerLevel) player.level(), center,
                corpses.stream().map(Mob::getUUID).toList()));
        player.sendOverlayMessage(Component.translatable("skill.corpseorigin.corpse_brother_rally.started"));
    }

    public static void tick(MinecraftServer server) {
        JOBS.entrySet().removeIf(entry -> tickJob(server, entry.getValue()));
    }

    private static boolean tickJob(MinecraftServer server, Job job) {
        ServerLevel level = server.getLevel(job.level.dimension());
        if (level == null) return true;
        job.age++;
        int alive = 0;
        for (UUID id : job.corpses) {
            if (!(level.getEntity(id) instanceof Mob mob) || !mob.isAlive()) continue;
            alive++;
            mob.setTarget(null);
            mob.getNavigation().moveTo(job.center.getX() + 0.5, job.center.getY(), job.center.getZ() + 0.5, 1.25);
            if (mob.distanceToSqr(job.center.getX() + .5, job.center.getY() + .5,
                    job.center.getZ() + .5) <= 9.0) {
                mob.discard();
                job.consumed++;
                QiEffects.burst(level, job.center.getX() + .5, job.center.getY() + 1,
                        job.center.getZ() + .5, 0xc0182a, 16, 1.0);
            }
        }

        int targetStage = job.consumed >= REQUIRED_CORPSES ? 3 : job.consumed >= 4 ? 2 : job.consumed >= 1 ? 1 : 0;
        if (targetStage > job.stage) {
            job.stage = targetStage;
            buildStage(level, job.center, targetStage);
            level.playSound(null, job.center, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS,
                    1.4F, 1.2F - targetStage * .15F);
        }
        return job.stage == 3 || (alive == 0 && job.age > 200) || job.age > 2400;
    }

    private static List<Mob> findCorpses(ServerPlayer player) {
        return ((ServerLevel) player.level()).getEntitiesOfClass(Mob.class,
                new AABB(player.blockPosition()).inflate(RADIUS),
                mob -> mob instanceof ZombieKin && mob.isAlive());
    }

    private static void buildStage(ServerLevel level, BlockPos center, int stage) {
        List<BlockPos> shell = shell(center);
        int amount = stage == 1 ? shell.size() / 4 : stage == 2 ? shell.size() * 3 / 5 : shell.size();
        for (int i = 0; i < amount; i++) level.setBlock(shell.get(i), ModBlocks.ZBR_FLESH.defaultBlockState(), 3);

        if (stage == 3) {
            for (BlockPos pos : shell) {
                if (level.getBlockEntity(pos) instanceof ZBRFleshBlockEntity flesh) {
                    flesh.setRallyNestCenter(center);
                }
            }
            BlockPos gateway = center.offset(-5, -1, 0);
            level.setBlock(gateway, ModBlocks.ZBR_FLESH.defaultBlockState(), 3);
            if (level.getBlockEntity(gateway) instanceof ZBRFleshBlockEntity flesh) {
                flesh.setCorpseNestGateway(true);
            }
        }
    }

    private static List<BlockPos> shell(BlockPos center) {
        List<BlockPos> result = new ArrayList<>();
        for (int x = -5; x <= 4; x++) for (int y = -1; y <= 8; y++) for (int z = -5; z <= 4; z++) {
            if (x == -5 || x == 4 || y == -1 || y == 8 || z == -5 || z == 4) {
                result.add(center.offset(x, y, z));
            }
        }
        result.sort(Comparator.comparingInt(BlockPos::getY));
        return result;
    }

    public static boolean isMatureNestSurface(ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                || !(level.getBlockEntity(pos) instanceof ZBRFleshBlockEntity flesh)) return false;
        BlockPos center = flesh.getRallyNestCenter();
        if (center == null || !center.closerThan(pos, SIZE * 2)) return false;
        List<BlockPos> surface = shell(center);
        if (!surface.contains(pos)) return false;
        // A surviving single block or a rebuilt shell is not a complete rally nest.
        for (BlockPos part : surface) {
            if (!level.hasChunkAt(part) || !level.getBlockState(part).is(ModBlocks.ZBR_FLESH)
                    || !(level.getBlockEntity(part) instanceof ZBRFleshBlockEntity member)
                    || !center.equals(member.getRallyNestCenter())) return false;
        }
        return true;
    }

    private static final class Job {
        final ServerLevel level;
        final BlockPos center;
        final List<UUID> corpses;
        int age;
        int consumed;
        int stage;
        Job(ServerLevel level, BlockPos center, List<UUID> corpses) {
            this.level = level; this.center = center.immutable(); this.corpses = corpses;
        }
    }
}
