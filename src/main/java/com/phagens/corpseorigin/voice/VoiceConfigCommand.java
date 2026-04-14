package com.phagens.corpseorigin.voice;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.phagens.corpseorigin.skill.ISkill;
import com.phagens.corpseorigin.skill.SkillManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 语音配置命令
 */
public class VoiceConfigCommand {
    
    /**
     * 注册命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("corpsevoice")
                .requires(source -> source.hasPermission(0))
                
                // 启用/禁用语音触发
                .then(Commands.literal("enable")
                    .executes(context -> {
                        VoiceSkillManager.getInstance().setEnabled(true);
                        context.getSource().sendSuccess(
                            () -> Component.translatable("command.corpseorigin.voice.enabled"), 
                            false);
                        return 1;
                    }))
                
                .then(Commands.literal("disable")
                    .executes(context -> {
                        VoiceSkillManager.getInstance().setEnabled(false);
                        context.getSource().sendSuccess(
                            () -> Component.translatable("command.corpseorigin.voice.disabled"), 
                            false);
                        return 1;
                    }))
                
                // 设置置信度阈值
                .then(Commands.literal("threshold")
                    .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                        .executes(context -> {
                            double threshold = DoubleArgumentType.getDouble(context, "value");
                            VoiceSkillManager.getInstance().setConfidenceThreshold(threshold);
                            context.getSource().sendSuccess(
                                () -> Component.translatable(
                                    "command.corpseorigin.voice.threshold_set", 
                                    String.format("%.0f%%", threshold * 100)), 
                                false);
                            return 1;
                        })))
                
                // 测试语音输入
                .then(Commands.literal("test")
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(context -> {
                            String text = StringArgumentType.getString(context, "text");
                            context.getSource().sendSuccess(
                                () -> Component.translatable(
                                    "command.corpseorigin.voice.testing", text), 
                                false);
                            
                            // 处理语音输入
                            VoiceSkillManager.getInstance().processVoiceInput(text);
                            return 1;
                        })))
                
                // 测试语音系统
                .then(Commands.literal("check")
                    .executes(context -> {
                        boolean available = VoiceTriggerIntegration.isVoiceSystemAvailable();
                        if (available) {
                            context.getSource().sendSuccess(
                                () -> Component.literal("§a语音系统可用"), false);
                        } else {
                            context.getSource().sendFailure(
                                Component.literal("§c语音系统不可用，请检查麦克风设置"));
                        }
                        return 1;
                    }))
                
                // 列出所有语音绑定
                .then(Commands.literal("list")
                    .executes(context -> {
                        context.getSource().sendSuccess(
                            () -> Component.translatable("command.corpseorigin.voice.list_header"), 
                            false);
                        
                        for (SkillVoiceBinding binding : VoiceSkillManager.getInstance().getAllBindings()) {
                            ISkill skill = SkillManager.getInstance().getSkill(binding.getSkillId());
                            String skillName = skill != null ? skill.getName().getString() : "Unknown";
                            
                            context.getSource().sendSuccess(
                                () -> Component.literal("  - " + skillName + ": " + binding.getVoiceCommand()), 
                                false);
                        }
                        return 1;
                    }))
                
                // 添加自定义绑定
                .then(Commands.literal("bind")
                    .then(Commands.argument("skill", StringArgumentType.word())
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                            .executes(context -> {
                                String skillIdStr = StringArgumentType.getString(context, "skill");
                                String command = StringArgumentType.getString(context, "command");
                                
                                ResourceLocation skillId = ResourceLocation.tryParse(skillIdStr);
                                if (skillId == null) {
                                    context.getSource().sendFailure(
                                        Component.translatable("command.corpseorigin.voice.invalid_skill_id"));
                                    return 0;
                                }
                                
                                ISkill skill = SkillManager.getInstance().getSkill(skillId);
                                if (skill == null) {
                                    context.getSource().sendFailure(
                                        Component.translatable("command.corpseorigin.voice.skill_not_found"));
                                    return 0;
                                }
                                
                                VoiceSkillManager.getInstance().addBinding(
                                    skillId, command, command);
                                
                                context.getSource().sendSuccess(
                                    () -> Component.translatable(
                                        "command.corpseorigin.voice.binding_added",
                                        skill.getName(), command), 
                                    false);
                                return 1;
                            }))))
                
                // 录制语音模板（仅客户端）
                .then(Commands.literal("record")
                    .executes(context -> {
                        // 显示可用的技能列表
                        if (context.getSource().isPlayer()) {
                            context.getSource().sendSuccess(
                                () -> Component.translatable("message.corpseorigin.voice_record_available_skills"), false);

                            String[] skills = {
                                "berserk - §c§l狂化",
                                "hardened_skin - §7硬化皮肤",
                                "sharp_claws - §4锐利爪牙",
                                "devour_enhancement - §2吞噬强化",
                                "evolution_sense - §a进化感知",
                                "giant_strength - §c巨人力量",
                                "heavy_strike - §8重击",
                                "swift_movement - §b迅捷移动",
                                "leap - §a飞跃",
                                "evasion - §e闪避",
                                "venom - §2剧毒",
                                "regeneration - §d再生",
                                "fear_aura - §5恐惧光环",
                                "immortal_body - §6不朽之身",
                                "corpse_king_power - §4§l尸王之力",
                                "shadow_strike - §8暗影突袭"
                            };

                            for (String skill : skills) {
                                context.getSource().sendSuccess(
                                    () -> Component.literal("§7  " + skill), false);
                            }

                            context.getSource().sendSuccess(
                                () -> Component.translatable("message.corpseorigin.voice_record_usage"), false);
                            context.getSource().sendSuccess(
                                () -> Component.translatable("message.corpseorigin.voice_record_example"), false);

                            return 1;
                        } else {
                            context.getSource().sendFailure(
                                Component.translatable("message.corpseorigin.voice_record_client_only"));
                            return 0;
                        }
                    })
                    .then(Commands.argument("skill", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            // 提供技能ID补全建议
                            String[] skillIds = {
                                "berserk", "hardened_skin", "sharp_claws", "devour_enhancement",
                                "evolution_sense", "giant_strength", "heavy_strike", "swift_movement",
                                "leap", "evasion", "venom", "regeneration", "fear_aura",
                                "immortal_body", "corpse_king_power", "shadow_strike"
                            };
                            for (String skillId : skillIds) {
                                builder.suggest(skillId);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            // 检查是否是玩家执行（单人游戏或多人游戏客户端）
                            if (context.getSource().isPlayer()) {
                                String inputSkillName = StringArgumentType.getString(context, "skill");

                                // 验证技能ID是否有效
                                String[] validSkills = {
                                    "berserk", "hardened_skin", "sharp_claws", "devour_enhancement",
                                    "evolution_sense", "giant_strength", "heavy_strike", "swift_movement",
                                    "leap", "evasion", "venom", "regeneration", "fear_aura",
                                    "immortal_body", "corpse_king_power", "shadow_strike"
                                };

                                String finalSkillName = null;
                                for (String validSkill : validSkills) {
                                    if (validSkill.equalsIgnoreCase(inputSkillName)) {
                                        finalSkillName = validSkill; // 使用正确的技能ID
                                        break;
                                    }
                                }

                                if (finalSkillName == null) {
                                    context.getSource().sendFailure(
                                        Component.translatable("message.corpseorigin.voice_record_invalid_skill", inputSkillName));
                                    context.getSource().sendFailure(
                                        Component.translatable("message.corpseorigin.voice_record_check_list"));
                                    return 0;
                                }

                                final String skillToRecord = finalSkillName;
                                context.getSource().sendSuccess(
                                    () -> Component.translatable("message.corpseorigin.voice_record_start", skillToRecord),
                                    false);

                                // 启动录制（3秒）
                            VoiceTemplateManager.getInstance().recordTemplate(skillToRecord, 3000);

                                return 1;
                            } else {
                                context.getSource().sendFailure(
                                    Component.translatable("message.corpseorigin.voice_record_client_only"));
                                return 0;
                            }
                        })))

                // 查看模板状态（仅客户端）
                .then(Commands.literal("templates")
                    .executes(context -> {
                        if (context.getSource().isPlayer()) {
                            VoiceTemplateManager manager = VoiceTemplateManager.getInstance();
                            int count = manager.getTemplateCount();

                            context.getSource().sendSuccess(
                                () -> Component.translatable("message.corpseorigin.voice_templates_loaded", count),
                                false);

                            if (count == 0) {
                                context.getSource().sendSuccess(
                                    () -> Component.translatable("message.corpseorigin.voice_templates_empty"),
                                    false);
                            }

                            return 1;
                        } else {
                            context.getSource().sendFailure(
                                Component.translatable("message.corpseorigin.voice_templates_client_only"));
                            return 0;
                        }
                    }))
                
                // 显示状态
                .executes(context -> {
                    VoiceSkillManager manager = VoiceSkillManager.getInstance();
                    boolean enabled = manager.isEnabled();
                    double threshold = manager.getConfidenceThreshold();
                    
                    context.getSource().sendSuccess(
                        () -> Component.translatable("command.corpseorigin.voice.status"), 
                        false);
                    context.getSource().sendSuccess(
                        () -> Component.translatable(
                            "command.corpseorigin.voice.status_enabled", 
                            enabled ? "是" : "否"), 
                        false);
                    context.getSource().sendSuccess(
                        () -> Component.translatable(
                            "command.corpseorigin.voice.status_threshold", 
                            String.format("%.0f%%", threshold * 100)), 
                        false);
                    context.getSource().sendSuccess(
                        () -> Component.translatable(
                            "command.corpseorigin.voice.status_bindings", 
                            manager.getAllBindings().size()), 
                        false);
                    
                    return 1;
                })
        );
    }
}
