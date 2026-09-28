package xiaoshi2022.corpseorigin.growth;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.InnerPowerManager;
import xiaoshi2022.corpseorigin.character.PlayerCharacterData;
import xiaoshi2022.corpseorigin.config.CorpseConfig;
import xiaoshi2022.corpseorigin.mixin.RealmAttributeRangeMixin;
import xiaoshi2022.corpseorigin.network.CorpseNetwork;
import xiaoshi2022.corpseorigin.registry.ModItems;
import xiaoshi2022.corpseorigin.skill.EvolutionManager;
import xiaoshi2022.corpseorigin.skill.EvolutionStats;
import xiaoshi2022.corpseorigin.skill.EvolutionTier;
import xiaoshi2022.corpseorigin.skill.chapter.ChapterCombat;
import xiaoshi2022.corpseorigin.skill.chapter.GroundShockwave;
import xiaoshi2022.corpseorigin.skill.chapter.ImpactTerrain;
import xiaoshi2022.corpseorigin.skill.longyou.BloodReserve;

import java.util.*;

/** One authoritative point ledger, independent practice, and repeatable late-game investments. */
public final class RealmProgression {
    private RealmProgression() {}
    public static final List<String> STATS = List.of("vitality", "power", "guard", "qi", "recovery", "agility");
    private static Component statName(String stat){return Component.translatable("realm.corpseorigin.stat."+stat);}
    private static final Map<UUID, BalanceRules.Window> OFFENSE = new HashMap<>(), DEFENSE = new HashMap<>();
    private record Meditation(ServerPlayer player, Object level, Vec3 anchor, int ticks) {}
    private static final Map<UUID, Meditation> MEDITATING = new HashMap<>();
    private static final Map<UUID, Meditation> MOTION = new HashMap<>();
    private static final List<TerrainBurst> TERRAIN = new ArrayList<>();
    public static RealmConfig config() { return CorpseConfig.get().realm; }

    public static void initialize() {
        config().sanitize();
        EvolutionManager.configure(config().difficultyMultiplier);
        EvolutionManager.preserveLegacyProgress(config().preserveExistingProgress);
        RealmNetworking.register();
        // Numeric capacity only: do not alter default health or damage of vanilla entities.
        ((RealmAttributeRangeMixin) Attributes.MAX_HEALTH.value()).corpseorigin$setMaximum(RealmRules.ATTRIBUTE_CAP);
        ((RealmAttributeRangeMixin) Attributes.ATTACK_DAMAGE.value()).corpseorigin$setMaximum(RealmRules.ATTRIBUTE_CAP);
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            var root = Commands.literal("corpsegrowth").executes(c -> status(c.getSource().getPlayerOrException()));
            var buy = Commands.literal("train");
            for (String stat : STATS) buy.then(Commands.literal(stat)
                    .executes(c -> purchase(c.getSource().getPlayerOrException(), stat, 1))
                    .then(Commands.argument("count", IntegerArgumentType.integer(1,1000))
                            .executes(c -> purchase(c.getSource().getPlayerOrException(), stat, IntegerArgumentType.getInteger(c,"count")))));
            root.then(buy);
            root.then(Commands.literal("meditate").executes(c -> meditate(c.getSource().getPlayerOrException())));
            var refine = Commands.literal("refine");
            for (String profession : List.of("blood", "qi", "medicine")) refine.then(Commands.literal(profession)
                    .executes(c -> refine(c.getSource().getPlayerOrException(), profession)));
            root.then(refine);
            root.then(Commands.literal("burst").executes(c -> burst(c.getSource().getPlayerOrException())));
            root.then(Commands.literal("recharge").executes(c -> recharge(c.getSource().getPlayerOrException())));
            dispatcher.register(root);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, taken, blocked) -> {
            if (taken <= 0 || !Float.isFinite(taken)) return;
            if (source.getEntity() instanceof ServerPlayer attacker && eligible(attacker) && ChapterCombat.canHit(attacker,target)) {
                int gain = OFFENSE.computeIfAbsent(attacker.getUUID(), k -> new BalanceRules.Window())
                        .take(attacker.level().getGameTime(), 1, config().combatXpPerSecond);
                practice(attacker,"power",gain);
            }
            if (target instanceof ServerPlayer defender && eligible(defender) && source.getEntity() instanceof LivingEntity attacker
                    && attacker != defender && !attacker.isAlliedTo(defender)) {
                int gain = DEFENSE.computeIfAbsent(defender.getUUID(), k -> new BalanceRules.Window())
                        .take(defender.level().getGameTime(),1,config().combatXpPerSecond);
                practice(defender,"guard",gain);
                MEDITATING.remove(defender.getUUID());
            }
        });
        // Killing blows may not produce AFTER_DAMAGE in vanilla's death branch.
        ServerLivingEntityEvents.AFTER_DEATH.register((target, source) -> {
            if (source.getEntity() instanceof ServerPlayer p && eligible(p) && target != p && !target.isAlliedTo(p)) {
                int gain = OFFENSE.computeIfAbsent(p.getUUID(), k -> new BalanceRules.Window())
                        .take(p.level().getGameTime(),1,config().combatXpPerSecond);
                practice(p,"power",gain);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var p : server.getPlayerList().getPlayers()) {
                tickMeditation(p);
                if (p.tickCount % 20 != 0 || !p.isAlive()) continue;
                var previous=MOTION.put(p.getUUID(),new Meditation(p,p.level(),p.position(),0));
                if(previous!=null && previous.player==p && previous.level==p.level() && p.isSprinting() && p.onGround()
                        && !p.isPassenger() && !p.getAbilities().flying && !p.isFallFlying()
                        && p.position().distanceToSqr(previous.anchor)>=4 && p.position().distanceToSqr(previous.anchor)<=256)
                    practice(p,"agility",1);
                EvolutionStats.reconcile(p);
                if (!eligible(p) || level(p) < 5 || p.getFoodData().getFoodLevel() <= 6) continue;
                if (p.getHealth() < p.getMaxHealth()) {
                    double ratio = RealmRules.regeneration(level(p),rank(p,"recovery"));
                    p.heal((float)(p.getMaxHealth() * ratio));
                    p.causeFoodExhaustion(.2f);
                }
            }
            TERRAIN.removeIf(TerrainBurst::tick);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h,s) -> {
            UUID id=h.player.getUUID(); OFFENSE.remove(id); DEFENSE.remove(id); MEDITATING.remove(id); MOTION.remove(id);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { OFFENSE.clear(); DEFENSE.clear(); MEDITATING.clear(); MOTION.clear(); TERRAIN.clear(); });
    }

    public static int level(ServerPlayer p) { return EvolutionManager.getLevel(PlayerCharacterData.get(p).getEarnedPoints(p.getUUID())); }
    private static boolean eligible(ServerPlayer p) { return config().enabled && p.isAlive() && !p.isCreative() && !p.isSpectator(); }
    public static int rank(ServerPlayer p, String stat) {
        var t = PlayerCharacterData.get(p).cultivation(p.getUUID());
        return (int)Math.min(config().maxTrainingRank, (long)Math.max(0,t.getIntOr("rank_"+stat,0))
                + Math.max(0,t.getLongOr("xp_"+stat,0)) / config().practiceXpPerRank);
    }
    public static double healthBonus(ServerPlayer p) { return RealmRules.trained(RealmRules.health(level(p)) * config().statMultiplier + 4,rank(p,"vitality")) - 4; }
    public static double attackBonus(ServerPlayer p) { return RealmRules.trained(RealmRules.attack(level(p)) * config().statMultiplier + 1,rank(p,"power")) - 1; }
    public static int qiBonus(ServerPlayer p) { return config().enabled ? (int)Math.min(100000000,
            RealmRules.trained(RealmRules.qi(level(p)) * config().statMultiplier,rank(p,"qi"))) : 0; }
    public static int qiRegen(ServerPlayer p) { return config().enabled ? Math.max(1, qiBonus(p) / 1000 + rank(p,"recovery") / 20) : 1; }

    /** Invoked once at LivingEntity.hurtServer entry, also covers fixed-damage skills/projectiles. */
    public static float adjustDamage(LivingEntity target, DamageSource source, float amount) {
        if (!config().enabled || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || amount <= 0 || !Float.isFinite(amount)) return amount;
        if (source.getEntity() instanceof ServerPlayer attacker && attacker != target) {
            double qiMultiplier = source instanceof xiaoshi2022.corpseorigin.skill.QiSkillDamageSource
                    ? RealmRules.qiSkillMultiplier(InnerPowerManager.getMaxInnerPower(attacker), level(attacker), config()) : 1;
            amount = RealmRules.damage(amount, attackBonus(attacker), qiMultiplier);
        }
        if (target instanceof ServerPlayer player) {
            amount *= (float)(1 - RealmRules.protection(level(player),rank(player,"guard")));
        }
        return amount;
    }

    public static void practice(ServerPlayer p, String stat, int xp) {
        if (!eligible(p) || xp <= 0 || !STATS.contains(stat)) return;
        var data=PlayerCharacterData.get(p); var tag=data.cultivation(p.getUUID());
        long cap=(long)config().maxTrainingRank * config().practiceXpPerRank;
        tag.putLong("xp_"+stat,Math.min(cap,Math.max(0,tag.getLongOr("xp_"+stat,0)) + xp));
        data.setCultivation(p.getUUID(),tag);
    }
    public static void onMeal(ServerPlayer p, ItemStack stack) {
        if (stack.is(ConventionalItemTags.RAW_MEAT_FOODS) || stack.is(ConventionalItemTags.RAW_FISH_FOODS)) practice(p,"vitality",1);
    }
    public static void onFlesh(ServerPlayer p) { practice(p,"vitality",config().fleshPracticeXp); }

    private static void message(ServerPlayer p, String key, Object... args) { p.sendSystemMessage(Component.translatable("realm.corpseorigin."+key,args)); }
    public static int status(ServerPlayer p) {
        var data=PlayerCharacterData.get(p);
        message(p,"status",EvolutionTier.formatFullName(level(p)),data.getEarnedPoints(p.getUUID()),
                data.getAvailablePoints(p.getUUID()),EvolutionManager.pointsToNextLevel(data.getEarnedPoints(p.getUUID())));
        message(p,"attributes",Math.round(p.getHealth()),Math.round(p.getMaxHealth()),Math.round(p.getAttributeValue(Attributes.ATTACK_DAMAGE)),
                String.format(Locale.ROOT,"%.1f",100*RealmRules.protection(level(p),rank(p,"guard"))),InnerPowerManager.getMaxInnerPower(p));
        var tag=data.cultivation(p.getUUID());
        for (int i=0;i<STATS.size();i++) {
            String stat=STATS.get(i); int paid=tag.getIntOr("rank_"+stat,0);
            message(p,"stat_status",statName(stat),stat,rank(p,stat),paid,tag.getLongOr("xp_"+stat,0),RealmRules.cost(paid,1,config().trainingCostBase,config().trainingCostStep));
        }
        message(p,"help");
        message(p,"jobs",tag.getIntOr("job_blood",0),tag.getIntOr("job_qi",0),tag.getIntOr("job_medicine",0));
        return 1;
    }
    public static int purchase(ServerPlayer p, String stat, int count) {
        if (!eligible(p) || !STATS.contains(stat) || count < 1 || count > 1000) return 0;
        var data=PlayerCharacterData.get(p); var tag=data.cultivation(p.getUUID());
        int paid=Math.max(0,tag.getIntOr("rank_"+stat,0));
        if ((long)rank(p,stat)+count > config().maxTrainingRank) { message(p,"limit"); return 0; }
        long cost=RealmRules.cost(paid,count,config().trainingCostBase,config().trainingCostStep);
        if (cost > Integer.MAX_VALUE || !data.spendPoints(p.getUUID(),(int)cost)) { message(p,"insufficient",cost); return 0; }
        tag.putInt("rank_"+stat,paid+count); data.setCultivation(p.getUUID(),tag);
        EvolutionStats.reconcile(p);
        CorpseNetwork.sendEvolutionSync(p); InnerPowerManager.syncTo(p);
        message(p,"purchased",statName(stat),count,cost);
        return 1;
    }
    public static int meditate(ServerPlayer p) {
        if (!eligible(p)) return 0;
        if (MEDITATING.remove(p.getUUID()) != null) { message(p,"meditation_stopped"); return 1; }
        MEDITATING.put(p.getUUID(),new Meditation(p,p.level(),p.position(),-40));
        message(p,"meditation_start",config().meditationTicks);
        return 1;
    }
    private static void tickMeditation(ServerPlayer p) {
        Meditation m=MEDITATING.get(p.getUUID()); if (m==null) return;
        if(m.ticks<0 && eligible(p) && m.player==p && m.level==p.level()) {
            MEDITATING.put(p.getUUID(),new Meditation(p,p.level(),p.position(),m.ticks+1));return;
        }
        if (!eligible(p) || m.player!=p || m.level!=p.level() || p.position().distanceToSqr(m.anchor)>.04
                || !p.isShiftKeyDown() || !p.onGround() || p.isPassenger() || p.getFoodData().getFoodLevel()<=6) {
            MEDITATING.remove(p.getUUID()); message(p,"meditation_interrupted"); return;
        }
        int ticks=m.ticks+1;
        if (ticks>=config().meditationTicks) {
            p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-1);
            practice(p,"qi",config().meditationXp); practice(p,"recovery",config().meditationXp);
            ticks=0; p.sendOverlayMessage(Component.translatable("realm.corpseorigin.meditation_gain",config().meditationXp));
        }
        MEDITATING.put(p.getUUID(),new Meditation(p,p.level(),m.anchor,ticks));
    }
    public static int refine(ServerPlayer p, String job) {
        if (!eligible(p) || !List.of("blood","qi","medicine").contains(job)) return 0;
        var data=PlayerCharacterData.get(p); var tag=data.cultivation(p.getUUID());
        long now=p.level().getGameTime();
        if (now<tag.getLongOr("refine_until",0)) { message(p,"refine_cooldown"); return 0; }
        ItemStack stack=p.getMainHandItem();
        boolean valid=switch(job) { case "blood" -> stack.is(ModItems.ZBR_FLESH); case "qi" -> stack.is(Items.AMETHYST_SHARD); default -> stack.is(Items.GOLDEN_CARROT); };
        if (!valid) { message(p,"materials"); return 0; }
        if (job.equals("blood") && !BloodReserve.isEligible(p)) { message(p,"no_blood"); return 0; }
        if (job.equals("qi") && InnerPowerManager.getMaxInnerPower(p)<=0) { message(p,"no_qi"); return 0; }
        int xp=tag.getIntOr("job_"+job,0), proficiency=Math.min(100,xp/100);
        stack.shrink(1); tag.putInt("job_"+job,(int)Math.min(10000L,(long)xp+5)); tag.putLong("refine_until",now+40);
        data.setCultivation(p.getUUID(),tag);
        switch(job) {
            case "blood" -> { BloodReserve.add(p,20+proficiency); onFlesh(p); }
            case "qi" -> { InnerPowerManager.regen(p,200+20*proficiency); practice(p,"qi",8); }
            default -> { p.heal((float)(p.getMaxHealth()*(.02+proficiency*.0002))); practice(p,"recovery",8); }
        }
        message(p,"refined"); return 1;
    }
    public static int burst(ServerPlayer p) {
        if (!eligible(p) || level(p)<15) { message(p,"burst_locked"); return 0; }
        var data=PlayerCharacterData.get(p); var tag=data.cultivation(p.getUUID()); long now=p.level().getGameTime();
        if (now<tag.getLongOr("burst_until",0)) { message(p,"burst_cooldown"); return 0; }
        if (!InnerPowerManager.consume(p,config().burstQiCost)) { message(p,"qi_required",config().burstQiCost); return 0; }
        tag.putLong("burst_until",now+config().burstCooldownTicks); data.setCultivation(p.getUUID(),tag);
        double radius=config().burstRadius; int targets=0;
        for (LivingEntity target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius))) {
            if (target.distanceToSqr(p)>radius*radius || !ChapterCombat.canHit(p,target)) continue;
            if (++targets>512) break;
            target.hurtServer(p.level(),p.damageSources().playerAttack(p),(float)Math.min(RealmRules.ATTRIBUTE_CAP,attackBonus(p)*30));
        }
        for(int i=0;i<32;i++) {
            double a=Math.PI*2*i/32;
            p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    p.getX()+Math.cos(a)*radius*.5,p.getY()+1,p.getZ()+Math.sin(a)*radius*.5,1,0,0,0,0);
        }
        GroundShockwave.spawn(p,p.position(),12,3);
        if (config().burstBreaksTerrain && TERRAIN.size()<4) TERRAIN.add(new TerrainBurst(p));
        message(p,"burst_done",radius,config().burstQiCost,Component.translatable(config().burstBreaksTerrain?"options.on":"options.off")); return 1;
    }

    /** Repeatable point sink even when permanent training has reached its configured limit. */
    public static int recharge(ServerPlayer p) {
        if (!eligible(p)) return 0;
        boolean qi=InnerPowerManager.getInnerPower(p)<InnerPowerManager.getMaxInnerPower(p);
        boolean health=p.getHealth()<p.getMaxHealth();
        boolean blood=BloodReserve.isEligible(p) && BloodReserve.get(p)<BloodReserve.MAX;
        if (!qi && !health && !blood) { message(p,"full"); return 0; }
        if (!PlayerCharacterData.get(p).spendPoints(p.getUUID(),config().rechargePointCost)) { message(p,"insufficient",config().rechargePointCost); return 0; }
        if(health) p.heal(p.getMaxHealth()*.20f);
        if(qi) InnerPowerManager.regen(p,Math.max(1,InnerPowerManager.getMaxInnerPower(p)/4));
        if(blood) BloodReserve.add(p,100);
        CorpseNetwork.sendEvolutionSync(p);
        message(p,"recharged",config().rechargePointCost); return 1;
    }

    /** Bounded, loaded-chunk-only crater. Protection callbacks remain authoritative. */
    private static final class TerrainBurst {
        final ServerPlayer owner;
        final Object level;
        final BlockPos center;
        final int radius;
        int cursor, broken;
        TerrainBurst(ServerPlayer p) { owner=p; level=p.level(); center=p.blockPosition().below(); radius=(int)Math.min(32,config().burstRadius); }
        boolean tick() {
            if (!config().enabled || !config().burstBreaksTerrain || !owner.isAlive() || owner.isRemoved() || owner.level()!=level) return true;
            int side=radius*2+1, total=side*side*9, budget=config().terrainBlocksPerTick;
            for(int checked=0;cursor<total && checked<2048 && budget>0 && broken<config().terrainTotalBlocks;checked++,cursor++) {
                int x=cursor%side-radius,z=(cursor/side)%side-radius,y=cursor/(side*side)-6;
                if(x*x+z*z>radius*radius) continue;
                if(ImpactTerrain.breakBlock(owner,center.offset(x,y,z),false)) { broken++; budget--; }
            }
            return cursor>=total || broken>=config().terrainTotalBlocks;
        }
    }
}
