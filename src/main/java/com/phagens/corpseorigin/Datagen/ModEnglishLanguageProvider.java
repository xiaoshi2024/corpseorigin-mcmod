package com.phagens.corpseorigin.Datagen;

import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModEnglishLanguageProvider extends LanguageProvider {

    public ModEnglishLanguageProvider(PackOutput output) {
        super(output, CorpseOrigin.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        addItemTranslations();
        addBlockTranslations();
        addEntityTranslations();
        addEffectTranslations();
        addGongFaTranslations();
        addSkillTranslations();
        addTooltipTranslations();
        addDeathMessageTranslations();
        addKeyBindingTranslations();
        addGuiTranslations();
        addCommandTranslations();
        addConfigurationTranslations();
        addSubtitleTranslations();
        addOtherTranslations();
        addGongFaState();
        addDialogueTranslations();
        addAdvancementTranslations();
    }

    private void addItemTranslations() {
        // Base Items
        add("item.corpseorigin.base_gong_fa", "Cultivation Method");
        add("item.corpseorigin.gf_cy_ren", "Mortal Rank Fragment");
        add("item.corpseorigin.gf_cy_di", "Earth Rank Fragment");
        add("item.corpseorigin.gf_cy_tian", "Heaven Rank Fragment");
        add("item.corpseorigin.gf_cy_shen", "Divine Rank Fragment");
        add("item.corpseorigin.gf_cy_chaoshen", "Transcendent Rank Fragment");

        // Weapons
        add("item.corpseorigin.ming_juque", "Juque Sword");
        add("item.corpseorigin.ming_juque_tw", "Juque Sword II");

        // Potions and Containers
        add("item.corpseorigin.bywater_bucket", "Corpse Water Bucket");
        add("item.corpseorigin.bywater_bottle", "Corpse Water Bottle");

        // Agents
        add("item.corpseorigin.s_agent", "Yellow Strengthening Agent");
        add("item.corpseorigin.blue_s_agent", "Blue Neutralizing Agent");
        add("item.corpseorigin.null_s_agent", "Empty Agent");

        // Special Items
        add("item.corpseorigin.hair_dryer", "Hair Dryer");
        add("item.corpseorigin.mission_scroll", "Mission Scroll");

        // Organ Drops
        add("item.corpseorigin.ordinary_zb_eye", "Ordinary Corpse Eye");
        add("item.corpseorigin.dr_mu_eye", "Dr. Mu's Eye");
        add("item.corpseorigin.zb_worm_item", "Corpse Worm");

        // Dr. Mu's Eye related
        add("item.corpseorigin.dr_mu_eye.effect1", "§7Consumption Effect:");
        add("item.corpseorigin.dr_mu_eye.effect2", "  §a• Unconscious corpses regain human intelligence");
        add("item.corpseorigin.dr_mu_eye.effect3", "  §e• Conscious corpses gain enhanced intelligence");

        // Ordinary Corpse Eye related
        add("item.corpseorigin.ordinary_zb_eye.effect1", "§7Consumption Effect:");
        add("item.corpseorigin.ordinary_zb_eye.effect2", "  §a• %d%% chance to gain permanent night vision (need %d consumed)");
        add("item.corpseorigin.ordinary_zb_eye.effect3", "  §a• %d%% chance to grow extra eyes (need %d consumed)");
        add("item.corpseorigin.ordinary_zb_eye.evolution_success", "§a§lYou feel the power of evolution!");
        add("item.corpseorigin.ordinary_zb_eye.consumed", "§7Consumed Corpse Eyes: %d/%d");
        add("item.corpseorigin.ordinary_zb_eye.night_vision_unlocked", "§6You have evolved permanent night vision!");
        add("item.corpseorigin.ordinary_zb_eye.extra_eye_grown", "§cYou grew your %dth extra eye!");
        add("item.corpseorigin.ordinary_zb_eye.multi_eye_evolved", "§c§lYou evolved Multi-Eye Form! All 9 eyes have awakened!");

        // Corpse Worm related
        add("item.corpseorigin.zb_worm.description", "A writhing corpse worm, emitting an eerie aura");
        add("item.corpseorigin.zb_worm.effect1", "  §a• %d%% chance to gain speed boost");
        add("item.corpseorigin.zb_worm.effect2", "  §a• %d%% chance to gain mining efficiency boost");
        add("item.corpseorigin.zb_worm.corpse_bonus", "§aCorpses gain extra effects when consumed");
        add("item.corpseorigin.zb_worm.not_corpse_warning", "§cYou are not a corpse, eating this makes you feel uncomfortable");
        add("item.corpseorigin.zb_worm.consumed", "§7You consumed a Corpse Worm");
        add("item.corpseorigin.zb_worm.effect_gained", "§aYou gained a special effect!");

        // Mission Scroll related
        add("item.corpseorigin.mission_scroll.type", "Mission Type: %s");
        add("item.corpseorigin.mission_scroll.progress", "Progress: %d / %d");
        add("item.corpseorigin.mission_scroll.completed", "§a§lComplete! Return to the Corpse King to submit");
        add("item.corpseorigin.mission_scroll.incomplete", "§eMission incomplete");
        add("item.corpseorigin.mission_scroll.reward_points", "Reward: %d Evolution Points");
        add("item.corpseorigin.mission_scroll.reward_levels", "Reward: +1 Evolution Level");
        add("item.corpseorigin.mission_scroll.can_submit", "§aMission complete, right-click the Corpse King with the scroll to submit!");
        add("item.corpseorigin.mission_scroll.remaining", "§e%d more targets to eliminate");

        // Awakening Stones
        add("item.corpseorigin.yns_c", "Awakening Stone - Common");
        add("item.corpseorigin.yns_b", "Awakening Stone - Uncommon");
        add("item.corpseorigin.yns_a", "Awakening Stone - Rare");
        add("item.corpseorigin.yns_s", "Awakening Stone - Epic");
        add("item.corpseorigin.yns_ss", "Awakening Stone - Legendary");
        add("item.corpseorigin.yns_sss", "Awakening Stone - Master");

        // Cultivation Method related
        add("item.corpseorigin.lei_xi_gong_fa", "Lightning Cultivation Method");

        // Block Items
        add("item.corpseorigin.qi_xings_guan_item", "Seven Star Coffin");
        add("item.corpseorigin.zbr_flesh_item", "Corpse Flesh Block");
        add("item.corpseorigin.alienated_fragment_item", "Alienated Fragment");
        add("item.corpseorigin.technique_swap_table", "Cultivation Workshop");

        // Spawn Eggs
        add("item.corpseorigin.lower_level_zb_spawn_egg", "Corpse Spawn Egg");
        add("item.corpseorigin.longyou_spawn_egg", "Long You Spawn Egg");
        add("item.corpseorigin.zbr_fish_spawn_egg", "Corpse Fish Spawn Egg");
        add("item.corpseorigin.kaiweinai_spawn_egg", "Kai Wei Nai Spawn Egg");
        add("item.corpseorigin.coco_penguin_spawn_egg", "CoCo Penguin Spawn Egg");
        add("item.corpseorigin.coco_zombie_spawn_egg", "CoCo Corpse Spawn Egg");
        add("item.corpseorigin.zb_worm_spawn_egg", "Corpse Worm Spawn Egg");
        add("item.corpseorigin.uncle_spawn_egg", "Uncle (Shoujo Mangaka) Spawn Egg");
        add("item.corpseorigin.coco_zombie_x_spawn_egg", "CoCo Corpse-%s Spawn Egg");
        add("item.corpseorigin.guigun_spawn_egg", "Gui Gun Spawn Egg");
        add("item.corpseorigin.centipede_spawn_egg", "Centipede Corpse Spawn Egg");
    }

    private void addBlockTranslations() {
        add("block.corpseorigin.qi_xing_guan", "Seven Star Coffin");
        add("block.corpseorigin.zbr_flesh", "Corpse Flesh Block");
        add("block.corpseorigin.alienated_fragment", "Alienated Fragment");
        add("block.corpseorigin.technique_swap_table", "Cultivation Exchange Table");
    }

    private void addEntityTranslations() {
        add("entity.corpseorigin.lower_level_zb", "Corpse");
        add("entity.corpseorigin.lower_level_zb.named", "Corpse-%s");
        add("entity.corpseorigin.lower_level_zb.default", "Corpse");
        add("entity.corpseorigin.longyou", "Long You");
        add("entity.corpseorigin.zbr_fish", "Corpse Fish");
        add("entity.corpseorigin.kaiweinai", "Kai Wei Nai");
        add("entity.corpseorigin.coco_penguin", "CoCo");
        add("entity.corpseorigin.coco_zombie", "CoCo Corpse");
        add("entity.corpseorigin.zb_worm", "Corpse Worm");
        add("entity.corpseorigin.uncle", "Uncle (Shoujo Mangaka)");
        add("entity.corpseorigin.coco_zombie_x", "CoCo Corpse-%s");
        add("entity.corpseorigin.guigun", "Gui Gun");
        add("entity.corpseorigin.centipede_head", "Centipede Corpse");

        add("entity.corpseorigin.corpse", "Corpse");
        add("entity.corpseorigin.corpse_gib", "Corpse Gib");
    }

    private void addEffectTranslations() {
        add("effect.corpseorigin.by", "Corpse Water Infection");
        add("effect.corpseorigin.side_effect", "Agent Side Effect");
    }

    private void addGongFaTranslations() {
        // Rarity
        add("gongfa.rarity.1", "Mortal Rank");
        add("gongfa.rarity.2", "Earth Rank");
        add("gongfa.rarity.3", "Heaven Rank");
        add("gongfa.rarity.4", "Divine Rank");
        add("gongfa.rarity.5", "Transcendent Rank");

        // Layers
        add("gongfa.ceng.copy_1", "First Layer");
        add("gongfa.ceng.copy_2", "Second Layer");
        add("gongfa.ceng.copy_3", "Third Layer");
        add("gongfa.ceng.copy_4", "Fourth Layer");
        add("gongfa.ceng.copy_5", "Fifth Layer");
        add("gongfa.ceng.copy_6", "Sixth Layer");
        add("gongfa.ceng.copy_7", "Seventh Layer");
        add("gongfa.ceng.copy_8", "Eighth Layer");
        add("gongfa.ceng.copy_9", "Ninth Layer");

        // Attributes
        add("gongfa.attribute.attack_damage", "Attack Damage");
        add("gongfa.attribute.movement_speed", "Movement Speed");
        add("gongfa.attribute.max_health", "Max Health");
        add("gongfa.attribute.armor", "Armor");
        add("gongfa.attribute.knockback_resistance", "Knockback Resistance");

        // Skill Names
        add("gongfa.skill.lightning_basic", "Basic Lightning Strike");
        add("gongfa.skill.lightning_strike", "Lightning Strike");
        add("gongfa.skill.lightning_dash", "Lightning Dash");
        add("gongfa.skill.heavenly_thunder", "Heavenly Thunder");
        add("gongfa.skill.thousand_thunder", "Thousand Thunder Return");
        add("gongfa.skill.thunder_god", "Thunder God Possession");
    }

    private void addSkillTranslations() {
        // Evolution Skills
        add("skill.corpseorigin.hardened_skin", "Hardened Skin");
        add("skill.corpseorigin.hardened_skin.desc", "Hardens your skin, increasing armor");
        add("skill.corpseorigin.sharp_claws", "Sharp Claws");
        add("skill.corpseorigin.sharp_claws.desc", "Grow sharp claws, increasing attack damage");
        add("skill.corpseorigin.devour_enhancement", "Devour Enhancement");
        add("skill.corpseorigin.devour_enhancement.desc", "Restore more health and hunger when devouring");
        add("skill.corpseorigin.evolution_sense", "Evolution Sense");
        add("skill.corpseorigin.evolution_sense.desc", "Gain night vision ability");
        add("skill.corpseorigin.giant_strength", "Giant Strength");
        add("skill.corpseorigin.giant_strength.desc", "Greatly increase attack damage");
        add("skill.corpseorigin.berserk", "Berserk");
        add("skill.corpseorigin.berserk.desc", "Temporarily increase attack and movement speed");
        add("skill.corpseorigin.heavy_strike", "Heavy Strike");
        add("skill.corpseorigin.heavy_strike.desc", "Attacks have knockback effect");
        add("skill.corpseorigin.swift_movement", "Swift Movement");
        add("skill.corpseorigin.swift_movement.desc", "Increase movement speed");
        add("skill.corpseorigin.leap", "Leap Enhancement");
        add("skill.corpseorigin.leap.desc", "Increase jump height");
        add("skill.corpseorigin.evasion", "Evasion");
        add("skill.corpseorigin.evasion.desc", "Chance to dodge attacks");
        add("skill.corpseorigin.venom", "Venom");
        add("skill.corpseorigin.venom.desc", "Attacks poison the target");
        add("skill.corpseorigin.regeneration", "Rapid Regeneration");
        add("skill.corpseorigin.regeneration.desc", "Increase health regeneration speed");
        add("skill.corpseorigin.fear_aura", "Fear Aura");
        add("skill.corpseorigin.fear_aura.desc", "Weaken and slow nearby enemies");
        add("skill.corpseorigin.immortal_body", "Immortal Body");
        add("skill.corpseorigin.immortal_body.desc", "Resurrect once upon death");
        add("skill.corpseorigin.corpse_king_power", "Corpse King Power");
        add("skill.corpseorigin.corpse_king_power.desc", "Simulates Long You's infection ability, can infect other players to become corpses, and briefly control infected players");
        add("skill.corpseorigin.shadow_strike", "Shadow Strike");
        add("skill.corpseorigin.shadow_strike.desc", "Enter invisibility and greatly increase speed");

        // Multi-Eye Skills
        add("skill.corpseorigin.multi_eye_perception", "Multi-Eye Perception");
        add("skill.corpseorigin.multi_eye_perception.desc", "Trigger the power of multiple eyes, sensing hostile creatures within 32 blocks, highlighting enemies within 24 blocks");
        add("skill.corpseorigin.multi_eye.activated", "§c§lMulti-Eye Perception Activated! You sense hostility all around!");
        add("skill.corpseorigin.multi_eye.ended", "§7Multi-Eye Perception effect has ended");
        add("skill.corpseorigin.multi_eye.cooldown", "§cSkill on cooldown, %d seconds remaining");
        add("skill.corpseorigin.multi_eye.detected", "§eSensing %d hostile targets");
        add("skill.corpseorigin.multi_eye.auto_learned", "§c§lYour multiple eyes have awakened! Gained Multi-Eye Perception!");
        add("skill.corpseorigin.disguise", "Disguise");
        add("skill.corpseorigin.disguise.desc", "I look like a human?");

        // Skill Tree
        add("skilltree.corpseorigin.corpse_evolution", "Corpse Evolution");
        add("skilltree.corpseorigin.corpse_evolution.desc", "Evolution path of the corpse, gain power through devouring");
        add("skilltree.corpseorigin.corpse_evolution.condition", "Automatically unlocked after becoming a corpse");

        add("skilltype.corpseorigin.basic_evolution", "Basic Evolution");
        add("skilltype.corpseorigin.power_mutation", "Power Mutation");
        add("skilltype.corpseorigin.agility_mutation", "Agility Mutation");
        add("skilltype.corpseorigin.special_mutation", "Special Mutation");
        add("skilltype.corpseorigin.divine_ability", "Divine Ability");
        add("skilltype.corpseorigin.supreme_ability", "Supreme Ability");

        // Skill Prompts
        add("skill.corpseorigin.no_activatable", "No Activatable Skills");
        add("skill.corpseorigin.cooldown", "Skill on cooldown, %d seconds remaining");
        add("skill.corpseorigin.cooldown_remaining", "Cooldown: %d seconds");
        add("skill.corpseorigin.voice_activated", "Voice activated skill: %s (confidence: %s)");
        add("skill.corpseorigin.voice_cooldown", "%s is on cooldown");
    }

    private void addTooltipTranslations() {
        add("tooltip.corpseorigin.bywater_bottle", "Drinking it will poison you...");
        add("tooltip.corpseorigin.bywater_bucket", "Placing it will contaminate water sources");
        add("tooltip.corpseorigin.ming_juque", "A divine sword that can execute weakened enemies");
        add("tooltip.corpseorigin.s_agent", "§eAn unstable experimental agent in its early stage, direct use will burden the body");
        add("tooltip.corpseorigin.s_agent.warning", "§c§lWarning: Must be used with Blue Neutralizing Agent, or you may die!");
        add("tooltip.corpseorigin.blue_s_agent", "§bUsed to neutralize the side effects of the Yellow Strengthening Agent and stabilize the body");
        add("tooltip.corpseorigin.null_s_agent", "Empty Agent, used for subsequent black fire instruments");
        add("tooltip.corpseorigin.gongfa.rarity", "Rarity: %s");
        add("tooltip.corpseorigin.gongfa.ceng", "Cultivation Layer: %s");
        add("tooltip.corpseorigin.gongfa.attribute", "Blessings:");
        add("tooltip.corpseorigin.gongfa.skills", "Skills:");

        // ===== Add organ-related tooltips =====
        add("item.corpseorigin.organ.type", "Organ Type: %s");
        add("item.corpseorigin.organ.evolution_chance", "Evolution Chance: %d%%");
        add("item.corpseorigin.organ.required_amount", "Required Amount: %d");
        add("item.corpseorigin.organ.corpse_only", "§cOnly corpses can digest this");
        add("item.corpseorigin.organ.not_corpse_warning", "§cYou are not a corpse, eating organs will poison you!");
    }

    private void addDeathMessageTranslations() {
        add("death.attack.corpse_water", "%1$s drowned in corpse water");
        add("death.attack.zombie_brother", "%1$s was killed by a Corpse");
        add("death.attack.side_effect", "%1$s died from agent side effects");
    }

    private void addKeyBindingTranslations() {
        add("key.categories.corpseorigin", "Corpse Origin Mod");
        add("key.corpseorigin.category", "Corpse Origin Mod");
        add("key.corpseorigin.open_gongfu", "Open Cultivation Interface");
        add("key.corpseorigin.take_out_juque", "Take out Juque Sword");
        add("key.corpseorigin.skill_release", "Skill Release");
        add("key.corpseorigin.skill_wheel", "Skill Wheel");
        add("key.corpseorigin.skill_tree", "Skill Tree");

        add("key.corpseorigin.voice_listen", "Voice Listen");
    }

    private void addGuiTranslations() {
        add("container.corpseorigin.gong_fu", "Cultivation");
        add("gui.corpseorigin.required_level", "Required Level: %d (Current: %d)");
        add("gui.corpseorigin.skill_need_points", "§cInsufficient evolution points");
        add("gui.corpseorigin.skill_need_level", "§cInsufficient Corpse level");
        add("gui.corpseorigin.skill_need_prereq", "§cPrerequisite skill not learned");
        add("gui.corpseorigin.skill_already_learned", "§aLearned");
        add("gui.corpseorigin.skill_error", "§cFailed to unlock");
        add("gui.corpseorigin.skill_unknown", "§cUnknown reason");

        add("gui.corpseorigin.unlock", "Unlock Skill");
        add("gui.corpseorigin.cost", "Cost: %d Evolution Points");
        add("gui.corpseorigin.type", "Type: %s");
        add("gui.corpseorigin.unlocked", "§aUnlocked");
        add("gui.corpseorigin.can_unlock", "§eAvailable");
        add("gui.corpseorigin.locked", "§cLocked");
        add("gui.corpseorigin.prerequisites", "Prerequisites:");
        add("gui.corpseorigin.evolution_points", "Evolution Points: %d");
        add("gui.corpseorigin.evolution_level", "Evolution Level: %d");
        add("gui.corpseorigin.convert_experience", "Convert Experience");
    }

    private void addCommandTranslations() {
        add("command.corpseorigin.voice.enabled", "Voice trigger enabled");
        add("command.corpseorigin.voice.disabled", "Voice trigger disabled");
        add("command.corpseorigin.voice.threshold_set", "Confidence threshold set to %s");
        add("command.corpseorigin.voice.testing", "Testing voice input: %s");
        add("command.corpseorigin.voice.list_header", "Skill Voice Bindings:");
        add("command.corpseorigin.voice.binding_added", "Added voice binding for %s: %s");
        add("command.corpseorigin.voice.invalid_skill_id", "Invalid skill ID");
        add("command.corpseorigin.voice.skill_not_found", "Skill not found");
        add("command.corpseorigin.voice.status", "Voice Trigger Status:");
        add("command.corpseorigin.voice.status_enabled", "  Enabled: %s");
        add("command.corpseorigin.voice.status_threshold", "  Confidence threshold: %s");
        add("command.corpseorigin.voice.status_bindings", "  Binding count: %d");
        add("command.corpseorigin.voice.check_available", "§aVoice system available");
        add("command.corpseorigin.voice.check_unavailable", "§cVoice system unavailable, please check microphone settings");
    }

    private void addConfigurationTranslations() {
        add("corpseorigin.configuration.title", "Corpse Origin Configs");
        add("corpseorigin.configuration.section.corpseorigin.common.toml", "Corpse Origin Common Config");
        add("corpseorigin.configuration.section.corpseorigin.common.toml.title", "Corpse Origin Common Config");
        add("corpseorigin.configuration.items", "Item List");
        add("corpseorigin.configuration.logDirtBlock", "Log Dirt Block");
        add("corpseorigin.configuration.magicNumberIntroduction", "Magic Number Text");
        add("corpseorigin.configuration.magicNumber", "Magic Number");
        add("itemGroup.corpseorigin.gongfa", "Cultivation System");

        add("corpseorigin.configuration", "Corpse Origin Configuration");
        add("corpseorigin.configuration.general", "General Settings");
        add("corpseorigin.configuration.voice", "Voice Recognition Settings");

        add("corpseorigin.config.enableVoiceTrigger", "Enable Voice Trigger");
        add("corpseorigin.config.enableVoiceTrigger.tooltip", "Enable voice trigger system for skills");
        add("corpseorigin.config.showSkillIcon", "Show Skill Icon");
        add("corpseorigin.config.showSkillIcon.tooltip", "Show selected skill icon with cooldown indicator on HUD");
        add("corpseorigin.config.similarityThreshold", "Similarity Threshold");
        add("corpseorigin.config.similarityThreshold.tooltip", "Voice recognition similarity threshold (0.0-1.0)");
        add("corpseorigin.config.voiceLanguage", "Voice Language");
        add("corpseorigin.config.voiceLanguage.tooltip", "Select voice recognition language");
        add("corpseorigin.config.voiceLanguage.zh-CN", "Chinese (Simplified)");
        add("corpseorigin.config.voiceLanguage.en-US", "English (US)");
        add("corpseorigin.config.voiceLanguage.ja-JP", "Japanese");
    }

    private void addSubtitleTranslations() {
        add("subtitles.corpseorigin.ground_chi", "Earth-rank corpse: Eat~~");
    }

    private void addOtherTranslations() {
        add("itemGroup.corpseorigin", "Corpse Origin Mod");
        add("message.corpseorigin.cultivation_opened", "Cultivation interface opened");
        add("message.corpseorigin.water_infected", "This water is contaminated with corpse water!");

        add("message.corpseorigin.ordinary_player", "§cYou are just an ordinary person");
        add("message.corpseorigin.skill_tree_coming_soon", "Skill tree interface coming soon");
        add("message.corpseorigin.evolution_points_gained", "§aDevoured %2$s gained %1$d evolution points!");
        add("message.corpseorigin.voice_recording_start", "§eStarting voice recording... (Hold V key to speak)");
        add("message.corpseorigin.voice_too_short", "§cRecording too short or no sound detected");
        add("message.corpseorigin.voice_not_recognized", "§cCould not recognize voice command");
        add("message.corpseorigin.voice_listening", "§eListening for voice...");
        add("message.corpseorigin.voice_listening_stopped", "§7Voice listening stopped");
        add("message.corpseorigin.voice_skill_activated", "§aVoice activated skill: %s");
        add("message.corpseorigin.voice_skill_not_unlocked", "§cSkill not unlocked or unavailable");
        add("message.corpseorigin.voice_skill_failed", "§cSkill activation failed");
        add("message.corpseorigin.voice_record_available_skills", "§e=== Available Skills to Record ===");
        add("message.corpseorigin.voice_record_usage", "§eUsage: /corpsevoice record <skillID>");
        add("message.corpseorigin.voice_record_example", "§7Example: /corpsevoice record berserk");
        add("message.corpseorigin.voice_record_invalid_skill", "§cInvalid skill ID: %s");
        add("message.corpseorigin.voice_record_check_list", "§7Please use /corpsevoice record to see available skills");
        add("message.corpseorigin.voice_record_start", "§eStart recording voice template for §f%s §e, hold V key to speak...");
        add("message.corpseorigin.voice_record_client_only", "§cVoice recording can only be executed on client");
        add("message.corpseorigin.voice_templates_loaded", "§eLoaded %d voice templates");
        add("message.corpseorigin.voice_templates_empty", "§7Use /corpsevoice record <skillName> to record templates");
        add("message.corpseorigin.voice_templates_client_only", "§cVoice templates can only be viewed on client");
        add("message.corpseorigin.skill_not_found", "§cSkill does not exist");
        add("message.corpseorigin.skill_already_learned", "§cYou have already learned this skill");
        add("message.corpseorigin.not_enough_points", "§cInsufficient evolution points");
        add("message.corpseorigin.missing_prerequisite", "§cMissing prerequisite skill");
        add("message.corpseorigin.skill_learned", "§aSuccessfully learned skill: %s");
        add("message.corpseorigin.evolution_point_gained", "§aYou gained 1 evolution point!");
        add("message.corpseorigin.experience_converted", "§aYour 5 levels of experience have been converted to 1 evolution point!");
        add("message.corpseorigin.evolution_level_up", "§aYour evolution level has increased to level %s!");
        add("message.corpseorigin.mission_completed", "§a§lMission complete! Return to the Corpse King to submit!");
        add("message.corpseorigin.mission_progress", "§eMission progress: %d more targets to complete");
        add("message.corpseorigin.mission_progress_kill", "§eMission progress: %d more kills needed");
        add("message.corpseorigin.mission_progress_collect", "§eMission progress: %d more items to collect");
        add("message.corpseorigin.mission_progress_infect", "§eMission progress: %d more players to infect");
        add("message.corpseorigin.corpse_infected", "§c§lCorpse is being infected by corpse water...");
        add("message.corpseorigin.corpse_transformed", "§4§lCorpse has transformed into a Corpse!");
    }

    private void addDialogueTranslations() {
        // Mission Types
        add("mission.corpseorigin.kill_villager", "Slaughter Villagers");
        add("mission.corpseorigin.kill_zombie", "Eliminate Zombies/Corpses");
        add("mission.corpseorigin.kill_any", "Kill Creatures");
        add("mission.corpseorigin.infect_player", "Infect Players");
        add("mission.corpseorigin.collect_item", "Collect Items");
        add("mission.corpseorigin.collect_block", "Collect Blocks");

        // Long You Dialogue
        add("dialogue.longyou.greeting", "[Corpse King·Long You] You have arrived, my subject.");
        add("dialogue.longyou.option.command", "What are your orders, my Lord?");
        add("dialogue.longyou.option.task", "Mission & Upgrade System");
        add("dialogue.longyou.option.appointment", "King's Appointment");
        add("dialogue.longyou.option.about", "About the Corpse Race");
        add("dialogue.longyou.command.line1", "§6§l[Corpse King·Long You] §rMy subject, go and gather more life force for me, expand the power of our corpse race.");
        add("dialogue.longyou.command.line2", "§6§l[Corpse King·Long You] §rEliminate those humans who resist us, convert them into our kind.");
        add("dialogue.longyou.command.line3", "§6§l[Corpse King·Long You] §rWhen you become strong enough, I will grant you even greater power.");
        add("dialogue.longyou.task.line1", "§6§l[Corpse King·Long You] §rI have opened the mission system for you. Completing missions will reward evolution points and special rewards.");
        add("dialogue.longyou.task.line2", "§6§l[Corpse King·Long You] §rMissions include: eliminating humans, infecting villagers, collecting resources, and more.");
        add("dialogue.longyou.task.line3", "§6§l[Corpse King·Long You] §rThe more missions you complete, the higher your level, and the richer the rewards.");
        add("dialogue.longyou.appointment.line1", "§6§l[Corpse King·Long You] §rAs my subject, you have shown extraordinary potential.");
        add("dialogue.longyou.appointment.line2", "§6§l[Corpse King·Long You] §rI appoint you as an elite warrior of the corpse race, granting you special abilities.");
        add("dialogue.longyou.appointment.line3", "§6§l[Corpse King·Long You] §rUse this power well, fight for the rise of the corpse race!");
        add("dialogue.longyou.about.line1", "§6§l[Corpse King·Long You] §rOur corpse race is the new ruler of this world, destined to replace fragile humans.");
        add("dialogue.longyou.about.line2", "§6§l[Corpse King·Long You] §rThrough continuous evolution, we will become stronger and invincible.");
        add("dialogue.longyou.about.line3", "§6§l[Corpse King·Long You] §rThe future of the corpse race lies in the hands of you, my subjects.");
        add("dialogue.longyou.invalid_option", "§4§lInvalid option!");
        add("dialogue.longyou.only_corpse", "§4§lOnly corpses can speak with the Corpse King!");
        add("dialogue.longyou.rebellion", "§4§l[Corpse King·Long You] §rYou dare to strike me! This is treason!");
        add("dialogue.longyou.rebellion.broadcast", "§4§l[Corpse King·Long You] §r%s dares to strike me! This is treason!");
        add("dialogue.longyou.option.receive_mission", "Receive Mission");
        add("dialogue.longyou.option.submit_mission", "Submit Mission");
        add("dialogue.longyou.mission.already_has", "§c§l[Corpse King·Long You] §rYou already have an incomplete mission! Go finish it first!");
        add("dialogue.longyou.mission.received", "§a§l[Corpse King·Long You] §rHere is your mission scroll. Bring it to me when complete. Mission: %s");
        add("dialogue.longyou.mission.kill_villager", "§6§l[Corpse King·Long You] §rGo slaughter %d villagers, show them the power of the corpse race!");
        add("dialogue.longyou.mission.kill_zombie", "§6§l[Corpse King·Long You] §rGo eliminate %d zombies or corpses, purge the inferior kind!");
        add("dialogue.longyou.mission.kill_any", "§6§l[Corpse King·Long You] §rGo kill %d creatures, demonstrate your strength!");
        add("dialogue.longyou.mission.infect_player", "§6§l[Corpse King·Long You] §rGo infect %d players! Make them part of our corpse race! This is the most valuable mission!");
        add("dialogue.longyou.mission.collect_item", "§6§l[Corpse King·Long You] §rGo collect %d items! I am interested in the novel things of this world!");
        add("dialogue.longyou.mission.completed", "§a§l[Corpse King·Long You] §rWell done! You have completed the mission, here is your reward!");
        add("dialogue.longyou.mission.reward_points", "§aYou gained %d evolution points!");
        add("dialogue.longyou.mission.reward_level", "§aYour evolution level has increased to level %d!");
        add("dialogue.longyou.mission.no_completed", "§c§l[Corpse King·Long You] §rYou have no completed mission! Finish a mission first before coming to me!");
    }

    private void addAdvancementTranslations() {
        add("advancements.corpseorigin.root.title", "Corpse Origin");
        add("advancements.corpseorigin.root.description", "Welcome to the world of corpses");
        add("advancements.corpseorigin.become_corpse.title", "Corpse Arrival");
        add("advancements.corpseorigin.become_corpse.description", "You have been infected by corpse water and become one of the corpse race");
        add("advancements.corpseorigin.meet_corpse_king.title", "First Meeting with the Corpse King");
        add("advancements.corpseorigin.meet_corpse_king.description", "You have encountered the legendary Corpse King Long You");
        add("advancements.corpseorigin.weapon_shattered.title", "A Rat Has Skin");
        add("advancements.corpseorigin.weapon_shattered.description", "Attacked the Corpse King from a distance, your weapon was shattered by his immense power");
        add("advancements.corpseorigin.cannibalism_discovery.title", "Devouring Instinct");
        add("advancements.corpseorigin.cannibalism_discovery.description", "You witnessed the cruel law of the jungle among corpses");
    }

    private void addGongFaState() {
        add("gongfa.category.gf", "Cultivation Method");
        add("gongfa.category.yn", "Supernatural Ability");
        add("gongfa.category.xm", "Bloodline");
        add("gongfa.category.fb", "Magic Treasure");
        add("gongfa.category.st", "Divine Power");
        add("gongfa.category.sg", "Divine Rank");
        add("gongfa.category.sz", "Divine Treasure");
        add("gongfa.category.tfst", "Innate Divine Power");
        add("gongfa.category.qy", "Constellation");
        add("gongfa.category.universal", "Universal");
    }
}