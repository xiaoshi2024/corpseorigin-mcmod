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
    }

    private void addItemTranslations() {
        add("item.corpseorigin.base_gong_fa", "Base Cultivation Method");
        add("item.corpseorigin.gf_cy_ren", "Mortal Rank Inheritance");
        add("item.corpseorigin.gf_cy_di", "Earth Rank Inheritance");
        add("item.corpseorigin.gf_cy_tian", "Heaven Rank Inheritance");
        add("item.corpseorigin.gf_cy_shen", "Divine Rank Inheritance");
        add("item.corpseorigin.gf_cy_chaoshen", "Transcendent Rank Inheritance");

        add("item.corpseorigin.baseball_bat", "Baseball Bat");
        add("item.corpseorigin.blood_sword", "Blood Sword");
        add("item.corpseorigin.blood_sword.desc", "§cLife Steal: Heals 10% of attack damage");



        add("item.corpseorigin.ming_juque", "Juque Sword");
        add("item.corpseorigin.ming_juque_tw", "Juque Sword II");
        
        add("item.corpseorigin.bywater_bucket", "Corpse Water Bucket");
        add("item.corpseorigin.bywater_bottle", "Corpse Water Bottle");
        
        add("item.corpseorigin.s_agent", "Yellow Strengthening Agent");
        add("item.corpseorigin.blue_s_agent", "Blue Neutralizing Agent");
        add("item.corpseorigin.null_s_agent", "Null Agent");
        
        add("item.corpseorigin.hair_dryer", "Hair Dryer");
        add("item.corpseorigin.mission_scroll", "Mission Scroll");
        
        add("item.corpseorigin.ordinary_zb_eye", "Ordinary Zombie Eye");
        add("item.corpseorigin.dr_mu_eye", "Dr. Mu's Eye");
        add("item.corpseorigin.zb_worm_item", "Zombie Worm");
        
        add("item.corpseorigin.qi_xings_guan_item", "Seven Star Coffin");
        add("item.corpseorigin.zbr_flesh_item", "Zombie Flesh");
        add("item.corpseorigin.alienated_fragment_item", "Alienated Fragment");
        add("item.corpseorigin.technique_swap_table", "Technique Swap Table");
        
        add("item.corpseorigin.lower_level_zb_spawn_egg", "Zombie Brother Spawn Egg");
        add("item.corpseorigin.longyou_spawn_egg", "Long You Spawn Egg");
        add("item.corpseorigin.zbr_fish_spawn_egg", "Zombie Fish Spawn Egg");
        add("item.corpseorigin.kaiweinai_spawn_egg", "Kai Wei Nai Spawn Egg");
        add("item.corpseorigin.coco_penguin_spawn_egg", "CoCo Penguin Spawn Egg");
        add("item.corpseorigin.coco_zombie_spawn_egg", "CoCo Zombie Spawn Egg");
        add("item.corpseorigin.zb_worm_spawn_egg", "Zombie Worm Spawn Egg");
        add("item.corpseorigin.uncle_spawn_egg", "Uncle Spawn Egg");
        add("item.corpseorigin.coco_zombie_x_spawn_egg", "CoCo Zombie-%s Spawn Egg");
        add("item.corpseorigin.guigun_spawn_egg", "Gui Gun Spawn Egg");
        add("item.corpseorigin.centipede_spawn_egg", "Centipede Zombie Spawn Egg");
    }

    private void addBlockTranslations() {
        add("block.corpseorigin.qi_xing_guan", "Seven Star Corpse Coffin");
        add("block.corpseorigin.zbr_flesh", "Zombie Brother Flesh");
        add("block.corpseorigin.alienated_fragment", "Alienated Fragment");
        add("block.corpseorigin.technique_swap_table", "Technique Swap Table");
    }

    private void addEntityTranslations() {
        add("entity.corpseorigin.lower_level_zb", "Zombie Brother");
        add("entity.corpseorigin.lower_level_zb.named", "Zombie Brother-%s");
        add("entity.corpseorigin.lower_level_zb.default", "Zombie Brother");
        add("entity.corpseorigin.longyou", "Long You");
        add("entity.corpseorigin.zbr_fish", "Zombie Fish");
        add("entity.corpseorigin.kaiweinai", "Kai Wei Nai");
        add("entity.corpseorigin.coco_penguin", "CoCo");
        add("entity.corpseorigin.coco_zombie", "CoCo Zombie");
        add("entity.corpseorigin.zb_worm", "Zombie Worm");
        add("entity.corpseorigin.uncle", "Uncle");
        add("entity.corpseorigin.coco_zombie_x", "CoCo Zombie-%s");
        add("entity.corpseorigin.guigun", "Gui Gun");
        add("entity.corpseorigin.centipede_head", "Centipede Zombie");
    }

    private void addEffectTranslations() {
        add("effect.corpseorigin.by", "Corpse Water Infection");
        add("effect.corpseorigin.side_effect", "Agent Side Effect");
    }

    private void addGongFaTranslations() {
        add("gongfa.rarity.1", "Mortal Rank");
        add("gongfa.rarity.2", "Earth Rank");
        add("gongfa.rarity.3", "Heaven Rank");
        add("gongfa.rarity.4", "Divine Rank");
        add("gongfa.rarity.5", "Transcendent Rank");
        
        add("gongfa.ceng.copy_1", "First Layer");
        add("gongfa.ceng.copy_2", "Second Layer");
        add("gongfa.ceng.copy_3", "Third Layer");
        add("gongfa.ceng.copy_4", "Fourth Layer");
        add("gongfa.ceng.copy_5", "Fifth Layer");
        add("gongfa.ceng.copy_6", "Sixth Layer");
        add("gongfa.ceng.copy_7", "Seventh Layer");
        add("gongfa.ceng.copy_8", "Eighth Layer");
        add("gongfa.ceng.copy_9", "Ninth Layer");
        
        add("gongfa.attribute.attack_damage", "Attack Damage");
        add("gongfa.attribute.movement_speed", "Movement Speed");
        add("gongfa.attribute.max_health", "Max Health");
        add("gongfa.attribute.armor", "Armor");
        add("gongfa.attribute.knockback_resistance", "Knockback Resistance");
        
        add("gongfa.skill.lightning_basic", "Basic Lightning Strike");
        add("gongfa.skill.lightning_strike", "Lightning Strike");
        add("gongfa.skill.lightning_dash", "Lightning Dash");
        add("gongfa.skill.heavenly_thunder", "Heavenly Thunder");
        add("gongfa.skill.thousand_thunder", "Thousand Thunder Return");
        add("gongfa.skill.thunder_god", "Thunder God Possession");
    }

    private void addSkillTranslations() {
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
        add("skill.corpseorigin.leap", "Leap");
        add("skill.corpseorigin.leap.desc", "Increase jump height");
        add("skill.corpseorigin.evasion", "Evasion");
        add("skill.corpseorigin.evasion.desc", "Chance to dodge attacks");
        add("skill.corpseorigin.venom", "Venom");
        add("skill.corpseorigin.venom.desc", "Attacks poison the target");
        add("skill.corpseorigin.regeneration", "Regeneration");
        add("skill.corpseorigin.regeneration.desc", "Increase health regeneration speed");
        add("skill.corpseorigin.fear_aura", "Fear Aura");
        add("skill.corpseorigin.fear_aura.desc", "Weaken and slow nearby enemies");
        add("skill.corpseorigin.immortal_body", "Immortal Body");
        add("skill.corpseorigin.immortal_body.desc", "Resurrect once upon death");
        add("skill.corpseorigin.corpse_king_power", "Corpse King Power");
        add("skill.corpseorigin.corpse_king_power.desc", "Simulates Long You's infection ability, can infect other players to become zombies, and briefly control infected players");
        add("skill.corpseorigin.shadow_strike", "Shadow Strike");
        add("skill.corpseorigin.shadow_strike.desc", "Enter invisibility and greatly increase speed");
        
        add("skilltree.corpseorigin.corpse_evolution", "Corpse Evolution");
        add("skilltree.corpseorigin.corpse_evolution.desc", "Evolution path of the corpse, gain power through devouring");
        add("skilltree.corpseorigin.corpse_evolution.condition", "Automatically unlocked after becoming a corpse");
        
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
        add("tooltip.corpseorigin.null_s_agent", "Null Agent, used for subsequent black fire instruments");
        add("tooltip.corpseorigin.gongfa.rarity", "Rarity: %s");
        add("tooltip.corpseorigin.gongfa.ceng", "Cultivation Layer: %s");
        add("tooltip.corpseorigin.gongfa.attribute", "Blessings:");
        add("tooltip.corpseorigin.gongfa.skills", "Skills:");
    }

    private void addDeathMessageTranslations() {
        add("death.attack.corpse_water", "%1$s died in corpse water");
        add("death.attack.zombie_brother", "%1$s was killed by Zombie Brother");
        add("death.attack.side_effect", "%1$s died from agent side effects");
    }

    private void addKeyBindingTranslations() {
        add("key.categories.corpseorigin", "Corpse Origin");
        add("key.corpseorigin.category", "Corpse Origin");
        add("key.corpseorigin.open_gongfu", "Open Cultivation Screen");
        add("key.corpseorigin.take_out_juque", "Take out JuQue from Back Slot");
        add("key.corpseorigin.skill_release", "Skill Release");
        add("key.corpseorigin.skill_wheel", "Skill Wheel");
        add("key.corpseorigin.skill_tree", "Skill Tree");
    }

    private void addGuiTranslations() {
        add("container.corpseorigin.gong_fu", "Cultivation");
        add("gui.corpseorigin.required_level", "Required Level: %d (Current: %d)");
        add("gui.corpseorigin.skill_need_points", "§cInsufficient evolution points");
        add("gui.corpseorigin.skill_need_level", "§cInsufficient level");
        add("gui.corpseorigin.skill_need_prereq", "§cPrerequisite skill not learned");
        add("gui.corpseorigin.skill_already_learned", "§aLearned");
        add("gui.corpseorigin.skill_error", "§cFailed to unlock");
        add("gui.corpseorigin.skill_unknown", "§cUnknown reason");
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
        add("corpseorigin.configuration.title", "CorpseOrigin Configs");
        add("corpseorigin.configuration.section.corpseorigin.common.toml", "CorpseOrigin Common Config");
        add("corpseorigin.configuration.section.corpseorigin.common.toml.title", "CorpseOrigin Common Config");
        add("corpseorigin.configuration.items", "Item List");
        add("corpseorigin.configuration.logDirtBlock", "Log Dirt Block");
        add("corpseorigin.configuration.magicNumberIntroduction", "Magic Number Text");
        add("corpseorigin.configuration.magicNumber", "Magic Number");
        
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
        add("subtitles.corpseorigin.ground_chi", "Ground-rank zombie: Eat~~");
    }

    private void addOtherTranslations() {
        add("itemGroup.corpseorigin", "Corpse Origin Mod");
        add("message.corpseorigin.cultivation_opened", "Cultivation interface opened");
        add("message.corpseorigin.water_infected", "This water is contaminated with corpse water!");
    }
}
