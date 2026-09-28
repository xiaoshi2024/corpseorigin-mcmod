package xiaoshi2022.corpseorigin.skill.chapter;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.growth.GourdOrganState;
import xiaoshi2022.corpseorigin.skill.SkillResourceRules;
import xiaoshi2022.corpseorigin.skill.SkillResources;

public final class GourdInheritance {
    // Knowledge belongs to PlayerCharacterData (world save, shell snapshots and memory books).
    // Cooldowns belong to the player: old books and cloned bodies cannot reset them.
    private static final AttachmentType<CompoundTag> COOLDOWNS = AttachmentRegistry.create(CorpseOrigin.id("gourd_trait_cooldowns"),
            b -> b.initializer(CompoundTag::new).persistent(CompoundTag.CODEC).copyOnDeath());
    public static final AttachmentType<String> SELECTED = AttachmentRegistry.create(CorpseOrigin.id("gourd_selected_trait"),
            b -> b.initializer(() -> "").syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.all()));
    public static final AttachmentType<Boolean> DISGUISED = AttachmentRegistry.create(CorpseOrigin.id("gourd_mortal_disguise"),
            b -> b.initializer(() -> false).syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

    private GourdInheritance() {}
    public static boolean role(Player p) {
        return "xiaojingang".equals(p.level().isClientSide()
                ? p.getAttachedOrCreate(ChapterActorState.ROLE)
                : CharacterManager.getInstance().getPlayerCharacterId(p));
    }
    public static boolean disguised(Player p) { return role(p) && p.isAlive() && p.getAttachedOrCreate(DISGUISED); }
    public static boolean active(ServerPlayer p) {
        return CorpseConfig.get().gourdInheritance.enabled && role(p) && p.isAlive() && !p.isSpectator() && !GourdOrganState.dead(p);
    }
    public static GourdTrait selected(Player p) {
        if (!role(p) || !p.isAlive() || p.isSpectator()) return null;
        return p instanceof ServerPlayer server ? GourdMemory.selected(memory(server)) : GourdTrait.byId(p.getAttachedOrCreate(SELECTED));
    }
    public static boolean climbing(Player p) {
        if (p instanceof ServerPlayer sp && !active(sp)) return false;
        GourdTrait t = selected(p);
        return !disguised(p) && (t == GourdTrait.SPIDER || t == GourdTrait.CAVE_SPIDER);
    }
    public static CompoundTag memory(ServerPlayer p) { return PlayerCharacterData.get(p).getGourdMemory(p.getUUID()); }
    public static void reveal(ServerPlayer p) {
        if (p.getAttachedOrCreate(DISGUISED)) {
            p.setAttached(DISGUISED, false);
            p.sendOverlayMessage(Component.translatable("message.corpseorigin.gourd_inherit.revealed"));
        }
    }
    public static void onDevoured(ServerPlayer p, LivingEntity prey, boolean automatic) {
        if (!active(p)) return;
        GourdTrait trait = GourdTrait.donor(BuiltInRegistries.ENTITY_TYPE.getKey(prey.getType()).toString());
        if (trait == null) return;
        CompoundTag memory = memory(p);
        if (GourdMemory.knows(memory, trait)) {
            p.sendSystemMessage(Component.translatable("message.corpseorigin.gourd_inherit.known", Component.translatable(trait.nameKey())));
            return;
        }
        var cfg = CorpseConfig.get().gourdInheritance;
        double chance = switch (trait.tier) {
            case PASSIVE -> cfg.passiveChance;
            case NEUTRAL -> cfg.neutralChance;
            case HOSTILE -> cfg.hostileChance;
            case ELITE -> cfg.eliteChance;
            case BOSS -> cfg.bossChance;
        };
        chance = Double.isFinite(chance) ? Math.clamp(chance, 0, 1) : 0;
        if (automatic) chance *= Double.isFinite(cfg.automaticMultiplier) ? Math.clamp(cfg.automaticMultiplier, 0, 1) : 0;
        if (!GourdTrait.succeeds(p.getRandom().nextDouble(), chance, false)) {
            p.sendSystemMessage(Component.translatable("message.corpseorigin.gourd_inherit.failed", Component.translatable(trait.nameKey())));
            return;
        }
        PlayerCharacterData.get(p).setGourdMemory(p.getUUID(), GourdMemory.learn(memory, trait));
        syncSelection(p);
        p.sendSystemMessage(Component.translatable("message.corpseorigin.gourd_inherit.learned",
                Component.translatable(trait.nameKey()), Component.translatable(trait.descriptionKey())));
    }
    private static long now(ServerPlayer p) { return p.level().getServer().overworld().getGameTime(); }
    public static Component reason(ServerPlayer p, int mode) {
        if (mode == 2 && disguised(p)) return null;
        if (!active(p)) return Component.translatable("message.corpseorigin.gourd_inherit.unavailable");
        if (GourdOrganState.windingUp(p)) return Component.translatable("skill.corpseorigin.gourd_devour.busy");
        if (mode == 0) return GourdMemory.known(memory(p)).isEmpty() ? Component.translatable("message.corpseorigin.gourd_inherit.empty") : null;
        if (mode == 2 || selected(p) == GourdTrait.VILLAGER) return disguiseReason(p);
        GourdTrait trait = selected(p);
        if (trait == null) return Component.translatable("message.corpseorigin.gourd_inherit.empty");
        long until = p.getAttachedOrCreate(COOLDOWNS).getLongOr(trait.id, 0);
        if (until > now(p)) return Component.translatable("message.corpseorigin.gourd_inherit.cooldown", (until - now(p) + 19) / 20);
        return GourdTraitActions.reason(p, trait);
    }
    private static Component disguiseReason(ServerPlayer p) {
        if (!GourdMemory.knows(memory(p), GourdTrait.VILLAGER)) return Component.translatable("message.corpseorigin.gourd_inherit.need_villager");
        if (GourdOrganState.detached(p)) {
            var organ = GourdOrganState.find(p);
            if (organ == null || organ.level() != p.level()) return Component.translatable("skill.corpseorigin.gourd.away");
        }
        return null;
    }
    public static void use(ServerPlayer p, int mode) {
        if (mode == 0) {
            CompoundTag updated = GourdMemory.cycle(memory(p), p.isShiftKeyDown());
            PlayerCharacterData.get(p).setGourdMemory(p.getUUID(), updated);
            syncSelection(p);
            GourdTrait t = GourdMemory.selected(updated);
            if (t != null) p.sendSystemMessage(Component.translatable("message.corpseorigin.gourd_inherit.selected",
                    Component.translatable(t.nameKey()), Component.translatable(t.descriptionKey()), t.blood, t.cooldown / 20));
            return;
        }
        if (mode == 2 || selected(p) == GourdTrait.VILLAGER) {
            if (disguised(p)) { reveal(p); return; }
            if (GourdOrganState.detached(p)) GourdOrganState.toggle(p);
            if (GourdOrganState.detached(p)) return;
            p.setAttached(DISGUISED, true);
            GourdTraitActions.cancel(p);
            p.sendOverlayMessage(Component.translatable("message.corpseorigin.gourd_inherit.disguised"));
            return;
        }
        GourdTrait t = selected(p);
        if (t == null || !SkillResources.pay(p, new SkillResourceRules.Cost(0, t.blood))) return;
        reveal(p);
        GourdTraitActions.cast(p, t);
        CompoundTag cds = p.getAttachedOrCreate(COOLDOWNS).copy();
        cds.putLong(t.id, now(p) + t.cooldown);
        p.setAttached(COOLDOWNS, cds);
    }
    private static void syncSelection(ServerPlayer p) {
        GourdTrait trait = active(p) ? selected(p) : null;
        String id = trait == null ? "" : trait.id;
        if (!p.getAttachedOrCreate(SELECTED).equals(id)) p.setAttached(SELECTED, id);
    }
    public static void register() {
        GourdTraitActions.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                syncSelection(p);
                if (!active(p) || GourdOrganState.detached(p)) reveal(p);
                if (active(p) && !disguised(p)) GourdTraitActions.passive(p, selected(p));
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, base, taken, blocked) -> {
            if (taken > 0 && source.getEntity() instanceof ServerPlayer p) reveal(p);
        });
    }
}
