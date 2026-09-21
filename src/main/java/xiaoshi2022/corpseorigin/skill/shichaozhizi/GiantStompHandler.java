package xiaoshi2022.corpseorigin.skill.shichaozhizi;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.ShiChaoZhiZi;

import java.util.Map;
import java.util.WeakHashMap;

public final class GiantStompHandler {
    private static final float DAMAGE = 36.0F;
    private static final int STEP_COOLDOWN = 10;
    private static final double STEP_DISTANCE = 0.6;
    private static final Map<ServerPlayer, StepState> STEPS = new WeakHashMap<>();

    private GiantStompHandler() {
    }

    public static void tick(MinecraftServer server) {
        STEPS.keySet().removeIf(player -> player.isRemoved() || !player.isAlive()
                || !ShiChaoZhiZi.isSecondForm(player));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator() || !ShiChaoZhiZi.isSecondForm(player)) {
                STEPS.remove(player);
                continue;
            }
            StepState state = STEPS.computeIfAbsent(player, StepState::new);
            Vec3 movement = player.position().subtract(state.position);
            boolean landed = player.onGround() && !state.onGround;
            boolean discontinuity = state.level != player.level() || movement.lengthSqr() > 64.0;
            state.position = player.position();
            state.level = player.level();
            state.onGround = player.onGround();
            if (state.cooldown > 0) state.cooldown--;
            if (discontinuity || player.isPassenger() || player.isInWater()
                    || player.getAbilities().flying || !player.onGround()) {
                state.distance = 0.0;
                continue;
            }
            double horizontalMovement = movement.horizontalDistance();
            state.distance += horizontalMovement;
            boolean walkingStep = horizontalMovement > 0.001 && state.distance >= STEP_DISTANCE;
            if (state.cooldown == 0 && (landed || walkingStep)) {
                stomp(player);
                state.distance = 0.0;
                state.cooldown = STEP_COOLDOWN;
            }
        }
    }

    private static void stomp(ServerPlayer player) {
        ServerLevel level = player.level();
        AABB body = player.getBoundingBox();
        // Only the footprint and lower legs can crush a target, not the entire giant body.
        AABB feet = new AABB(body.minX, body.minY - 0.25, body.minZ,
                body.maxX, body.minY + Math.min(2.5, player.getBbHeight() * 0.25), body.maxZ);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, feet)) {
            if (target == player || !target.isAlive() || target.isSpectator()
                    || target.getBbHeight() >= player.getBbHeight() * 0.5
                    || target.getBbWidth() >= player.getBbWidth()
                    || target.getBoundingBox().minY < body.minY - 0.5
                    || player.isAlliedTo(target) || player.isPassengerOfSameVehicle(target)) {
                continue;
            }
            if (target instanceof Player other
                    && (other.isCreative() || !player.canHarmPlayer(other))) continue;
            Vec3 origin = player.position().add(0.0, 0.1, 0.0);
            if (level.clip(new ClipContext(origin, target.getBoundingBox().getCenter(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType()
                    != HitResult.Type.MISS) continue;
            // Normal physical damage preserves armor, toughness, enchantments and kill credit.
            target.hurtServer(level, level.damageSources().playerAttack(player), DAMAGE);
        }
    }

    private static final class StepState {
        private Vec3 position;
        private ServerLevel level;
        private boolean onGround;
        private double distance;
        private int cooldown;

        private StepState(ServerPlayer player) {
            position = player.position();
            level = player.level();
            onGround = player.onGround();
        }
    }
}
