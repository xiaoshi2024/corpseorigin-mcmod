package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import xiaoshi2022.corpseorigin.CorpseOrigin;

import java.util.*;

/** Bounded player adaptations. Projectiles use swept collision, server damage and vanilla particles. */
public final class GourdTraitActions {
    private static final AttachmentType<Boolean> CHARGED = AttachmentRegistry.create(CorpseOrigin.id("gourd_creeper_charged"), b -> b.initializer(() -> false));
    private static final Map<UUID, Flight> FLIGHTS = new HashMap<>();
    private static final List<Shot> SHOTS = new ArrayList<>();
    private record Flight(ServerPlayer owner, ServerLevel level, long until) {}
    private static final class Shot {
        final ServerPlayer owner;
        final ServerLevel level;
        final GourdTrait trait;
        final LivingEntity target;
        Vec3 position, direction;
        int life = 60;
        Shot(ServerPlayer owner, GourdTrait trait, Vec3 direction, LivingEntity target) {
            this.owner = owner; this.level = owner.level(); this.trait = trait;
            this.position = owner.getEyePosition().add(direction.scale(.6));
            this.direction = direction.normalize(); this.target = target;
        }
    }
    private GourdTraitActions() {}
    private static Component message(String suffix) { return Component.translatable("message.corpseorigin.gourd_inherit." + suffix); }
    private static void effect(LivingEntity target, net.minecraft.core.Holder<MobEffect> type, int ticks, int strength) {
        target.addEffect(new MobEffectInstance(type, ticks, strength, false, false, true));
    }
    private static InteractionHand holding(ServerPlayer p, Item item) {
        return p.getMainHandItem().is(item) ? InteractionHand.MAIN_HAND : p.getOffhandItem().is(item) ? InteractionHand.OFF_HAND : null;
    }
    private static BlockHitResult blockHit(ServerPlayer p, double range) {
        return p.level().clip(new ClipContext(p.getEyePosition(), p.getEyePosition().add(p.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
    }
    private static boolean canBreak(ServerPlayer p, BlockPos pos) {
        var state = p.level().getBlockState(pos);
        return p.mayBuild() && p.level().hasChunkAt(pos) && p.level().getWorldBorder().isWithinBounds(pos)
                && p.level().mayInteract(p, pos) && p.level().getBlockEntity(pos) == null
                && state.getDestroySpeed(p.level(), pos) >= 0
                && PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(p.level(), p, pos, state, null);
    }
    private static Vec3 blinkDestination(ServerPlayer p) {
        Vec3 start = p.position();
        double distance = Math.min(12, p.getEyePosition().distanceTo(blockHit(p, 12).getLocation()) - .7);
        for (double d = distance; d >= 1; d -= .5) {
            Vec3 end = start.add(p.getLookAngle().scale(d));
            BlockPos pos = BlockPos.containing(end);
            if (p.level().hasChunkAt(pos) && p.level().getWorldBorder().isWithinBounds(pos)
                    && end.y >= p.level().getMinY() && end.y + p.getBbHeight() < p.level().getMaxY()
                    && p.level().noCollision(p, p.getBoundingBox().move(end.subtract(start)))) return end;
        }
        return null;
    }
    public static Component reason(ServerPlayer p, GourdTrait t) {
        if (t == GourdTrait.SHEEP && holding(p, Items.SHEARS) == null) return message("need_shears");
        if ((t == GourdTrait.COW || t == GourdTrait.MOOSHROOM) && holding(p, Items.BUCKET) == null
                && (t != GourdTrait.MOOSHROOM || holding(p, Items.BOWL) == null)) return message("need_container");
        if (t == GourdTrait.DOLPHIN && holding(p, Items.COD) == null) return message("need_cod");
        if (t == GourdTrait.ENDERMAN) {
            if (!p.isShiftKeyDown()) return blinkDestination(p) == null ? message("no_room") : null;
            var hit = blockHit(p, 4);
            if (!p.getMainHandItem().isEmpty() || hit.getType() != HitResult.Type.BLOCK
                    || !p.level().getBlockState(hit.getBlockPos()).is(BlockTags.ENDERMAN_HOLDABLE)
                    || !canBreak(p, hit.getBlockPos())) return message("cannot_carry");
        }
        if (t == GourdTrait.ZOMBIE) {
            var hit = blockHit(p, 4);
            if (hit.getType() != HitResult.Type.BLOCK || !p.level().getBlockState(hit.getBlockPos()).is(BlockTags.WOODEN_DOORS)
                    || !canBreak(p, hit.getBlockPos())) return message("need_door");
        }
        return null;
    }
    public static void cast(ServerPlayer p, GourdTrait t) {
        switch (t) {
            case SHEEP -> {
                give(p, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_wool")), 1 + p.getRandom().nextInt(3)));
                InteractionHand hand = holding(p, Items.SHEARS);
                if (hand != null && !p.isCreative()) p.getItemInHand(hand).hurtAndBreak(1, p,
                        hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            case COW, MOOSHROOM -> {
                boolean stew = t == GourdTrait.MOOSHROOM && holding(p, Items.BOWL) != null;
                InteractionHand hand = holding(p, stew ? Items.BOWL : Items.BUCKET);
                if (hand != null) exchange(p, hand, stew ? Items.MUSHROOM_STEW : Items.MILK_BUCKET);
            }
            case PIG -> {
                p.setDeltaMovement(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.1).add(0, .2, 0));
                p.hurtMarked = true;
                effect(p, MobEffects.SPEED, 100, 1);
                ChapterCombat.arc(p, p.getLookAngle(), 3, 65, 6, .9);
            }
            case CHICKEN -> give(p, new ItemStack(Items.EGG));
            case CAT -> give(p, new ItemStack(p.getRandom().nextBoolean() ? Items.RABBIT_HIDE : Items.FEATHER));
            case DOLPHIN -> {
                InteractionHand hand = holding(p, Items.COD);
                if (hand != null && !p.isCreative()) p.getItemInHand(hand).shrink(1);
                BlockPos treasure = p.level().findNearestMapStructure(StructureTags.ON_TREASURE_MAPS, p.blockPosition(), 32, false);
                p.sendSystemMessage(treasure == null ? message("no_treasure")
                        : Component.translatable("message.corpseorigin.gourd_inherit.treasure", treasure.getX(), treasure.getZ()));
                effect(p, MobEffects.DOLPHINS_GRACE, 300, 0);
            }
            case GOAT -> {
                p.setDeltaMovement(p.getLookAngle().multiply(.6, 0, .6).add(0, 1.15, 0));
                p.hurtMarked = true;
                effect(p, MobEffects.SLOW_FALLING, 160, 0);
                ChapterCombat.arc(p, p.getLookAngle(), 3, 45, 8, 1.3);
            }
            case ENDERMAN -> {
                if (p.isShiftKeyDown()) {
                    BlockPos pos = blockHit(p, 4).getBlockPos();
                    ItemStack block = new ItemStack(p.level().getBlockState(pos).getBlock());
                    if (p.level().destroyBlock(pos, false, p)) p.setItemInHand(InteractionHand.MAIN_HAND, block);
                } else {
                    Vec3 end = blinkDestination(p);
                    if (end != null) { p.teleportTo(end.x, end.y, end.z); p.fallDistance = 0; }
                }
            }
            case WOLF -> {
                effect(p, MobEffects.STRENGTH, 200, 0);
                for (Wolf wolf : p.level().getEntitiesOfClass(Wolf.class, p.getBoundingBox().inflate(12), w -> w.isOwnedBy(p))) {
                    effect(wolf, MobEffects.STRENGTH, 200, 0); wolf.heal(2);
                    LivingEntity target = ChapterCombat.aim(p, 16);
                    if (target != null && ChapterCombat.canHit(p, target)) wolf.setTarget(target);
                }
            }
            case ZOMBIFIED_PIGLIN -> { effect(p, MobEffects.STRENGTH, 200, 0); effect(p, MobEffects.RESISTANCE, 100, 0); }
            case PANDA -> { effect(p, MobEffects.REGENERATION, 160, 0); effect(p, MobEffects.ABSORPTION, 160, 0); }
            case ZOMBIE -> p.level().destroyBlock(blockHit(p, 4).getBlockPos(), true, p);
            case CREEPER -> {
                boolean charged = p.getAttachedOrCreate(CHARGED);
                p.setAttached(CHARGED, false);
                blast(p, p.position(), charged ? 5 : 3.5, charged ? 18 : 12, false);
            }
            case SPIDER, CAVE_SPIDER -> {
                p.setDeltaMovement(p.getLookAngle().scale(.85).add(0, .35, 0)); p.hurtMarked = true;
                ChapterCombat.arc(p, p.getLookAngle(), 3, 55, 6, .5);
            }
            case WITCH -> {
                if (p.isShiftKeyDown()) { p.heal(4); p.removeEffect(MobEffects.POISON); }
                else shoot(p, t, p.getLookAngle(), ChapterCombat.aim(p, 24));
            }
            case SLIME, MAGMA_CUBE -> {
                effect(p, MobEffects.ABSORPTION, 160, 0);
                for (int angle : new int[] {-14, 0, 14}) shoot(p, t, ChapterCombat.rotateY(p.getLookAngle(), angle), null);
            }
            case BLAZE, GHAST -> { fly(p); shoot(p, t, p.getLookAngle(), null); }
            case GUARDIAN -> beam(p, 24, 9, false);
            case ELDER_GUARDIAN -> {
                beam(p, 28, 12, false);
                for (LivingEntity target : nearby(p, p.position(), 8)) effect(target, MobEffects.MINING_FATIGUE, 120, 1);
            }
            case SHULKER -> { effect(p, MobEffects.RESISTANCE, 100, 0); shoot(p, t, p.getLookAngle(), ChapterCombat.aim(p, 24)); }
            case WITHER_SKELETON -> {
                LivingEntity target = ChapterCombat.aim(p, 4);
                if (target != null && target.hurtServer(p.level(), p.damageSources().playerAttack(p), 9)) effect(target, MobEffects.WITHER, 100, 0);
            }
            case PHANTOM -> { fly(p); p.setDeltaMovement(p.getLookAngle().scale(1)); p.hurtMarked = true; ChapterCombat.arc(p, p.getLookAngle(), 4, 50, 9, .7); }
            case WARDEN -> beam(p, 24, 18, true);
            case ENDER_DRAGON -> {
                fly(p);
                for (int angle : new int[] {-10, 0, 10}) shoot(p, t, ChapterCombat.rotateY(p.getLookAngle(), angle), null);
            }
            case WITHER -> {
                fly(p);
                for (int angle : new int[] {-12, 0, 12}) shoot(p, t, ChapterCombat.rotateY(p.getLookAngle(), angle), ChapterCombat.aim(p, 28));
            }
            case LLAMA, SKELETON -> shoot(p, t, p.getLookAngle(), null);
            case VILLAGER -> throw new IllegalStateException("Disguise is handled by GourdInheritance");
        }
        p.swing(InteractionHand.MAIN_HAND, true);
    }
    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }
    private static void exchange(ServerPlayer p, InteractionHand hand, Item result) {
        ItemStack held = p.getItemInHand(hand);
        if (!p.isCreative()) held.shrink(1);
        if (held.isEmpty()) p.setItemInHand(hand, new ItemStack(result)); else give(p, new ItemStack(result));
    }
    public static void passive(ServerPlayer p, GourdTrait t) {
        if (t == null) return;
        if (t == GourdTrait.CHICKEN && p.getDeltaMovement().y < 0) {
            p.setDeltaMovement(p.getDeltaMovement().multiply(1, .65, 1)); p.fallDistance = 0; p.hurtMarked = true;
        }
        if (p.tickCount % 20 != 0) return;
        switch (t) {
            case DOLPHIN -> { if (p.isInWater()) { effect(p, MobEffects.DOLPHINS_GRACE, 30, 0); effect(p, MobEffects.WATER_BREATHING, 30, 0); } }
            case GUARDIAN, ELDER_GUARDIAN -> { if (p.isInWater()) effect(p, MobEffects.WATER_BREATHING, 30, 0); }
            case BLAZE, MAGMA_CUBE -> effect(p, MobEffects.FIRE_RESISTANCE, 30, 0);
            case SHULKER -> { if (p.isShiftKeyDown()) effect(p, MobEffects.RESISTANCE, 30, 1); }
            case WITHER -> { if (p.getHealth() <= p.getMaxHealth() * .5) effect(p, MobEffects.RESISTANCE, 30, 0); }
            case PANDA -> { if (p.isShiftKeyDown() && p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() < .001 && p.tickCount % 100 == 0) p.heal(1); }
            case CAT -> {
                for (Mob mob : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(8),
                        m -> Set.of("creeper", "phantom").contains(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath()))) {
                    if (!p.hasLineOfSight(mob)) continue;
                    if (mob.getTarget() == p) mob.setTarget(null);
                    Vec3 away = mob.position().subtract(p.position()).multiply(1, 0, 1).normalize();
                    mob.getNavigation().moveTo(mob.getX() + away.x * 5, mob.getY(), mob.getZ() + away.z * 5, 1.2);
                    if (BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath().equals("phantom")) mob.setDeltaMovement(away.scale(.5).add(0, .2, 0));
                }
            }
            case WARDEN -> {
                for (LivingEntity target : nearby(p, p.position(), 12))
                    if (target.getDeltaMovement().lengthSqr() > .01 || target.swinging) effect(target, MobEffects.GLOWING, 30, 0);
            }
            default -> { }
        }
    }
    private static List<LivingEntity> nearby(ServerPlayer p, Vec3 center, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                t -> ChapterCombat.canHit(p, t) && t.distanceToSqr(center) <= radius * radius && p.hasLineOfSight(t));
    }
    private static void blast(ServerPlayer p, Vec3 center, double radius, float damage, boolean fire) {
        p.level().sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + .5, center.z, 3, .4, .4, .4, 0);
        for (LivingEntity target : nearby(p, center, radius)) {
            // Do not let splash damage cross a wall between impact and target.
            if (p.level().clip(new ClipContext(center, target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() != HitResult.Type.MISS) continue;
            if (target.hurtServer(p.level(), p.damageSources().playerAttack(p), damage)) {
                Vec3 knockback = target.position().subtract(center).normalize();
                target.push(knockback.x * .7, .3, knockback.z * .7); target.hurtMarked = true;
                if (fire) target.igniteForSeconds(4);
            }
        }
    }
    private static void beam(ServerPlayer p, int range, float damage, boolean sonic) {
        LivingEntity target = ChapterCombat.aim(p, range);
        Vec3 start = p.getEyePosition(), end = target == null ? blockHit(p, range).getLocation() : target.getBoundingBox().getCenter();
        int steps = Math.max(1, (int) start.distanceTo(end));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.lerp(end, (double)i / steps);
            p.level().sendParticles(sonic ? ParticleTypes.SONIC_BOOM : ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        if (target != null) target.hurtServer(p.level(), sonic ? p.damageSources().sonicBoom(p) : p.damageSources().indirectMagic(p, p), damage);
    }
    private static void shoot(ServerPlayer p, GourdTrait t, Vec3 direction, LivingEntity target) { SHOTS.add(new Shot(p, t, direction, target)); }
    private static ParticleOptions particle(GourdTrait t) {
        return switch (t) {
            case BLAZE, GHAST, MAGMA_CUBE -> ParticleTypes.FLAME;
            case ENDER_DRAGON -> PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1);
            case WITHER -> ParticleTypes.SMOKE;
            case WITCH -> ParticleTypes.WITCH;
            case SHULKER -> ParticleTypes.END_ROD;
            case SLIME -> ParticleTypes.HAPPY_VILLAGER;
            case LLAMA -> ParticleTypes.SPIT;
            default -> ParticleTypes.CRIT;
        };
    }
    private static boolean tickShot(Shot s) {
        if (--s.life < 0 || !GourdInheritance.active(s.owner) || s.owner.level() != s.level || GourdInheritance.disguised(s.owner)) return false;
        if (s.trait == GourdTrait.SHULKER && s.target != null && s.target.isAlive() && s.target.level() == s.level
                && ChapterCombat.canHit(s.owner, s.target)) {
            Vec3 toward = s.target.getBoundingBox().getCenter().subtract(s.position).normalize();
            s.direction = s.direction.scale(.75).add(toward.scale(.25)).normalize();
        }
        Vec3 end = s.position.add(s.direction.scale(s.trait == GourdTrait.SHULKER ? .5 : 1.1));
        if (!s.level.hasChunkAt(BlockPos.containing(end))) return false;
        var wall = s.level.clip(new ClipContext(s.position, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, s.owner));
        var hit = ProjectileUtil.getEntityHitResult(s.owner, s.position, wall.getLocation(),
                new AABB(s.position, end).inflate(.3), e -> e instanceof LivingEntity t && ChapterCombat.canHit(s.owner, t),
                s.position.distanceToSqr(wall.getLocation()));
        s.level.sendParticles(particle(s.trait), s.position.x, s.position.y, s.position.z, 5, .08, .08, .08, .01);
        if (hit != null) {
            impact(s, hit.getLocation(), (LivingEntity) hit.getEntity()); return false;
        }
        if (wall.getType() == HitResult.Type.BLOCK) {
            impact(s, wall.getLocation().subtract(s.direction.scale(.1)), null); return false;
        }
        s.position = end; return true;
    }
    private static void impact(Shot s, Vec3 position, LivingEntity target) {
        if (s.trait == GourdTrait.GHAST || s.trait == GourdTrait.WITHER || s.trait == GourdTrait.ENDER_DRAGON) {
            blast(s.owner, position, 2.5, s.trait == GourdTrait.GHAST ? 10 : 8, s.trait == GourdTrait.GHAST);
            if (s.trait == GourdTrait.WITHER) for (LivingEntity t : nearby(s.owner, position, 2.5)) effect(t, MobEffects.WITHER, 80, 0);
            return;
        }
        if (target == null) return;
        float damage = switch (s.trait) { case LLAMA -> 3; case SLIME, MAGMA_CUBE -> 3; case SHULKER, WITCH -> 4; default -> 7; };
        if (!target.hurtServer(s.level, s.owner.damageSources().indirectMagic(s.owner, s.owner), damage)) return;
        switch (s.trait) {
            case LLAMA, SLIME -> effect(target, MobEffects.SLOWNESS, 60, 0);
            case MAGMA_CUBE, BLAZE -> target.igniteForSeconds(4);
            case WITCH -> { effect(target, MobEffects.POISON, 80, 0); effect(target, MobEffects.SLOWNESS, 60, 0); }
            case SHULKER -> effect(target, MobEffects.LEVITATION, 60, 0);
            default -> { }
        }
    }
    private static void fly(ServerPlayer p) {
        FLIGHTS.put(p.getUUID(), new Flight(p, p.level(), p.level().getGameTime() + 160));
        effect(p, MobEffects.SLOW_FALLING, 260, 0);
    }
    public static void cancel(ServerPlayer p) {
        FLIGHTS.remove(p.getUUID());
        SHOTS.removeIf(s -> s.owner == p);
    }
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            SHOTS.removeIf(s -> !tickShot(s));
            FLIGHTS.values().removeIf(f -> {
                ServerPlayer p = f.owner;
                if (!GourdInheritance.active(p) || GourdInheritance.disguised(p) || p.level() != f.level
                        || f.level.getGameTime() >= f.until || GourdInheritance.selected(p) == null || !GourdInheritance.selected(p).flying()) return true;
                var input = p.getLastClientInput();
                Vec3 forward = p.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 side = new Vec3(-forward.z, 0, forward.x);
                Vec3 horizontal = forward.scale((input.forward() ? 1 : 0) - (input.backward() ? 1 : 0))
                        .add(side.scale((input.right() ? 1 : 0) - (input.left() ? 1 : 0))).normalize().scale(.45);
                p.setDeltaMovement(horizontal.x, input.jump() ? .35 : input.shift() ? -.3 : -.02, horizontal.z);
                p.fallDistance = 0; p.hurtMarked = true;
                return false;
            });
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer p) || !GourdInheritance.active(p) || GourdInheritance.disguised(p)) return true;
            GourdTrait t = GourdInheritance.selected(p);
            if (source.is(DamageTypes.FALL) && (t == GourdTrait.CHICKEN || t == GourdTrait.CAT || t == GourdTrait.SLIME)) return false;
            if (t == GourdTrait.CREEPER && source.is(DamageTypes.LIGHTNING_BOLT)) p.setAttached(CHARGED, true);
            return true;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
            if (taken <= 0) return;
            if (source.getEntity() instanceof ServerPlayer p && GourdInheritance.active(p) && source.is(DamageTypes.PLAYER_ATTACK)) {
                GourdTrait t = GourdInheritance.selected(p);
                if (t == GourdTrait.CAVE_SPIDER) effect(entity, MobEffects.POISON, 80, 0);
                if (t == GourdTrait.WITHER_SKELETON) effect(entity, MobEffects.WITHER, 80, 0);
            }
            if (entity instanceof ServerPlayer p && GourdInheritance.active(p) && !GourdInheritance.disguised(p)
                    && GourdInheritance.selected(p) == GourdTrait.ZOMBIFIED_PIGLIN) effect(p, MobEffects.STRENGTH, 100, 0);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancel(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { SHOTS.clear(); FLIGHTS.clear(); });
    }
}
