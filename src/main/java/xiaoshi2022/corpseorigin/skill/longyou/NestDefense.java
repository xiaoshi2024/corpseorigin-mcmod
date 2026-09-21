package xiaoshi2022.corpseorigin.skill.longyou;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import xiaoshi2022.corpseorigin.character.*;
import xiaoshi2022.corpseorigin.entity.ZombieKin;
import xiaoshi2022.corpseorigin.registry.ModBlocks;
import xiaoshi2022.corpseorigin.network.NestRadarPayload;
import java.util.*;

public final class NestDefense {
    private static final Map<UUID, Long> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> ALARMS = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final List<Hand> HANDS = new ArrayList<>();
    private NestDefense() {}

    public static boolean authorized(ServerPlayer p) {
        String id = CharacterManager.getInstance().getPlayerCharacterId(p);
        return p.isAlive() && (LongYou.ID.equals(id) || ShiChaoZhiZi.ID.equals(id))
                && PlayerCharacterData.get(p).hasLearned(p.getUUID(), NestSenseSkill.PATH);
    }

    public static void open(ServerPlayer p) {
        if (!authorized(p)) return;
        SESSIONS.put(p.getUUID(), p.level().getGameTime() + 60);
        send(p, true);
    }

    private static BlockPos fleshNear(ServerLevel level, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-5, -4, -5), pos.offset(5, 4, 5))) {
            if (level.hasChunkAt(check) && level.getBlockState(check).is(ModBlocks.ZBR_FLESH)) return check.immutable();
        }
        return null;
    }

    private static boolean intruder(ServerPlayer caster, LivingEntity target) {
        return target != caster && target.isAlive() && !(target instanceof ZombieKin)
                && !target.isAlliedTo(caster)
                && !(target instanceof Player p && (p.isCreative() || p.isSpectator()
                    || LongYou.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(p))
                    || ShiChaoZhiZi.ID.equals(CharacterManager.getInstance().getPlayerCharacterId(p))))
                && target.distanceToSqr(caster) <= 128 * 128;
    }

    private static void send(ServerPlayer p, boolean open) {
        List<NestRadarPayload.Contact> contacts = new ArrayList<>();
        var candidates = p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(128),
                e -> intruder(p, e));
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        for (LivingEntity e : candidates.stream().limit(64).toList()) {
            boolean alarm = ALARMS.getOrDefault(e.getUUID(), 0L) > p.level().getGameTime();
            if (!alarm && fleshNear(p.level(), e.blockPosition()) == null) continue;
            String name = e.getName().getString();
            contacts.add(new NestRadarPayload.Contact(e.getId(), name.substring(0, Math.min(128, name.length())),
                    (float)(e.getX() - p.getX()), (float)(e.getZ() - p.getZ()), alarm));
        }
        ServerPlayNetworking.send(p, new NestRadarPayload(open, contacts));
    }

    public static void action(ServerPlayer p, int targetId, int mode) {
        if (!authorized(p)) return;
        long now = p.level().getGameTime();
        if (mode == -1) { SESSIONS.remove(p.getUUID()); return; }
        if (!SESSIONS.containsKey(p.getUUID())) return;
        if (mode == 0) { SESSIONS.put(p.getUUID(), now + 60); return; }
        if (mode != 1 && mode != 2 || COOLDOWNS.getOrDefault(p.getUUID(), 0L) > now) return;
        if (!(p.level().getEntity(targetId) instanceof LivingEntity victim) || !intruder(p, victim)) return;
        BlockPos anchor = fleshNear(p.level(), victim.blockPosition());
        if (anchor == null || HANDS.stream().anyMatch(h -> h.victim == victim)) return;
        COOLDOWNS.put(p.getUUID(), now + 60);
        HANDS.add(new Hand(p, victim, anchor, mode));
    }

    public static void register() {
        AttackBlockCallback.EVENT.register((p, level, hand, pos, face) -> {
            if (level instanceof ServerLevel server && level.getBlockState(pos).is(ModBlocks.ZBR_FLESH)) {
                ALARMS.put(p.getUUID(), server.getGameTime() + 600);
                for (ServerPlayer watcher : server.players()) {
                    if (authorized(watcher) && watcher != p && watcher.distanceToSqr(p) < 128 * 128) {
                        watcher.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                                "message.corpseorigin.nest_alarm", p.getName()));
                    }
                }
            }
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.overworld().getGameTime();
            ALARMS.values().removeIf(end -> end <= now);
            COOLDOWNS.values().removeIf(end -> end <= now);
            SESSIONS.entrySet().removeIf(entry -> {
                ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
                if (p == null || !authorized(p) || entry.getValue() <= now) return true;
                if (server.getTickCount() % 20 == 0) send(p, false);
                return false;
            });
            HANDS.removeIf(Hand::tick);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            HANDS.clear(); SESSIONS.clear(); ALARMS.clear(); COOLDOWNS.clear();
        });
    }

    public static boolean intercept(Projectile projectile) {
        if (!(projectile.level() instanceof ServerLevel level)) return false;
        Vec3 start = projectile.position(), end = start.add(projectile.getDeltaMovement());
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, projectile));
        if (hit.getType() == HitResult.Type.BLOCK && level.getBlockState(hit.getBlockPos()).is(ModBlocks.ZBR_FLESH)) {
            if (projectile.getOwner() != null) ALARMS.put(projectile.getOwner().getUUID(), level.getGameTime() + 600);
            projectile.discard(); return true;
        }
        return false;
    }

    private static final class Hand {
        final ServerPlayer caster;
        final LivingEntity victim;
        final ServerLevel level;
        final Vec3 origin, capture;
        final int mode;
        final List<Display.BlockDisplay> parts = new ArrayList<>();
        int age;
        Hand(ServerPlayer caster, LivingEntity victim, BlockPos anchor, int mode) {
            this.caster = caster; this.victim = victim; this.level = caster.level(); this.mode = mode;
            origin = Vec3.atCenterOf(anchor);
            capture = victim.position();
            for (int i = 0; i < 7; i++) {
                Display.BlockDisplay part = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level) {
                    @Override public boolean shouldBeSaved() { return false; }
                };
                part.setBlockState(ModBlocks.ZBR_FLESH.defaultBlockState());
                part.setPos(origin); part.setWidth(12); part.setHeight(12);
                part.setTransformationInterpolationDuration(2);
                level.addFreshEntity(part); parts.add(part);
            }
        }
        boolean tick() {
            if (!authorized(caster) || caster.level() != level || !victim.isAlive()
                    || victim.level() != level || !intruder(caster, victim) || ++age > 80) {
                parts.forEach(Entity::discard); return true;
            }
            double growth = Math.min(1, age / 20.0);
            Vec3 forward = victim.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5);
            Vec3 palm = origin.lerp(capture.add(forward), growth);
            for (int i = 0; i < parts.size(); i++) {
                Display.BlockDisplay part = parts.get(i);
                float w = i == 0 ? 4 : i == 1 ? 2 : .65F;
                float h = i == 0 ? 3 : i == 1 ? 4 : 2.5F;
                Vec3 at = palm.add(i < 2 ? -w / 2 : (i - 2) * .8 - 2,
                        i == 1 ? -3 : i >= 2 ? 3 : 0, -.5);
                part.setPos(at);
                part.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                        new Vector3f(w, h * (float)growth, 1), new Quaternionf()));
            }
            if (age >= 20) {
                Vec3 pull = capture.lerp(origin.add(0, 1, 0), Math.min(1, (age - 20) / 40.0));
                // Pull toward the surface, without placing the target inside solid flesh.
                if (level.noCollision(victim, victim.getBoundingBox().move(pull.subtract(victim.position())))) {
                    victim.teleportTo(pull.x, pull.y, pull.z);
                }
                victim.setDeltaMovement(Vec3.ZERO);
                if (victim instanceof Mob mob) mob.getNavigation().stop();
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.SLOWNESS, 10, 10));
                if (age % 10 == 0) {
                    victim.hurtServer(level, level.damageSources().indirectMagic(caster, caster), mode == 1 ? 80 : 200);
                    if (mode == 1) caster.heal(10);
                }
            }
            return false;
        }
    }
}
