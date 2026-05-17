package com.phagens.corpseorigin.Datagen;


import com.phagens.corpseorigin.CorpseOrigin;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModChineseLanguageProvider extends LanguageProvider {
    
    public ModChineseLanguageProvider(PackOutput output) {
        super(output, CorpseOrigin.MODID, "zh_cn");
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
        // 基础物品
        add("item.corpseorigin.base_gong_fa", "功法");
        add("item.corpseorigin.gf_cy_ren", "人阶功法残页");
        add("item.corpseorigin.gf_cy_di", "地阶功法残页");
        add("item.corpseorigin.gf_cy_tian", "天阶功法残页");
        add("item.corpseorigin.gf_cy_shen", "神阶功法残页");
        add("item.corpseorigin.gf_cy_chaoshen", "超神阶功法残页");

        add("item.corpseorigin.yns_c", "觉醒石-平庸");
        add("item.corpseorigin.yns_b", "觉醒石-普通");
        add("item.corpseorigin.yns_a", "觉醒石-优秀");
        add("item.corpseorigin.yns_s", "觉醒石-卓越");
        add("item.corpseorigin.yns_ss", "觉醒石-传说");
        add("item.corpseorigin.yns_sss", "觉醒石-大师");
        
        // 武器
        add("item.corpseorigin.ming_juque", "巨阙剑");
        add("item.corpseorigin.ming_juque_tw", "巨阙剑 贰阶");
        
        // 药水与容器
        add("item.corpseorigin.bywater_bucket", "尸水桶");
        add("item.corpseorigin.bywater_bottle", "尸水瓶");
        
        // 强化剂
        add("item.corpseorigin.s_agent", "黄色强化剂");
        add("item.corpseorigin.blue_s_agent", "蓝色中和剂");
        add("item.corpseorigin.null_s_agent", "空药剂");
        
        // 特殊物品
        add("item.corpseorigin.hair_dryer", "吹风机");
        add("item.corpseorigin.mission_scroll", "任务纸条");
        
        // 器官掉落物
        add("item.corpseorigin.ordinary_zb_eye", "普通尸眼");
        add("item.corpseorigin.dr_mu_eye", "穆博士的眼睛");
        add("item.corpseorigin.zb_worm_item", "尸兄虫");
        
        // 方块物品
        add("item.corpseorigin.qi_xings_guan_item", "七星棺");
        add("item.corpseorigin.zbr_flesh_item", "尸兄肉块");
        add("item.corpseorigin.alienated_fragment_item", "异化碎块");
        add("item.corpseorigin.technique_swap_table", "修行工坊");
        
        // 刷怪蛋
        add("item.corpseorigin.lower_level_zb_spawn_egg", "尸兄刷怪蛋");
        add("item.corpseorigin.longyou_spawn_egg", "龙右刷怪蛋");
        add("item.corpseorigin.zbr_fish_spawn_egg", "尸兄鱼刷怪蛋");
        add("item.corpseorigin.kaiweinai_spawn_egg", "开胃奶刷怪蛋");
        add("item.corpseorigin.coco_penguin_spawn_egg", "CoCo企鹅刷怪蛋");
        add("item.corpseorigin.coco_zombie_spawn_egg", "CoCo尸兄刷怪蛋");
        add("item.corpseorigin.zb_worm_spawn_egg", "尸兄虫刷怪蛋");
        add("item.corpseorigin.uncle_spawn_egg", "大叔（少女漫画家）刷怪蛋");
        add("item.corpseorigin.coco_zombie_x_spawn_egg", "CoCo尸兄-%s刷怪蛋");
        add("item.corpseorigin.guigun_spawn_egg", "鬼棍刷怪蛋");
        add("item.corpseorigin.centipede_spawn_egg", "蜈蚣尸兄刷怪蛋");
    }

    private void addBlockTranslations() {
        add("block.corpseorigin.qi_xing_guan", "七星棺");
        add("block.corpseorigin.zbr_flesh", "尸兄肉块");
        add("block.corpseorigin.alienated_fragment", "异化碎块");
        add("block.corpseorigin.technique_swap_table", "功法兑换台");
    }

    private void addEntityTranslations() {
        add("entity.corpseorigin.lower_level_zb", "尸兄");
        add("entity.corpseorigin.lower_level_zb.named", "尸兄-%s");
        add("entity.corpseorigin.lower_level_zb.default", "尸兄");
        add("entity.corpseorigin.longyou", "龙右");
        add("entity.corpseorigin.zbr_fish", "尸兄鱼");
        add("entity.corpseorigin.kaiweinai", "开胃奶");
        add("entity.corpseorigin.coco_penguin", "CoCo");
        add("entity.corpseorigin.coco_zombie", "CoCo尸兄");
        add("entity.corpseorigin.zb_worm", "尸兄虫");
        add("entity.corpseorigin.uncle", "大叔（少女漫画家）");
        add("entity.corpseorigin.coco_zombie_x", "CoCo尸兄-%s");
        add("entity.corpseorigin.guigun", "鬼棍");
        add("entity.corpseorigin.centipede_head", "蜈蚣尸兄");
    }

    private void addEffectTranslations() {
        add("effect.corpseorigin.by", "尸水感染");
        add("effect.corpseorigin.side_effect", "药剂副作用");
    }

    private void addGongFaTranslations() {
        // 品级
        add("gongfa.rarity.1", "人阶");
        add("gongfa.rarity.2", "地阶");
        add("gongfa.rarity.3", "天阶");
        add("gongfa.rarity.4", "神阶");
        add("gongfa.rarity.5", "超神阶");
        
        // 层级
        add("gongfa.ceng.copy_1", "一重天");
        add("gongfa.ceng.copy_2", "二重天");
        add("gongfa.ceng.copy_3", "三重天");
        add("gongfa.ceng.copy_4", "四重天");
        add("gongfa.ceng.copy_5", "五重天");
        add("gongfa.ceng.copy_6", "六重天");
        add("gongfa.ceng.copy_7", "七重天");
        add("gongfa.ceng.copy_8", "八重天");
        add("gongfa.ceng.copy_9", "九重天");
        
        // 属性
        add("gongfa.attribute.attack_damage", "攻击力");
        add("gongfa.attribute.movement_speed", "移动速度");
        add("gongfa.attribute.max_health", "生命值");
        add("gongfa.attribute.armor", "护甲值");
        add("gongfa.attribute.knockback_resistance", "击退抗性");
        
        // 技能名称
        add("gongfa.skill.lightning_basic", "基础雷击");
        add("gongfa.skill.lightning_strike", "雷电打击");
        add("gongfa.skill.lightning_dash", "疾风雷");
        add("gongfa.skill.heavenly_thunder", "天雷降世");
        add("gongfa.skill.thousand_thunder", "万雷归宗");
        add("gongfa.skill.thunder_god", "雷神附体");
    }

    private void addSkillTranslations() {
        // 进化技能
        add("skill.corpseorigin.hardened_skin", "硬化皮肤");
        add("skill.corpseorigin.hardened_skin.desc", "皮肤硬化，增加护甲值");
        add("skill.corpseorigin.sharp_claws", "利爪");
        add("skill.corpseorigin.sharp_claws.desc", "长出锋利的爪子，增加攻击力");
        add("skill.corpseorigin.devour_enhancement", "吞噬强化");
        add("skill.corpseorigin.devour_enhancement.desc", "吞噬时恢复更多生命值和饥饿度");
        add("skill.corpseorigin.evolution_sense", "进化感知");
        add("skill.corpseorigin.evolution_sense.desc", "获得夜视能力");
        add("skill.corpseorigin.giant_strength", "巨力");
        add("skill.corpseorigin.giant_strength.desc", "大幅增加攻击力");
        add("skill.corpseorigin.berserk", "狂暴");
        add("skill.corpseorigin.berserk.desc", "临时增加攻击力和移动速度");
        add("skill.corpseorigin.heavy_strike", "重击");
        add("skill.corpseorigin.heavy_strike.desc", "攻击带有击退效果");
        add("skill.corpseorigin.swift_movement", "疾行");
        add("skill.corpseorigin.swift_movement.desc", "增加移动速度");
        add("skill.corpseorigin.leap", "跳跃强化");
        add("skill.corpseorigin.leap.desc", "增加跳跃高度");
        add("skill.corpseorigin.evasion", "闪避");
        add("skill.corpseorigin.evasion.desc", "有几率闪避攻击");
        add("skill.corpseorigin.venom", "毒液");
        add("skill.corpseorigin.venom.desc", "攻击使目标中毒");
        add("skill.corpseorigin.regeneration", "快速再生");
        add("skill.corpseorigin.regeneration.desc", "加快生命恢复速度");
        add("skill.corpseorigin.fear_aura", "恐惧光环");
        add("skill.corpseorigin.fear_aura.desc", "使附近敌人虚弱和缓慢");
        add("skill.corpseorigin.immortal_body", "不死之身");
        add("skill.corpseorigin.immortal_body.desc", "死亡时复活一次");
        add("skill.corpseorigin.corpse_king_power", "尸王之力");
        add("skill.corpseorigin.corpse_king_power.desc", "模拟龙右的感染能力，可以感染其他玩家成为尸兄，并短暂控制被感染的玩家");
        add("skill.corpseorigin.shadow_strike", "影袭");
        add("skill.corpseorigin.shadow_strike.desc", "进入隐身状态并大幅提升速度");
        
        // 技能树
        add("skilltree.corpseorigin.corpse_evolution", "尸兄进化");
        add("skilltree.corpseorigin.corpse_evolution.desc", "尸兄的进化路线，通过吞噬获得力量");
        add("skilltree.corpseorigin.corpse_evolution.condition", "成为尸兄后自动解锁");
        
        // 技能提示
        add("skill.corpseorigin.no_activatable", "无可激活技能");
        add("skill.corpseorigin.cooldown", "技能冷却中，剩余 %d 秒");
        add("skill.corpseorigin.cooldown_remaining", "冷却剩余: %d 秒");
        add("skill.corpseorigin.voice_activated", "语音激活技能: %s (置信度: %s)");
        add("skill.corpseorigin.voice_cooldown", "%s 冷却中");
    }

    private void addTooltipTranslations() {
        add("tooltip.corpseorigin.bywater_bottle", "喝下它会中毒...");
        add("tooltip.corpseorigin.bywater_bucket", "放置后会污染水源");
        add("tooltip.corpseorigin.ming_juque", "巨阙神兵，可斩杀残血之敌");
        add("tooltip.corpseorigin.s_agent", "§e初期不稳定的实验药剂-【主角曾扬言：他可是一拳能打死一头牛！你可以试试看！]，直接使用会对身体造成负担");
        add("tooltip.corpseorigin.s_agent.warning", "§c§l警告：需要配合蓝色中和剂使用，否则可能暴毙！");
        add("tooltip.corpseorigin.blue_s_agent", "§b用于中和黄色强化剂的副作用，稳定身体状态");
        add("tooltip.corpseorigin.null_s_agent", "空药剂,用于后续黑色火线的仪器里再添加");
        add("tooltip.corpseorigin.gongfa.rarity", "品级: %s");
        add("tooltip.corpseorigin.gongfa.ceng", "功法层级: %s");
        add("tooltip.corpseorigin.gongfa.attribute", "武学加持:");
        add("tooltip.corpseorigin.gongfa.skills", "技艺:");
    }

    private void addDeathMessageTranslations() {
        add("death.attack.corpse_water", "%1$s 在尸水中溺亡");
        add("death.attack.zombie_brother", "%1$s 被尸兄杀死了");
        add("death.attack.side_effect", "%1$s 因强化剂副作用暴毙");
    }

    private void addKeyBindingTranslations() {
        add("key.categories.corpseorigin", "尸兄模组");
        add("key.corpseorigin.category", "尸兄模组");
        add("key.corpseorigin.open_gongfu", "打开修行界面");
        add("key.corpseorigin.take_out_juque", "从背部取出巨阙剑");
        add("key.corpseorigin.skill_release", "技能释放");
        add("key.corpseorigin.skill_wheel", "技能轮盘");
        add("key.corpseorigin.skill_tree", "技能树");
    }

    private void addGuiTranslations() {
        add("container.corpseorigin.gong_fu", "修行");
        add("gui.corpseorigin.required_level", "需要等级: %d (当前: %d)");
        add("gui.corpseorigin.skill_need_points", "§c进化点不足");
        add("gui.corpseorigin.skill_need_level", "§c尸兄等级不足");
        add("gui.corpseorigin.skill_need_prereq", "§c前置技能未学习");
        add("gui.corpseorigin.skill_already_learned", "§a已学习");
        add("gui.corpseorigin.skill_error", "§c无法解锁");
        add("gui.corpseorigin.skill_unknown", "§c未知原因");
    }

    private void addCommandTranslations() {
        add("command.corpseorigin.voice.enabled", "语音触发已启用");
        add("command.corpseorigin.voice.disabled", "语音触发已禁用");
        add("command.corpseorigin.voice.threshold_set", "置信度阈值已设置为 %s");
        add("command.corpseorigin.voice.testing", "测试语音输入: %s");
        add("command.corpseorigin.voice.list_header", "技能语音绑定列表:");
        add("command.corpseorigin.voice.binding_added", "已为 %s 添加语音绑定: %s");
        add("command.corpseorigin.voice.invalid_skill_id", "无效的技能ID");
        add("command.corpseorigin.voice.skill_not_found", "未找到该技能");
        add("command.corpseorigin.voice.status", "语音触发状态:");
        add("command.corpseorigin.voice.status_enabled", "  启用: %s");
        add("command.corpseorigin.voice.status_threshold", "  置信度阈值: %s");
        add("command.corpseorigin.voice.status_bindings", "  绑定数量: %d");
        add("command.corpseorigin.voice.check_available", "§a语音系统可用");
        add("command.corpseorigin.voice.check_unavailable", "§c语音系统不可用，请检查麦克风设置");
    }

    private void addConfigurationTranslations() {
        add("corpseorigin.configuration.title", "尸兄模组配置");
        add("corpseorigin.configuration.section.corpseorigin.common.toml", "尸兄模组通用配置");
        add("corpseorigin.configuration.section.corpseorigin.common.toml.title", "尸兄模组通用配置");
        add("corpseorigin.configuration.items", "物品列表");
        add("corpseorigin.configuration.logDirtBlock", "记录泥土方块");
        add("corpseorigin.configuration.magicNumberIntroduction", "魔法数字文本");
        add("corpseorigin.configuration.magicNumber", "魔法数字");
        add("itemGroup.corpseorigin.gongfa","修行体系");
        
        add("corpseorigin.configuration", "尸源配置");
        add("corpseorigin.configuration.general", "通用设置");
        add("corpseorigin.configuration.voice", "语音识别设置");
        
        add("corpseorigin.config.enableVoiceTrigger", "启用语音触发");
        add("corpseorigin.config.enableVoiceTrigger.tooltip", "启用技能语音触发系统");
        add("corpseorigin.config.showSkillIcon", "显示技能图标");
        add("corpseorigin.config.showSkillIcon.tooltip", "在HUD上显示当前选中的技能图标和冷却时间");
        add("corpseorigin.config.similarityThreshold", "相似度阈值");
        add("corpseorigin.config.similarityThreshold.tooltip", "语音识别的相似度阈值，超过此值即视为匹配 (0.0-1.0)");
        add("corpseorigin.config.voiceLanguage", "语音识别语言");
        add("corpseorigin.config.voiceLanguage.tooltip", "选择语音识别的语言：zh-CN(中文), en-US(英文), ja-JP(日文)");
        add("corpseorigin.config.voiceLanguage.zh-CN", "中文 (简体)");
        add("corpseorigin.config.voiceLanguage.en-US", "English (US)");
        add("corpseorigin.config.voiceLanguage.ja-JP", "日本語");
    }

    private void addSubtitleTranslations() {
        add("subtitles.corpseorigin.ground_chi", "地阶尸兄：吃~~");
    }

    private void addOtherTranslations() {
        add("itemGroup.corpseorigin", "尸兄模组");
        add("message.corpseorigin.cultivation_opened", "修行界面已打开");
        add("message.corpseorigin.water_infected", "这片水已被尸水污染！");
    }
}
