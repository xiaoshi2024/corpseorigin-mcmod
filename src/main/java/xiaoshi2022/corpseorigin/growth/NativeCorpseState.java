package xiaoshi2022.corpseorigin.growth;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.entity.HybridZombie;

/** Infection is a persistent trait of the original mob, never a replacement entity. */
public final class NativeCorpseState {
    public static final int EYE = 1, RIBS = 2;
    public static final AttachmentType<Integer> APPEARANCE = AttachmentRegistry.create(
            CorpseOrigin.id("native_corpse_appearance"), b -> b.persistent(Codec.INT)
                    .syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));
    public static final AttachmentType<String> ORGANS = AttachmentRegistry.create(
            CorpseOrigin.id("native_corpse_organs"), b -> b.persistent(Codec.STRING)
                    .syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.all()));
    private static final AttachmentType<String> SOURCE_ORGANS = AttachmentRegistry.create(
            CorpseOrigin.id("native_corpse_source_organs"), b -> b.persistent(Codec.STRING));
    public static final AttachmentType<Integer> GENERATION = AttachmentRegistry.create(
            CorpseOrigin.id("native_corpse_generation"), b -> b.initializer(() -> 0)
                    .persistent(Codec.INT).syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));
    // Bosses that reject all potion effects still incubate infection. Server-only and saved with the mob.
    public static final AttachmentType<Integer> INCUBATION = AttachmentRegistry.create(
            CorpseOrigin.id("native_corpse_incubation"), b -> b.initializer(() -> 0).persistent(Codec.INT));

    private NativeCorpseState() {}
    public static void register() { /* Force common-side attachment registration before worlds load. */ }

    public static int generation(Entity entity) {
        Integer value = entity.getAttached(GENERATION);
        return value == null ? 0 : value;
    }

    public static boolean infected(Entity entity) {
        return entity instanceof Mob && generation(entity) > 0;
    }

    public static boolean incubating(Entity entity) {
        Integer value = entity.getAttached(INCUBATION);
        return value != null && value > 0;
    }

    public static int appearance(Entity entity) {
        Integer mask = entity.getAttached(APPEARANCE);
        return mask == null ? 0 : mask;
    }

    public static String organLoadout(Entity entity) {
        String saved = entity.getAttached(ORGANS);
        return saved == null ? "[]" : saved;
    }

    public static java.util.List<OrganSlot> organSlots(Entity entity) {
        return parse(organLoadout(entity));
    }

    private static java.util.List<OrganSlot> parse(String json) {
        try { return OrganLibrary.parseSlots(json); }
        catch (IllegalArgumentException e) { return java.util.List.of(); }
    }

    /** Snapshot the saved GUI loadout before the source logs out or the incubation chunk unloads. */
    public static void captureSource(Entity target, Entity source) {
        if (!(target instanceof Mob)) return;
        String json = "[]";
        if (source instanceof xiaoshi2022.corpseorigin.entity.LowerLevelZbEntity zb) json = zb.getOrganLoadout();
        else if (source != null && infected(source)) json = organLoadout(source);
        else if (source instanceof net.minecraft.world.entity.player.Player player)
            json = player.getAttachedOrCreate(SurvivalGrowth.BODY).getStringOr(OrganLibrary.BODY_KEY, "[]");
        target.setAttached(SOURCE_ORGANS, OrganLibrary.JSON.toJson(parse(json)));
    }

    public static java.util.List<OrganDefinition> activeDefinitions(Mob mob) {
        var ids = organSlots(mob).stream().map(OrganSlot::organ).collect(java.util.stream.Collectors.toSet());
        return OrganLibrary.definitions().stream().filter(d -> ids.contains(d.id())).toList();
    }

    private static void initializeAppearance(Mob mob) {
        if (mob.getAttached(APPEARANCE) != null) return;
        var cfg = CorpseHorror.config();
        var rng = mob.getRandom();
        int flags = (rng.nextDouble() < cfg.nativeEyeChance ? EYE : 0)
                | (rng.nextDouble() < cfg.nativeRibsChance ? RIBS : 0);
        var inherited = new java.util.ArrayList<OrganSlot>();
        String saved = mob.getAttached(SOURCE_ORGANS);
        var source = parse(saved == null ? "[]" : saved).stream().filter(s -> !s.replacesBody()).toList();
        for (var slot : source) {
            if (OrganLibrary.definitions().stream().anyMatch(d -> d.id().equals(slot.organ()))
                    && rng.nextDouble() < cfg.nativeOrganChance) inherited.add(slot);
        }
        if (source.isEmpty() && rng.nextDouble() < cfg.nativeOrganChance) {
            var pool = OrganLibrary.definitions().stream().filter(d -> OrganLibrary.isCustomOrgan(d.id())).toList();
            if (!pool.isEmpty()) {
                var def = pool.get(rng.nextInt(pool.size()));
                inherited.add(new OrganSlot(def.id(), "body", 0, 0, 0, 0, 0, 0, 1, false));
            }
        }
        mob.setAttached(ORGANS, OrganLibrary.JSON.toJson(inherited));
        mob.setAttached(APPEARANCE, flags);
        mob.removeAttached(SOURCE_ORGANS);
    }

    public static void transform(Mob mob, ServerLevel level) {
        if (!mob.isAlive() || infected(mob)) return;
        String prefix = HybridZombie.INFECTION_GENERATION + "_pending:";
        int generation = 1;
        for (String tag : java.util.List.copyOf(mob.entityTags())) {
            if (!tag.startsWith(prefix)) continue;
            try { generation = Math.max(1, Integer.parseInt(tag.substring(prefix.length()))); }
            catch (NumberFormatException ignored) { }
            mob.removeTag(tag);
        }
        HybridZombie.tagAsHybrid(mob, EntityType.getKey(mob.getType()).toString(), HybridZombie.GRADE_HALF, generation);
        mob.setAttached(GENERATION, generation);
        initializeAppearance(mob);
        mob.removeAttached(INCUBATION);
        CorpseHorror.blood(level, mob.position(), 6);
    }

    public static void tick(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !mob.isAlive()) return;
        if (infected(mob)) {
            initializeAppearance(mob);
            xiaoshi2022.corpseorigin.entity.evolution.ZbOrganEffects.tick(mob);
            if (activeDefinitions(mob).stream().anyMatch(d -> "gills".equals(d.trait())))
                mob.setAirSupply(mob.getMaxAirSupply());
        }
        Integer remaining = mob.getAttached(INCUBATION);
        if (remaining == null) return;
        if (remaining <= 1) {
            transform(mob, level);
            mob.removeAttached(INCUBATION);
            xiaoshi2022.corpseorigin.effect.BYeffect.clearInfectionSource(mob.getUUID());
            xiaoshi2022.corpseorigin.effect.BYeffect.clearTotalDuration(mob.getUUID());
        } else mob.setAttached(INCUBATION, remaining - 1);
    }
}
