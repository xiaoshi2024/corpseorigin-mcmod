package xiaoshi2022.corpseorigin.skill.chapter;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import xiaoshi2022.corpseorigin.character.CharacterManager;
import xiaoshi2022.corpseorigin.entity.*;
import xiaoshi2022.corpseorigin.growth.SurvivalGrowth;
import xiaoshi2022.corpseorigin.registry.*;
import xiaoshi2022.corpseorigin.skill.ISkill;
import xiaoshi2022.corpseorigin.skill.SkillResourceRules;
import xiaoshi2022.corpseorigin.skill.longyou.TianGangCombat;
import java.util.*;

/** Server AI adapters select the actual role's skill catalogue and reuse its cooldown/resource prices. */
public final class CloneRoleSkills {
    private CloneRoleSkills() {}
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final class State {
        final String role;
        int cursor;
        final Map<UUID, Long> swallowMarks = new HashMap<>();
        final List<Pulse> pulses = new ArrayList<>();
        State(String role) { this.role = role; }
    }
    private static final class Pulse {
        final String kind;
        final long until;
        final int period;
        final UUID target;
        long next;
        Pulse(String kind, long next, long until, int period, UUID target) {
            this.kind = kind; this.next = next; this.until = until; this.period = period; this.target = target;
        }
    }
    public static void clear(UUID id) { STATES.remove(id); }
    public static void clearAll() { STATES.clear(); }
    private static State state(CloneAvatarEntity clone) {
        State state = STATES.get(clone.getUUID());
        if (state == null || !state.role.equals(clone.getBodyRole())) {
            state = new State(clone.getBodyRole());
            STATES.put(clone.getUUID(), state);
        }
        return state;
    }

    /** Inventory/possession controls require a player decision and are not automatically fired in combat. */
    public static final Set<String> PLAYER_CONTROLS = Set.of("gourd_link", "gourd_inheritance", "gourd_inheritance_cycle", "gourd_inheritance_use",
            "gourd_mortal_disguise", "flesh_abandon", "peel_shell", "golden_cicada_shell", "water_pollution",
            "nest_sense", "jingang_infant_convergence", "detach_guardian", "merge_guardian", "revive_guardian",
            "flesh_reshape", "son_of_corpse_nest", "gourd_devour");

    public static boolean supported(String path) {
        return switch (path) {
            case "slaughter_awakening", "slaughter_momentum", "water_orb", "spatial_blink", "ancient_poetry_sword",
                 "heart_grab_ambush", "chameleon_disguise", "corpse_fish_eggs", "water_bite", "black_friday_eight",
                 "keeper_melee", "sword_flower", "bag_capture", "summon_swarm", "swarm_bite", "meteor_sword",
                 "tengu_divine_array", "dark_siphon", "hound_unleashed", "killing_incarnation", "round_dance",
                 "severed_arm_strike", "tiger_claw_bee_wheel", "muscle_rage", "pounce_combo", "bat_cloak",
                 "blood_wing_blade", "chrysanthemum_shield", "dog_eye_cannon", "parcel_bomb", "wood_bind",
                 "five_elements_formation", "killing_gas", "blood_lotus_armor", "thousand_eyes", "life_drain_suck",
                 "antenna_block", "special_forces_combat", "defense_stance", "blood_cloud", "blood_lotus",
                 "osmium_gold", "osmium_ice_spike", "ham_summon", "bear_charge", "flame_strike", "swallow_nest",
                 "reverse_formation_fireball", "crimson_blood_spear", "power_strike", "tian_gang_blood_lotus",
                 "mouth_snake", "qi_lock", "tyrant_strike", "corpse_king_infrasound", "corpse_brother_rally",
                 "corpse_king_thunder", "thunder_power", "ball_lightning", "natural_judgment", "xuanwu_body",
                 "tiangang_ji", "tiangang_li", "tiangang_yu", "tiangang_hui", "tiangang_mie", "tiangang_wu",
                 "tiangang_shen", "tiangang_nipo", "tiangang_pogang", "tiangang_tiangangpo",
                 "gourd_arms", "gourd_acid", "gourd_fire", "gourd_eyes", "gourd_power" -> true;
            default -> false;
        };
    }

    public static boolean cast(CloneAvatarEntity clone, LivingEntity target) {
        var role = CharacterManager.getInstance().getCharacter(clone.getBodyRole());
        if (role == null || !ChapterCombat.canHit(clone, target) || !clone.hasLineOfSight(target)) return false;
        List<ISkill> skills = role.getSkills().stream().filter(ISkill::isActivatable)
                .filter(s -> supported(s.getId().getPath()))
                .filter(s -> !xiaoshi2022.corpseorigin.growth.FreeGrowth.isFree(clone.getBodyRole())
                        || (character(clone) != null && character(clone).hasLearnedSkill(s.getId().getPath()))).toList();
        if (skills.isEmpty()) return false;
        State state = state(clone);
        for (int i = 0; i < skills.size(); i++) {
            int index = Math.floorMod(state.cursor + i, skills.size());
            ISkill skill = skills.get(index);
            String path = skill.getId().getPath();
            var body = clone.getAttachedOrCreate(SurvivalGrowth.BODY);
            if (body.getLongOr("clone_skill_cd:" + path, 0) > clone.level().getGameTime()
                    || !affordable(clone, skill.getResourceCost())) continue;
            Item weapon = weaponFor(path);
            if (weapon != null && !clone.getMainHandItem().is(weapon)) continue;
            if (path.startsWith("gourd_") && (body.getBooleanOr("gourd_dead", false) || body.getBooleanOr("gourd_detached", false))) continue;
            CloneWeaponArts.face(clone, target);
            if (!execute(clone, target, path)) continue;
            body = clone.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
            body.putLong("clone_skill_cd:" + path, clone.level().getGameTime() + Math.max(60, skill.getCooldownTicks()));
            clone.setAttached(SurvivalGrowth.BODY, body);
            pay(clone, skill.getResourceCost());
            state.cursor = index + 1;
            clone.setAttached(ChapterScenes.ACTION, actionFor(path));
            clone.setAttached(ChapterScenes.UNTIL, clone.level().getGameTime() + 20);
            clone.swing(InteractionHand.MAIN_HAND, true);
            return true;
        }
        return false;
    }

    public static Item weaponFor(String path) {
        return switch (path) {
            case "meteor_sword" -> ModItems.RED_METEOR_SWORD;
            case "blood_wing_blade", "dark_siphon" -> ModItems.BLOOD_WING_BLADE;
            case "tiger_claw_bee_wheel" -> ModItems.BEE_WHEEL;
            case "tian_gang_blood_lotus" -> ModItems.TIAN_GANG_KEY;
            case "parcel_bomb" -> ModItems.PARCEL_BOMB;
            case "black_friday_eight" -> ModItems.BILLIARD_EIGHT;
            case "ham_summon" -> ModItems.DOG_CAGE;
            default -> null;
        };
    }
    private static String actionFor(String path) {
        return switch (path) {
            case "bear_charge" -> "charge";
            case "pounce_combo" -> "pounce";
            case "muscle_rage" -> "transform";
            case "round_dance" -> "round_dance";
            default -> "cast";
        };
    }

    public static boolean affordable(CloneAvatarEntity clone, SkillResourceRules.Cost cost) {
        CompoundTag body = clone.getAttachedOrCreate(SurvivalGrowth.BODY);
        int maximum = maxInner(clone);
        return cost.affordable(maximum, Math.min(maximum, body.getIntOr("clone_qi", maximum)), body.getIntOr("clone_blood", 100));
    }
    private static int maxInner(CloneAvatarEntity clone) {
        var role = CharacterManager.getInstance().getCharacter(clone.getBodyRole());
        int inherited = xiaoshi2022.corpseorigin.growth.FreeGrowth.isFree(clone.getBodyRole()) && character(clone) != null
                ? character(clone).getCloneInnerCapacity() : 0;
        if(role==null)return inherited;
        // Some selectable roles define a priced combat skill but no player capacity (for example Laura).
        // Give their AI enough reserve for one such cast; free-growth bodies still require inherited capacity.
        int minimum=xiaoshi2022.corpseorigin.growth.FreeGrowth.isFree(clone.getBodyRole())?0:
                role.getSkills().stream().filter(ISkill::isActivatable).mapToInt(s->s.getResourceCost().inner()).max().orElse(0);
        return Math.max(inherited, Math.max(minimum, role.getMaxInnerPower()));
    }
    private static xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent character(CloneAvatarEntity clone) {
        var state=clone.getBodyState();
        return state==null?null:state.getComponent().as(xiaoshi2022.corpseorigin.shell.CharacterShellStateComponent.class);
    }
    public static void pay(CloneAvatarEntity clone, SkillResourceRules.Cost cost) {
        var body = clone.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
        body.putInt("clone_qi", Math.max(0, Math.min(maxInner(clone), body.getIntOr("clone_qi", maxInner(clone))) - cost.inner()));
        body.putInt("clone_blood", Math.max(0, body.getIntOr("clone_blood", 100) - cost.blood()));
        clone.setAttached(SurvivalGrowth.BODY, body);
    }

    private static boolean close(CloneAvatarEntity clone, LivingEntity target, double range) {
        return clone.distanceToSqr(target) <= range * range;
    }
    private static boolean hit(CloneAvatarEntity clone, LivingEntity target, double range, float amount, int color) {
        if (!close(clone, target, range) || !ChapterCombat.canHit(clone, target) || !clone.hasLineOfSight(target)) return false;
        target.hurtServer((ServerLevel) clone.level(), clone.damageSources().mobAttack(clone), amount);
        QiEffects.cloud((ServerLevel) clone.level(), clone.getEyePosition().lerp(target.getEyePosition(), .5), color,
                (float) Math.min(8, Math.max(.5, clone.distanceTo(target) * .5)), 12);
        return true;
    }
    private static void buff(LivingEntity target, Holder<MobEffect> effect, int ticks, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, false, true));
    }
    private static boolean guard(CloneAvatarEntity clone, String key, int ticks, int amplifier) {
        var body = clone.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
        if (body.getLongOr(key, 0) > clone.level().getGameTime()) return false;
        body.putLong(key, clone.level().getGameTime() + ticks);
        clone.setAttached(SurvivalGrowth.BODY, body);
        buff(clone, MobEffects.RESISTANCE, ticks, amplifier);
        QiEffects.aura(clone, key, 0xe8cf68, 2, ticks);
        return true;
    }
    private static void schedule(CloneAvatarEntity clone, LivingEntity target, String kind, int delay, int duration, int period) {
        long now = clone.level().getGameTime();
        state(clone).pulses.removeIf(p -> p.kind.equals(kind));
        state(clone).pulses.add(new Pulse(kind, now + delay, now + delay + duration, period, target == null ? null : target.getUUID()));
    }
    public static boolean startKey(CloneAvatarEntity clone) {
        if (state(clone).pulses.stream().anyMatch(p -> p.kind.equals("key"))) return false;
        schedule(clone, clone.getTarget(), "key", 24, 100, 10);
        clone.setAttached(ChapterScenes.ACTION, "charge");
        clone.setAttached(ChapterScenes.UNTIL, clone.level().getGameTime() + 24);
        QiEffects.aura(clone, "key_charge", 0xdd3366, 2, 24);
        return true;
    }

    private static boolean execute(CloneAvatarEntity c, LivingEntity t, String path) {
        ServerLevel level = (ServerLevel) c.level();
        switch (path) {
            case "water_orb":
                if (!hit(c,t,20,24,0x4fc3f7)) return false;
                buff(t,MobEffects.SLOWNESS,80,2); t.push(c.getLookAngle().x*1.5,.4,c.getLookAngle().z*1.5); return true;
            case "keeper_melee": return hit(c,t,3,6,0xc0182a);
            case "heart_grab_ambush": return hit(c,t,3,14,0xc0182a);
            case "special_forces_combat": return hit(c,t,3.5,12,0xe8e0c8);
            case "power_strike":
                if (!hit(c,t,4,80,0xd8552c)) return false;
                t.push(c.getLookAngle().x*2,.6,c.getLookAngle().z*2); t.hurtMarked=true; return true;
            case "tyrant_strike":
                if (!hit(c,t,4,30,0xc0182a)) return false;
                buff(t,MobEffects.SLOWNESS,80,1); t.push(c.getLookAngle().x,.55,c.getLookAngle().z); return true;
            case "mouth_snake": return hit(c,t,16,12,0x8ce06a);
            case "water_bite":
                if (!hit(c,t,4,c.isInWater()?8:5,0x4fc3f7)) return false;
                c.setDeltaMovement(c.getLookAngle().scale(c.isInWater()?.8:.4)); c.hurtMarked=true; return true;
            case "swarm_bite":
                if (!close(c,t,3)) return false;
                area(c,3,4,0x9cab38); buff(t,MobEffects.POISON,60,0); return true;
            case "slaughter_awakening", "slaughter_momentum":
                if (c.hasEffect(MobEffects.STRENGTH)) return false;
                buff(c,MobEffects.STRENGTH,200,path.equals("slaughter_momentum")?2:1);
                buff(c,MobEffects.SPEED,200,1); buff(c,MobEffects.REGENERATION,200,0); buff(c,MobEffects.RESISTANCE,200,0); return true;
            case "defense_stance", "antenna_block": return guard(c,"clone_guard_until",100,1);
            case "chrysanthemum_shield": return guard(c,"clone_niunai_shield_until",xiaoshi2022.corpseorigin.skill.kaiweinai.ChrysanthemumShieldSkill.SHIELD_DURATION,1);
            case "osmium_gold": return guard(c,"clone_gold_until",200,3);
            case "blood_lotus_armor":
                c.setAttached(SkillRework.LOTUS_ARMOR,level.getGameTime()+200); return guard(c,"clone_lotus_until",200,1);
            case "xuanwu_body": return guard(c,"clone_xuanwu_until",200,2);
            case "muscle_rage":
                c.setAttached(CreatureAbilities.INFANT,false); c.setAttached(CreatureAbilities.RAGE_UNTIL,level.getGameTime()+160);
                buff(c,MobEffects.STRENGTH,160,2); buff(c,MobEffects.SPEED,160,1); return true;
            case "chameleon_disguise":
                if (c.hasEffect(MobEffects.INVISIBILITY)) return false;
                buff(c,MobEffects.INVISIBILITY,100,0); return true;
            case "spatial_blink": {
                if (close(c,t,4)) return false;
                Vec3 spot=t.position().subtract(c.getLookAngle().multiply(2,0,2));
                if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(spot))
                        || !level.noCollision(c,c.getBoundingBox().move(spot.subtract(c.position())))) return false;
                c.teleportTo(spot.x,spot.y,spot.z); return true;
            }
            case "bear_charge", "pounce_combo":
                if (!close(c,t,12)) return false;
                c.setDeltaMovement(c.getLookAngle().multiply(1,0,1).normalize().scale(.8).add(0,.35,0)); c.hurtMarked=true;
                schedule(c,t,path,5,path.equals("pounce_combo")?24:12,8); return true;
            case "bag_capture", "wood_bind":
                if (!close(c,t,path.equals("bag_capture")?5:12)) return false;
                SkillConstructEntity.spawn(c,ModEntities.VINE_BIND,t,path.equals("bag_capture")?60:160); return true;
            case "killing_incarnation":
                if (SkillConstructEntity.findOwned(c,"slaughter_incarnation") != null) return false;
                SkillConstructEntity.spawn(c,ModEntities.SLAUGHTER_INCARNATION,c,400); return true;
            case "severed_arm_strike", "blood_lotus": {
                var construct=SkillConstructEntity.spawn(c,path.equals("blood_lotus")?ModEntities.BLOOD_LOTUS_PETAL:ModEntities.SEVERED_FOREARM,null,60);
                construct.setDeltaMovement(c.getLookAngle().scale(1.6)); construct.hurtMarked=true; return true;
            }
            case "sword_flower", "round_dance", "blood_cloud", "killing_gas", "five_elements_formation", "thousand_eyes", "ancient_poetry_sword":
                schedule(c,t,path,10,path.equals("round_dance")?40:160,10); return true;
            case "dark_siphon", "life_drain_suck":
                if (!close(c,t,path.equals("dark_siphon")?3.5:6)) return false;
                schedule(c,t,path,0,60,10); return true;
            case "swallow_nest":
                if (!close(c,t,12)) return false;
                buff(t,MobEffects.GLOWING,200,0); state(c).swallowMarks.put(t.getUUID(),level.getGameTime()+200); return true;
            case "flame_strike":
                boolean marked=state(c).swallowMarks.getOrDefault(t.getUUID(),0L)>level.getGameTime();
                if (!hit(c,t,8,marked?12:6,0xff7a1a)) return false;
                if(marked)t.igniteForSeconds(6); return true;
            case "reverse_formation_fireball":
                if(c.isInWater())return false;
                schedule(c,t,"fireball",30,1,1); QiEffects.aura(c,"fireball_charge",0xff7a1a,2,30); return true;
            case "crimson_blood_spear":
                schedule(c,t,"blood_spear",24,1,1); QiEffects.aura(c,"blood_spear_charge",0xd30b30,2,24); return true;
            case "meteor_sword", "blood_wing_blade", "tiger_claw_bee_wheel", "parcel_bomb", "tian_gang_blood_lotus":
                return CloneWeaponArts.fire(c,c.getMainHandItem())>0;
            case "black_friday_eight":
                if(c.getMainHandItem().getCount()<8)return false;
                for(int i=0;i<8;i++)ChapterBombEntity.launch(c,ModItems.BILLIARD_EIGHT);
                c.getMainHandItem().shrink(8); return true;
            case "dog_eye_cannon", "hound_unleashed": {
                var dog=level.getEntitiesOfClass(HamEntity.class,c.getBoundingBox().inflate(6),
                        d->d.isAlive()&&!d.isPassenger()&&!d.isLeashed()&&c.hasLineOfSight(d)
                                &&(d.getOwnerReference()==null||Objects.equals(c.getOwnerUuid(),d.getOwnerReference().getUUID())))
                        .stream().min(Comparator.comparingDouble(c::distanceToSqr)).orElse(null);
                if(dog==null)return false;
                dog.getNavigation().stop(); dog.setOrderedToSit(false); dog.setInSittingPose(false); dog.setTarget(t);
                dog.setDeltaMovement(c.getLookAngle().scale(1.8).add(0,.35,0)); dog.hurtMarked=true; return true;
            }
            case "ham_summon": return xiaoshi2022.corpseorigin.item.DogCageItem.fireClone(c,c.getMainHandItem());
            case "osmium_ice_spike":
                if(!close(c,t,20))return false;
                level.addFreshEntity(OsmiumIceSpearEntity.create(level,c,t.position().add(0,t.getBbHeight()+8,0),new Vec3(0,-.65,0)));return true;
            case "corpse_fish_eggs":
                for(int i=0;i<3;i++)level.addFreshEntity(CorpseFishEggEntity.create(c,i));return true;
            case "bat_cloak":
                for(int i=0;i<5;i++){var bat=new VampireBatEntity(ModEntities.VAMPIRE_BAT,level);bat.setOwner(c);bat.setBiteSlot(i);
                    bat.setPos(c.getEyePosition().add(Math.cos(i*1.26),.3,Math.sin(i*1.26)));level.addFreshEntity(bat);}return true;
            case "summon_swarm": return summonAnts(c,t);
            case "tengu_divine_array": {
                var ship=level.getEntitiesOfClass(GreatTenguEntity.class,c.getBoundingBox().inflate(32),e->e.isOwnedBy(c)).stream().findFirst().orElse(null);
                if(ship==null){ship=new GreatTenguEntity(ModEntities.GREAT_TENGU,level);ship.setOwner(c);ship.setPos(c.position().add(0,5,0));level.addFreshEntity(ship);}
                ship.callLaser(t);return true;
            }
            case "qi_lock", "gourd_eyes":
                for(var enemy:level.getEntitiesOfClass(LivingEntity.class,c.getBoundingBox().inflate(20),e->ChapterCombat.canHit(c,e)&&c.hasLineOfSight(e)))buff(enemy,MobEffects.GLOWING,200,0);
                if(path.equals("gourd_eyes"))gourd(c,1);return true;
            case "gourd_power": gourd(c,6);buff(c,MobEffects.ABSORPTION,240,1);buff(c,MobEffects.RESISTANCE,240,1);return true;
            case "gourd_arms":
                if(!hit(c,t,5,26,0xe8cd4c))return false;gourd(c,2);t.push(c.getLookAngle().x*1.2,.35,c.getLookAngle().z*1.2);return true;
            case "gourd_acid", "gourd_fire":
                if(!close(c,t,16))return false;
                gourd(c,path.equals("gourd_fire")?5:4);schedule(c,t,path,10,path.equals("gourd_fire")?100:80,path.equals("gourd_fire")?10:20);return true;
            case "corpse_king_infrasound": return xiaoshi2022.corpseorigin.skill.longyou.InfrasoundFieldHandler.start(c);
            case "corpse_brother_rally":
                for(var ally:level.getEntitiesOfClass(CloneAvatarEntity.class,c.getBoundingBox().inflate(20),a->Objects.equals(a.getOwnerUuid(),c.getOwnerUuid()))){ally.setTarget(t);buff(ally,MobEffects.STRENGTH,200,1);}return true;
            case "thunder_power": buff(c,MobEffects.STRENGTH,200,1);return true;
            case "corpse_king_thunder", "ball_lightning", "natural_judgment":
                if(!close(c,t,24))return false;schedule(c,t,path,20,1,1);QiEffects.aura(t,"thunder_warning",0xa76dff,2,20);return true;
            case "tiangang_ji": buff(c,MobEffects.SPEED,600,3);return true;
            case "tiangang_li": buff(c,MobEffects.STRENGTH,600,3);return true;
            case "tiangang_yu": return guard(c,"clone_tiangang_yu",300,3);
            case "tiangang_shen":
                if (!guard(c,"clone_tiangang_shen",600,2)) return false;
                c.setAttached(TianGangCombat.SHEN_ACTIVE,true);buff(c,MobEffects.STRENGTH,600,2);buff(c,MobEffects.SPEED,600,2);
                return true;
            case "tiangang_wu", "tiangang_pogang":
                if(path.equals("tiangang_pogang")&&!c.getAttachedOrCreate(TianGangCombat.SHEN_ACTIVE))return false;
                if(!close(c,t,path.equals("tiangang_wu")?6:4))return false;
                TianGangCombat.breakGuard(t);return hit(c,t,6,path.equals("tiangang_wu")?36:48,0x88aaff);
            case "tiangang_nipo":
                if(!c.getAttachedOrCreate(TianGangCombat.SHEN_ACTIVE)||!close(c,t,4))return false;
                schedule(c,t,path,0,50,10);return true;
            case "tiangang_mie":
                if(!close(c,t,12))return false;c.setDeltaMovement(c.getLookAngle().scale(.9));c.hurtMarked=true;
                schedule(c,t,path,5,16,8);return true;
            case "tiangang_hui", "tiangang_tiangangpo":
                if(!c.onGround()||!level.noCollision(c,c.getBoundingBox().expandTowards(0,2,0)))return false;
                if(path.equals("tiangang_tiangangpo")&&!c.getAttachedOrCreate(TianGangCombat.SHEN_ACTIVE))return false;
                c.setDeltaMovement(0,1.25,0);c.hurtMarked=true;schedule(c,t,path,30,1,1);return true;
            default: return false;
        }
    }

    private static boolean summonAnts(CloneAvatarEntity c, LivingEntity target) {
        ServerLevel level=(ServerLevel)c.level();
        int existing=level.getEntitiesOfClass(CorpseAntEntity.class,c.getBoundingBox().inflate(40),a->a.ownedBy(c)).size();
        int added=0;
        for(int i=0;i<Math.min(6,8-existing);i++){
            var ant=(i%3==0?ModEntities.BULLET_ANT:ModEntities.RED_FIRE_ANT).create(level,EntitySpawnReason.TRIGGERED);
            if(ant==null)continue;
            ant.setOwner(c);ant.setTarget(target);ant.setPos(c.position().add(Math.cos(i*1.05)*2,0,Math.sin(i*1.05)*2));
            if(level.noCollision(ant)&&level.addFreshEntity(ant))added++;
        }
        return added>0;
    }
    private static void gourd(CloneAvatarEntity c,int form) {
        var body=c.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
        body.putInt("gourd_form",form);body.putLong("gourd_until",c.level().getGameTime()+120);
        c.setAttached(SurvivalGrowth.BODY,body);
    }
    private static void area(CloneAvatarEntity c,double radius,float damage,int color) {
        for(var t:c.level().getEntitiesOfClass(LivingEntity.class,c.getBoundingBox().inflate(radius),
                t->ChapterCombat.canHit(c,t)&&c.hasLineOfSight(t)&&c.distanceToSqr(t)<=radius*radius))
            t.hurtServer((ServerLevel)c.level(),c.damageSources().mobAttack(c),damage);
        QiEffects.cloud((ServerLevel)c.level(),c.position().add(0,.8,0),color,(float)radius,12);
    }

    public static void tick(CloneAvatarEntity c) {
        if(c.tickCount%20==0){
            var body=c.getAttachedOrCreate(SurvivalGrowth.BODY).copy();
            body.putInt("clone_qi",Math.min(maxInner(c),body.getIntOr("clone_qi",maxInner(c))+2));
            body.putInt("clone_blood",Math.min(100,body.getIntOr("clone_blood",100)+1));
            c.setAttached(SurvivalGrowth.BODY,body);
            if("jingang_zb".equals(c.getBodyRole()))buff(c,MobEffects.RESISTANCE,40,1);
            if("heixiaofei".equals(c.getBodyRole()))c.heal(2);
        }
        State state=state(c);
        long now=c.level().getGameTime();
        if(c.getAttachedOrCreate(TianGangCombat.SHEN_ACTIVE)
                && (!c.getBodyRole().equals("longyou") || c.getAttachedOrCreate(SurvivalGrowth.BODY).getLongOr("clone_tiangang_shen",0)<=now))
            c.setAttached(TianGangCombat.SHEN_ACTIVE,false);
        state.swallowMarks.values().removeIf(until -> until <= now);
        var iterator=state.pulses.iterator();
        while(iterator.hasNext()){
            Pulse pulse=iterator.next();
            if(now>=pulse.until){iterator.remove();continue;}
            if(now<pulse.next)continue;
            pulse.next=now+pulse.period;
            LivingEntity t=pulse.target!=null&&((ServerLevel)c.level()).getEntity(pulse.target) instanceof LivingEntity living?living:c.getTarget();
            if(pulse.kind.equals("shen_end")){c.setAttached(TianGangCombat.SHEN_ACTIVE,false);iterator.remove();continue;}
            if(t==null||!ChapterCombat.canHit(c,t)||!c.hasLineOfSight(t))continue;
            CloneWeaponArts.face(c,t);
            switch(pulse.kind){
                case "key":
                    if(!c.getMainHandItem().is(ModItems.TIAN_GANG_KEY)){iterator.remove();break;}
                    hit(c,t,50,30,0xee2266);break;
                case "dark_siphon", "life_drain_suck": {
                    if(pulse.kind.equals("dark_siphon")&&!c.getMainHandItem().is(ModItems.BLOOD_WING_BLADE)){iterator.remove();break;}
                    float before=t.getHealth();
                    if(hit(c,t,pulse.kind.equals("dark_siphon")?3.5:6,pulse.kind.equals("dark_siphon")?16:4,0xa80d27))c.heal(Math.min(10,Math.max(0,before-t.getHealth())*.6F));
                    break;
                }
                case "bear_charge", "pounce_combo": hit(c,t,4,pulse.kind.equals("bear_charge")?12:8,0xb66d35);break;
                case "sword_flower": area(c,6,6.3F,0xff76b3);break;
                case "round_dance": area(c,6,7,0xdf203c);break;
                case "blood_cloud": area(c,4,4.9F,0xb51236);buff(c,MobEffects.STRENGTH,30,2);break;
                case "five_elements_formation": area(c,6,4,0x57b84f);buff(t,MobEffects.SLOWNESS,25,1);break;
                case "killing_gas": area(c,5,3,0x73b835);if(close(c,t,5))buff(t,MobEffects.POISON,60,1);break;
                case "thousand_eyes": hit(c,t,20,6,0xc0182a);break;
                case "ancient_poetry_sword": hit(c,t,12,12,0x8a3fd6);break;
                case "blood_spear": hit(c,t,32,40,0xd30b30);break;
                case "fireball": if(hit(c,t,24,24,0xff7a1a))t.igniteForSeconds(6);break;
                case "gourd_acid": if(hit(c,t,16,7,0x4fc3f7)){buff(t,MobEffects.POISON,160,1);buff(t,MobEffects.SLOWNESS,80,1);}break;
                case "gourd_fire": if(hit(c,t,16,5,0xff7a1a))t.igniteForSeconds(8);break;
                case "corpse_king_thunder", "ball_lightning", "natural_judgment":
                    if(hit(c,t,24,pulse.kind.equals("natural_judgment")?60:32,0xa76dff))buff(t,MobEffects.SLOWNESS,80,2);break;
                case "tiangang_nipo": hit(c,t,4,24,0x88aaff);break;
                case "tiangang_mie": hit(c,t,4,40,0x88aaff);break;
                case "tiangang_hui", "tiangang_tiangangpo": area(c,pulse.kind.equals("tiangang_hui")?6:9,40,0x88aaff);break;
                default: break;
            }
        }
    }
}
