package xiaoshi2022.corpseorigin.growth;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import xiaoshi2022.corpseorigin.CorpseOrigin;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.component.PlayerCorpseComponent;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.event.EvolutionEventHandler;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import java.util.List;

/** Server-authoritative body traits and one-time personal opportunities. */
public final class SurvivalGrowth {
    public static final List<String> TRAITS = List.of("wings", "gills", "vampire");
    public static final AttachmentType<CompoundTag> BODY = AttachmentRegistry.create(CorpseOrigin.id("evolution_parts"),
            b -> b.initializer(CompoundTag::new).persistent(CompoundTag.CODEC).copyOnDeath()
                    .syncWith(ByteBufCodecs.COMPOUND_TAG, AttachmentSyncPredicate.all()));
    // Personal milestones deliberately do not travel with shells: swapping/cloning cannot farm them.
    public static final AttachmentType<CompoundTag> JOURNAL = AttachmentRegistry.create(CorpseOrigin.id("growth_journal"),
            b -> b.initializer(CompoundTag::new).persistent(CompoundTag.CODEC).copyOnDeath()
                    .syncWith(ByteBufCodecs.COMPOUND_TAG, AttachmentSyncPredicate.all()));
    private static final TagKey<EntityType<?>> FLYING = preyTag("flying_prey");
    private static final TagKey<EntityType<?>> AQUATIC = preyTag("aquatic_prey");
    private static final TagKey<EntityType<?>> VAMPIRE = preyTag("vampire_donors");
    private static TagKey<EntityType<?>> preyTag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, CorpseOrigin.id(name));
    }
    private SurvivalGrowth() {}
    public static boolean has(Player player, String trait) {
        return player.getAttachedOrCreate(BODY).getBooleanOr(trait, false);
    }
    public static boolean active(ServerPlayer player, String trait) {
        return CorpseConfig.get().growth.enabled && PlayerCorpseComponent.isCorpse(player) && has(player, trait);
    }
    private static boolean eligible(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }
    private static boolean claim(ServerPlayer player, String event) {
        var journal = player.getAttachedOrCreate(JOURNAL).copy();
        if (journal.getBooleanOr(event, false)) return false;
        journal.putBoolean(event, true);
        player.setAttached(JOURNAL, journal);
        return true;
    }
    public static void register() {
        OrganEnergy.register();
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, taken, blocked) -> {
            if (taken > 0 && source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player
                    && target != player && !target.isAlliedTo(player) && eligible(player) && active(player, "vampire"))
                player.heal(Math.min(OrganEvolution.power(player,"vampire")?3:2, taken * (OrganEvolution.power(player,"vampire")?.20f:.15f)));
        });
        UseEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND
                    || !CorpseConfig.get().growth.enabled || !eligible(serverPlayer)
                    || !(target instanceof LivingEntity living) || !living.isAlive()
                    || player.distanceToSqr(target) > 25 || !player.hasLineOfSight(target)) return InteractionResult.PASS;
            if (FreeGrowth.isFree(serverPlayer) && player.isShiftKeyDown()
                    && target instanceof net.minecraft.world.entity.npc.villager.Villager
                    && player.getMainHandItem().is(net.minecraft.world.item.Items.EMERALD)) {
                String lesson="village_training:"+Math.floorDiv(level.getGameTime(),24000);
                if(claim(serverPlayer,lesson)) {
                    player.getMainHandItem().shrink(1);
                    FreeGrowth.opportunity(serverPlayer,lesson);
                    EvolutionEventHandler.awardPoints(serverPlayer,Math.clamp(CorpseConfig.get().growth.villageTrainingPoints,0,100));
                    player.sendSystemMessage(Component.translatable("message.corpseorigin.survival_growth.text_01"));
                } else player.sendOverlayMessage(Component.translatable("message.corpseorigin.survival_growth.text_02"));
                return InteractionResult.SUCCESS;
            }
            for (var teaching : CorpseConfig.get().growth.teachings) {
                if(!CorpseConfig.get().growth.teachingEnabled)break;
                if (teaching == null || teaching.id == null || teaching.id.isBlank()
                        || teaching.npcTag == null || teaching.npcTag.isBlank()
                        || !target.entityTags().contains(teaching.npcTag)) continue;
                var character = CharacterManager.getInstance().getPlayerCharacter(player);
                if (!character.getId().equals(teaching.role) && !FreeGrowth.isFree(character.getId())) continue;
                var skill = character.getSkills().stream().filter(s -> s.getId().getPath().equals(teaching.skill)).findFirst();
                if (skill.isEmpty()) continue;
                boolean unclaimed=!player.getAttachedOrCreate(JOURNAL).getBooleanOr("teaching:"+teaching.id,false);
                long teachingNow=level.getGameTime();
                if(unclaimed && !OpportunityRules.ready(teachingNow,player.getAttachedOrCreate(JOURNAL).getLongOr("last_teaching",Long.MIN_VALUE),CorpseConfig.get().growth.teachingCooldownTicks)){
                    player.sendOverlayMessage(Component.translatable("message.corpseorigin.survival_growth.text_03"));
                    return InteractionResult.SUCCESS;
                }
                FreeGrowth.awaken(serverPlayer);
                boolean first = claim(serverPlayer, "teaching:" + teaching.id);
                if(first){var progress=player.getAttachedOrCreate(JOURNAL).copy();progress.putLong("last_teaching",teachingNow);player.setAttached(JOURNAL,progress);}
                var data = PlayerCharacterData.get(player);
                boolean learned = data.hasLearned(player.getUUID(), teaching.skill);
                // 自由路线学满就不再收新招式（成长点照给 —— 师父该教的还是教了，只是你装不下了）
                boolean capped = !learned && FreeGrowth.skillLimitReached(serverPlayer);
                if (!learned && !capped) data.learnSkill(player.getUUID(), teaching.skill);
                if (first) EvolutionEventHandler.awardPoints(serverPlayer, Math.clamp(teaching.points, 0, 10000));
                if (capped) {
                    player.sendOverlayMessage(Component.translatable(
                            "message.corpseorigin.free_growth.skill_limit", FreeGrowth.SKILL_LIMIT));
                } else if (!learned || first) {
                    CorpseNetwork.sendEvolutionSync(serverPlayer);
                    player.sendSystemMessage(Component.translatable("growth.corpseorigin.taught", skill.get().getName()));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                OrganEvolution.tick(player);
                if (!CorpseConfig.get().growth.enabled) continue;
                var appearance = player.getAttachedOrCreate(BODY).copy();
                if (EvolutionAppearance.initialize(appearance, player.getRandom())) player.setAttached(BODY, appearance);
                if (!eligible(player)) continue;
                if (FreeGrowth.isFree(player) && claim(player,"survival_guide"))
                    player.sendSystemMessage(Component.translatable(
                            "message.yourmod.growth.info",
                            Math.clamp(CorpseConfig.get().growth.preyRequired, 1, 10000),
                            Math.clamp(CorpseConfig.get().growth.fleshPerOpportunity, 1, 10000)
                    ));
                if (player.tickCount % 100 == 0) explore(player);
            }
        });
    }
    private static void explore(ServerPlayer player) {
        ServerLevel level = (ServerLevel)player.level();
        for (var reward : CorpseConfig.get().growth.exploration) {
            if (reward == null || reward.structure == null || reward.points <= 0) continue;
            String event = "exploration:" + reward.structure;
            if (player.getAttachedOrCreate(JOURNAL).getBooleanOr(event, false)) continue;
            Identifier id = Identifier.tryParse(reward.structure);
            if (id == null) continue;
            var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                    .get(ResourceKey.create(Registries.STRUCTURE, id));
            if (structure.isEmpty() || !level.structureManager()
                    .getStructureWithPieceAt(player.blockPosition(), structure.get().value()).isValid()) continue;
            if (claim(player, event)) {
                FreeGrowth.opportunity(player,event);
                int points = Math.clamp(reward.points, 1, 10000);
                int awarded = EvolutionEventHandler.awardPoints(player, points);
                player.sendSystemMessage(Component.translatable("growth.corpseorigin.explored", reward.structure, awarded));
            }
        }
    }
    public static void fleshConsumed(ServerPlayer player) {
        if(!CorpseConfig.get().growth.enabled || !eligible(player) || !FreeGrowth.isFree(player)
                ||!PlayerCorpseComponent.isCorpse(player))return;
        var journal=player.getAttachedOrCreate(JOURNAL).copy();
        int previous=journal.getIntOr("flesh_consumed",0);
        if(previous>=10000)return;
        int count=Math.max(0,previous)+1;
        journal.putInt("flesh_consumed",count);player.setAttached(JOURNAL,journal);
        var cfg=CorpseConfig.get().growth;
        if(count%Math.clamp(cfg.fleshPerGrowthPoint,1,10000)==0)EvolutionEventHandler.awardPoints(player,1);
        int interval=(int)Math.clamp((long)cfg.fleshPerOpportunity*Math.clamp(cfg.fleshOpportunityMultiplier,1,100),1,10000);
        long now=player.level().getGameTime();
        long last=journal.getLongOr("last_flesh_opportunity",Long.MIN_VALUE);
        if(cfg.fleshOpportunitiesEnabled && count%interval==0 && OpportunityRules.ready(now,last,cfg.fleshOpportunityCooldownTicks)){
            journal.putLong("last_flesh_opportunity",now);player.setAttached(JOURNAL,journal);
            FreeGrowth.opportunity(player,"flesh_count:"+count);
        }
    }
}
