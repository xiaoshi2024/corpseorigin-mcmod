//package com.phagens.corpseorigin.Datagen;
//
//
//import com.phagens.corpseorigin.CorpseOrigin;
//import net.minecraft.data.PackOutput;
//import net.neoforged.neoforge.common.data.LanguageProvider;
//
//public class ModChineseLanguageProvider extends LanguageProvider {
//
//    public ModChineseLanguageProvider(PackOutput output) {
//        super(output, CorpseOrigin.MODID, "zh_cn");
//    }
//
//    @Override
//    protected void addTranslations() {
//        addItemTranslations();
//        addBlockTranslations();
//        addEntityTranslations();
//        addEffectTranslations();
//        addGongFaTranslations();
//        addSkillTranslations();
//        addTooltipTranslations();
//        addDeathMessageTranslations();
//        addKeyBindingTranslations();
//        addGuiTranslations();
//        addCommandTranslations();
//        addConfigurationTranslations();
//        addSubtitleTranslations();
//        addOtherTranslations();
//        addGongFaState();
//        addDialogueTranslations();
//        addAdvancementTranslations();
//    }
//
//    private void addItemTranslations() {
//        // 基础物品
//        add("item.corpseorigin.base_gong_fa", "功法");
//        add("item.corpseorigin.gf_cy_ren", "人阶功法残页");
//        add("item.corpseorigin.gf_cy_di", "地阶功法残页");
//        add("item.corpseorigin.gf_cy_tian", "天阶功法残页");
//        add("item.corpseorigin.gf_cy_shen", "神阶功法残页");
//        add("item.corpseorigin.gf_cy_chaoshen", "超神阶功法残页");
//
//
//        // 武器
//        add("item.corpseorigin.ming_juque", "巨阙剑");
//        add("item.corpseorigin.ming_juque_tw", "巨阙剑 贰阶");
//        add("item.corpseorigin.ball_bat", "棒球棍");
//        add("item.corpseorigin.blood_sword", "血剑");
//        add("item.corpseorigin.blood_sword.desc", "§c吸血：造成10%攻击力的治疗");
//
//
//        // 药水与容器
//        add("item.corpseorigin.bywater_bucket", "尸水桶");
//        add("item.corpseorigin.bywater_bottle", "尸水瓶");
//
//        // 强化剂
//        add("item.corpseorigin.s_agent", "黄色强化剂");
//        add("item.corpseorigin.blue_s_agent", "蓝色中和剂");
//        add("item.corpseorigin.null_s_agent", "空药剂");
//
//        // 特殊物品
//        add("item.corpseorigin.hair_dryer", "吹风机");
//        add("item.corpseorigin.mission_scroll", "任务纸条");
//
//        // 器官掉落物
//        add("item.corpseorigin.ordinary_zb_eye", "普通尸眼");
//        add("item.corpseorigin.dr_mu_eye", "穆博士的眼睛");
//        add("item.corpseorigin.zb_worm_item", "尸兄虫");
//
//        // 穆博士的眼睛相关（中文）
//        add("item.corpseorigin.dr_mu_eye.effect1", "§7食用效果:");
//        add("item.corpseorigin.dr_mu_eye.effect2", "  §a• 失去意识的尸兄可恢复人类智慧");
//        add("item.corpseorigin.dr_mu_eye.effect3", "  §e• 已有意识的尸兄获得智慧增强");
//
//// 普通尸眼相关
//        add("item.corpseorigin.ordinary_zb_eye.effect1", "§7食用效果:");
//        add("item.corpseorigin.ordinary_zb_eye.effect2", "  §a• %d%%概率获得永久夜视 (需食用%d个)");
//        add("item.corpseorigin.ordinary_zb_eye.effect3", "  §a• %d%%概率长出额外眼睛 (需食用%d个)");
//        add("item.corpseorigin.ordinary_zb_eye.evolution_success", "§a§l你感受到了进化的力量！");
//        add("item.corpseorigin.ordinary_zb_eye.consumed", "§7已食用尸眼: %d/%d");
//        add("item.corpseorigin.ordinary_zb_eye.night_vision_unlocked", "§6你进化出了永久夜视能力！");
//        add("item.corpseorigin.ordinary_zb_eye.extra_eye_grown", "§c你长出了第%d只额外眼睛！");
//        add("item.corpseorigin.ordinary_zb_eye.multi_eye_evolved", "§c§l你进化出了多眼形态！全部9只眼睛已觉醒！");
//
//// 尸兄虫相关
//        add("item.corpseorigin.zb_worm.description", "一只蠕动的尸兄虫，散发着诡异的气息");
//        add("item.corpseorigin.zb_worm.effect1", "  §a• %d%%概率获得速度增益");
//        add("item.corpseorigin.zb_worm.effect2", "  §a• %d%%概率获得挖掘效率增益");
//        add("item.corpseorigin.zb_worm.corpse_bonus", "§a尸兄食用可获得额外效果");
//        add("item.corpseorigin.zb_worm.not_corpse_warning", "§c你不是尸兄，食用会让你感到不适");
//        add("item.corpseorigin.zb_worm.consumed", "§7你食用了尸兄虫");
//        add("item.corpseorigin.zb_worm.effect_gained", "§a你获得了特殊效果！");
//
//// 任务纸条相关
//        add("item.corpseorigin.mission_scroll.type", "任务类型：%s");
//        add("item.corpseorigin.mission_scroll.progress", "进度：%d / %d");
//        add("item.corpseorigin.mission_scroll.completed", "§a§l已完成！请回到尸王处提交任务");
//        add("item.corpseorigin.mission_scroll.incomplete", "§e任务未完成");
//        add("item.corpseorigin.mission_scroll.reward_points", "奖励：%d 进化点");
//        add("item.corpseorigin.mission_scroll.reward_levels", "奖励：+1 进化等级");
//        add("item.corpseorigin.mission_scroll.can_submit", "§a任务已完成，请拿着纸条右键尸王提交任务！");
//        add("item.corpseorigin.mission_scroll.remaining", "§e还需击杀 %d 个目标");
//
//// 觉醒石（中文）
//        add("item.corpseorigin.yns_c", "觉醒石-平庸");
//        add("item.corpseorigin.yns_b", "觉醒石-普通");
//        add("item.corpseorigin.yns_a", "觉醒石-优秀");
//        add("item.corpseorigin.yns_s", "觉醒石-卓越");
//        add("item.corpseorigin.yns_ss", "觉醒石-传说");
//        add("item.corpseorigin.yns_sss", "觉醒石-大师");
//
//// 功法相关
//        add("item.corpseorigin.lei_xi_gong_fa", "雷系功法");
//
//        // 方块物品
//        add("item.corpseorigin.qi_xings_guan_item", "七星棺");
//        add("item.corpseorigin.zbr_flesh_item", "尸兄肉块");
//        add("item.corpseorigin.alienated_fragment_item", "异化碎块");
//        add("item.corpseorigin.technique_swap_table", "修行工坊");
//
//        // 刷怪蛋
//        add("item.corpseorigin.lower_level_zb_spawn_egg", "尸兄刷怪蛋");
//        add("item.corpseorigin.longyou_spawn_egg", "龙右刷怪蛋");
//        add("item.corpseorigin.zbr_fish_spawn_egg", "尸兄鱼刷怪蛋");
//        add("item.corpseorigin.kaiweinai_spawn_egg", "开胃奶刷怪蛋");
//        add("item.corpseorigin.coco_penguin_spawn_egg", "CoCo企鹅刷怪蛋");
//        add("item.corpseorigin.coco_zombie_spawn_egg", "CoCo尸兄刷怪蛋");
//        add("item.corpseorigin.zb_worm_spawn_egg", "尸兄虫刷怪蛋");
//        add("item.corpseorigin.uncle_spawn_egg", "大叔（少女漫画家）刷怪蛋");
//        add("item.corpseorigin.coco_zombie_x_spawn_egg", "CoCo尸兄-%s刷怪蛋");
//        add("item.corpseorigin.guigun_spawn_egg", "鬼棍刷怪蛋");
//        add("item.corpseorigin.centipede_spawn_egg", "蜈蚣尸兄刷怪蛋");
//    }
//
//    private void addBlockTranslations() {
//        add("block.corpseorigin.qi_xing_guan", "七星棺");
//        add("block.corpseorigin.zbr_flesh", "尸兄肉块");
//        add("block.corpseorigin.alienated_fragment", "异化碎块");
//        add("block.corpseorigin.technique_swap_table", "功法兑换台");
//    }
//
//    private void addEntityTranslations() {
//        add("entity.corpseorigin.lower_level_zb", "尸兄");
//        add("entity.corpseorigin.lower_level_zb.named", "尸兄-%s");
//        add("entity.corpseorigin.lower_level_zb.default", "尸兄");
//        add("entity.corpseorigin.longyou", "龙右");
//        add("entity.corpseorigin.zbr_fish", "尸兄鱼");
//        add("entity.corpseorigin.kaiweinai", "开胃奶");
//        add("entity.corpseorigin.coco_penguin", "CoCo");
//        add("entity.corpseorigin.coco_zombie", "CoCo尸兄");
//        add("entity.corpseorigin.zb_worm", "尸兄虫");
//        add("entity.corpseorigin.uncle", "大叔（少女漫画家）");
//        add("entity.corpseorigin.coco_zombie_x", "CoCo尸兄-%s");
//        add("entity.corpseorigin.guigun", "鬼棍");
//        add("entity.corpseorigin.centipede_head", "蜈蚣尸兄");
//
//        add("entity.corpseorigin.corpse", "尸体");
//        add("entity.corpseorigin.corpse_gib", "尸体残肢");
//    }
//
//    private void addEffectTranslations() {
//        add("effect.corpseorigin.by", "尸水感染");
//        add("effect.corpseorigin.side_effect", "药剂副作用");
//    }
//
//    private void addGongFaTranslations() {
//        // 品级
//        add("gongfa.rarity.1", "人阶");
//        add("gongfa.rarity.2", "地阶");
//        add("gongfa.rarity.3", "天阶");
//        add("gongfa.rarity.4", "神阶");
//        add("gongfa.rarity.5", "超神阶");
//
//        // 层级
//        add("gongfa.ceng.copy_1", "一重天");
//        add("gongfa.ceng.copy_2", "二重天");
//        add("gongfa.ceng.copy_3", "三重天");
//        add("gongfa.ceng.copy_4", "四重天");
//        add("gongfa.ceng.copy_5", "五重天");
//        add("gongfa.ceng.copy_6", "六重天");
//        add("gongfa.ceng.copy_7", "七重天");
//        add("gongfa.ceng.copy_8", "八重天");
//        add("gongfa.ceng.copy_9", "九重天");
//
//        // 属性
//        add("gongfa.attribute.attack_damage", "攻击力");
//        add("gongfa.attribute.movement_speed", "移动速度");
//        add("gongfa.attribute.max_health", "生命值");
//        add("gongfa.attribute.armor", "护甲值");
//        add("gongfa.attribute.knockback_resistance", "击退抗性");
//
//        // 技能名称
//        add("gongfa.skill.lightning_basic", "基础雷击");
//        add("gongfa.skill.lightning_strike", "雷电打击");
//        add("gongfa.skill.lightning_dash", "疾风雷");
//        add("gongfa.skill.heavenly_thunder", "天雷降世");
//        add("gongfa.skill.thousand_thunder", "万雷归宗");
//        add("gongfa.skill.thunder_god", "雷神附体");
//    }
//
//    private void addSkillTranslations() {
//        // 进化技能
//        add("skill.corpseorigin.hardened_skin", "硬化皮肤");
//        add("skill.corpseorigin.hardened_skin.desc", "皮肤硬化，增加护甲值");
//        add("skill.corpseorigin.sharp_claws", "利爪");
//        add("skill.corpseorigin.sharp_claws.desc", "长出锋利的爪子，增加攻击力");
//        add("skill.corpseorigin.devour_enhancement", "吞噬强化");
//        add("skill.corpseorigin.devour_enhancement.desc", "吞噬时恢复更多生命值和饥饿度");
//        add("skill.corpseorigin.evolution_sense", "进化感知");
//        add("skill.corpseorigin.evolution_sense.desc", "获得夜视能力");
//        add("skill.corpseorigin.giant_strength", "巨力");
//        add("skill.corpseorigin.giant_strength.desc", "大幅增加攻击力");
//        add("skill.corpseorigin.berserk", "狂暴");
//        add("skill.corpseorigin.berserk.desc", "临时增加攻击力和移动速度");
//        add("skill.corpseorigin.heavy_strike", "重击");
//        add("skill.corpseorigin.heavy_strike.desc", "攻击带有击退效果");
//        add("skill.corpseorigin.swift_movement", "疾行");
//        add("skill.corpseorigin.swift_movement.desc", "增加移动速度");
//        add("skill.corpseorigin.leap", "跳跃强化");
//        add("skill.corpseorigin.leap.desc", "增加跳跃高度");
//        add("skill.corpseorigin.evasion", "闪避");
//        add("skill.corpseorigin.evasion.desc", "有几率闪避攻击");
//        add("skill.corpseorigin.venom", "毒液");
//        add("skill.corpseorigin.venom.desc", "攻击使目标中毒");
//        add("skill.corpseorigin.regeneration", "快速再生");
//        add("skill.corpseorigin.regeneration.desc", "加快生命恢复速度");
//        add("skill.corpseorigin.fear_aura", "恐惧光环");
//        add("skill.corpseorigin.fear_aura.desc", "使附近敌人虚弱和缓慢");
//        add("skill.corpseorigin.immortal_body", "不死之身");
//        add("skill.corpseorigin.immortal_body.desc", "死亡时复活一次");
//        add("skill.corpseorigin.corpse_king_power", "尸王之力");
//        add("skill.corpseorigin.corpse_king_power.desc", "模拟龙右的感染能力，可以感染其他玩家成为尸兄，并短暂控制被感染的玩家");
//        add("skill.corpseorigin.shadow_strike", "影袭");
//        add("skill.corpseorigin.shadow_strike.desc", "进入隐身状态并大幅提升速度");
//
//        // 多眼技能
//        add("skill.corpseorigin.multi_eye_perception", "多眼感知");
//        add("skill.corpseorigin.multi_eye_perception.desc", "触发多眼的力量，感知周围32格内的敌意生物，高亮显示24格内的敌人");
//        add("skill.corpseorigin.multi_eye.activated", "§c§l多眼感知已激活！你感受到了周围的敌意！");
//        add("skill.corpseorigin.multi_eye.ended", "§7多眼感知效果已结束");
//        add("skill.corpseorigin.multi_eye.cooldown", "§c技能冷却中，还需 %d 秒");
//        add("skill.corpseorigin.multi_eye.detected", "§e感知到 %d 个敌意目标");
//        add("skill.corpseorigin.multi_eye.auto_learned", "§c§l你的多眼觉醒了！获得了多眼感知能力！");
//        add("skill.corpseorigin.disguise", "伪装");
//        add("skill.corpseorigin.disguise.desc", "我看起来像人类？");
//
//        // 技能树
//        add("skilltree.corpseorigin.corpse_evolution", "尸兄进化");
//        add("skilltree.corpseorigin.corpse_evolution.desc", "尸兄的进化路线，通过吞噬获得力量");
//        add("skilltree.corpseorigin.corpse_evolution.condition", "成为尸兄后自动解锁");
//
//        add("skilltype.corpseorigin.basic_evolution", "基础进化");
//        add("skilltype.corpseorigin.power_mutation", "力量变异");
//        add("skilltype.corpseorigin.agility_mutation", "敏捷变异");
//        add("skilltype.corpseorigin.special_mutation", "特殊变异");
//        add("skilltype.corpseorigin.divine_ability", "神级能力");
//        add("skilltype.corpseorigin.supreme_ability", "超神能力");
//
//        // 白小飞技能
//        add("skill.corpseorigin.baixiaofei.hot_blood", "热血");
//        add("skill.corpseorigin.baixiaofei.hot_blood.desc", "白小飞的热血意志，增加攻击力");
//        add("skill.corpseorigin.baixiaofei.fighting_technique", "格斗术");
//        add("skill.corpseorigin.baixiaofei.fighting_technique.desc", "精通各种格斗技巧");
//        add("skill.corpseorigin.baixiaofei.survival_instinct", "生存本能");
//        add("skill.corpseorigin.baixiaofei.survival_instinct.desc", "危险时自动触发防御");
//        add("skill.corpseorigin.baixiaofei.flying_kick", "飞踢");
//        add("skill.corpseorigin.baixiaofei.flying_kick.desc", "高速冲刺并踢击敌人");
//        add("skill.corpseorigin.baixiaofei.sword_mastery", "剑术精通");
//        add("skill.corpseorigin.baixiaofei.sword_mastery.desc", "精通剑术，伤害提升");
//
//        // 龙右技能
//        add("skill.corpseorigin.longyou.corpse_king_authority", "尸王威压");
//        add("skill.corpseorigin.longyou.corpse_king_authority.desc", "尸王的威严，大幅提升属性");
//        add("skill.corpseorigin.longyou.dark_energy", "黑暗能量");
//        add("skill.corpseorigin.longyou.dark_energy.desc", "释放黑暗能量，强化自身");
//        add("skill.corpseorigin.longyou.blood_line", "血继限界");
//        add("skill.corpseorigin.longyou.blood_line.desc", "尸王血脉，持续恢复生命");
//        add("skill.corpseorigin.longyou.undead_immortality", "不死不灭");
//        add("skill.corpseorigin.longyou.undead_immortality.desc", "尸王的不死之身，增加护甲");
//        add("skill.corpseorigin.longyou.haunt", "怨灵缠身");
//        add("skill.corpseorigin.longyou.haunt.desc", "释放怨灵攻击周围敌人");
//        add("skill.corpseorigin.longyou.fear_dominance", "恐惧支配");
//        add("skill.corpseorigin.longyou.fear_dominance.desc", "支配恐惧，隐身并削弱敌人");
//
//        // 技能提示
//        add("skill.corpseorigin.no_activatable", "无可激活技能");
//        add("skill.corpseorigin.cooldown", "技能冷却中，剩余 %d 秒");
//        add("skill.corpseorigin.cooldown_remaining", "冷却剩余: %d 秒");
//        add("skill.corpseorigin.voice_activated", "语音激活技能: %s (置信度: %s)");
//        add("skill.corpseorigin.voice_cooldown", "%s 冷却中");
//    }
//
//    private void addTooltipTranslations() {
//        add("tooltip.corpseorigin.bywater_bottle", "喝下它会中毒...");
//        add("tooltip.corpseorigin.bywater_bucket", "放置后会污染水源");
//        add("tooltip.corpseorigin.ming_juque", "巨阙神兵，可斩杀残血之敌");
//        add("tooltip.corpseorigin.s_agent", "§e初期不稳定的实验药剂-【主角曾扬言：他可是一拳能打死一头牛！你可以试试看！]，直接使用会对身体造成负担");
//        add("tooltip.corpseorigin.s_agent.warning", "§c§l警告：需要配合蓝色中和剂使用，否则可能暴毙！");
//        add("tooltip.corpseorigin.blue_s_agent", "§b用于中和黄色强化剂的副作用，稳定身体状态");
//        add("tooltip.corpseorigin.null_s_agent", "空药剂,用于后续黑色火线的仪器里再添加");
//        add("tooltip.corpseorigin.gongfa.rarity", "品级: %s");
//        add("tooltip.corpseorigin.gongfa.ceng", "功法层级: %s");
//        add("tooltip.corpseorigin.gongfa.attribute", "武学加持:");
//        add("tooltip.corpseorigin.gongfa.skills", "技艺:");
//
//        // ===== 添加器官相关的工具提示 =====
//        add("item.corpseorigin.organ.type", "器官类型: %s");
//        add("item.corpseorigin.organ.evolution_chance", "进化概率: %d%%");
//        add("item.corpseorigin.organ.required_amount", "所需数量: %d");
//        add("item.corpseorigin.organ.corpse_only", "§c只有尸兄才能消化此物");
//        add("item.corpseorigin.organ.not_corpse_warning", "§c你不是尸兄，食用器官会中毒！");
//
//
//    }
//
//    private void addDeathMessageTranslations() {
//        add("death.attack.corpse_water", "%1$s 在尸水中溺亡");
//        add("death.attack.zombie_brother", "%1$s 被尸兄杀死了");
//        add("death.attack.side_effect", "%1$s 因强化剂副作用暴毙");
//    }
//
//    private void addKeyBindingTranslations() {
//        add("key.categories.corpseorigin", "尸兄模组");
//        add("key.corpseorigin.category", "尸兄模组");
//        add("key.corpseorigin.open_gongfu", "打开修行界面");
//        add("key.corpseorigin.take_out_juque", "从背部取出巨阙剑");
//        add("key.corpseorigin.skill_release", "技能释放");
//        add("key.corpseorigin.skill_wheel", "技能轮盘");
//        add("key.corpseorigin.skill_tree", "技能树");
//
//        add("key.corpseorigin.voice_listen", "语音监听");
//    }
//
//    private void addGuiTranslations() {
//        add("container.corpseorigin.gong_fu", "修行");
//        add("gui.corpseorigin.required_level", "需要等级: %d (当前: %d)");
//        add("gui.corpseorigin.skill_need_points", "§c进化点不足");
//        add("gui.corpseorigin.skill_need_level", "§c尸兄等级不足");
//        add("gui.corpseorigin.skill_need_prereq", "§c前置技能未学习");
//        add("gui.corpseorigin.skill_already_learned", "§a已学习");
//        add("gui.corpseorigin.skill_error", "§c无法解锁");
//        add("gui.corpseorigin.skill_unknown", "§c未知原因");
//
//        add("gui.corpseorigin.unlock", "解锁技能");
//        add("gui.corpseorigin.cost", "消耗: %d 进化点");
//        add("gui.corpseorigin.type", "类型: %s");
//        add("gui.corpseorigin.unlocked", "§a已解锁");
//        add("gui.corpseorigin.can_unlock", "§e可解锁");
//        add("gui.corpseorigin.locked", "§c未解锁");
//        add("gui.corpseorigin.prerequisites", "前置技能:");
//        add("gui.corpseorigin.evolution_points", "进化点: %d");
//        add("gui.corpseorigin.evolution_level", "进化等级: %d");
//        add("gui.corpseorigin.convert_experience", "转化经验");
//    }
//
//    private void addCommandTranslations() {
//        add("command.corpseorigin.voice.enabled", "语音触发已启用");
//        add("command.corpseorigin.voice.disabled", "语音触发已禁用");
//        add("command.corpseorigin.voice.threshold_set", "置信度阈值已设置为 %s");
//        add("command.corpseorigin.voice.testing", "测试语音输入: %s");
//        add("command.corpseorigin.voice.list_header", "技能语音绑定列表:");
//        add("command.corpseorigin.voice.binding_added", "已为 %s 添加语音绑定: %s");
//        add("command.corpseorigin.voice.invalid_skill_id", "无效的技能ID");
//        add("command.corpseorigin.voice.skill_not_found", "未找到该技能");
//        add("command.corpseorigin.voice.status", "语音触发状态:");
//        add("command.corpseorigin.voice.status_enabled", "  启用: %s");
//        add("command.corpseorigin.voice.status_threshold", "  置信度阈值: %s");
//        add("command.corpseorigin.voice.status_bindings", "  绑定数量: %d");
//        add("command.corpseorigin.voice.check_available", "§a语音系统可用");
//        add("command.corpseorigin.voice.check_unavailable", "§c语音系统不可用，请检查麦克风设置");
//
//        // 角色指令翻译
//        add("command.corpseorigin.character.list", "§e=== 可用角色列表 ===");
//        add("command.corpseorigin.character.list.entry", "§6- %s: %s");
//        add("command.corpseorigin.character.set.success", "§a已将 %s 的角色设置为 %s");
//        add("command.corpseorigin.character.set.self", "§a你的角色已切换为 %s");
//        add("command.corpseorigin.character.set.skills", "§e获得技能: %s");
//        add("command.corpseorigin.character.clear.success", "§a已清除 %s 的角色，恢复为凡人");
//        add("command.corpseorigin.character.info", "§e=== %s 的角色信息 ===");
//        add("command.corpseorigin.character.info.current", "§6当前角色: %s");
//        add("command.corpseorigin.character.info.description", "§7描述: %s");
//        add("command.corpseorigin.character.info.skills", "§6技能:");
//        add("command.corpseorigin.character.info.skill", "  §a• %s");
//        add("command.corpseorigin.character.info.no_skills", "  §7无特殊技能");
//        add("command.corpseorigin.character.info.traits", "§6特质:");
//        add("command.corpseorigin.character.info.trait", "  §c• %s");
//        add("command.corpseorigin.character.info.mortal", "§7凡人 - 没有特殊能力");
//        add("command.corpseorigin.character.info.self", "§6当前角色: %s");
//        add("command.corpseorigin.character.error.not_found", "§c角色不存在: %s");
//        add("command.corpseorigin.character.error.player_not_found", "§c玩家不存在");
//        add("command.corpseorigin.character.error.already_set", "§c%s 已经是 %s 了");
//    }
//
//    private void addConfigurationTranslations() {
//        add("corpseorigin.configuration.title", "尸兄模组配置");
//        add("corpseorigin.configuration.section.corpseorigin.common.toml", "尸兄模组通用配置");
//        add("corpseorigin.configuration.section.corpseorigin.common.toml.title", "尸兄模组通用配置");
//        add("corpseorigin.configuration.items", "物品列表");
//        add("corpseorigin.configuration.logDirtBlock", "记录泥土方块");
//        add("corpseorigin.configuration.magicNumberIntroduction", "魔法数字文本");
//        add("corpseorigin.configuration.magicNumber", "魔法数字");
//        add("itemGroup.corpseorigin.gongfa","修行体系");
//
//        add("corpseorigin.configuration", "尸源配置");
//        add("corpseorigin.configuration.general", "通用设置");
//        add("corpseorigin.configuration.voice", "语音识别设置");
//
//        add("corpseorigin.config.enableVoiceTrigger", "启用语音触发");
//        add("corpseorigin.config.enableVoiceTrigger.tooltip", "启用技能语音触发系统");
//        add("corpseorigin.config.showSkillIcon", "显示技能图标");
//        add("corpseorigin.config.showSkillIcon.tooltip", "在HUD上显示当前选中的技能图标和冷却时间");
//        add("corpseorigin.config.similarityThreshold", "相似度阈值");
//        add("corpseorigin.config.similarityThreshold.tooltip", "语音识别的相似度阈值，超过此值即视为匹配 (0.0-1.0)");
//        add("corpseorigin.config.voiceLanguage", "语音识别语言");
//        add("corpseorigin.config.voiceLanguage.tooltip", "选择语音识别的语言：zh-CN(中文), en-US(英文), ja-JP(日文)");
//        add("corpseorigin.config.voiceLanguage.zh-CN", "中文 (简体)");
//        add("corpseorigin.config.voiceLanguage.en-US", "English (US)");
//        add("corpseorigin.config.voiceLanguage.ja-JP", "日本語");
//    }
//
//    private void addSubtitleTranslations() {
//        add("subtitles.corpseorigin.ground_chi", "地阶尸兄：吃~~");
//    }
//
//    private void addOtherTranslations() {
//        add("itemGroup.corpseorigin", "尸兄模组");
//        add("message.corpseorigin.cultivation_opened", "修行界面已打开");
//        add("message.corpseorigin.water_infected", "这片水已被尸水污染！");
//
//        // 角色翻译
//        add("character.corpseorigin.mortal", "凡人");
//        add("character.corpseorigin.mortal.desc", "普通的人类，没有特殊能力");
//        add("character.corpseorigin.mortal.trait", "平凡的人类");
//        add("character.corpseorigin.baixiaofei", "白小飞");
//        add("character.corpseorigin.baixiaofei.desc", "尸兄世界的主角，热血青年，拥有强大的战斗意志");
//        add("character.corpseorigin.baixiaofei.trait1", "热血青年");
//        add("character.corpseorigin.baixiaofei.trait2", "战斗天才");
//        add("character.corpseorigin.longyou", "龙右");
//        add("character.corpseorigin.longyou.desc", "尸王，尸族的统治者，拥有恐怖的力量");
//        add("character.corpseorigin.longyou.trait1", "尸王威压");
//        add("character.corpseorigin.longyou.trait2", "不死不灭");
//
//        add("message.corpseorigin.ordinary_player", "§c你只是个普通人");
//        add("message.corpseorigin.skill_tree_coming_soon", "技能树界面即将推出");
//        add("message.corpseorigin.evolution_points_gained", "§a吞噬 %2$s 获得 %1$d 进化点数！");
//        add("message.corpseorigin.voice_recording_start", "§e开始语音录音...（按住V键说话）");
//        add("message.corpseorigin.voice_too_short", "§c录音时间太短或没有检测到声音");
//        add("message.corpseorigin.voice_not_recognized", "§c无法识别语音指令");
//        add("message.corpseorigin.voice_listening", "§e正在监听语音...");
//        add("message.corpseorigin.voice_listening_stopped", "§7语音监听已停止");
//        add("message.corpseorigin.voice_skill_activated", "§a通过语音激活技能: %s");
//        add("message.corpseorigin.voice_skill_not_unlocked", "§c技能未解锁或不可用");
//        add("message.corpseorigin.voice_skill_failed", "§c技能激活失败");
//        add("message.corpseorigin.voice_record_available_skills", "§e=== 可录制的技能 ===");
//        add("message.corpseorigin.voice_record_usage", "§e使用方法: /corpsevoice record <技能ID>");
//        add("message.corpseorigin.voice_record_example", "§7示例: /corpsevoice record berserk");
//        add("message.corpseorigin.voice_record_invalid_skill", "§c无效的技能ID: %s");
//        add("message.corpseorigin.voice_record_check_list", "§7请使用 /corpsevoice record 查看可用技能列表");
//        add("message.corpseorigin.voice_record_start", "§e开始录制 §f%s §e语音模板，请按住V键说话...");
//        add("message.corpseorigin.voice_record_client_only", "§c语音录制只能在客户端执行");
//        add("message.corpseorigin.voice_templates_loaded", "§e已加载 %d 个语音模板");
//        add("message.corpseorigin.voice_templates_empty", "§7使用 /corpsevoice record <技能名> 录制模板");
//        add("message.corpseorigin.voice_templates_client_only", "§c语音模板只能在客户端查看");
//        add("message.corpseorigin.skill_not_found", "§c技能不存在");
//        add("message.corpseorigin.skill_already_learned", "§c你已经学习了这个技能");
//        add("message.corpseorigin.not_enough_points", "§c进化点数不足");
//        add("message.corpseorigin.missing_prerequisite", "§c缺少前置技能");
//        add("message.corpseorigin.skill_learned", "§a成功学习技能: %s");
//        add("message.corpseorigin.evolution_point_gained", "§a你获得了 1 点进化点！");
//        add("message.corpseorigin.experience_converted", "§a你的 5 级经验已转化为 1 点进化点！");
//        add("message.corpseorigin.evolution_level_up", "§a你的进化等级提升至 %s 级！");
//        add("message.corpseorigin.mission_completed", "§a§l任务完成！请回到尸王处提交任务！");
//        add("message.corpseorigin.mission_progress", "§e任务进度：还需完成 %d 个目标");
//        add("message.corpseorigin.mission_progress_kill", "§e任务进度：还需击杀 %d 个目标");
//        add("message.corpseorigin.mission_progress_collect", "§e任务进度：还需收集 %d 个目标");
//        add("message.corpseorigin.mission_progress_infect", "§e任务进度：还需感染 %d 个玩家");
//        add("message.corpseorigin.corpse_infected", "§c§l尸体开始被尸水感染...");
//        add("message.corpseorigin.corpse_transformed", "§4§l尸体变成了尸兄！");
//    }
//
//    private void addDialogueTranslations(){
//        // 任务类型
//        add("mission.corpseorigin.kill_villager", "屠杀村民");
//        add("mission.corpseorigin.kill_zombie", "消灭僵尸/尸兄");
//        add("mission.corpseorigin.kill_any", "击杀生物");
//        add("mission.corpseorigin.infect_player", "感染玩家");
//        add("mission.corpseorigin.collect_item", "收集物品");
//        add("mission.corpseorigin.collect_block", "收集方块");
//
//// 龙右对话
//        add("dialogue.longyou.greeting", "[尸王·龙右] 你来了，我的部下。");
//        add("dialogue.longyou.option.command", "大人您有何吩咐？");
//        add("dialogue.longyou.option.task", "任务升级系统");
//        add("dialogue.longyou.option.appointment", "尸王钦点");
//        add("dialogue.longyou.option.about", "关于尸族");
//        add("dialogue.longyou.command.line1", "§6§l[尸王·龙右] §r我的部下，去为我收集更多的生命力，壮大我尸族的势力。");
//        add("dialogue.longyou.command.line2", "§6§l[尸王·龙右] §r消灭那些反抗我们的人类，将他们转化为我们的同类。");
//        add("dialogue.longyou.command.line3", "§6§l[尸王·龙右] §r当你变得足够强大时，我会赐予你更强大的力量。");
//        add("dialogue.longyou.task.line1", "§6§l[尸王·龙右] §r我已为你开通了任务系统，完成任务可获得进化点和特殊奖励。");
//        add("dialogue.longyou.task.line2", "§6§l[尸王·龙右] §r任务分为：消灭人类、感染村民、收集资源等多种类型。");
//        add("dialogue.longyou.task.line3", "§6§l[尸王·龙右] §r完成的任务越多，你的等级越高，获得的奖励也越丰厚。");
//        add("dialogue.longyou.appointment.line1", "§6§l[尸王·龙右] §r作为我的部下，你展现出了非凡的潜力。");
//        add("dialogue.longyou.appointment.line2", "§6§l[尸王·龙右] §r我钦点你为尸族的精英战士，赐予你特殊的能力。");
//        add("dialogue.longyou.appointment.line3", "§6§l[尸王·龙右] §r好好利用这份力量，为尸族的崛起而战！");
//        add("dialogue.longyou.about.line1", "§6§l[尸王·龙右] §r我们尸族是这个世界的新主宰，将取代脆弱的人类。");
//        add("dialogue.longyou.about.line2", "§6§l[尸王·龙右] §r通过不断进化，我们将变得更加强大，无可匹敌。");
//        add("dialogue.longyou.about.line3", "§6§l[尸王·龙右] §r尸族的未来，就掌握在你们这些部下的手中。");
//        add("dialogue.longyou.invalid_option", "§4§l无效的选项！");
//        add("dialogue.longyou.only_corpse", "§4§l只有尸兄才能与尸王对话！");
//        add("dialogue.longyou.rebellion", "§4§l[尸王·龙右] §r你竟敢对我动手，视为造反！");
//        add("dialogue.longyou.rebellion.broadcast", "§4§l[尸王·龙右] §r%s 竟敢对我动手，视为造反！");
//        add("dialogue.longyou.option.receive_mission", "领取任务");
//        add("dialogue.longyou.option.submit_mission", "提交任务");
//        add("dialogue.longyou.mission.already_has", "§c§l[尸王·龙右] §r你已有未完成的任务，先去完成它！");
//        add("dialogue.longyou.mission.received", "§a§l[尸王·龙右] §r这是你的任务纸条，完成后拿来给我。任务：%s");
//        add("dialogue.longyou.mission.kill_villager", "§6§l[尸王·龙右] §r去屠杀 %d 个村民，让他们知道尸族的力量！");
//        add("dialogue.longyou.mission.kill_zombie", "§6§l[尸王·龙右] §r去消灭 %d 个僵尸或尸兄，清理那些低等的同类！");
//        add("dialogue.longyou.mission.kill_any", "§6§l[尸王·龙右] §r去击杀 %d 个生物，展示你的力量！");
//        add("dialogue.longyou.mission.infect_player", "§6§l[尸王·龙右] §r去感染 %d 个玩家！让他们成为我们尸族的一员！这是最有价值的任务！");
//        add("dialogue.longyou.mission.collect_item", "§6§l[尸王·龙右] §r去收集 %d 个物品！我对这个世界的新奇事物很感兴趣！");
//        add("dialogue.longyou.mission.completed", "§a§l[尸王·龙右] §r做得好！你完成了任务，这是你的奖励！");
//        add("dialogue.longyou.mission.reward_points", "§a你获得了 %d 点进化点！");
//        add("dialogue.longyou.mission.reward_level", "§a你的进化等级提升至 %d 级！");
//        add("dialogue.longyou.mission.no_completed", "§c§l[尸王·龙右] §r你没有完成的任务！先去完成任务再来找我！");
//    }
//
//    private void addAdvancementTranslations(){
//        add("advancements.corpseorigin.root.title", "尸兄起源");
//        add("advancements.corpseorigin.root.description", "欢迎来到尸兄的世界");
//        add("advancements.corpseorigin.become_corpse.title", "尸兄降临");
//        add("advancements.corpseorigin.become_corpse.description", "你已被尸水感染，成为了尸兄的一员");
//        add("advancements.corpseorigin.meet_corpse_king.title", "初见尸王");
//        add("advancements.corpseorigin.meet_corpse_king.description", "你遇见了传说中的尸王龙右");
//        add("advancements.corpseorigin.weapon_shattered.title", "相鼠有皮");
//        add("advancements.corpseorigin.weapon_shattered.description", "远程攻击尸王，武器被其强大的力量震坏");
//        add("advancements.corpseorigin.cannibalism_discovery.title", "吞噬本能");
//        add("advancements.corpseorigin.cannibalism_discovery.description", "你目睹了尸兄之间弱肉强食的残酷法则");
//    }
//
//    private  void addGongFaState(){
//        add("gongfa.category.gf","功法");
//        add("gongfa.category.yn","异能");
//        add("gongfa.category.xm","血脉");
//        add("gongfa.category.fb","法宝");
//        add("gongfa.category.st","神通");
//        add("gongfa.category.sg","神格");
//        add("gongfa.category.sz","神藏");
//        add("gongfa.category.tfst","天赋神通");
//        add("gongfa.category.qy","星宿");
//        add("gongfa.category.universal","通用");
//
//
//    }
//}
