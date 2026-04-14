package com.phagens.corpseorigin.voice;

import com.phagens.corpseorigin.CorpseOrigin;
import com.phagens.corpseorigin.network.ActivateSkillPacket;
import com.phagens.corpseorigin.skill.ISkill;
import com.phagens.corpseorigin.skill.ISkillHandler;
import com.phagens.corpseorigin.skill.SkillAttachment;
import com.phagens.corpseorigin.skill.SkillManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 语音技能管理器
 * 管理技能与语音指令的绑定，处理语音识别结果
 */
public class VoiceSkillManager {
    
    private static VoiceSkillManager INSTANCE;
    
    private final Map<ResourceLocation, SkillVoiceBinding> bindings = new HashMap<>();
    private boolean enabled = true;
    private double confidenceThreshold = 0.75;
    
    // 默认语音指令映射
    private static final Map<String, String> DEFAULT_COMMANDS = new HashMap<>();
    
    static {
        // 基础进化技能
        DEFAULT_COMMANDS.put("corpseorigin:hardened_skin", "硬化皮肤");
        DEFAULT_COMMANDS.put("corpseorigin:sharp_claws", "利爪");
        DEFAULT_COMMANDS.put("corpseorigin:devour_enhancement", "吞噬");
        DEFAULT_COMMANDS.put("corpseorigin:evolution_sense", "感知");
        
        // 力量型技能
        DEFAULT_COMMANDS.put("corpseorigin:giant_strength", "巨力");
        DEFAULT_COMMANDS.put("corpseorigin:berserk", "狂暴");
        DEFAULT_COMMANDS.put("corpseorigin:heavy_strike", "重击");
        
        // 敏捷型技能
        DEFAULT_COMMANDS.put("corpseorigin:swift_movement", "疾行");
        DEFAULT_COMMANDS.put("corpseorigin:leap", "跳跃");
        DEFAULT_COMMANDS.put("corpseorigin:evasion", "闪避");
        
        // 特殊型技能
        DEFAULT_COMMANDS.put("corpseorigin:venom", "毒液");
        DEFAULT_COMMANDS.put("corpseorigin:regeneration", "再生");
        DEFAULT_COMMANDS.put("corpseorigin:fear_aura", "恐惧");
        
        // 神级技能
        DEFAULT_COMMANDS.put("corpseorigin:immortal_body", "不死");
        DEFAULT_COMMANDS.put("corpseorigin:corpse_king_power", "尸王");
        DEFAULT_COMMANDS.put("corpseorigin:shadow_strike", "影袭");
    }
    
    private VoiceSkillManager() {
        // 初始化默认绑定
        initDefaultBindings();
    }
    
    /**
     * 获取管理器实例
     */
    public static VoiceSkillManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VoiceSkillManager();
        }
        return INSTANCE;
    }
    
    /**
     * 初始化默认绑定
     */
    private void initDefaultBindings() {
        for (Map.Entry<String, String> entry : DEFAULT_COMMANDS.entrySet()) {
            ResourceLocation skillId = ResourceLocation.parse(entry.getKey());
            String command = entry.getValue();
            
            SkillVoiceBinding binding = new SkillVoiceBinding(skillId, command, command);
            bindings.put(skillId, binding);
        }
        
        CorpseOrigin.LOGGER.info("初始化了 {} 个技能语音绑定", bindings.size());
    }
    
    /**
     * 添加语音绑定
     */
    public void addBinding(ResourceLocation skillId, String voiceCommand, String displayName) {
        SkillVoiceBinding binding = new SkillVoiceBinding(skillId, voiceCommand, displayName);
        bindings.put(skillId, binding);
    }
    
    /**
     * 获取绑定
     */
    public SkillVoiceBinding getBinding(ResourceLocation skillId) {
        return bindings.get(skillId);
    }
    
    /**
     * 移除绑定
     */
    public void removeBinding(ResourceLocation skillId) {
        bindings.remove(skillId);
    }
    
    /**
     * 处理语音识别结果
     */
    public void processVoiceInput(String voiceInput) {
        if (!enabled || voiceInput == null || voiceInput.isEmpty()) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        
        if (player == null) {
            return;
        }
        
        // 获取玩家技能处理器
        ISkillHandler handler = SkillAttachment.getSkillHandler(player);
        
        // 查找匹配的技能
        SkillVoiceBinding bestMatch = null;
        double bestConfidence = 0;
        
        for (SkillVoiceBinding binding : bindings.values()) {
            if (binding.matches(voiceInput)) {
                // 计算置信度
                double confidence = calculateConfidence(voiceInput, binding.getVoiceCommand());
                
                if (confidence > bestConfidence && confidence >= confidenceThreshold) {
                    // 检查玩家是否已学习此技能
                    if (handler.hasLearned(binding.getSkillId())) {
                        bestMatch = binding;
                        bestConfidence = confidence;
                    }
                }
            }
        }
        
        // 执行匹配的技能
        if (bestMatch != null) {
            activateSkill(bestMatch.getSkillId(), bestConfidence);
        } else {
            CorpseOrigin.LOGGER.debug("语音识别无匹配: {}", voiceInput);
        }
    }
    
    /**
     * 激活技能
     */
    private void activateSkill(ResourceLocation skillId, double confidence) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        
        if (player == null) return;
        
        ISkillHandler handler = SkillAttachment.getSkillHandler(player);
        ISkill skill = SkillManager.getInstance().getSkill(skillId);
        
        if (skill == null) {
            CorpseOrigin.LOGGER.warn("语音触发未知技能: {}", skillId);
            return;
        }
        
        // 检查冷却
        if (handler.isOnCooldown(skill)) {
            player.sendSystemMessage(Component.translatable(
                    "skill.corpseorigin.voice_cooldown", skill.getName()));
            return;
        }
        
        // 发送激活请求到服务器
        ActivateSkillPacket packet = new ActivateSkillPacket(skillId);
        PacketDistributor.sendToServer(packet);
        
        // 显示反馈
        player.sendSystemMessage(Component.translatable(
                "skill.corpseorigin.voice_activated", skill.getName(), 
                String.format("%.0f%%", confidence * 100)));
        
        CorpseOrigin.LOGGER.debug("语音激活技能: {} (置信度: {})", skillId, confidence);
    }
    
    /**
     * 计算置信度
     */
    private double calculateConfidence(String input, String command) {
        String normalizedInput = input.toLowerCase().trim();
        String normalizedCommand = command.toLowerCase().trim();
        
        // 完全匹配
        if (normalizedInput.equals(normalizedCommand)) {
            return 1.0;
        }
        
        // 包含匹配
        if (normalizedInput.contains(normalizedCommand)) {
            return 0.9;
        }
        
        // 计算相似度
        return calculateSimilarity(normalizedInput, normalizedCommand);
    }
    
    /**
     * 计算字符串相似度
     */
    private double calculateSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * 计算Levenshtein距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * 启用/禁用语音触发
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置置信度阈值
     */
    public void setConfidenceThreshold(double threshold) {
        this.confidenceThreshold = Math.max(0, Math.min(1, threshold));
    }
    
    /**
     * 获取置信度阈值
     */
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
    
    /**
     * 获取所有绑定
     */
    public Collection<SkillVoiceBinding> getAllBindings() {
        return Collections.unmodifiableCollection(bindings.values());
    }
}
