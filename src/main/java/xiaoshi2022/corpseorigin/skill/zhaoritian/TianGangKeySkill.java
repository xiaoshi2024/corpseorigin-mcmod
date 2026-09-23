package xiaoshi2022.corpseorigin.skill.zhaoritian;

import com.geckolib.animatable.GeoItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.InnerPowerManager;
import net.minecraft.world.InteractionHand;
import xiaoshi2022.corpseorigin.item.weapon.TianGangKeyItem;
import xiaoshi2022.corpseorigin.network.TianGangBeamPayload;
import xiaoshi2022.corpseorigin.skill.AbstractSkill;
import xiaoshi2022.corpseorigin.skill.SkillType;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.QiEffects;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** All resource checks are routed through SkillManager; targets never come from the client. */
public final class TianGangKeySkill extends AbstractSkill {
    public static final String PATH = "tian_gang_blood_lotus";
    public static final int CHARGE_TICKS = 24;
    public static final int DRAIN_PERIOD = 50, DRAIN_POINTS = 3, DRAIN_COST = 1, DAMAGE_INTERVAL = 10;
    public static final double RANGE = 50;
    private static final float DAMAGE = 30;
    private static final Map<UUID, Cast> CASTS = new HashMap<>();

    public TianGangKeySkill() { super(PATH, SkillType.ULTIMATE, 400, 3, 1, true, 10); }
    @Override public Component checkUsable(ServerPlayer player) {
        if (!"zhaoritian".equals(CharacterManager.getInstance().getPlayerCharacterId(player)))
            return Component.translatable("item.corpseorigin.tian_gang_key.wrong_role");
        if (TianGangKeyItem.weaponHand(player) == null)
            return Component.translatable("skill.corpseorigin.tian_gang_blood_lotus.need_key");
        return CASTS.containsKey(player.getUUID()) ? Component.translatable("skill.corpseorigin.tian_gang_blood_lotus.busy") : null;
    }
    @Override public void onActivate(ServerPlayer player) {
        CASTS.put(player.getUUID(), new Cast(player));
        ItemStack stack = player.getItemInHand(TianGangKeyItem.weaponHand(player));
        ((TianGangKeyItem) stack.getItem()).triggerAnim(player, GeoItem.getOrAssignId(stack, player.level()), "main", "fire");
    }
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> CASTS.values().removeIf(Cast::tick));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CASTS.clear());
    }

    public static boolean isChanneling(ServerPlayer player) { return CASTS.containsKey(player.getUUID()); }

    public static void swingWeapon(ServerPlayer player) {
        Cast cast = CASTS.get(player.getUUID());
        if (cast == null || !cast.validOwner() || cast.age < CHARGE_TICKS) return;
        long now = player.level().getGameTime();
        if (now - cast.lastSwingTick < 6) return;
        cast.lastSwingTick = now;
        player.swing(cast.hand, true);
    }

    public static void acceptPose(ServerPlayer player, xiaoshi2022.corpseorigin.network.TianGangBladePosePayload packet) {
        Cast cast = CASTS.get(player.getUUID());
        if (cast == null || !cast.id.equals(packet.cast()) || !cast.validOwner()
                || !TianGangBladeSweep.valid(packet.rootOffset(), packet.direction())) return;
        long tick = player.level().getGameTime();
        if (cast.receivedTick == tick) return;
        // Do not sweep across teleports, camera switches, or gaps in pose delivery.
        if (tick - cast.receivedTick > 3 || cast.firstPerson != packet.firstPerson()
                || (cast.receivedPlayerPosition != null && cast.receivedPlayerPosition.distanceToSqr(player.position()) > 16))
            cast.previous = null;
        cast.firstPerson = packet.firstPerson();
        cast.receivedPlayerPosition = player.position();
        cast.receivedTick = tick;
        cast.pending = new TianGangBladeSweep.Pose(player.position().add(packet.rootOffset()), packet.direction().normalize());
    }

    public static void cancel(ServerPlayer player) {
        Cast cast = CASTS.remove(player.getUUID());
        if (cast != null) cast.stop();
    }

    private static final class Cast {
        final ServerPlayer player;
        final ServerLevel level;
        final ItemStack weapon;
        final InteractionHand hand;
        long lastSwingTick = Long.MIN_VALUE / 2;
        final UUID id = UUID.randomUUID();
        final Map<UUID, Long> nextHit = new HashMap<>();
        long receivedTick = Long.MIN_VALUE / 2;
        Vec3 receivedPlayerPosition;
        boolean firstPerson;
        TianGangBladeSweep.Pose pending, previous;
        int age;
        Cast(ServerPlayer player) {
            this.player = player; level = player.level(); hand = TianGangKeyItem.weaponHand(player);
            weapon = player.getItemInHand(hand);
        }

        boolean validOwner() {
            return !player.isRemoved() && player.isAlive() && !player.isSpectator() && player.level() == level
                    && player.getItemInHand(hand) == weapon
                    && "zhaoritian".equals(CharacterManager.getInstance().getPlayerCharacterId(player));
        }
        boolean tick() {
            if (!validOwner()
                    || (age < CHARGE_TICKS && InnerPowerManager.getInnerPower(player) <= 0)) {
                stop();
                return true;
            }
            boolean firing = age >= CHARGE_TICKS;
            int firingTick = age - CHARGE_TICKS;
            // Three evenly spaced points per 50 ticks: 60 points fund exactly 1000 firing ticks.
            boolean paymentDue = firing && (firingTick == 0
                    || (long) firingTick * DRAIN_POINTS / DRAIN_PERIOD
                    > (long) (firingTick - 1) * DRAIN_POINTS / DRAIN_PERIOD);
            if (paymentDue
                    && !InnerPowerManager.consume(player, DRAIN_COST)) {
                stop();
                return true;
            }
            age++;
            broadcast(Vec3.ZERO, firing, 10);
            if (!firing) {
                if (age % 6 == 0) QiEffects.aura(player, "tiangang_charge", 0xD01035, 1.2f, 8);
                pending = null;
                previous = null;
                return false;
            }
            long now = level.getGameTime();
            nextHit.values().removeIf(expiry -> expiry <= now);
            if (pending == null) {
                if (now - receivedTick > 3) previous = null;
                return false;
            }
            var current = pending;
            pending = null;
            var from = previous == null ? current : previous;
            previous = current;
            // Broad phase once per tick; bounded angular sampling covers fast 50-block sweeps.
            var candidates = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(from.root(), current.root()).inflate(RANGE + .4),
                    e -> ChapterCombat.canHit(player, e) && !nextHit.containsKey(e.getUUID()));
            if (candidates.isEmpty()) return false;
            int steps = TianGangBladeSweep.steps(from, current);
            for (int i = 0; i <= steps; i++) {
                var pose = TianGangBladeSweep.interpolate(from, current, i / (double) steps);
                Vec3 start = pose.root(), direction = pose.direction();
                // Prevent an animated muzzle on the far side of a wall from bypassing it.
                if (!clearPath(player.getEyePosition(), start)) continue;
                double range = 0;
                for (int j = 0; j <= RANGE * 2; j++) {
                    if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(start.add(direction.scale(j * .5))))) break;
                    range = j * .5;
                }
                Vec3 end = level.clip(new ClipContext(start, start.add(direction.scale(range)),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
                for (LivingEntity target : candidates) {
                if (nextHit.containsKey(target.getUUID())) continue;
                var box = target.getBoundingBox().inflate(.3);
                Vec3 contact = box.contains(start) ? start : box.clip(start, end).orElse(null);
                if (contact == null) continue;
                if (!clearPath(start, contact) || !clearPath(start, target.getBoundingBox().getCenter())) continue;
                nextHit.put(target.getUUID(), now + DAMAGE_INTERVAL);
                if (target.hurtServer(level, player.damageSources().playerAttack(player), DAMAGE)) {
                    target.knockback(.2, -direction.x, -direction.z, player.damageSources().playerAttack(player), DAMAGE);
                    QiEffects.cloud(level, contact, 0xFF173E, .8f, 12);
                }
                }
            }
            return false;
        }
        boolean clearPath(Vec3 start, Vec3 end) {
            int steps = Math.max(1, (int) Math.ceil(start.distanceTo(end) * 2));
            for (int i = 0; i <= steps; i++)
                if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(start.lerp(end, i / (double) steps)))) return false;
            return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player)).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
        }
        void stop() {
            broadcast(Vec3.ZERO, false, 0);
            ((TianGangKeyItem) weapon.getItem()).triggerAnim(player,
                    GeoItem.getOrAssignId(weapon, level), "main", "rest");
        }
        void broadcast(Vec3 end, boolean firing, int ticks) {
            var packet = new TianGangBeamPayload(player.getUUID(), id, hand, end, ticks > 0 ? RANGE : 0, firing, ticks);
            level.getPlayers(p -> p.distanceToSqr(player) < 160 * 160)
                    .forEach(p -> ServerPlayNetworking.send(p, packet));
        }
    }
}
